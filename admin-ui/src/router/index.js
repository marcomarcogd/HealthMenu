import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../components/layout/AdminLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import TemplateCenterView from '../views/TemplateCenterView.vue'
import DictCenterView from '../views/DictCenterView.vue'
import TemplateDesignerView from '../views/TemplateDesignerView.vue'
import MenuCenterView from '../views/MenuCenterView.vue'
import CustomerCenterView from '../views/CustomerCenterView.vue'
import MenuPresentationView from '../views/MenuPresentationView.vue'

const routes = [
  { path: '/view/menu/:id', name: 'menu-view', component: MenuPresentationView },
  { path: '/share/menu/:token', name: 'menu-share', component: MenuPresentationView },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: DashboardView },
      { path: 'templates', name: 'templates', component: TemplateCenterView },
      { path: 'customers', name: 'customers', component: CustomerCenterView },
      { path: 'dicts', name: 'dicts', component: DictCenterView },
      { path: 'template-designer/:id?', name: 'template-designer', component: TemplateDesignerView },
      { path: 'menus', name: 'menus', component: MenuCenterView },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
