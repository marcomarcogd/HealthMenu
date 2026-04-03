import { describe, expect, it } from 'vitest'
import {
  buildAiPreviewSummary,
  createEmptyMenuForm,
  createEmptyMenuSelector,
  hasMenuContent,
  normalizeMenuForm,
  normalizeMenuSavePayload,
} from './menu-form'

describe('menu-form utils', () => {
  it('creates default form and selector', () => {
    expect(createEmptyMenuForm().status).toBe('DRAFT')
    expect(createEmptyMenuSelector()).toEqual({ customerId: null, templateId: null, sourceText: '' })
  })

  it('normalizes nested meal and item arrays', () => {
    const result = normalizeMenuForm({
      customerId: 1,
      meals: [{ mealName: '早餐', items: [{ itemName: '蛋白' }] }],
    })

    expect(result.sections).toEqual([])
    expect(result.meals[0].sortOrder).toBe(1)
    expect(result.meals[0].items[0].sortOrder).toBe(1)
  })

  it('preserves save payload ids as strings', () => {
    expect(normalizeMenuSavePayload({ customerId: '12', templateId: '34' })).toEqual({ customerId: '12', templateId: '34' })
  })

  it('builds ai preview summary', () => {
    expect(buildAiPreviewSummary({ title: '营养周菜单', meals: [{}, {}], sections: [{}] })).toBe('AI 解析完成：标题「营养周菜单」，识别 2 个餐次、1 个区块')
    expect(buildAiPreviewSummary({ title: '营养周菜单', meals: [], sections: [], parseMessage: '未识别出明确餐次' })).toBe('AI 解析完成：标题「营养周菜单」，识别 0 个餐次、0 个区块。未识别出明确餐次')
  })

  it('detects whether form has content', () => {
    expect(hasMenuContent(createEmptyMenuForm())).toBe(false)
    expect(hasMenuContent({ ...createEmptyMenuForm(), title: '本周食谱' })).toBe(true)
    expect(hasMenuContent({ ...createEmptyMenuForm(), meals: [{ items: [{ itemValue: '鸡蛋' }] }] })).toBe(true)
  })
})
