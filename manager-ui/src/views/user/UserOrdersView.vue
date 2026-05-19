<template>
  <div class="font-body-md min-h-screen bg-background">
    <TopNav />
    <div class="flex pt-16">
      <UserSideNav active="orders" />
      <main class="ml-64 w-full p-margin-page">
        <header class="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-outline-variant pb-6">
          <div>
            <h1 class="font-headline-lg text-headline-lg text-primary">我的订单</h1>
            <p class="mt-2 text-on-surface-variant">在此查看并管理您的书屋订单记录与配送状态。</p>
          </div>
          <span v-if="!loading && !errorMsg" class="text-xs uppercase tracking-widest text-outline">
            共 {{ orders.length }} 笔订单
          </span>
        </header>

        <div v-if="!loading && !errorMsg && orders.length" class="mb-8 flex flex-wrap gap-2">
          <button
            v-for="tab in statusTabs"
            :key="tab.key"
            type="button"
            class="px-4 py-2 text-xs uppercase tracking-wider border transition-colors"
            :class="
              statusFilter === tab.key
                ? 'border-secondary bg-secondary-container/30 text-secondary'
                : 'border-outline-variant text-outline hover:border-primary hover:text-primary'
            "
            @click="statusFilter = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.count != null" class="ml-1 opacity-70">({{ tab.count }})</span>
          </button>
        </div>

        <div v-if="loading" class="py-24 text-center text-outline">正在加载订单…</div>

        <div v-else-if="errorMsg" class="py-24 text-center">
          <p class="mb-4 text-error">{{ errorMsg }}</p>
          <button
            type="button"
            class="border border-primary px-6 py-2 text-primary hover:bg-primary/5"
            @click="loadOrders"
          >
            重试
          </button>
        </div>

        <div v-else-if="orders.length === 0" class="py-24 text-center">
          <span class="material-symbols-outlined mb-4 text-5xl text-outline/40">receipt_long</span>
          <p class="text-on-surface-variant">暂无订单记录</p>
          <RouterLink
            to="/portal/list"
            class="mt-6 inline-block border border-secondary px-6 py-2.5 text-sm text-secondary hover:bg-secondary-container/20"
          >
            去选购图书
          </RouterLink>
        </div>

        <div v-else-if="filteredOrders.length === 0" class="py-16 text-center text-outline">
          当前筛选下没有订单
        </div>

        <div v-else class="space-y-6">
          <article
            v-for="order in filteredOrders"
            :key="order.id"
            class="border border-outline-variant bg-surface-container-lowest"
          >
            <div
              class="flex flex-wrap items-center justify-between gap-4 border-b border-outline-variant/60 bg-surface-container-low px-gutter py-4"
            >
              <div class="min-w-0 space-y-1">
                <p class="font-medium text-primary">{{ order.orderNo }}</p>
                <p class="text-xs text-outline">下单时间：{{ formatOrderTime(order.createdAt) }}</p>
              </div>
              <div class="flex flex-wrap items-center gap-3">
                <span class="text-xs uppercase tracking-wider" :class="statusClass(order.status)">
                  {{ orderStatusLabel(order.status) }}
                </span>
                <span class="font-semibold text-secondary">¥ {{ formatPrice(order.totalAmount) }}</span>
              </div>
            </div>

            <ul class="divide-y divide-outline-variant/40">
              <li
                v-for="item in order.items"
                :key="`${order.id}-${item.bookId}`"
                class="flex flex-wrap items-center gap-4 px-gutter py-4"
              >
                <RouterLink
                  :to="{ name: 'portal-book', params: { id: String(item.bookId) } }"
                  class="flex min-w-0 flex-1 items-center gap-4 hover:opacity-90"
                >
                  <img
                    class="h-20 w-14 shrink-0 object-cover bg-surface-container-low"
                    :src="resolveBookCover(item.coverUrl)"
                    :alt="item.title"
                    loading="lazy"
                    @error="onCoverError"
                  />
                  <div class="min-w-0">
                    <h3 class="line-clamp-2 font-headline-md text-sm text-primary">{{ item.title }}</h3>
                    <p class="mt-1 text-xs text-outline">{{ item.author || '未知作者' }}</p>
                    <p class="mt-1 text-xs text-outline-variant">
                      ¥ {{ formatPrice(item.unitPrice) }} × {{ item.quantity }}
                    </p>
                  </div>
                </RouterLink>
                <p class="shrink-0 font-medium text-primary">¥ {{ formatPrice(lineAmount(item)) }}</p>
              </li>
              <li v-if="!order.items.length" class="px-gutter py-6 text-center text-sm text-outline">
                暂无明细（请联系客服）
              </li>
            </ul>

            <div class="flex flex-wrap justify-end gap-3 border-t border-outline-variant/60 px-gutter py-4">
              <button
                v-if="order.status === 'PENDING_PAY'"
                type="button"
                class="border border-secondary px-5 py-2 text-sm text-secondary hover:bg-secondary-container/20"
                :disabled="payingId === order.id"
                @click="payOrder(order)"
              >
                {{ payingId === order.id ? '支付中…' : '去付款' }}
              </button>
              <button
                v-if="order.status === 'PENDING_PAY'"
                type="button"
                class="border border-outline-variant px-5 py-2 text-sm text-outline hover:border-error hover:text-error disabled:opacity-50"
                :disabled="cancellingId === order.id || payingId === order.id"
                @click="cancelOrder(order)"
              >
                {{ cancellingId === order.id ? '取消中…' : '取消订单' }}
              </button>
            </div>
          </article>
        </div>

        <p v-if="toastMsg" class="fixed bottom-8 right-8 z-50 rounded bg-[#2C2420] px-4 py-2 text-sm text-white shadow-lg">
          {{ toastMsg }}
        </p>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { formatPrice, resolveBookCover } from '../../api/portal/catalog'
import {
  orderStatusLabel,
  userOrderCancel,
  userOrderPay,
  userOrdersList,
  type UserOrder,
  type UserOrderItem,
} from '../../api/user/orders'
import TopNav from './components/UserTopNav.vue'
import UserSideNav from './components/UserSideNav.vue'

const route = useRoute()
const orders = ref<UserOrder[]>([])
const payingId = ref<number | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const statusFilter = ref('ALL')
const cancellingId = ref<number | null>(null)
const toastMsg = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const statusTabs = computed(() => {
  const counts: Record<string, number> = {}
  for (const o of orders.value) {
    counts[o.status] = (counts[o.status] || 0) + 1
  }
  return [
    { key: 'ALL', label: '全部', count: orders.value.length },
    { key: 'PENDING_PAY', label: '待付款', count: counts.PENDING_PAY || 0 },
    { key: 'PENDING_SHIP', label: '待发货', count: counts.PENDING_SHIP || 0 },
    { key: 'SHIPPED', label: '已发货', count: counts.SHIPPED || 0 },
    { key: 'DONE', label: '已完成', count: counts.DONE || 0 },
    { key: 'CANCELLED', label: '已取消', count: counts.CANCELLED || 0 },
  ]
})

const filteredOrders = computed(() => {
  if (statusFilter.value === 'ALL') return orders.value
  return orders.value.filter((o) => o.status === statusFilter.value)
})

function lineAmount(item: UserOrderItem) {
  return Number(item.unitPrice) * Number(item.quantity)
}

function formatOrderTime(raw?: string | null) {
  if (!raw) return '—'
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function statusClass(status: string) {
  if (status === 'PENDING_PAY') return 'text-error'
  if (status === 'PENDING_SHIP') return 'text-secondary'
  if (status === 'SHIPPED') return 'text-primary'
  if (status === 'DONE') return 'text-outline'
  if (status === 'CANCELLED') return 'text-outline line-through'
  return 'text-outline'
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function showToast(msg: string) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMsg.value = ''
  }, 2200)
}

async function payOrder(order: UserOrder) {
  if (payingId.value != null) return
  payingId.value = order.id
  try {
    const res = await userOrderPay(order.id)
    if (res.success) {
      order.status = res.data?.status || 'PENDING_SHIP'
      showToast(res.data?.message || '支付成功')
    } else {
      showToast(res.message || '支付失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    payingId.value = null
  }
}

async function loadOrders() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await userOrdersList()
    if (res.success && res.data) {
      orders.value = res.data.map((o) => ({ ...o, items: o.items || [] }))
    } else {
      errorMsg.value = res.message || '加载订单失败'
      orders.value = []
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function cancelOrder(order: UserOrder) {
  if (cancellingId.value != null) return
  if (!window.confirm(`确定取消订单 ${order.orderNo} 吗？`)) return
  cancellingId.value = order.id
  try {
    const res = await userOrderCancel(order.id)
    if (res.success) {
      order.status = 'CANCELLED'
      showToast(res.data?.message || '订单已取消')
    } else {
      showToast(res.message || '取消失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    cancellingId.value = null
  }
}

onMounted(async () => {
  await loadOrders()
  if (route.query.placed) {
    showToast('订单已提交，可在下方查看')
  }
})
</script>
