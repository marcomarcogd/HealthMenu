<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminOptions } from '../api/options'
import { saveCustomer } from '../api/customer'

const customers = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
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
      gender: form.gender.trim(),
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

onMounted(() => {
  loadCustomers()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>客户管理</span>
        <el-button type="primary" @click="openCreateDialog">新建客户</el-button>
      </div>
    </template>

    <div v-loading="loading">
      <el-table v-if="hasCustomers" :data="customers">
        <el-table-column prop="label" label="客户名称" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="gender" label="性别" width="100" />
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
        <el-input v-model="form.gender" placeholder="选填" />
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
