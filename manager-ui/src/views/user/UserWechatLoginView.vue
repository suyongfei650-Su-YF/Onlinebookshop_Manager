<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="paper-texture flex min-h-screen flex-col font-body-md text-on-surface">
    <header class="fixed top-0 z-50 flex h-16 w-full items-center justify-between border-b border-stone-800 bg-stone-900 px-12">
      <div class="flex items-center gap-4">
        <span class="font-manrope text-xl font-bold tracking-widest text-amber-50">馆藏书屋</span>
        <span class="text-[10px] tracking-widest text-stone-400">用户端</span>
      </div>
      <div class="flex items-center gap-4">
        <RouterLink to="/admin/login" class="text-xs tracking-widest text-stone-300 hover:text-white">管理员入口</RouterLink>
      </div>
    </header>
    <main class="ink-wash-gradient flex flex-grow items-center justify-center pt-16">
      <div class="w-full max-w-[480px] px-6 py-section-gap">
        <div class="group relative overflow-hidden border border-outline-variant bg-surface-container-lowest p-10">
          <div class="relative z-10 flex flex-col items-center text-center">
            <div class="mb-stack-lg">
              <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary-container">
                <span class="material-symbols-outlined text-3xl text-amber-200" style="font-variation-settings: 'FILL' 1"
                  >auto_stories</span
                >
              </div>
              <h1 class="mb-2 font-headline-lg text-primary">微信扫码登录</h1>
              <p class="mx-auto max-w-[320px] font-body-md text-on-surface-variant">请使用微信扫描下方二维码以安全登录馆藏书屋</p>
            </div>
            <div class="relative mb-stack-lg border border-outline-variant bg-white p-4">
              <div class="relative flex h-56 w-56 items-center justify-center overflow-hidden bg-surface-container">
                <img
                  class="h-full w-full object-contain mix-blend-multiply"
                  alt="二维码"
                  src="https://lh3.googleusercontent.com/aida-public/AB6AXuA7pvy5k-6lfCYvBUV5OSAX3AAIA95wsCWq5zzagmqtP2rX7raYz7BE5xW1VthGAeldveGmFhJNcambt6V_A30ZaaL3WvpQ9z0ZTEcbmUU06ThoR9RmttEifiOkWHk6PSAYsKb08JdfLHxHfEmUPqyS8RWae8aAsr4TpfMPtedOpovmNkzYoF88enx6WVTD2R2J2jpavDRZluVGiMQqFwAKXT5d3-HHwB9ZaICqv9cwU9msYqsY_6ZJXfKJNinScDPZHyWYRUuRBg"
                />
              </div>
            </div>
            <div class="w-full space-y-3">
              <button type="button" class="w-full bg-primary py-4 text-white font-headline-md hover:bg-stone-800">刷新二维码</button>
              <RouterLink
                to="/user/login"
                class="flex w-full items-center justify-center gap-2 border border-secondary py-4 font-headline-md text-secondary hover:bg-secondary-fixed"
              >
                <span class="material-symbols-outlined text-xl">keyboard_backspace</span>
                返回账号密码登录
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
