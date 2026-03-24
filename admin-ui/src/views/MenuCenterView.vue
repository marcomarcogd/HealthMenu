<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getAdminOptions } from '../api/options'
import {
  deleteMenu,
  downloadMenuExcel,
  generateMenuImage,
  getMenuDetail,
  initMenuForm,
  listMenus,
  parseMenuText,
  publishMenu,
  saveMenu,
  uploadMenuImage,
} from '../api/menu'
import {
  buildAiPreviewSummary,
  createEmptyMenuForm,
  createEmptyMenuSelector,
  hasMenuContent,
  normalizeMenuForm,
  normalizeMenuSavePayload,
} from '../utils/menu-form'
import { copyText } from '../utils/clipboard'

const route = useRoute()
const router = useRouter()
const menus = ref([])
const loading = ref(false)
const formLoading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const currentStep = ref(0)
const options = ref({ customers: [], templates: [], recordStatuses: [] })
const selector = ref(createEmptyMenuSelector())
const menuForm = ref(createEmptyMenuForm())
const dirty = ref(false)
const lastSavedLinks = ref({ viewUrl: '', shareUrl: '' })
const generatingImageTarget = ref(null)
const routeEditLoading = ref(false)
const appBaseUrl = window.location.origin
const menuFilters = ref({
  keyword: '',
  status: 'ALL',
  sort: 'menuDateDesc',
})

const canInit = computed(() => selector.value.customerId && selector.value.templateId)
const canSave = computed(() => menuForm.value.customerId && menuForm.value.templateId)
const hasStepOneContent = computed(() => menuForm.value.sections.length || menuForm.value.meals.length)
const dialogTitle = computed(() => (menuForm.value.id ? '编辑餐单' : '新建餐单'))
const menuStatusOptions = computed(() => [
  { label: '全部状态', value: 'ALL' },
  ...(options.value.recordStatuses || []),
])
const filteredMenus = computed(() => {
  const keyword = menuFilters.value.keyword.trim().toLowerCase()
  const status = menuFilters.value.status
  const sort = menuFilters.value.sort

  const matched = menus.value.filter((row) => {
    const matchesStatus = status === 'ALL' || row.status === status
    if (!matchesStatus) {
      return false
    }

    if (!keyword) {
      return true
    }

    const searchable = [
      row.title,
      row.menuDate,
      row.themeName,
      row.themeCode,
      row.statusLabel,
      row.status,
      `第${row.weekIndex || '-'}周`,
    ]

    return searchable.some((value) => String(value || '').toLowerCase().includes(keyword))
  })

  return [...matched].sort((left, right) => compareMenus(left, right, sort))
})
const menuSummary = computed(() => {
  const total = menus.value.length
  const visible = filteredMenus.value.length
  const published = filteredMenus.value.filter((item) => item.status === 'PUBLISHED').length
  const drafts = visible - published

  return { total, visible, published, drafts }
})

async function refreshMenus() {
  loading.value = true
  try {
    menus.value = await listMenus()
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  options.value = await getAdminOptions()
}

function markDirty() {
  dirty.value = true
}

function resetEditor() {
  selector.value = createEmptyMenuSelector()
  menuForm.value = createEmptyMenuForm()
  lastSavedLinks.value = { viewUrl: '', shareUrl: '' }
  currentStep.value = 0
  dirty.value = false
}

function resolveAppUrl(url) {
  if (!url) {
    return ''
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  return new URL(url, appBaseUrl).toString()
}

function openUrl(url) {
  if (!url) {
    ElMessage.warning('链接暂不可用')
    return
  }
  window.open(resolveAppUrl(url), '_blank', 'noopener')
}

async function copyUrl(url, successText = '链接已复制') {
  if (!url) {
    ElMessage.warning('链接暂不可用')
    return
  }
  try {
    await copyText(resolveAppUrl(url))
    ElMessage.success(successText)
  } catch (error) {
    ElMessage.error(error?.message || '复制失败，请手动复制链接')
  }
}

function displayStatus(row) {
  return row.statusLabel || row.status || '-'
}

function displayTheme(row) {
  return row.themeName || row.themeCode || '-'
}

function displayPublishTime(row) {
  return row.lastPublishedAt || '-'
}

function compareMenus(left, right, sort) {
  if (sort === 'titleAsc') {
    return String(left.title || '').localeCompare(String(right.title || ''), 'zh-CN')
  }

  if (sort === 'publishDesc') {
    return getSortTime(right.lastPublishedAt || right.menuDate) - getSortTime(left.lastPublishedAt || left.menuDate)
  }

  if (sort === 'menuDateAsc') {
    return getSortTime(left.menuDate) - getSortTime(right.menuDate)
  }

  return getSortTime(right.menuDate || right.lastPublishedAt) - getSortTime(left.menuDate || left.lastPublishedAt)
}

function getSortTime(value) {
  const time = new Date(value || 0).getTime()
  return Number.isFinite(time) ? time : 0
}

function canPublish(row) {
  return row?.id && row?.status !== 'PUBLISHED'
}

function resetMenuFilters() {
  menuFilters.value = {
    keyword: '',
    status: 'ALL',
    sort: 'menuDateDesc',
  }
}

function validateImageFile(file) {
  const isImage = file?.type?.startsWith('image/')
  const isLt10Mb = (file?.size || 0) <= 10 * 1024 * 1024

  if (!isImage) {
    ElMessage.error('仅支持上传图片文件')
    return false
  }
  if (!isLt10Mb) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

async function handleImageUpload(target, options) {
  if (!validateImageFile(options.file)) {
    options.onError?.(new Error('INVALID_IMAGE_FILE'))
    return
  }
  try {
    const result = await uploadMenuImage(options.file)
    target.imagePath = result.path || ''
    markDirty()
    options.onSuccess?.(result)
    ElMessage.success('图片上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error?.message || '图片上传失败')
  }
}

function uploadSectionImage(section, options) {
  return handleImageUpload(section, options)
}

function uploadMealItemImage(item, options) {
  return handleImageUpload(item, options)
}

function isGeneratingImage(target) {
  return generatingImageTarget.value === target
}

function buildImagePrompt(target, fallbackLabel) {
  const explicitPrompt = target?.aiImagePrompt?.trim()
  if (explicitPrompt) {
    return explicitPrompt
  }

  const content = 'itemValue' in target ? target.itemValue : target.content
  return [fallbackLabel, content]
    .filter((value) => value?.trim?.())
    .join('，')
    .trim()
}

async function generateTargetImage(target, fallbackLabel) {
  const prompt = buildImagePrompt(target, fallbackLabel)
  if (!prompt) {
    ElMessage.warning('请先填写 AI 生图提示词，或补充当前内容')
    return
  }

  generatingImageTarget.value = target
  target.aiImagePrompt = prompt
  try {
    const result = await generateMenuImage(prompt)
    target.imagePath = result.path || ''
    target.aiImagePrompt = result.prompt || prompt
    markDirty()
    ElMessage.success('AI 图片生成成功')
  } catch (error) {
    ElMessage.error(error?.message || 'AI 图片生成失败')
  } finally {
    if (generatingImageTarget.value === target) {
      generatingImageTarget.value = null
    }
  }
}

function generateSectionImage(section) {
  return generateTargetImage(section, section.title || section.sectionType || '餐单区块')
}

function generateMealItemAiImage(item) {
  return generateTargetImage(item, item.itemName || '餐品图片')
}

function clearImage(target) {
  target.imagePath = ''
  markDirty()
}

async function confirmDiscardChanges() {
  if (!dirty.value && !hasMenuContent(menuForm.value)) {
    return true
  }

  try {
    await ElMessageBox.confirm('当前餐单有未保存修改，确认丢弃并继续吗？', '未保存提醒', {
      type: 'warning',
      confirmButtonText: '丢弃修改',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
}

async function openCreateDialog() {
  if (!(await confirmDiscardChanges())) {
    return
  }
  resetEditor()
  editorVisible.value = true
}

async function initForm(useAi) {
  if (!canInit.value) {
    ElMessage.warning('请先选择客户和模板')
    return
  }
  formLoading.value = true
  try {
    menuForm.value = normalizeMenuForm(await initMenuForm({
      customerId: selector.value.customerId,
      templateId: selector.value.templateId,
      sourceText: selector.value.sourceText,
      useAi,
    }))
    lastSavedLinks.value = { viewUrl: '', shareUrl: '' }
    currentStep.value = 1
    dirty.value = false
    ElMessage.success(useAi ? 'AI 预填完成' : '已按模板初始化餐单')
  } catch (error) {
    ElMessage.error(error?.message || '初始化餐单失败')
  } finally {
    formLoading.value = false
  }
}

async function previewAi() {
  if (!selector.value.sourceText) {
    ElMessage.warning('请先输入 AI 文本')
    return
  }
  formLoading.value = true
  try {
    const result = await parseMenuText(selector.value.sourceText)
    const summary = buildAiPreviewSummary(result)
    if (result.parseMode === 'AI') {
      ElMessage.success(summary)
    } else {
      ElMessage.warning(summary)
    }
  } catch (error) {
    ElMessage.error(error?.message || 'AI 解析失败')
  } finally {
    formLoading.value = false
  }
}

async function editMenu(row) {
  if (!(await confirmDiscardChanges())) {
    return
  }

  formLoading.value = true
  try {
    const detail = await getMenuDetail(row.id)
    if (!detail?.id) {
      throw new Error('餐单不存在或已删除')
    }
    menuForm.value = normalizeMenuForm(detail)
    selector.value.customerId = detail.customerId
    selector.value.templateId = detail.templateId
    lastSavedLinks.value = {
      viewUrl: detail.viewUrl || row.viewUrl || '',
      shareUrl: detail.shareUrl || row.shareUrl || '',
    }
    currentStep.value = 1
    dirty.value = false
    editorVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载餐单失败')
  } finally {
    formLoading.value = false
  }
}

async function openMenuFromRoute(menuId) {
  if (!menuId || routeEditLoading.value) {
    return
  }

  routeEditLoading.value = true
  try {
    const targetRow = menus.value.find((item) => String(item.id) === String(menuId)) || { id: menuId }
    await editMenu(targetRow)
    const nextQuery = { ...route.query }
    delete nextQuery.edit
    await router.replace({ path: route.path, query: nextQuery })
  } finally {
    routeEditLoading.value = false
  }
}

function nextStep() {
  if (!hasStepOneContent.value) {
    ElMessage.warning('当前餐单还没有可编辑内容')
    return
  }
  currentStep.value = Math.min(currentStep.value + 1, 2)
}

function prevStep() {
  currentStep.value = Math.max(currentStep.value - 1, 0)
}

async function submitMenu() {
  if (!canSave.value) {
    ElMessage.warning('餐单基础信息不完整')
    return
  }
  saving.value = true
  try {
    const result = await saveMenu(normalizeMenuSavePayload(menuForm.value))
    menuForm.value.id = result.id
    const detail = normalizeMenuForm(await getMenuDetail(result.id))
    if (!detail?.id) {
      throw new Error('餐单保存成功，但重新加载详情失败')
    }
    menuForm.value = detail
    lastSavedLinks.value = {
      viewUrl: detail.viewUrl || '',
      shareUrl: detail.shareUrl || '',
    }
    await refreshMenus()
    currentStep.value = 2
    dirty.value = false
    ElMessage.success('餐单保存成功')
  } catch (error) {
    ElMessage.error(error?.message || '餐单保存失败')
  } finally {
    saving.value = false
  }
}

async function publishCurrentMenu(row) {
  try {
    await ElMessageBox.confirm(`确认发布餐单“${row.title || '未命名餐单'}”吗？`, '发布确认', {
      type: 'warning',
      confirmButtonText: '立即发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await publishMenu(row.id)
    ElMessage.success('餐单已发布')
    await refreshMenus()
    if (menuForm.value.id === row.id) {
      const detail = normalizeMenuForm(await getMenuDetail(row.id))
      if (!detail?.id) {
        throw new Error('餐单已发布，但重新加载详情失败')
      }
      menuForm.value = detail
      lastSavedLinks.value = {
        viewUrl: detail.viewUrl || '',
        shareUrl: detail.shareUrl || '',
      }
    }
  } catch (error) {
    ElMessage.error(error?.message || '发布失败')
  }
}

async function exportExcel(row) {
  try {
    await downloadMenuExcel(row.id)
    ElMessage.success('Excel 导出已开始')
  } catch (error) {
    ElMessage.error(error?.message || '导出失败')
  }
}

async function removeMenu(row) {
  try {
    await ElMessageBox.confirm(`确认删除餐单“${row.title || '未命名餐单'}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await deleteMenu(row.id)
    ElMessage.success('餐单已删除')
    await refreshMenus()
    if (menuForm.value.id === row.id) {
      editorVisible.value = false
      resetEditor()
    }
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function requestCloseEditor() {
  if (!(await confirmDiscardChanges())) {
    return
  }
  editorVisible.value = false
  resetEditor()
}

async function handleDialogClose(done) {
  if (!(await confirmDiscardChanges())) {
    return
  }
  resetEditor()
  done()
}

onMounted(async () => {
  await Promise.all([loadOptions(), refreshMenus()])
  await openMenuFromRoute(route.query.edit)
})

watch(
  () => route.query.edit,
  async (value) => {
    await openMenuFromRoute(value)
  },
)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>餐单管理</span>
        <el-button type="primary" @click="openCreateDialog">新建餐单</el-button>
      </div>
    </template>

    <div class="menu-toolbar">
      <div class="menu-toolbar__filters">
        <el-input
          v-model="menuFilters.keyword"
          clearable
          placeholder="搜索标题、日期、主题或周次"
          class="menu-toolbar__search"
        />
        <el-select v-model="menuFilters.status" class="menu-toolbar__select">
          <el-option v-for="item in menuStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="menuFilters.sort" class="menu-toolbar__select">
          <el-option label="按日期倒序" value="menuDateDesc" />
          <el-option label="按日期正序" value="menuDateAsc" />
          <el-option label="按最近发布" value="publishDesc" />
          <el-option label="按标题排序" value="titleAsc" />
        </el-select>
        <el-button plain @click="resetMenuFilters">清空筛选</el-button>
      </div>
      <div class="menu-toolbar__summary">
        <span>当前显示 {{ menuSummary.visible }} / {{ menuSummary.total }}</span>
        <span>草稿 {{ menuSummary.drafts }}</span>
        <span>已发布 {{ menuSummary.published }}</span>
      </div>
    </div>

    <el-table :data="filteredMenus" v-loading="loading" empty-text="没有符合条件的餐单">
      <el-table-column prop="title" label="餐单标题" min-width="180" />
      <el-table-column prop="menuDate" label="日期" width="140" />
      <el-table-column prop="weekIndex" label="周次" width="90" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">{{ displayStatus(row) }}</template>
      </el-table-column>
      <el-table-column label="发布次数" width="100">
        <template #default="{ row }">{{ row.publishCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="最近发布" min-width="180">
        <template #default="{ row }">{{ displayPublishTime(row) }}</template>
      </el-table-column>
      <el-table-column label="主题" min-width="140">
        <template #default="{ row }">{{ displayTheme(row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="380">
        <template #default="{ row }">
          <el-button link type="primary" @click="editMenu(row)">编辑</el-button>
          <el-button v-if="canPublish(row)" link type="warning" @click="publishCurrentMenu(row)">发布</el-button>
          <el-button link @click="exportExcel(row)">导出 Excel</el-button>
          <el-button link type="success" @click="openUrl(row.viewUrl)">查看成品</el-button>
          <el-button link @click="copyUrl(row.shareUrl, '分享链接已复制')">复制链接</el-button>
          <el-button link type="danger" @click="removeMenu(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog
    v-model="editorVisible"
    :title="dialogTitle"
    width="1100px"
    top="4vh"
    :close-on-click-modal="false"
    :before-close="handleDialogClose"
    class="menu-editor-dialog"
  >
    <div v-loading="formLoading" class="menu-editor-shell">
      <div class="dialog-header-meta">
        <el-tag v-if="dirty" type="warning">未保存</el-tag>
      </div>

      <el-steps :active="currentStep" finish-status="success" simple>
        <el-step title="选择客户与模板" />
        <el-step title="编辑餐单内容" />
        <el-step title="预览与保存" />
      </el-steps>

      <div class="step-panel" v-if="currentStep === 0">
        <el-form label-position="top">
          <el-form-item label="客户">
            <el-select v-model="selector.customerId" placeholder="请选择客户">
              <el-option v-for="item in options.customers" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模板">
            <el-select v-model="selector.templateId" placeholder="请选择模板">
              <el-option v-for="item in options.templates" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="AI 导入文本（可选）">
            <el-input
              v-model="selector.sourceText"
              type="textarea"
              :rows="8"
              placeholder="粘贴营养师文本，可先解析，再按模板初始化或 AI 预填"
            />
          </el-form-item>
          <div class="inline-actions">
            <el-button :disabled="!selector.sourceText" @click="previewAi">先解析 AI 文本</el-button>
            <el-button type="primary" :disabled="!canInit" @click="initForm(false)">按模板初始化</el-button>
            <el-button type="success" :disabled="!canInit || !selector.sourceText" @click="initForm(true)">AI 预填初始化</el-button>
          </div>
        </el-form>
      </div>

      <div class="step-panel" v-else-if="currentStep === 1">
        <el-form v-if="hasStepOneContent" label-position="top">
          <el-form-item label="标题">
            <el-input v-model="menuForm.title" @input="markDirty" />
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker v-model="menuForm.menuDate" type="date" value-format="YYYY-MM-DD" @change="markDirty" />
          </el-form-item>
          <el-form-item label="周次">
            <el-input-number v-model="menuForm.weekIndex" :min="1" @change="markDirty" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="menuForm.status" @change="markDirty">
              <el-option v-for="item in options.recordStatuses" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="主题">
            <el-input :model-value="menuForm.themeName || menuForm.themeCode || '-'" disabled />
          </el-form-item>
          <el-form-item label="显示每周提示">
            <el-switch v-model="menuForm.showWeeklyTip" @change="markDirty" />
          </el-form-item>
          <el-form-item label="显示互换指南">
            <el-switch v-model="menuForm.showSwapGuide" @change="markDirty" />
          </el-form-item>

          <div class="editor-block" v-for="(section, sectionIndex) in menuForm.sections" :key="`section-${sectionIndex}`">
            <div class="editor-title">{{ section.title || section.sectionType }}</div>
            <el-input v-model="section.content" type="textarea" :rows="3" @input="markDirty" />
            <div v-if="section.allowImage" class="menu-image-actions">
              <el-input
                v-model="section.aiImagePrompt"
                placeholder="AI 生图提示词；不填则按当前区块内容生成"
                @input="markDirty"
              />
              <el-upload
                :show-file-list="false"
                accept="image/*"
                :http-request="(uploadOptions) => uploadSectionImage(section, uploadOptions)"
              >
                <el-button plain>上传图片</el-button>
              </el-upload>
              <el-button type="success" plain :loading="isGeneratingImage(section)" @click="generateSectionImage(section)">AI 生图</el-button>
              <el-button v-if="section.imagePath" link type="danger" @click="clearImage(section)">移除图片</el-button>
              <span class="menu-image-note">支持手动上传，也支持按提示词直接生成。</span>
            </div>
            <el-image
              v-if="section.imagePath"
              :src="section.imagePath"
              :preview-src-list="[section.imagePath]"
              fit="cover"
              class="menu-upload-preview"
            />
          </div>

          <div class="editor-block" v-for="(meal, mealIndex) in menuForm.meals" :key="`meal-${mealIndex}`">
            <div class="editor-title">{{ meal.mealName }}</div>
            <el-input v-model="meal.mealTime" placeholder="餐次时间" @input="markDirty" />
            <div class="meal-item-grid">
              <div
                v-for="(item, itemIndex) in meal.items"
                :key="`item-${mealIndex}-${itemIndex}`"
                class="menu-item-editor"
              >
                <div class="mini-label">{{ item.itemName }}</div>
                <el-input v-model="item.itemValue" type="textarea" :rows="2" @input="markDirty" />
                <div v-if="item.allowImage" class="menu-image-actions">
                  <el-input
                    v-model="item.aiImagePrompt"
                    placeholder="AI 生图提示词；不填则按当前食材内容生成"
                    @input="markDirty"
                  />
                  <el-upload
                    :show-file-list="false"
                    accept="image/*"
                    :http-request="(uploadOptions) => uploadMealItemImage(item, uploadOptions)"
                  >
                    <el-button plain>上传图片</el-button>
                  </el-upload>
                  <el-button type="success" plain :loading="isGeneratingImage(item)" @click="generateMealItemAiImage(item)">AI 生图</el-button>
                  <el-button v-if="item.imagePath" link type="danger" @click="clearImage(item)">移除图片</el-button>
                </div>
                <el-image
                  v-if="item.imagePath"
                  :src="item.imagePath"
                  :preview-src-list="[item.imagePath]"
                  fit="cover"
                  class="menu-upload-preview"
                />
              </div>
            </div>
          </div>

          <div class="inline-actions">
            <el-button @click="prevStep">返回</el-button>
            <el-button v-if="menuForm.viewUrl" type="success" plain @click="openUrl(menuForm.viewUrl)">查看当前成品</el-button>
            <el-button type="primary" @click="nextStep">去预览</el-button>
          </div>
        </el-form>

        <el-empty v-else description="当前餐单暂无内容，请返回上一步重新初始化。" />
      </div>

      <div class="step-panel" v-else>
        <el-alert
          title="这里是后台预览，仅用于确认编辑内容；最终交付请使用“查看成品”或“复制链接”。"
          type="info"
          :closable="false"
          show-icon
          class="preview-tip"
        />

        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ menuForm.title }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ menuForm.menuDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="周次">第 {{ menuForm.weekIndex || '-' }} 周</el-descriptions-item>
          <el-descriptions-item label="状态">{{ menuForm.statusLabel || menuForm.status }}</el-descriptions-item>
          <el-descriptions-item label="主题">{{ menuForm.themeName || menuForm.themeCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布次数">{{ menuForm.publishCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="最近发布">{{ menuForm.lastPublishedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="preview-block" v-for="(section, sectionIndex) in menuForm.sections" :key="`preview-section-${sectionIndex}`">
          <div class="editor-title">{{ section.title }}</div>
          <div>{{ section.content || '-' }}</div>
          <el-image
            v-if="section.imagePath"
            :src="section.imagePath"
            :preview-src-list="[section.imagePath]"
            fit="cover"
            class="menu-upload-preview"
          />
        </div>

        <div class="preview-block" v-for="(meal, mealIndex) in menuForm.meals" :key="`preview-meal-${mealIndex}`">
          <div class="editor-title">{{ meal.mealName }} {{ meal.mealTime ? `· ${meal.mealTime}` : '' }}</div>
          <div v-for="(item, itemIndex) in meal.items" :key="`preview-item-${mealIndex}-${itemIndex}`" class="preview-row">
            <strong>{{ item.itemName }}：</strong>{{ item.itemValue || '-' }}
            <el-image
              v-if="item.imagePath"
              :src="item.imagePath"
              :preview-src-list="[item.imagePath]"
              fit="cover"
              class="menu-upload-preview"
            />
          </div>
        </div>

        <div v-if="lastSavedLinks.viewUrl || lastSavedLinks.shareUrl" class="saved-link-panel">
          <div class="editor-title">成品交付</div>
          <div class="inline-actions">
            <el-button v-if="canPublish(menuForm)" type="warning" plain @click="publishCurrentMenu(menuForm)">发布餐单</el-button>
            <el-button v-if="menuForm.id" plain @click="exportExcel(menuForm)">导出 Excel</el-button>
            <el-button v-if="lastSavedLinks.viewUrl" type="success" @click="openUrl(lastSavedLinks.viewUrl)">立即查看</el-button>
            <el-button v-if="lastSavedLinks.shareUrl" @click="copyUrl(lastSavedLinks.shareUrl, '分享链接已复制')">复制链接</el-button>
          </div>
        </div>

        <div class="inline-actions">
          <el-button @click="prevStep">返回编辑</el-button>
          <el-button type="primary" :loading="saving" :disabled="saving" @click="submitMenu">保存餐单</el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="requestCloseEditor">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.menu-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.menu-toolbar__filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}

.menu-toolbar__search {
  width: 280px;
  max-width: 100%;
}

.menu-toolbar__select {
  width: 160px;
}

.menu-toolbar__summary {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #6b7280;
  font-size: 13px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .menu-toolbar,
  .menu-toolbar__filters,
  .menu-toolbar__summary {
    align-items: stretch;
    flex-direction: column;
  }

  .menu-toolbar__search,
  .menu-toolbar__select {
    width: 100%;
  }
}
</style>
