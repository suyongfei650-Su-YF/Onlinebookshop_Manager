import type { Router } from 'vue-router'
import { userLogout } from '../api/user/auth'

export const USER_AUTH_EVENT = 'bookshop:user-auth-changed'

export function notifyUserAuthChanged(loggedIn: boolean) {
  window.dispatchEvent(new CustomEvent(USER_AUTH_EVENT, { detail: { loggedIn } }))
}

/** 退出登录：调用后端销毁 Session，并跳转登录页 */
export async function performUserLogout(router: Router) {
  try {
    await userLogout()
  } catch {
    /* 网络异常时仍清除前端状态并跳转 */
  }
  notifyUserAuthChanged(false)
  await router.replace({ path: '/user/login', query: { logout: '1' } })
}
