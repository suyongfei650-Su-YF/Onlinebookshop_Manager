<template>
  <main class="min-h-screen bg-background text-on-background font-body-md selection:bg-secondary-fixed selection:text-on-secondary-fixed">
    <header class="absolute left-0 right-0 top-0 z-20 flex items-center justify-between px-6 py-6">
      <div class="flex items-center gap-4">
        <button
          type="button"
          class="text-xs tracking-widest text-on-surface-variant hover:text-primary"
          @click="openHelpDialog"
        >
          帮助中心
        </button>
        <button
          type="button"
          class="text-xs tracking-widest text-on-surface-variant hover:text-primary"
          @click="openNoticeDialog"
        >
          系统公告
        </button>
      </div>
      <div class="flex items-center gap-3">
        <RouterLink
          to="/portal"
          class="rounded-full border border-outline-variant px-5 py-2 text-xs tracking-widest text-on-surface-variant transition hover:border-primary hover:text-primary"
        >
          返回主页
        </RouterLink>
        <RouterLink
          to="/admin/login"
          class="rounded-full bg-primary px-5 py-2 text-xs tracking-widest text-on-primary shadow-lg shadow-primary/20 hover:bg-primary-container"
        >
          管理员入口
        </RouterLink>
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

    <div class="flex min-h-screen items-center justify-center px-gutter py-section-gap relative overflow-hidden">
      <div class="absolute inset-0 z-0 opacity-10 pointer-events-none">
        <div class="absolute top-[-10%] left-[-5%] w-[40%] h-[60%] bg-secondary-fixed-dim blur-[120px] rounded-full"></div>
        <div class="absolute bottom-[-10%] right-[-5%] w-[40%] h-[60%] bg-primary-fixed-dim blur-[120px] rounded-full"></div>
      </div>

      <div class="relative z-10 w-full max-w-[1000px] flex flex-col md:flex-row bg-surface-container-lowest shadow-2xl overflow-hidden min-h-[680px]">
        <div class="hidden md:flex md:w-1/2 relative flex-col justify-end p-margin-page bg-primary overflow-hidden">
          <img
            class="absolute inset-0 w-full h-full object-cover opacity-60"
            src="../../assets/remote/1c1dd47457be1814.png"
            alt="视觉背景"
          />
          <div class="relative z-10">
            <h1 class="font-headline-lg text-white mb-stack-sm tracking-widest">馆藏书屋</h1>
            <p class="font-body-md text-on-primary-container max-w-[280px]">
              当代学者之选，于故纸堆中寻找文明的温度。开启您的专属阅读之旅。
            </p>
          </div>
          <div class="absolute inset-0 bg-gradient-to-t from-primary via-transparent to-transparent opacity-80"></div>
        </div>

        <div class="w-full md:w-1/2 flex flex-col p-8 md:p-12 lg:p-16">
          <div class="flex items-center gap-stack-lg mb-12">
            <button
              type="button"
              class="font-headline-md pb-2 transition-all"
              :class="isLogin ? 'text-on-surface border-b-2 border-secondary' : 'text-on-tertiary-container hover:text-on-surface'"
              @click="isLogin = true"
            >
              登录
            </button>
            <button
              type="button"
              class="font-headline-md pb-2 transition-all"
              :class="!isLogin ? 'text-on-surface border-b-2 border-secondary' : 'text-on-tertiary-container hover:text-on-surface'"
              @click="isLogin = false"
            >
              注册
            </button>
          </div>

          <h2 class="font-headline-lg text-on-surface mb-stack-lg">{{ isLogin ? '欢迎归来' : '创建账号' }}</h2>

          <p v-if="infoMsg" class="mb-4 rounded-lg bg-secondary-container/40 px-3 py-2 text-sm text-secondary">
            {{ infoMsg }}
          </p>
          <p v-if="errorMsg" class="mb-4 rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">
            {{ errorMsg }}
          </p>
          <form class="space-y-stack-lg" @submit.prevent="onSubmit">
            <label class="block">
              <span class="font-label-sm text-on-surface-variant">账号 / 邮箱 / 手机号</span>
              <input
                v-model="account"
                class="w-full mt-2 py-3 border-0 border-b border-outline-variant bg-transparent focus:ring-0 focus:border-secondary"
                placeholder="请输入您的账号"
                type="text"
                autocomplete="username"
              />
            </label>

            <label v-if="!isLogin" class="block">
              <span class="font-label-sm text-on-surface-variant">昵称</span>
              <input
                v-model="nickname"
                class="w-full mt-2 py-3 border-0 border-b border-outline-variant bg-transparent focus:ring-0 focus:border-secondary"
                placeholder="请输入昵称"
                type="text"
              />
            </label>

            <label class="block">
              <span class="font-label-sm text-on-surface-variant">{{ isLogin ? '登录密码' : '设置密码' }}</span>
              <input
                v-model="password"
                class="w-full mt-2 py-3 border-0 border-b border-outline-variant bg-transparent focus:ring-0 focus:border-secondary"
                placeholder="请输入您的密码"
                type="password"
                :autocomplete="isLogin ? 'current-password' : 'new-password'"
              />
            </label>

            <label v-if="!isLogin" class="block">
              <span class="font-label-sm text-on-surface-variant">确认密码</span>
              <input
                v-model="confirmPassword"
                class="w-full mt-2 py-3 border-0 border-b border-outline-variant bg-transparent focus:ring-0 focus:border-secondary"
                placeholder="请再次输入密码"
                type="password"
                autocomplete="new-password"
              />
            </label>

            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <span class="font-label-sm text-on-surface-variant">图形验证码</span>
                <span class="text-[11px] text-outline">不区分大小写</span>
              </div>
              <div class="flex items-center gap-3">
                <input
                  v-model="captcha"
                  class="flex-1 py-3 border-0 border-b border-outline-variant bg-transparent focus:ring-0 focus:border-secondary"
                  placeholder="请输入右侧字符"
                  type="text"
                  autocomplete="off"
                  maxlength="8"
                />
                <button
                  type="button"
                  class="flex h-[40px] w-[112px] shrink-0 cursor-pointer overflow-hidden rounded-lg border border-outline-variant/50 bg-white transition-opacity hover:opacity-90"
                  title="点击刷新验证码"
                  @click="refreshCaptcha"
                >
                  <img v-if="captchaSrc" class="h-full w-full object-contain" :src="captchaSrc" alt="验证码" @error="onCaptchaImageError" />
                  <span v-else class="flex h-full w-full items-center justify-center text-xs text-outline">点击刷新</span>
                </button>
              </div>
            </div>

            <div class="pt-4">
              <button
                class="w-full bg-primary text-on-primary font-headline-md py-4 rounded hover:bg-on-surface transition-all duration-300 shadow-lg active:scale-[0.98]"
                type="submit"
                :disabled="loading"
              >
                {{ loading ? (isLogin ? '登录中…' : '注册中…') : isLogin ? '立即登录' : '立即注册' }}
              </button>
            </div>
          </form>

          <div class="mt-10">
            <div class="flex items-center gap-4 mb-6">
              <div class="h-[1px] flex-grow bg-surface-variant"></div>
              <span class="font-label-sm text-on-tertiary-container uppercase tracking-widest whitespace-nowrap">其他登录方式</span>
              <div class="h-[1px] flex-grow bg-surface-variant"></div>
            </div>
            <div class="flex justify-center">
              <RouterLink
                to="/user/wechat-login"
                class="group flex flex-col items-center gap-2 focus:outline-none"
                title="微信扫码登录"
              >
                <div class="flex h-12 w-12 items-center justify-center rounded-full border border-outline-variant/50 text-on-surface-variant transition-all duration-300 hover:border-secondary hover:bg-secondary/5 hover:text-secondary">
                  <span class="material-symbols-outlined">chat</span>
                </div>
                <span class="font-label-sm text-[10px] text-outline transition-colors group-hover:text-secondary">微信扫码登录</span>
              </RouterLink>
            </div>
          </div>

          <div class="mt-8 text-center text-xs text-on-tertiary-container">
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchUserCaptchaObjectUrl } from '../../api/user/http'
import { userLogin, userRegister } from '../../api/user/auth'
import { notifyUserAuthChanged } from '../../utils/userSession'

const route = useRoute()

const isLogin = ref(true)
const loading = ref(false)
const errorMsg = ref('')
const infoMsg = ref('')

const account = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const captcha = ref('')

const captchaSrc = ref('')
let prevObjectUrl = ''

const showHelpDialog = ref(false)
const showNoticeDialog = ref(false)
const noticeText = ref('')

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
    '1. 用户端登录/注册页面已上线验证码与扫码登录入口。',
    '2. 为保障账号安全，请勿将验证码或密码透露给他人。',
    '3. 若遇到页面异常，可刷新后重试，或联系帮助中心。',
  ].join('\n')
  showNoticeDialog.value = true
}
function closeNoticeDialog() {
  showNoticeDialog.value = false
}

async function refreshCaptcha() {
  errorMsg.value = ''
  try {
    const { objectUrl } = await fetchUserCaptchaObjectUrl()
    if (prevObjectUrl.startsWith('blob:')) URL.revokeObjectURL(prevObjectUrl)
    prevObjectUrl = objectUrl
    captchaSrc.value = objectUrl
  } catch {
    captchaSrc.value = ''
  } finally {
    captcha.value = ''
  }
}

function onCaptchaImageError() {
  // 不在用户端暴露后端错误细节
  captchaSrc.value = ''
}

async function onSubmit() {
  errorMsg.value = ''

  if (!account.value.trim()) {
    errorMsg.value = '请输入账号 / 邮箱 / 手机号'
    return
  }
  if (!password.value.trim()) {
    errorMsg.value = '请输入密码'
    return
  }
  if (!captcha.value.trim()) {
    errorMsg.value = '请输入图形验证码'
    return
  }
  if (!isLogin.value) {
    if (!nickname.value.trim()) {
      errorMsg.value = '请输入昵称'
      return
    }
    if (password.value !== confirmPassword.value) {
      errorMsg.value = '两次输入的密码不一致'
      return
    }
  }

  loading.value = true
  try {
    if (isLogin.value) {
      const r = await userLogin({
        account: account.value.trim(),
        password: password.value,
        captcha: captcha.value.trim(),
      })
      if (!r.success) {
        errorMsg.value = r.message || '登录失败'
        await refreshCaptcha()
        return
      }
    } else {
      const r = await userRegister({
        account: account.value.trim(),
        nickname: nickname.value.trim(),
        password: password.value,
        captcha: captcha.value.trim(),
      })
      if (!r.success) {
        errorMsg.value = r.message || '注册失败'
        await refreshCaptcha()
        return
      }
      isLogin.value = true
    }
    notifyUserAuthChanged(true)
    sessionStorage.setItem('user:just-logged-in', String(Date.now()))
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const target = redirect.startsWith('/portal') ? redirect : '/portal/profile'
    window.location.assign(target)
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '网络异常，请稍后重试'
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.logout === '1') {
    infoMsg.value = '您已安全退出登录'
  }
  refreshCaptcha()
})
</script>
