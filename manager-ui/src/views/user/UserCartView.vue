<template>
  <div class="font-body-md min-h-screen bg-background pb-32">
    <TopNav />
    <div class="flex pt-16">
      <UserSideNav active="cart" />
      <main class="ml-64 w-full p-margin-page">
        <header class="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-outline-variant pb-6">
          <div>
            <h1 class="font-display-xl text-display-xl text-primary">我的购物车</h1>
            <p class="mt-2 text-on-surface-variant">精选学术与典藏书目</p>
          </div>
          <span v-if="!loading && !errorMsg" class="text-xs uppercase tracking-widest text-outline">
            商品数量: {{ String(items.length).padStart(2, '0') }}
          </span>
        </header>

        <div v-if="loading" class="py-24 text-center text-outline">正在加载购物车…</div>

        <div v-else-if="errorMsg" class="py-24 text-center">
          <p class="mb-4 text-error">{{ errorMsg }}</p>
          <button
            type="button"
            class="border border-primary px-6 py-2 text-primary hover:bg-primary/5"
            @click="loadCart"
          >
            重试
          </button>
        </div>

        <div v-else-if="items.length === 0" class="flex flex-col items-center justify-center py-48 text-center">
          <span class="material-symbols-outlined mb-4 text-6xl text-outline/40">shopping_cart</span>
          <h2 class="font-headline-md text-primary mb-2">您的书架暂无藏品</h2>
          <p class="mb-8 text-on-surface-variant">去发现那些值得珍藏的文字吧</p>
          <RouterLink
            to="/portal/list"
            class="border border-primary bg-primary px-12 py-4 text-sm uppercase tracking-wider text-white hover:bg-primary/90"
          >
            浏览馆藏列表
          </RouterLink>
        </div>

        <div v-else class="space-y-4">
          <article
            v-for="item in items"
            :key="item.cartItemId"
            class="flex flex-wrap items-center gap-4 border border-outline-variant bg-surface-container-lowest p-4 transition-shadow hover:shadow-md md:flex-nowrap"
          >
            <label class="flex items-center px-2">
              <input
                v-model="selectedIds"
                class="h-5 w-5 rounded-none border-outline text-primary focus:ring-secondary"
                type="checkbox"
                :value="item.bookId"
                :disabled="!canCheckout(item)"
              />
            </label>
            <RouterLink :to="bookDetailPath(item.bookId)" class="block h-36 w-24 flex-shrink-0 overflow-hidden bg-surface-container-low">
              <img
                class="h-full w-full object-cover"
                :src="resolveBookCover(item.coverUrl)"
                :alt="item.title"
                loading="lazy"
                @error="onCoverError"
              />
            </RouterLink>
            <div class="min-w-0 flex-1">
              <RouterLink :to="bookDetailPath(item.bookId)" class="font-headline-md text-lg text-primary hover:text-secondary">
                {{ item.title }}
              </RouterLink>
              <p class="mt-1 text-xs uppercase tracking-wider text-outline">{{ item.author || '未知作者' }}</p>
              <p v-if="item.status !== 'ON_SHELF'" class="mt-2 text-xs text-error">已下架，无法结算</p>
              <p v-else-if="item.stock <= 0" class="mt-2 text-xs text-error">暂时缺货</p>
              <p v-else-if="item.quantity > item.stock" class="mt-2 text-xs text-error">库存不足，请减少数量</p>
            </div>
            <div class="text-center text-on-surface-variant">¥ {{ formatPrice(item.price) }}</div>
            <div class="flex items-center border border-outline-variant">
              <button
                type="button"
                class="px-3 py-1 hover:bg-surface-container-high disabled:opacity-40"
                :disabled="busyBookId === item.bookId || item.quantity <= 1"
                @click="changeQty(item, item.quantity - 1)"
              >
                <span class="material-symbols-outlined text-sm">remove</span>
              </button>
              <span class="min-w-[2.5rem] border-x border-outline-variant px-3 text-center">{{ item.quantity }}</span>
              <button
                type="button"
                class="px-3 py-1 hover:bg-surface-container-high disabled:opacity-40"
                :disabled="busyBookId === item.bookId || item.quantity >= item.stock"
                @click="changeQty(item, item.quantity + 1)"
              >
                <span class="material-symbols-outlined text-sm">add</span>
              </button>
            </div>
            <div class="min-w-[5rem] text-right font-semibold text-primary">
              ¥ {{ formatPrice(lineTotal(item)) }}
            </div>
            <button
              type="button"
              class="text-outline hover:text-error disabled:opacity-40"
              :disabled="busyBookId === item.bookId"
              title="删除"
              @click="removeItem(item)"
            >
              <span class="material-symbols-outlined">delete</span>
            </button>
          </article>
        </div>

        <section
          v-if="!loading && !errorMsg && items.length > 0"
          class="fixed bottom-0 left-64 right-0 z-30 border-t border-outline-variant bg-surface shadow-[0_-8px_30px_-12px_rgba(44,36,32,0.12)]"
        >
          <div class="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-6 px-margin-page py-5">
            <label class="flex cursor-pointer items-center gap-3">
              <input
                v-model="selectAll"
                class="h-5 w-5 rounded-none border-outline text-primary focus:ring-secondary"
                type="checkbox"
              />
              <span class="text-xs uppercase tracking-wider text-on-surface">全选</span>
            </label>
            <div class="flex flex-wrap items-center gap-8">
              <span class="text-on-surface-variant">
                已选 <strong class="text-primary">{{ selectedCheckoutItems.length }}</strong> 件
              </span>
              <div class="text-right">
                <p class="text-xs uppercase tracking-widest text-outline">预计总额</p>
                <p class="text-2xl font-bold text-error">¥ {{ formatPrice(selectedTotal) }}</p>
              </div>
              <RouterLink
                :to="checkoutLink"
                class="bg-[#2C2420] px-10 py-4 text-sm uppercase tracking-widest text-white hover:opacity-90"
                :class="{ 'pointer-events-none opacity-40': selectedCheckoutItems.length === 0 }"
              >
                立即结算
              </RouterLink>
            </div>
          </div>
        </section>

        <p v-if="toastMsg" class="fixed bottom-24 right-8 z-50 rounded bg-[#2C2420] px-4 py-2 text-sm text-white shadow-lg">
          {{ toastMsg }}
        </p>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { formatPrice, resolveBookCover } from '../../api/portal/catalog'
import {
  userCartList,
  userCartRemove,
  userCartUpdateQuantity,
  type CartBook,
} from '../../api/user/cart'
import TopNav from './components/UserTopNav.vue'
import UserSideNav from './components/UserSideNav.vue'

const items = ref<CartBook[]>([])
const loading = ref(true)
const errorMsg = ref('')
const selectedIds = ref<number[]>([])
const busyBookId = ref<number | null>(null)
const toastMsg = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const selectableIds = computed(() => items.value.filter(canCheckout).map((i) => i.bookId))

const selectAll = computed({
  get() {
    const ids = selectableIds.value
    return ids.length > 0 && ids.every((id) => selectedIds.value.includes(id))
  },
  set(checked: boolean) {
    selectedIds.value = checked ? [...selectableIds.value] : []
  },
})

const selectedCheckoutItems = computed(() =>
  items.value.filter((i) => selectedIds.value.includes(i.bookId) && canCheckout(i)),
)

const selectedTotal = computed(() =>
  selectedCheckoutItems.value.reduce((sum, i) => sum + lineTotal(i), 0),
)

const checkoutLink = computed(() => ({
  path: '/portal/checkout',
  query: { ids: selectedCheckoutItems.value.map((i) => String(i.bookId)).join(',') },
}))

function bookDetailPath(id: number) {
  return { name: 'portal-book', params: { id: String(id) } }
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function canCheckout(item: CartBook) {
  return item.status === 'ON_SHELF' && item.stock > 0 && item.quantity <= item.stock
}

function lineTotal(item: CartBook) {
  return Number(item.price) * item.quantity
}

function showToast(msg: string) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMsg.value = ''
  }, 2200)
}

async function loadCart() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await userCartList()
    if (res.success && res.data) {
      items.value = res.data
      selectedIds.value = res.data.filter(canCheckout).map((i) => i.bookId)
    } else {
      errorMsg.value = res.message || '加载购物车失败'
      items.value = []
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    items.value = []
  } finally {
    loading.value = false
  }
}

async function changeQty(item: CartBook, qty: number) {
  if (busyBookId.value != null) return
  busyBookId.value = item.bookId
  try {
    const res = await userCartUpdateQuantity(item.bookId, qty)
    if (res.success) {
      if (qty <= 0) {
        items.value = items.value.filter((i) => i.bookId !== item.bookId)
        selectedIds.value = selectedIds.value.filter((id) => id !== item.bookId)
      } else {
        const row = items.value.find((i) => i.bookId === item.bookId)
        if (row) row.quantity = res.data?.quantity ?? qty
      }
      showToast(res.data?.message || '数量已更新')
    } else {
      showToast(res.message || '更新失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    busyBookId.value = null
  }
}

async function removeItem(item: CartBook) {
  if (busyBookId.value != null) return
  busyBookId.value = item.bookId
  try {
    const res = await userCartRemove(item.bookId)
    if (res.success) {
      items.value = items.value.filter((i) => i.bookId !== item.bookId)
      selectedIds.value = selectedIds.value.filter((id) => id !== item.bookId)
      showToast(res.data?.message || '已移除')
    } else {
      showToast(res.message || '删除失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    busyBookId.value = null
  }
}

watch(items, () => {
  const valid = new Set(items.value.map((i) => i.bookId))
  selectedIds.value = selectedIds.value.filter((id) => valid.has(id))
})

onMounted(loadCart)
</script>

<style scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24;
}
</style>
