import { describe, expect, it } from 'vitest'
import { buildTemplateDesignPayload, normalizeTemplateSortOrders, validateTemplateDesign } from './template-design'

describe('template-design utils', () => {
  it('recalculates nested sortOrder values', () => {
    const result = normalizeTemplateSortOrders({
      sections: [{ title: 'B', sortOrder: 9 }, { title: 'A', sortOrder: 4 }],
      meals: [
        {
          mealName: '午餐',
          sortOrder: 7,
          items: [{ itemName: '蔬菜', sortOrder: 8 }, { itemName: '主食', sortOrder: 3 }],
        },
      ],
    })

    expect(result.sections.map((item) => item.sortOrder)).toEqual([1, 2])
    expect(result.meals[0].sortOrder).toBe(1)
    expect(result.meals[0].items.map((item) => item.sortOrder)).toEqual([1, 2])
  })

  it('builds payload without client-only keys', () => {
    const payload = buildTemplateDesignPayload({
      id: 1,
      name: '模板',
      description: 'desc',
      themeCode: 'fresh',
      status: 1,
      isDefault: 0,
      sections: [{ _key: 'a', title: '区块' }],
      meals: [{ _key: 'b', mealName: '早餐', items: [{ _key: 'c', itemName: '字段' }] }],
    })

    expect(payload.sections[0]).not.toHaveProperty('_key')
    expect(payload.meals[0]).not.toHaveProperty('_key')
    expect(payload.meals[0].items[0]).not.toHaveProperty('_key')
    expect(payload.sections[0].sortOrder).toBe(1)
    expect(payload.meals[0].items[0].sortOrder).toBe(1)
  })

  it('validates required names', () => {
    expect(validateTemplateDesign({ id: 1, name: '  ', sections: [], meals: [] })).toBe('模板名称不能为空')
    expect(validateTemplateDesign({ id: 1, name: '模板', sections: [{ title: ' ' }], meals: [] })).toBe('区块名称不能为空')
    expect(validateTemplateDesign({ id: 1, name: '模板', sections: [], meals: [{ mealName: '早餐', items: [{ itemName: ' ' }] }] })).toBe('字段名称不能为空')
  })
})
