import { describe, expect, it } from 'vitest'
import { resolvePresentationTitle, resolveSectionTitle } from './menu-presentation'

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
})
