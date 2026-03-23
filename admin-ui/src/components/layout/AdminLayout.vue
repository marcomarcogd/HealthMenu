<script setup>
import { Calendar, DocumentCopy, Grid, Setting, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menus = [
  { index: '/dashboard', label: '工作台', icon: Grid },
  { index: '/templates', label: '模板中心', icon: DocumentCopy },
  { index: '/customers', label: '客户管理', icon: User },
  { index: '/dicts', label: '字典中心', icon: Setting },
  { index: '/menus', label: '餐单管理', icon: Calendar },
]

const activeMenu = () => {
  if (route.path.startsWith('/template-designer')) {
    return '/templates'
  }
  return route.path
}

const go = (index) => {
  router.push(index)
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
          <div class="topbar-subtitle">康服到健管师餐单系统</div>
        </div>
      </header>
      <section class="page-body">
        <router-view />
      </section>
    </main>
  </div>
</template>
