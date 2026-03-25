import { createRouter, createWebHistory } from 'vue-router'
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
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'templates', name: 'templates', component: () => import('../views/TemplateCenterView.vue') },
      { path: 'customers', name: 'customers', component: () => import('../views/CustomerCenterView.vue') },
      { path: 'dicts', name: 'dicts', component: () => import('../views/DictCenterView.vue') },
      { path: 'template-designer/:id?', name: 'template-designer', component: () => import('../views/TemplateDesignerView.vue') },
      { path: 'menus', name: 'menus', component: () => import('../views/MenuCenterView.vue') },
      { path: 'users', name: 'users', component: () => import('../views/UserCenterView.vue') },
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

export default router
