<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicMenuById, getPublicMenuByToken } from '../api/public-menu'
import { copyText } from '../utils/clipboard'
import {
  buildPresentationFontVars,
  DEFAULT_PRESENTATION_FONT_SIZE,
  normalizePresentationFontSize,
  PRESENTATION_FONT_SIZE_OPTIONS,
  resolvePresentationTitle,
  resolveSectionTitle,
} from '../utils/menu-presentation'
import {
  DEFAULT_PRESENTATION_LAYOUT,
  normalizePresentationLayout,
  PRESENTATION_LAYOUT_OPTIONS,
  resolvePresentationLayout,
} from '../utils/menu-presentation-templates'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const payload = ref(null)
const pageRef = ref(null)
const cardRef = ref(null)
const logoSrc = `${import.meta.env.BASE_URL}logo.png`
const fontSize = ref(DEFAULT_PRESENTATION_FONT_SIZE)
const layout = ref(DEFAULT_PRESENTATION_LAYOUT)
const fontSizeStorageKey = 'healthmenu-presentation-font-size'
const layoutStorageKey = 'healthmenu-presentation-layout'

let html2canvasLoader = null

const menuForm = computed(() => payload.value?.menuForm || null)
const meals = computed(() => menuForm.value?.meals || [])
const shareMode = computed(() => Boolean(payload.value?.shareMode))
const fontSizeOptions = PRESENTATION_FONT_SIZE_OPTIONS
const layoutOptions = PRESENTATION_LAYOUT_OPTIONS
const currentLayout = computed(() => resolvePresentationLayout(layout.value))
const currentLayoutCode = computed(() => currentLayout.value.value)
const title = computed(() => resolvePresentationTitle(menuForm.value || {}))
const swapGuideSection = computed(() => (menuForm.value?.sections || []).find((section) => section.sectionType === 'SWAP_GUIDE') || null)
const weeklyTipSection = computed(() => (menuForm.value?.sections || []).find((section) => section.sectionType === 'WEEKLY_TIP') || null)
const swapGuideTitle = computed(() => resolveSectionTitle(swapGuideSection.value, '核心食物互换速查指南'))
const weeklyTipTitle = computed(() => resolveSectionTitle(weeklyTipSection.value, '每周提示'))
const presentationStyleVars = computed(() => ({
  ...buildPresentationFontVars(fontSize.value),
  ...currentLayout.value.cssVars,
}))
const presentationPageClasses = computed(() => ['presentation-page', currentLayout.value.className])
const extraSections = computed(() => (menuForm.value?.sections || []).filter((section) => !['SWAP_GUIDE', 'WEEKLY_TIP', 'EXCLUSIVE_TITLE', 'DAILY_MENU'].includes(section.sectionType)))
const renderedMeals = computed(() => meals.value
  .map((meal) => ({
    ...meal,
    items: (meal.items || []).filter((item) => hasRenderableMealItem(item)),
  }))
  .filter((meal) => meal.items.length > 0))

function hasRenderableMealItem(item) {
  return Boolean(item?.itemValue?.trim?.() || item?.imagePath)
}

async function loadMenu() {
  loading.value = true
  errorMessage.value = ''

  try {
    payload.value = route.params.token
      ? await getPublicMenuByToken(route.params.token)
      : await getPublicMenuById(route.params.id)
  } catch (error) {
    payload.value = null
    errorMessage.value = error?.message || '餐单加载失败'
  } finally {
    loading.value = false
  }
}

function printPage() {
  window.print()
}

function setFontSize(value) {
  const normalized = normalizePresentationFontSize(value)
  fontSize.value = normalized
  try {
    window.localStorage.setItem(fontSizeStorageKey, normalized)
  } catch {
    // Ignore local storage failures in private mode or restricted browsers.
  }
}

function readStoredLayout() {
  try {
    return normalizePresentationLayout(window.localStorage.getItem(layoutStorageKey))
  } catch {
    return DEFAULT_PRESENTATION_LAYOUT
  }
}

function setLayoutState(value, { persist = true } = {}) {
  const normalized = normalizePresentationLayout(value)
  layout.value = normalized
  if (persist) {
    try {
      window.localStorage.setItem(layoutStorageKey, normalized)
    } catch {
      // Ignore local storage failures in private mode or restricted browsers.
    }
  }
  return normalized
}

function resolveRouteLayout(rawLayout) {
  if (Array.isArray(rawLayout)) {
    return rawLayout[0] || ''
  }
  return typeof rawLayout === 'string' ? rawLayout : ''
}

async function replaceRouteLayout(value) {
  const normalized = normalizePresentationLayout(value)
  const currentQueryLayout = resolveRouteLayout(route.query.layout)
  if (currentQueryLayout === normalized) {
    return
  }

  await router.replace({
    query: {
      ...route.query,
      layout: normalized,
    },
  })
}

function setLayout(value) {
  const normalized = setLayoutState(value)
  replaceRouteLayout(normalized).catch(() => {
    ElMessage.error('样式切换失败，请稍后重试')
  })
}

async function copyCurrentLayoutLink() {
  try {
    const url = new URL(window.location.href)
    url.searchParams.set('layout', layout.value)
    await copyText(url.toString())
    ElMessage.success('当前样式链接已复制')
  } catch (error) {
    ElMessage.error(error?.message || '复制链接失败')
  }
}

async function syncLayoutFromRoute(rawLayout) {
  const queryLayout = resolveRouteLayout(rawLayout)
  if (queryLayout) {
    const normalized = setLayoutState(queryLayout)
    if (queryLayout !== normalized) {
      await replaceRouteLayout(normalized)
    }
    return
  }

  setLayoutState(readStoredLayout())
}

async function ensureHtml2Canvas() {
  if (!html2canvasLoader) {
    html2canvasLoader = import('html2canvas')
      .then((module) => module.default || module)
      .catch(() => {
        html2canvasLoader = null
        throw new Error('导出组件加载失败，请稍后重试')
      })
  }

  return html2canvasLoader
}

function nextFrame() {
  return new Promise((resolve) => {
    requestAnimationFrame(() => resolve())
  })
}

async function waitForPresentationAssets(root) {
  if (!root) {
    return
  }

  if (document.fonts?.ready) {
    await document.fonts.ready
  }

  const images = Array.from(root.querySelectorAll('img'))
  await Promise.all(images.map(async (image) => {
    if (image.complete && image.naturalWidth > 0) {
      if (typeof image.decode === 'function') {
        try {
          await image.decode()
        } catch {
          // Ignore decode failures and fall back to the rendered bitmap.
        }
      }
      return
    }

    await new Promise((resolve) => {
      const done = () => resolve()
      image.addEventListener('load', done, { once: true })
      image.addEventListener('error', done, { once: true })
    })
  }))

  await nextFrame()
  await nextFrame()
}

async function exportImage() {
  if (!pageRef.value || !cardRef.value) {
    return
  }

  let exportHost = null

  try {
    const html2canvas = await ensureHtml2Canvas()
    await waitForPresentationAssets(cardRef.value)

    const sourceRect = cardRef.value.getBoundingClientRect()
    exportHost = document.createElement('div')
    exportHost.style.position = 'fixed'
    exportHost.style.left = '-10000px'
    exportHost.style.top = '0'
    exportHost.style.width = `${Math.ceil(sourceRect.width)}px`
    exportHost.style.pointerEvents = 'none'
    exportHost.style.opacity = '0'

    const pageClone = pageRef.value.cloneNode(true)
    const toolbar = pageClone.querySelector('.presentation-toolbar')
    if (toolbar) {
      toolbar.remove()
    }

    pageClone.style.minHeight = 'auto'
    pageClone.style.padding = '0'
    pageClone.style.width = `${Math.ceil(sourceRect.width)}px`

    const clone = pageClone.querySelector('.presentation-card')
    if (!clone) {
      throw new Error('导出内容生成失败，请刷新页面后重试')
    }

    clone.style.width = `${Math.ceil(sourceRect.width)}px`
    clone.style.maxWidth = `${Math.ceil(sourceRect.width)}px`
    clone.style.margin = '0'

    exportHost.appendChild(pageClone)
    document.body.appendChild(exportHost)

    await waitForPresentationAssets(pageClone)
    const exportRect = clone.getBoundingClientRect()
    const exportViewportWidth = Math.max(
      window.innerWidth || 0,
      document.documentElement?.clientWidth || 0,
      Math.ceil(exportRect.width),
    )
    const exportViewportHeight = Math.max(
      window.innerHeight || 0,
      document.documentElement?.clientHeight || 0,
      Math.ceil(exportRect.height),
    )
    const canvas = await html2canvas(clone, {
      scale: Math.max(2, Math.min(window.devicePixelRatio || 2, 3)),
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff',
      scrollX: 0,
      scrollY: 0,
      width: Math.ceil(exportRect.width),
      height: Math.ceil(exportRect.height),
      windowWidth: exportViewportWidth,
      windowHeight: exportViewportHeight,
      letterRendering: true,
      allowTaint: false,
    })

    const link = document.createElement('a')
    const date = (menuForm.value?.menuDate || '').trim().replace(/[\s/:]+/g, '-')
    link.download = `${title.value}${date ? `_${date}` : ''}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
  } catch (error) {
    ElMessage.error(error?.message || '导出图片失败')
  } finally {
    if (exportHost?.parentNode) {
      exportHost.parentNode.removeChild(exportHost)
    }
  }
}

function resolveImageUrl(path) {
  if (!path) {
    return ''
  }

  if (/^https?:\/\//i.test(path)) {
    return path
  }

  return new URL(path, window.location.origin).toString()
}

watch(() => [route.params.id, route.params.token], loadMenu, { immediate: true })
watch(() => route.query.layout, syncLayoutFromRoute, { immediate: true })

onMounted(() => {
  try {
    fontSize.value = normalizePresentationFontSize(window.localStorage.getItem(fontSizeStorageKey))
  } catch {
    fontSize.value = DEFAULT_PRESENTATION_FONT_SIZE
  }
  document.body.classList.add('menu-presentation-body')
})

onBeforeUnmount(() => {
  document.body.classList.remove('menu-presentation-body')
})
</script>

<template>
  <div ref="pageRef" :class="presentationPageClasses" :style="presentationStyleVars" v-loading="loading">
    <div class="presentation-toolbar" :class="{ 'presentation-toolbar--share': shareMode }">
      <div class="presentation-layout-controls">
        <span class="presentation-font-label">模板</span>
        <button
          v-for="option in layoutOptions"
          :key="option.value"
          type="button"
          class="presentation-layout-btn"
          :class="{ 'presentation-layout-btn--active': layout === option.value }"
          @click="setLayout(option.value)"
        >
          {{ option.label }}
        </button>
        <span class="presentation-layout-hint">{{ currentLayout.description }}</span>
      </div>

      <div class="presentation-font-controls">
        <span class="presentation-font-label">字号</span>
        <button
          v-for="option in fontSizeOptions"
          :key="option.value"
          type="button"
          class="presentation-font-btn"
          :class="{ 'presentation-font-btn--active': fontSize === option.value }"
          @click="setFontSize(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <button class="presentation-btn presentation-btn--secondary" @click="copyCurrentLayoutLink">复制当前样式链接</button>
      <button class="presentation-btn presentation-btn--secondary" @click="printPage">打印</button>
      <button class="presentation-btn presentation-btn--primary" @click="exportImage">导出图片</button>
    </div>

    <div v-if="errorMessage" class="presentation-empty">
      <h1>餐单不存在</h1>
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else-if="menuForm" ref="cardRef" class="presentation-card">
      <div class="presentation-header">
        <img class="presentation-logo" :src="logoSrc" alt="康服到">
        <div class="presentation-title">{{ title }}</div>
      </div>

      <section v-if="menuForm.showSwapGuide && swapGuideSection" class="presentation-section">
        <div class="presentation-section-title">{{ swapGuideTitle }}</div>
        <div
          class="presentation-content"
          :style="{ color: swapGuideSection.color || '#2d2d2d', fontWeight: swapGuideSection.bold ? 700 : 400 }"
        >
          {{ swapGuideSection.content || '暂无指南' }}
        </div>
        <img
          v-if="swapGuideSection.imagePath"
          class="presentation-image"
          :src="resolveImageUrl(swapGuideSection.imagePath)"
          alt="swap guide"
        >
      </section>

      <section v-if="menuForm.showWeeklyTip && weeklyTipSection" class="presentation-section">
        <div class="presentation-section-title">{{ weeklyTipTitle }}</div>
        <div
          class="presentation-content"
          :style="{ color: weeklyTipSection.color || '#2d2d2d', fontWeight: weeklyTipSection.bold ? 700 : 400 }"
        >
          {{ weeklyTipSection.content || '暂无每周提示' }}
        </div>
        <img
          v-if="weeklyTipSection.imagePath"
          class="presentation-image"
          :src="resolveImageUrl(weeklyTipSection.imagePath)"
          alt="weekly tip"
        >
      </section>

      <section v-for="section in extraSections" :key="`${section.sectionType}-${section.sortOrder}`" class="presentation-section">
        <div class="presentation-section-title">{{ resolveSectionTitle(section, section.sectionType) }}</div>
        <div
          class="presentation-content"
          :style="{ color: section.color || '#2d2d2d', fontWeight: section.bold ? 700 : 400 }"
        >
          {{ section.content || '-' }}
        </div>
        <img
          v-if="section.imagePath"
          class="presentation-image"
          :src="resolveImageUrl(section.imagePath)"
          alt="section image"
        >
      </section>

      <div class="presentation-date">{{ menuForm.menuDate || '' }}</div>

      <section
        v-for="meal in renderedMeals"
        :key="`${meal.mealCode}-${meal.sortOrder}`"
        class="presentation-meal"
        :class="{
          'presentation-meal--fresh': currentLayoutCode === 'fresh-card',
          'presentation-meal--brief': currentLayoutCode === 'brief-pro',
        }"
      >
        <template v-if="currentLayoutCode === 'fresh-card'">
          <div class="presentation-meal-fresh-header">
            <div class="presentation-meal-fresh-name">{{ meal.mealName }}</div>
            <div class="presentation-meal-fresh-time">{{ meal.mealTime || meal.timeLabel }}</div>
          </div>
          <div class="presentation-meal-fresh-body">
            <div v-for="item in meal.items" :key="`${item.itemCode}-${item.sortOrder}`" class="presentation-meal-fresh-row">
              <div class="presentation-meal-fresh-label">{{ item.itemName }}</div>
              <div class="presentation-meal-fresh-value-group">
                <div
                  class="presentation-meal-fresh-value"
                  :style="{ color: item.color || '#2d2d2d', fontWeight: item.bold ? 700 : 400 }"
                >
                  {{ item.itemValue || '-' }}
                </div>
                <img
                  v-if="item.imagePath"
                  class="presentation-item-image"
                  :src="resolveImageUrl(item.imagePath)"
                  alt="meal item"
                >
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="currentLayoutCode === 'brief-pro'">
          <div class="presentation-meal-brief-header">
            <div class="presentation-meal-brief-name">{{ meal.mealName }}</div>
            <div class="presentation-meal-brief-time">{{ meal.mealTime || meal.timeLabel }}</div>
          </div>
          <div class="presentation-meal-brief-table">
            <div v-for="item in meal.items" :key="`${item.itemCode}-${item.sortOrder}`" class="presentation-meal-brief-row">
              <div class="presentation-meal-brief-label">{{ item.itemName }}</div>
              <div class="presentation-meal-brief-value-group">
                <div
                  class="presentation-meal-brief-value"
                  :style="{ color: item.color || '#2d2d2d', fontWeight: item.bold ? 700 : 400 }"
                >
                  {{ item.itemValue || '-' }}
                </div>
                <img
                  v-if="item.imagePath"
                  class="presentation-item-image"
                  :src="resolveImageUrl(item.imagePath)"
                  alt="meal item"
                >
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="presentation-meal-side">
            <div class="presentation-meal-name">{{ meal.mealName }}</div>
            <div class="presentation-meal-time">{{ meal.mealTime || meal.timeLabel }}</div>
          </div>
          <div class="presentation-meal-main">
            <div v-for="item in meal.items" :key="`${item.itemCode}-${item.sortOrder}`" class="presentation-meal-row">
              <div class="presentation-meal-label">{{ item.itemName }}：</div>
              <div class="presentation-meal-value-group">
                <div
                  class="presentation-meal-value"
                  :style="{ color: item.color || '#2d2d2d', fontWeight: item.bold ? 700 : 400 }"
                >
                  {{ item.itemValue || '-' }}
                </div>
                <img
                  v-if="item.imagePath"
                  class="presentation-item-image"
                  :src="resolveImageUrl(item.imagePath)"
                  alt="meal item"
                >
              </div>
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
@font-face {
  font-family: 'FZZhongCuYaSong';
  src: url('../assets/fonts/FZZhongCuYaSong.ttf') format('truetype');
  font-weight: 500;
  font-style: normal;
  font-display: swap;
}

.presentation-page,
.presentation-page *,
.presentation-card,
.presentation-card * {
  font-family: 'FZZhongCuYaSong', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.presentation-page {
  min-height: 100vh;
  background: var(--presentation-page-bg);
  padding: 24px 16px 48px;
  --presentation-title-size: 24px;
  --presentation-section-title-size: 24px;
  --presentation-date-size: 22px;
  --presentation-content-size: 20px;
  --presentation-meal-name-size: 20px;
  --presentation-meal-time-size: 12px;
  --presentation-meal-main-size: 20px;
  --presentation-meal-label-size: 20px;
}

.presentation-toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.presentation-toolbar--share {
  position: sticky;
  top: 0;
  z-index: 10;
  padding-top: 8px;
  background: var(--presentation-toolbar-bg);
  backdrop-filter: blur(8px);
}

.presentation-btn {
  border: none;
  border-radius: 6px;
  padding: 9px 18px;
  cursor: pointer;
  font: inherit;
}

.presentation-btn--primary {
  background: var(--presentation-accent);
  color: #fff;
}

.presentation-btn--secondary {
  background: var(--presentation-accent-soft);
  color: var(--presentation-accent);
}

.presentation-layout-controls,
.presentation-font-controls {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--presentation-toolbar-border);
}

.presentation-layout-hint {
  font-size: 12px;
  color: var(--presentation-muted-text);
  margin-left: 4px;
}

.presentation-font-label {
  font-size: 13px;
  color: var(--presentation-muted-text);
}

.presentation-layout-btn,
.presentation-font-btn {
  border: 1px solid var(--presentation-toolbar-border);
  border-radius: 999px;
  background: #fff;
  color: var(--presentation-accent);
  padding: 5px 10px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
}

.presentation-layout-btn--active,
.presentation-font-btn--active {
  background: var(--presentation-accent);
  border-color: var(--presentation-accent);
  color: #fff;
}

.presentation-card {
  width: 100%;
  max-width: var(--presentation-card-max-width);
  margin: 0 auto;
  background: var(--presentation-card-bg);
  border-radius: var(--presentation-card-radius);
  box-shadow: var(--presentation-card-shadow);
  padding: 40px 30px 32px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--presentation-card-border);
  background-image: linear-gradient(0deg, rgba(248, 246, 243, 0.02) 1px, transparent 1px);
  background-size: 100% 24px;
}

.presentation-header {
  display: flex;
  align-items: center;
  justify-content: var(--presentation-header-align);
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--presentation-divider-color);
}

.presentation-logo {
  display: block;
  width: 112px;
  height: auto;
  flex-shrink: 0;
  object-fit: contain;
}

.presentation-title {
  flex: 1;
  text-align: right;
  font-size: var(--presentation-title-size);
  font-weight: 500;
  color: var(--presentation-accent);
  letter-spacing: 0.5px;
}

.presentation-section {
  margin-bottom: 28px;
}

.presentation-section-title,
.presentation-date {
  font-size: var(--presentation-section-title-size);
  font-weight: 500;
  color: var(--presentation-accent);
  text-align: center;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--presentation-divider-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  letter-spacing: 0.4px;
  margin-bottom: 16px;
}

.presentation-section-title::after,
.presentation-date::after {
  content: '';
  width: 52px;
  height: 2px;
  background: var(--presentation-accent);
  display: block;
  margin: 6px auto 0;
  border-radius: 1px;
}

.presentation-content {
  font-size: var(--presentation-content-size);
  line-height: 1.85;
  padding: 16px 18px;
  background: var(--presentation-section-bg);
  border-radius: var(--presentation-section-radius);
  border: 1px solid var(--presentation-section-border);
  white-space: pre-wrap;
}

.presentation-image,
.presentation-item-image {
  display: block;
  width: 100%;
  max-width: 320px;
  margin-top: 14px;
  border-radius: 8px;
  border: 1px solid var(--presentation-section-border);
  box-shadow: var(--presentation-image-shadow);
}

.presentation-date {
  font-size: var(--presentation-date-size);
  color: var(--presentation-text-color);
}

.presentation-meal {
  margin-bottom: 10px;
  padding: 10px 8px;
  border-bottom: 1px solid var(--presentation-divider-color);
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border-radius: var(--presentation-meal-radius);
}

.presentation-meal:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.presentation-meal-side {
  width: 92px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--presentation-meal-side-bg);
  color: var(--presentation-meal-side-color);
  padding: 10px 8px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(42, 92, 69, 0.08);
}

.presentation-meal-name {
  font-size: var(--presentation-meal-name-size);
  font-weight: 500;
  line-height: 1.2;
}

.presentation-meal-time {
  font-size: var(--presentation-meal-time-size);
  color: var(--presentation-meal-side-subtle);
  line-height: 1.3;
  margin-top: 4px;
  max-width: 78px;
  text-align: center;
  word-break: break-word;
}

.presentation-meal-main {
  flex: 1;
  font-size: var(--presentation-meal-main-size);
  line-height: 1.55;
  color: var(--presentation-text-color);
}

.presentation-meal-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
  padding: 2px 0;
  border-bottom: 1px solid var(--presentation-meal-row-divider);
}

.presentation-meal-row:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.presentation-meal-label {
  min-width: 64px;
  color: var(--presentation-label-color);
  font-size: var(--presentation-meal-label-size);
  flex-shrink: 0;
}

.presentation-meal-value-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.presentation-meal-value {
  flex: 1;
  word-break: break-word;
  white-space: pre-wrap;
}

.presentation-empty {
  max-width: 420px;
  margin: 120px auto 0;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
}

.presentation-empty h1 {
  margin: 0 0 12px;
  font-size: 28px;
  color: var(--presentation-accent);
}

.presentation-empty p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.presentation-page--fresh-card .presentation-header {
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 14px;
}

.presentation-page--fresh-card .presentation-title {
  text-align: center;
}

.presentation-page--fresh-card .presentation-section {
  padding: 18px 18px 20px;
  border-radius: 20px;
  border: 1px solid var(--presentation-section-border);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 26px rgba(74, 111, 89, 0.08);
}

.presentation-page--fresh-card .presentation-section-title {
  margin-bottom: 14px;
}

.presentation-page--fresh-card .presentation-content {
  background: transparent;
  border: none;
  padding: 0;
}

.presentation-page--fresh-card .presentation-date {
  padding: 12px 18px;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--presentation-section-border);
  border-radius: 999px;
}

.presentation-page--fresh-card .presentation-date::after {
  display: none;
}

.presentation-page--fresh-card .presentation-meal {
  padding: 0;
  margin-bottom: 14px;
  border: 1px solid var(--presentation-section-border);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(74, 111, 89, 0.08);
  display: block;
  overflow: hidden;
}

.presentation-page--brief-pro .presentation-card {
  background-image: none;
}

.presentation-page--brief-pro .presentation-header {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.presentation-page--brief-pro .presentation-logo {
  width: 92px;
  align-self: auto;
}

.presentation-page--brief-pro .presentation-title {
  width: auto;
  flex: 1;
  text-align: left;
  letter-spacing: 0.2px;
}

.presentation-page--brief-pro .presentation-section {
  padding-left: 16px;
  border-left: 4px solid var(--presentation-accent);
  margin-bottom: 24px;
}

.presentation-page--brief-pro .presentation-section-title {
  align-items: flex-start;
  text-align: left;
}

.presentation-page--brief-pro .presentation-section-title::after {
  margin-left: 0;
}

.presentation-page--brief-pro .presentation-date {
  align-items: flex-start;
  text-align: left;
}

.presentation-page--brief-pro .presentation-date::after {
  margin-left: 0;
  margin-right: auto;
}

.presentation-page--brief-pro .presentation-meal {
  padding: 0;
  margin-bottom: 12px;
  border: 1px solid var(--presentation-section-border);
  background: #fafbfa;
  box-shadow: none;
  display: block;
  overflow: hidden;
}

.presentation-page--brief-pro .presentation-meal-side {
  box-shadow: none;
  border: 1px solid var(--presentation-section-border);
}

.presentation-meal-fresh-header,
.presentation-meal-brief-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.presentation-meal-fresh-name,
.presentation-meal-brief-name {
  font-size: var(--presentation-meal-name-size);
  font-weight: 500;
}

.presentation-meal-fresh-time,
.presentation-meal-brief-time {
  font-size: calc(var(--presentation-meal-time-size) + 1px);
  line-height: 1.4;
  text-align: right;
  word-break: break-word;
}

.presentation-page--fresh-card .presentation-meal-fresh-header {
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(231, 245, 235, 0.95) 0%, rgba(214, 235, 220, 0.95) 100%);
  color: #2f6a4c;
  border-bottom: 1px solid var(--presentation-section-border);
}

.presentation-page--fresh-card .presentation-meal-fresh-time {
  color: #486a56;
}

.presentation-meal-fresh-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px 18px;
}

.presentation-meal-fresh-row {
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(247, 252, 248, 1) 0%, rgba(240, 248, 242, 1) 100%);
  border: 1px solid rgba(220, 233, 223, 0.95);
}

.presentation-meal-fresh-label {
  font-size: calc(var(--presentation-meal-label-size) - 2px);
  color: #55705f;
  margin-bottom: 8px;
  font-weight: 500;
}

.presentation-meal-fresh-value-group,
.presentation-meal-brief-value-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.presentation-meal-fresh-value,
.presentation-meal-brief-value {
  font-size: var(--presentation-meal-main-size);
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.presentation-page--brief-pro .presentation-meal-brief-header {
  padding: 14px 16px;
  background: #eef2ef;
  color: #1f4739;
  border-bottom: 1px solid var(--presentation-section-border);
}

.presentation-page--brief-pro .presentation-meal-brief-time {
  color: #54655d;
}

.presentation-meal-brief-table {
  display: flex;
  flex-direction: column;
}

.presentation-meal-brief-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(213, 219, 212, 0.95);
}

.presentation-meal-brief-row:last-child {
  border-bottom: none;
}

.presentation-meal-brief-label {
  font-size: calc(var(--presentation-meal-label-size) - 2px);
  color: #4f5f56;
  font-weight: 500;
  letter-spacing: 0.2px;
}

@media (max-width: 900px) {
  .presentation-layout-controls {
    width: 100%;
    justify-content: center;
  }

  .presentation-layout-hint {
    width: 100%;
    text-align: center;
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .presentation-page {
    padding: 16px 12px 32px;
  }

  .presentation-card {
    padding: 32px 20px 24px;
  }

  .presentation-header {
    gap: 14px;
    margin-bottom: 20px;
  }

  .presentation-logo {
    width: 92px;
  }

  .presentation-toolbar {
    gap: 10px;
  }

  .presentation-font-controls,
  .presentation-layout-controls {
    width: 100%;
    justify-content: center;
  }

  .presentation-meal {
    gap: 10px;
    padding: 8px 4px;
  }

  .presentation-meal-side {
    width: 78px;
    padding: 8px 6px;
  }

  .presentation-meal-time {
    max-width: 66px;
  }

  .presentation-meal-label {
    min-width: 56px;
  }

  .presentation-section-title,
  .presentation-date,
  .presentation-title {
    letter-spacing: 0.2px;
  }

  .presentation-content,
  .presentation-meal-main,
  .presentation-meal-label {
    line-height: 1.45;
  }

  .presentation-page--fresh-card .presentation-section {
    padding: 14px 12px;
  }

  .presentation-page--brief-pro .presentation-section {
    padding-left: 12px;
  }

  .presentation-page--fresh-card .presentation-meal,
  .presentation-page--brief-pro .presentation-meal {
    padding: 0;
  }

  .presentation-page--fresh-card .presentation-meal-fresh-header,
  .presentation-page--fresh-card .presentation-meal-fresh-body,
  .presentation-page--brief-pro .presentation-meal-brief-header {
    padding-left: 12px;
    padding-right: 12px;
  }

  .presentation-meal-brief-row {
    grid-template-columns: 74px minmax(0, 1fr);
    gap: 10px;
    padding: 10px 12px;
  }

  .presentation-meal-fresh-row {
    padding: 10px 12px;
  }
}
</style>
