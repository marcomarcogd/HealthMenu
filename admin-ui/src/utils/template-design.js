export function validateTemplateDesign(design) {
  if (!design?.id) {
    return '缺少模板 ID，无法保存'
  }

  if (!design.name?.trim()) {
    return '模板名称不能为空'
  }

  const invalidSection = (design.sections || []).find((section) => !section.title?.trim())
  if (invalidSection) {
    return '区块名称不能为空'
  }

  const invalidMeal = (design.meals || []).find((meal) => !meal.mealName?.trim())
  if (invalidMeal) {
    return '餐次名称不能为空'
  }

  const invalidItem = (design.meals || []).flatMap((meal) => meal.items || []).find((item) => !item.itemName?.trim())
  if (invalidItem) {
    return '字段名称不能为空'
  }

  return ''
}

export function normalizeTemplateSortOrders(design) {
  const sections = (design.sections || []).map((section, index) => ({
    ...section,
    sortOrder: index + 1,
  }))

  const meals = (design.meals || []).map((meal, mealIndex) => ({
    ...meal,
    sortOrder: mealIndex + 1,
    items: (meal.items || []).map((item, itemIndex) => ({
      ...item,
      sortOrder: itemIndex + 1,
    })),
  }))

  return {
    ...design,
    sections,
    meals,
  }
}

export function buildTemplateDesignPayload(design) {
  const normalized = normalizeTemplateSortOrders(design)

  return {
    id: normalized.id,
    name: normalized.name,
    description: normalized.description,
    themeCode: normalized.themeCode,
    status: normalized.status,
    isDefault: normalized.isDefault,
    sections: normalized.sections.map(({ _key, _styleConfigState, ...section }) => ({ ...section })),
    meals: normalized.meals.map(({ _key, items, ...meal }) => ({
      ...meal,
      items: items.map(({ _key: itemKey, ...item }) => ({ ...item })),
    })),
  }
}

const DEFAULT_STYLE_CONFIG = {
  emphasis: false,
  lightBackground: false,
  imageFirst: false,
  printFriendly: false,
}

export function createDefaultSectionStyleConfig() {
  return { ...DEFAULT_STYLE_CONFIG }
}

export function parseSectionStyleConfig(styleConfigJson) {
  if (!styleConfigJson?.trim()) {
    return {
      config: createDefaultSectionStyleConfig(),
      hasCustomData: false,
      parseError: false,
    }
  }

  try {
    const parsed = JSON.parse(styleConfigJson)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {
        config: createDefaultSectionStyleConfig(),
        hasCustomData: true,
        parseError: true,
      }
    }

    return {
      config: {
        emphasis: !!parsed.emphasis,
        lightBackground: !!parsed.lightBackground,
        imageFirst: !!parsed.imageFirst,
        printFriendly: !!parsed.printFriendly,
      },
      hasCustomData: hasUnsupportedStyleKeys(parsed),
      parseError: false,
    }
  } catch {
    return {
      config: createDefaultSectionStyleConfig(),
      hasCustomData: true,
      parseError: true,
    }
  }
}

export function stringifySectionStyleConfig(config) {
  const normalized = {
    emphasis: !!config?.emphasis,
    lightBackground: !!config?.lightBackground,
    imageFirst: !!config?.imageFirst,
    printFriendly: !!config?.printFriendly,
  }

  if (!Object.values(normalized).some(Boolean)) {
    return ''
  }

  return JSON.stringify(normalized, null, 2)
}

function hasUnsupportedStyleKeys(parsed) {
  return Object.keys(parsed).some((key) => !(key in DEFAULT_STYLE_CONFIG))
}
