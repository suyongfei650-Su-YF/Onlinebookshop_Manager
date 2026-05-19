<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4"
    @click.self="close"
  >
    <div
      class="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl border border-outline-variant bg-surface-container-lowest shadow-2xl"
      role="dialog"
      aria-modal="true"
    >
      <header class="sticky top-0 z-10 flex items-center justify-between border-b border-outline-variant bg-surface-container-lowest px-6 py-4">
        <div>
          <h3 class="font-headline-md text-lg text-primary">订单详情</h3>
          <p v-if="order" class="mt-1 text-xs text-outline">{{ order.orderNo }}</p>
        </div>
        <button type="button" class="rounded-full p-2 text-outline hover:bg-surface-container-high" @click="close">
          <span class="material-symbols-outlined">close</span>
        </button>
      </header>

      <div v-if="loading" class="p-12 text-center text-on-surface-variant">加载中…</div>
      <p v-else-if="errorMsg" class="m-6 rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ errorMsg }}</p>

      <template v-else-if="order">
        <div class="space-y-6 p-6">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-xs text-on-surface-variant">下单用户</p>
              <p class="mt-1 font-medium text-on-surface">
                {{ order.customerNickname || order.customerUsername || '—' }}
                <span v-if="order.customerUsername" class="text-sm text-outline">（{{ order.customerUsername }}）</span>
              </p>
            </div>
            <div class="text-right">
              <span class="inline-flex rounded-full px-3 py-1 text-xs font-bold" :class="statusBadgeClass(order.status)">
                {{ orderStatusLabel(order.status) }}
              </span>
              <p class="mt-2 font-['Work_Sans'] text-xl font-bold text-secondary">¥ {{ formatPrice(order.totalAmount) }}</p>
            </div>
          </div>

          <div class="grid gap-4 rounded-lg border border-outline-variant bg-surface-container-low p-4 sm:grid-cols-2">
            <div>
              <p class="text-xs text-on-surface-variant">下单时间</p>
              <p class="mt-1 text-sm">{{ formatTime(order.createdAt) }}</p>
            </div>
            <div>
              <p class="text-xs text-on-surface-variant">支付方式</p>
              <p class="mt-1 text-sm">{{ paymentMethodLabel(order.paymentMethod) }}</p>
            </div>
            <div class="sm:col-span-2">
              <p class="text-xs text-on-surface-variant">收货信息</p>
              <p class="mt-1 text-sm text-on-surface">
                {{ order.receiverName || '—' }}
                <span v-if="order.receiverPhone" class="text-outline"> · {{ order.receiverPhone }}</span>
              </p>
              <p class="mt-1 text-sm text-on-surface-variant">{{ order.shippingAddress || '未填写' }}</p>
            </div>
          </div>

          <div>
            <h4 class="mb-3 text-sm font-medium text-primary">商品明细（{{ order.items?.length || 0 }} 件）</h4>
            <ul class="divide-y divide-outline-variant rounded-lg border border-outline-variant">
              <li
                v-for="item in order.items"
                :key="`${order.id}-${item.bookId}`"
                class="flex flex-wrap items-center gap-4 p-4"
              >
                <img
                  class="h-20 w-14 shrink-0 rounded border border-outline-variant object-cover bg-surface-container"
                  :src="resolveAdminBookCover(item.coverUrl)"
                  :alt="item.title"
                  @error="onCoverError"
                />
                <div class="min-w-0 flex-1">
                  <p class="font-medium text-on-surface">{{ item.title }}</p>
                  <p class="text-xs text-outline">{{ item.author || '未知作者' }}</p>
                  <p class="mt-1 text-xs text-on-surface-variant">
                    ¥ {{ formatPrice(item.unitPrice) }} × {{ item.quantity }}
                  </p>
                </div>
                <p class="shrink-0 font-medium text-primary">¥ {{ formatPrice(lineAmount(item)) }}</p>
              </li>
              <li v-if="!order.items?.length" class="p-6 text-center text-sm text-outline">暂无明细</li>
            </ul>
          </div>
        </div>

        <footer class="flex flex-wrap justify-end gap-3 border-t border-outline-variant px-6 py-4">
          <button
            type="button"
            class="rounded-lg border border-outline-variant px-5 py-2 text-sm text-on-surface hover:bg-surface-container-low"
            @click="close"
          >
            关闭
          </button>
          <button
            v-if="order.status === 'PENDING_SHIP'"
            type="button"
            class="rounded-lg bg-primary px-5 py-2 text-sm font-medium text-on-primary hover:opacity-90 disabled:opacity-50"
            :disabled="acting"
            @click="emitShip"
          >
            {{ acting ? '处理中…' : '确认发货' }}
          </button>
          <button
            v-if="order.status === 'SHIPPED'"
            type="button"
            class="rounded-lg bg-secondary px-5 py-2 text-sm font-medium text-on-secondary hover:opacity-90 disabled:opacity-50"
            :disabled="acting"
            @click="emitComplete"
          >
            {{ acting ? '处理中…' : '标记完成' }}
          </button>
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { resolveAdminBookCover } from '../../../api/admin/books'
import {
  adminOrderDetail,
  orderStatusLabel,
  paymentMethodLabel,
  type AdminOrderDetail,
  type AdminOrderItem,
} from '../../../api/admin/orders'

const props = defineProps<{
  modelValue: boolean
  orderId: number | null
  acting?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  ship: [number]
  complete: [number]
}>()

const order = ref<AdminOrderDetail | null>(null)
const loading = ref(false)
const errorMsg = ref('')

function formatPrice(v: number | string | null | undefined) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function formatTime(raw?: string | null) {
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

function lineAmount(item: AdminOrderItem) {
  return Number(item.unitPrice) * Number(item.quantity)
}

function statusBadgeClass(status: string) {
  if (status === 'PENDING_PAY') return 'border border-error/20 bg-error-container text-on-error-container'
  if (status === 'PENDING_SHIP') return 'border border-secondary/20 bg-secondary-container text-on-secondary-container'
  if (status === 'SHIPPED') return 'border border-stone-200 bg-stone-100 text-stone-600'
  if (status === 'DONE') return 'border border-stone-700 bg-stone-900 text-white'
  if (status === 'CANCELLED') return 'border border-outline-variant bg-surface-container text-outline'
  return 'border border-outline-variant bg-surface-container text-on-surface-variant'
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveAdminBookCover(null)
}

function close() {
  emit('update:modelValue', false)
}

function emitShip() {
  if (order.value) emit('ship', order.value.id)
}

function emitComplete() {
  if (order.value) emit('complete', order.value.id)
}

async function loadDetail() {
  if (props.orderId == null) return
  loading.value = true
  errorMsg.value = ''
  order.value = null
  try {
    const res = await adminOrderDetail(props.orderId)
    if (res.success && res.data) {
      order.value = { ...res.data, items: res.data.items || [] }
    } else {
      errorMsg.value = res.message || '加载订单详情失败'
    }
  } catch {
    errorMsg.value = '无法连接服务器'
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.orderId] as const,
  ([open, id]) => {
    if (open && id != null) void loadDetail()
  },
)

defineExpose({ reload: loadDetail, order })
</script>
