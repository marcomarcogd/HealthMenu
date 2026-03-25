<script setup>
import { Calendar, DocumentCopy, Grid, Setting, SwitchButton, User, UserFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menus = [
  { index: '/dashboard', label: '工作台', icon: Grid },
  { index: '/templates', label: '模板中心', icon: DocumentCopy },
  { index: '/customers', label: '客户管理', icon: User },
  { index: '/dicts', label: '字典中心', icon: Setting },
  { index: '/menus', label: '餐单管理', icon: Calendar },
  { index: '/users', label: '账号管理', icon: UserFilled },
]

const currentUser = computed(() => authStore.currentUser)

const activeMenu = () => {
  if (route.path.startsWith('/template-designer')) {
    return '/templates'
  }
  return route.path
}

const go = (index) => {
  router.push(index)
}

async function handleLogout() {
  await authStore.signOut()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand-block">
        <div class="brand-title">餐单系统</div>
        <div class="brand-subtitle">健管师后台</div>
      </div>
      <el-menu :default-active="activeMenu()" class="side-menu" @select="go">
        <el-menu-item v-for="item in menus" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="admin-main">
      <header class="topbar">
        <div>
          <div class="topbar-title">康服到健管师餐单系统</div>
          <div class="topbar-subtitle">客户、模板、餐单与字典的一体化管理后台</div>
        </div>
        <div class="topbar-user">
          <div class="topbar-user-meta">
            <div class="topbar-user-name">{{ currentUser?.displayName || currentUser?.username || '未登录' }}</div>
            <div class="topbar-user-role">{{ currentUser?.roleLabel || '后台账号' }}</div>
          </div>
          <el-button text type="primary" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </header>
      <section class="page-body">
        <router-view />
      </section>
    </main>
  </div>
</template>
