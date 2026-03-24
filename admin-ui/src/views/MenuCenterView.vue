<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getAdminOptions } from '../api/options'
import {
  deleteMenu,
  downloadMenuExcel,
  downloadMenusExcel,
  generateMenuImage,
  getMenuDetail,
  initMenuForm,
  listMenus,
  parseMenuText,
  publishMenu,
  publishMenus,
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

const MENU_EDITOR_DRAFT_NAMESPACE = 'healthmenu.menu-editor-draft'
const MENU_EDITOR_DRAFT_INDEX_KEY = `${MENU_EDITOR_DRAFT_NAMESPACE}:index`

const route = useRoute()
const router = useRouter()
const menus = ref([])
const selectedMenuIds = ref([])
const selectedMenuRows = ref([])
const loading = ref(false)
const formLoading = ref(false)
const saving = ref(false)
const batchActionLoading = ref(false)
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
const autosaveHandle = ref(null)
const activeDraftKey = ref('')
const recoverableDrafts = ref([])
const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0,
})
const menuFilters = ref({
  keyword: '',
  customerId: '',
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
const customerFilterOptions = computed(() => [
  { label: '全部客户', value: '' },
  ...(options.value.customers || []).map((item) => ({
    label: item.label,
    value: item.value,
  })),
])
const hasSelectedMenus = computed(() => selectedMenuIds.value.length > 0)
const selectedPublishableIds = computed(() => selectedMenuRows.value
  .filter((item) => canPublish(item))
  .map((item) => item.id))
const menuSummary = computed(() => {
  const total = pagination.value.total
  const visible = menus.value.length
  const published = menus.value.filter((item) => item.status === 'PUBLISHED').length
  const drafts = visible - published

  return { total, visible, published, drafts }
})
const draftAlertTitle = computed(() => {
  const count = recoverableDrafts.value.length
  if (!count) {
    return ''
  }
  return count === 1 ? '检测到 1 份未保存草稿' : `检测到 ${count} 份未保存草稿`
})

async function refreshMenus(targetPage = pagination.value.page) {
  loading.value = true
  try {
    const result = await listMenus({
      keyword: menuFilters.value.keyword.trim() || undefined,
      customerId: menuFilters.value.customerId || undefined,
      status: menuFilters.value.status === 'ALL' ? undefined : menuFilters.value.status,
      sort: menuFilters.value.sort,
      page: targetPage,
      pageSize: pagination.value.pageSize,
    })

    const safeTotal = Number(result?.total || 0)
    const maxPage = Math.max(1, Math.ceil(safeTotal / pagination.value.pageSize))
    if (safeTotal > 0 && targetPage > maxPage) {
      await refreshMenus(maxPage)
      return
    }

    menus.value = result?.records || []
    selectedMenuIds.value = []
    selectedMenuRows.value = []
    pagination.value = {
      ...pagination.value,
      page: Number(result?.page || targetPage || 1),
      total: safeTotal,
      pageSize: Number(result?.pageSize || pagination.value.pageSize),
    }
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
  clearEditorDraft()
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

function canPublish(row) {
  return row?.id && row?.status !== 'PUBLISHED'
}

function resetMenuFilters() {
  menuFilters.value = {
    keyword: '',
    customerId: '',
    status: 'ALL',
    sort: 'menuDateDesc',
  }
  pagination.value.page = 1
  refreshMenus(1)
}

function applyMenuFilters() {
  refreshMenus(1)
}

function handlePageChange(page) {
  refreshMenus(page)
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  refreshMenus(1)
}

function handleSelectionChange(rows = []) {
  selectedMenuRows.value = rows
  selectedMenuIds.value = rows.map((item) => item.id).filter(Boolean)
}

function createDraftStorageKey() {
  return `${MENU_EDITOR_DRAFT_NAMESPACE}:new:${Date.now()}`
}

function resolveCurrentDraftKey({ createIfMissing = true } = {}) {
  if (menuForm.value.id) {
    activeDraftKey.value = `${MENU_EDITOR_DRAFT_NAMESPACE}:${menuForm.value.id}`
    return activeDraftKey.value
  }
  if (!activeDraftKey.value && createIfMissing) {
    activeDraftKey.value = createDraftStorageKey()
  }
  return activeDraftKey.value
}

function buildDraftPayload() {
  const storageKey = resolveCurrentDraftKey()
  return {
    storageKey,
    menuId: menuForm.value.id || null,
    selector: selector.value,
    menuForm: menuForm.value,
    lastSavedLinks: lastSavedLinks.value,
    currentStep: currentStep.value,
    dirty: dirty.value,
    savedAt: new Date().toISOString(),
  }
}

function readDraftIndex() {
  const raw = window.localStorage.getItem(MENU_EDITOR_DRAFT_INDEX_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    window.localStorage.removeItem(MENU_EDITOR_DRAFT_INDEX_KEY)
    return []
  }
}

function writeDraftIndex(entries) {
  if (!entries.length) {
    window.localStorage.removeItem(MENU_EDITOR_DRAFT_INDEX_KEY)
    return
  }
  window.localStorage.setItem(MENU_EDITOR_DRAFT_INDEX_KEY, JSON.stringify(entries))
}

function syncRecoverableDrafts() {
  const entries = readDraftIndex()
    .map((entry) => {
      const storageKey = entry?.storageKey
      if (!storageKey) {
        return null
      }
      const raw = window.localStorage.getItem(storageKey)
      if (!raw) {
        return null
      }

      try {
        const parsed = JSON.parse(raw)
        return {
          storageKey,
          menuId: parsed?.menuId || entry?.menuId || null,
          title: parsed?.menuForm?.title || entry?.title || '未命名草稿',
          savedAt: parsed?.savedAt || entry?.savedAt || '',
        }
      } catch {
        window.localStorage.removeItem(storageKey)
        return null
      }
    })
    .filter(Boolean)
    .sort((a, b) => new Date(b.savedAt || 0).getTime() - new Date(a.savedAt || 0).getTime())

  writeDraftIndex(entries)
  recoverableDrafts.value = entries
}

function saveEditorDraft() {
  if (!editorVisible.value || !hasMenuContent(menuForm.value)) {
    return
  }
  if (menuForm.value.id && !dirty.value) {
    return
  }

  const payload = buildDraftPayload()
  window.localStorage.setItem(payload.storageKey, JSON.stringify(payload))
  const nextEntries = readDraftIndex()
    .filter((entry) => entry?.storageKey !== payload.storageKey)
  nextEntries.unshift({
    storageKey: payload.storageKey,
    menuId: payload.menuId,
    title: payload.menuForm?.title || '未命名草稿',
    savedAt: payload.savedAt,
  })
  writeDraftIndex(nextEntries)
  syncRecoverableDrafts()
}

function scheduleDraftSave() {
  if (autosaveHandle.value) {
    window.clearTimeout(autosaveHandle.value)
  }

  autosaveHandle.value = window.setTimeout(() => {
    saveEditorDraft()
    autosaveHandle.value = null
  }, 500)
}

function clearEditorDraft(storageKeyOverride = '') {
  const storageKey = storageKeyOverride || resolveCurrentDraftKey({ createIfMissing: false })
  if (autosaveHandle.value) {
    window.clearTimeout(autosaveHandle.value)
    autosaveHandle.value = null
  }
  if (storageKey) {
    window.localStorage.removeItem(storageKey)
    writeDraftIndex(readDraftIndex().filter((entry) => entry?.storageKey !== storageKey))
  }
  activeDraftKey.value = ''
  syncRecoverableDrafts()
}

function loadRecoverableDrafts() {
  syncRecoverableDrafts()
}

function formatDraftTime(value) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return date.toLocaleString('zh-CN', {
    hour12: false,
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function restoreEditorDraft(draft) {
  if (!(await confirmDiscardChanges())) {
    return
  }

  const storageKey = draft?.storageKey
  if (!storageKey) {
    syncRecoverableDrafts()
    return
  }
  const currentStorageKey = resolveCurrentDraftKey({ createIfMissing: false })
  if (currentStorageKey && currentStorageKey !== storageKey) {
    clearEditorDraft(currentStorageKey)
  }

  const raw = window.localStorage.getItem(storageKey)
  if (!raw) {
    dismissEditorDraft(draft)
    return
  }

  try {
    const parsed = JSON.parse(raw)
    selector.value = {
      ...createEmptyMenuSelector(),
      ...(parsed?.selector || {}),
    }
    menuForm.value = normalizeMenuForm(parsed?.menuForm || {})
    lastSavedLinks.value = {
      viewUrl: parsed?.lastSavedLinks?.viewUrl || '',
      shareUrl: parsed?.lastSavedLinks?.shareUrl || '',
    }
    currentStep.value = Math.min(Math.max(Number(parsed?.currentStep || 0), 0), 2)
    dirty.value = parsed?.dirty !== false || hasMenuContent(parsed?.menuForm)
    activeDraftKey.value = storageKey
    editorVisible.value = true
    syncRecoverableDrafts()
    ElMessage.success('已恢复上次未保存草稿')
  } catch {
    dismissEditorDraft(draft)
    ElMessage.error('草稿恢复失败，已清除损坏数据')
  }
}

function dismissEditorDraft(draft) {
  const storageKey = draft?.storageKey || resolveCurrentDraftKey({ createIfMissing: false })
  if (!storageKey) {
    return
  }
  window.localStorage.removeItem(storageKey)
  writeDraftIndex(readDraftIndex().filter((entry) => entry?.storageKey !== storageKey))
  if (activeDraftKey.value === storageKey) {
    activeDraftKey.value = ''
  }
  syncRecoverableDrafts()
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
  clearEditorDraft()
  selector.value = createEmptyMenuSelector()
  menuForm.value = createEmptyMenuForm()
  activeDraftKey.value = createDraftStorageKey()
  lastSavedLinks.value = { viewUrl: '', shareUrl: '' }
  currentStep.value = 0
  dirty.value = false
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
  clearEditorDraft()

  formLoading.value = true
  try {
    const detail = await getMenuDetail(row.id)
    if (!detail?.id) {
      throw new Error('餐单不存在或已删除')
    }
    menuForm.value = normalizeMenuForm(detail)
    activeDraftKey.value = `${MENU_EDITOR_DRAFT_NAMESPACE}:${detail.id}`
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

async function refreshActiveMenuDetail(menuIds = []) {
  if (!menuForm.value.id || !menuIds.includes(menuForm.value.id)) {
    return
  }

  const detail = normalizeMenuForm(await getMenuDetail(menuForm.value.id))
  if (!detail?.id) {
    throw new Error('餐单操作成功，但重新加载详情失败')
  }
  menuForm.value = detail
  lastSavedLinks.value = {
    viewUrl: detail.viewUrl || '',
    shareUrl: detail.shareUrl || '',
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
    const currentDraftStorageKey = resolveCurrentDraftKey({ createIfMissing: false })
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
    clearEditorDraft(currentDraftStorageKey)
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
    await refreshActiveMenuDetail([row.id])
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

async function publishSelectedMenus() {
  const ids = selectedPublishableIds.value
  if (!ids.length) {
    ElMessage.warning('请选择至少一条未发布餐单')
    return
  }

  try {
    await ElMessageBox.confirm(`确认批量发布 ${ids.length} 份餐单吗？`, '批量发布确认', {
      type: 'warning',
      confirmButtonText: '立即批量发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  batchActionLoading.value = true
  try {
    await publishMenus(ids)
    ElMessage.success(`已批量发布 ${ids.length} 份餐单`)
    await refreshMenus()
    await refreshActiveMenuDetail(ids)
  } catch (error) {
    ElMessage.error(error?.message || '批量发布失败')
  } finally {
    batchActionLoading.value = false
  }
}

async function exportSelectedMenus() {
  if (!selectedMenuIds.value.length) {
    ElMessage.warning('请先选择需要导出的餐单')
    return
  }

  batchActionLoading.value = true
  try {
    await downloadMenusExcel(selectedMenuIds.value)
    ElMessage.success(`已开始导出 ${selectedMenuIds.value.length} 份餐单`)
  } catch (error) {
    ElMessage.error(error?.message || '批量导出失败')
  } finally {
    batchActionLoading.value = false
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
  loadRecoverableDrafts()
  await openMenuFromRoute(route.query.edit)
})

watch(
  () => route.query.edit,
  async (value) => {
    await openMenuFromRoute(value)
  },
)

watch(
  [editorVisible, selector, menuForm, currentStep, dirty, lastSavedLinks],
  () => {
    if (!editorVisible.value) {
      return
    }
    scheduleDraftSave()
  },
  { deep: true },
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

    <el-alert
      v-if="recoverableDrafts.length"
      type="warning"
      :closable="false"
      show-icon
      class="draft-alert"
    >
      <template #title>
        {{ draftAlertTitle }}
      </template>
      <div class="draft-alert__list">
        <div v-for="draft in recoverableDrafts" :key="draft.storageKey" class="draft-alert__item">
          <span>
            {{ draft.title }}
            <span v-if="draft.menuId"> · 餐单 #{{ draft.menuId }}</span>
            <span v-if="draft.savedAt"> · 保存于 {{ formatDraftTime(draft.savedAt) }}</span>
          </span>
          <div class="draft-alert__actions">
            <el-button type="warning" plain size="small" @click="restoreEditorDraft(draft)">恢复草稿</el-button>
            <el-button size="small" @click="dismissEditorDraft(draft)">清除草稿</el-button>
          </div>
        </div>
      </div>
    </el-alert>

    <div class="menu-toolbar">
      <div class="menu-toolbar__filters">
        <el-input
          v-model="menuFilters.keyword"
          clearable
          placeholder="搜索标题、日期、主题或周次"
          class="menu-toolbar__search"
          @keyup.enter="applyMenuFilters"
          @clear="applyMenuFilters"
        />
        <el-select v-model="menuFilters.customerId" class="menu-toolbar__select" @change="applyMenuFilters">
          <el-option v-for="item in customerFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="menuFilters.status" class="menu-toolbar__select" @change="applyMenuFilters">
          <el-option v-for="item in menuStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="menuFilters.sort" class="menu-toolbar__select" @change="applyMenuFilters">
          <el-option label="按日期倒序" value="menuDateDesc" />
          <el-option label="按日期正序" value="menuDateAsc" />
          <el-option label="按最近修改" value="updatedDesc" />
          <el-option label="按标题排序" value="titleAsc" />
        </el-select>
        <el-button type="primary" plain @click="applyMenuFilters">查询</el-button>
        <el-button plain @click="resetMenuFilters">清空筛选</el-button>
      </div>
      <div class="menu-toolbar__actions">
        <el-button
          type="warning"
          plain
          :disabled="!selectedPublishableIds.length || batchActionLoading"
          :loading="batchActionLoading"
          @click="publishSelectedMenus"
        >
          批量发布
        </el-button>
        <el-button
          plain
          :disabled="!hasSelectedMenus || batchActionLoading"
          :loading="batchActionLoading"
          @click="exportSelectedMenus"
        >
          批量导出
        </el-button>
      </div>
      <div class="menu-toolbar__summary">
        <span>当前显示 {{ menuSummary.visible }} / {{ menuSummary.total }}</span>
        <span>已选 {{ selectedMenuIds.length }}</span>
        <span>草稿 {{ menuSummary.drafts }}</span>
        <span>已发布 {{ menuSummary.published }}</span>
      </div>
    </div>

    <el-table
      :data="menus"
      v-loading="loading"
      empty-text="没有符合条件的餐单"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="customerName" label="客户" min-width="120">
        <template #default="{ row }">{{ row.customerName || '-' }}</template>
      </el-table-column>
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

    <div class="menu-pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="pagination.page"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        :total="pagination.total"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
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
.draft-alert {
  margin-bottom: 18px;
}

.draft-alert__list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.draft-alert__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.draft-alert__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

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

.menu-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

.menu-pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .menu-toolbar,
  .menu-toolbar__filters,
  .menu-toolbar__actions,
  .menu-toolbar__summary {
    align-items: stretch;
    flex-direction: column;
  }

  .menu-toolbar__search,
  .menu-toolbar__select {
    width: 100%;
  }

  .menu-pagination {
    justify-content: stretch;
  }
}
</style>
