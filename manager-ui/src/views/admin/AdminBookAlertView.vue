<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  adminBookAlertsAiAnalyze,
  adminBookAlertsReport,
  type BookAlertBook,
  type BookAlertReport,
  type PieSlice,
} from '../../api/admin/bookAlerts'
import { resolveAdminBookCover } from '../../api/admin/books'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'
import { exportBookAlertsExcel } from '../../utils/adminExport'

const DAILY_KEY = 'bookshop_admin_alert_seen_date'

const { nc } = useAdminNav()
const logout = useAdminLogout()
const router = useRouter()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const loading = ref(false)
const exporting = ref(false)
const exportToast = ref('')
const aiLoading = ref(false)
const errorMsg = ref('')
const report = ref<BookAlertReport | null>(null)
const aiAnalysis = ref('')
const aiHint = ref('')
const aiEnabled = ref(false)
const showDailyBanner = ref(false)

const stockPieRef = ref<HTMLDivElement | null>(null)
const alertPieRef = ref<HTMLDivElement | null>(null)
const categoryPieRef = ref<HTMLDivElement | null>(null)
const categoryBarRef = ref<HTMLDivElement | null>(null)

let stockChart: ECharts | null = null
let alertChart: ECharts | null = null
let categoryPieChart: ECharts | null = null
let categoryBarChart: ECharts | null = null

const PIE_COLORS = ['#2C2420', '#D4AF37', '#8b6914', '#c4a35a', '#6b7280', '#b45309', '#047857', '#7c3aed']

function pieOption(title: string, data: PieSlice[]) {
  return {
    color: PIE_COLORS,
    title: { text: title, left: 'center', top: 8, textStyle: { fontSize: 13, fontWeight: 600, color: '#44403c' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['38%', '62%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: data.map((d) => ({ name: d.name, value: Number(d.value) || 0 })),
      },
    ],
  }
}

function barOption(r: BookAlertReport) {
  const names = r.categoryBarChart.map((x) => x.categoryName)
  return {
    color: ['#D4AF37', '#2C2420', '#8b6914'],
    title: { text: '分类销量 vs 书目数量 vs 库存', left: 'center', top: 8, textStyle: { fontSize: 13, fontWeight: 600 } },
    tooltip: { trigger: 'axis' },
    legend: { top: 32, data: ['销量(册)', '在售种类', '库存合计'] },
    grid: { left: 48, right: 24, top: 72, bottom: 48 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 28, fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [
      { name: '销量(册)', type: 'bar', data: r.categoryBarChart.map((x) => x.soldQty), barMaxWidth: 28 },
      { name: '在售种类', type: 'bar', data: r.categoryBarChart.map((x) => x.bookCount), barMaxWidth: 28 },
      { name: '库存合计', type: 'bar', data: r.categoryBarChart.map((x) => x.stockTotal), barMaxWidth: 28 },
    ],
  }
}

function disposeCharts() {
  stockChart?.dispose()
  alertChart?.dispose()
  categoryPieChart?.dispose()
  categoryBarChart?.dispose()
  stockChart = alertChart = categoryPieChart = categoryBarChart = null
}

function renderCharts() {
  const r = report.value
  if (!r) return
  if (stockPieRef.value) {
    stockChart?.dispose()
    stockChart = echarts.init(stockPieRef.value)
    stockChart.setOption(pieOption('库存状态分布', r.stockStatusPie))
  }
  if (alertPieRef.value) {
    alertChart?.dispose()
    alertChart = echarts.init(alertPieRef.value)
    alertChart.setOption(pieOption('预警类型占比', r.alertTypePie))
  }
  if (categoryPieRef.value) {
    categoryPieChart?.dispose()
    categoryPieChart = echarts.init(categoryPieRef.value)
    categoryPieChart.setOption(pieOption('分类销量占比', r.categorySalesPie))
  }
  if (categoryBarRef.value) {
    categoryBarChart?.dispose()
    categoryBarChart = echarts.init(categoryBarRef.value)
    categoryBarChart.setOption(barOption(r))
  }
}

function onResize() {
  stockChart?.resize()
  alertChart?.resize()
  categoryPieChart?.resize()
  categoryBarChart?.resize()
}

function checkDailyBanner() {
  const today = report.value?.date || new Date().toISOString().slice(0, 10)
  const seen = localStorage.getItem(DAILY_KEY)
  showDailyBanner.value = seen !== today && (report.value?.counts?.alertTotal ?? 0) > 0
}

function dismissDailyBanner() {
  if (report.value?.date) {
    localStorage.setItem(DAILY_KEY, report.value.date)
  }
  showDailyBanner.value = false
}

async function loadReport() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminBookAlertsReport()
    if (res.success && res.data) {
      report.value = res.data
      if (res.data.ruleAnalysis) {
        aiAnalysis.value = res.data.ruleAnalysis
      }
      checkDailyBanner()
      await nextTick()
      renderCharts()
    } else {
      const msg = res.message || '加载预警数据失败'
      if (msg.includes('未登录') || msg.includes('会话')) {
        void router.push({ path: '/admin/login', query: { redirect: '/admin/book-alerts' } })
        return
      }
      errorMsg.value = msg
    }
  } catch {
    errorMsg.value = '网络异常，请确认后端已启动并已 Maven 编译 Java8 后重启'
  } finally {
    loading.value = false
  }
}

async function runAiAnalyze() {
  aiLoading.value = true
  aiHint.value = ''
  try {
    const res = await adminBookAlertsAiAnalyze()
    if (res.success && res.data) {
      report.value = res.data.report
      aiAnalysis.value = res.data.analysis || ''
      aiHint.value = res.data.hint || ''
      aiEnabled.value = !!res.data.aiEnabled
      await nextTick()
      renderCharts()
    } else {
      aiHint.value = res.message || 'AI 分析失败'
    }
  } catch {
    aiHint.value = '网络异常'
  } finally {
    aiLoading.value = false
  }
}

function coverSrc(b: BookAlertBook) {
  return resolveAdminBookCover(b.coverUrl)
}

function goEditBook(id: number) {
  void router.push({ path: '/admin/books', query: { highlight: String(id) } })
}

function showExportToast(msg: string) {
  exportToast.value = msg
  setTimeout(() => {
    exportToast.value = ''
  }, 2200)
}

function exportReport() {
  if (!report.value) {
    showExportToast('暂无数据可导出，请先刷新')
    return
  }
  exporting.value = true
  try {
    exportBookAlertsExcel(report.value, aiAnalysis.value)
    showExportToast('已导出详细预警清单（含断货/补货建议）')
  } catch {
    showExportToast('导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

watch(report, () => {
  void nextTick(() => renderCharts())
})

onMounted(() => {
  window.addEventListener('resize', onResize)
  void loadReport()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
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
      <span class="text-stone-600">图书预警 · 库存与销售智能分析</span>
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

    <main class="ml-64 min-h-screen bg-stone-50 px-8 pb-12 pt-24">
      <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="text-2xl font-bold text-stone-900">图书预警中心</h2>
          <p class="mt-1 text-sm text-stone-500">
            {{ report?.dateLabel || '今日' }} · 售空补货、畅销加印、滞销减量、品类扩充
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-xl border border-stone-300 bg-white px-4 py-2 text-sm text-stone-700 hover:bg-stone-50 disabled:opacity-50"
            :disabled="loading || exporting || !report"
            @click="exportReport"
          >
            <span class="material-symbols-outlined align-middle text-base">download</span>
            {{ exporting ? '导出中…' : '导出 Excel' }}
          </button>
          <button
            type="button"
            class="rounded-xl border border-stone-300 bg-white px-4 py-2 text-sm text-stone-700 hover:bg-stone-50 disabled:opacity-50"
            :disabled="loading"
            @click="loadReport"
          >
            {{ loading ? '刷新中…' : '刷新数据' }}
          </button>
          <button
            type="button"
            class="rounded-xl bg-[#2C2420] px-5 py-2 text-sm font-medium text-[#D4AF37] hover:bg-stone-800 disabled:opacity-50"
            :disabled="aiLoading || loading"
            @click="runAiAnalyze"
          >
            {{ aiLoading ? 'AI 分析中…' : 'AI 智能分析' }}
          </button>
        </div>
      </div>

      <div
        v-if="showDailyBanner && report"
        class="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-300/80 bg-amber-50 px-5 py-4"
      >
        <p class="text-sm text-amber-900">
          <span class="font-semibold">今日预警：</span>
          售空 {{ report.counts.outOfStock }} 种 · 低库存 {{ report.counts.lowStock }} 种 · 建议减量
          {{ report.counts.reduceCandidates }} 种
        </p>
        <button type="button" class="text-xs text-amber-800 underline hover:text-amber-950" @click="dismissDailyBanner">
          知道了
        </button>
      </div>

      <p
        v-if="exportToast"
        class="fixed bottom-8 left-1/2 z-50 -translate-x-1/2 rounded-full bg-stone-900 px-6 py-2 text-sm text-white shadow-lg"
      >
        {{ exportToast }}
      </p>

      <p v-if="errorMsg" class="mb-4 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-700">{{ errorMsg }}</p>

      <div v-if="loading && !report" class="py-20 text-center text-stone-500">正在加载预警数据…</div>

      <template v-else-if="report">
        <div class="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <div class="rounded-2xl border border-red-200 bg-white p-4 shadow-sm">
            <p class="text-xs font-medium uppercase tracking-wider text-red-600">售空</p>
            <p class="mt-2 text-3xl font-bold text-stone-900">{{ report.counts.outOfStock }}</p>
            <p class="mt-1 text-xs text-stone-500">需补货或下架</p>
          </div>
          <div class="rounded-2xl border border-amber-200 bg-white p-4 shadow-sm">
            <p class="text-xs font-medium uppercase tracking-wider text-amber-700">低库存</p>
            <p class="mt-2 text-3xl font-bold text-stone-900">{{ report.counts.lowStock }}</p>
            <p class="mt-1 text-xs text-stone-500">低于 {{ report.lowStockThreshold }} 册</p>
          </div>
          <div class="rounded-2xl border border-emerald-200 bg-white p-4 shadow-sm">
            <p class="text-xs font-medium uppercase tracking-wider text-emerald-700">畅销大卖</p>
            <p class="mt-2 text-3xl font-bold text-stone-900">{{ report.hotSelling.length }}</p>
            <p class="mt-1 text-xs text-stone-500">近 30 天热销</p>
          </div>
          <div class="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
            <p class="text-xs font-medium uppercase tracking-wider text-stone-600">建议减量</p>
            <p class="mt-2 text-3xl font-bold text-stone-900">{{ report.counts.reduceCandidates }}</p>
            <p class="mt-1 text-xs text-stone-500">高库存低销量</p>
          </div>
          <div class="rounded-2xl border border-[#D4AF37]/40 bg-white p-4 shadow-sm">
            <p class="text-xs font-medium uppercase tracking-wider text-[#8b6914]">可扩充品类</p>
            <p class="mt-2 text-3xl font-bold text-stone-900">{{ report.suggestAddCategories.length }}</p>
            <p class="mt-1 text-xs text-stone-500">销量高但书目少</p>
          </div>
        </div>

        <div class="mb-8 grid gap-6 lg:grid-cols-2">
          <div class="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
            <div ref="stockPieRef" class="h-72 w-full" />
          </div>
          <div class="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
            <div ref="alertPieRef" class="h-72 w-full" />
          </div>
          <div class="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
            <div ref="categoryPieRef" class="h-72 w-full" />
          </div>
          <div class="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm lg:col-span-2">
            <div ref="categoryBarRef" class="h-80 w-full" />
          </div>
        </div>

        <section v-if="aiAnalysis || aiHint" class="mb-8 rounded-2xl border border-[#D4AF37]/30 bg-[#D4AF37]/8 p-6">
          <h3 class="flex items-center gap-2 text-sm font-semibold text-[#8b6914]">
            <span class="material-symbols-outlined text-lg">psychology</span>
            AI 经营建议
            <span v-if="aiEnabled" class="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-normal text-emerald-800">大模型已启用</span>
          </h3>
          <p v-if="aiHint" class="mt-2 text-xs text-stone-500">{{ aiHint }}</p>
          <pre class="mt-3 whitespace-pre-wrap font-sans text-sm leading-relaxed text-stone-800">{{ aiAnalysis }}</pre>
        </section>

        <div class="grid gap-8 xl:grid-cols-2">
          <section class="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm">
            <h3 class="mb-4 text-sm font-semibold text-red-700">售空图书（{{ report.outOfStock.length }}）</h3>
            <ul v-if="report.outOfStock.length" class="space-y-3">
              <li v-for="b in report.outOfStock" :key="b.bookId" class="flex gap-3 rounded-lg bg-stone-50 p-3">
                <img :src="coverSrc(b)" alt="" class="h-14 w-10 shrink-0 rounded object-cover bg-stone-200" />
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-stone-900">{{ b.title }}</p>
                  <p class="text-xs text-stone-500">{{ b.categoryName || '—' }} · 库存 {{ b.stock }} · 已售 {{ b.soldQty ?? 0 }}</p>
                </div>
                <button type="button" class="shrink-0 text-xs text-[#8b6914] hover:underline" @click="goEditBook(b.bookId)">编辑</button>
              </li>
            </ul>
            <p v-else class="py-6 text-center text-sm text-stone-400">暂无售空图书</p>
          </section>

          <section class="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm">
            <h3 class="mb-4 text-sm font-semibold text-amber-800">低库存（{{ report.lowStock.length }}）</h3>
            <ul v-if="report.lowStock.length" class="space-y-3">
              <li v-for="b in report.lowStock" :key="b.bookId" class="flex gap-3 rounded-lg bg-stone-50 p-3">
                <img :src="coverSrc(b)" alt="" class="h-14 w-10 shrink-0 rounded object-cover bg-stone-200" />
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-stone-900">{{ b.title }}</p>
                  <p class="text-xs text-stone-500">{{ b.categoryName || '—' }} · 库存 {{ b.stock }} · 已售 {{ b.soldQty ?? 0 }}</p>
                </div>
                <button type="button" class="shrink-0 text-xs text-[#8b6914] hover:underline" @click="goEditBook(b.bookId)">补货</button>
              </li>
            </ul>
            <p v-else class="py-6 text-center text-sm text-stone-400">库存充足</p>
          </section>

          <section class="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm">
            <h3 class="mb-4 text-sm font-semibold text-emerald-800">畅销大卖（{{ report.hotSelling.length }}）</h3>
            <ul v-if="report.hotSelling.length" class="space-y-3">
              <li v-for="b in report.hotSelling" :key="b.bookId" class="flex gap-3 rounded-lg bg-stone-50 p-3">
                <img :src="coverSrc(b)" alt="" class="h-14 w-10 shrink-0 rounded object-cover bg-stone-200" />
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-stone-900">{{ b.title }}</p>
                  <p class="text-xs text-stone-500">{{ b.categoryName || '—' }} · 库存 {{ b.stock }} · 已售 {{ b.soldQty ?? 0 }}</p>
                </div>
                <span class="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-800">HOT</span>
              </li>
            </ul>
            <p v-else class="py-6 text-center text-sm text-stone-400">暂无显著畅销数据</p>
          </section>

          <section class="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm">
            <h3 class="mb-4 text-sm font-semibold text-stone-700">建议减量（{{ report.reduceCandidates.length }}）</h3>
            <ul v-if="report.reduceCandidates.length" class="space-y-3">
              <li v-for="b in report.reduceCandidates" :key="b.bookId" class="flex gap-3 rounded-lg bg-stone-50 p-3">
                <img :src="coverSrc(b)" alt="" class="h-14 w-10 shrink-0 rounded object-cover bg-stone-200" />
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-stone-900">{{ b.title }}</p>
                  <p class="text-xs text-stone-500">{{ b.categoryName || '—' }} · 库存 {{ b.stock }} · 已售 {{ b.soldQty ?? 0 }}</p>
                </div>
              </li>
            </ul>
            <p v-else class="py-6 text-center text-sm text-stone-400">未发现明显滞销</p>
          </section>
        </div>

        <section class="mt-8 rounded-2xl border border-[#D4AF37]/30 bg-white p-5 shadow-sm">
          <h3 class="mb-4 text-sm font-semibold text-[#8b6914]">建议扩充品类（{{ report.suggestAddCategories.length }}）</h3>
          <div v-if="report.suggestAddCategories.length" class="overflow-x-auto">
            <table class="w-full min-w-[480px] text-left text-sm">
              <thead class="border-b border-stone-200 text-xs text-stone-500">
                <tr>
                  <th class="py-2 pr-4">分类</th>
                  <th class="py-2 pr-4">在售种类</th>
                  <th class="py-2 pr-4">累计销量</th>
                  <th class="py-2">库存合计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in report.suggestAddCategories" :key="c.categoryId" class="border-b border-stone-100">
                  <td class="py-3 pr-4 font-medium text-stone-900">{{ c.categoryName }}</td>
                  <td class="py-3 pr-4">{{ c.bookCount }}</td>
                  <td class="py-3 pr-4">{{ c.soldQty }}</td>
                  <td class="py-3">{{ c.stockTotal }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="py-6 text-center text-sm text-stone-400">各分类书目较均衡</p>
        </section>
      </template>
    </main>
  </div>
</template>
