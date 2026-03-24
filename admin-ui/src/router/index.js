import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/view/menu/:id', name: 'menu-view', component: () => import('../views/MenuPresentationView.vue') },
  { path: '/share/menu/:token', name: 'menu-share', component: () => import('../views/MenuPresentationView.vue') },
  {
    path: '/',
    component: () => import('../components/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'templates', name: 'templates', component: () => import('../views/TemplateCenterView.vue') },
      { path: 'customers', name: 'customers', component: () => import('../views/CustomerCenterView.vue') },
      { path: 'dicts', name: 'dicts', component: () => import('../views/DictCenterView.vue') },
      { path: 'template-designer/:id?', name: 'template-designer', component: () => import('../views/TemplateDesignerView.vue') },
      { path: 'menus', name: 'menus', component: () => import('../views/MenuCenterView.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
