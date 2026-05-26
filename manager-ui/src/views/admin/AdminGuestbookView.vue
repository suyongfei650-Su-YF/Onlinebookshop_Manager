<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  adminGuestbookClearReply,
  adminGuestbookList,
  adminGuestbookReply,
  type AdminGuestbookMessage,
} from '../../api/admin/guestbook'
import AdminProfileDialog from './components/AdminProfileDialog.vue'
import { useAdminNav } from './composables/useAdminNav'
import { useAdminLogout } from './composables/useAdminLogout'
import { useAdminProfile } from './composables/useAdminProfile'
import { hasUserAvatar, resolveUserAvatar, userAvatarInitials } from '../../utils/userAvatar'

const { nc } = useAdminNav()
const logout = useAdminLogout()
const router = useRouter()
const showProfileDialog = ref(false)
const { displayName, roleName, avatarUrl, updateProfile, changePassword } = useAdminProfile()

const messages = ref<AdminGuestbookMessage[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const replyFilter = ref<'' | 'pending' | 'replied'>('')
const pendingCount = ref(0)
const loading = ref(false)
const errorMsg = ref('')
const toastMsg = ref('')
const actingId = ref<number | null>(null)
const expandedId = ref<number | null>(null)
const replyDrafts = ref<Record<number, string>>({})
const avatarFailedIds = ref<Set<number>>(new Set())

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const filterTabs = computed(() => [
  { key: '' as const, label: '全部留言' },
  { key: 'pending' as const, label: '待回复' },
  { key: 'replied' as const, label: '已回复' },
])

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

function hasReply(msg: AdminGuestbookMessage) {
  return !!(msg.adminReply && msg.adminReply.trim())
}

function showAvatarImg(msg: AdminGuestbookMessage) {
  return !!msg.id && hasUserAvatar(msg.avatarUrl) && !avatarFailedIds.value.has(msg.id)
}

function avatarSrc(msg: AdminGuestbookMessage) {
  return resolveUserAvatar(msg.avatarUrl)
}

function onAvatarError(msg: AdminGuestbookMessage) {
  if (msg.id) {
    avatarFailedIds.value = new Set(avatarFailedIds.value).add(msg.id)
  }
}

function readerInitials(msg: AdminGuestbookMessage) {
  return userAvatarInitials(msg.nickname)
}

function draftFor(msg: AdminGuestbookMessage) {
  if (replyDrafts.value[msg.id] !== undefined) {
    return replyDrafts.value[msg.id]
  }
  return msg.adminReply || ''
}

function setDraft(id: number, value: string) {
  replyDrafts.value = { ...replyDrafts.value, [id]: value }
}

function toggleExpand(msg: AdminGuestbookMessage) {
  if (expandedId.value === msg.id) {
    expandedId.value = null
    return
  }
  expandedId.value = msg.id
  if (replyDrafts.value[msg.id] === undefined) {
    setDraft(msg.id, msg.adminReply || '')
  }
}

async function loadMessages() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminGuestbookList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      replyFilter: replyFilter.value,
    })
    if (res.success && res.data) {
      messages.value = res.data.list || []
      total.value = res.data.total || 0
      pendingCount.value = res.data.pendingCount ?? 0
    } else {
      const msg = res.message || '加载留言失败'
      if (msg.includes('未登录') || msg.includes('会话')) {
        void router.push({ path: '/admin/login', query: { redirect: '/admin/guestbook' } })
        return
      }
      errorMsg.value = msg
    }
  } catch {
      errorMsg.value =
        '网络异常，请确认后端已启动（8080）。若显示 Not Found，请刷新页面并确认已 Maven 编译 Java8 后重启后端。'
  } finally {
    loading.value = false
  }
}

function setFilter(key: '' | 'pending' | 'replied') {
  replyFilter.value = key
  page.value = 1
  void loadMessages()
}

function onSearch() {
  page.value = 1
  void loadMessages()
}

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  void loadMessages()
}

async function submitReply(msg: AdminGuestbookMessage) {
  const text = (replyDrafts.value[msg.id] ?? '').trim()
  if (!text || text.length < 2) {
    showToast('回复至少 2 个字')
    return
  }
  if (text.length > 500) {
    showToast('回复不能超过 500 字')
    return
  }
  actingId.value = msg.id
  try {
    const res = await adminGuestbookReply(msg.id, text)
    if (res.success && res.data) {
      const idx = messages.value.findIndex((m) => m.id === msg.id)
      if (idx >= 0) messages.value[idx] = res.data
      showToast('回复已保存，读者留言板可见')
      expandedId.value = null
      void loadMessages()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

async function clearReply(msg: AdminGuestbookMessage) {
  if (!hasReply(msg)) return
  if (!window.confirm('确定清除该条官方回复？读者留言板将不再显示。')) return
  actingId.value = msg.id
  try {
    const res = await adminGuestbookClearReply(msg.id)
    if (res.success && res.data) {
      const idx = messages.value.findIndex((m) => m.id === msg.id)
      if (idx >= 0) messages.value[idx] = res.data
      setDraft(msg.id, '')
      showToast('已清除回复')
      void loadMessages()
    } else {
      showToast(res.message || '清除失败')
    }
  } catch {
    showToast('网络异常')
  } finally {
    actingId.value = null
  }
}

onMounted(() => {
  void loadMessages()
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
      <span class="text-stone-600">留言管理 · guestbook_message</span>
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
          <h2 class="text-2xl font-bold text-stone-900">读者留言</h2>
          <p class="mt-1 text-sm text-stone-500">可选择性回复留言，回复将展示在门户「读者留言板」</p>
        </div>
        <a
          href="/portal/guestbook"
          target="_blank"
          rel="noopener"
          class="text-sm text-[#8b6914] hover:underline"
        >
          预览留言板 ↗
        </a>
      </div>

      <div class="mb-6 flex flex-wrap gap-2">
        <button
          v-for="tab in filterTabs"
          :key="tab.key || 'all'"
          type="button"
          class="rounded-full px-4 py-2 text-sm transition-colors"
          :class="
            replyFilter === tab.key
              ? 'bg-stone-900 text-white'
              : 'border border-stone-200 bg-white text-stone-600 hover:border-stone-400'
          "
          @click="setFilter(tab.key)"
        >
          {{ tab.label }}
          <span v-if="tab.key === 'pending'" class="ml-1 opacity-80">({{ pendingCount }})</span>
        </button>
      </div>

      <div class="mb-6 flex flex-wrap gap-3">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索留言、读者昵称或回复内容…"
          class="min-w-[240px] flex-1 rounded-xl border border-stone-200 bg-white px-4 py-2.5 text-sm focus:border-stone-400 focus:outline-none"
          @keyup.enter="onSearch"
        />
        <button
          type="button"
          class="rounded-xl bg-stone-900 px-6 py-2.5 text-sm font-medium text-white hover:bg-stone-800"
          @click="onSearch"
        >
          搜索
        </button>
      </div>

      <p v-if="errorMsg" class="mb-4 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-700">{{ errorMsg }}</p>
      <p
        v-if="toastMsg"
        class="fixed bottom-8 left-1/2 z-50 -translate-x-1/2 rounded-full bg-stone-900 px-6 py-2 text-sm text-white shadow-lg"
      >
        {{ toastMsg }}
      </p>

      <div v-if="loading" class="py-20 text-center text-stone-500">加载中…</div>

      <div v-else class="space-y-4">
        <article
          v-for="msg in messages"
          :key="msg.id"
          class="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="flex min-w-0 flex-1 gap-3">
              <div
                v-if="!showAvatarImg(msg)"
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-stone-200 bg-[#2C2420] text-sm font-semibold text-[#D4AF37]"
                :aria-label="(msg.nickname || '读者') + ' 头像'"
              >
                {{ readerInitials(msg) }}
              </div>
              <img
                v-else
                class="h-11 w-11 shrink-0 rounded-full border border-stone-200 bg-stone-100 object-cover"
                :src="avatarSrc(msg)"
                :alt="msg.nickname || '读者'"
                @error="onAvatarError(msg)"
              />
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-semibold text-stone-900">{{ msg.nickname || '读者' }}</span>
                  <span
                    class="rounded-full px-2 py-0.5 text-xs"
                    :class="
                      hasReply(msg)
                        ? 'bg-emerald-50 text-emerald-700'
                        : 'bg-amber-50 text-amber-800'
                    "
                  >
                    {{ hasReply(msg) ? '已回复' : '待回复' }}
                  </span>
                  <span v-if="msg.status === 'HIDDEN'" class="text-xs text-stone-400">已隐藏</span>
                </div>
                <time class="mt-1 block text-xs text-stone-400">{{ formatTime(msg.createdAt) }}</time>
                <p class="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-stone-700">{{ msg.content }}</p>
              </div>
            </div>
            <button
              type="button"
              class="shrink-0 rounded-lg border border-stone-200 px-3 py-1.5 text-xs text-stone-600 hover:bg-stone-50"
              @click="toggleExpand(msg)"
            >
              {{ expandedId === msg.id ? '收起' : hasReply(msg) ? '编辑回复' : '回复' }}
            </button>
          </div>

          <div
            v-if="hasReply(msg) && expandedId !== msg.id"
            class="mt-4 rounded-xl border border-amber-200/80 bg-amber-50/80 px-4 py-3"
          >
            <p class="text-xs font-medium text-amber-900">
              官方回复
              <span v-if="msg.replierName" class="font-normal text-amber-700">· {{ msg.replierName }}</span>
            </p>
            <p class="mt-1 whitespace-pre-wrap text-sm text-stone-800">{{ msg.adminReply }}</p>
            <time class="mt-2 block text-xs text-stone-400">{{ formatTime(msg.adminReplyAt) }}</time>
          </div>

          <div v-if="expandedId === msg.id" class="mt-4 border-t border-stone-100 pt-4">
            <label class="mb-2 block text-xs font-medium uppercase tracking-wider text-stone-500">
              {{ hasReply(msg) ? '修改官方回复' : '撰写官方回复（可选，保存后读者可见）' }}
            </label>
            <textarea
              :value="draftFor(msg)"
              rows="4"
              maxlength="500"
              class="w-full resize-y rounded-xl border border-stone-200 bg-stone-50 px-4 py-3 text-sm focus:border-stone-400 focus:outline-none"
              placeholder="感谢读者的反馈，我们将……"
              :disabled="actingId === msg.id"
              @input="setDraft(msg.id, ($event.target as HTMLTextAreaElement).value)"
            />
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <span class="text-xs text-stone-400">{{ draftFor(msg).trim().length }} / 500</span>
              <button
                type="button"
                class="rounded-lg bg-stone-900 px-5 py-2 text-sm text-white hover:bg-stone-800 disabled:opacity-50"
                :disabled="actingId === msg.id || draftFor(msg).trim().length < 2"
                @click="submitReply(msg)"
              >
                {{ actingId === msg.id ? '保存中…' : '保存回复' }}
              </button>
              <button
                v-if="hasReply(msg)"
                type="button"
                class="rounded-lg border border-stone-300 px-4 py-2 text-sm text-stone-600 hover:bg-stone-50 disabled:opacity-50"
                :disabled="actingId === msg.id"
                @click="clearReply(msg)"
              >
                清除回复
              </button>
              <button
                type="button"
                class="text-sm text-stone-400 hover:text-stone-600"
                @click="expandedId = null"
              >
                取消
              </button>
            </div>
          </div>
        </article>

        <p v-if="!messages.length" class="py-16 text-center text-stone-500">暂无留言</p>
      </div>

      <div v-if="totalPages > 1 && !loading" class="mt-8 flex justify-center gap-2">
        <button
          type="button"
          class="rounded-lg border border-stone-200 px-3 py-1.5 text-sm disabled:opacity-40"
          :disabled="page <= 1"
          @click="goPage(page - 1)"
        >
          上一页
        </button>
        <span class="px-3 py-1.5 text-sm text-stone-600">{{ page }} / {{ totalPages }}</span>
        <button
          type="button"
          class="rounded-lg border border-stone-200 px-3 py-1.5 text-sm disabled:opacity-40"
          :disabled="page >= totalPages"
          @click="goPage(page + 1)"
        >
          下一页
        </button>
      </div>
    </main>
  </div>
</template>
