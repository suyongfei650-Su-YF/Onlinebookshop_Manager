<template>
  <div class="font-body-md bg-background min-h-screen">
    <TopNav />
    <div class="flex pt-16">
      <UserSideNav active="profile" />
      <main class="ml-64 w-full px-16 py-12">
        <div class="max-w-[800px] mx-auto">
          <header class="flex items-center gap-8 mb-16">
            <img class="w-32 h-32 rounded-full object-cover border border-outline-variant/40" :src="avatarPreview" alt="头像" @error="onAvatarError" />
            <div>
              <h1 class="font-headline-lg text-headline-lg text-primary">{{ profile?.nickname || '个人中心' }}</h1>
              <p class="text-on-surface-variant">
                {{ profile?.roleTag ? profile.roleTag : '普通读者' }}
                <span v-if="profile?.status" class="ml-2 text-xs">
                  （{{ profile.status === 'ACTIVE' ? '正常' : '禁用' }}）
                </span>
              </p>
            </div>
          </header>
          <section class="bg-white p-10 rounded-lg shadow-sm">
            <h2 class="font-headline-md text-headline-md text-primary mb-6">基本信息</h2>
            <div v-if="loading" class="text-on-surface-variant">正在加载个人资料…</div>
            <div v-else>
              <p v-if="errorMsg" class="mb-4 rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">
                {{ errorMsg }}
              </p>
              <p v-if="successMsg" class="mb-4 rounded-lg bg-secondary-fixed px-3 py-2 text-sm text-on-secondary-fixed">
                {{ successMsg }}
              </p>

              <div class="space-y-6">
                <!-- 头像 -->
                <div class="rounded-lg border border-outline-variant/40 p-5">
                  <div class="flex items-start justify-between gap-6">
                    <div class="min-w-0">
                      <div class="text-sm text-on-surface-variant">头像（avatar_url）</div>
                      <div class="mt-1 text-xs text-on-surface-variant break-all">
                        {{ profile?.avatarUrl ? (profile.avatarUrl.startsWith('data:image/') ? 'base64(data URL)' : profile.avatarUrl) : '—' }}
                      </div>
                    </div>
                    <div class="shrink-0 flex items-center gap-2">
                      <input ref="avatarFileInputEl" type="file" accept="image/*" class="hidden" @change="onPickAvatarFile" />
                      <button
                        type="button"
                        class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary disabled:opacity-50"
                        :disabled="avatarConverting || fieldSaving.avatarUrl"
                        @click="avatarFileInputEl?.click()"
                      >
                        {{ avatarConverting ? '处理中…' : '本地上传' }}
                      </button>
                      <button
                        type="button"
                        class="rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                        :disabled="!draft.avatarUrl || avatarConverting || fieldSaving.avatarUrl"
                        @click="saveField('avatarUrl')"
                      >
                        {{ fieldSaving.avatarUrl ? '保存中…' : '保存头像' }}
                      </button>
                      <button
                        v-if="draft.avatarUrl"
                        type="button"
                        class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary disabled:opacity-50"
                        :disabled="avatarConverting || fieldSaving.avatarUrl"
                        @click="draft.avatarUrl = ''"
                      >
                        清空
                      </button>
                    </div>
                  </div>
                  <div v-if="draft.avatarUrl" class="mt-4 flex items-center gap-4">
                    <img class="h-14 w-14 rounded-full object-cover border border-outline-variant/40" :src="draft.avatarUrl" alt="新头像预览" />
                    <div class="text-xs text-on-surface-variant">
                      已生成 base64 头像预览，点击“保存头像”写入数据库。
                    </div>
                  </div>
                </div>

                <!-- 昵称 -->
                <div class="rounded-lg border border-outline-variant/40 p-5">
                  <div class="flex items-start justify-between gap-6">
                    <div class="min-w-0 flex-1">
                      <div class="text-sm text-on-surface-variant">昵称（nickname）</div>
                      <div v-if="!editing.nickname" class="mt-1 text-on-surface">{{ profile?.nickname || '—' }}</div>
                      <div v-else class="mt-2">
                        <input
                          v-model="draft.nickname"
                          class="w-full border-0 border-b border-outline-variant focus:ring-0 focus:border-secondary bg-transparent"
                          placeholder="请输入昵称"
                          maxlength="64"
                        />
                      </div>
                    </div>
                    <div class="shrink-0 flex items-center gap-2">
                      <button
                        v-if="!editing.nickname"
                        type="button"
                        class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary"
                        @click="startEdit('nickname')"
                      >
                        修改
                      </button>
                      <template v-else>
                        <button
                          type="button"
                          class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary disabled:opacity-50"
                          :disabled="fieldSaving.nickname"
                          @click="cancelEdit('nickname')"
                        >
                          取消
                        </button>
                        <button
                          type="button"
                          class="rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                          :disabled="fieldSaving.nickname"
                          @click="saveField('nickname')"
                        >
                          {{ fieldSaving.nickname ? '保存中…' : '保存' }}
                        </button>
                      </template>
                    </div>
                  </div>
                </div>

                <!-- 用户名 -->
                <div class="rounded-lg border border-outline-variant/40 p-5">
                  <div class="flex items-start justify-between gap-6">
                    <div class="min-w-0 flex-1">
                      <div class="text-sm text-on-surface-variant">用户名（username）</div>
                      <div v-if="!editing.username" class="mt-1 text-on-surface">{{ profile?.username || '—' }}</div>
                      <div v-else class="mt-2">
                        <input
                          v-model="draft.username"
                          class="w-full border-0 border-b border-outline-variant focus:ring-0 focus:border-secondary bg-transparent"
                          placeholder="3-32位字母/数字/下划线（留空表示清除）"
                          maxlength="64"
                        />
                      </div>
                    </div>
                    <div class="shrink-0 flex items-center gap-2">
                      <button
                        v-if="!editing.username"
                        type="button"
                        class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary"
                        @click="startEdit('username')"
                      >
                        修改
                      </button>
                      <template v-else>
                        <button
                          type="button"
                          class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary disabled:opacity-50"
                          :disabled="fieldSaving.username"
                          @click="cancelEdit('username')"
                        >
                          取消
                        </button>
                        <button
                          type="button"
                          class="rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                          :disabled="fieldSaving.username"
                          @click="saveField('username')"
                        >
                          {{ fieldSaving.username ? '保存中…' : '保存' }}
                        </button>
                      </template>
                    </div>
                  </div>
                </div>

                <!-- 手机号 -->
                <div class="rounded-lg border border-outline-variant/40 p-5">
                  <div class="flex items-start justify-between gap-6">
                    <div class="min-w-0 flex-1">
                      <div class="text-sm text-on-surface-variant">手机号（phone）</div>
                      <div v-if="!editing.phone" class="mt-1 text-on-surface">{{ profile?.phone || '—' }}</div>
                      <div v-else class="mt-2">
                        <input
                          v-model="draft.phone"
                          class="w-full border-0 border-b border-outline-variant focus:ring-0 focus:border-secondary bg-transparent"
                          placeholder="例如：13800000000（留空表示清除）"
                          maxlength="32"
                        />
                      </div>
                    </div>
                    <div class="shrink-0 flex items-center gap-2">
                      <button
                        v-if="!editing.phone"
                        type="button"
                        class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary"
                        @click="startEdit('phone')"
                      >
                        修改
                      </button>
                      <template v-else>
                        <button
                          type="button"
                          class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary disabled:opacity-50"
                          :disabled="fieldSaving.phone"
                          @click="cancelEdit('phone')"
                        >
                          取消
                        </button>
                        <button
                          type="button"
                          class="rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                          :disabled="fieldSaving.phone"
                          @click="saveField('phone')"
                        >
                          {{ fieldSaving.phone ? '保存中…' : '保存' }}
                        </button>
                      </template>
                    </div>
                  </div>
                </div>

                <!-- 邮箱 -->
                <div class="rounded-lg border border-outline-variant/40 p-5">
                  <div class="flex items-start justify-between gap-6">
                    <div class="min-w-0 flex-1">
                      <div class="text-sm text-on-surface-variant">邮箱（email）</div>
                      <div v-if="!editing.email" class="mt-1 text-on-surface">{{ profile?.email || '—' }}</div>
                      <div v-else class="mt-2">
                        <input
                          v-model="draft.email"
                          class="w-full border-0 border-b border-outline-variant focus:ring-0 focus:border-secondary bg-transparent"
                          placeholder="例如：name@example.com（留空表示清除）"
                          maxlength="128"
                        />
                      </div>
                    </div>
                    <div class="shrink-0 flex items-center gap-2">
                      <button
                        v-if="!editing.email"
                        type="button"
                        class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary"
                        @click="startEdit('email')"
                      >
                        修改
                      </button>
                      <template v-else>
                        <button
                          type="button"
                          class="rounded-md border border-outline-variant px-4 py-2 text-sm text-on-surface hover:border-secondary disabled:opacity-50"
                          :disabled="fieldSaving.email"
                          @click="cancelEdit('email')"
                        >
                          取消
                        </button>
                        <button
                          type="button"
                          class="rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                          :disabled="fieldSaving.email"
                          @click="saveField('email')"
                        >
                          {{ fieldSaving.email ? '保存中…' : '保存' }}
                        </button>
                      </template>
                    </div>
                  </div>
                </div>

                <!-- 时间信息 -->
                <div class="grid grid-cols-2 gap-6 text-xs text-on-surface-variant pt-2">
                  <div>注册时间：{{ profile?.createdAt ? formatDate(profile.createdAt) : '—' }}</div>
                  <div>更新时间：{{ profile?.updatedAt ? formatDate(profile.updatedAt) : '—' }}</div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import TopNav from './components/UserTopNav.vue'
import UserSideNav from './components/UserSideNav.vue'
import { userMe, userUpdateMe, type UserProfile } from '../../api/user/auth'

const loading = ref(true)
const errorMsg = ref('')
const successMsg = ref('')
const profile = ref<UserProfile | null>(null)

const fallbackAvatar = 'https://placehold.co/256x256/1f2937/f3f4f6?text=Avatar'
const avatarFileInputEl = ref<HTMLInputElement | null>(null)
const avatarConverting = ref(false)
const AVATAR_MAX_LEN = 2_000_000

const draft = reactive({
  nickname: '',
  username: '',
  email: '',
  phone: '',
  avatarUrl: '',
})

const editing = reactive({
  nickname: false,
  username: false,
  phone: false,
  email: false,
})

const fieldSaving = reactive({
  nickname: false,
  username: false,
  phone: false,
  email: false,
  avatarUrl: false,
})

const avatarPreview = computed(() => draft.avatarUrl?.trim() || profile.value?.avatarUrl?.trim() || fallbackAvatar)

function onAvatarError(e: Event) {
  const el = e.target as HTMLImageElement | null
  if (el) el.src = fallbackAvatar
}

function formatDate(raw: string) {
  // 后端可能返回 ISO 字符串；简单格式化到本地时间
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function fillForm(p: UserProfile) {
  draft.nickname = p.nickname || ''
  draft.username = p.username || ''
  draft.email = p.email || ''
  draft.phone = p.phone || ''
  draft.avatarUrl = p.avatarUrl || ''
}

function estimateDataUrlBytes(dataUrl: string) {
  const i = dataUrl.indexOf(',')
  if (i < 0) return dataUrl.length
  const b64 = dataUrl.slice(i + 1)
  const padding = b64.endsWith('==') ? 2 : b64.endsWith('=') ? 1 : 0
  return Math.floor((b64.length * 3) / 4) - padding
}

async function fileToDataUrl(file: File) {
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.onload = () => resolve(String(reader.result || ''))
    reader.readAsDataURL(file)
  })
}

async function loadImage(src: string) {
  return await new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('图片解析失败'))
    img.src = src
  })
}

async function compressImageToDataUrl(file: File, maxBytes: number) {
  const originalDataUrl = await fileToDataUrl(file)
  const img = await loadImage(originalDataUrl)

  const maxSide = 512
  const scale = Math.min(1, maxSide / Math.max(img.width || 1, img.height || 1))
  const w = Math.max(1, Math.round((img.width || 1) * scale))
  const h = Math.max(1, Math.round((img.height || 1) * scale))

  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('Canvas 不可用')
  ctx.drawImage(img, 0, 0, w, h)

  // 优先用 jpeg 压缩（png 对照片类图片通常更大）
  const mime = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
  let quality = mime === 'image/jpeg' ? 0.86 : 0.92
  let out = canvas.toDataURL(mime, quality)

  // 尝试在约束内；若不行降低质量
  for (let i = 0; i < 10 && estimateDataUrlBytes(out) > maxBytes; i++) {
    if (mime !== 'image/jpeg') {
      // png 不支持质量参数（浏览器会忽略）；改用 jpeg 再压缩
      out = canvas.toDataURL('image/jpeg', 0.86)
      quality = 0.86
      continue
    }
    quality = Math.max(0.4, quality - 0.08)
    out = canvas.toDataURL('image/jpeg', quality)
  }

  return out
}

async function onPickAvatarFile(e: Event) {
  const input = e.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) return
  // 允许重复选择同一文件也触发 change
  if (input) input.value = ''

  errorMsg.value = ''
  successMsg.value = ''

  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    return
  }
  // 粗略限制源文件大小（最终仍以 base64 长度为准）
  if (file.size > 8 * 1024 * 1024) {
    errorMsg.value = '图片过大（建议小于 8MB）'
    return
  }

  avatarConverting.value = true
  try {
    // 给 base64 留出一些头部空间（data:mime;base64,）
    const maxBytes = Math.floor((AVATAR_MAX_LEN - 64) * 0.75)
    const dataUrl = await compressImageToDataUrl(file, maxBytes)
    if (dataUrl.length > AVATAR_MAX_LEN) {
      errorMsg.value = '头像压缩后仍过大，请换小一点的图片'
      return
    }
    draft.avatarUrl = dataUrl
    successMsg.value = '头像已生成，点击“保存头像”即可生效'
  } catch (err) {
    errorMsg.value = (err as Error)?.message || '头像处理失败'
  } finally {
    avatarConverting.value = false
  }
}

function startEdit(field: 'nickname' | 'username' | 'phone' | 'email') {
  successMsg.value = ''
  errorMsg.value = ''
  editing[field] = true
  // draft 已在 fillForm 中同步过；这里不额外处理
}

function cancelEdit(field: 'nickname' | 'username' | 'phone' | 'email') {
  successMsg.value = ''
  errorMsg.value = ''
  editing[field] = false
  if (profile.value) fillForm(profile.value)
}

async function saveField(field: 'nickname' | 'username' | 'phone' | 'email' | 'avatarUrl') {
  successMsg.value = ''
  errorMsg.value = ''
  if (!profile.value) {
    errorMsg.value = '个人资料未加载'
    return
  }
  if (field === 'nickname' && !draft.nickname.trim()) {
    errorMsg.value = '昵称不能为空'
    return
  }

  fieldSaving[field] = true
  try {
    const body: Record<string, string | null> = {}
    if (field === 'nickname') body.nickname = draft.nickname.trim()
    if (field === 'username') body.username = draft.username.trim() || ''
    if (field === 'phone') body.phone = draft.phone.trim() || ''
    if (field === 'email') body.email = draft.email.trim() || ''
    if (field === 'avatarUrl') body.avatarUrl = draft.avatarUrl.trim() || ''

    const r = await userUpdateMe(body as any)
    if (!r.success || !r.data) {
      errorMsg.value = r.message || '保存失败'
      return
    }
    profile.value = r.data
    fillForm(r.data)
    if (field !== 'avatarUrl') {
      ;(editing as any)[field] = false
    }
    successMsg.value = '已保存'
  } finally {
    fieldSaving[field] = false
  }
}

async function loadProfile() {
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const r = await userMe()
    if (!r.success || !r.data) {
      errorMsg.value = r.message || '读取个人资料失败'
      profile.value = null
      return
    }
    profile.value = r.data
    fillForm(r.data)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>
