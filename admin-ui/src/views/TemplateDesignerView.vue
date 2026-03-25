<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { getTemplateDetail, saveTemplateDesign } from '../api/template-design'
import { getAdminOptions } from '../api/options'
import {
  buildTemplateDesignPayload,
  createDefaultSectionStyleConfig,
  normalizeTemplateSortOrders,
  parseSectionStyleConfig,
  stringifySectionStyleConfig,
  validateTemplateDesign,
} from '../utils/template-design'

const route = useRoute()
const router = useRouter()
const design = ref(createEmptyDesign())
const options = ref({ sectionTypes: [], contentFormats: [] })
const selectedPanel = ref('section')
const selectedIndex = ref(0)
const selectedMealIndex = ref(0)
const selectedItemIndex = ref(0)
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)
const loadError = ref('')
const loadVersion = ref(0)
const styleConfigNotice = ref('')
let keySeed = 0

const templateId = computed(() => route.params.id || null)
const selectedSection = computed(() => design.value.sections?.[selectedIndex.value] || null)
const selectedMeal = computed(() => design.value.meals?.[selectedMealIndex.value] || null)
const selectedItem = computed(() => selectedMeal.value?.items?.[selectedItemIndex.value] || null)
const isEmptyState = computed(() => !loading.value && !loadError.value && !templateId.value)
const isDesignerReady = computed(() => !loading.value && !loadError.value && !!templateId.value)
const sectionTypeLabelMap = computed(() => buildOptionLabelMap(options.value.sectionTypes))
const contentFormatLabelMap = computed(() => buildOptionLabelMap(options.value.contentFormats))
const stylePresetOptions = [
  { key: 'emphasis', label: '强调显示' },
  { key: 'lightBackground', label: '浅色背景' },
  { key: 'imageFirst', label: '图片优先' },
  { key: 'printFriendly', label: '适合打印' },
]

function createEmptyDesign() {
  return { id: null, name: '', description: '', themeCode: '', status: 1, isDefault: 0, sections: [], meals: [] }
}

function buildOptionLabelMap(list = []) {
  return list.reduce((map, item) => {
    map[item.value] = item.label
    return map
  }, {})
}

function nextKey(prefix) {
  keySeed += 1
  return `${prefix}-${Date.now()}-${keySeed}`
}

function createSectionStyleState(styleConfigJson) {
  const parsed = parseSectionStyleConfig(styleConfigJson)
  return {
    config: parsed.config,
    hasCustomData: parsed.hasCustomData,
    parseError: parsed.parseError,
  }
}

function normalizeLoadedDesign(payload) {
  return {
    ...createEmptyDesign(),
    ...payload,
    sections: (payload.sections || []).map((section) => ({
      ...section,
      _key: nextKey('section'),
      _styleConfigState: createSectionStyleState(section.styleConfigJson),
    })),
    meals: (payload.meals || []).map((meal) => ({
      ...meal,
      _key: nextKey('meal'),
      items: (meal.items || []).map((item) => ({ ...item, _key: nextKey('item') })),
    })),
  }
}

function markDirty() {
  dirty.value = true
}

function syncSortOrders() {
  design.value = normalizeTemplateSortOrders(design.value)
}

function openSection(index) {
  selectedPanel.value = 'section'
  selectedIndex.value = index
  const section = design.value.sections?.[index]
  if (!section?._styleConfigState) {
    section._styleConfigState = createSectionStyleState(section?.styleConfigJson)
  }
  styleConfigNotice.value = buildStyleConfigNotice(section)
}

function openMeal(index) {
  selectedPanel.value = 'meal'
  selectedMealIndex.value = index
  selectedItemIndex.value = 0
  styleConfigNotice.value = ''
}

function openItem(mealIndex, itemIndex) {
  selectedPanel.value = 'item'
  selectedMealIndex.value = mealIndex
  selectedItemIndex.value = itemIndex
  styleConfigNotice.value = ''
}

function addSection() {
  const firstType = options.value.sectionTypes?.[0]?.value || 'REMARK'
  design.value.sections.push({
    _key: nextKey('section'),
    id: null,
    sectionType: firstType,
    title: '新建区块',
    sortOrder: design.value.sections.length + 1,
    enabled: true,
    allowImage: false,
    styleConfigJson: '',
    _styleConfigState: createSectionStyleState(''),
  })
  syncSortOrders()
  openSection(design.value.sections.length - 1)
  markDirty()
}

function removeSection(index) {
  design.value.sections.splice(index, 1)
  syncSortOrders()
  if (!design.value.sections.length) {
    selectedPanel.value = 'meal'
    styleConfigNotice.value = ''
  } else {
    selectedIndex.value = Math.min(index, design.value.sections.length - 1)
    openSection(selectedIndex.value)
  }
  markDirty()
}

function addMeal() {
  design.value.meals.push({
    _key: nextKey('meal'),
    id: null,
    mealCode: 'custom',
    mealName: '新餐次',
    timeLabel: '',
    sortOrder: design.value.meals.length + 1,
    enabled: true,
    items: [],
  })
  syncSortOrders()
  openMeal(design.value.meals.length - 1)
  markDirty()
}

function removeMeal(index) {
  design.value.meals.splice(index, 1)
  syncSortOrders()
  if (!design.value.meals.length) {
    selectedPanel.value = 'section'
  } else {
    selectedMealIndex.value = Math.min(index, design.value.meals.length - 1)
    selectedItemIndex.value = 0
  }
  markDirty()
}

function addMealItem() {
  if (!selectedMeal.value) return
  const format = options.value.contentFormats?.[0]?.value || 'PLAIN_TEXT'
  selectedMeal.value.items.push({
    _key: nextKey('item'),
    id: null,
    itemCode: 'custom_item',
    itemName: '新字段',
    contentFormat: format,
    sortOrder: selectedMeal.value.items.length + 1,
    enabled: true,
    allowImage: false,
  })
  syncSortOrders()
  openItem(selectedMealIndex.value, selectedMeal.value.items.length - 1)
  markDirty()
}

function removeMealItem(mealIndex, itemIndex) {
  design.value.meals[mealIndex].items.splice(itemIndex, 1)
  syncSortOrders()
  if (!design.value.meals[mealIndex].items.length) {
    openMeal(mealIndex)
  } else {
    openItem(mealIndex, Math.min(itemIndex, design.value.meals[mealIndex].items.length - 1))
  }
  markDirty()
}

function onSectionDragChange() {
  syncSortOrders()
  if (selectedPanel.value === 'section') {
    const key = selectedSection.value?._key
    if (key) {
      selectedIndex.value = design.value.sections.findIndex((item) => item._key === key)
    }
  }
  markDirty()
}

function onMealDragChange() {
  syncSortOrders()
  if (selectedPanel.value === 'meal' || selectedPanel.value === 'item') {
    const mealKey = selectedMeal.value?._key
    if (mealKey) {
      selectedMealIndex.value = design.value.meals.findIndex((item) => item._key === mealKey)
    }
  }
  markDirty()
}

function onMealItemDragChange(mealIndex) {
  syncSortOrders()
  if (selectedPanel.value === 'item') {
    const meal = design.value.meals[mealIndex]
    const itemKey = selectedItem.value?._key
    if (meal && itemKey) {
      selectedMealIndex.value = mealIndex
      selectedItemIndex.value = meal.items.findIndex((item) => item._key === itemKey)
    }
  }
  markDirty()
}

function updateSectionStyleFlag(flag, value) {
  if (!selectedSection.value) return
  if (!selectedSection.value._styleConfigState) {
    selectedSection.value._styleConfigState = {
      config: createDefaultSectionStyleConfig(),
      hasCustomData: false,
      parseError: false,
    }
  }
  selectedSection.value._styleConfigState.config[flag] = value
  selectedSection.value._styleConfigState.parseError = false
  selectedSection.value.styleConfigJson = stringifySectionStyleConfig(selectedSection.value._styleConfigState.config)
  styleConfigNotice.value = buildStyleConfigNotice(selectedSection.value)
  markDirty()
}

function buildStyleConfigNotice(section) {
  if (!section?._styleConfigState) {
    return ''
  }
  if (section._styleConfigState.parseError) {
    return '已存在无法识别的样式数据，当前先按默认样式展示。'
  }
  if (section._styleConfigState.hasCustomData) {
    return '已检测到额外自定义样式，本页按钮不会覆盖这些未识别字段。'
  }
  return ''
}

function getSectionTypeLabel(value) {
  return sectionTypeLabelMap.value[value] || value || '未设置'
}

function getContentFormatLabel(value) {
  return contentFormatLabelMap.value[value] || value || '纯文本'
}

async function loadDesigner() {
  const currentVersion = loadVersion.value + 1
  loadVersion.value = currentVersion
  loading.value = true
  loadError.value = ''
  styleConfigNotice.value = ''
  try {
    options.value = await getAdminOptions()
    if (!templateId.value) {
      design.value = createEmptyDesign()
      dirty.value = false
      return
    }
    const detail = await getTemplateDetail(templateId.value)
    if (!detail?.id) {
      loadError.value = '未找到对应模板，请返回模板中心重新选择。'
      design.value = createEmptyDesign()
      dirty.value = false
      return
    }
    if (loadVersion.value !== currentVersion) {
      return
    }
    design.value = normalizeLoadedDesign(detail)
    selectedPanel.value = design.value.sections.length ? 'section' : design.value.meals.length ? 'meal' : 'section'
    selectedIndex.value = 0
    selectedMealIndex.value = 0
    selectedItemIndex.value = 0
    if (selectedPanel.value === 'section' && design.value.sections.length) {
      styleConfigNotice.value = buildStyleConfigNotice(design.value.sections[0])
    }
    dirty.value = false
  } catch (error) {
    if (loadVersion.value !== currentVersion) {
      return
    }
    loadError.value = error?.message || '模板设计器加载失败，请稍后重试'
    design.value = createEmptyDesign()
    dirty.value = false
  } finally {
    if (loadVersion.value === currentVersion) {
      loading.value = false
    }
  }
}

function buildPayload() {
  syncSortOrders()
  return buildTemplateDesignPayload(design.value)
}

async function saveDraft() {
  if (!design.value.id) {
    ElMessage.warning('请先从模板中心选择一个模板进入设计器')
    return
  }

  const validationMessage = validateTemplateDesign(design.value)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  saving.value = true
  try {
    const saved = await saveTemplateDesign(design.value.id, buildPayload())
    design.value = normalizeLoadedDesign(saved)
    dirty.value = false
    loadError.value = ''
    if (selectedPanel.value === 'section' && design.value.sections[selectedIndex.value]) {
      styleConfigNotice.value = buildStyleConfigNotice(design.value.sections[selectedIndex.value])
    }
    ElMessage.success('模板结构已保存')
  } catch (error) {
    ElMessage.error(error?.message || '模板保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

function goBackToTemplates() {
  router.push('/templates')
}

function beforeUnload(event) {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前模板有未保存修改，确定离开吗？', '未保存提醒', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
})

onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  loadDesigner()
})

watch(() => route.params.id, (newId, oldId) => {
  if (newId === oldId) {
    return
  }
  loadDesigner()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
})
</script>

<template>
  <div v-loading="loading">
    <el-result
      v-if="loadError"
      icon="error"
      title="模板设计器加载失败"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="goBackToTemplates">返回模板中心</el-button>
      </template>
    </el-result>

    <el-empty v-else-if="isEmptyState" description="请先在模板中心选择一个模板，再进入设计器。">
      <el-button type="primary" @click="goBackToTemplates">去模板中心</el-button>
    </el-empty>

    <div v-else-if="isDesignerReady" class="designer-layout">
      <el-card shadow="never" class="designer-panel left-panel">
        <template #header>
          <div class="card-header">
            <span>组件面板</span>
          </div>
        </template>
        <div class="tool-group">
          <div class="tool-title">模板信息</div>
          <el-input v-model="design.name" placeholder="模板名称" @input="markDirty" />
          <el-input v-model="design.description" type="textarea" :rows="3" placeholder="模板说明" @input="markDirty" />
        </div>
        <div class="tool-group">
          <div class="tool-title">区块组件</div>
          <el-button class="wide-btn" @click="addSection">新增区块</el-button>
          <div class="tool-tag-list">
            <el-tag v-for="item in options.sectionTypes" :key="item.value" type="info">{{ item.label }}</el-tag>
          </div>
        </div>
        <div class="tool-group">
          <div class="tool-title">餐次结构</div>
          <el-button class="wide-btn" @click="addMeal">新增餐次</el-button>
          <el-button class="wide-btn" :disabled="!selectedMeal" @click="addMealItem">给当前餐次加字段</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="designer-panel center-panel">
        <template #header>
          <div class="card-header">
            <span>拖拽画布</span>
            <div class="card-header-actions">
              <el-tag v-if="dirty" type="warning">未保存</el-tag>
              <el-button type="primary" :loading="saving" :disabled="saving" @click="saveDraft">保存</el-button>
            </div>
          </div>
        </template>
        <div class="canvas-block">
          <div class="canvas-title">页面区块</div>
          <draggable
            v-model="design.sections"
            item-key="_key"
            handle=".drag-handle"
            class="drag-list"
            ghost-class="drag-ghost"
            chosen-class="drag-chosen"
            @change="onSectionDragChange"
          >
            <template #item="{ element, index }">
              <div class="canvas-item" @click="openSection(index)">
                <div class="canvas-item-main">
                  <el-icon class="drag-handle"><Rank /></el-icon>
                  <div>
                    <div class="canvas-item-title">{{ element.title }}</div>
                    <div class="canvas-item-desc">{{ getSectionTypeLabel(element.sectionType) }}</div>
                  </div>
                </div>
                <div class="canvas-item-actions">
                  <el-tag size="small">{{ element.enabled ? '启用' : '停用' }}</el-tag>
                  <el-button link type="danger" @click.stop="removeSection(index)">删除</el-button>
                </div>
              </div>
            </template>
          </draggable>
        </div>
        <div class="canvas-block">
          <div class="canvas-title">餐次与字段</div>
          <draggable
            v-model="design.meals"
            item-key="_key"
            handle=".drag-handle"
            class="drag-list"
            ghost-class="drag-ghost"
            chosen-class="drag-chosen"
            @change="onMealDragChange"
          >
            <template #item="{ element, index }">
              <div class="meal-card" @click="openMeal(index)">
                <div class="meal-card-head">
                  <div class="canvas-item-main">
                    <el-icon class="drag-handle"><Rank /></el-icon>
                    <div>
                      <div class="canvas-item-title">{{ element.mealName }}</div>
                      <div class="canvas-item-desc">{{ element.mealCode }}</div>
                    </div>
                  </div>
                  <div class="canvas-item-actions">
                    <el-tag size="small">{{ element.items?.length || 0 }} 项</el-tag>
                    <el-button link type="danger" @click.stop="removeMeal(index)">删除</el-button>
                  </div>
                </div>
                <draggable
                  v-model="element.items"
                  item-key="_key"
                  handle=".drag-handle"
                  class="drag-sub-list"
                  ghost-class="drag-ghost"
                  chosen-class="drag-chosen"
                  @change="onMealItemDragChange(index)"
                >
                  <template #item="{ element: item, index: itemIndex }">
                    <div class="sub-item" @click.stop="openItem(index, itemIndex)">
                      <div class="canvas-item-main">
                        <el-icon class="drag-handle"><Rank /></el-icon>
                        <span>{{ item.itemName }} · {{ getContentFormatLabel(item.contentFormat) }}</span>
                      </div>
                      <div class="canvas-item-actions">
                        <el-button link type="danger" @click.stop="removeMealItem(index, itemIndex)">删除</el-button>
                      </div>
                    </div>
                  </template>
                </draggable>
              </div>
            </template>
          </draggable>
        </div>
      </el-card>

      <el-card shadow="never" class="designer-panel right-panel">
        <template #header>
          <div class="card-header">
            <span>属性配置</span>
          </div>
        </template>
        <el-alert
          title="“允许图片”已接通餐单编辑、成品展示和 AI 生图。开启后，该区块或字段会在餐单编辑页显示上传图片和 AI 生图入口。"
          type="info"
          :closable="false"
          show-icon
          class="capability-notice"
        />
        <el-form v-if="selectedPanel === 'section' && selectedSection" label-position="top">
          <el-form-item label="区块标题"><el-input v-model="selectedSection.title" @input="markDirty" /></el-form-item>
          <el-form-item label="区块类型">
            <el-select v-model="selectedSection.sectionType" @change="markDirty">
              <el-option v-for="item in options.sectionTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="展示风格">
            <div class="style-panel">
              <el-alert v-if="styleConfigNotice" :title="styleConfigNotice" type="warning" :closable="false" show-icon />
              <div class="style-toggle-list">
                <div v-for="preset in stylePresetOptions" :key="preset.key" class="style-toggle-item">
                  <span>{{ preset.label }}</span>
                  <el-switch
                    :model-value="selectedSection._styleConfigState?.config?.[preset.key] || false"
                    @change="(value) => updateSectionStyleFlag(preset.key, value)"
                  />
                </div>
              </div>
              <el-collapse>
                <el-collapse-item title="高级配置（仅查看）" name="advanced-style-json">
                  <el-input
                    :model-value="selectedSection.styleConfigJson || '未设置'"
                    type="textarea"
                    :rows="5"
                    readonly
                  />
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-form-item>
          <el-form-item label="是否启用"><el-switch v-model="selectedSection.enabled" @change="markDirty" /></el-form-item>
          <el-form-item label="允许图片">
            <el-switch v-model="selectedSection.allowImage" @change="markDirty" />
            <div class="form-help-text">开启后，餐单编辑页可为该区块上传图片或手动触发 AI 生图。</div>
          </el-form-item>
        </el-form>

        <el-form v-else-if="selectedPanel === 'meal' && selectedMeal" label-position="top">
          <el-form-item label="餐次名称"><el-input v-model="selectedMeal.mealName" @input="markDirty" /></el-form-item>
          <el-form-item label="餐次编码"><el-input v-model="selectedMeal.mealCode" @input="markDirty" /></el-form-item>
          <el-form-item label="用餐时间/时段">
            <el-input v-model="selectedMeal.timeLabel" placeholder="例如 07:30 / 上午加餐 / 训练后" @input="markDirty" />
          </el-form-item>
          <el-form-item label="是否启用"><el-switch v-model="selectedMeal.enabled" @change="markDirty" /></el-form-item>
        </el-form>

        <el-form v-else-if="selectedPanel === 'item' && selectedItem" label-position="top">
          <el-form-item label="字段名称"><el-input v-model="selectedItem.itemName" @input="markDirty" /></el-form-item>
          <el-form-item label="字段编码"><el-input v-model="selectedItem.itemCode" @input="markDirty" /></el-form-item>
          <el-form-item label="内容格式">
            <el-select v-model="selectedItem.contentFormat" @change="markDirty">
              <el-option v-for="item in options.contentFormats" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="是否启用"><el-switch v-model="selectedItem.enabled" @change="markDirty" /></el-form-item>
          <el-form-item label="允许图片">
            <el-switch v-model="selectedItem.allowImage" @change="markDirty" />
            <div class="form-help-text">开启后，该字段支持上传图片，并可按当前内容或提示词手动 AI 生图。</div>
          </el-form-item>
        </el-form>

        <el-empty v-else description="请选择左侧元素进行配置" />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.designer-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 320px;
  gap: 16px;
}

.designer-panel {
  min-height: 720px;
}

.capability-notice {
  margin-bottom: 16px;
}

.tool-group,
.canvas-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-group + .tool-group,
.canvas-block + .canvas-block {
  margin-top: 16px;
}

.tool-title,
.canvas-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.card-header,
.card-header-actions,
.meal-card-head,
.canvas-item,
.sub-item,
.style-toggle-item,
.canvas-item-main {
  display: flex;
  align-items: center;
}

.card-header,
.meal-card-head,
.canvas-item,
.sub-item,
.style-toggle-item {
  justify-content: space-between;
}

.card-header-actions,
.canvas-item-actions,
.tool-tag-list,
.style-toggle-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.drag-list,
.drag-sub-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.canvas-item,
.meal-card,
.sub-item,
.style-panel {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.canvas-item,
.sub-item,
.style-panel {
  padding: 12px;
}

.meal-card {
  padding: 12px;
}

.canvas-item-main {
  gap: 10px;
  min-width: 0;
}

.canvas-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.canvas-item-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.drag-handle {
  cursor: grab;
  color: #909399;
}

.drag-ghost {
  opacity: 0.45;
}

.drag-chosen {
  box-shadow: 0 0 0 1px #409eff inset;
}

.style-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.style-toggle-list {
  flex-direction: column;
}

.style-toggle-item {
  padding: 8px 0;
}

.wide-btn {
  width: 100%;
}

.form-help-text {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
}
</style>
