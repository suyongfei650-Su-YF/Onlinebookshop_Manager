<template>
  <aside class="fixed left-0 top-16 h-[calc(100vh-64px)] w-64 flex flex-col pt-8 bg-stone-50 border-r border-stone-200 text-sm uppercase tracking-wider">
    <div class="px-6 mb-8">
      <div class="font-bold text-stone-900">尊敬的读者</div>
      <div class="text-[10px] text-stone-500">尊享会员</div>
    </div>
    <nav class="flex-1">
      <RouterLink :class="itemClass('profile')" to="/portal/profile">个人资料</RouterLink>
      <RouterLink :class="itemClass('orders')" to="/portal/orders">我的订单</RouterLink>
      <RouterLink :class="itemClass('favorites')" to="/portal/favorites">我的收藏</RouterLink>
      <RouterLink :class="itemClass('cart')" to="/portal/cart">我的购物车</RouterLink>
    </nav>
    <div class="border-t border-stone-200">
      <button
        type="button"
        class="flex w-full items-center px-6 py-4 text-stone-500 hover:text-stone-900 hover:bg-stone-100 disabled:opacity-50"
        :disabled="loggingOut"
        @click="onLogout"
      >
        {{ loggingOut ? '退出中…' : '退出登录' }}
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { performUserLogout } from '../../../utils/userSession'

const props = defineProps<{ active: 'profile' | 'orders' | 'favorites' | 'cart' }>()
const router = useRouter()
const loggingOut = ref(false)

const itemClass = (name: 'profile' | 'orders' | 'favorites' | 'cart') =>
  props.active === name
    ? 'flex items-center px-6 py-4 text-amber-700 bg-stone-100 font-bold border-r-4 border-amber-600'
    : 'flex items-center px-6 py-4 text-stone-500 hover:text-stone-900 hover:bg-stone-100'

async function onLogout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await performUserLogout(router)
  } finally {
    loggingOut.value = false
  }
}
</script>
