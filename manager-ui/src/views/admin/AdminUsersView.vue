<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  adminCustomerCreate,
  adminCustomersList,
  adminCustomerToggleStatus,
  adminCustomerUpdate,
  customerStatusLabel,
  type AdminCustomer,
  type AdminCustomerForm,
} from '../../api/admin/customers'
import AdminCustomerFormDialog from './components/AdminCustomerFormDialog.vue'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'

const route = useRoute()
const router = useRouter()
const { nc } = useAdminNav()
const logout = useAdminLogout()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const customers = ref<AdminCustomer[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const roleTagFilter = ref('')

const ROLE_TAG_OPTIONS = ['普通读者', '高级读者', '认证学者', '资深会员'] as const

let keywordDebounceTimer: ReturnType<typeof setTimeout> | undefined
const statusCounts = ref<Record<string, number>>({})
const loading = ref(false)
const errorMsg = ref('')
const toastMsg = ref('')
const actingId = ref<number | null>(null)

const showFormDialog = ref(false)
const editingCustomer = ref<AdminCustomer | null>(null)
const formSaving = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const hasActiveFilters = computed(
  () => !!keyword.value.trim() || !!statusFilter.value || !!roleTagFilter.value,
)

const filterSummary = computed(() => {
  const parts: string[] = []
  const kw = keyword.value.trim()
  if (kw) parts.push(`关键词「${kw}」`)
  if (statusFilter.value) parts.push(customerStatusLabel(statusFilter.value))
  if (roleTagFilter.value) parts.push(`标签「${roleTagFilter.value}」`)
  return parts.join(' · ')
})

const statusTabs = computed(() => {
  const c = statusCounts.value
  const all = Object.values(c).reduce((s, n) => s + (n || 0), 0)
  return [
    { key: '', label: '全部', count: all },
    { key: 'ACTIVE', label: '账号正常', count: c.ACTIVE || 0 },
    { key: 'DISABLED', label: '已被禁用', count: c.DISABLED || 0 },
  ]
})

function showToast(msg: string) {
  toastMsg.value = msg
  setTimeout(() => {
    toastMsg.value = ''
  }, 2200)
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

function maskPhone(phone?: string | null) {
  if (!phone?.trim()) return '—'
  const p = phone.trim()
  if (p.length === 11) return `${p.slice(0, 3)} **** ${p.slice(7)}`
  return p
}

function customerInitials(c: AdminCustomer) {
  const name = c.nickname || c.username || '?'
  return name.slice(0, 2)
}

function statusBadgeClass(status: string) {
  if (status === 'ACTIVE') return 'bg-green-50 text-green-700'
  if (status === 'DISABLED') return 'bg-stone-100 text-stone-500'
  return 'bg-surface-container text-on-surface-variant'
}

function statusDotClass(status: string) {
  if (status === 'ACTIVE') return 'bg-green-500'
  if (status === 'DISABLED') return 'bg-stone-400'
  return 'bg-outline'
}

function isDisabled(c: AdminCustomer) {
  return c.status === 'DISABLED'
}

async function loadCustomers() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminCustomersList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value,
      roleTag: roleTagFilter.value,
    })
    if (res.success && res.data) {
      customers.value = res.data.list || []
      total.value = res.data.total || 0
      statusCounts.value = res.data.statusCounts || {}
    } else {
      const msg = res.message || '加载用户失败'
      if (msg.includes('未登录') || msg.includes('会话')) {
        errorMsg.value = '登录状态已过期，请重新登录'
        setTimeout(() => router.push('/admin/login'), 500)
      } else {
        errorMsg.value = msg
      }
      customers.value = []
      total.value = 0
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    customers.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function syncQueryToRoute() {
  const query: Record<string, string> = {}
  const kw = keyword.value.trim()
  if (kw) query.keyword = kw
  if (statusFilter.value) query.status = statusFilter.value
  if (roleTagFilter.value) query.roleTag = roleTagFilter.value
  router.replace({ path: '/admin/users', query })
}

function applyRouteQuery() {
  const q = route.query
  keyword.value = typeof q.keyword === 'string' ? q.keyword : ''
  statusFilter.value = typeof q.status === 'string' ? q.status : ''
  roleTagFilter.value = typeof q.roleTag === 'string' ? q.roleTag : ''
}

function onSearch() {
  page.value = 1
  syncQueryToRoute()
  void loadCustomers()
}

function onKeywordInput() {
  if (keywordDebounceTimer) clearTimeout(keywordDebounceTimer)
  keywordDebounceTimer = setTimeout(() => onSearch(), 450)
}

function setStatusTab(key: string) {
  statusFilter.value = key
  page.value = 1
  void loadCustomers()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  roleTagFilter.value = ''
  page.value = 1
  router.replace({ path: '/admin/users' })
  void loadCustomers()
}

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  void loadCustomers()
}

function openCreate() {
  editingCustomer.value = null
  showFormDialog.value = true
}

function openEdit(c: AdminCustomer) {
  editingCustomer.value = c
  showFormDialog.value = true
}

async function onFormSubmit(payload: AdminCustomerForm & { password?: string }) {
  formSaving.value = true
  try {
    const body: Partial<AdminCustomerForm> & { password?: string } = {
      nickname: payload.nickname,
      username: payload.username || undefined,
      email: payload.email || undefined,
      phone: payload.phone || undefined,
      roleTag: payload.roleTag,
      status: payload.status,
    }
    if (payload.password) body.password = payload.password
    const res = editingCustomer.value
      ? await adminCustomerUpdate(editingCustomer.value.id, body)
      : await adminCustomerCreate(body)
    if (res.success) {
      showFormDialog.value = false
      showToast(editingCustomer.value ? '用户已更新' : '用户已新增')
      await loadCustomers()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    formSaving.value = false
  }
}

async function toggleStatus(c: AdminCustomer) {
  const action = c.status === 'ACTIVE' ? '禁用' : '启用'
  if (!window.confirm(`确认${action}用户「${c.nickname}」吗？`)) return
  actingId.value = c.id
  try {
    const res = await adminCustomerToggleStatus(c.id)
    if (res.success) {
      showToast(res.data?.status === 'ACTIVE' ? '已启用' : '已禁用')
      await loadCustomers()
    } else {
      showToast(res.message || '操作失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

function viewOrders(c: AdminCustomer) {
  router.push({ path: '/admin/orders', query: { keyword: c.username || c.nickname || String(c.id) } })
}

onMounted(() => {
  applyRouteQuery()
  void loadCustomers()
})
</script>

<template>
  <div class="bg-background font-body-md text-on-background">
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
      <span class="text-stone-600">用户管理 · customer 表</span>
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

    <AdminCustomerFormDialog
      v-model="showFormDialog"
      :customer="editingCustomer"
      :saving="formSaving"
      @submit="onFormSubmit"
    />

    <main class="ml-64 mt-16 min-h-screen p-8">
      <div class="mx-auto max-w-container-max space-y-8">
        <section class="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 class="font-headline-lg text-headline-lg text-primary">用户管理</h2>
            <p class="mt-1 font-body-md text-on-surface-variant">
              昵称、登录账号、联系方式、状态与订单统计。
            </p>
          </div>
          <button
            type="button"
            class="flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-white shadow-lg shadow-primary/10 hover:bg-stone-800"
            @click="openCreate"
          >
            <span class="material-symbols-outlined text-sm">person_add</span>
            新增用户
          </button>
        </section>

        <section class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div class="rounded-xl border border-stone-100 bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">总用户数</p>
            <p class="mt-2 font-['Work_Sans'] text-3xl font-bold text-primary">
              {{ statusCounts.ACTIVE != null || statusCounts.DISABLED != null ? (statusCounts.ACTIVE || 0) + (statusCounts.DISABLED || 0) : total }}
            </p>
          </div>
          <div class="rounded-xl border border-stone-100 bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">正常活跃</p>
            <p class="mt-2 font-['Work_Sans'] text-3xl font-bold text-green-700">{{ statusCounts.ACTIVE || 0 }}</p>
          </div>
          <div class="rounded-xl border border-stone-100 bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">账户禁用</p>
            <p class="mt-2 font-['Work_Sans'] text-3xl font-bold text-stone-500">{{ statusCounts.DISABLED || 0 }}</p>
          </div>
        </section>

        <div class="rounded-xl border border-stone-200 bg-surface-container-lowest p-1 shadow-sm">
          <div class="flex items-center gap-1 overflow-x-auto">
            <button
              v-for="tab in statusTabs"
              :key="tab.key"
              type="button"
              class="flex shrink-0 items-center gap-2 rounded-lg px-5 py-2.5 text-sm transition-all"
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

        <div class="flex flex-wrap items-end gap-4 rounded-xl border border-outline-variant bg-surface-container-lowest p-4 shadow-sm">
          <div class="min-w-[240px] flex-1">
            <label class="mb-1 block text-xs text-on-surface-variant">搜索</label>
            <input
              v-model="keyword"
              type="search"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="昵称、用户名、邮箱、手机号、用户 ID"
              @input="onKeywordInput"
              @keydown.enter="onSearch"
            />
          </div>
          <div class="w-36">
            <label class="mb-1 block text-xs text-on-surface-variant">读者标签</label>
            <select
              v-model="roleTagFilter"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              @change="onSearch"
            >
              <option value="">全部标签</option>
              <option v-for="tag in ROLE_TAG_OPTIONS" :key="tag" :value="tag">{{ tag }}</option>
            </select>
          </div>
          <div class="w-28">
            <label class="mb-1 block text-xs text-on-surface-variant">每页</label>
            <select
              v-model.number="pageSize"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm"
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
              class="rounded-lg bg-secondary-container px-5 py-2 text-sm font-medium text-on-secondary-container"
              @click="onSearch"
            >
              查询
            </button>
            <button
              type="button"
              class="rounded-lg border border-outline-variant px-4 py-2 text-sm text-outline"
              @click="resetFilters"
            >
              重置
            </button>
            <button
              type="button"
              class="rounded-lg border border-outline-variant p-2 text-outline hover:bg-surface-container-low"
              title="刷新"
              @click="loadCustomers"
            >
              <span class="material-symbols-outlined text-lg">refresh</span>
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ errorMsg }}</p>

        <p
          v-if="hasActiveFilters && filterSummary"
          class="rounded-lg border border-secondary/20 bg-secondary-container/30 px-4 py-2 text-sm text-on-secondary-container"
        >
          当前筛选：{{ filterSummary }}
        </p>

        <section class="overflow-hidden rounded-xl border border-stone-100 bg-white shadow-sm">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[900px] border-collapse text-left">
              <thead class="bg-surface-container-low text-xs uppercase tracking-widest text-stone-500">
                <tr>
                  <th class="px-6 py-4 font-medium">ID</th>
                  <th class="px-6 py-4 font-medium">读者信息</th>
                  <th class="px-6 py-4 font-medium">登录账号</th>
                  <th class="px-6 py-4 font-medium">联系电话</th>
                  <th class="px-6 py-4 font-medium">订单数</th>
                  <th class="px-6 py-4 font-medium">注册时间</th>
                  <th class="px-6 py-4 font-medium">状态</th>
                  <th class="px-6 py-4 text-right font-medium">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-50">
                <tr v-if="loading">
                  <td colspan="8" class="px-6 py-12 text-center text-stone-400">加载中…</td>
                </tr>
                <tr v-else-if="!customers.length">
                  <td colspan="8" class="px-6 py-12 text-center text-stone-400">
                    {{ hasActiveFilters ? '未找到匹配的用户，请调整筛选条件' : '暂无用户' }}
                  </td>
                </tr>
                <tr
                  v-for="c in customers"
                  :key="c.id"
                  class="group transition-colors hover:bg-surface-container-lowest"
                  :class="isDisabled(c) ? 'bg-stone-50/40' : ''"
                >
                  <td class="px-6 py-5 font-mono text-sm text-stone-400">#{{ c.id }}</td>
                  <td class="px-6 py-5">
                    <div class="flex items-center gap-3" :class="isDisabled(c) ? 'opacity-60 grayscale' : ''">
                      <div
                        v-if="!c.avatarUrl?.trim()"
                        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 border-stone-100 bg-secondary-container text-xs font-bold text-on-secondary-container"
                      >
                        {{ customerInitials(c) }}
                      </div>
                      <img
                        v-else
                        :src="c.avatarUrl"
                        alt=""
                        class="h-10 w-10 shrink-0 rounded-full border-2 border-stone-100 object-cover"
                      />
                      <div>
                        <div class="font-medium text-primary" :class="isDisabled(c) ? 'line-through text-stone-500' : ''">
                          {{ c.nickname }}
                        </div>
                        <div class="text-xs text-stone-400">{{ c.roleTag || '普通读者' }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-5 text-sm text-stone-600">
                    <div v-if="c.username">{{ c.username }}</div>
                    <div v-if="c.email" class="text-xs text-stone-400">{{ c.email }}</div>
                    <span v-if="!c.username && !c.email" class="text-stone-400">—</span>
                  </td>
                  <td class="px-6 py-5 text-sm text-stone-600">{{ maskPhone(c.phone) }}</td>
                  <td class="px-6 py-5 text-sm text-stone-600">{{ c.orderCount ?? 0 }}</td>
                  <td class="px-6 py-5 text-sm text-stone-500 whitespace-nowrap">{{ formatTime(c.createdAt) }}</td>
                  <td class="px-6 py-5">
                    <span
                      class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium"
                      :class="statusBadgeClass(c.status)"
                    >
                      <span class="h-1.5 w-1.5 rounded-full" :class="statusDotClass(c.status)"></span>
                      {{ customerStatusLabel(c.status) }}
                    </span>
                  </td>
                  <td class="px-6 py-5 text-right">
                    <div class="flex justify-end gap-1 opacity-80 group-hover:opacity-100">
                      <button
                        type="button"
                        class="rounded-lg p-2 text-stone-400 hover:bg-secondary-container/20 hover:text-secondary"
                        title="查看订单"
                        @click="viewOrders(c)"
                      >
                        <span class="material-symbols-outlined text-xl">receipt_long</span>
                      </button>
                      <button
                        type="button"
                        class="rounded-lg p-2 text-stone-400 hover:bg-stone-100 hover:text-primary"
                        title="编辑"
                        @click="openEdit(c)"
                      >
                        <span class="material-symbols-outlined text-xl">edit</span>
                      </button>
                      <button
                        type="button"
                        class="rounded-lg p-2 text-stone-400 hover:bg-error-container/20 hover:text-error disabled:opacity-50"
                        :title="c.status === 'ACTIVE' ? '禁用' : '启用'"
                        :disabled="actingId === c.id"
                        @click="toggleStatus(c)"
                      >
                        <span class="material-symbols-outlined text-xl">{{
                          c.status === 'ACTIVE' ? 'block' : 'check_circle'
                        }}</span>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="flex flex-wrap items-center justify-between gap-4 border-t border-stone-100 bg-surface-container-low p-4 text-sm">
            <span class="text-stone-500">
              第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条记录
              <template v-if="filterSummary">（{{ filterSummary }}）</template>
            </span>
            <div class="flex gap-2">
              <button
                type="button"
                class="flex h-8 w-8 items-center justify-center rounded border border-stone-200 disabled:opacity-40"
                :disabled="page <= 1 || loading"
                @click="goPage(page - 1)"
              >
                <span class="material-symbols-outlined text-sm">chevron_left</span>
              </button>
              <button
                type="button"
                class="flex h-8 w-8 items-center justify-center rounded border border-stone-200 disabled:opacity-40"
                :disabled="page >= totalPages || loading"
                @click="goPage(page + 1)"
              >
                <span class="material-symbols-outlined text-sm">chevron_right</span>
              </button>
            </div>
          </div>
        </section>
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
