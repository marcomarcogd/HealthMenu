<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { copyTemplate, deleteTemplate, previewTemplate, saveTemplate, updateTemplateStatus } from '../api/template'
import { useTemplateStore } from '../stores/template'

const router = useRouter()
const store = useTemplateStore()
const previewVisible = ref(false)
const editorVisible = ref(false)
const previewData = ref(null)
const saving = ref(false)
const form = reactive(createEmptyForm())

function createEmptyForm() {
  return {
    id: null,
    name: '',
    description: '',
    themeCode: 'standard',
    status: 1,
    isDefault: 0,
  }
}

const dialogTitle = computed(() => (form.id ? '编辑模板' : '新建模板'))

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function openCreateDialog() {
  resetForm()
  editorVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    description: row.description || '',
    themeCode: row.themeCode || 'standard',
    status: row.status ?? 1,
    isDefault: row.isDefault ?? 0,
  })
  editorVisible.value = true
}

function closeEditor() {
  editorVisible.value = false
  resetForm()
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }

  saving.value = true
  try {
    await saveTemplate({
      ...form,
      name: form.name.trim(),
      description: form.description.trim(),
      themeCode: form.themeCode.trim(),
    })
    ElMessage.success('模板已保存')
    closeEditor()
    await store.fetchTemplates()
  } catch (error) {
    ElMessage.error(error?.message || '模板保存失败')
  } finally {
    saving.value = false
  }
}

async function handleCopy(row) {
  try {
    await copyTemplate(row.id, { name: `${row.name}-副本` })
    ElMessage.success('模板已复制')
    await store.fetchTemplates()
  } catch (error) {
    ElMessage.error(error?.message || '模板复制失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除模板“${row.name}”吗？`, '删除确认', { type: 'warning' })
    await deleteTemplate(row.id)
    ElMessage.success('模板已删除')
    await store.fetchTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '模板删除失败')
    }
  }
}

async function handleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateTemplateStatus(row.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '模板已启用' : '模板已停用')
    await store.fetchTemplates()
  } catch (error) {
    ElMessage.error(error?.message || '模板状态更新失败')
  }
}

async function handlePreview(row) {
  try {
    previewData.value = await previewTemplate(row.id)
    previewVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '模板预览失败')
  }
}

onMounted(() => {
  store.fetchTemplates()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>模板中心</span>
        <el-button type="primary" @click="openCreateDialog">新建模板</el-button>
      </div>
    </template>

    <el-table :data="store.templates" v-loading="store.loading">
      <el-table-column prop="name" label="模板名称" min-width="180" />
      <el-table-column prop="description" label="说明" min-width="240" />
      <el-table-column prop="themeCode" label="主题" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <div class="action-row">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="router.push(`/template-designer/${row.id}`)">设计器</el-button>
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" @click="handleCopy(row)">复制</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="editorVisible" :title="dialogTitle" width="680px" @closed="resetForm">
    <el-form label-position="top">
      <el-form-item label="模板名称">
        <el-input v-model="form.name" placeholder="例如：减脂模板 / 孕期模板" />
      </el-form-item>
      <el-form-item label="模板说明">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="说明适用人群或场景" />
      </el-form-item>
      <el-form-item label="主题编码">
        <el-input v-model="form.themeCode" placeholder="当前仍为自由输入" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeEditor">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="submit">保存模板</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="previewVisible" title="模板预览" width="720px">
    <template v-if="previewData">
      <div class="preview-title">{{ previewData.previewTitle }}</div>
      <div class="preview-block">
        <strong>区块</strong>
        <ul>
          <li v-for="section in previewData.template.sections" :key="section.id || section.sectionType">
            {{ section.title || section.sectionType }}
          </li>
        </ul>
      </div>
      <div class="preview-block">
        <strong>餐次</strong>
        <ul>
          <li v-for="meal in previewData.template.meals" :key="meal.id || meal.mealCode">
            {{ meal.mealName }}（{{ meal.items.length }} 项）
          </li>
        </ul>
      </div>
    </template>
  </el-dialog>
</template>
