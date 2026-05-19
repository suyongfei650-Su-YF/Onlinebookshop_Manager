<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchAdminCaptchaObjectUrl } from '../../api/admin/http'
import { adminRegister } from '../../api/admin/auth'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const displayName = ref('')
const email = ref('')
const captcha = ref('')
const captchaSrc = ref('')
const errorMsg = ref('')
const loading = ref(false)
let prevObjectUrl = ''

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
  if (password.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  if (!captcha.value.trim()) {
    errorMsg.value = '请输入图形验证码'
    return
  }
  loading.value = true
  try {
    const res = await adminRegister({
      username: username.value.trim(),
      password: password.value,
      displayName: displayName.value.trim(),
      email: email.value.trim(),
      captcha: captcha.value.trim(),
    })
    if (res.success) await router.push('/admin/login')
    else {
      errorMsg.value = res.message || '注册失败'
      refreshCaptcha()
    }
  } catch {
    errorMsg.value = '无法连接服务器'
    refreshCaptcha()
  } finally {
    loading.value = false
  }
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
        <span class="font-label-sm text-label-sm uppercase tracking-widest text-on-surface-variant">管理员注册</span>
      </div>
      <RouterLink to="/admin/login" class="font-label-sm text-label-sm text-secondary hover:underline">返回登录</RouterLink>
    </header>
    <main class="flex flex-1 items-center justify-center p-gutter pt-24">
      <div
        class="relative w-full max-w-[440px] overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-12 shadow-[0_32px_64px_-12px_rgba(44,36,32,0.12)]"
      >
        <div class="absolute left-0 top-0 h-full w-1 bg-secondary-container"></div>
        <h1 class="mb-2 font-headline-lg text-headline-lg text-primary">创建管理员账号</h1>
        <p v-if="errorMsg" class="mb-4 rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ errorMsg }}</p>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="ml-1 block font-label-sm text-on-surface-variant" for="reg-user">登录用户名</label>
            <input
              id="reg-user"
              v-model="username"
              class="mt-1 w-full rounded-lg border border-outline-variant/40 bg-surface-container-low px-4 py-3 font-body-md focus:border-secondary focus:outline-none"
              placeholder="3～64 个字符"
              autocomplete="username"
            />
          </div>
          <div>
            <label class="ml-1 block font-label-sm text-on-surface-variant" for="reg-dn">显示名称</label>
            <input
              id="reg-dn"
              v-model="displayName"
              class="mt-1 w-full rounded-lg border border-outline-variant/40 bg-surface-container-low px-4 py-3 font-body-md focus:border-secondary focus:outline-none"
              placeholder="后台展示用姓名"
            />
          </div>
          <div>
            <label class="ml-1 block font-label-sm text-on-surface-variant" for="reg-mail">邮箱（用于找回密码）</label>
            <input
              id="reg-mail"
              v-model="email"
              class="mt-1 w-full rounded-lg border border-outline-variant/40 bg-surface-container-low px-4 py-3 font-body-md focus:border-secondary focus:outline-none"
              placeholder="name@example.com"
              type="email"
              autocomplete="email"
            />
          </div>
          <div>
            <label class="ml-1 block font-label-sm text-on-surface-variant" for="reg-pw">密码（至少 6 位）</label>
            <input
              id="reg-pw"
              v-model="password"
              class="mt-1 w-full rounded-lg border border-outline-variant/40 bg-surface-container-low px-4 py-3 font-body-md focus:border-secondary focus:outline-none"
              type="password"
              autocomplete="new-password"
            />
          </div>
          <div>
            <label class="ml-1 block font-label-sm text-on-surface-variant" for="reg-pw2">确认密码</label>
            <input
              id="reg-pw2"
              v-model="confirmPassword"
              class="mt-1 w-full rounded-lg border border-outline-variant/40 bg-surface-container-low px-4 py-3 font-body-md focus:border-secondary focus:outline-none"
              type="password"
              autocomplete="new-password"
            />
          </div>
          <div class="flex gap-3">
            <input
              v-model="captcha"
              class="min-w-0 flex-1 rounded-lg border border-outline-variant/40 bg-surface-container-low px-4 py-3 font-body-md focus:border-secondary focus:outline-none"
              placeholder="验证码（不区分大小写）"
              maxlength="8"
            />
            <button
              type="button"
              class="relative h-[40px] w-[112px] shrink-0 overflow-hidden rounded-lg border border-outline-variant/50 bg-white"
              title="刷新"
              @click="refreshCaptcha"
            >
              <img alt="" class="h-full w-full object-contain" :src="captchaSrc"/>
              <span v-if="!captchaSrc" class="absolute inset-0 flex items-center justify-center text-xs text-on-surface-variant">点击刷新</span>
            </button>
          </div>
          <button
            type="submit"
            :disabled="loading"
            class="w-full rounded-lg bg-primary py-4 font-headline-md text-on-primary shadow-md transition hover:bg-primary-container disabled:opacity-60"
          >
            {{ loading ? '提交中…' : '注册并前往登录' }}
          </button>
        </form>
      </div>
    </main>
  </div>
</template>
