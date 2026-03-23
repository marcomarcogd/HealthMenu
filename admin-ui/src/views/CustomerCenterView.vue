<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listDictItems, listDictTypes } from '../api/dict'
import { getAdminOptions } from '../api/options'
import { saveCustomer } from '../api/customer'

const customers = ref([])
const genderOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const searchKeyword = ref('')
const form = reactive(createEmptyForm())

function createEmptyForm() {
  return {
    id: null,
    name: '',
    nickname: '',
    gender: '',
    phone: '',
    exclusiveTitle: '',
    note: '',
    status: 1,
  }
}

const hasCustomers = computed(() => customers.value.length > 0)
const dialogTitle = computed(() => (form.id ? '编辑客户' : '新建客户'))
const filteredCustomers = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return customers.value
  }
  return customers.value.filter((item) =>
    [item.label, item.nickname, item.phone, item.exclusiveTitle]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  )
})

function createFallbackGenderOptions() {
  return [
    { label: '女', value: '女' },
    { label: '男', value: '男' },
  ]
}

function normalizeDictOptions(items) {
  return (items || [])
    .filter((item) => item.status !== 0)
    .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
    .map((item) => ({
      label: item.itemLabel || item.itemValue || item.itemCode,
      value: item.itemValue || item.itemCode,
    }))
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, {
    id: row.value,
    name: row.label || '',
    nickname: row.nickname || '',
    gender: row.gender || '',
    phone: row.phone || '',
    exclusiveTitle: row.exclusiveTitle || '',
    note: row.note || '',
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  resetForm()
}

async function loadCustomers() {
  loading.value = true
  try {
    const data = await getAdminOptions()
    customers.value = data.customers || []
  } catch (error) {
    customers.value = []
    ElMessage.error(error?.message || '客户列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadGenderOptions() {
  try {
    const types = await listDictTypes()
    const genderType = (types || []).find((item) => item.typeCode === 'gender')
    if (!genderType?.id) {
      genderOptions.value = createFallbackGenderOptions()
      return
    }
    const items = await listDictItems(genderType.id)
    genderOptions.value = normalizeDictOptions(items)
    if (!genderOptions.value.length) {
      genderOptions.value = createFallbackGenderOptions()
    }
  } catch {
    genderOptions.value = createFallbackGenderOptions()
  }
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入客户名称')
    return
  }

  saving.value = true
  try {
    await saveCustomer({
      ...form,
      name: form.name.trim(),
      nickname: form.nickname.trim(),
      gender: (form.gender || '').trim(),
      phone: form.phone.trim(),
      exclusiveTitle: form.exclusiveTitle.trim(),
      note: form.note.trim(),
    })
    ElMessage.success(form.id ? '客户已更新' : '客户已创建')
    closeDialog()
    await loadCustomers()
  } catch (error) {
    ElMessage.error(error?.message || '客户保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadGenderOptions()])
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>客户管理</span>
        <div class="action-row">
          <el-input v-model="searchKeyword" placeholder="按姓名、昵称、手机号搜索" clearable class="customer-search" />
          <el-button type="primary" @click="openCreateDialog">新建客户</el-button>
        </div>
      </div>
    </template>

    <div v-loading="loading">
      <el-table v-if="hasCustomers" :data="filteredCustomers">
        <el-table-column prop="label" label="客户名称" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="性别" width="100">
          <template #default="{ row }">{{ row.gender || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="150" />
        <el-table-column prop="exclusiveTitle" label="专属标题" min-width="220" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无客户数据，可先创建客户。" />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" @closed="resetForm">
    <el-form label-position="top">
      <el-form-item label="客户名称">
        <el-input v-model="form.name" placeholder="请输入客户名称" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" placeholder="选填" />
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="form.gender" clearable filterable placeholder="优先从字典选择">
          <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="专属标题">
        <el-input v-model="form.exclusiveTitle" placeholder="如：小王的专属餐单" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.note" type="textarea" :rows="4" placeholder="选填" />
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
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="submit">保存客户</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.customer-search {
  width: 260px;
}
</style>
