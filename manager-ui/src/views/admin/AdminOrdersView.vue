<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  adminOrderComplete,
  adminOrderShip,
  adminOrdersList,
  orderStatusLabel,
  type AdminOrder,
} from '../../api/admin/orders'
import AdminOrderDetailDialog from './components/AdminOrderDetailDialog.vue'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'

const { nc } = useAdminNav()
const logout = useAdminLogout()
const route = useRoute()
const router = useRouter()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const orders = ref<AdminOrder[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const statusCounts = ref<Record<string, number>>({})
const paidTotalAmount = ref(0)
const loading = ref(false)
const errorMsg = ref('')
const toastMsg = ref('')
const actingId = ref<number | null>(null)

const showDetailDialog = ref(false)
const detailOrderId = ref<number | null>(null)
const detailDialogRef = ref<InstanceType<typeof AdminOrderDetailDialog> | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const statusTabs = computed(() => {
  const c = statusCounts.value
  const all = Object.values(c).reduce((s, n) => s + (n || 0), 0)
  return [
    { key: '', label: '全部订单', count: all },
    { key: 'PENDING_PAY', label: '待付款', count: c.PENDING_PAY || 0 },
    { key: 'PENDING_SHIP', label: '待发货', count: c.PENDING_SHIP || 0 },
    { key: 'SHIPPED', label: '已发货', count: c.SHIPPED || 0 },
    { key: 'DONE', label: '已完成', count: c.DONE || 0 },
    { key: 'CANCELLED', label: '已取消', count: c.CANCELLED || 0 },
  ]
})

function showToast(msg: string) {
  toastMsg.value = msg
  setTimeout(() => {
    toastMsg.value = ''
  }, 2200)
}

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

function statusBadgeClass(status: string) {
  if (status === 'PENDING_PAY') return 'border border-error/20 bg-error-container text-on-error-container'
  if (status === 'PENDING_SHIP') return 'border border-secondary/20 bg-secondary-container text-on-secondary-container'
  if (status === 'SHIPPED') return 'border border-stone-200 bg-stone-100 text-stone-600'
  if (status === 'DONE') return 'border border-stone-700 bg-stone-900 text-white'
  if (status === 'CANCELLED') return 'border border-outline-variant bg-surface-container text-outline line-through'
  return 'border border-outline-variant bg-surface-container text-on-surface-variant'
}

function customerInitials(o: AdminOrder) {
  const name = o.customerNickname || o.customerUsername || '?'
  return name.slice(0, 2).toUpperCase()
}

function canShip(o: AdminOrder) {
  return o.status === 'PENDING_SHIP'
}

function canComplete(o: AdminOrder) {
  return o.status === 'SHIPPED'
}

async function loadOrders() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminOrdersList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value,
    })
    if (res.success && res.data) {
      orders.value = res.data.list || []
      total.value = res.data.total || 0
      statusCounts.value = res.data.statusCounts || {}
      paidTotalAmount.value = Number(res.data.paidTotalAmount) || 0
    } else {
      const msg = res.message || '加载订单失败'
      if (msg.includes('未登录') || msg.includes('会话')) {
        errorMsg.value = '登录状态已过期，请重新登录'
        setTimeout(() => router.push('/admin/login'), 500)
      } else {
        errorMsg.value = msg
      }
      orders.value = []
      total.value = 0
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function applyRouteQuery() {
  const q = route.query
  keyword.value = typeof q.keyword === 'string' ? q.keyword : ''
  statusFilter.value = typeof q.status === 'string' ? q.status : ''
}

function syncQueryToRoute() {
  const query: Record<string, string> = {}
  const kw = keyword.value.trim()
  if (kw) query.keyword = kw
  if (statusFilter.value) query.status = statusFilter.value
  router.replace({ path: '/admin/orders', query })
}

function onSearch() {
  page.value = 1
  syncQueryToRoute()
  void loadOrders()
}

function setStatusTab(key: string) {
  statusFilter.value = key
  page.value = 1
  syncQueryToRoute()
  void loadOrders()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  page.value = 1
  router.replace({ path: '/admin/orders' })
  void loadOrders()
}

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  void loadOrders()
}

function openDetail(o: AdminOrder) {
  detailOrderId.value = o.id
  showDetailDialog.value = true
}

async function shipOrder(o: AdminOrder) {
  if (!window.confirm(`确认对订单 ${o.orderNo} 发货吗？`)) return
  actingId.value = o.id
  try {
    const res = await adminOrderShip(o.id)
    if (res.success) {
      showToast('已发货')
      o.status = 'SHIPPED'
      await loadOrders()
      if (showDetailDialog.value && detailOrderId.value === o.id) {
        await detailDialogRef.value?.reload()
      }
    } else {
      showToast(res.message || '发货失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

async function completeOrder(o: AdminOrder) {
  if (!window.confirm(`确认将订单 ${o.orderNo} 标记为已完成吗？`)) return
  actingId.value = o.id
  try {
    const res = await adminOrderComplete(o.id)
    if (res.success) {
      showToast('订单已完成')
      o.status = 'DONE'
      await loadOrders()
      if (showDetailDialog.value && detailOrderId.value === o.id) {
        await detailDialogRef.value?.reload()
      }
    } else {
      showToast(res.message || '操作失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

async function onDetailShip(id: number) {
  const o = orders.value.find((x) => x.id === id)
  if (o) await shipOrder(o)
}

async function onDetailComplete(id: number) {
  const o = orders.value.find((x) => x.id === id)
  if (o) await completeOrder(o)
}

onMounted(() => {
  applyRouteQuery()
  void loadOrders()
})
</script>

<template>
  <div class="bg-background text-on-background font-body-md">
    <aside
      class="fixed left-0 top-0 z-40 flex h-screen w-64 flex-col border-r border-stone-800 bg-[#2C2420] font-['Manrope'] text-sm tracking-wide shadow-2xl"
    >
      <div class="p-8">
        <h1 class="text-xl font-bold tracking-tight text-stone-50">馆藏书屋</h1>
        <p class="mt-1 text-xs text-stone-400">系统管理后台</p>
      </div>
      <nav class="flex-1 space-y-2 px-4">
        <RouterLink to="/admin/dashboard" :class="nc('/admin/dashboard')">
          <span class="material-symbols-outlined">dashboard</span>
          仪表盘
        </RouterLink>
        <RouterLink to="/admin/book-alerts" :class="nc('/admin/book-alerts')">
          <span class="material-symbols-outlined">inventory_2</span>
          图书预警
        </RouterLink>
        <RouterLink to="/admin/books" :class="nc('/admin/books')">
          <span class="material-symbols-outlined">menu_book</span>
          图书管理
        </RouterLink>
        <RouterLink to="/admin/orders" :class="nc('/admin/orders')">
          <span class="material-symbols-outlined">receipt_long</span>
          订单管理
        </RouterLink>
        <RouterLink to="/admin/users" :class="nc('/admin/users')">
          <span class="material-symbols-outlined">group</span>
          用户管理
        </RouterLink>
        <RouterLink to="/admin/categories" :class="nc('/admin/categories')">
          <span class="material-symbols-outlined">category</span>
          分类管理
        </RouterLink>
        <RouterLink to="/admin/guestbook" :class="nc('/admin/guestbook')">
          <span class="material-symbols-outlined">forum</span>
          留言管理
        </RouterLink>
      </nav>
      <div class="border-t border-stone-800 p-4">
        <button
          type="button"
          class="flex w-full items-center gap-3 px-4 py-3 text-left text-stone-400 transition-colors hover:bg-stone-800 hover:text-stone-100"
          @click="logout"
        >
          <span class="material-symbols-outlined">logout</span>
          退出登录
        </button>
      </div>
    </aside>

    <header
      class="fixed right-0 top-0 z-30 flex h-16 w-[calc(100%-16rem)] items-center justify-between border-b border-stone-200 bg-white/80 px-8 font-['Manrope'] text-sm font-medium shadow-sm backdrop-blur-md"
    >
      <span class="text-stone-600">订单管理 · shop_order / order_item</span>
      <div class="flex items-center gap-4">
        <button type="button" class="rounded-full p-2 hover:bg-stone-50" @click="showProfileDialog = true">
          <span class="material-symbols-outlined">settings</span>
        </button>
        <div class="ml-2 flex items-center gap-3 border-l border-stone-200 pl-4">
          <img class="h-8 w-8 rounded-full border border-stone-200 object-cover" :src="avatarUrl" alt="" />
          <span class="font-semibold text-on-surface">{{ displayName }}</span>
        </div>
      </div>
    </header>

    <AdminProfileDialog
      v-model="showProfileDialog"
      :display-name="displayName"
      :role-name="roleName"
      :avatar-url="avatarUrl"
      @save="updateProfile"
      @change-password="changePassword"
    />

    <AdminOrderDetailDialog
      ref="detailDialogRef"
      v-model="showDetailDialog"
      :order-id="detailOrderId"
      :acting="actingId != null"
      @ship="onDetailShip"
      @complete="onDetailComplete"
    />

    <main class="ml-64 mt-16 min-h-screen p-8">
      <div class="mx-auto max-w-container-max">
        <div class="mb-stack-lg">
          <h2 class="font-headline-lg text-headline-lg text-primary">订单管理</h2>
          <p class="mt-1 font-body-md text-on-surface-variant">
            订单号、用户、金额、状态、收货信息与明细。
          </p>
        </div>

        <div class="mb-stack-lg grid grid-cols-1 gap-4 md:grid-cols-3">
          <div class="rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
            <p class="text-sm text-on-surface-variant">已付款成交总额</p>
            <p class="mt-2 font-['Work_Sans'] text-2xl font-bold text-primary">¥ {{ formatPrice(paidTotalAmount) }}</p>
          </div>
          <div class="rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
            <p class="text-sm text-on-surface-variant">待发货</p>
            <p class="mt-2 font-['Work_Sans'] text-2xl font-bold text-secondary">
              {{ statusCounts.PENDING_SHIP || 0 }}
              <span class="text-sm font-normal text-on-surface-variant"> 笔</span>
            </p>
          </div>
          <div class="rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
            <p class="text-sm text-on-surface-variant">待付款</p>
            <p class="mt-2 font-['Work_Sans'] text-2xl font-bold text-error">
              {{ statusCounts.PENDING_PAY || 0 }}
              <span class="text-sm font-normal text-on-surface-variant"> 笔</span>
            </p>
          </div>
        </div>

        <div class="mb-stack-lg rounded-xl border border-stone-200 bg-surface-container-lowest p-1 shadow-sm">
          <div class="flex items-center gap-1 overflow-x-auto">
            <button
              v-for="tab in statusTabs"
              :key="tab.key"
              type="button"
              class="flex shrink-0 items-center gap-2 rounded-lg px-6 py-3 text-sm transition-all"
              :class="
                statusFilter === tab.key
                  ? 'bg-primary font-medium text-white shadow-md'
                  : 'text-on-surface-variant hover:bg-stone-100'
              "
              @click="setStatusTab(tab.key)"
            >
              {{ tab.label }}
              <span
                v-if="tab.count > 0"
                class="rounded-full px-1.5 py-0.5 text-[10px] font-bold"
                :class="statusFilter === tab.key ? 'bg-white/20 text-white' : 'bg-secondary-container text-on-secondary-container'"
              >
                {{ tab.count }}
              </span>
            </button>
          </div>
        </div>

        <div
          class="mb-stack-lg flex flex-wrap items-end gap-stack-md rounded-xl border border-outline-variant bg-surface-container-lowest p-stack-md shadow-sm"
        >
          <div class="min-w-[240px] flex-1">
            <label class="mb-1 block text-xs text-on-surface-variant">搜索</label>
            <input
              v-model="keyword"
              type="search"
              class="w-full border-b-2 border-outline-variant bg-surface-container-low px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="订单号、用户昵称、用户名、手机号"
              @keydown.enter="onSearch"
            />
          </div>
          <div class="w-28">
            <label class="mb-1 block text-xs text-on-surface-variant">每页</label>
            <select
              v-model.number="pageSize"
              class="w-full border-b-2 border-outline-variant bg-surface-container-low px-3 py-2 text-sm"
              @change="onSearch"
            >
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </div>
          <div class="flex gap-2 self-end">
            <button
              type="button"
              class="rounded-lg bg-secondary-container px-5 py-2 text-sm font-medium text-on-secondary-container hover:opacity-90"
              @click="onSearch"
            >
              查询
            </button>
            <button
              type="button"
              class="rounded-lg border border-outline-variant px-4 py-2 text-sm text-outline hover:bg-surface-container-low"
              @click="resetFilters"
            >
              重置
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="mb-4 rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ errorMsg }}</p>

        <div class="overflow-hidden rounded-xl border border-outline-variant bg-surface-container-lowest shadow-sm">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[900px] border-collapse text-left">
              <thead class="border-b border-outline-variant bg-surface-container-high">
                <tr>
                  <th class="px-6 py-4 text-xs uppercase tracking-wider text-on-surface-variant">订单编号</th>
                  <th class="px-6 py-4 text-xs uppercase tracking-wider text-on-surface-variant">用户</th>
                  <th class="px-6 py-4 text-xs uppercase tracking-wider text-on-surface-variant">商品数</th>
                  <th class="px-6 py-4 text-xs uppercase tracking-wider text-on-surface-variant">总金额</th>
                  <th class="px-6 py-4 text-xs uppercase tracking-wider text-on-surface-variant">状态</th>
                  <th class="px-6 py-4 text-xs uppercase tracking-wider text-on-surface-variant">下单时间</th>
                  <th class="px-6 py-4 text-right text-xs uppercase tracking-wider text-on-surface-variant">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-outline-variant">
                <tr v-if="loading">
                  <td colspan="7" class="px-6 py-12 text-center text-on-surface-variant">加载中…</td>
                </tr>
                <tr v-else-if="!orders.length">
                  <td colspan="7" class="px-6 py-12 text-center text-on-surface-variant">暂无订单</td>
                </tr>
                <tr v-for="o in orders" :key="o.id" class="transition-colors hover:bg-surface-container-low/80">
                  <td class="px-6 py-5 font-['Work_Sans'] font-medium text-primary">{{ o.orderNo }}</td>
                  <td class="px-6 py-5">
                    <div class="flex items-center gap-3">
                      <div
                        v-if="!o.customerAvatarUrl?.trim()"
                        class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary-container text-xs font-bold text-on-secondary-container"
                      >
                        {{ customerInitials(o) }}
                      </div>
                      <img
                        v-else
                        :src="o.customerAvatarUrl"
                        alt=""
                        class="h-8 w-8 shrink-0 rounded-full object-cover"
                      />
                      <div>
                        <span class="font-medium text-on-surface">{{ o.customerNickname || o.customerUsername || '—' }}</span>
                        <p v-if="o.receiverName" class="text-[11px] text-outline">收货：{{ o.receiverName }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-5 text-sm text-on-surface-variant">{{ o.itemCount ?? 0 }}</td>
                  <td class="px-6 py-5 font-['Work_Sans']">¥ {{ formatPrice(o.totalAmount) }}</td>
                  <td class="px-6 py-5">
                    <span class="inline-flex rounded-full px-3 py-1 text-xs font-bold" :class="statusBadgeClass(o.status)">
                      {{ orderStatusLabel(o.status) }}
                    </span>
                  </td>
                  <td class="px-6 py-5 text-sm text-on-surface-variant whitespace-nowrap">{{ formatTime(o.createdAt) }}</td>
                  <td class="px-6 py-5 text-right">
                    <div class="flex justify-end gap-2">
                      <button
                        type="button"
                        class="text-sm font-medium text-secondary hover:underline"
                        @click="openDetail(o)"
                      >
                        详情
                      </button>
                      <button
                        v-if="canShip(o)"
                        type="button"
                        class="rounded bg-primary px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
                        :disabled="actingId === o.id"
                        @click="shipOrder(o)"
                      >
                        {{ actingId === o.id ? '处理中…' : '发货' }}
                      </button>
                      <button
                        v-else-if="canComplete(o)"
                        type="button"
                        class="rounded bg-secondary px-4 py-1.5 text-sm font-medium text-on-secondary hover:opacity-90 disabled:opacity-50"
                        :disabled="actingId === o.id"
                        @click="completeOrder(o)"
                      >
                        {{ actingId === o.id ? '处理中…' : '完成' }}
                      </button>
                      <span v-else class="px-2 text-xs text-outline">—</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div
            class="flex flex-wrap items-center justify-between gap-4 border-t border-outline-variant bg-surface-container-low px-6 py-4"
          >
            <span class="text-sm text-on-surface-variant">
              第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条订单
            </span>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="rounded border border-outline-variant px-3 py-1 text-sm disabled:opacity-40"
                :disabled="page <= 1 || loading"
                @click="goPage(page - 1)"
              >
                上一页
              </button>
              <button
                type="button"
                class="rounded border border-outline-variant px-3 py-1 text-sm disabled:opacity-40"
                :disabled="page >= totalPages || loading"
                @click="goPage(page + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>

      <p
        v-if="toastMsg"
        class="fixed bottom-8 right-8 z-[110] rounded-lg bg-[#2C2420] px-4 py-2 text-sm text-white shadow-lg"
      >
        {{ toastMsg }}
      </p>
    </main>
  </div>
</template>
