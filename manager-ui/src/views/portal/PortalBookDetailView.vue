<template>
  <div class="bg-background text-on-background font-body-md selection:bg-secondary-container selection:text-on-secondary-container min-h-screen">
    <main class="max-w-[1280px] mx-auto px-6 md:px-margin-page pt-28 pb-section-gap">
      <nav class="mb-8 flex flex-wrap items-center gap-2 text-sm text-outline">
        <RouterLink class="hover:text-secondary transition-colors" to="/portal">首页</RouterLink>
        <span class="material-symbols-outlined text-[14px]">chevron_right</span>
        <RouterLink class="hover:text-secondary transition-colors" to="/portal/list">藏书分类</RouterLink>
        <template v-if="book?.categoryName">
          <span class="material-symbols-outlined text-[14px]">chevron_right</span>
          <RouterLink
            class="hover:text-secondary transition-colors"
            :to="{ path: '/portal/list', query: { categoryId: String(book.categoryId) } }"
          >
            {{ book.categoryName }}
          </RouterLink>
        </template>
        <span class="material-symbols-outlined text-[14px]">chevron_right</span>
        <span class="text-primary line-clamp-1">{{ book?.title || '图书详情' }}</span>
      </nav>

      <div v-if="loading" class="py-24 text-center text-outline">正在加载图书信息…</div>
      <div v-else-if="errorMsg" class="py-24 text-center">
        <p class="text-error mb-4">{{ errorMsg }}</p>
        <RouterLink to="/portal/list" class="inline-block px-6 py-2 border border-primary text-primary hover:bg-primary/5">
          返回藏书分类
        </RouterLink>
      </div>

      <section v-else-if="book" class="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-start">
        <div class="lg:col-span-5 xl:col-span-4">
          <div class="sticky top-28 bg-surface-container-low p-6 aspect-[3/4] flex items-center justify-center shadow-lg">
            <img
              class="w-full h-full object-cover"
              :src="resolveBookCover(book.coverUrl)"
              :alt="book.title"
              @error="onCoverError"
            />
          </div>
        </div>

        <div class="lg:col-span-7 xl:col-span-8 flex flex-col gap-6">
          <div>
            <p v-if="book.categoryName" class="text-[11px] uppercase tracking-widest text-secondary mb-2">
              {{ book.categoryName }}
            </p>
            <h1 class="font-headline-lg text-primary text-2xl md:text-3xl leading-snug">{{ book.title }}</h1>
            <p class="mt-3 text-lg text-outline">{{ book.author || '未知作者' }}</p>
          </div>

          <div class="flex flex-wrap gap-x-8 gap-y-2 text-sm text-outline border-y border-outline-variant py-4">
            <span>ISBN <span class="text-on-background">{{ book.isbn || '—' }}</span></span>
            <span>库存 <span class="text-on-background">{{ book.stock > 0 ? `${book.stock} 册` : '暂时缺货' }}</span></span>
          </div>

          <div class="flex items-baseline gap-4">
            <span class="text-3xl font-semibold text-primary">¥{{ formatPrice(book.price) }}</span>
            <span
              v-if="book.stock <= 0"
              class="text-xs uppercase tracking-wider bg-stone-800/90 text-white px-2 py-1"
            >
              缺货
            </span>
          </div>

          <div class="flex flex-col sm:flex-row gap-3">
            <button
              type="button"
              class="flex-1 py-3 bg-primary text-white font-medium hover:bg-primary/90 transition-colors disabled:opacity-40"
              :disabled="book.stock <= 0 || cartBusy"
              @click="addToCart"
            >
              {{ cartBusy ? '处理中…' : '加入购物车' }}
            </button>
            <button
              type="button"
              class="flex-1 inline-flex items-center justify-center gap-2 py-3 border font-medium transition-colors disabled:opacity-50"
              :class="
                favorited
                  ? 'border-secondary bg-secondary-container/30 text-secondary'
                  : 'border-outline-variant text-primary hover:border-secondary hover:text-secondary'
              "
              :disabled="favoriteBusy"
              @click="toggleFavorite"
            >
              <span class="material-symbols-outlined text-xl" :class="{ 'icon-filled': favorited }">favorite</span>
              {{ favorited ? '已收藏' : '收藏' }}
            </button>
            <button
              type="button"
              class="flex-1 py-3 border border-secondary text-secondary text-center font-medium hover:bg-secondary-container/20 transition-colors sm:max-w-[10rem]"
              @click="onContinueBrowse"
            >
              继续浏览
            </button>
          </div>
          <p v-if="cartHint" class="text-sm text-secondary">{{ cartHint }}</p>
          <p v-if="favoriteHint" class="text-sm text-secondary">{{ favoriteHint }}</p>

          <article class="mt-4">
            <h2 class="font-headline-md text-primary mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-secondary">auto_stories</span>
              内容简介
            </h2>
            <div
              v-if="book.description?.trim()"
              class="text-on-background/90 leading-relaxed whitespace-pre-line text-base border-l-2 border-secondary/40 pl-5"
            >
              {{ book.description }}
            </div>
            <p v-else class="text-outline italic">暂无简介，欢迎选购后阅读全书精彩内容。</p>
          </article>
        </div>
      </section>
    </main>

    <footer class="w-full py-12 px-8 border-t border-stone-200 bg-stone-50 text-center text-stone-500 text-xs">
      © 2024 馆藏书屋 · 图书详情
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  formatPrice,
  portalBookDetail,
  resolveBookCover,
  type PortalBookDetail,
} from '../../api/portal/catalog'
import { userCartAdd } from '../../api/user/cart'
import { userFavoriteAdd, userFavoriteCheck, userFavoriteRemove } from '../../api/user/favorites'
import { userMe } from '../../api/user/auth'
import { continueBrowseFromBookDetail } from '../../utils/portalScrollRestore'

const route = useRoute()
const router = useRouter()
const book = ref<PortalBookDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const favorited = ref(false)
const favoriteBusy = ref(false)
const favoriteHint = ref('')
const cartHint = ref('')
const cartBusy = ref(false)
const loggedIn = ref(false)

function onContinueBrowse() {
  continueBrowseFromBookDetail(router)
}

function bookIdFromRoute(): number | null {
  const raw = route.params.id
  const id = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(id) && id > 0 ? id : null
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

async function addToCart() {
  const id = bookIdFromRoute()
  if (id == null || !book.value || cartBusy.value) return

  if (!loggedIn.value) {
    const me = await userMe()
    loggedIn.value = !!(me.success && me.data)
    if (!loggedIn.value) {
      cartHint.value = '请先登录后再加入购物车'
      router.push({ path: '/user/login', query: { redirect: route.fullPath } })
      return
    }
  }

  cartBusy.value = true
  cartHint.value = ''
  try {
    const res = await userCartAdd(id, 1)
    if (res.success) {
      cartHint.value = res.data?.message || `《${book.value.title}》已加入购物车`
    } else {
      cartHint.value = res.message || '加入购物车失败'
      if (res.message?.includes('未登录')) {
        router.push({ path: '/user/login', query: { redirect: route.fullPath } })
      }
    }
  } catch {
    cartHint.value = '网络异常，请稍后重试'
  } finally {
    cartBusy.value = false
  }
}

async function loadFavoriteState(bookId: number) {
  favorited.value = false
  favoriteHint.value = ''
  try {
    const me = await userMe()
    loggedIn.value = !!(me.success && me.data)
    if (!loggedIn.value) return
    const res = await userFavoriteCheck(bookId)
    if (res.success && res.data) {
      favorited.value = !!res.data.favorited
    }
  } catch {
    loggedIn.value = false
  }
}

async function toggleFavorite() {
  const id = bookIdFromRoute()
  if (id == null || favoriteBusy.value) return

  if (!loggedIn.value) {
    const me = await userMe()
    loggedIn.value = !!(me.success && me.data)
    if (!loggedIn.value) {
      favoriteHint.value = '请先登录后再收藏'
      router.push({ path: '/user/login', query: { redirect: route.fullPath } })
      return
    }
  }

  favoriteBusy.value = true
  favoriteHint.value = ''
  try {
    if (favorited.value) {
      const res = await userFavoriteRemove(id)
      if (res.success) {
        favorited.value = false
        favoriteHint.value = res.data?.message || '已取消收藏'
      } else {
        favoriteHint.value = res.message || '取消收藏失败'
        if (res.message?.includes('未登录')) {
          router.push({ path: '/user/login', query: { redirect: route.fullPath } })
        }
      }
    } else {
      const res = await userFavoriteAdd(id)
      if (res.success) {
        favorited.value = true
        favoriteHint.value = res.data?.message || '收藏成功'
      } else {
        favoriteHint.value = res.message || '收藏失败'
        if (res.message?.includes('未登录')) {
          router.push({ path: '/user/login', query: { redirect: route.fullPath } })
        }
      }
    }
  } catch {
    favoriteHint.value = '网络异常，请稍后重试'
  } finally {
    favoriteBusy.value = false
  }
}

async function loadBook() {
  const id = bookIdFromRoute()
  if (id == null) {
    errorMsg.value = '无效的图书编号'
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  book.value = null
  try {
    const res = await portalBookDetail(id)
    if (res.success && res.data) {
      book.value = res.data
      await loadFavoriteState(id)
    } else {
      errorMsg.value = res.message || '图书不存在或已下架'
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
  } finally {
    loading.value = false
  }
}

onMounted(loadBook)
watch(() => route.params.id, loadBook)
</script>

<style scoped>
.icon-filled {
  font-variation-settings: 'FILL' 1;
}
</style>
