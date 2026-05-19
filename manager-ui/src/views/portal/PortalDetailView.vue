<template>
  <div class="bg-background text-on-surface font-body-md selection:bg-secondary-container selection:text-on-secondary-container">
    <header class="fixed top-0 left-0 w-full flex justify-between items-center px-16 py-6 h-20 bg-[#2C2420] text-white z-50 shadow-2xl shadow-stone-900/20">
      <div class="flex items-center gap-12">
        <span class="text-xl font-semibold tracking-[0.15em] text-white font-headline-md">馆藏书屋</span>
      </div>
      <div class="flex items-center gap-6">
        <RouterLink to="/portal/list"><span class="material-symbols-outlined">search</span></RouterLink>
        <RouterLink to="/portal/cart"><span class="material-symbols-outlined">shopping_cart</span></RouterLink>
        <RouterLink to="/portal/profile"><span class="material-symbols-outlined">person</span></RouterLink>
      </div>
    </header>

    <main class="max-w-[1280px] mx-auto px-margin-page pt-[160px] pb-section-gap">
      <nav class="mb-stack-lg flex items-center gap-2 text-on-surface-variant font-label-sm">
        <RouterLink class="hover:text-primary" to="/portal">首页</RouterLink>
        <span class="material-symbols-outlined text-[14px]">chevron_right</span>
        <RouterLink class="hover:text-primary" to="/portal/list">活动专题</RouterLink>
        <span class="material-symbols-outlined text-[14px]">chevron_right</span>
        <span class="text-primary">人文典藏周</span>
      </nav>

      <section class="grid grid-cols-1 md:grid-cols-12 gap-16 items-start">
        <div class="md:col-span-5 lg:col-span-6 sticky top-32">
          <div class="aspect-[3/4] bg-surface-container-low shadow-stone-900/10 shadow-2xl relative overflow-hidden">
            <img class="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuA7rk5Yng7YH4ZPh5tqmNCthUr62nNuebHrcODxDkTJTNa8nSK7B1mchFOOZgPf0FwQZ-qwKRtlWcoir2Jue5b1KkKL5mVrHEznNJd_x7PcEGow7ppo4KqUCCaoih70Bc4OkcB4pXVfhbSJFhVAAARQceuTo__AdhQWn2uFF-q0rhsU6sstLXos59UtelDYCDIV3j5CJHFueEFN5xZCgfcWYKpUGLHXeCoZ6ObF5pPx1WEhHcNH8Lcq1mF48cZ0OgeARHTL8ZyoDQ" alt="人文典藏周活动主视觉" />
          </div>
        </div>
        <div class="md:col-span-7 lg:col-span-6 flex flex-col gap-stack-lg">
          <h1 class="font-display-xl text-primary leading-tight">人文典藏周：收藏思想的火花</h1>
          <div class="flex flex-wrap gap-x-6 gap-y-2 text-on-surface-variant font-body-md border-b border-outline-variant pb-stack-md">
            <span>活动时间：2026.05.10 - 2026.05.24</span>
            <span>活动范围：历史 / 文学 / 艺术</span>
            <span>适用人群：全体注册会员</span>
          </div>
          <div class="flex items-baseline gap-4 bg-surface-container-low p-stack-md rounded-lg">
            <span class="text-secondary font-headline-lg">典藏专区低至 7.5 折</span>
            <span class="text-on-surface-variant text-body-md">满 199 减 30 / 满 299 减 60</span>
            <span class="ml-auto bg-secondary-container text-on-secondary-container font-label-sm px-3 py-1 rounded-full">限时福利</span>
          </div>
          <div class="flex flex-col gap-stack-md mt-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="rounded-lg border border-outline-variant p-4">
                <p class="text-xs text-on-surface-variant uppercase mb-1">新会员专享</p>
                <p class="font-headline-md text-primary">首单再减 20 元</p>
              </div>
              <div class="rounded-lg border border-outline-variant p-4">
                <p class="text-xs text-on-surface-variant uppercase mb-1">会员积分</p>
                <p class="font-headline-md text-primary">活动期双倍累计</p>
              </div>
            </div>
          </div>
          <div class="flex gap-4 items-stretch h-14">
            <RouterLink to="/portal/list" class="flex-[2] inline-flex items-center justify-center bg-primary text-white font-headline-md hover:bg-primary-container transition-all duration-300">立即参与活动</RouterLink>
            <RouterLink to="/portal" class="flex-[1] inline-flex items-center justify-center border-2 border-secondary text-secondary font-headline-md hover:bg-secondary-container transition-all duration-300">返回首页</RouterLink>
          </div>
          <p class="text-on-surface-variant leading-relaxed italic">“每一本经典，都是与伟大思想的一次久别重逢。”</p>
        </div>
      </section>

      <section class="mt-section-gap">
        <div class="flex border-b border-outline-variant mb-stack-lg">
          <button class="px-8 py-4 font-headline-md text-primary border-b-2 border-secondary">活动说明</button>
          <button class="px-8 py-4 font-headline-md text-on-surface-variant hover:text-primary transition-all">参与方式</button>
          <button class="px-8 py-4 font-headline-md text-on-surface-variant hover:text-primary transition-all flex items-center gap-2">优惠规则 <span class="font-label-sm bg-surface-container-high px-2 rounded-full text-[10px]">重要</span></button>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-12 gap-12">
          <div class="md:col-span-8 flex flex-col gap-stack-lg">
            <div class="max-w-none text-on-surface leading-loose font-body-lg">
              <h3 class="font-headline-lg mb-4">活动亮点</h3>
              <p class="mb-6">“人文典藏周”聚焦历史、文学、艺术三大主题馆，精选高口碑经典与装帧典藏版本。活动期间，用户可通过专题页快速浏览编辑推荐书单，并根据阅读偏好组合下单，享受阶梯满减与会员折扣叠加优惠。</p>
              <p class="mb-6">本次活动特别推出“思想入门”“经典重读”“艺术之眼”三条推荐路线，无论你是首次接触人文社科，还是希望系统补充阅读框架，都能找到合适的典藏组合。</p>
            </div>
            <div class="grid grid-cols-2 gap-8 mt-4">
              <div class="bg-surface-container p-stack-lg rounded-xl">
                <span class="font-label-sm uppercase text-secondary block mb-2">参与步骤</span>
                <p class="font-headline-md">选书 - 加购 - 领券 - 结算</p>
              </div>
              <div class="bg-surface-container p-stack-lg rounded-xl">
                <span class="font-label-sm uppercase text-secondary block mb-2">配送服务</span>
                <p class="font-headline-md">满 99 元包邮（偏远地区除外）</p>
              </div>
            </div>
          </div>
          <div class="md:col-span-4">
            <div class="p-stack-lg bg-surface-container-low border border-outline-variant">
              <h4 class="font-headline-md mb-4 text-center">活动节奏</h4>
              <div class="flex flex-col items-center gap-2">
                <span class="text-display-xl font-bold text-primary">15天</span>
                <div class="w-full space-y-3 text-sm text-on-surface-variant">
                  <p>05.10 - 05.13 预热期：专题书单发布</p>
                  <p>05.14 - 05.21 爆发期：折扣与满减同步开启</p>
                  <p>05.22 - 05.24 收官期：限量典藏补货</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="mt-stack-lg pt-stack-lg border-t border-outline-variant">
        <h3 class="font-headline-lg mb-8">规则说明</h3>
        <div class="space-y-6 text-on-surface-variant leading-relaxed">
          <p>1. 活动优惠可与会员折扣叠加，不可与其他站内专项券（如新人首单券）同时使用，系统将自动选择最优方案。</p>
          <p>2. 满减门槛按同一订单实付商品金额计算，运费与增值服务不计入门槛。</p>
          <p>3. 限量典藏书标注“限量”字样，售完即止；如遇缺货将按下单顺序处理并通知。</p>
          <p>4. 活动最终解释权归馆藏书屋平台所有。如有疑问可在首页底部“帮助中心”联系在线客服。</p>
        </div>
      </section>
    </main>

    <footer class="w-full py-24 px-16 grid grid-cols-1 md:grid-cols-2 gap-8 items-end mt-[120px] bg-stone-50 border-t border-stone-200">
      <div>
        <span class="font-serif text-lg italic text-[#2C2420] block mb-4">馆藏书屋</span>
        <p class="font-manrope text-xs tracking-wider font-light leading-loose text-stone-500 max-w-sm">© 2024 馆藏书屋。为敏锐的读者精心打造。</p>
      </div>
      <div class="flex flex-col gap-3 justify-start md:justify-end md:items-end">
        <p class="font-manrope text-xs tracking-wider font-light leading-loose text-stone-500 max-w-[560px] md:text-right">
          隐私政策：我们仅在提供下单、支付、物流与售后服务所必需的范围内收集信息，并采用加密与权限控制保护您的数据安全。未经您的授权，不会将个人信息用于与阅读服务无关的用途。
        </p>
        <div class="font-manrope text-xs tracking-wider font-light leading-loose text-stone-500 md:text-right">
          帮助中心：联系电话 8055-0166608 ｜ 邮箱地址 suyongfei650@gmail.com
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

