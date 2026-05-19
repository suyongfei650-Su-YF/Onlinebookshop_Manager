<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  adminCategoriesList,
  adminCategoryCreate,
  adminCategoryDelete,
  adminCategoryToggleVisible,
  adminCategoryUpdate,
  type AdminCategory,
  type AdminCategoryForm,
  type AdminCategoryStats,
} from '../../api/admin/categories'
import AdminCategoryFormDialog from './components/AdminCategoryFormDialog.vue'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'

type CatNode = AdminCategory & { children: CatNode[] }
type FlatRow = { node: CatNode; depth: number; hasChildren: boolean }

const { nc } = useAdminNav()
const logout = useAdminLogout()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const categories = ref<AdminCategory[]>([])
const stats = ref<AdminCategoryStats | null>(null)
const keyword = ref('')
const loading = ref(false)
const errorMsg = ref('')
const toastMsg = ref('')
const expandedIds = ref<Set<number>>(new Set())
const actingId = ref<number | null>(null)

const showFormDialog = ref(false)
const editingCategory = ref<AdminCategory | null>(null)
const fixedParentId = ref<number | null>(null)
const formSaving = ref(false)

let searchTimer: ReturnType<typeof setTimeout> | undefined

const topLevelOptions = computed(() => categories.value.filter((c) => !c.parentId))

const isSearchMode = computed(() => !!keyword.value.trim())

const displayRows = computed((): FlatRow[] => {
  if (isSearchMode.value) {
    return categories.value.map((c) => ({
      node: { ...c, children: [] },
      depth: c.parentId ? 1 : 0,
      hasChildren: false,
    }))
  }
  return flattenForest(buildForest(categories.value))
})

function buildForest(list: AdminCategory[]): CatNode[] {
  const map = new Map<number, CatNode>()
  for (const c of list) map.set(c.id, { ...c, children: [] })
  const roots: CatNode[] = []
  for (const c of list) {
    const node = map.get(c.id)!
    if (c.parentId && map.has(c.parentId)) {
      map.get(c.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  }
  return roots
}

function flattenForest(nodes: CatNode[], depth = 0): FlatRow[] {
  const rows: FlatRow[] = []
  for (const node of nodes) {
    const hasChildren = node.children.length > 0
    rows.push({ node, depth, hasChildren })
    if (hasChildren && expandedIds.value.has(node.id)) {
      rows.push(...flattenForest(node.children, depth + 1))
    }
  }
  return rows
}

function showToast(msg: string) {
  toastMsg.value = msg
  setTimeout(() => {
    toastMsg.value = ''
  }, 2200)
}

function visibleLabel(v?: number) {
  return v === 1 ? '显示中' : '已隐藏'
}

function visibleClass(v?: number) {
  return v === 1
    ? 'border-green-100 bg-green-50 text-green-700'
    : 'border-stone-200 bg-stone-100 text-stone-500'
}

function toggleExpand(id: number) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

function openCreate(parentId?: number | null) {
  editingCategory.value = null
  fixedParentId.value = parentId ?? null
  showFormDialog.value = true
}

function openEdit(c: AdminCategory) {
  editingCategory.value = c
  fixedParentId.value = null
  showFormDialog.value = true
}

async function loadCategories() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminCategoriesList(keyword.value)
    if (res.success && res.data) {
      categories.value = res.data.list || []
      stats.value = res.data.stats || null
      if (!isSearchMode.value && expandedIds.value.size === 0) {
        const roots = categories.value.filter((c) => !c.parentId).map((c) => c.id)
        expandedIds.value = new Set(roots)
      }
    } else {
      errorMsg.value = res.message || '加载分类失败'
      categories.value = []
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    categories.value = []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  expandedIds.value = new Set()
  void loadCategories()
}

function onKeywordInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => onSearch(), 400)
}

function resetSearch() {
  keyword.value = ''
  expandedIds.value = new Set()
  void loadCategories()
}

async function onFormSubmit(payload: AdminCategoryForm) {
  formSaving.value = true
  try {
    const body = {
      name: payload.name,
      nameEn: payload.nameEn || undefined,
      code: payload.code || undefined,
      parentId: payload.parentId,
      sortWeight: payload.sortWeight,
      visible: payload.visible,
    }
    const res = editingCategory.value
      ? await adminCategoryUpdate(editingCategory.value.id, body)
      : await adminCategoryCreate(body)
    if (res.success) {
      showFormDialog.value = false
      showToast(editingCategory.value ? '分类已更新' : '分类已新增')
      await loadCategories()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    formSaving.value = false
  }
}

async function toggleVisible(c: AdminCategory) {
  actingId.value = c.id
  try {
    const res = await adminCategoryToggleVisible(c.id)
    if (res.success) {
      showToast(res.data?.visible === 1 ? '已设为显示' : '已隐藏')
      await loadCategories()
    } else {
      showToast(res.message || '操作失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

async function removeCategory(c: AdminCategory) {
  if (!window.confirm(`确认删除分类「${c.name}」吗？`)) return
  actingId.value = c.id
  try {
    const res = await adminCategoryDelete(c.id)
    if (res.success) {
      showToast('分类已删除')
      await loadCategories()
    } else {
      showToast(res.message || '删除失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

onMounted(() => void loadCategories())
</script>

<template>
  <div class="font-body-md text-on-surface">
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
      class="fixed right-0 top-0 z-30 flex h-16 w-[calc(100%-16rem)] items-center justify-between border-b border-stone-200 bg-white/80 px-8 backdrop-blur-md"
    >
      <span class="text-sm text-stone-600">分类管理 · category 表</span>
      <div class="flex items-center gap-4">
        <button type="button" class="rounded-full p-2 hover:bg-stone-50" @click="showProfileDialog = true">
          <span class="material-symbols-outlined">settings</span>
        </button>
        <img class="h-8 w-8 rounded-full border object-cover" :src="avatarUrl" alt="" />
        <span class="text-sm font-semibold text-primary">{{ displayName }}</span>
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

    <AdminCategoryFormDialog
      v-model="showFormDialog"
      :category="editingCategory"
      :parent-options="topLevelOptions"
      :fixed-parent-id="fixedParentId"
      :saving="formSaving"
      @submit="onFormSubmit"
    />

    <main class="ml-64 min-h-screen pt-16">
      <div class="mx-auto max-w-[1280px] space-y-8 p-8">
        <section class="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 class="font-headline-lg text-2xl text-primary">分类管理</h2>
            <p class="mt-2 text-sm text-stone-500">管理图书分类层级、排序权重与前台展示状态。</p>
          </div>
          <button
            type="button"
            class="flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-white shadow-lg hover:bg-stone-800"
            @click="openCreate()"
          >
            <span class="material-symbols-outlined text-sm">add_circle</span>
            新增类目
          </button>
        </section>

        <section class="grid grid-cols-1 gap-4 md:grid-cols-4">
          <div class="rounded-xl border bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">总类目数</p>
            <p class="mt-2 text-3xl font-bold text-primary">{{ stats?.totalCategories ?? '—' }}</p>
          </div>
          <div class="rounded-xl border bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">已分类图书</p>
            <p class="mt-2 text-3xl font-bold text-primary">{{ stats?.categorizedBooks ?? '—' }}</p>
          </div>
          <div class="rounded-xl border bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">未分类图书</p>
            <p class="mt-2 text-3xl font-bold text-amber-700">{{ stats?.uncategorizedBooks ?? '—' }}</p>
          </div>
          <div class="rounded-xl border bg-white p-6 shadow-sm">
            <p class="text-xs uppercase tracking-widest text-stone-400">展示中类目</p>
            <p class="mt-2 text-3xl font-bold text-green-700">{{ stats?.visibleCount ?? '—' }}</p>
          </div>
        </section>

        <div class="flex flex-wrap items-end gap-4 rounded-xl border bg-white p-4 shadow-sm">
          <div class="min-w-[240px] flex-1">
            <label class="mb-1 block text-xs text-stone-500">搜索</label>
            <input
              v-model="keyword"
              type="search"
              class="w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="名称、英文名、编码、ID"
              @input="onKeywordInput"
              @keydown.enter="onSearch"
            />
          </div>
          <div class="flex gap-2 self-end">
            <button type="button" class="rounded-lg bg-secondary-container px-5 py-2 text-sm" @click="onSearch">查询</button>
            <button type="button" class="rounded-lg border px-4 py-2 text-sm" @click="resetSearch">重置</button>
            <button type="button" class="rounded-lg border p-2" title="刷新" @click="loadCategories">
              <span class="material-symbols-outlined text-lg">refresh</span>
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="rounded-lg bg-error-container px-3 py-2 text-sm">{{ errorMsg }}</p>

        <section class="overflow-hidden rounded-xl border bg-white shadow-sm">
          <div
            class="grid grid-cols-12 gap-4 border-b bg-stone-50 px-6 py-3 text-xs font-semibold uppercase tracking-wider text-stone-500"
          >
            <div class="col-span-5">分类名称与层级</div>
            <div class="col-span-2 text-center">图书数量</div>
            <div class="col-span-2 text-center">排序权重</div>
            <div class="col-span-1 text-center">展示状态</div>
            <div class="col-span-2 text-right">管理操作</div>
          </div>

          <div v-if="loading" class="px-6 py-12 text-center text-stone-400">加载中…</div>
          <div v-else-if="!displayRows.length" class="px-6 py-12 text-center text-stone-400">
            {{ keyword.trim() ? '未找到匹配的分类' : '暂无分类，请点击「新增类目」' }}
          </div>

          <div
            v-for="row in displayRows"
            :key="row.node.id"
            class="grid grid-cols-12 items-center gap-4 border-b border-stone-50 px-6 py-4 transition-colors hover:bg-stone-50/50"
            :class="row.node.visible !== 1 ? 'opacity-70' : ''"
          >
            <div class="col-span-5 flex items-center gap-2" :style="{ paddingLeft: `${row.depth * 1.5}rem` }">
              <button
                v-if="row.hasChildren && !isSearchMode"
                type="button"
                class="text-stone-400"
                @click="toggleExpand(row.node.id)"
              >
                <span class="material-symbols-outlined text-xl">{{
                  expandedIds.has(row.node.id) ? 'expand_more' : 'chevron_right'
                }}</span>
              </button>
              <span v-else-if="row.depth > 0" class="w-6" />
              <span class="material-symbols-outlined text-secondary">{{ row.depth === 0 ? 'folder' : 'description' }}</span>
              <span class="font-medium" :class="row.node.visible !== 1 ? 'text-stone-500 line-through' : ''">
                {{ row.node.name }}
                <span v-if="row.node.nameEn" class="text-stone-400">({{ row.node.nameEn }})</span>
              </span>
              <span v-if="row.node.code" class="rounded bg-stone-100 px-2 py-0.5 text-[10px] font-bold text-stone-500">{{
                row.node.code
              }}</span>
            </div>
            <div class="col-span-2 text-center text-sm text-stone-600">{{ row.node.bookCount ?? 0 }} 册</div>
            <div class="col-span-2 text-center text-sm text-stone-600">{{ row.node.sortWeight ?? 0 }}</div>
            <div class="col-span-1 flex justify-center">
              <span class="rounded-full border px-3 py-0.5 text-[11px] font-medium" :class="visibleClass(row.node.visible)">
                {{ visibleLabel(row.node.visible) }}
              </span>
            </div>
            <div class="col-span-2 flex justify-end gap-1">
              <button
                v-if="!row.node.parentId"
                type="button"
                class="rounded p-1.5 text-stone-400 hover:text-secondary"
                title="新增子分类"
                @click="openCreate(row.node.id)"
              >
                <span class="material-symbols-outlined text-lg">add</span>
              </button>
              <button type="button" class="rounded p-1.5 text-stone-400 hover:text-primary" title="编辑" @click="openEdit(row.node)">
                <span class="material-symbols-outlined text-lg">edit</span>
              </button>
              <button
                type="button"
                class="rounded p-1.5 text-stone-400 hover:text-secondary disabled:opacity-50"
                :title="row.node.visible === 1 ? '隐藏' : '显示'"
                :disabled="actingId === row.node.id"
                @click="toggleVisible(row.node)"
              >
                <span class="material-symbols-outlined text-lg">{{
                  row.node.visible === 1 ? 'visibility_off' : 'visibility'
                }}</span>
              </button>
              <button
                type="button"
                class="rounded p-1.5 text-stone-400 hover:text-error disabled:opacity-50"
                title="删除"
                :disabled="actingId === row.node.id"
                @click="removeCategory(row.node)"
              >
                <span class="material-symbols-outlined text-lg">delete</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <p v-if="toastMsg" class="fixed bottom-8 right-8 z-[110] rounded-lg bg-[#2C2420] px-4 py-2 text-sm text-white shadow-lg">
        {{ toastMsg }}
      </p>
    </main>
  </div>
</template>
