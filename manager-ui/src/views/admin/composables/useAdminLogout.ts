import { useRouter } from 'vue-router'
import { adminLogout } from '../../../api/admin/auth'

export function useAdminLogout() {
  const router = useRouter()
  return async () => {
    try {
      await adminLogout()
    } catch {
      /* 后端不可用时仍回到登录页 */
    }
    await router.push('/admin/login')
  }
}
