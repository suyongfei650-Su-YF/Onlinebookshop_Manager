<template>
  <header
    class="portal-site-header fixed top-0 left-0 z-[200] flex h-20 w-full items-center justify-between bg-[#2C2420] px-4 text-white shadow-2xl shadow-stone-900/20 sm:px-8 md:px-16"
  >
    <div class="relative z-[210] flex min-w-0 flex-1 items-center gap-4 md:gap-10">
      <button type="button" class="portal-brand group shrink-0" @click="goHome">
        <span class="portal-brand-emblem" aria-hidden="true">館</span>
        <span class="portal-brand-text">
          <span class="portal-brand-title">馆藏书屋</span>
          <span class="portal-brand-sub">Rare Book Archive</span>
        </span>
      </button>
      <nav
        class="relative z-[210] flex min-w-0 items-center gap-4 overflow-x-auto md:gap-8 font-manrope text-xs sm:text-sm font-medium tracking-[0.05em] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        <RouterLink to="/portal/list" class="portal-nav-link" :class="{ 'portal-nav-link--active': isListPage }">
          藏书分类
        </RouterLink>
        <button type="button" class="portal-nav-link" @click="goHomeSection('new-arrivals')">新品上架</button>
        <button type="button" class="portal-nav-link hidden sm:inline" @click="goHomeSection('hot-ranking')">
          热销版本
        </button>
      </nav>
    </div>
    <div class="relative z-[210] flex shrink-0 items-center gap-4 md:gap-6">
      <form
        class="portal-search relative z-[220] min-w-0 max-w-[9rem] sm:max-w-[11rem] md:max-w-none md:w-64"
        @submit.prevent="searchAndGo"
      >
        <input
          v-model="searchText"
          class="w-full border-none bg-stone-800/50 py-2 pl-3 pr-9 text-sm text-white placeholder-stone-500 focus:ring-1 focus:ring-[#D4AF37]"
          placeholder="探索馆藏书籍..."
          type="search"
          enterkeyhint="search"
          autocomplete="off"
          aria-label="搜索馆藏书籍"
          :aria-expanded="showSuggestPanel"
          aria-controls="portal-search-suggest"
          @focus="onSearchFocus"
          @blur="onSearchBlur"
          @input="scheduleSuggestions"
        />
        <button
          type="submit"
          class="portal-search-submit absolute right-1 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded text-stone-400 hover:text-[#D4AF37]"
          aria-label="搜索"
        >
          <span class="material-symbols-outlined text-lg">search</span>
        </button>

        <div
          v-if="showSuggestPanel"
          id="portal-search-suggest"
          class="portal-suggest-panel absolute left-0 right-0 top-[calc(100%+6px)] overflow-hidden rounded border border-stone-600/80 bg-[#1f1916] shadow-2xl shadow-black/50 md:left-auto md:right-0 md:w-[22rem]"
          role="listbox"
          @mousedown.prevent
        >
          <div v-if="suggestLoading" class="px-4 py-3 text-xs text-stone-400">正在匹配馆藏…</div>
          <template v-else-if="suggestBooks.length">
            <ul class="max-h-72 overflow-y-auto py-1">
              <li v-for="book in suggestBooks" :key="book.id" role="option">
                <button
                  type="button"
                  class="portal-suggest-item flex w-full items-center gap-3 px-3 py-2.5 text-left hover:bg-stone-800/80"
                  @click="openSuggestBook(book.id)"
                >
                  <img
                    class="h-12 w-9 shrink-0 object-cover bg-stone-800"
                    :src="resolveBookCover(book.coverUrl)"
                    :alt="book.title"
                    loading="lazy"
                    @error="onSuggestCoverError"
                  />
                  <span class="min-w-0 flex-1">
                    <span class="block truncate text-sm text-white">{{ book.title }}</span>
                    <span class="mt-0.5 block truncate text-xs text-stone-400">
                      {{ book.author || '未知作者' }} · ¥{{ formatPrice(book.price) }}
                    </span>
                  </span>
                </button>
              </li>
            </ul>
            <button
              v-if="suggestTotal > suggestBooks.length"
              type="button"
              class="w-full border-t border-stone-700/80 px-3 py-2.5 text-center text-xs tracking-wider text-[#D4AF37] hover:bg-stone-800/60"
              @click="searchAndGo"
            >
              查看全部 {{ suggestTotal }} 条结果
            </button>
          </template>
          <div v-else class="px-4 py-3 text-xs text-stone-400">未找到相关图书</div>
        </div>
      </form>
      <RouterLink class="relative hover:opacity-80" to="/portal/cart">
        <span class="material-symbols-outlined">shopping_cart</span>
      </RouterLink>
      <RouterLink class="hover:opacity-80" :to="profileLink">
        <img
          v-if="userAvatarUrl"
          class="h-7 w-7 rounded-full border border-white/40 object-cover"
          :src="userAvatarUrl"
          alt="用户头像"
          @error="onAvatarError"
        />
        <span v-else class="material-symbols-outlined">person</span>
      </RouterLink>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { userMe } from '../../../api/user/auth'
import { formatPrice, portalBooksList, resolveBookCover, type PortalBook } from '../../../api/portal/catalog'
import { navigateToPortalBook } from '../../../utils/portalScrollRestore'
import { scrollToPortalHomeSection, type PortalHomeSectionId } from '../../../utils/portalHomeScroll'
import { USER_AUTH_EVENT } from '../../../utils/userSession'

const SUGGEST_SIZE = 6
const SUGGEST_DEBOUNCE_MS = 280

const router = useRouter()
const route = useRoute()
const searchText = ref('')
const userAvatarUrl = ref('')
const suggestBooks = ref<PortalBook[]>([])
const suggestTotal = ref(0)
const suggestLoading = ref(false)
const suggestOpen = ref(false)
let suggestTimer: ReturnType<typeof setTimeout> | null = null
let suggestBlurTimer: ReturnType<typeof setTimeout> | null = null
let suggestSeq = 0

const showSuggestPanel = computed(() => suggestOpen.value && searchText.value.trim().length > 0)

const isListPage = computed(() => route.path === '/portal/list' || route.name === 'portal-list')
const isHomePage = computed(() => route.path === '/portal' || route.name === 'portal-home')
const profileLink = computed(() => (userAvatarUrl.value ? '/portal/profile' : '/user/login'))

function goHomeSection(sectionId: PortalHomeSectionId) {
  if (isHomePage.value) {
    scrollToPortalHomeSection(sectionId)
    return
  }
  router.push({ path: '/portal', hash: `#${sectionId}` }).catch(fallbackTo)
}

function goHome() {
  if (route.path === '/portal') return
  router.push('/portal').catch(fallbackTo)
}

function fallbackTo(err?: unknown) {
  if (err) console.warn('[portal-nav]', err)
}

function buildSearchQuery(categoryId?: number): Record<string, string> {
  const query: Record<string, string> = {}
  if (categoryId != null) query.categoryId = String(categoryId)
  const kw = searchText.value.trim()
  if (kw) query.keyword = kw
  return query
}

function goCatalog(categoryId?: number) {
  const query = buildSearchQuery(categoryId)
  router.push({ name: 'portal-list', query }).catch(() => {
    window.location.assign(router.resolve({ name: 'portal-list', query }).href)
  })
}

function closeSuggest() {
  suggestOpen.value = false
}

function searchAndGo() {
  closeSuggest()
  const query = buildSearchQuery()
  router
    .push({ name: 'portal-list', query })
    .then(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    })
    .catch(() => {
      window.location.assign(router.resolve({ name: 'portal-list', query }).href)
    })
}

async function loadSuggestions(kw: string) {
  const seq = ++suggestSeq
  suggestLoading.value = true
  try {
    const res = await portalBooksList({ page: 1, size: SUGGEST_SIZE, keyword: kw })
    if (seq !== suggestSeq) return
    if (res.success && res.data) {
      suggestBooks.value = res.data.list || []
      suggestTotal.value = res.data.total || 0
    } else {
      suggestBooks.value = []
      suggestTotal.value = 0
    }
  } catch {
    if (seq !== suggestSeq) return
    suggestBooks.value = []
    suggestTotal.value = 0
  } finally {
    if (seq === suggestSeq) suggestLoading.value = false
  }
}

function scheduleSuggestions() {
  if (suggestTimer) clearTimeout(suggestTimer)
  const kw = searchText.value.trim()
  if (!kw) {
    suggestSeq++
    suggestBooks.value = []
    suggestTotal.value = 0
    suggestLoading.value = false
    return
  }
  suggestOpen.value = true
  suggestTimer = setTimeout(() => {
    void loadSuggestions(kw)
  }, SUGGEST_DEBOUNCE_MS)
}

function onSearchFocus() {
  if (suggestBlurTimer) {
    clearTimeout(suggestBlurTimer)
    suggestBlurTimer = null
  }
  if (searchText.value.trim()) {
    suggestOpen.value = true
    scheduleSuggestions()
  }
}

function onSearchBlur() {
  suggestBlurTimer = setTimeout(() => {
    closeSuggest()
  }, 180)
}

function openSuggestBook(bookId: number) {
  closeSuggest()
  if (isListPage.value) {
    navigateToPortalBook(router, bookId, 'list', route.fullPath)
    return
  }
  navigateToPortalBook(router, bookId, 'home')
}

function onSuggestCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function syncSearchFromRoute() {
  if (!isListPage.value) return
  const kw = route.query.keyword
  searchText.value = typeof kw === 'string' ? kw : ''
}

watch(() => route.query.keyword, syncSearchFromRoute)
watch(isListPage, syncSearchFromRoute)
watch(
  () => route.fullPath,
  () => {
    closeSuggest()
  },
)

function onAvatarError() {
  userAvatarUrl.value = ''
}

async function loadUserAvatar() {
  try {
    const r = await userMe()
    userAvatarUrl.value = r.success && r.data ? r.data.avatarUrl?.trim() || '' : ''
  } catch {
    userAvatarUrl.value = ''
  }
}

function onUserAuthChanged(e: Event) {
  const detail = (e as CustomEvent<{ loggedIn?: boolean }>).detail
  if (detail?.loggedIn === false) {
    userAvatarUrl.value = ''
    return
  }
  void loadUserAvatar()
}

onMounted(() => {
  loadUserAvatar()
  syncSearchFromRoute()
  window.addEventListener(USER_AUTH_EVENT, onUserAuthChanged)
})

onUnmounted(() => {
  window.removeEventListener(USER_AUTH_EVENT, onUserAuthChanged)
  if (suggestTimer) clearTimeout(suggestTimer)
  if (suggestBlurTimer) clearTimeout(suggestBlurTimer)
})

defineExpose({ goCatalog })
</script>

<style scoped>
.portal-site-header button {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  font: inherit;
  color: inherit;
}

.portal-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-align: left;
}

.portal-brand-emblem {
  display: flex;
  height: 2.5rem;
  width: 2.5rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(212, 175, 55, 0.85);
  background: linear-gradient(145deg, rgba(212, 175, 55, 0.22), rgba(44, 36, 32, 0.2));
  font-family: 'Times New Roman', 'Songti SC', 'SimSun', serif;
  font-size: 1.15rem;
  color: #d4af37;
  box-shadow: 0 0 18px rgba(212, 175, 55, 0.2);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.portal-brand:hover .portal-brand-emblem {
  transform: translateY(-1px);
  box-shadow: 0 0 22px rgba(212, 175, 55, 0.35);
}

.portal-brand-text {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.portal-brand-title {
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: #fff;
  text-shadow: 0 1px 12px rgba(212, 175, 55, 0.28);
  line-height: 1.2;
}

.portal-brand-sub {
  font-size: 0.62rem;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: #a8a29e;
  line-height: 1;
}

.portal-nav-link {
  flex-shrink: 0;
  white-space: nowrap;
  padding-bottom: 0.125rem;
  color: #d6d3d1;
  text-decoration: none;
  transition: color 0.2s ease, border-color 0.2s ease;
}

.portal-nav-link:hover {
  color: #fff;
}

.portal-nav-link--active {
  color: #d4af37;
  border-bottom: 2px solid #d4af37;
}

.portal-search-submit {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
}

.portal-suggest-item {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  font: inherit;
  color: inherit;
}
</style>
