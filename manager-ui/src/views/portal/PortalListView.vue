<template>
  <div class="bg-background text-on-background font-body-md selection:bg-secondary-container selection:text-on-secondary-container min-h-screen">
    <main class="max-w-[1280px] mx-auto px-6 md:px-margin-page pt-28 pb-section-gap">
      <div class="mb-10 border-b border-outline-variant pb-6">
        <span class="font-label-sm text-secondary uppercase tracking-widest">Catalog</span>
        <h1 class="font-headline-lg text-primary mt-2">藏书分类</h1>
        <p class="text-sm text-outline mt-2">
          <template v-if="keyword">搜索「{{ keyword }}」，共 {{ total }} 条结果</template>
          <template v-else>数据来自馆藏数据库，共 {{ totalAll }} 册在架图书</template>
        </p>
      </div>

      <div class="flex flex-col lg:flex-row gap-10 lg:gap-16">
        <aside class="w-full lg:w-64 flex-shrink-0">
          <div class="lg:sticky lg:top-32 space-y-8">
            <div>
              <h2 class="font-headline-md text-headline-md text-primary mb-4 border-b border-outline-variant pb-3">筛选目录</h2>
              <form class="relative" @submit.prevent="applyKeyword">
                <input
                  v-model="keywordInput"
                  class="w-full bg-surface-container-low border border-outline-variant text-sm py-2.5 pl-3 pr-10 focus:border-secondary focus:ring-0"
                  placeholder="搜索书名、作者、ISBN"
                  type="search"
                />
                <button type="submit" class="absolute right-2 top-1/2 -translate-y-1/2 text-outline hover:text-primary">
                  <span class="material-symbols-outlined text-xl">search</span>
                </button>
              </form>
            </div>

            <div>
              <h3 class="font-label-sm text-label-sm uppercase text-outline mb-4">图书分类</h3>
              <ul class="space-y-2">
                <li>
                  <button
                    type="button"
                    class="w-full flex justify-between items-center py-1.5 text-left transition-colors"
                    :class="selectedCategoryId === null ? 'text-secondary font-medium' : 'hover:text-secondary text-on-background'"
                    @click="selectCategory(null)"
                  >
                    <span>全部书籍</span>
                    <span class="text-xs text-outline-variant">{{ totalAll }}</span>
                  </button>
                </li>
                <li v-for="cat in topCategories" :key="cat.id">
                  <button
                    type="button"
                    class="w-full flex justify-between items-center py-1.5 text-left transition-colors"
                    :class="isCategoryActive(cat.id) ? 'text-secondary font-medium' : 'hover:text-secondary text-on-background'"
                    @click="selectCategory(cat.id)"
                  >
                    <span>{{ cat.name }}</span>
                    <span class="text-xs text-outline-variant">{{ cat.bookCount }}</span>
                  </button>
                  <ul v-if="childCategories(cat.id).length" class="mt-1 ml-3 space-y-1 border-l border-outline-variant/40 pl-3">
                    <li v-for="sub in childCategories(cat.id)" :key="sub.id">
                      <button
                        type="button"
                        class="w-full flex justify-between items-center py-1 text-sm text-left transition-colors"
                        :class="isCategoryActive(sub.id) ? 'text-secondary font-medium' : 'hover:text-secondary text-outline'"
                        @click="selectCategory(sub.id)"
                      >
                        <span>{{ sub.name }}</span>
                        <span class="text-xs text-outline-variant">{{ sub.bookCount }}</span>
                      </button>
                    </li>
                  </ul>
                </li>
              </ul>
            </div>

            <div>
              <h3 class="font-label-sm text-label-sm uppercase text-outline mb-4">价格区间 (¥)</h3>
              <div class="space-y-3">
                <div class="flex items-center gap-2">
                  <input
                    v-model="priceMinInput"
                    class="w-full bg-surface-container-low border border-outline-variant text-xs py-2 px-3 focus:border-secondary focus:ring-0"
                    placeholder="最低"
                    type="number"
                    min="0"
                    step="0.01"
                  />
                  <span class="text-outline-variant">—</span>
                  <input
                    v-model="priceMaxInput"
                    class="w-full bg-surface-container-low border border-outline-variant text-xs py-2 px-3 focus:border-secondary focus:ring-0"
                    placeholder="最高"
                    type="number"
                    min="0"
                    step="0.01"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label v-for="p in pricePresets" :key="p.label" class="flex items-center gap-2 cursor-pointer">
                    <input
                      v-model="pricePreset"
                      class="rounded-sm border-outline-variant text-secondary focus:ring-secondary/20"
                      type="radio"
                      name="pricePreset"
                      :value="p.key"
                      @change="applyPricePreset(p)"
                    />
                    <span class="text-sm">{{ p.label }}</span>
                  </label>
                </div>
                <button
                  type="button"
                  class="w-full py-2 text-sm border border-secondary text-secondary hover:bg-secondary-container/20 transition-colors"
                  @click="applyPriceFilter"
                >
                  应用价格筛选
                </button>
              </div>
            </div>

            <button
              type="button"
              class="w-full py-2 text-sm text-outline border border-outline-variant hover:border-primary hover:text-primary transition-colors"
              @click="resetFilters"
            >
              重置筛选
            </button>
          </div>
        </aside>

        <div class="flex-1 min-w-0">
          <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8 border-b border-surface-container-highest pb-6">
            <div class="flex items-center flex-wrap gap-4 sm:gap-6">
              <button
                v-for="s in sortOptions"
                :key="s.key"
                type="button"
                class="pb-1 transition-colors"
                :class="sort === s.key ? 'text-primary font-medium border-b border-primary' : 'text-outline hover:text-primary'"
                @click="changeSort(s.key)"
              >
                {{ s.label }}
              </button>
            </div>
            <span class="text-xs text-outline shrink-0">
              共 {{ total }} 件
              <span v-if="keyword"> · 关键词「{{ keyword }}」</span>
              <span v-else-if="selectedCategoryName"> · {{ selectedCategoryName }}</span>
            </span>
          </div>

          <div v-if="loading" class="py-24 text-center text-outline">正在加载馆藏数据…</div>
          <div v-else-if="errorMsg" class="py-24 text-center">
            <p class="text-error mb-4">{{ errorMsg }}</p>
            <button type="button" class="px-6 py-2 border border-primary text-primary hover:bg-primary/5" @click="loadBooks">重试</button>
          </div>
          <div v-else-if="books.length === 0" class="py-24 text-center text-outline">
            <span class="material-symbols-outlined text-5xl mb-4 opacity-40">menu_book</span>
            <p v-if="keyword">未找到与「{{ keyword }}」相关的图书</p>
            <p v-else>暂无符合条件的图书</p>
          </div>
          <div v-else class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-x-gutter gap-y-12">
            <article v-for="b in books" :key="b.id" class="group">
              <div class="relative mb-4 overflow-hidden bg-surface-container-low p-4 aspect-[3/4] flex items-center justify-center">
                <RouterLink
                  :to="bookDetailPath(b.id)"
                  class="block w-full h-full"
                  :title="`查看《${b.title}》详情`"
                  @click="onOpenBookDetail"
                >
                  <img
                    class="w-full h-full object-cover shadow-lg transition-transform duration-500 group-hover:scale-105"
                    :src="resolveBookCover(b.coverUrl)"
                    :alt="b.title"
                    loading="lazy"
                    @error="onCoverError"
                  />
                </RouterLink>
                <span
                  v-if="b.stock <= 0"
                  class="absolute top-3 left-3 bg-stone-800/80 text-white text-[10px] px-2 py-1 uppercase tracking-wider"
                >
                  缺货
                </span>
                <button
                  type="button"
                  class="absolute bottom-3 right-3 bg-primary text-white p-2.5 opacity-0 group-hover:opacity-100 translate-y-2 group-hover:translate-y-0 transition-all duration-300 disabled:opacity-40 z-10"
                  :disabled="b.stock <= 0"
                  title="加入购物车"
                  @click.stop="addToCart(b)"
                >
                  <span class="material-symbols-outlined text-xl">add_shopping_cart</span>
                </button>
              </div>
              <p v-if="categoryNameOf(b.categoryId)" class="text-[11px] uppercase tracking-wider text-secondary mb-1">
                {{ categoryNameOf(b.categoryId) }}
              </p>
              <RouterLink :to="bookDetailPath(b.id)" class="block" @click="onOpenBookDetail">
                <h4 class="font-headline-md text-base text-primary mb-1 line-clamp-2 min-h-[2.5rem] group-hover:text-secondary transition-colors" :title="b.title">
                  {{ b.title }}
                </h4>
              </RouterLink>
              <p class="text-sm text-outline mb-1 truncate">{{ b.author || '未知作者' }}</p>
              <p v-if="bookDescriptionExcerpt(b.description)" class="text-xs text-outline-variant mb-2 line-clamp-2 leading-relaxed">
                {{ bookDescriptionExcerpt(b.description) }}
              </p>
              <div class="flex items-baseline justify-between gap-2">
                <span class="text-primary font-semibold">¥{{ formatPrice(b.price) }}</span>
                <span class="text-[11px] text-outline-variant">ISBN {{ b.isbn || '—' }}</span>
              </div>
            </article>
          </div>

          <nav v-if="totalPages > 1" class="mt-16 flex justify-center items-center gap-3">
            <button
              type="button"
              class="w-10 h-10 flex items-center justify-center border border-outline-variant text-outline hover:border-primary hover:text-primary disabled:opacity-30"
              :disabled="page <= 1"
              @click="goPage(page - 1)"
            >
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <div class="flex items-center gap-1.5">
              <button
                v-for="p in pageNumbers"
                :key="`p-${p}`"
                type="button"
                class="min-w-10 h-10 px-2 flex items-center justify-center text-sm"
                :class="p === page ? 'bg-primary text-white font-medium' : 'border border-outline-variant text-outline hover:border-primary hover:text-primary'"
                @click="typeof p === 'number' && goPage(p)"
              >
                {{ p }}
              </button>
            </div>
            <button
              type="button"
              class="w-10 h-10 flex items-center justify-center border border-outline-variant text-outline hover:border-primary hover:text-primary disabled:opacity-30"
              :disabled="page >= totalPages"
              @click="goPage(page + 1)"
            >
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </nav>
        </div>
      </div>
    </main>

    <p
      v-if="cartToast"
      class="fixed bottom-8 left-1/2 z-50 -translate-x-1/2 rounded bg-[#2C2420] px-5 py-2.5 text-sm text-white shadow-lg"
    >
      {{ cartToast }}
    </p>

    <footer class="w-full py-16 px-8 md:px-16 grid grid-cols-1 md:grid-cols-2 gap-8 items-end mt-16 bg-stone-50 border-t border-stone-200">
      <div class="flex flex-col gap-4">
        <div class="font-serif text-lg italic text-[#2C2420]">馆藏书屋</div>
        <p class="text-stone-500 max-w-sm text-xs tracking-wider font-light leading-loose">致力为全球藏书家与智识追求者提供最纯粹的阅读资源与最优雅的浏览体验。</p>
      </div>
      <div class="md:text-right text-stone-500 text-xs tracking-wider font-light">© 2024 馆藏书屋 · 藏书分类</div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { markPortalBookNavigation, tryRestorePendingPortalScroll } from '../../utils/portalScrollRestore'
import {
  bookDescriptionExcerpt,
  formatPrice,
  portalBooksList,
  portalCategories,
  resolveBookCover,
  type PortalBook,
  type PortalCategory,
} from '../../api/portal/catalog'
import { userCartAdd } from '../../api/user/cart'
import { userMe } from '../../api/user/auth'

const route = useRoute()
const router = useRouter()

const categories = ref<PortalCategory[]>([])
const books = ref<PortalBook[]>([])
const total = ref(0)
const totalAll = ref(0)
const page = ref(1)
const pageSize = 12
const loading = ref(false)
const errorMsg = ref('')
const cartToast = ref('')
let cartToastTimer: ReturnType<typeof setTimeout> | null = null
const cartAddingId = ref<number | null>(null)

const keywordInput = ref('')
const keyword = ref('')
const selectedCategoryId = ref<number | null>(null)
const priceMinInput = ref('')
const priceMaxInput = ref('')
const priceMin = ref<number | null>(null)
const priceMax = ref<number | null>(null)
const pricePreset = ref('all')
const sort = ref<'default' | 'price_asc' | 'price_desc' | 'title'>('default')

const sortOptions = [
  { key: 'default' as const, label: '综合排序' },
  { key: 'price_asc' as const, label: '价格升序' },
  { key: 'price_desc' as const, label: '价格降序' },
  { key: 'title' as const, label: '书名' },
]

const pricePresets = [
  { key: 'all', label: '不限价格', min: null as number | null, max: null as number | null },
  { key: 'p1', label: '¥0 - ¥50', min: 0, max: 50 },
  { key: 'p2', label: '¥50 - ¥150', min: 50, max: 150 },
  { key: 'p3', label: '¥150 以上', min: 150, max: null },
]

const topCategories = computed(() => categories.value.filter((c) => !c.parentId))
const categoryMap = computed(() => new Map(categories.value.map((c) => [c.id, c])))
const selectedCategoryName = computed(() => {
  if (selectedCategoryId.value == null) return ''
  const cat = categoryMap.value.get(selectedCategoryId.value)
  if (!cat) return ''
  if (cat.parentId) {
    const parent = categoryMap.value.get(cat.parentId)
    return parent ? `${parent.name} · ${cat.name}` : cat.name
  }
  return cat.name
})
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const pageNumbers = computed(() => {
  const tp = totalPages.value
  const cur = page.value
  if (tp <= 7) return Array.from({ length: tp }, (_, i) => i + 1)
  const pages: (number | string)[] = [1]
  if (cur > 3) pages.push('…')
  for (let i = Math.max(2, cur - 1); i <= Math.min(tp - 1, cur + 1); i++) pages.push(i)
  if (cur < tp - 2) pages.push('…')
  pages.push(tp)
  return pages
})

function childCategories(parentId: number) {
  return categories.value.filter((c) => c.parentId === parentId)
}

function isCategoryActive(id: number) {
  if (selectedCategoryId.value === id) return true
  const cat = categoryMap.value.get(id)
  if (cat && !cat.parentId) {
    return childCategories(id).some((sub) => sub.id === selectedCategoryId.value)
  }
  return false
}

function bookDetailPath(id: number) {
  return { name: 'portal-book', params: { id: String(id) } }
}

function onOpenBookDetail() {
  markPortalBookNavigation('list', route.fullPath)
}

function categoryNameOf(id: number | null | undefined) {
  if (id == null) return ''
  return categoryMap.value.get(id)?.name || ''
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function showCartToast(msg: string) {
  cartToast.value = msg
  if (cartToastTimer) clearTimeout(cartToastTimer)
  cartToastTimer = setTimeout(() => {
    cartToast.value = ''
  }, 2200)
}

async function addToCart(b: PortalBook) {
  if (b.stock <= 0 || cartAddingId.value != null) return

  cartAddingId.value = b.id
  try {
    const me = await userMe()
    if (!(me.success && me.data)) {
      showCartToast('请先登录')
      router.push({ path: '/user/login', query: { redirect: route.fullPath } })
      return
    }
    const res = await userCartAdd(b.id, 1)
    if (res.success) {
      showCartToast(res.data?.message || `《${b.title}》已加入购物车`)
    } else {
      showCartToast(res.message || '加入购物车失败')
      if (res.message?.includes('未登录')) {
        router.push({ path: '/user/login', query: { redirect: route.fullPath } })
      }
    }
  } catch {
    showCartToast('网络异常，请稍后重试')
  } finally {
    cartAddingId.value = null
  }
}

function applyPricePreset(p: (typeof pricePresets)[number]) {
  priceMinInput.value = p.min != null ? String(p.min) : ''
  priceMaxInput.value = p.max != null ? String(p.max) : ''
  applyPriceFilter()
}

function applyPriceFilter() {
  priceMin.value = priceMinInput.value === '' ? null : Number(priceMinInput.value)
  priceMax.value = priceMaxInput.value === '' ? null : Number(priceMaxInput.value)
  page.value = 1
  syncRoute()
  loadBooks()
}

function applyKeyword() {
  keyword.value = keywordInput.value.trim()
  page.value = 1
  syncRoute()
  loadBooks()
}

function selectCategory(id: number | null) {
  selectedCategoryId.value = id
  page.value = 1
  syncRoute()
  loadBooks()
}

function changeSort(key: typeof sort.value) {
  sort.value = key
  page.value = 1
  syncRoute()
  loadBooks()
}

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  syncRoute()
  loadBooks()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resetFilters() {
  keywordInput.value = ''
  keyword.value = ''
  selectedCategoryId.value = null
  priceMinInput.value = ''
  priceMaxInput.value = ''
  priceMin.value = null
  priceMax.value = null
  pricePreset.value = 'all'
  sort.value = 'default'
  page.value = 1
  syncRoute()
  loadBooks()
}

function syncRoute() {
  const q: Record<string, string> = {}
  if (selectedCategoryId.value != null) q.categoryId = String(selectedCategoryId.value)
  if (keyword.value) q.keyword = keyword.value
  if (priceMin.value != null) q.priceMin = String(priceMin.value)
  if (priceMax.value != null) q.priceMax = String(priceMax.value)
  if (sort.value !== 'default') q.sort = sort.value
  if (page.value > 1) q.page = String(page.value)
  router.replace({ path: '/portal/list', query: q })
}

function readRouteQuery() {
  const q = route.query
  selectedCategoryId.value = q.categoryId ? Number(q.categoryId) : null
  keyword.value = typeof q.keyword === 'string' ? q.keyword : ''
  keywordInput.value = keyword.value
  priceMin.value = q.priceMin ? Number(q.priceMin) : null
  priceMax.value = q.priceMax ? Number(q.priceMax) : null
  priceMinInput.value = priceMin.value != null ? String(priceMin.value) : ''
  priceMaxInput.value = priceMax.value != null ? String(priceMax.value) : ''
  sort.value = (typeof q.sort === 'string' ? q.sort : 'default') as typeof sort.value
  page.value = q.page ? Math.max(1, Number(q.page)) : 1
}

async function loadCategories() {
  const res = await portalCategories()
  if (res.success && res.data) {
    categories.value = res.data
  }
}

async function loadTotalAll() {
  const res = await portalBooksList({ page: 1, size: 1 })
  if (res.success && res.data) {
    totalAll.value = res.data.total || 0
  }
}

async function loadBooks() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await portalBooksList({
      page: page.value,
      size: pageSize,
      keyword: keyword.value,
      categoryId: selectedCategoryId.value,
      priceMin: priceMin.value,
      priceMax: priceMax.value,
      sort: sort.value,
    })
    if (res.success && res.data) {
      books.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      errorMsg.value = res.message || '加载图书失败'
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
  } finally {
    loading.value = false
    await nextTick()
    tryRestorePendingPortalScroll('list')
  }
}

onMounted(async () => {
  readRouteQuery()
  await Promise.all([loadCategories(), loadTotalAll()])
  await loadBooks()
})

watch(
  () => route.query,
  () => {
    readRouteQuery()
    loadBooks()
  },
)
</script>
