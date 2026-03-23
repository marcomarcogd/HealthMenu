export function createEmptyMenuForm() {
  return {
    id: null,
    customerId: null,
    templateId: null,
    menuDate: '',
    weekIndex: 1,
    title: '',
    themeCode: '',
    themeName: '',
    showWeeklyTip: true,
    showSwapGuide: true,
    status: 'DRAFT',
    statusLabel: '草稿',
    viewUrl: '',
    shareUrl: '',
    publishCount: 0,
    lastPublishedAt: '',
    sections: [],
    meals: [],
  }
}

export function normalizeMenuForm(payload = {}) {
  const form = {
    ...createEmptyMenuForm(),
    ...payload,
  }

  return {
    ...form,
    weekIndex: Number(form.weekIndex || 1),
    showWeeklyTip: form.showWeeklyTip ?? true,
    showSwapGuide: form.showSwapGuide ?? true,
    status: form.status || 'DRAFT',
    statusLabel: form.statusLabel || form.status || '草稿',
    themeCode: form.themeCode || '',
    themeName: form.themeName || form.themeCode || '',
    viewUrl: form.viewUrl || '',
    shareUrl: form.shareUrl || '',
    publishCount: Number(form.publishCount || 0),
    lastPublishedAt: form.lastPublishedAt || '',
    sections: Array.isArray(form.sections) ? form.sections.map((section, index) => ({
      title: '',
      content: '',
      sectionType: '',
      allowImage: false,
      imagePath: '',
      aiImagePrompt: '',
      bold: false,
      color: '#2d2d2d',
      ...section,
      sortOrder: section?.sortOrder ?? index + 1,
    })) : [],
    meals: Array.isArray(form.meals) ? form.meals.map((meal, mealIndex) => ({
      mealCode: '',
      mealName: '',
      timeLabel: '',
      mealTime: '',
      ...meal,
      sortOrder: meal?.sortOrder ?? mealIndex + 1,
      items: Array.isArray(meal?.items) ? meal.items.map((item, itemIndex) => ({
        itemCode: '',
        itemName: '',
        itemValue: '',
        allowImage: false,
        imagePath: '',
        aiImagePrompt: '',
        bold: false,
        color: '#2d2d2d',
        ...item,
        sortOrder: item?.sortOrder ?? itemIndex + 1,
      })) : [],
    })) : [],
  }
}

export function createEmptyMenuSelector() {
  return {
    customerId: null,
    templateId: null,
    sourceText: '',
  }
}

export function normalizeMenuSavePayload(payload) {
  return {
    ...payload,
    customerId: payload.customerId ? Number(payload.customerId) : null,
    templateId: payload.templateId ? Number(payload.templateId) : null,
  }
}

export function buildAiPreviewSummary(result = {}) {
  const mealCount = result.meals?.length || 0
  const sectionCount = result.sections?.length || 0
  const title = result.title?.trim() || '未识别标题'
  return `AI 解析完成：标题「${title}」，识别 ${mealCount} 个餐次、${sectionCount} 个区块`
}

export function hasMenuContent(form) {
  if (!form) {
    return false
  }

  if (form.title?.trim() || form.menuDate || form.customerId || form.templateId) {
    return true
  }

  if ((form.sections || []).some((section) => section.content?.trim() || section.imagePath)) {
    return true
  }

  return (form.meals || []).some((meal) => {
    if (meal.mealTime?.trim()) {
      return true
    }
    return (meal.items || []).some((item) => item.itemValue?.trim() || item.imagePath)
  })
}
