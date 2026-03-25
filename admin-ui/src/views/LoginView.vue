<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

function resolveRedirect() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }
  return '/dashboard'
}

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    await authStore.signIn({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    await router.replace(resolveRedirect())
  } catch (error) {
    ElMessage.error(error?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-header">
        <div class="login-title">康服到健管师餐单系统</div>
        <div class="login-subtitle">登录后可管理客户、模板、餐单和后台账号</div>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="login-btn" type="primary" :loading="loading" :disabled="loading" @click="submit">
          登录后台
        </el-button>
      </el-form>

      <div class="login-tip">
        已发布餐单无需登录即可查看，未发布内容仅后台登录后可预览。
      </div>
    </div>
  </div>
</template>
