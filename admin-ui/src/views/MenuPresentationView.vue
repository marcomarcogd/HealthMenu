<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicMenuById, getPublicMenuByToken } from '../api/public-menu'

const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const payload = ref(null)
const cardRef = ref(null)
const logoSrc = '/logo.png'

let html2canvasLoader = null

const menuForm = computed(() => payload.value?.menuForm || null)
const meals = computed(() => menuForm.value?.meals || [])
const shareMode = computed(() => Boolean(payload.value?.shareMode))
const title = computed(() => menuForm.value?.title || '专属餐单')
const swapGuideSection = computed(() => (menuForm.value?.sections || []).find((section) => section.sectionType === 'SWAP_GUIDE') || null)
const weeklyTipSection = computed(() => (menuForm.value?.sections || []).find((section) => section.sectionType === 'WEEKLY_TIP') || null)
const extraSections = computed(() => (menuForm.value?.sections || []).filter((section) => !['SWAP_GUIDE', 'WEEKLY_TIP', 'EXCLUSIVE_TITLE', 'DAILY_MENU'].includes(section.sectionType)))

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

async function ensureHtml2Canvas() {
  if (window.html2canvas) {
    return window.html2canvas
  }

  if (!html2canvasLoader) {
    html2canvasLoader = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://cdn.jsdelivr.net/npm/html2canvas@1.4.1/dist/html2canvas.min.js'
      script.async = true
      script.onload = () => resolve(window.html2canvas)
      script.onerror = () => reject(new Error('导出依赖加载失败'))
      document.head.appendChild(script)
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
  if (!cardRef.value) {
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

    const clone = cardRef.value.cloneNode(true)
    clone.style.width = `${Math.ceil(sourceRect.width)}px`
    clone.style.maxWidth = `${Math.ceil(sourceRect.width)}px`
    clone.style.margin = '0'

    exportHost.appendChild(clone)
    document.body.appendChild(exportHost)

    await waitForPresentationAssets(clone)
    const exportRect = clone.getBoundingClientRect()
    const exportViewportWidth = Math.max(window.innerWidth || 0, Math.ceil(exportRect.width), 1024)
    const exportViewportHeight = Math.max(window.innerHeight || 0, Math.ceil(exportRect.height))
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

watch(() => route.fullPath, loadMenu, { immediate: true })

onMounted(() => {
  document.body.classList.add('menu-presentation-body')
})

onBeforeUnmount(() => {
  document.body.classList.remove('menu-presentation-body')
})
</script>

<template>
  <div class="presentation-page" v-loading="loading">
    <div class="presentation-toolbar" :class="{ 'presentation-toolbar--share': shareMode }">
      <button class="presentation-btn presentation-btn--secondary" @click="printPage">打印</button>
      <button class="presentation-btn presentation-btn--primary" @click="exportImage">导出图片</button>
    </div>

    <div v-if="errorMessage" class="presentation-empty">
      <h1>餐单不存在</h1>
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else-if="menuForm" ref="cardRef" class="presentation-card">
      <div class="presentation-header">
        <img class="presentation-logo" :src="logoSrc" alt="康服团">
        <div class="presentation-title">{{ title }}</div>
      </div>

      <section v-if="menuForm.showSwapGuide && swapGuideSection" class="presentation-section">
        <div class="presentation-section-title">核心食物互换速查指南</div>
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
        <div class="presentation-section-title">每周提示</div>
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
        <div class="presentation-section-title">{{ section.title || section.sectionType }}</div>
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

      <section v-for="meal in meals" :key="`${meal.mealCode}-${meal.sortOrder}`" class="presentation-meal">
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
.presentation-page * {
  font-family: 'FZZhongCuYaSong', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.presentation-page {
  min-height: 100vh;
  background: #faf7f2;
  padding: 24px 16px 48px;
}

.presentation-toolbar {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.presentation-toolbar--share {
  position: sticky;
  top: 0;
  z-index: 10;
  padding-top: 8px;
  background: rgba(250, 247, 242, 0.94);
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
  background: #2a5c45;
  color: #fff;
}

.presentation-btn--secondary {
  background: #eef3ee;
  color: #2a5c45;
}

.presentation-card {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
  padding: 40px 30px 32px;
  position: relative;
  overflow: hidden;
  border: 1px solid #e8e5e0;
  background-image: linear-gradient(0deg, rgba(248, 246, 243, 0.02) 1px, transparent 1px);
  background-size: 100% 24px;
}

.presentation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e5e0;
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
  font-size: 24px;
  font-weight: 500;
  color: #2a5c45;
  letter-spacing: 0.5px;
}

.presentation-section {
  margin-bottom: 28px;
}

.presentation-section-title,
.presentation-date {
  font-size: 24px;
  font-weight: 500;
  color: #2a5c45;
  text-align: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e5e0;
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
  background: #2a5c45;
  display: block;
  margin: 6px auto 0;
  border-radius: 1px;
}

.presentation-content {
  font-size: 20px;
  line-height: 1.85;
  padding: 16px 18px;
  background: #f8f6f3;
  border-radius: 4px;
  border: 1px solid #e8e5e0;
  white-space: pre-wrap;
}

.presentation-image,
.presentation-item-image {
  display: block;
  width: 100%;
  max-width: 320px;
  margin-top: 14px;
  border-radius: 8px;
  border: 1px solid #e8e5e0;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
}

.presentation-date {
  font-size: 22px;
  color: #2d2d2d;
}

.presentation-meal {
  margin-bottom: 16px;
  padding: 14px 12px;
  border-bottom: 1px solid #e8e5e0;
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.presentation-meal:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.presentation-meal-side {
  width: 100px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #2a5c45;
  color: #fff;
  padding: 12px 8px;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(42, 92, 69, 0.1);
}

.presentation-meal-name {
  font-size: 20px;
  font-weight: 500;
  line-height: 1.2;
}

.presentation-meal-time {
  font-size: 12px;
  color: #e6f0ea;
  line-height: 1.35;
  margin-top: 6px;
  max-width: 85px;
  text-align: center;
  word-break: break-word;
}

.presentation-meal-main {
  flex: 1;
  font-size: 20px;
  line-height: 1.8;
  color: #333;
}

.presentation-meal-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
  padding: 4px 0;
}

.presentation-meal-row:last-child {
  margin-bottom: 0;
}

.presentation-meal-label {
  min-width: 72px;
  color: #666;
  font-size: 20px;
  flex-shrink: 0;
}

.presentation-meal-value-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  color: #2a5c45;
}

.presentation-empty p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
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

  .presentation-meal {
    flex-direction: column;
  }

  .presentation-section-title,
  .presentation-date,
  .presentation-title {
    font-size: 22px;
  }

  .presentation-content,
  .presentation-meal-main,
  .presentation-meal-label {
    font-size: 18px;
  }
}
</style>
