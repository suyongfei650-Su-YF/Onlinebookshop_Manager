<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { adminDashboardStats, type DashboardStats } from '../../api/admin/dashboard'
import { resolveAdminBookCover } from '../../api/admin/books'
import { orderStatusLabel } from '../../api/admin/orders'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'
import { exportDashboardExcel } from '../../utils/adminExport'
import { adminBookAlertsReport } from '../../api/admin/bookAlerts'

const router = useRouter()
const { nc } = useAdminNav()
const logout = useAdminLogout()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const loading = ref(false)
const exporting = ref(false)
const exportToast = ref('')
const errorMsg = ref('')
const stats = ref<DashboardStats | null>(null)
const globalSearch = ref('')

const CHART_BAR_AREA_PX = 168

const salesMax = computed(() => Math.max(...salesChartData.value.map((p) => Number(p.amount) || 0), 1))
const orderMax = computed(() => Math.max(...orderChartData.value.map((p) => Number(p.orderCount) || 0), 1))
const topBookMaxSold = computed(() => Math.max(...(stats.value?.topBooks || []).map((b) => Number(b.soldQty) || 0), 1))

function buildEmpty7Days() {
  const rows: { day: string; orderCount: number; amount: number }[] = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const day = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    rows.push({ day, orderCount: 0, amount: 0 })
  }
  return rows
}

const salesChartData = computed(() => {
  const list = stats.value?.salesLast7Days
  return list?.length ? list : buildEmpty7Days()
})

const orderChartData = computed(() => {
  const list = stats.value?.orderTrend7Days
  return list?.length ? list : buildEmpty7Days()
})

function formatPrice(v: number | string | null | undefined) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function formatInt(v: number | null | undefined) {
  return v != null ? String(v) : '0'
}

/** 柱状图高度：百分比在 flex 子项上无效，改用像素 */
function barHeightPx(value: number, max: number) {
  const v = Number(value) || 0
  const m = Number(max) || 1
  if (v <= 0) return '6px'
  const px = Math.round((v / m) * CHART_BAR_AREA_PX)
  return `${Math.max(6, Math.min(CHART_BAR_AREA_PX, px))}px`
}

function stockLabel(book: { stock: number; status: string }) {
  if (book.status !== 'ON_SHELF') return '已下架'
  if (book.stock <= 0) return '缺货'
  if (book.stock < 10) return '库存低'
  return '充足'
}

function stockClass(book: { stock: number; status: string }) {
  if (book.status !== 'ON_SHELF' || book.stock <= 0) return 'bg-error-container text-on-error-container'
  if (book.stock < 10) return 'bg-amber-50 text-amber-700'
  return 'bg-green-50 text-green-700'
}

function onCoverError(e: Event) {
  ;(e.target as HTMLImageElement).src = resolveAdminBookCover(null)
}

function goPendingShip() {
  router.push({ path: '/admin/orders', query: { status: 'PENDING_SHIP' } })
}

function goPendingPay() {
  router.push({ path: '/admin/orders', query: { status: 'PENDING_PAY' } })
}

function goLowStock() {
  router.push('/admin/book-alerts')
}

function goUserSearch() {
  const kw = globalSearch.value.trim()
  if (!kw) return
  router.push({ path: '/admin/users', query: { keyword: kw } })
}

function goBooks() {
  router.push('/admin/books')
}

async function loadDashboard() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminDashboardStats()
    if (res.success && res.data) stats.value = res.data
    else {
      const msg = res.message || '加载仪表盘失败'
      errorMsg.value = msg.includes('未登录')
        ? `${msg}，请先打开 /admin/login 使用 admin / admin123 登录`
        : msg
      stats.value = null
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    stats.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadDashboard())

function showExportToast(msg: string) {
  exportToast.value = msg
  setTimeout(() => {
    exportToast.value = ''
  }, 2200)
}

function exportDashboard() {
  if (!stats.value) {
    showExportToast('暂无数据可导出，请先刷新')
    return
  }
  exporting.value = true
  void (async () => {
    try {
      let alertReport = null
      try {
        const alertRes = await adminBookAlertsReport()
        if (alertRes.success && alertRes.data) alertReport = alertRes.data
      } catch {
        /* 预警数据可选 */
      }
      exportDashboardExcel(stats.value!, alertReport)
      showExportToast(
        alertReport
          ? '已导出仪表盘及补货/断货明细 Excel'
          : '仪表盘已导出（未获取到预警明细，请确认后端已重启）',
      )
    } catch {
      showExportToast('导出失败，请重试')
    } finally {
      exporting.value = false
    }
  })()
}
</script>

<template>
  <div class="font-body-md">
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

    <header class="fixed right-0 top-0 z-30 flex h-16 w-[calc(100%-16rem)] items-center justify-between border-b border-stone-200 bg-white/80 px-8 backdrop-blur-md">
      <div class="flex items-center gap-2 text-stone-500 text-sm">
        <RouterLink to="/admin/dashboard" class="hover:text-[#2C2420]">首页</RouterLink>
        <span class="material-symbols-outlined text-base">chevron_right</span>
        <span class="border-b-2 border-[#D4AF37] pb-1 text-[#D4AF37]">仪表盘</span>
      </div>
      <div class="flex items-center gap-4">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-stone-400 text-lg">search</span>
          <input
            v-model="globalSearch"
            type="search"
            class="w-56 rounded-full border-none bg-stone-100 py-2 pl-10 pr-4 text-xs focus:ring-1 focus:ring-secondary/30"
            placeholder="搜索用户（昵称/手机/用户名）"
            @keydown.enter="goUserSearch"
          />
        </div>
        <button type="button" class="rounded-full p-2 hover:bg-stone-50" @click="showProfileDialog = true">
          <span class="material-symbols-outlined">settings</span>
        </button>
        <img class="h-9 w-9 rounded-full border object-cover" :src="avatarUrl" alt="" />
        <span class="text-sm font-semibold">{{ displayName }}</span>
      </div>
    </header>

    <AdminProfileDialog v-model="showProfileDialog" :display-name="displayName" :role-name="roleName" :avatar-url="avatarUrl" @save="updateProfile" @change-password="changePassword" />

    <main class="ml-64 min-h-screen px-8 pb-16 pt-24">
      <div class="mx-auto max-w-container-max space-y-8">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 class="font-headline-lg text-2xl text-primary">运营仪表盘</h2>
            <p class="mt-1 text-sm text-stone-500">数据来自 shop_order、customer、book、order_item 表</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="rounded-lg border border-stone-200 px-4 py-2 text-sm hover:bg-stone-50 disabled:opacity-50"
              :disabled="loading || exporting || !stats"
              @click="exportDashboard"
            >
              <span class="material-symbols-outlined align-middle text-base">download</span>
              {{ exporting ? '导出中…' : '导出 Excel' }}
            </button>
            <button type="button" class="rounded-lg border border-stone-200 px-4 py-2 text-sm hover:bg-stone-50" :disabled="loading" @click="loadDashboard">
              <span class="material-symbols-outlined align-middle text-base">refresh</span> 刷新
            </button>
          </div>
        </div>

        <p
          v-if="exportToast"
          class="fixed bottom-8 left-1/2 z-50 -translate-x-1/2 rounded-full bg-stone-900 px-6 py-2 text-sm text-white shadow-lg"
        >
          {{ exportToast }}
        </p>

        <p v-if="errorMsg" class="rounded-lg bg-error-container px-3 py-2 text-sm">{{ errorMsg }}</p>
        <p v-if="loading && !stats" class="text-sm text-stone-500">正在加载…</p>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <button type="button" class="rounded-xl border bg-white p-6 text-left shadow-sm hover:shadow-md" @click="router.push('/admin/orders')">
            <span class="material-symbols-outlined text-secondary">shopping_cart</span>
            <h3 class="mt-3 text-xs uppercase tracking-wider text-stone-500">今日订单</h3>
            <p class="text-3xl font-bold text-primary">{{ formatInt(stats?.todayOrders) }}</p>
          </button>
          <button type="button" class="rounded-xl border bg-white p-6 text-left shadow-sm hover:shadow-md" @click="router.push('/admin/orders')">
            <span class="material-symbols-outlined text-secondary">payments</span>
            <h3 class="mt-3 text-xs uppercase tracking-wider text-stone-500">今日销售额</h3>
            <p class="text-3xl font-bold text-primary">¥ {{ formatPrice(stats?.todayAmount) }}</p>
          </button>
          <button type="button" class="rounded-xl border bg-white p-6 text-left shadow-sm hover:shadow-md" @click="router.push('/admin/users')">
            <span class="material-symbols-outlined text-secondary">person_add</span>
            <h3 class="mt-3 text-xs uppercase tracking-wider text-stone-500">今日新增用户</h3>
            <p class="text-3xl font-bold text-primary">{{ formatInt(stats?.newCustomersToday) }}</p>
          </button>
          <button type="button" class="rounded-xl border bg-white p-6 text-left shadow-sm hover:shadow-md" @click="goLowStock">
            <span class="material-symbols-outlined text-error">warning</span>
            <h3 class="mt-3 text-xs uppercase tracking-wider text-stone-500">库存预警 (&lt;10)</h3>
            <p class="text-3xl font-bold text-primary">{{ formatInt(stats?.lowStockCount) }}</p>
          </button>
        </div>

        <div class="grid grid-cols-2 gap-3 md:grid-cols-4">
          <div class="rounded-lg border bg-white px-4 py-3 text-sm"><p class="text-stone-500">图书</p><p class="text-xl font-bold">{{ formatInt(stats?.totalBooks) }}</p></div>
          <div class="rounded-lg border bg-white px-4 py-3 text-sm"><p class="text-stone-500">订单</p><p class="text-xl font-bold">{{ formatInt(stats?.totalOrders) }}</p></div>
          <button type="button" class="rounded-lg border bg-white px-4 py-3 text-left text-sm hover:border-secondary" @click="goPendingShip"><p class="text-stone-500">待发货</p><p class="text-xl font-bold text-secondary">{{ formatInt(stats?.pendingShipCount) }}</p></button>
          <button type="button" class="rounded-lg border bg-white px-4 py-3 text-left text-sm hover:border-error" @click="goPendingPay"><p class="text-stone-500">待付款</p><p class="text-xl font-bold text-error">{{ formatInt(stats?.pendingPayCount) }}</p></button>
        </div>

        <div class="grid grid-cols-12 gap-6">
          <div class="col-span-12 rounded-xl border bg-white p-6 shadow-sm lg:col-span-8">
            <h2 class="font-headline-md text-lg text-primary">近 7 日销售额</h2>
            <p class="mb-6 text-sm text-stone-500">累计成交 ¥ {{ formatPrice(stats?.orderAmountSum) }}</p>
            <div v-if="stats" class="relative h-48">
              <div class="absolute inset-x-0 bottom-6 top-0 flex items-end justify-between gap-2">
                <div
                  v-for="p in salesChartData"
                  :key="p.day"
                  class="flex h-full min-w-0 flex-1 flex-col items-center justify-end"
                >
                  <div
                    class="w-full max-w-10 rounded-t bg-secondary transition-all"
                    :style="{ height: barHeightPx(Number(p.amount) || 0, salesMax) }"
                    :title="`¥${formatPrice(p.amount)}`"
                  />
                </div>
              </div>
              <div class="absolute inset-x-0 bottom-0 flex justify-between gap-2">
                <span v-for="p in salesChartData" :key="'s-' + p.day" class="min-w-0 flex-1 text-center text-[10px] text-stone-400">{{ p.day }}</span>
              </div>
            </div>
            <p v-else class="py-12 text-center text-sm text-stone-400">加载中…</p>
          </div>
          <div class="col-span-12 rounded-xl border bg-white p-6 shadow-sm lg:col-span-4">
            <h2 class="font-headline-md text-lg text-primary">近 7 日订单量</h2>
            <p class="mb-6 text-sm text-stone-500">按日统计订单笔数</p>
            <div v-if="stats" class="relative h-48">
              <div class="absolute inset-x-0 bottom-6 top-0 flex items-end justify-between gap-2">
                <div
                  v-for="p in orderChartData"
                  :key="'o-' + p.day"
                  class="flex h-full min-w-0 flex-1 flex-col items-center justify-end"
                >
                  <div
                    class="w-full max-w-10 rounded-t bg-stone-300 transition-all hover:bg-secondary/60"
                    :style="{ height: barHeightPx(Number(p.orderCount) || 0, orderMax) }"
                    :title="`${p.orderCount ?? 0} 笔`"
                  />
                </div>
              </div>
              <div class="absolute inset-x-0 bottom-0 flex justify-between gap-2">
                <span v-for="p in orderChartData" :key="'ol-' + p.day" class="min-w-0 flex-1 text-center text-[10px] text-stone-400">{{ p.day }}</span>
              </div>
            </div>
            <p v-else class="py-12 text-center text-sm text-stone-400">加载中…</p>
          </div>
        </div>

        <section class="overflow-hidden rounded-xl border bg-white shadow-sm">
          <div class="flex items-center justify-between border-b p-6">
            <div>
              <h2 class="text-lg font-semibold text-primary">畅销图书 Top 5</h2>
              <p class="text-sm text-stone-500">按 order_item 累计销量（已付款订单）</p>
            </div>
            <button type="button" class="text-sm text-secondary hover:underline" @click="goBooks">图书管理 →</button>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[640px] text-left text-sm">
              <thead class="bg-stone-50 text-xs uppercase text-stone-500">
                <tr>
                  <th class="px-6 py-3">#</th>
                  <th class="px-6 py-3">图书</th>
                  <th class="px-6 py-3">分类</th>
                  <th class="px-6 py-3">单价</th>
                  <th class="px-6 py-3">销量</th>
                  <th class="px-6 py-3">库存</th>
                </tr>
              </thead>
              <tbody class="divide-y">
                <tr v-if="!stats?.topBooks?.length">
                  <td colspan="6" class="px-6 py-10 text-center text-stone-400">暂无销量数据</td>
                </tr>
                <tr v-for="(b, idx) in stats?.topBooks || []" :key="b.bookId" class="hover:bg-stone-50/50">
                  <td class="px-6 py-4">
                    <span class="flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold" :class="idx === 0 ? 'bg-secondary text-white' : 'bg-stone-200'">{{ idx + 1 }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <img class="h-14 w-10 rounded object-cover" :src="resolveAdminBookCover(b.coverUrl)" :alt="b.title" @error="onCoverError" />
                      <div>
                        <p class="font-medium">{{ b.title }}</p>
                        <p class="text-xs text-stone-500">{{ b.author || '—' }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-stone-600">{{ b.categoryName || '未分类' }}</td>
                  <td class="px-6 py-4">¥ {{ formatPrice(b.price) }}</td>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-2">
                      <div class="h-1.5 w-20 overflow-hidden rounded-full bg-stone-100">
                        <div class="h-full bg-secondary" :style="{ width: `${Math.round(((b.soldQty || 0) / topBookMaxSold) * 100)}%` }" />
                      </div>
                      <span class="font-bold">{{ b.soldQty }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span class="rounded-full px-2 py-0.5 text-[10px] font-bold" :class="stockClass(b)">{{ stockLabel(b) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="stats?.recentOrders?.length" class="rounded-xl border bg-white p-6 shadow-sm">
          <div class="mb-4 flex items-center justify-between">
            <h2 class="text-lg font-semibold text-primary">最近订单</h2>
            <RouterLink to="/admin/orders" class="text-sm text-secondary hover:underline">查看全部</RouterLink>
          </div>
          <ul class="divide-y">
            <li v-for="o in stats.recentOrders" :key="o.id" class="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
              <span class="font-medium text-primary">{{ o.orderNo }}</span>
              <span class="text-stone-500">{{ o.customerNickname || o.customerUsername }}</span>
              <span>{{ orderStatusLabel(o.status) }}</span>
              <span class="font-semibold text-secondary">¥ {{ formatPrice(o.totalAmount) }}</span>
            </li>
          </ul>
        </section>
      </div>
    </main>
  </div>
</template>
