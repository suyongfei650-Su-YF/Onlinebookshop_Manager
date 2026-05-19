import type { Router } from 'vue-router'

const BACK_FROM_KEY = 'portal:book-detail-back-from'
const SCROLL_HOME_KEY = 'portal:scroll:home'
const SCROLL_LIST_KEY = 'portal:scroll:list'
const LIST_PATH_KEY = 'portal:list-return-path'
const PENDING_RESTORE_KEY = 'portal:pending-restore'

export type PortalBookBackFrom = 'home' | 'list'

/** 进入图书详情前记录来源页与滚动位置 */
export function markPortalBookNavigation(from: PortalBookBackFrom, listFullPath?: string) {
  sessionStorage.setItem(BACK_FROM_KEY, from)
  if (from === 'home') {
    sessionStorage.setItem(SCROLL_HOME_KEY, String(window.scrollY))
  } else {
    sessionStorage.setItem(SCROLL_LIST_KEY, String(window.scrollY))
    sessionStorage.setItem(LIST_PATH_KEY, listFullPath || '/portal/list')
  }
}

/** 详情页「继续浏览」：回到来源页并标记待恢复滚动 */
export function continueBrowseFromBookDetail(router: Router) {
  const from = (sessionStorage.getItem(BACK_FROM_KEY) as PortalBookBackFrom) || 'list'
  sessionStorage.setItem(PENDING_RESTORE_KEY, from)
  if (from === 'home') {
    router.push({ name: 'portal-home' })
    return
  }
  const path = sessionStorage.getItem(LIST_PATH_KEY) || '/portal/list'
  router.push(path)
}

export function navigateToPortalBook(
  router: Router,
  bookId: number,
  from: PortalBookBackFrom,
  listFullPath?: string,
) {
  markPortalBookNavigation(from, listFullPath)
  router.push({ name: 'portal-book', params: { id: String(bookId) } })
}

/** 来源页挂载后恢复滚动（仅当从详情页返回时） */
export function tryRestorePendingPortalScroll(page: PortalBookBackFrom) {
  const pending = sessionStorage.getItem(PENDING_RESTORE_KEY)
  if (pending !== page) return

  sessionStorage.removeItem(PENDING_RESTORE_KEY)
  const key = page === 'home' ? SCROLL_HOME_KEY : SCROLL_LIST_KEY
  const raw = sessionStorage.getItem(key)
  sessionStorage.removeItem(key)
  sessionStorage.removeItem(BACK_FROM_KEY)
  if (page === 'list') {
    sessionStorage.removeItem(LIST_PATH_KEY)
  }

  if (raw == null) return
  const y = Number(raw)
  if (!Number.isFinite(y) || y < 0) return

  requestAnimationFrame(() => {
    window.scrollTo({ top: y, left: 0 })
  })
}
