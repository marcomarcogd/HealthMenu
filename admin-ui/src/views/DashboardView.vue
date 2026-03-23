<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminOptions } from '../api/options'
import { listDictTypes } from '../api/dict'
import { listMenus } from '../api/menu'
import { listTemplates } from '../api/template'

const router = useRouter()
const loading = ref(false)
const dashboard = ref(createEmptyDashboard())

function createEmptyDashboard() {
  return {
    customers: [],
    templates: [],
    dictTypes: [],
    menus: [],
  }
}

const stats = computed(() => {
  const templates = dashboard.value.templates || []
  const menus = dashboard.value.menus || []
  const customers = dashboard.value.customers || []
  const dictTypes = dashboard.value.dictTypes || []

  return [
    {
      label: '客户数',
      value: customers.length,
      hint: customers.length ? `已录入 ${customers.length} 位客户` : '还没有客户档案',
    },
    {
      label: '启用模板',
      value: templates.filter((item) => item.status === 1).length,
      hint: `模板总数 ${templates.length}`,
    },
    {
      label: '餐单总数',
      value: menus.length,
      hint: `已发布 ${menus.filter((item) => item.status === 'PUBLISHED').length} 份`,
    },
    {
      label: '字典类型',
      value: dictTypes.length,
      hint: dictTypes.length ? '可维护主题、餐次和枚举项' : '还没有字典类型',
    },
  ]
})

const todoItems = computed(() => {
  const templates = dashboard.value.templates || []
  const menus = dashboard.value.menus || []
  const customers = dashboard.value.customers || []
  const draftMenus = menus.filter((item) => item.status !== 'PUBLISHED')
  const disabledTemplates = templates.filter((item) => item.status !== 1)
  const missingCustomerTitle = customers.filter((item) => !item.exclusiveTitle)

  return [
    {
      title: '待发布餐单',
      count: draftMenus.length,
      description: draftMenus.length ? '还有草稿未正式发布，可继续编辑或直接发布。' : '当前没有待发布餐单。',
      actionLabel: '去餐单中心',
      action: () => router.push('/menus'),
    },
    {
      title: '停用模板',
      count: disabledTemplates.length,
      description: disabledTemplates.length ? '部分模板处于停用状态，发布前建议先确认是否仍需保留。' : '所有模板都处于启用状态。',
      actionLabel: '去模板中心',
      action: () => router.push('/templates'),
    },
    {
      title: '未补标题客户',
      count: missingCustomerTitle.length,
      description: missingCustomerTitle.length ? '部分客户缺少专属标题，成品页展示会比较普通。' : '客户专属标题都已补齐。',
      actionLabel: '去客户中心',
      action: () => router.push('/customers'),
    },
  ]
})

const recentMenus = computed(() => {
  return [...(dashboard.value.menus || [])]
    .sort((left, right) => {
      const rightTime = new Date(right.lastPublishedAt || right.menuDate || 0).getTime()
      const leftTime = new Date(left.lastPublishedAt || left.menuDate || 0).getTime()
      return rightTime - leftTime
    })
    .slice(0, 6)
})

const quickActions = [
  {
    title: '新建餐单',
    description: '直接进入餐单中心开始初始化、编辑和发布。',
    action: () => router.push('/menus'),
  },
  {
    title: '新增客户',
    description: '先建客户档案，后续餐单和分享页会直接复用。',
    action: () => router.push('/customers'),
  },
  {
    title: '管理模板',
    description: '进入模板中心维护模板，或跳到设计器继续调整结构。',
    action: () => router.push('/templates'),
  },
  {
    title: '维护字典',
    description: '高频选项放到字典中心，减少自由输入和脏数据。',
    action: () => router.push('/dicts'),
  },
]

async function loadDashboard() {
  loading.value = true

  try {
    const [options, templates, dictTypes, menus] = await Promise.all([
      getAdminOptions(),
      listTemplates(),
      listDictTypes(),
      listMenus(),
    ])

    dashboard.value = {
      customers: options?.customers || [],
      templates: templates || [],
      dictTypes: dictTypes || [],
      menus: menus || [],
    }
  } catch (error) {
    dashboard.value = createEmptyDashboard()
    ElMessage.error(error?.message || '工作台加载失败')
  } finally {
    loading.value = false
  }
}

function openMenu(row) {
  if (!row?.id) {
    return
  }

  router.push({ path: '/menus', query: { edit: row.id } })
}

function statusTagType(status) {
  return status === 'PUBLISHED' ? 'success' : 'info'
}

function statusText(row) {
  return row?.statusLabel || row?.status || '-'
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="page-grid two-col dashboard-grid" v-loading="loading">
    <el-card class="dashboard-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>工作台概览</span>
          <el-button link type="primary" @click="loadDashboard">刷新</el-button>
        </div>
      </template>

      <div class="dashboard-stat-grid">
        <div v-for="item in stats" :key="item.label" class="dashboard-stat-card">
          <div class="dashboard-stat-label">{{ item.label }}</div>
          <div class="dashboard-stat-value">{{ item.value }}</div>
          <div class="dashboard-stat-hint">{{ item.hint }}</div>
        </div>
      </div>

      <div class="dashboard-section">
        <div class="dashboard-section-title">快捷入口</div>
        <div class="dashboard-quick-grid">
          <button
            v-for="item in quickActions"
            :key="item.title"
            type="button"
            class="dashboard-quick-card"
            @click="item.action"
          >
            <span class="dashboard-quick-title">{{ item.title }}</span>
            <span class="dashboard-quick-desc">{{ item.description }}</span>
          </button>
        </div>
      </div>

      <div class="dashboard-section">
        <div class="dashboard-section-title">待处理提醒</div>
        <div class="dashboard-todo-list">
          <div v-for="item in todoItems" :key="item.title" class="dashboard-todo-item">
            <div class="dashboard-todo-main">
              <div class="dashboard-todo-title">
                <span>{{ item.title }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="dashboard-todo-desc">{{ item.description }}</div>
            </div>
            <el-button text type="primary" @click="item.action">{{ item.actionLabel }}</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="dashboard-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>最近餐单</span>
          <el-button link type="primary" @click="router.push('/menus')">查看全部</el-button>
        </div>
      </template>

      <div v-if="recentMenus.length" class="dashboard-menu-list">
        <button
          v-for="row in recentMenus"
          :key="row.id"
          type="button"
          class="dashboard-menu-item"
          @click="openMenu(row)"
        >
          <div class="dashboard-menu-top">
            <div class="dashboard-menu-title">{{ row.title || '未命名餐单' }}</div>
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusText(row) }}</el-tag>
          </div>
          <div class="dashboard-menu-meta">
            <span>{{ row.menuDate || '未设置日期' }}</span>
            <span>{{ row.themeName || row.themeCode || '未设置主题' }}</span>
          </div>
          <div class="dashboard-menu-meta">
            <span>发布次数 {{ row.publishCount || 0 }}</span>
            <span>{{ row.lastPublishedAt || '未发布' }}</span>
          </div>
        </button>
      </div>
      <el-empty v-else description="还没有餐单，先去餐单中心新建一份。" />
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-grid {
  align-items: start;
}

.dashboard-card {
  min-height: 100%;
}

.dashboard-section {
  margin-top: 28px;
}

.dashboard-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 14px;
}

.dashboard-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-stat-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 18px;
  background: linear-gradient(180deg, #fcfcfd 0%, #f8fafc 100%);
}

.dashboard-stat-label {
  font-size: 13px;
  color: #6b7280;
}

.dashboard-stat-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
}

.dashboard-stat-hint {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.5;
  color: #4b5563;
}

.dashboard-quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-quick-card {
  border: 1px solid #dbe4ee;
  background: #ffffff;
  border-radius: 16px;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.dashboard-quick-card:hover {
  border-color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.08);
}

.dashboard-quick-title {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.dashboard-quick-desc {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #6b7280;
}

.dashboard-todo-list {
  display: grid;
  gap: 12px;
}

.dashboard-todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.dashboard-todo-main {
  min-width: 0;
}

.dashboard-todo-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  color: #111827;
}

.dashboard-todo-title strong {
  font-size: 22px;
  line-height: 1;
}

.dashboard-todo-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #6b7280;
}

.dashboard-menu-list {
  display: grid;
  gap: 12px;
}

.dashboard-menu-item {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.dashboard-menu-item:hover {
  border-color: #409eff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.dashboard-menu-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-menu-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.dashboard-menu-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #6b7280;
}

@media (max-width: 960px) {
  .dashboard-stat-grid,
  .dashboard-quick-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-todo-item,
  .dashboard-menu-top,
  .dashboard-menu-meta {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
