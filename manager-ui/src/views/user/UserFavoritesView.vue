<template>
  <div class="font-body-md min-h-screen bg-background">
    <TopNav />
    <div class="flex pt-16">
      <UserSideNav active="favorites" />
      <main class="ml-64 w-full p-margin-page">
        <header class="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-outline-variant pb-6">
          <div>
            <h1 class="font-display-xl text-display-xl text-primary">我的收藏</h1>
            <p class="mt-2 text-on-surface-variant">珍藏您的每一次精神邂逅。</p>
          </div>
          <span v-if="!loading && !errorMsg" class="text-xs uppercase tracking-widest text-outline">
            共 {{ books.length }} 册
          </span>
        </header>

        <div v-if="loading" class="py-24 text-center text-outline">正在加载收藏…</div>

        <div v-else-if="errorMsg" class="py-24 text-center">
          <p class="mb-4 text-error">{{ errorMsg }}</p>
          <button
            type="button"
            class="border border-primary px-6 py-2 text-primary hover:bg-primary/5"
            @click="loadFavorites"
          >
            重试
          </button>
        </div>

        <div v-else-if="books.length === 0" class="py-24 text-center">
          <span class="material-symbols-outlined mb-4 text-5xl text-outline/40">favorite</span>
          <p class="text-on-surface-variant">还没有收藏任何图书</p>
          <RouterLink
            to="/portal/list"
            class="mt-6 inline-block border border-secondary px-6 py-2.5 text-sm text-secondary hover:bg-secondary-container/20"
          >
            去藏书分类逛逛
          </RouterLink>
        </div>

        <div v-else class="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <article
            v-for="book in books"
            :key="book.favoriteId"
            class="group relative border border-outline-variant bg-surface-container-lowest p-4 transition-shadow hover:shadow-lg"
          >
            <button
              type="button"
              class="absolute right-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 text-outline shadow hover:text-error disabled:opacity-50"
              :disabled="removingId === book.id"
              title="取消收藏"
              @click.stop="removeFavorite(book)"
            >
              <span class="material-symbols-outlined text-lg">{{ removingId === book.id ? 'hourglass_empty' : 'close' }}</span>
            </button>

            <RouterLink :to="bookDetailPath(book.id)" class="block">
              <div class="relative mb-4 aspect-[3/4] overflow-hidden bg-surface-container-low">
                <img
                  class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  :src="resolveBookCover(book.coverUrl)"
                  :alt="book.title"
                  loading="lazy"
                  @error="onCoverError"
                />
                <span
                  v-if="book.status !== 'ON_SHELF'"
                  class="absolute left-2 top-2 bg-stone-800/85 px-2 py-0.5 text-[10px] uppercase tracking-wider text-white"
                >
                  已下架
                </span>
                <span
                  v-else-if="book.stock <= 0"
                  class="absolute left-2 top-2 bg-stone-800/85 px-2 py-0.5 text-[10px] uppercase tracking-wider text-white"
                >
                  缺货
                </span>
              </div>
              <p v-if="book.categoryName" class="mb-1 text-[10px] uppercase tracking-wider text-secondary">
                {{ book.categoryName }}
              </p>
              <h3 class="line-clamp-2 font-headline-md text-base text-primary group-hover:text-secondary">
                {{ book.title }}
              </h3>
              <p class="mt-1 truncate text-sm text-outline">{{ book.author || '未知作者' }}</p>
              <p class="mt-2 font-semibold text-secondary">¥ {{ formatPrice(book.price) }}</p>
              <p v-if="book.favoritedAt" class="mt-2 text-[10px] text-outline-variant">
                收藏于 {{ formatFavoritedAt(book.favoritedAt) }}
              </p>
            </RouterLink>
          </article>
        </div>

        <p v-if="toastMsg" class="fixed bottom-8 right-8 z-50 rounded bg-[#2C2420] px-4 py-2 text-sm text-white shadow-lg">
          {{ toastMsg }}
        </p>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { formatPrice, resolveBookCover } from '../../api/portal/catalog'
import {
  userFavoriteRemove,
  userFavoritesList,
  type FavoriteBook,
} from '../../api/user/favorites'
import TopNav from './components/UserTopNav.vue'
import UserSideNav from './components/UserSideNav.vue'

const books = ref<FavoriteBook[]>([])
const loading = ref(true)
const errorMsg = ref('')
const removingId = ref<number | null>(null)
const toastMsg = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

function bookDetailPath(id: number) {
  return { name: 'portal-book', params: { id: String(id) } }
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function formatFavoritedAt(raw: string) {
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function showToast(msg: string) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMsg.value = ''
  }, 2200)
}

async function loadFavorites() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await userFavoritesList()
    if (res.success && res.data) {
      books.value = res.data
    } else {
      errorMsg.value = res.message || '加载收藏失败'
      books.value = []
    }
  } catch {
    errorMsg.value = '无法连接服务器，请确认后端已启动'
    books.value = []
  } finally {
    loading.value = false
  }
}

async function removeFavorite(book: FavoriteBook) {
  if (removingId.value != null) return
  removingId.value = book.id
  try {
    const res = await userFavoriteRemove(book.id)
    if (res.success) {
      books.value = books.value.filter((b) => b.id !== book.id)
      showToast(res.data?.message || '已取消收藏')
    } else {
      showToast(res.message || '取消收藏失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    removingId.value = null
  }
}

onMounted(loadFavorites)
</script>
