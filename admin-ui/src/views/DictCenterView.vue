<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDictItem, deleteDictType, saveDictItem, saveDictType } from '../api/dict'
import { useDictStore } from '../stores/dict'

const store = useDictStore()
const typeDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const typeSaving = ref(false)
const itemSaving = ref(false)

const typeForm = reactive(createEmptyTypeForm())
const itemForm = reactive(createEmptyItemForm())

function createEmptyTypeForm() {
  return {
    id: null,
    typeCode: '',
    typeName: '',
    description: '',
    status: 1,
  }
}

function createEmptyItemForm() {
  return {
    id: null,
    dictTypeId: null,
    itemCode: '',
    itemLabel: '',
    itemValue: '',
    sortOrder: 0,
    isSystem: 0,
    status: 1,
  }
}

const activeType = computed(() => store.types.find((item) => item.id === store.activeTypeId) || null)
const typeDialogTitle = computed(() => (typeForm.id ? '编辑字典类型' : '新建字典类型'))
const itemDialogTitle = computed(() => (itemForm.id ? '编辑字典项' : '新建字典项'))

function resetTypeForm() {
  Object.assign(typeForm, createEmptyTypeForm())
}

function resetItemForm() {
  Object.assign(itemForm, {
    ...createEmptyItemForm(),
    dictTypeId: store.activeTypeId,
  })
}

async function selectType(id) {
  await store.fetchItems(id)
  resetItemForm()
}

function openCreateTypeDialog() {
  resetTypeForm()
  typeDialogVisible.value = true
}

function openEditTypeDialog(row) {
  Object.assign(typeForm, {
    id: row.id,
    typeCode: row.typeCode || '',
    typeName: row.typeName || '',
    description: row.description || '',
    status: row.status ?? 1,
  })
  typeDialogVisible.value = true
}

function closeTypeDialog() {
  typeDialogVisible.value = false
  resetTypeForm()
}

function openCreateItemDialog() {
  if (!store.activeTypeId) {
    ElMessage.warning('请先选择字典类型')
    return
  }
  resetItemForm()
  itemDialogVisible.value = true
}

function openEditItemDialog(row) {
  Object.assign(itemForm, {
    id: row.id,
    dictTypeId: row.dictTypeId,
    itemCode: row.itemCode || '',
    itemLabel: row.itemLabel || '',
    itemValue: row.itemValue || '',
    sortOrder: row.sortOrder ?? 0,
    isSystem: row.isSystem ?? 0,
    status: row.status ?? 1,
  })
  itemDialogVisible.value = true
}

function closeItemDialog() {
  itemDialogVisible.value = false
  resetItemForm()
}

async function submitType() {
  if (!typeForm.typeCode.trim() || !typeForm.typeName.trim()) {
    ElMessage.warning('请先填写类型编码和类型名称')
    return
  }
  typeSaving.value = true
  try {
    await saveDictType({
      ...typeForm,
      typeCode: typeForm.typeCode.trim(),
      typeName: typeForm.typeName.trim(),
      description: typeForm.description.trim(),
    })
    ElMessage.success('字典类型已保存')
    closeTypeDialog()
    await store.fetchTypes()
    if (store.activeTypeId) {
      await store.fetchItems(store.activeTypeId)
    }
  } catch (error) {
    ElMessage.error(error?.message || '字典类型保存失败')
  } finally {
    typeSaving.value = false
  }
}

async function submitItem() {
  if (!store.activeTypeId) {
    ElMessage.warning('请先选择字典类型')
    return
  }
  if (!itemForm.itemCode.trim() || !itemForm.itemLabel.trim()) {
    ElMessage.warning('请先填写字典项编码和名称')
    return
  }
  itemSaving.value = true
  try {
    await saveDictItem({
      ...itemForm,
      dictTypeId: store.activeTypeId,
      itemCode: itemForm.itemCode.trim(),
      itemLabel: itemForm.itemLabel.trim(),
      itemValue: itemForm.itemValue.trim(),
    })
    ElMessage.success('字典项已保存')
    closeItemDialog()
    await store.fetchItems(store.activeTypeId)
  } catch (error) {
    ElMessage.error(error?.message || '字典项保存失败')
  } finally {
    itemSaving.value = false
  }
}

async function removeType(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典类型“${row.typeName}”吗？`, '删除确认', { type: 'warning' })
    await deleteDictType(row.id)
    ElMessage.success('字典类型已删除')
    await store.fetchTypes()
    if (store.activeTypeId) {
      await store.fetchItems(store.activeTypeId)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '字典类型删除失败')
    }
  }
}

async function removeItem(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典项“${row.itemLabel}”吗？`, '删除确认', { type: 'warning' })
    await deleteDictItem(row.id)
    ElMessage.success('字典项已删除')
    await store.fetchItems(store.activeTypeId)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '字典项删除失败')
    }
  }
}

watch(() => store.activeTypeId, (value) => {
  itemForm.dictTypeId = value
})

onMounted(async () => {
  await store.fetchTypes()
  if (store.activeTypeId) {
    await store.fetchItems(store.activeTypeId)
    resetItemForm()
  }
})
</script>

<template>
  <div class="page-body">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>字典类型</span>
          <el-button type="primary" @click="openCreateTypeDialog">新建类型</el-button>
        </div>
      </template>

      <el-table :data="store.types" v-loading="store.loading">
        <el-table-column prop="typeName" label="名称" min-width="140" />
        <el-table-column prop="typeCode" label="编码" min-width="140" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <div class="action-row">
              <el-button link type="primary" @click="selectType(row.id)">查看项</el-button>
              <el-button link type="primary" @click="openEditTypeDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeType(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>字典项{{ activeType ? ` · ${activeType.typeName}` : '' }}</span>
          <el-button type="primary" :disabled="!store.activeTypeId" @click="openCreateItemDialog">新建字典项</el-button>
        </div>
      </template>

      <el-empty v-if="!store.activeTypeId" description="请先选择一个字典类型。" />
      <el-table v-else :data="store.items">
        <el-table-column prop="itemLabel" label="名称" min-width="140" />
        <el-table-column prop="itemCode" label="编码" min-width="140" />
        <el-table-column prop="itemValue" label="值" min-width="140" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="action-row">
              <el-button link type="primary" @click="openEditItemDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeItem(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="640px" @closed="resetTypeForm">
    <el-form label-position="top">
      <el-form-item label="类型编码">
        <el-input v-model="typeForm.typeCode" />
      </el-form-item>
      <el-form-item label="类型名称">
        <el-input v-model="typeForm.typeName" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="typeForm.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeTypeDialog">取消</el-button>
        <el-button type="primary" :loading="typeSaving" :disabled="typeSaving" @click="submitType">保存类型</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="640px" @closed="resetItemForm">
    <el-form label-position="top">
      <el-form-item label="字典项编码">
        <el-input v-model="itemForm.itemCode" :disabled="!store.activeTypeId" />
      </el-form-item>
      <el-form-item label="字典项名称">
        <el-input v-model="itemForm.itemLabel" :disabled="!store.activeTypeId" />
      </el-form-item>
      <el-form-item label="字典项值">
        <el-input v-model="itemForm.itemValue" :disabled="!store.activeTypeId" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="itemForm.sortOrder" :min="0" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeItemDialog">取消</el-button>
        <el-button type="primary" :loading="itemSaving" :disabled="itemSaving || !store.activeTypeId" @click="submitItem">保存字典项</el-button>
      </div>
    </template>
  </el-dialog>
</template>
