<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteRole, listRolePermissions, listRoles, saveRole } from '../api/role'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const roles = ref([])
const permissionOptions = ref([])

const form = reactive(createEmptyForm())

function createEmptyForm() {
  return {
    id: null,
    roleCode: '',
    roleName: '',
    permissionCodes: [],
    isSystem: 0,
  }
}

const dialogTitle = computed(() => (form.id ? '编辑角色' : '新建角色'))
const isAdminRole = computed(() => form.roleCode.trim().toUpperCase() === 'ADMIN')
const sortedPermissionOptions = computed(() =>
  [...permissionOptions.value].sort((left, right) => left.label.localeCompare(right.label, 'zh-CN')),
)
const requiredAdminPermissions = ['USER_MANAGE', 'ROLE_MANAGE']

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(role) {
  Object.assign(form, {
    id: role.id,
    roleCode: role.roleCode,
    roleName: role.roleName,
    permissionCodes: [...(role.permissionCodes || [])],
    isSystem: role.isSystem || 0,
  })
  dialogVisible.value = true
}

function isPermissionDisabled(permissionCode) {
  return isAdminRole.value && requiredAdminPermissions.includes(permissionCode)
}

function handlePermissionChange(nextPermissions) {
  if (!isAdminRole.value) {
    return
  }
  const mergedPermissions = [...new Set([...nextPermissions, ...requiredAdminPermissions])]
  if (mergedPermissions.length !== nextPermissions.length) {
    form.permissionCodes = mergedPermissions
    ElMessage.warning('管理员角色必须保留账号管理和角色权限管理')
  }
}

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await listRoles()
  } catch (error) {
    roles.value = []
    ElMessage.error(error?.message || '角色列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadPermissionOptions() {
  try {
    permissionOptions.value = await listRolePermissions()
  } catch (error) {
    permissionOptions.value = []
    ElMessage.error(error?.message || '权限列表加载失败')
  }
}

async function submit() {
  if (!form.roleCode.trim()) {
    ElMessage.warning('请输入角色编码')
    return
  }
  if (!form.roleName.trim()) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (!form.permissionCodes.length) {
    ElMessage.warning('请至少选择一个权限')
    return
  }

  saving.value = true
  try {
    await saveRole({
      id: form.id,
      roleCode: form.roleCode.trim(),
      roleName: form.roleName.trim(),
      permissionCodes: form.permissionCodes,
    })
    ElMessage.success(form.id ? '角色已更新' : '角色已创建')
    dialogVisible.value = false
    resetForm()
    await Promise.all([loadRoles(), authStore.loadCurrentUser()])
  } catch (error) {
    ElMessage.error(error?.message || '角色保存失败')
  } finally {
    saving.value = false
  }
}

async function removeRole(role) {
  try {
    await ElMessageBox.confirm(`确认删除角色“${role.roleName}”吗？`, '删除角色', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteRole(role.id)
    ElMessage.success('角色已删除')
    await Promise.all([loadRoles(), authStore.loadCurrentUser()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '角色删除失败')
    }
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadPermissionOptions()])
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>角色权限</span>
        <div class="action-row">
          <el-button type="primary" @click="openCreateDialog">新建角色</el-button>
        </div>
      </div>
    </template>

    <div v-loading="loading">
      <el-table :data="roles">
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleCode" label="角色编码" min-width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isSystem === 1 ? 'success' : 'info'">{{ row.isSystem === 1 ? '系统内置' : '自定义' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限范围" min-width="320">
          <template #default="{ row }">{{ (row.permissionLabels || []).join('、') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.isSystem === 1" @click="removeRole(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" @closed="resetForm">
    <el-form label-position="top">
      <el-form-item label="角色编码">
        <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="建议使用大写字母、数字和下划线，例如 CONTENT_EDITOR" />
        <div class="role-note">角色编码创建后不支持修改，系统内置角色也可以调整权限，但不能删除。</div>
      </el-form-item>
      <el-form-item label="角色名称">
        <el-input v-model="form.roleName" placeholder="请输入角色名称" />
      </el-form-item>
      <el-form-item label="权限配置">
        <el-checkbox-group v-model="form.permissionCodes" class="permission-grid" @change="handlePermissionChange">
          <el-checkbox
            v-for="item in sortedPermissionOptions"
            :key="item.code"
            :value="item.code"
            :disabled="isPermissionDisabled(item.code)"
          >
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
        <div v-if="isAdminRole" class="role-note">管理员角色必须保留“账号管理”和“角色权限管理”，避免后台失去维护入口。</div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="submit">保存角色</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.permission-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.role-note {
  margin-top: 8px;
  font-size: 13px;
  color: #6a7890;
}
</style>
