<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="paper-texture flex min-h-screen flex-col font-body-md text-on-surface">
    <header class="fixed top-0 z-50 flex h-16 w-full items-center justify-between border-b border-stone-800 bg-stone-900 px-12">
      <div class="flex items-center gap-4">
        <span class="font-manrope text-xl font-bold tracking-widest text-amber-50">馆藏书屋</span>
      </div>
      <div class="hidden gap-8 md:flex">
        <a class="font-manrope text-sm font-light tracking-tight text-stone-400 transition-colors hover:text-stone-200" href="#">典藏概览</a>
        <a class="font-manrope text-sm font-light tracking-tight text-stone-400 transition-colors hover:text-stone-200" href="#">图书编目</a>
        <a class="font-manrope text-sm font-light tracking-tight text-stone-400 transition-colors hover:text-stone-200" href="#">流通管理</a>
        <a class="font-manrope text-sm font-light tracking-tight text-stone-400 transition-colors hover:text-stone-200" href="#">读者数据</a>
      </div>
      <div class="flex items-center gap-6">
        <span class="material-symbols-outlined cursor-pointer p-2 text-amber-200 transition-all duration-300 hover:bg-stone-800/50">notifications</span>
        <span class="material-symbols-outlined cursor-pointer p-2 text-amber-200 transition-all duration-300 hover:bg-stone-800/50">settings</span>
      </div>
    </header>
    <main class="ink-wash-gradient flex flex-grow items-center justify-center pt-16">
      <div class="w-full max-w-[480px] px-6 py-section-gap">
        <div class="group relative overflow-hidden border border-outline-variant bg-surface-container-lowest p-10">
          <div
            class="absolute -right-12 -top-12 h-24 w-24 rounded-full bg-primary-container/5 transition-transform duration-700 group-hover:scale-150"
          ></div>
          <div class="relative z-10 flex flex-col items-center text-center">
            <div class="mb-stack-lg">
              <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary-container">
                <span class="material-symbols-outlined text-3xl text-amber-200" style="font-variation-settings: 'FILL' 1">auto_stories</span>
              </div>
              <h1 class="mb-2 font-headline-lg text-primary">微信扫码登录</h1>
              <p class="mx-auto max-w-[320px] font-body-md text-on-surface-variant">请使用微信扫描下方二维码以安全登录馆藏书屋管理后台</p>
            </div>
            <div class="relative mb-stack-lg border border-outline-variant bg-white p-4 qr-frame group/qr">
              <div class="relative flex h-56 w-56 items-center justify-center overflow-hidden bg-surface-container">
                <div class="pointer-events-none absolute inset-0 opacity-10">
                  <img
                    class="h-full w-full object-cover"
                    alt=""
                    src="https://lh3.googleusercontent.com/aida-public/AB6AXuCFyA453i0Og0OmFERBLMFcJTe6bkPs5znwiV0e-YHKkKgcRv8qIIiNkmgLtWt8cmq1K8BbStdoVau9F_hPM41KJKLXvNcbq21SgEHgTrvAy70MKicGszi-LikXVmk1FQGv4JD21suirnjgTw4cwXAkdyFTJYfclB9j5cocHoM3fG-Z9GBYcrybb5o759U91DX_gIZ_Hh77yql1z7KlgsFth9Z80k9j3rYZD8eOvtwLezh13gishSc5f5jJ3tCPCp5IYmZRpjbIZw"
                  />
                </div>
                <div class="relative z-10 h-full w-full p-4">
                  <img
                    class="h-full w-full object-contain mix-blend-multiply"
                    alt="二维码"
                    src="https://lh3.googleusercontent.com/aida-public/AB6AXuA7pvy5k-6lfCYvBUV5OSAX3AAIA95wsCWq5zzagmqtP2rX7raYz7BE5xW1VthGAeldveGmFhJNcambt6V_A30ZaaL3WvpQ9z0ZTEcbmUU06ThoR9RmttEifiOkWHk6PSAYsKb08JdfLHxHfEmUPqyS8RWae8aAsr4TpfMPtedOpovmNkzYoF88enx6WVTD2R2J2jpavDRZluVGiMQqFwAKXT5d3-HHwB9ZaICqv9cwU9msYqsY_6ZJXfKJNinScDPZHyWYRUuRBg"
                  />
                </div>
                <div
                  class="absolute inset-0 flex flex-col items-center justify-center bg-primary-container/90 opacity-0 transition-opacity duration-300 hover:opacity-100"
                >
                  <span class="material-symbols-outlined mb-2 text-5xl text-amber-200">check_circle</span>
                  <span class="font-label-sm text-amber-50">扫码成功</span>
                  <span class="mt-1 text-[10px] text-amber-50/60">正在跳转管理后台...</span>
                </div>
              </div>
            </div>
            <div class="w-full space-y-4">
              <div class="flex items-center justify-center gap-2 text-on-surface-variant">
                <span class="material-symbols-outlined text-lg">timer</span>
                <span class="font-label-sm uppercase tracking-wider">二维码将在 01:45 后失效</span>
              </div>
              <div class="flex flex-col gap-3">
                <button
                  type="button"
                  class="group/refresh flex w-full items-center justify-center gap-2 rounded-none bg-primary py-4 font-headline-md text-base text-white transition-all duration-300 hover:bg-stone-800"
                >
                  <span class="material-symbols-outlined text-xl transition-transform duration-500 group-hover:rotate-180">refresh</span>
                  刷新二维码
                </button>
                <RouterLink
                  to="/admin/login"
                  class="flex w-full items-center justify-center gap-2 rounded-none border border-secondary py-4 font-headline-md text-base text-secondary transition-all duration-300 hover:bg-secondary-fixed"
                >
                  <span class="material-symbols-outlined text-xl">keyboard_backspace</span>
                  返回账号密码登录
                </RouterLink>
              </div>
            </div>
          </div>
          <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-transparent via-secondary to-transparent opacity-20"></div>
        </div>
        <div class="mt-stack-lg flex justify-between px-2 font-label-sm uppercase tracking-widest text-on-surface-variant/60">
          <a class="transition-colors hover:text-secondary" href="#">登录遇到问题?</a>
          <a class="transition-colors hover:text-secondary" href="#">查看扫码教程</a>
        </div>
      </div>
    </main>
    <footer
      class="font-manrope mt-auto flex w-full items-center justify-between border-t border-stone-200 bg-stone-50 px-12 py-6 text-xs uppercase tracking-widest text-stone-400"
    >
      <div>© 2024 馆藏书屋 | 文人美学数字化管理系统</div>
      <div class="flex gap-8">
        <a class="transition-colors hover:text-amber-700" href="#">隐私政策</a>
        <a class="transition-colors hover:text-amber-700" href="#">服务条款</a>
        <a class="transition-colors hover:text-amber-700" href="#">联系技术支持</a>
      </div>
    </footer>
  </div>
</template>
