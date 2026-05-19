export type PortalHomeSectionId = 'new-arrivals' | 'hot-ranking'

export function isPortalHomeSectionId(value: string): value is PortalHomeSectionId {
  return value === 'new-arrivals' || value === 'hot-ranking'
}

/** 滚动到首页区块（需配合区块上的 scroll-mt-20 抵消固定顶栏） */
export function scrollToPortalHomeSection(
  sectionId: PortalHomeSectionId,
  behavior: ScrollBehavior = 'smooth',
) {
  const el = document.getElementById(sectionId)
  if (!el) return false
  el.scrollIntoView({ behavior, block: 'start' })
  return true
}
