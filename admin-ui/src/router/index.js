import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PERMISSIONS } from '../constants/permissions'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/view/menu/:id', name: 'menu-view', component: () => import('../views/MenuPresentationView.vue') },
  { path: '/share/menu/:token', name: 'menu-share', component: () => import('../views/MenuPresentationView.vue') },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../components/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { permission: PERMISSIONS.DASHBOARD_VIEW } },
      { path: 'templates', name: 'templates', component: () => import('../views/TemplateCenterView.vue'), meta: { permission: PERMISSIONS.TEMPLATE_MANAGE } },
      { path: 'customers', name: 'customers', component: () => import('../views/CustomerCenterView.vue'), meta: { permission: PERMISSIONS.CUSTOMER_MANAGE } },
      { path: 'dicts', name: 'dicts', component: () => import('../views/DictCenterView.vue'), meta: { permission: PERMISSIONS.DICT_MANAGE } },
      { path: 'template-designer/:id?', name: 'template-designer', component: () => import('../views/TemplateDesignerView.vue'), meta: { permission: PERMISSIONS.TEMPLATE_MANAGE } },
      { path: 'menus', name: 'menus', component: () => import('../views/MenuCenterView.vue'), meta: { permission: PERMISSIONS.MENU_MANAGE } },
      { path: 'users', name: 'users', component: () => import('../views/UserCenterView.vue'), meta: { permission: PERMISSIONS.USER_MANAGE } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth)
  const requiredPermission = to.matched.map((record) => record.meta?.permission).find(Boolean)

  if (requiresAuth) {
    const currentUser = await authStore.ensureLoaded()
    if (!currentUser) {
      return {
        name: 'login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
    if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
      ElMessage.warning('当前账号没有访问该页面的权限')
      return resolveFirstAllowedRoute(authStore)
    }
  }

  if (to.name === 'login') {
    const currentUser = await authStore.ensureLoaded()
    if (currentUser) {
      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
      return redirect
    }
  }

  return true
})

function resolveFirstAllowedRoute(authStore) {
  const fallbackRoutes = ['dashboard', 'menus', 'customers']
  for (const name of fallbackRoutes) {
    const route = router.getRoutes().find((item) => item.name === name)
    const permission = route?.meta?.permission
    if (!permission || authStore.hasPermission(permission)) {
      return { name }
    }
  }
  return { name: 'login' }
}

export default router
