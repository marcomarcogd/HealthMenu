export const DEFAULT_PRESENTATION_FONT_SIZE = 'medium'

export const PRESENTATION_FONT_SIZE_OPTIONS = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
  { label: '特大', value: 'xlarge' },
]

export function resolvePresentationTitle(menuForm = {}) {
  const exclusiveTitleSection = (menuForm.sections || []).find((section) => section?.sectionType === 'EXCLUSIVE_TITLE')
  return exclusiveTitleSection?.content?.trim() || menuForm.title?.trim() || '专属餐单'
}

export function resolveSectionTitle(section, fallback = '') {
  return section?.title?.trim() || fallback
}

export function normalizePresentationFontSize(value) {
  return PRESENTATION_FONT_SIZE_OPTIONS.some((item) => item.value === value)
    ? value
    : DEFAULT_PRESENTATION_FONT_SIZE
}

export function buildPresentationFontVars(size = DEFAULT_PRESENTATION_FONT_SIZE) {
  const normalized = normalizePresentationFontSize(size)
  const scaleMap = {
    small: {
      '--presentation-title-size': '22px',
      '--presentation-section-title-size': '22px',
      '--presentation-date-size': '20px',
      '--presentation-content-size': '18px',
      '--presentation-meal-name-size': '18px',
      '--presentation-meal-time-size': '11px',
      '--presentation-meal-main-size': '18px',
      '--presentation-meal-label-size': '18px',
    },
    medium: {
      '--presentation-title-size': '24px',
      '--presentation-section-title-size': '24px',
      '--presentation-date-size': '22px',
      '--presentation-content-size': '20px',
      '--presentation-meal-name-size': '20px',
      '--presentation-meal-time-size': '12px',
      '--presentation-meal-main-size': '20px',
      '--presentation-meal-label-size': '20px',
    },
    large: {
      '--presentation-title-size': '26px',
      '--presentation-section-title-size': '26px',
      '--presentation-date-size': '24px',
      '--presentation-content-size': '22px',
      '--presentation-meal-name-size': '22px',
      '--presentation-meal-time-size': '13px',
      '--presentation-meal-main-size': '22px',
      '--presentation-meal-label-size': '22px',
    },
    xlarge: {
      '--presentation-title-size': '28px',
      '--presentation-section-title-size': '28px',
      '--presentation-date-size': '26px',
      '--presentation-content-size': '24px',
      '--presentation-meal-name-size': '24px',
      '--presentation-meal-time-size': '14px',
      '--presentation-meal-main-size': '24px',
      '--presentation-meal-label-size': '24px',
    },
  }
  return scaleMap[normalized]
}
