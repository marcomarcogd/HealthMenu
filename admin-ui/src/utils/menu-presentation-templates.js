export const DEFAULT_PRESENTATION_LAYOUT = 'classic'

export const PRESENTATION_LAYOUT_OPTIONS = [
  { label: '标准版', value: 'classic', description: '沿用当前经典餐单样式，稳妥通用。' },
  { label: '清新卡片版', value: 'fresh-card', description: '色彩更轻，区块卡片感更强，适合日常查看。' },
  { label: '专业简报版', value: 'brief-pro', description: '信息层级更清晰，适合正式打印与交付。' },
]

const PRESENTATION_LAYOUT_CONFIGS = {
  classic: {
    value: 'classic',
    label: '标准版',
    description: '沿用当前经典餐单样式，稳妥通用。',
    className: 'presentation-page--classic',
    cssVars: {
      '--presentation-page-bg': '#faf7f2',
      '--presentation-toolbar-bg': 'rgba(250, 247, 242, 0.94)',
      '--presentation-toolbar-border': '#e8e5e0',
      '--presentation-card-max-width': '560px',
      '--presentation-card-bg': '#ffffff',
      '--presentation-card-border': '#e8e5e0',
      '--presentation-card-radius': '8px',
      '--presentation-card-shadow': '0 4px 16px rgba(0, 0, 0, 0.03)',
      '--presentation-accent': '#2a5c45',
      '--presentation-accent-soft': '#eef3ee',
      '--presentation-section-bg': '#f8f6f3',
      '--presentation-section-border': '#e8e5e0',
      '--presentation-divider-color': '#e8e5e0',
      '--presentation-meal-side-bg': '#2a5c45',
      '--presentation-meal-side-color': '#ffffff',
      '--presentation-meal-side-subtle': '#e6f0ea',
      '--presentation-label-color': '#666666',
      '--presentation-text-color': '#333333',
      '--presentation-muted-text': '#516257',
      '--presentation-meal-row-divider': 'transparent',
      '--presentation-image-shadow': '0 3px 10px rgba(0, 0, 0, 0.05)',
      '--presentation-date-align': 'center',
      '--presentation-header-align': 'space-between',
      '--presentation-section-radius': '4px',
      '--presentation-meal-radius': '0px',
    },
  },
  'fresh-card': {
    value: 'fresh-card',
    label: '清新卡片版',
    description: '色彩更轻，区块卡片感更强，适合日常查看。',
    className: 'presentation-page--fresh-card',
    cssVars: {
      '--presentation-page-bg': 'linear-gradient(180deg, #f2f8f4 0%, #f9f5ef 100%)',
      '--presentation-toolbar-bg': 'rgba(242, 248, 244, 0.95)',
      '--presentation-toolbar-border': '#d5e6da',
      '--presentation-card-max-width': '600px',
      '--presentation-card-bg': 'linear-gradient(180deg, #ffffff 0%, #f8fcf8 100%)',
      '--presentation-card-border': '#dce9df',
      '--presentation-card-radius': '22px',
      '--presentation-card-shadow': '0 14px 36px rgba(74, 111, 89, 0.12)',
      '--presentation-accent': '#2f6a4c',
      '--presentation-accent-soft': '#edf6ef',
      '--presentation-section-bg': '#f5fbf6',
      '--presentation-section-border': '#dce9df',
      '--presentation-divider-color': '#dce9df',
      '--presentation-meal-side-bg': 'linear-gradient(180deg, #edf7ef 0%, #dff0e2 100%)',
      '--presentation-meal-side-color': '#2f6a4c',
      '--presentation-meal-side-subtle': '#4d775f',
      '--presentation-label-color': '#5f7264',
      '--presentation-text-color': '#2f3a33',
      '--presentation-muted-text': '#55705f',
      '--presentation-meal-row-divider': 'rgba(220, 233, 223, 0.9)',
      '--presentation-image-shadow': '0 10px 22px rgba(74, 111, 89, 0.12)',
      '--presentation-date-align': 'center',
      '--presentation-header-align': 'space-between',
      '--presentation-section-radius': '18px',
      '--presentation-meal-radius': '18px',
    },
  },
  'brief-pro': {
    value: 'brief-pro',
    label: '专业简报版',
    description: '信息层级更清晰，适合正式打印与交付。',
    className: 'presentation-page--brief-pro',
    cssVars: {
      '--presentation-page-bg': '#f4f5f3',
      '--presentation-toolbar-bg': 'rgba(244, 245, 243, 0.96)',
      '--presentation-toolbar-border': '#d5dbd4',
      '--presentation-card-max-width': '640px',
      '--presentation-card-bg': '#ffffff',
      '--presentation-card-border': '#cfd8d3',
      '--presentation-card-radius': '6px',
      '--presentation-card-shadow': '0 10px 24px rgba(42, 48, 44, 0.08)',
      '--presentation-accent': '#1f4739',
      '--presentation-accent-soft': '#eff3f0',
      '--presentation-section-bg': '#ffffff',
      '--presentation-section-border': '#d9dfda',
      '--presentation-divider-color': '#d3dad5',
      '--presentation-meal-side-bg': '#f1f4f2',
      '--presentation-meal-side-color': '#1f4739',
      '--presentation-meal-side-subtle': '#557064',
      '--presentation-label-color': '#4f5f56',
      '--presentation-text-color': '#26312c',
      '--presentation-muted-text': '#5d6a62',
      '--presentation-meal-row-divider': 'rgba(213, 219, 212, 0.95)',
      '--presentation-image-shadow': '0 6px 16px rgba(42, 48, 44, 0.08)',
      '--presentation-date-align': 'flex-end',
      '--presentation-header-align': 'flex-start',
      '--presentation-section-radius': '10px',
      '--presentation-meal-radius': '10px',
    },
  },
}

export function normalizePresentationLayout(value) {
  return Object.prototype.hasOwnProperty.call(PRESENTATION_LAYOUT_CONFIGS, value)
    ? value
    : DEFAULT_PRESENTATION_LAYOUT
}

export function resolvePresentationLayout(value) {
  return PRESENTATION_LAYOUT_CONFIGS[normalizePresentationLayout(value)]
}
