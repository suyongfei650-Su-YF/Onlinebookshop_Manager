import { useRoute } from 'vue-router'

/** 管理员端侧栏导航项样式（原型 _15 / _17） */
const inactive =
  'flex items-center gap-3 px-4 py-3 text-stone-400 hover:text-stone-100 hover:bg-stone-800/30 transition-all'
const active =
  'flex items-center gap-3 px-4 py-3 text-[#D4AF37] bg-stone-800/50 border-r-4 border-[#D4AF37] transition-all'

export function useAdminNav() {
  const route = useRoute()
  const nc = (path: string) => (route.path === path ? active : inactive)
  return { nc }
}
