<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchAdminCaptchaObjectUrl } from '../../api/admin/http'
import { adminLogin } from '../../api/admin/auth'

const router = useRouter()
const username = ref('admin')
const password = ref('admin123')
const captcha = ref('')
const rememberMe = ref(false)
const captchaSrc = ref('')
const errorMsg = ref('')
const loading = ref(false)
const showHelpDialog = ref(false)
const showNoticeDialog = ref(false)
let prevObjectUrl = ''
const noticeText = ref('')

async function refreshCaptcha() {
  try {
    const { objectUrl } = await fetchAdminCaptchaObjectUrl()
    if (prevObjectUrl.startsWith('blob:')) URL.revokeObjectURL(prevObjectUrl)
    prevObjectUrl = objectUrl
    captchaSrc.value = objectUrl
  } catch (e) {
    errorMsg.value = `验证码加载失败：${e instanceof Error ? e.message : String(e)}`
  }
  captcha.value = ''
}

onMounted(async () => {
  await refreshCaptcha()
})

async function onSubmit() {
  errorMsg.value = ''
  if (!captcha.value.trim()) {
    errorMsg.value = '请输入图形验证码'
    return
  }
  loading.value = true
  const res = await adminLogin({
    username: username.value,
    password: password.value,
    captcha: captcha.value.trim(),
    rememberMe: rememberMe.value,
  })
  if (!res.success) {
    errorMsg.value = res.message || '登录失败'
    await refreshCaptcha()
    loading.value = false
    return
  }

  // 整页跳转，避免 router.push 与路由守卫竞态导致“登录成功却无法进入后台”
  sessionStorage.setItem('admin:just-logged-in', String(Date.now()))
  const redirect = typeof router.currentRoute.value.query.redirect === 'string'
    ? router.currentRoute.value.query.redirect
    : '/admin/dashboard'
  const target = redirect.startsWith('/admin') && !redirect.startsWith('/admin/login') ? redirect : '/admin/dashboard'
  window.location.assign(target)
}

function openHelpDialog() {
  showHelpDialog.value = true
}

function closeHelpDialog() {
  showHelpDialog.value = false
}

function openNoticeDialog() {
  const now = new Date()
  const dateText = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  noticeText.value = [
    `【系统公告】更新时间：${dateText}`,
    '1. 管理后台近期已优化登录与验证码体验。',
    '2. 为保障账号安全，请定期修改管理员密码。',
    '3. 若遇到数据异常，请先刷新页面后重试操作。',
    '4. 如有问题可通过帮助中心联系方式反馈。',
  ].join('\n')
  showNoticeDialog.value = true
}

function closeNoticeDialog() {
  showNoticeDialog.value = false
}
</script>

<template>
  <div
    class="stone-texture flex min-h-screen flex-col bg-background text-on-background selection:bg-secondary-container selection:text-on-secondary-container"
  >
    <header class="fixed left-0 right-0 top-0 z-50 flex h-16 items-center justify-between px-margin-page">
      <div class="flex items-center gap-3">
        <span class="font-headline-lg text-headline-md font-extrabold tracking-tighter text-primary">馆藏书屋</span>
        <span class="h-4 w-[1px] bg-outline"></span>
        <span class="font-label-sm text-label-sm uppercase tracking-widest text-on-surface-variant">Archivist</span>
      </div>
      <div class="flex items-center gap-stack-lg">
        <RouterLink
          to="/user/login"
          class="rounded-full bg-primary px-4 py-2 font-label-sm text-[11px] uppercase tracking-widest text-on-primary shadow-sm shadow-primary/10 transition hover:bg-primary-container"
        >
          返回普通用户登录
        </RouterLink>
        <button
          type="button"
          class="font-label-sm text-label-sm text-on-surface-variant transition-colors hover:text-primary"
          @click="openHelpDialog"
        >
          帮助中心
        </button>
        <button
          type="button"
          class="font-label-sm text-label-sm text-on-surface-variant transition-colors hover:text-primary"
          @click="openNoticeDialog"
        >
          系统公告
        </button>
      </div>
    </header>
    <div
      v-if="showHelpDialog"
      class="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 px-4"
      @click.self="closeHelpDialog"
    >
      <div class="w-full max-w-[520px] rounded-xl bg-surface-container-lowest p-6 shadow-2xl">
        <h2 class="mb-4 font-headline-lg text-headline-md text-primary">帮助中心</h2>
        <p class="mb-2 font-body-md text-on-surface">联系电话：8055-0166608</p>
        <p class="mb-2 font-body-md text-on-surface">邮箱地址：suyongfei650@gmail.com</p>
        <p class="mb-6 font-body-md text-on-surface-variant">如需帮助请将问题发往邮箱；或者拨打电话咨询</p>
        <div class="flex justify-end">
          <button
            type="button"
            class="rounded-lg bg-primary px-4 py-2 font-label-sm text-on-primary transition hover:bg-primary-container"
            @click="closeHelpDialog"
          >
            我知道了
          </button>
        </div>
      </div>
    </div>
    <div
      v-if="showNoticeDialog"
      class="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 px-4"
      @click.self="closeNoticeDialog"
    >
      <div class="w-full max-w-[560px] rounded-xl bg-surface-container-lowest p-6 shadow-2xl">
        <h2 class="mb-4 font-headline-lg text-headline-md text-primary">系统公告</h2>
        <p class="whitespace-pre-line font-body-md text-on-surface-variant">{{ noticeText }}</p>
        <div class="mt-6 flex justify-end">
          <button
            type="button"
            class="rounded-lg bg-primary px-4 py-2 font-label-sm text-on-primary transition hover:bg-primary-container"
            @click="closeNoticeDialog"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
    <main class="flex flex-1 items-center justify-center p-gutter">
      <div
        class="relative w-full max-w-[440px] overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-12 shadow-[0_32px_64px_-12px_rgba(44,36,32,0.12)]"
      >
        <div class="absolute left-0 top-0 h-full w-1 bg-primary-container"></div>
        <div class="mb-12">
          <h1 class="mb-2 font-headline-lg text-headline-lg text-primary">系统管理员登录</h1>
          <p class="font-body-md text-body-md text-on-surface-variant">请验证您的凭证以访问后台管理中心</p>
        </div>
        <p v-if="errorMsg" class="mb-4 rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">
          {{ errorMsg }}
        </p>
        <form class="space-y-stack-lg" @submit.prevent="onSubmit">
          <div class="space-y-2">
            <label class="ml-1 block font-label-sm text-label-sm text-on-surface-variant" for="username">管理员账号</label>
            <div class="group relative flex items-center">
              <span class="material-symbols-outlined absolute left-4 text-outline transition-colors group-focus-within:text-secondary">account_circle</span>
              <input
                id="username"
                v-model="username"
                autocomplete="username"
                class="w-full rounded-t-lg border-b-2 border-transparent bg-surface-container-low py-4 pl-12 pr-4 font-body-md text-on-surface transition-all focus:border-secondary focus:ring-0"
                placeholder="请输入用户名"
                type="text"
              />
            </div>
          </div>
          <div class="space-y-2">
            <label class="ml-1 block font-label-sm text-label-sm text-on-surface-variant" for="password">安全密码</label>
            <div class="group relative flex items-center">
              <span class="material-symbols-outlined absolute left-4 text-outline transition-colors group-focus-within:text-secondary">lock</span>
              <input
                id="password"
                v-model="password"
                autocomplete="current-password"
                class="w-full rounded-t-lg border-b-2 border-transparent bg-surface-container-low py-4 pl-12 pr-4 font-body-md text-on-surface transition-all focus:border-secondary focus:ring-0"
                placeholder="请输入密码"
                type="password"
              />
            </div>
          </div>
          <div class="space-y-2">
            <label class="ml-1 block font-label-sm text-label-sm text-on-surface-variant" for="captcha">图形验证码</label>
            <div class="flex gap-stack-md">
              <div class="group relative flex flex-1 items-center">
                <span class="material-symbols-outlined absolute left-4 text-outline transition-colors group-focus-within:text-secondary">verified_user</span>
                <input
                  id="captcha"
                  v-model="captcha"
                  autocomplete="off"
                  class="w-full rounded-t-lg border-b-2 border-transparent bg-surface-container-low py-4 pl-12 pr-4 font-body-md text-on-surface transition-all focus:border-secondary focus:ring-0"
                  placeholder="请输入右侧 4 位字符（不区分大小写）"
                  type="text"
                  maxlength="8"
                />
              </div>
              <button
                type="button"
                title="点击刷新验证码"
                class="flex h-[40px] w-[112px] shrink-0 cursor-pointer overflow-hidden rounded-lg border border-outline-variant/50 bg-white transition-opacity hover:opacity-90"
                @click="refreshCaptcha"
              >
                <img alt="验证码" class="h-full w-full object-contain" :src="captchaSrc"/>
              </button>
            </div>
            <p class="ml-1 font-label-sm text-[11px] text-outline">不区分大小写，看不清可点击图片刷新</p>
          </div>
          <div class="flex items-center justify-between pt-2">
            <label class="group flex cursor-pointer items-center gap-2">
              <input v-model="rememberMe" class="h-4 w-4 rounded-sm border-outline text-primary focus:ring-primary" type="checkbox" />
              <span class="font-label-sm text-label-sm text-on-surface-variant transition-colors group-hover:text-primary">保持登录（7 天）</span>
            </label>
            <RouterLink
              to="/admin/forgot"
              class="font-label-sm text-label-sm text-secondary underline decoration-outline-variant underline-offset-4 hover:text-on-secondary-container"
            >
              找回密码
            </RouterLink>
          </div>
          <button
            type="submit"
            :disabled="loading"
            class="mt-stack-lg flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-5 font-headline-md text-body-lg text-on-primary shadow-lg shadow-primary/10 transition-all hover:bg-primary-container active:scale-[0.98] disabled:opacity-60"
          >
            <span>{{ loading ? '登录中…' : '立即登录' }}</span>
            <span class="material-symbols-outlined text-[20px]">arrow_forward</span>
          </button>
          <p class="text-center font-label-sm text-label-sm text-on-surface-variant">
            还没有账号？
            <RouterLink to="/admin/register" class="text-secondary underline underline-offset-2 hover:text-on-secondary-container">管理员注册</RouterLink>
          </p>
          <div class="mb-6 mt-8 flex items-center gap-4">
            <div class="h-[1px] flex-1 bg-gradient-to-r from-transparent to-outline-variant/40"></div>
            <div class="h-[1px] flex-1 bg-gradient-to-l from-transparent to-outline-variant/40"></div>
          </div>
        </form>
        <div class="mt-12 text-center">
          <p class="font-label-sm text-label-sm text-outline">© 2024 馆藏书屋 Archivist Library System. All Rights Reserved.</p>
        </div>
      </div>
    </main>
    <div class="pointer-events-none fixed bottom-0 right-0 hidden p-margin-page opacity-10 lg:block">
      <span class="material-symbols-outlined text-[160px] text-primary">menu_book</span>
    </div>
    <div class="fixed bottom-0 left-0 h-[2px] w-full bg-gradient-to-r from-transparent via-outline-variant/30 to-transparent"></div>
  </div>
</template>
