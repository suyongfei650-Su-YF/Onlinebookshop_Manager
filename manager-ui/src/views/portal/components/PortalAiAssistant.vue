<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  portalAiChat,
  portalAiStatus,
  type AiChatMessage,
  type PortalAiAction,
  type PortalAiBookCard,
} from '../../../api/portal/ai'
import { formatPrice, resolveBookCover } from '../../../api/portal/catalog'
import { userMe } from '../../../api/user/auth'
import { userCartAdd } from '../../../api/user/cart'
import { userFavoriteAdd, userFavoriteRemove } from '../../../api/user/favorites'
import { USER_AUTH_EVENT, notifyUserCartChanged } from '../../../utils/userSession'

const router = useRouter()

const open = ref(false)
const input = ref('')
const loading = ref(false)
const aiEnabled = ref(false)
const aiModel = ref('')
const loggedIn = ref(false)
const userNickname = ref('')
const cartCount = ref<number | null>(null)
const favoriteCount = ref<number | null>(null)
const favoriteBookIds = ref<number[]>([])
const addingBookId = ref<number | null>(null)
const favoritingBookId = ref<number | null>(null)

const messages = ref<AiChatMessage[]>([
  {
    role: 'assistant',
    content:
      '您好，我是馆藏书屋智能阅读助手。可以帮您找书、做推荐；登录后还可管理购物车与个人收藏，例如「查看我的收藏」「根据收藏推荐」。',
  },
])

const listRef = ref<HTMLElement | null>(null)

const quickPrompts = [
  '推荐几本热销图书',
  '根据收藏推荐',
  '查看我的收藏',
  '把第一本加入购物车',
]

function statusSubtitle() {
  const ai = aiEnabled.value ? `大模型已连接 · ${aiModel.value || 'AI'}` : '馆藏检索模式'
  if (loggedIn.value) {
    const name = userNickname.value || '读者'
    const cart =
      cartCount.value != null && cartCount.value >= 0 ? ` · 购物车 ${cartCount.value} 种` : ''
    const fav =
      favoriteCount.value != null && favoriteCount.value >= 0
        ? ` · 收藏 ${favoriteCount.value} 册`
        : ''
    return `${ai} · 已登录：${name}${cart}${fav}`
  }
  return `${ai} · 游客模式（找书/说明；加购与收藏需登录）`
}

function isBookFavorited(bookId: number) {
  return favoriteBookIds.value.includes(bookId)
}

async function refreshUserState() {
  const statusRes = await portalAiStatus()
  if (statusRes.success && statusRes.data) {
    aiEnabled.value = !!statusRes.data.enabled
    aiModel.value = statusRes.data.model || ''
    if (typeof statusRes.data.loggedIn === 'boolean') {
      loggedIn.value = statusRes.data.loggedIn
      userNickname.value = statusRes.data.userNickname || ''
      cartCount.value =
        statusRes.data.cartCount != null ? Number(statusRes.data.cartCount) : null
      favoriteCount.value =
        statusRes.data.favoriteCount != null ? Number(statusRes.data.favoriteCount) : null
      if (statusRes.data.favoriteBookIds) {
        favoriteBookIds.value = statusRes.data.favoriteBookIds.map(Number)
      }
      return
    }
  }
  const me = await userMe()
  loggedIn.value = !!(me.success && me.data)
  userNickname.value = me.data?.nickname || ''
}

function onAuthChanged() {
  void refreshUserState()
}

onMounted(async () => {
  await refreshUserState()
  window.addEventListener(USER_AUTH_EVENT, onAuthChanged)
})

onUnmounted(() => {
  window.removeEventListener(USER_AUTH_EVENT, onAuthChanged)
})

function toggle() {
  open.value = !open.value
  if (open.value) {
    void nextTick(() => scrollBottom())
  }
}

function scrollBottom() {
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function historyForApi() {
  return messages.value
    .filter((m) => m.role === 'user' || m.role === 'assistant')
    .slice(-10)
    .map((m) => ({ role: m.role, content: m.content }))
}

function applyChatMeta(data: {
  loggedIn?: boolean
  guest?: boolean
  userNickname?: string | null
  cartCount?: number | null
  favoriteCount?: number | null
  favoriteBookIds?: number[]
  actions?: PortalAiAction[]
}) {
  if (typeof data.loggedIn === 'boolean') {
    loggedIn.value = data.loggedIn
  }
  if (data.userNickname) {
    userNickname.value = data.userNickname
  }
  if (data.cartCount != null) {
    cartCount.value = Number(data.cartCount)
    notifyUserCartChanged(cartCount.value)
  } else if (data.actions?.some((a) => a.success && a.type === 'ADD_TO_CART' && a.cartCount != null)) {
    const added = data.actions.find((a) => a.type === 'ADD_TO_CART' && a.success)
    if (added?.cartCount != null) {
      cartCount.value = Number(added.cartCount)
      notifyUserCartChanged(cartCount.value)
    }
  }
  if (data.favoriteBookIds) {
    favoriteBookIds.value = data.favoriteBookIds.map(Number)
  }
  if (data.favoriteCount != null) {
    favoriteCount.value = Number(data.favoriteCount)
  } else if (data.actions?.some((a) => a.type === 'ADD_TO_FAVORITE' || a.type === 'REMOVE_FAVORITE')) {
    const favAction = data.actions.find(
      (a) =>
        (a.type === 'ADD_TO_FAVORITE' || a.type === 'REMOVE_FAVORITE') &&
        a.cartCount != null,
    )
    if (favAction?.cartCount != null) {
      favoriteCount.value = Number(favAction.cartCount)
    }
  }
}

async function send(text?: string) {
  const msg = (text ?? input.value).trim()
  if (!msg || loading.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: msg })
  loading.value = true
  await nextTick()
  scrollBottom()

  try {
    const res = await portalAiChat(msg, historyForApi().slice(0, -1))
    if (res.success && res.data) {
      applyChatMeta(res.data)
      messages.value.push({
        role: 'assistant',
        content: res.data.reply || '暂无回复',
        books: res.data.books,
        hint: res.data.hint,
        usedAi: res.data.usedAi,
        actions: res.data.actions,
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: res.message || '请求失败，请稍后重试',
      })
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '网络异常，请确认后端已启动后重试。',
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollBottom()
  }
}

function goLogin() {
  open.value = false
  void router.push({ path: '/user/login', query: { redirect: router.currentRoute.value.fullPath } })
}

async function addBookToCart(book: PortalAiBookCard, e?: Event) {
  e?.preventDefault()
  e?.stopPropagation()
  if (!book.id || addingBookId.value != null) return

  if (!loggedIn.value) {
    messages.value.push({
      role: 'assistant',
      content: '加入购物车需要先登录读者账号。',
      actions: [{ type: 'LOGIN_REQUIRED', success: false, message: '请先登录' }],
    })
    await nextTick()
    scrollBottom()
    return
  }

  addingBookId.value = book.id
  try {
    const res = await userCartAdd(book.id, 1)
    if (res.success) {
      const count = res.data?.cartCount
      if (count != null) {
        cartCount.value = Number(count)
        notifyUserCartChanged(cartCount.value)
      }
      messages.value.push({
        role: 'assistant',
        content: `已将《${book.title}》加入您的购物车。`,
        actions: [
          {
            type: 'ADD_TO_CART',
            success: true,
            bookId: book.id,
            bookTitle: book.title,
            cartCount: count != null ? Number(count) : null,
          },
        ],
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: res.message || '加入购物车失败',
      })
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '网络异常，加入购物车失败。',
    })
  } finally {
    addingBookId.value = null
    await nextTick()
    scrollBottom()
  }
}

async function toggleBookFavorite(book: PortalAiBookCard, e?: Event) {
  e?.preventDefault()
  e?.stopPropagation()
  if (!book.id || favoritingBookId.value != null) return

  if (!loggedIn.value) {
    messages.value.push({
      role: 'assistant',
      content: '收藏图书需要先登录读者账号。',
      actions: [{ type: 'LOGIN_REQUIRED', success: false, message: '请先登录' }],
    })
    await nextTick()
    scrollBottom()
    return
  }

  const favorited = isBookFavorited(book.id)
  favoritingBookId.value = book.id
  try {
    const res = favorited ? await userFavoriteRemove(book.id) : await userFavoriteAdd(book.id)
    if (res.success) {
      if (favorited) {
        favoriteBookIds.value = favoriteBookIds.value.filter((id) => id !== book.id)
        if (favoriteCount.value != null && favoriteCount.value > 0) {
          favoriteCount.value -= 1
        }
      } else {
        if (!favoriteBookIds.value.includes(book.id)) {
          favoriteBookIds.value = [...favoriteBookIds.value, book.id]
        }
        favoriteCount.value = (favoriteCount.value ?? favoriteBookIds.value.length - 1) + 1
      }
      messages.value.push({
        role: 'assistant',
        content: favorited
          ? `已取消收藏《${book.title}》。`
          : `已将《${book.title}》加入您的收藏。`,
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: res.message || (favorited ? '取消收藏失败' : '收藏失败'),
      })
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '网络异常，收藏操作失败。',
    })
  } finally {
    favoritingBookId.value = null
    await nextTick()
    scrollBottom()
  }
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}
</script>

<template>
  <div class="portal-ai-root fixed bottom-6 right-4 z-[300] sm:right-6">
    <Transition name="portal-ai-panel">
      <section
        v-if="open"
        class="portal-ai-panel mb-4 flex h-[min(520px,calc(100vh-7rem))] w-[min(400px,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-stone-700/80 bg-[#1f1916] text-stone-100 shadow-2xl shadow-black/50"
        aria-label="智能阅读助手"
      >
        <header class="flex items-center justify-between border-b border-stone-700/80 bg-[#2C2420] px-4 py-3">
          <div class="min-w-0">
            <h2 class="truncate font-headline-md text-sm text-[#D4AF37]">智能阅读助手</h2>
            <p class="truncate text-[11px] text-stone-400">
              {{ statusSubtitle() }}
            </p>
          </div>
          <button
            type="button"
            class="rounded-lg p-1.5 text-stone-400 hover:bg-stone-800 hover:text-white"
            aria-label="关闭"
            @click="open = false"
          >
            <span class="material-symbols-outlined text-xl">close</span>
          </button>
        </header>

        <div ref="listRef" class="flex-1 space-y-4 overflow-y-auto px-3 py-4">
          <div
            v-for="(m, idx) in messages"
            :key="idx"
            class="flex"
            :class="m.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[92%] rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed"
              :class="
                m.role === 'user'
                  ? 'bg-[#D4AF37] text-[#1a1410]'
                  : 'bg-stone-800/90 text-stone-100'
              "
            >
              <p class="whitespace-pre-wrap">{{ m.content }}</p>
              <p v-if="m.hint" class="mt-2 border-t border-stone-600/50 pt-2 text-[11px] text-stone-400">
                {{ m.hint }}
              </p>
              <div
                v-if="m.actions?.some((a) => a.type === 'LOGIN_REQUIRED')"
                class="mt-2 flex flex-wrap gap-2 border-t border-stone-600/50 pt-2"
              >
                <button
                  type="button"
                  class="rounded-lg bg-[#D4AF37] px-3 py-1.5 text-xs font-medium text-[#1a1410] hover:bg-[#e8c96a]"
                  @click="goLogin"
                >
                  去登录
                </button>
                <RouterLink
                  to="/portal/cart"
                  class="rounded-lg border border-stone-500 px-3 py-1.5 text-xs text-stone-200 hover:border-[#D4AF37]/60"
                  @click="open = false"
                >
                  购物车
                </RouterLink>
                <RouterLink
                  to="/portal/favorites"
                  class="rounded-lg border border-stone-500 px-3 py-1.5 text-xs text-stone-200 hover:border-[#D4AF37]/60"
                  @click="open = false"
                >
                  我的收藏
                </RouterLink>
              </div>
              <ul v-if="m.books?.length" class="mt-3 space-y-2 border-t border-stone-600/50 pt-2">
                <li v-for="book in m.books as PortalAiBookCard[]" :key="book.id">
                  <div class="flex gap-2 rounded-lg bg-stone-900/60 p-2">
                    <RouterLink
                      :to="{ name: 'portal-book', params: { id: String(book.id) } }"
                      class="flex min-w-0 flex-1 gap-2 transition hover:opacity-90"
                      @click="open = false"
                    >
                      <img
                        class="h-14 w-10 shrink-0 object-cover bg-stone-800"
                        :src="resolveBookCover(book.coverUrl)"
                        :alt="book.title"
                        @error="onCoverError"
                      />
                      <span class="min-w-0 flex-1">
                        <span class="line-clamp-2 text-xs font-medium text-white">{{ book.title }}</span>
                        <span class="mt-0.5 block truncate text-[11px] text-stone-400">
                          {{ book.author || '未知作者' }}
                          <template v-if="book.categoryName"> · {{ book.categoryName }}</template>
                        </span>
                        <span class="mt-0.5 block text-[11px] text-[#D4AF37]">¥ {{ formatPrice(book.price) }}</span>
                      </span>
                    </RouterLink>
                    <div class="flex shrink-0 flex-col gap-1 self-center">
                      <button
                        type="button"
                        class="rounded-lg border border-[#D4AF37]/50 px-2 py-1 text-[10px] text-[#D4AF37] hover:bg-[#D4AF37]/10 disabled:opacity-50"
                        :disabled="addingBookId === book.id"
                        :title="loggedIn ? '加入购物车' : '登录后加入购物车'"
                        @click="addBookToCart(book, $event)"
                      >
                        {{ addingBookId === book.id ? '…' : loggedIn ? '加购' : '登录加购' }}
                      </button>
                      <button
                        type="button"
                        class="rounded-lg border px-2 py-1 text-[10px] disabled:opacity-50"
                        :class="
                          book.id && isBookFavorited(book.id)
                            ? 'border-rose-400/60 text-rose-300 hover:bg-rose-950/40'
                            : 'border-stone-500 text-stone-300 hover:border-rose-400/50 hover:text-rose-300'
                        "
                        :disabled="favoritingBookId === book.id"
                        :title="loggedIn ? (book.id && isBookFavorited(book.id) ? '取消收藏' : '加入收藏') : '登录后收藏'"
                        @click="toggleBookFavorite(book, $event)"
                      >
                        {{
                          favoritingBookId === book.id
                            ? '…'
                            : !loggedIn
                              ? '登录收藏'
                              : book.id && isBookFavorited(book.id)
                                ? '已藏'
                                : '收藏'
                        }}
                      </button>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
          <div v-if="loading" class="flex justify-start">
            <div class="rounded-2xl bg-stone-800/90 px-4 py-2 text-sm text-stone-400">正在思考…</div>
          </div>
        </div>

        <div class="border-t border-stone-700/80 bg-[#1a1613] px-3 py-2">
          <div class="mb-2 flex flex-wrap gap-1.5">
            <button
              v-for="q in quickPrompts"
              :key="q"
              type="button"
              class="rounded-full border border-stone-600/60 px-2.5 py-1 text-[11px] text-stone-300 hover:border-[#D4AF37]/60 hover:text-[#D4AF37]"
              :disabled="loading"
              @click="send(q)"
            >
              {{ q }}
            </button>
          </div>
          <form class="flex gap-2" @submit.prevent="send()">
            <input
              v-model="input"
              type="text"
              class="min-w-0 flex-1 rounded-xl border border-stone-600/60 bg-stone-900/80 px-3 py-2 text-sm text-white placeholder-stone-500 focus:border-[#D4AF37] focus:outline-none focus:ring-1 focus:ring-[#D4AF37]/40"
              placeholder="找书、根据收藏推荐、加购或收藏…"
              maxlength="500"
              :disabled="loading"
            />
            <button
              type="submit"
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#D4AF37] text-[#1a1410] disabled:opacity-50"
              :disabled="loading || !input.trim()"
              aria-label="发送"
            >
              <span class="material-symbols-outlined">send</span>
            </button>
          </form>
        </div>
      </section>
    </Transition>

    <button
      type="button"
      class="portal-ai-fab flex h-14 w-14 items-center justify-center rounded-full bg-[#D4AF37] text-[#1a1410] shadow-lg shadow-black/40 transition hover:scale-105 hover:bg-[#e8c96a]"
      :aria-expanded="open"
      aria-label="打开智能阅读助手"
      @click="toggle"
    >
      <span class="material-symbols-outlined text-2xl">{{ open ? 'expand_more' : 'auto_awesome' }}</span>
    </button>
  </div>
</template>

<style scoped>
.portal-ai-panel-enter-active,
.portal-ai-panel-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.portal-ai-panel-enter-from,
.portal-ai-panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}
</style>
