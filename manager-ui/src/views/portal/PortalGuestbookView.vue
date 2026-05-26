<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  portalGuestbookList,
  type GuestbookMessage,
} from '../../api/portal/guestbook'
import { userGuestbookDelete, userGuestbookPost } from '../../api/user/guestbook'
import { userMe } from '../../api/user/auth'
import { hasUserAvatar, resolveUserAvatar, userAvatarInitials } from '../../utils/userAvatar'

const router = useRouter()

const loading = ref(true)
const loadingMore = ref(false)
const posting = ref(false)
const errorMsg = ref('')
const messages = ref<GuestbookMessage[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loggedIn = ref(false)
const draft = ref('')
const deletingId = ref<number | null>(null)
/** 加载失败的头像 id，回退为昵称首字占位 */
const avatarFailedIds = ref<Set<number>>(new Set())

const hasMore = computed(() => messages.value.length < total.value)

function showAvatarImg(msg: GuestbookMessage) {
  return !!msg.id && hasUserAvatar(msg.avatarUrl) && !avatarFailedIds.value.has(msg.id)
}

function avatarSrc(msg: GuestbookMessage) {
  return resolveUserAvatar(msg.avatarUrl)
}

function onAvatarError(msg: GuestbookMessage) {
  if (msg.id) {
    avatarFailedIds.value = new Set(avatarFailedIds.value).add(msg.id)
  }
}

function initials(msg: GuestbookMessage) {
  return userAvatarInitials(msg.nickname)
}

function formatTime(raw?: string | null) {
  if (!raw) return ''
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

async function refreshLogin() {
  try {
    const r = await userMe()
    loggedIn.value = !!(r.success && r.data)
  } catch {
    loggedIn.value = false
  }
}

async function loadMessages(reset = false) {
  if (reset) {
    page.value = 1
    messages.value = []
    loading.value = true
  } else {
    loadingMore.value = true
  }
  errorMsg.value = ''
  try {
    const res = await portalGuestbookList(page.value, pageSize)
    if (res.success && res.data) {
      const list = res.data.list || []
      if (reset) {
        messages.value = list
      } else {
        messages.value = [...messages.value, ...list]
      }
      total.value = res.data.total ?? messages.value.length
      if (typeof res.data.loggedIn === 'boolean') {
        loggedIn.value = res.data.loggedIn
      }
    } else {
      const msg = res.message || '加载留言失败'
      errorMsg.value = msg.includes('404') || msg.includes('Not Found') || msg.includes('接口不存在')
        ? '留言接口未就绪：请重启后端（Maven 编译 Java8 后运行「启动后端 SpringBoot」），并确认已执行 sql/patch_guestbook_message.sql'
        : msg
    }
  } catch {
    errorMsg.value = '网络异常，请确认后端已启动（8080）且前端为 npm run dev（5173）'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  page.value += 1
  await loadMessages(false)
}

async function submitMessage() {
  const text = draft.value.trim()
  if (!text || posting.value) return
  if (!loggedIn.value) {
    void router.push({ path: '/user/login', query: { redirect: '/portal/guestbook' } })
    return
  }
  posting.value = true
  errorMsg.value = ''
  try {
    const res = await userGuestbookPost(text)
    if (res.success && res.data) {
      draft.value = ''
      messages.value = [{ ...res.data, mine: true }, ...messages.value]
      total.value += 1
    } else {
      errorMsg.value = res.message || '发布失败'
    }
  } catch {
    errorMsg.value = '网络异常，发布失败'
  } finally {
    posting.value = false
  }
}

async function removeMessage(msg: GuestbookMessage) {
  if (!msg.id || deletingId.value != null) return
  if (!confirm('确定删除这条留言吗？')) return
  deletingId.value = msg.id
  try {
    const res = await userGuestbookDelete(msg.id)
    if (res.success) {
      messages.value = messages.value.filter((m) => m.id !== msg.id)
      total.value = Math.max(0, total.value - 1)
    } else {
      errorMsg.value = res.message || '删除失败'
    }
  } catch {
    errorMsg.value = '网络异常，删除失败'
  } finally {
    deletingId.value = null
  }
}

function goLogin() {
  void router.push({ path: '/user/login', query: { redirect: '/portal/guestbook' } })
}

onMounted(async () => {
  await refreshLogin()
  await loadMessages(true)
})
</script>

<template>
  <div class="min-h-screen bg-[#f7f4ef] text-stone-800">
    <main class="mx-auto max-w-3xl px-4 pb-16 pt-20 sm:px-6">
      <header class="mb-10 text-center">
        <p class="font-label-sm uppercase tracking-[0.25em] text-[#8b6914]">Reader Guestbook</p>
        <h1 class="mt-2 font-headline-lg text-3xl text-[#2C2420] sm:text-4xl">读者留言板</h1>
        <p class="mt-3 text-sm leading-relaxed text-stone-600">
          分享您的阅读心得、购书体验或对馆藏书屋的建议。登录后即可留言；管理员可选择性官方回复，所有人可见。
        </p>
        <p v-if="!loading" class="mt-2 text-xs text-stone-500">共 {{ total }} 条留言</p>
      </header>

      <section class="mb-10 rounded-2xl border border-stone-200/80 bg-white p-5 shadow-sm sm:p-6">
        <h2 class="mb-3 text-sm font-semibold uppercase tracking-wider text-[#8b6914]">写下留言</h2>
        <template v-if="loggedIn">
          <textarea
            v-model="draft"
            rows="4"
            maxlength="500"
            class="w-full resize-y rounded-xl border border-stone-200 bg-stone-50/80 px-4 py-3 text-sm leading-relaxed focus:border-[#D4AF37] focus:outline-none focus:ring-1 focus:ring-[#D4AF37]/40"
            placeholder="例如：最近在读的文学书很不错，希望多上一些艺术设计类新书…"
            :disabled="posting"
          />
          <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
            <span class="text-xs text-stone-400">{{ draft.trim().length }} / 500</span>
            <button
              type="button"
              class="rounded-xl bg-[#2C2420] px-6 py-2.5 text-sm font-medium text-[#D4AF37] hover:bg-stone-800 disabled:opacity-50"
              :disabled="posting || draft.trim().length < 2"
              @click="submitMessage"
            >
              {{ posting ? '发布中…' : '发布留言' }}
            </button>
          </div>
        </template>
        <template v-else>
          <p class="text-sm text-stone-600">登录读者账号后即可参与留言。</p>
          <button
            type="button"
            class="mt-4 rounded-xl border border-[#D4AF37] bg-[#D4AF37]/10 px-6 py-2.5 text-sm font-medium text-[#8b6914] hover:bg-[#D4AF37]/20"
            @click="goLogin"
          >
            去登录
          </button>
        </template>
      </section>

      <p v-if="errorMsg" class="mb-4 rounded-lg bg-red-50 px-4 py-2 text-center text-sm text-red-700" role="alert">
        {{ errorMsg }}
      </p>

      <div v-if="loading" class="py-16 text-center text-stone-500">正在加载留言…</div>

      <ul v-else-if="messages.length" class="space-y-4">
        <li
          v-for="msg in messages"
          :key="msg.id"
          class="rounded-2xl border border-stone-200/80 bg-white p-5 shadow-sm"
        >
          <div class="flex gap-3">
            <div
              v-if="!showAvatarImg(msg)"
              class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-stone-200 bg-[#2C2420] text-sm font-semibold text-[#D4AF37]"
              :aria-label="(msg.nickname || '读者') + ' 头像'"
            >
              {{ initials(msg) }}
            </div>
            <img
              v-else
              class="h-11 w-11 shrink-0 rounded-full border border-stone-200 object-cover bg-stone-100"
              :src="avatarSrc(msg)"
              :alt="msg.nickname || '读者'"
              @error="onAvatarError(msg)"
            />
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <span class="font-medium text-[#2C2420]">{{ msg.nickname || '读者' }}</span>
                <time class="text-xs text-stone-400">{{ formatTime(msg.createdAt) }}</time>
              </div>
              <p class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-stone-700">{{ msg.content }}</p>
              <div
                v-if="msg.adminReply && msg.adminReply.trim()"
                class="mt-4 rounded-xl border border-[#D4AF37]/30 bg-[#D4AF37]/8 px-4 py-3"
              >
                <p class="text-xs font-semibold text-[#8b6914]">
                  馆藏书屋 官方回复
                  <span v-if="msg.replierName" class="font-normal text-stone-500">· {{ msg.replierName }}</span>
                </p>
                <p class="mt-1.5 whitespace-pre-wrap text-sm leading-relaxed text-stone-700">{{ msg.adminReply }}</p>
                <time v-if="msg.adminReplyAt" class="mt-2 block text-xs text-stone-400">
                  {{ formatTime(msg.adminReplyAt) }}
                </time>
              </div>
              <button
                v-if="msg.mine"
                type="button"
                class="mt-3 text-xs text-stone-400 hover:text-red-600 disabled:opacity-50"
                :disabled="deletingId === msg.id"
                @click="removeMessage(msg)"
              >
                {{ deletingId === msg.id ? '删除中…' : '删除我的留言' }}
              </button>
            </div>
          </div>
        </li>
      </ul>

      <div v-else class="rounded-2xl border border-dashed border-stone-300 bg-white/60 py-16 text-center">
        <span class="material-symbols-outlined mb-3 text-5xl text-stone-300">forum</span>
        <p class="text-stone-500">还没有留言，来做第一个分享阅读的读者吧。</p>
      </div>

      <div v-if="hasMore && !loading" class="mt-8 text-center">
        <button
          type="button"
          class="rounded-full border border-stone-300 px-8 py-2.5 text-sm text-stone-600 hover:border-[#D4AF37] hover:text-[#8b6914] disabled:opacity-50"
          :disabled="loadingMore"
          @click="loadMore"
        >
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </div>

      <p class="mt-12 text-center text-xs text-stone-400">
        <RouterLink to="/portal" class="hover:text-[#8b6914]">返回首页</RouterLink>
        <span class="mx-2">·</span>
        <RouterLink to="/portal/list" class="hover:text-[#8b6914]">浏览藏书</RouterLink>
      </p>
    </main>
  </div>
</template>
