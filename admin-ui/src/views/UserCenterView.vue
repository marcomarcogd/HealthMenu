<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUser, listUsers, resetUserPassword, saveUser } from '../api/user'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const dialogVisible = ref(false)
const resetDialogVisible = ref(false)
const searchKeyword = ref('')
const users = ref([])

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '健管师', value: 'HEALTH_MANAGER' },
]

const form = reactive(createEmptyForm())
const resetForm = reactive({
  id: null,
  displayName: '',
  password: '',
})

function createEmptyForm() {
  return {
    id: null,
    username: '',
    displayName: '',
    roleCode: 'HEALTH_MANAGER',
    status: 1,
    password: '',
  }
}

const dialogTitle = computed(() => (form.id ? '编辑账号' : '新建账号'))
const filteredUsers = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return users.value
  }
  return users.value.filter((item) =>
    [item.username, item.displayName, item.roleLabel]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  )
})

function resetSaveForm() {
  Object.assign(form, createEmptyForm())
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (input) => String(input).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function openCreateDialog() {
  resetSaveForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    displayName: row.displayName,
    roleCode: row.roleCode,
    status: row.status,
    password: '',
  })
  dialogVisible.value = true
}

function openResetDialog(row) {
  Object.assign(resetForm, {
    id: row.id,
    displayName: row.displayName,
    password: '',
  })
  resetDialogVisible.value = true
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await listUsers()
  } catch (error) {
    users.value = []
    ElMessage.error(error?.message || '账号列表加载失败')
  } finally {
    loading.value = false
  }
}

async function submitSave() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.displayName.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!form.id && !form.password.trim()) {
    ElMessage.warning('新建账号时请先设置密码')
    return
  }

  saving.value = true
  try {
    await saveUser({
      id: form.id,
      username: form.username.trim(),
      displayName: form.displayName.trim(),
      roleCode: form.roleCode,
      status: form.status,
      password: form.password.trim() || undefined,
    })
    ElMessage.success(form.id ? '账号已更新' : '账号已创建')
    dialogVisible.value = false
    resetSaveForm()
    await loadUsers()
    await authStore.loadCurrentUser()
  } catch (error) {
    ElMessage.error(error?.message || '账号保存失败')
  } finally {
    saving.value = false
  }
}

async function submitResetPassword() {
  if (!resetForm.password.trim()) {
    ElMessage.warning('请输入新密码')
    return
  }

  resetting.value = true
  try {
    await resetUserPassword(resetForm.id, {
      password: resetForm.password.trim(),
    })
    ElMessage.success('密码已重置')
    resetDialogVisible.value = false
    resetForm.password = ''
  } catch (error) {
    ElMessage.error(error?.message || '密码重置失败')
  } finally {
    resetting.value = false
  }
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(`确认删除账号“${row.displayName}”吗？`, '删除账号', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteUser(row.id)
    ElMessage.success('账号已删除')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '账号删除失败')
    }
  }
}

onMounted(async () => {
  await loadUsers()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>账号管理</span>
        <div class="action-row">
          <el-input v-model="searchKeyword" placeholder="按账号、姓名、角色搜索" clearable class="user-search" />
          <el-button type="primary" @click="openCreateDialog">新建账号</el-button>
        </div>
      </div>
    </template>

    <div v-loading="loading">
      <el-table :data="filteredUsers">
        <el-table-column prop="username" label="账号" min-width="160" />
        <el-table-column prop="displayName" label="姓名" min-width="140" />
        <el-table-column prop="roleLabel" label="角色" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="openResetDialog(row)">重置密码</el-button>
            <el-button
              link
              type="danger"
              :disabled="authStore.currentUser?.id === row.id"
              @click="removeUser(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @closed="resetSaveForm">
    <el-form label-position="top">
      <el-form-item label="账号">
        <el-input v-model="form.username" placeholder="建议使用英文、数字组合" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.displayName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="角色">
        <el-radio-group v-model="form.roleCode">
          <el-radio v-for="item in roleOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="form.id ? '登录密码（留空则不修改）' : '登录密码'">
        <el-input v-model="form.password" type="password" show-password placeholder="至少 8 位" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="submitSave">保存账号</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="resetDialogVisible" title="重置密码" width="420px" @closed="resetForm.password = ''">
    <el-form label-position="top">
      <el-form-item label="账号姓名">
        <el-input :model-value="resetForm.displayName" disabled />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="resetForm.password" type="password" show-password placeholder="至少 8 位" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" :disabled="resetting" @click="submitResetPassword">确认重置</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-search {
  width: 260px;
}
</style>
