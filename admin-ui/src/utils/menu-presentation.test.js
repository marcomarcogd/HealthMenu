import { describe, expect, it } from 'vitest'
import {
  buildPresentationFontVars,
  DEFAULT_PRESENTATION_FONT_SIZE,
  normalizePresentationFontSize,
  resolvePresentationTitle,
  resolveSectionTitle,
} from './menu-presentation'
import {
  DEFAULT_PRESENTATION_LAYOUT,
  normalizePresentationLayout,
  resolvePresentationLayout,
} from './menu-presentation-templates'

describe('menu-presentation utils', () => {
  it('prefers exclusive title content for the page title', () => {
    expect(resolvePresentationTitle({
      title: '餐单标题',
      sections: [{ sectionType: 'EXCLUSIVE_TITLE', content: '客户专属标题' }],
    })).toBe('客户专属标题')
  })

  it('falls back to menu title and default title', () => {
    expect(resolvePresentationTitle({ title: '餐单标题', sections: [] })).toBe('餐单标题')
    expect(resolvePresentationTitle({ sections: [] })).toBe('专属餐单')
  })

  it('prefers template section title over hard-coded defaults', () => {
    expect(resolveSectionTitle({ title: '本周执行重点' }, '每周提示')).toBe('本周执行重点')
    expect(resolveSectionTitle({ title: ' ' }, '每周提示')).toBe('每周提示')
  })

  it('normalizes invalid font size values to medium', () => {
    expect(normalizePresentationFontSize('large')).toBe('large')
    expect(normalizePresentationFontSize('huge')).toBe(DEFAULT_PRESENTATION_FONT_SIZE)
  })

  it('builds font variables for the selected size', () => {
    expect(buildPresentationFontVars('small')).toMatchObject({
      '--presentation-title-size': '22px',
      '--presentation-meal-main-size': '18px',
    })
    expect(buildPresentationFontVars('xlarge')).toMatchObject({
      '--presentation-title-size': '28px',
      '--presentation-meal-main-size': '24px',
    })
  })

  it('normalizes invalid presentation layouts to classic', () => {
    expect(normalizePresentationLayout('fresh-card')).toBe('fresh-card')
    expect(normalizePresentationLayout('poster')).toBe(DEFAULT_PRESENTATION_LAYOUT)
  })

  it('resolves presentation layout config metadata', () => {
    expect(resolvePresentationLayout('brief-pro')).toMatchObject({
      value: 'brief-pro',
      label: '专业简报版',
      className: 'presentation-page--brief-pro',
    })
  })
})
