<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { adminCategoriesList, type AdminCategory } from '../../api/admin/categories'
import {
  adminBookCreate,
  adminBookDelete,
  adminBooksList,
  adminBookToggleStatus,
  adminBookUpdate,
  resolveAdminBookCover,
  type AdminBook,
  type AdminBookForm,
} from '../../api/admin/books'
import AdminBookFormDialog from './components/AdminBookFormDialog.vue'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'

const { nc } = useAdminNav()
const logout = useAdminLogout()
const router = useRouter()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const books = ref<AdminBook[]>([])
const categories = ref<AdminCategory[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref('')
const categoryId = ref('')
const loading = ref(false)
const errorMsg = ref('')
const toastMsg = ref('')

const showFormDialog = ref(false)
const editingBook = ref<AdminBook | null>(null)
const formSaving = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

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

function formatCreatedAt(raw?: string | null) {
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

function stockClass(stock: number) {
  if (stock <= 0) return 'bg-error-container/30 text-error'
  if (stock < 10) return 'bg-amber-100 text-amber-800'
  return 'bg-surface-container text-on-surface'
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveAdminBookCover(null)
}

async function loadCategories() {
  try {
    const res = await adminCategoriesList()
    if (res.success && res.data) {
      categories.value = res.data
    }
  } catch {
    categories.value = []
  }
}

async function loadBooks() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminBooksList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      status: status.value,
      categoryId: categoryId.value === '' ? undefined : Number(categoryId.value),
    })
    if (res.success && res.data) {
      books.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      const msg = res.message || '加载图书失败'
      if (msg.includes('未登录') || msg.includes('会话')) {
        errorMsg.value = '登录状态已过期，请重新登录'
        setTimeout(() => router.push('/admin/login'), 500)
      } else {
        errorMsg.value = msg
      }
      books.value = []
      total.value = 0
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    books.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  void loadBooks()
}

function resetFilters() {
  keyword.value = ''
  status.value = ''
  categoryId.value = ''
  page.value = 1
  void loadBooks()
}

function openCreate() {
  editingBook.value = null
  showFormDialog.value = true
}

function openEdit(b: AdminBook) {
  editingBook.value = b
  showFormDialog.value = true
}

async function onFormSubmit(payload: AdminBookForm) {
  formSaving.value = true
  try {
    const body = {
      categoryId: payload.categoryId,
      title: payload.title,
      author: payload.author || null,
      isbn: payload.isbn || null,
      price: payload.price,
      stock: payload.stock,
      status: payload.status,
      coverUrl: payload.coverUrl || null,
      description: payload.description || null,
    }
    const res = editingBook.value
      ? await adminBookUpdate(editingBook.value.id, body)
      : await adminBookCreate(body)
    if (res.success) {
      showFormDialog.value = false
      showToast(editingBook.value ? '图书已更新' : '图书已新增')
      await loadBooks()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    formSaving.value = false
  }
}

async function toggleStatus(b: AdminBook) {
  const res = await adminBookToggleStatus(b.id)
  if (!res.success) {
    showToast(res.message || '更新状态失败')
    return
  }
  showToast(res.data?.status === 'ON_SHELF' ? '已上架' : '已下架')
  await loadBooks()
}

async function removeBook(b: AdminBook) {
  if (!window.confirm(`确认删除《${b.title}》吗？删除后不可恢复。`)) return
  const res = await adminBookDelete(b.id)
  if (!res.success) {
    showToast(res.message || '删除失败')
    return
  }
  showToast('已删除')
  if (books.value.length === 1 && page.value > 1) page.value -= 1
  await loadBooks()
}

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  void loadBooks()
}

onMounted(() => {
  void loadCategories()
  void loadBooks()
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
      <span class="text-stone-600">图书管理 · 馆藏书目维护</span>
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

    <AdminBookFormDialog
      v-model="showFormDialog"
      :book="editingBook"
      :categories="categories"
      :saving="formSaving"
      @submit="onFormSubmit"
    />

    <main class="ml-64 mt-16 min-h-screen p-8">
      <div class="mx-auto max-w-container-max">
        <div class="mb-stack-lg flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 class="font-headline-lg text-headline-lg text-primary">图书管理</h2>
            <p class="font-body-md text-on-surface-variant">分类、ISBN、价格、库存、封面、简介与上架状态</p>
          </div>
          <button
            type="button"
            class="flex items-center gap-2 rounded-lg bg-primary px-6 py-3 font-medium text-on-primary shadow-sm hover:opacity-90"
            @click="openCreate"
          >
            <span class="material-symbols-outlined text-xl">add_circle</span>
            新增图书
          </button>
        </div>

        <div
          class="mb-stack-lg flex flex-wrap items-end gap-stack-md rounded-xl border border-outline-variant bg-surface-container-lowest p-stack-md shadow-sm"
        >
          <div class="min-w-[220px] flex-1">
            <label class="mb-1 block text-xs text-on-surface-variant">关键词</label>
            <input
              v-model="keyword"
              type="search"
              class="w-full border-b-2 border-outline-variant bg-surface-container-low px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="书名、作者、ISBN"
              @keydown.enter="onSearch"
            />
          </div>
          <div class="w-48">
            <label class="mb-1 block text-xs text-on-surface-variant">分类</label>
            <select
              v-model="categoryId"
              class="w-full border-b-2 border-outline-variant bg-surface-container-low px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            >
              <option value="">全部分类</option>
              <option v-for="c in categories" :key="c.id" :value="String(c.id)">
                {{ c.parentId ? `　└ ${c.name}` : c.name }}
              </option>
            </select>
          </div>
          <div class="w-40">
            <label class="mb-1 block text-xs text-on-surface-variant">状态</label>
            <select
              v-model="status"
              class="w-full border-b-2 border-outline-variant bg-surface-container-low px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            >
              <option value="">全部</option>
              <option value="ON_SHELF">已上架</option>
              <option value="OFF_SHELF">已下架</option>
            </select>
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
            <table class="w-full min-w-[960px] border-collapse text-left">
              <thead class="border-b border-outline-variant bg-surface-container-high">
                <tr>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">ID</th>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">封面</th>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">书名 / ISBN</th>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">分类</th>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">作者</th>
                  <th class="px-4 py-3 text-right text-xs uppercase tracking-wider text-on-surface-variant">价格</th>
                  <th class="px-4 py-3 text-center text-xs uppercase tracking-wider text-on-surface-variant">库存</th>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">状态</th>
                  <th class="px-4 py-3 text-xs uppercase tracking-wider text-on-surface-variant">入库时间</th>
                  <th class="px-4 py-3 text-right text-xs uppercase tracking-wider text-on-surface-variant">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-outline-variant">
                <tr v-if="loading">
                  <td colspan="10" class="px-6 py-12 text-center text-on-surface-variant">加载中…</td>
                </tr>
                <tr v-else-if="!books.length">
                  <td colspan="10" class="px-6 py-12 text-center text-on-surface-variant">暂无图书记录</td>
                </tr>
                <tr v-for="b in books" :key="b.id" class="transition-colors hover:bg-surface-container-low/80">
                  <td class="px-4 py-3 text-sm text-outline">{{ b.id }}</td>
                  <td class="px-4 py-3">
                    <div class="h-20 w-14 overflow-hidden rounded border border-outline-variant bg-surface-container">
                      <img
                        class="h-full w-full object-cover"
                        :src="resolveAdminBookCover(b.coverUrl)"
                        :alt="b.title"
                        loading="lazy"
                        @error="onCoverError"
                      />
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    <div class="max-w-[200px] font-medium text-primary">{{ b.title }}</div>
                    <div class="mt-0.5 text-xs text-outline">ISBN: {{ b.isbn || '—' }}</div>
                    <p
                      v-if="b.description?.trim()"
                      class="mt-1 line-clamp-1 max-w-[220px] text-[11px] text-on-surface-variant"
                      :title="b.description || ''"
                    >
                      {{ b.description }}
                    </p>
                  </td>
                  <td class="px-4 py-3 text-sm text-on-surface-variant">{{ b.categoryName || '未分类' }}</td>
                  <td class="px-4 py-3 text-sm">{{ b.author || '—' }}</td>
                  <td class="px-4 py-3 text-right font-medium text-primary">¥{{ formatPrice(b.price) }}</td>
                  <td class="px-4 py-3 text-center">
                    <span class="rounded px-2 py-0.5 text-sm" :class="stockClass(Number(b.stock))">{{ b.stock ?? 0 }}</span>
                  </td>
                  <td class="px-4 py-3">
                    <span
                      class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium"
                      :class="b.status === 'ON_SHELF' ? 'bg-green-100 text-green-800' : 'bg-stone-200 text-stone-700'"
                    >
                      {{ b.status === 'ON_SHELF' ? '上架' : '下架' }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-xs text-outline whitespace-nowrap">{{ formatCreatedAt(b.createdAt) }}</td>
                  <td class="px-4 py-3 text-right">
                    <div class="flex justify-end gap-1">
                      <button
                        type="button"
                        class="rounded-full p-2 text-outline hover:bg-secondary-container/20 hover:text-secondary"
                        title="编辑"
                        @click="openEdit(b)"
                      >
                        <span class="material-symbols-outlined text-lg">edit</span>
                      </button>
                      <button
                        type="button"
                        class="rounded-full p-2 text-outline hover:bg-secondary-container/20 hover:text-secondary"
                        :title="b.status === 'ON_SHELF' ? '下架' : '上架'"
                        @click="toggleStatus(b)"
                      >
                        <span class="material-symbols-outlined text-lg">{{
                          b.status === 'ON_SHELF' ? 'visibility_off' : 'visibility'
                        }}</span>
                      </button>
                      <button
                        type="button"
                        class="rounded-full p-2 text-outline hover:bg-error-container/20 hover:text-error"
                        title="删除"
                        @click="removeBook(b)"
                      >
                        <span class="material-symbols-outlined text-lg">delete</span>
                      </button>
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
              第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条
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
