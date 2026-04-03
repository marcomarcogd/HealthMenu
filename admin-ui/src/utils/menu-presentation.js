export function resolvePresentationTitle(menuForm = {}) {
  const exclusiveTitleSection = (menuForm.sections || []).find((section) => section?.sectionType === 'EXCLUSIVE_TITLE')
  return exclusiveTitleSection?.content?.trim() || menuForm.title?.trim() || '专属餐单'
}

export function resolveSectionTitle(section, fallback = '') {
  return section?.title?.trim() || fallback
}
