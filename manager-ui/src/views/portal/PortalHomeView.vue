<template>
  <div class="bg-surface-bright text-on-surface font-body-md">
    <p
      v-if="portalApiError"
      class="fixed left-0 right-0 top-20 z-50 mx-auto max-w-3xl rounded-lg bg-error-container px-4 py-2 text-center text-sm shadow"
      role="alert"
    >
      {{ portalApiError }}
      <span class="mt-1 block text-xs opacity-80">HTTP 502 = 8080 后端未启动。IDEA 运行「启动后端 SpringBoot」或 Tomcat，再访问 http://127.0.0.1:5173</span>
    </p>
    <main class="pt-20">
      <section
        class="portal-hero relative -mt-20 flex h-screen w-full flex-col justify-end overflow-hidden bg-primary pt-20"
      >
        <div
          v-for="(slide, index) in heroSlides"
          :key="slide.tag"
          class="absolute inset-0 transition-opacity duration-700 ease-in-out"
          :class="index === currentHeroIndex ? 'opacity-100 z-[1]' : 'opacity-0 z-0 pointer-events-none'"
          :aria-hidden="index !== currentHeroIndex"
        >
          <div class="absolute inset-0 bg-gradient-to-br" :class="slide.bgClass" />
          <img
            v-if="!heroCoverFailed[index]"
            class="portal-hero-img absolute inset-0 h-full w-full object-cover object-[center_35%] opacity-80"
            :src="heroImageSrc(slide.cover)"
            :alt="slide.tag"
            loading="eager"
            @error="onHeroCoverError(index)"
          />
          <div class="absolute inset-0 bg-gradient-to-t from-primary/95 via-primary/45 to-primary/10" />
          <div class="absolute inset-0 bg-gradient-to-r from-primary/75 via-primary/25 to-transparent" />
        </div>
        <button
          type="button"
          class="absolute left-6 top-1/2 z-20 -translate-y-1/2 rounded-full bg-black/35 p-2 text-white transition hover:bg-black/55"
          aria-label="上一张轮播"
          @click="prevHero"
        >
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <button
          type="button"
          class="absolute right-6 top-1/2 z-20 -translate-y-1/2 rounded-full bg-black/35 p-2 text-white transition hover:bg-black/55"
          aria-label="下一张轮播"
          @click="nextHero"
        >
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
        <div class="relative z-10 mt-auto w-full px-6 pb-14 pt-16 text-left sm:px-12 md:px-16 md:pb-20">
          <div class="max-w-2xl">
            <span class="mb-3 block font-label-sm uppercase tracking-[0.2em] text-secondary">{{ currentHero.tag }}</span>
            <h2 class="mb-4 max-w-xl font-headline-lg text-xl leading-snug text-white sm:text-2xl md:text-[1.75rem]">
              {{ currentHero.title }}
            </h2>
            <p class="mb-8 max-w-lg font-body-lg leading-relaxed text-stone-300">{{ currentHero.desc }}</p>
            <div class="flex flex-wrap gap-4">
              <button
                type="button"
                class="bg-secondary px-7 py-3.5 font-headline-md text-sm tracking-widest text-white hover:opacity-90"
                @click="onHeroPrimary"
              >
                {{ currentHero.primaryText }}
              </button>
              <a
                class="border border-white px-7 py-3.5 font-headline-md text-sm tracking-widest text-white hover:bg-white hover:text-primary"
                href="#"
                @click.prevent="onSecondaryAction(currentHero.secondaryLink)"
              >
                {{ currentHero.secondaryText }}
              </a>
            </div>
          </div>
        </div>
        <div class="absolute bottom-8 right-6 z-20 flex gap-3 sm:right-16 md:bottom-10">
          <button
            v-for="(item, index) in heroSlides"
            :key="item.tag"
            type="button"
            class="h-[2px] transition-all duration-300"
            :class="index === currentHeroIndex ? 'w-12 bg-white' : 'w-12 bg-white/30 hover:bg-white/70'"
            :aria-label="`切换到第${index + 1}张轮播`"
            @click="goToHero(index)"
          ></button>
        </div>
      </section>

      <section id="new-arrivals" class="max-w-container-max mx-auto scroll-mt-20 px-16 py-[120px]">
        <div class="flex justify-between items-end mb-16">
          <div>
            <span class="font-label-sm text-secondary uppercase tracking-widest block mb-2">刚刚入藏</span>
            <h3 class="font-headline-lg text-primary">新书上架</h3>
          </div>
          <button type="button" class="font-label-sm text-outline hover:text-primary" @click="goCatalog()">查看全部新品 →</button>
        </div>

        <div v-if="newArrivalsLoading" class="flex h-[700px] items-center justify-center text-outline">
          正在加载新书…
        </div>
        <div v-else-if="featuredNewBook" class="grid grid-cols-12 gap-8 h-[700px]">
          <RouterLink
            :to="{ name: 'portal-book', params: { id: String(featuredNewBook.id) } }"
            class="col-span-7 h-full relative group overflow-hidden bg-surface-container-low"
            @click="onOpenBookFromHome"
          >
            <img
              class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
              :src="resolveBookCover(featuredNewBook.coverUrl)"
              :alt="featuredNewBook.title"
              @error="onBookCoverError"
            />
            <div class="absolute bottom-0 left-0 w-full bg-gradient-to-t from-primary/80 to-transparent p-10">
              <span class="text-secondary text-xs font-bold tracking-widest uppercase">重磅推荐</span>
              <h4 class="text-white text-2xl font-headline-md mt-2 line-clamp-2">{{ featuredNewBook.title }}</h4>
              <p class="text-stone-300 mt-2 font-body-md">
                {{ featuredNewBook.author || '未知作者' }} 著 · ¥{{ formatPrice(featuredNewBook.price) }}
              </p>
            </div>
          </RouterLink>
          <div class="col-span-5 grid grid-rows-2 gap-8 h-full">
            <RouterLink
              v-for="b in sideNewBooks"
              :key="b.id"
              :to="{ name: 'portal-book', params: { id: String(b.id) } }"
              class="bg-surface-container flex p-6 gap-6 hover:bg-surface-container-high transition-colors"
              @click="onOpenBookFromHome"
            >
              <img
                class="w-24 h-36 shrink-0 object-cover shadow-lg"
                :src="resolveBookCover(b.coverUrl)"
                :alt="b.title"
                @error="onBookCoverError"
              />
              <div class="flex min-w-0 flex-col justify-center">
                <span class="text-secondary font-label-sm">{{ categoryNameOf(b.categoryId) }}</span>
                <h5 class="text-primary font-headline-md text-lg mt-1 line-clamp-2">{{ b.title }}</h5>
                <p class="text-outline text-sm mt-1">¥ {{ formatPrice(b.price) }}</p>
              </div>
            </RouterLink>
          </div>
        </div>
        <div v-else class="flex h-[400px] items-center justify-center text-outline">暂无在架新书</div>
      </section>

      <section id="hot-ranking" class="scroll-mt-20 bg-surface-container py-[120px]">
        <div class="max-w-container-max mx-auto px-16">
          <div class="text-center mb-16">
            <span class="font-label-sm text-secondary tracking-[0.3em] uppercase">馆藏流行趋势</span>
            <h3 class="font-headline-lg text-primary mt-2">热销榜单</h3>
          </div>
          <div v-if="hotRankLoading" class="py-16 text-center text-outline">正在加载热销榜单…</div>
          <div v-else-if="hotRankBooks.length" class="grid grid-cols-1 gap-12 sm:grid-cols-2 md:grid-cols-5">
            <RouterLink
              v-for="(book, index) in hotRankBooks"
              :key="book.id"
              :to="{ name: 'portal-book', params: { id: String(book.id) } }"
              class="group flex flex-col items-center"
              @click="onOpenBookFromHome"
            >
              <div class="relative mb-6">
                <span class="absolute -left-6 top-0 z-0 font-display-xl italic leading-none text-surface-container-highest">
                  {{ rankLabel(index) }}
                </span>
                <img
                  class="relative z-10 h-64 w-48 object-cover shadow-2xl transition-transform duration-300 group-hover:-translate-y-2"
                  :src="resolveBookCover(book.coverUrl)"
                  :alt="book.title"
                  @error="onBookCoverError"
                />
              </div>
              <h4 class="line-clamp-2 px-2 text-center font-headline-md text-base text-primary">{{ book.title }}</h4>
              <p class="mt-1 text-xs text-outline">{{ book.author || '未知作者' }}</p>
              <p class="mt-1 text-sm text-secondary">¥ {{ formatPrice(book.price) }}</p>
            </RouterLink>
          </div>
          <div v-else class="py-16 text-center text-outline">暂无热销图书数据</div>
        </div>
      </section>

      <section class="max-w-container-max mx-auto px-16 py-[120px]">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-12">
          <button
            v-for="cat in homeFeatureCategories"
            :key="cat.title"
            type="button"
            class="relative group flex h-[500px] w-full cursor-pointer flex-col justify-end overflow-hidden p-8 text-left text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D4AF37]"
            @click="goCatalog(cat.categoryId)"
          >
            <img
              class="absolute inset-0 z-0 h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
              :src="categoryCoverSrc(cat.cover)"
              :alt="cat.title"
            />
            <div class="relative z-10">
              <span class="font-label-sm uppercase tracking-widest opacity-80">分类</span>
              <h4 class="mb-4 font-headline-lg">{{ cat.title }}</h4>
              <p class="mb-6 font-body-md opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                {{ cat.desc }}
              </p>
            </div>
            <div class="absolute inset-0 z-[5] bg-gradient-to-t from-black/80 via-black/20 to-transparent"></div>
          </button>
        </div>
      </section>

      <section class="max-w-container-max mx-auto px-16 py-[120px] bg-primary text-white text-center">
        <h3 class="font-headline-lg mb-6">订阅我们的馆藏期刊</h3>
        <p class="font-body-lg text-stone-400 mb-10 max-w-2xl mx-auto">每周为您递送精选书评、作者专访及限时珍藏版书籍发售预告。加入我们的阅读社群。</p>
        <form class="max-w-md mx-auto" @submit.prevent="onNewsletterSubmit">
          <div class="flex border-b border-stone-700 py-2" :class="{ 'border-red-400/60': newsletterError }">
            <input
              v-model="newsletterEmail"
              class="flex-grow border-none bg-transparent text-white placeholder-stone-600 focus:ring-0"
              placeholder="输入您的电子邮箱地址"
              type="email"
              autocomplete="email"
              :disabled="newsletterSubmitting"
            />
            <button
              class="shrink-0 px-4 font-label-sm uppercase tracking-widest text-secondary disabled:opacity-50"
              type="submit"
              :disabled="newsletterSubmitting"
            >
              {{ newsletterSubmitting ? '提交中…' : '立即订阅' }}
            </button>
          </div>
          <p v-if="newsletterError" class="mt-3 text-left text-sm text-red-300">{{ newsletterError }}</p>
          <p v-else-if="newsletterSuccess" class="mt-3 text-left text-sm text-secondary">{{ newsletterSuccess }}</p>
        </form>
      </section>
    </main>

    <footer class="w-full py-24 px-16 grid grid-cols-1 md:grid-cols-2 gap-8 items-end mt-[120px] bg-stone-50 border-t border-stone-200 text-[#2C2420]">
      <div class="space-y-6">
        <h2 class="font-serif text-lg italic text-[#2C2420]">馆藏书屋</h2>
        <p class="font-manrope text-xs tracking-wider font-light leading-loose max-w-sm">作为全球知名的数字化私人馆藏，我们致力于将纸质阅读的庄重感带入数字时代。</p>
      </div>
      <div class="flex flex-col items-end gap-4">
        <div class="flex flex-wrap items-center justify-end gap-3">
          <button
            type="button"
            class="group inline-flex items-center gap-2 rounded-full border border-stone-300/80 bg-white px-4 py-2 text-[11px] tracking-wider text-stone-600 shadow-sm transition-all hover:-translate-y-0.5 hover:border-secondary hover:text-secondary hover:shadow-md"
            @click="showHelpDialog = true"
          >
            <span class="material-symbols-outlined text-base transition-colors group-hover:text-secondary">support_agent</span>
            帮助中心
          </button>
          <button
            type="button"
            class="group inline-flex items-center gap-2 rounded-full border border-stone-300/80 bg-white px-4 py-2 text-[11px] tracking-wider text-stone-600 shadow-sm transition-all hover:-translate-y-0.5 hover:border-secondary hover:text-secondary hover:shadow-md"
            @click="showNoticeDialog = true"
          >
            <span class="material-symbols-outlined text-base transition-colors group-hover:text-secondary">campaign</span>
            系统公告
          </button>
        </div>
        <p class="font-manrope text-xs tracking-wider font-light leading-loose text-stone-400 uppercase">© 2024 馆藏书屋. 为有品位的读者精心打造。</p>
      </div>
    </footer>

    <div
      v-if="showHelpDialog"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-black/40 px-4"
      @click.self="showHelpDialog = false"
    >
      <div class="w-full max-w-[520px] rounded-xl bg-surface-container-lowest p-6 shadow-2xl">
        <h3 class="mb-4 font-headline-md text-primary">帮助中心</h3>
        <p class="mb-2 text-sm text-on-surface">联系电话：8055-0166608</p>
        <p class="mb-2 text-sm text-on-surface">邮箱地址：suyongfei650@gmail.com</p>
        <p class="mb-6 text-xs text-on-surface-variant">如需帮助请将问题发往邮箱；或者拨打电话咨询。</p>
        <div class="flex justify-end">
          <button
            type="button"
            class="rounded-lg bg-primary px-4 py-2 text-sm text-on-primary hover:bg-primary-container transition"
            @click="showHelpDialog = false"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="showNoticeDialog"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-black/40 px-4"
      @click.self="showNoticeDialog = false"
    >
      <div class="w-full max-w-[560px] rounded-xl bg-surface-container-lowest p-6 shadow-2xl">
        <h3 class="mb-4 font-headline-md text-primary">系统公告</h3>
        <p class="whitespace-pre-line text-xs text-on-surface-variant leading-relaxed">{{ noticeText }}</p>
        <div class="mt-6 flex justify-end">
          <button
            type="button"
            class="rounded-lg bg-primary px-4 py-2 text-sm text-on-primary hover:bg-primary-container transition"
            @click="showNoticeDialog = false"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  formatPrice,
  portalBooksList,
  portalCategories,
  resolveBookCover,
  type PortalBook,
  type PortalCategory,
} from '../../api/portal/catalog'
import { isPortalHomeSectionId, scrollToPortalHomeSection } from '../../utils/portalHomeScroll'
import { portalNewsletterSubscribe } from '../../api/portal/newsletter'
import { markPortalBookNavigation, tryRestorePendingPortalScroll } from '../../utils/portalScrollRestore'

const route = useRoute()
const router = useRouter()

function onOpenBookFromHome() {
  markPortalBookNavigation('home')
}

async function onNewsletterSubmit() {
  newsletterError.value = ''
  newsletterSuccess.value = ''
  const email = newsletterEmail.value.trim()
  if (!email) {
    newsletterError.value = '请输入电子邮箱'
    return
  }
  newsletterSubmitting.value = true
  try {
    const res = await portalNewsletterSubscribe(email)
    if (res.success) {
      newsletterSuccess.value = res.data?.message || '订阅成功，感谢关注！'
      newsletterEmail.value = ''
      return
    }
    newsletterError.value = res.message || '订阅失败，请稍后重试'
  } catch {
    newsletterError.value = '网络异常，请稍后重试'
  } finally {
    newsletterSubmitting.value = false
  }
}
const heroCoverFailed = ref<Record<number, boolean>>({})
const newArrivalBooks = ref<PortalBook[]>([])
const categories = ref<PortalCategory[]>([])
const newArrivalsLoading = ref(true)
const hotRankBooks = ref<PortalBook[]>([])
const hotRankLoading = ref(true)
const portalApiError = ref('')

const categoryMap = computed(() => new Map(categories.value.map((c) => [c.id, c])))
const featuredNewBook = computed(() => newArrivalBooks.value[0] ?? null)
const sideNewBooks = computed(() => newArrivalBooks.value.slice(1, 3))

function categoryNameOf(id: number | null | undefined) {
  if (id == null) return '馆藏'
  return categoryMap.value.get(id)?.name || '馆藏'
}

function onBookCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function rankLabel(index: number) {
  return String(index + 1).padStart(2, '0')
}

function setPortalApiError(msg: string) {
  if (msg) portalApiError.value = msg
}

async function loadNewArrivals() {
  newArrivalsLoading.value = true
  try {
    const [catRes, bookRes] = await Promise.all([
      portalCategories(),
      portalBooksList({ page: 1, size: 3, sort: 'default' }),
    ])
    if (catRes.success && catRes.data) categories.value = catRes.data
    else if (!catRes.success) setPortalApiError(catRes.message || '分类加载失败')
    if (bookRes.success && bookRes.data?.list) newArrivalBooks.value = bookRes.data.list
    else if (!bookRes.success) setPortalApiError(bookRes.message || '图书列表加载失败')
  } finally {
    newArrivalsLoading.value = false
  }
}

async function loadHotRanking() {
  hotRankLoading.value = true
  try {
    const res = await portalBooksList({ page: 1, size: 5, sort: 'price_desc' })
    if (res.success && res.data?.list) hotRankBooks.value = res.data.list
    else if (!res.success) setPortalApiError(res.message || '热销榜加载失败')
  } finally {
    hotRankLoading.value = false
  }
}

const newsletterEmail = ref('')
const newsletterSubmitting = ref(false)
const newsletterSuccess = ref('')
const newsletterError = ref('')

const showHelpDialog = ref(false)
const showNoticeDialog = ref(false)
const currentHeroIndex = ref(0)
const noticeText = [
  '【系统公告】',
  '1. 用户端登录/注册页面已上线验证码与扫码登录入口。',
  '2. 为保障账号安全，请勿将验证码或密码透露给他人。',
  '3. 若遇到页面异常，可刷新后重试，或联系帮助中心。',
].join('\n')

/** 首页分类入口（categoryId 对应数据库 category 表） */
const homeFeatureCategories = [
  {
    title: '艺术与设计',
    categoryId: 2,
    cover: '/categories/art-design.png',
    desc: '探索形式、色彩与灵魂的交织，收录全球顶尖艺评、设计理论与视觉艺术典藏。',
  },
  {
    title: '经典文学',
    categoryId: 2,
    cover: '/categories/classic-literature.png',
    desc: '重温中外名著与当代文学经典，在叙事与诗性中体味人性、时代与思想的光芒。',
  },
  {
    title: '前沿科技',
    categoryId: 7,
    cover: '/categories/frontier-science.png',
    desc: '聚焦科学发现、技术革新与未来趋势，用通俗读本读懂正在改变世界的知识前沿。',
  },
]

const heroSlides = [
  {
    tag: '季度精选',
    title: '春季读书月：在墨香中感悟万物新生',
    desc: '精选文学、哲学与自然志，邀您共赴一场跨越时空的思想盛宴。新会员首单享珍藏版礼品。',
    cover: '/hero/hero-1.jpg',
    bgClass: 'from-[#3d2f24] via-[#2c2420] to-[#160f0c]',
    primaryText: '立即探索',
    primaryLink: '/portal/list',
    secondaryText: '查看活动详情',
    secondaryLink: '/portal/detail',
  },
  {
    tag: '限时专题',
    title: '人文典藏周：收藏思想的火花',
    desc: '汇聚历史、文学与艺术名家作品，限时推出典藏套装与会员专享折扣，打造你的私人书单。',
    cover: '/hero/hero-2.jpg',
    bgClass: 'from-[#4a3a2a] via-[#2c2420] to-[#1a1410]',
    primaryText: '挑选典藏',
    primaryLink: '/portal/list',
    secondaryText: '活动说明',
    secondaryLink: '/portal/detail',
  },
  {
    tag: '新会员福利',
    title: '加入阅读社群：解锁首购礼遇',
    desc: '注册即享新人礼、热销书单推荐与每周书评精选，和更多读者一起开启高质量阅读生活。',
    cover: '/hero/hero-3.jpg',
    bgClass: 'from-[#2a3440] via-[#2c2420] to-[#121820]',
    primaryText: '去注册',
    primaryLink: '/user/login',
    secondaryText: '热销榜单',
    secondaryLink: '#hot-ranking',
  },
]

function heroImageSrc(cover: string) {
  if (cover.startsWith('http://') || cover.startsWith('https://')) return cover
  if (cover.startsWith('/hero/')) {
    const base = (import.meta.env.BASE_URL || '/').replace(/\/$/, '')
    return `${base}${cover}`
  }
  return resolveBookCover(cover)
}

function categoryCoverSrc(cover: string) {
  if (cover.startsWith('http://') || cover.startsWith('https://')) return cover
  if (cover.startsWith('/categories/')) {
    const base = (import.meta.env.BASE_URL || '/').replace(/\/$/, '')
    return `${base}${cover}`
  }
  return cover
}

function onHeroCoverError(index: number) {
  heroCoverFailed.value[index] = true
}
const currentHero = computed(() => heroSlides[currentHeroIndex.value])

let heroTimer: ReturnType<typeof setInterval> | null = null

function goCatalog(categoryId?: number) {
  const query: Record<string, string> = {}
  if (categoryId != null) query.categoryId = String(categoryId)
  router.push({ path: '/portal/list', query })
}

function onHeroPrimary() {
  const link = currentHero.value.primaryLink
  if (link === '/portal/list') {
    goCatalog()
    return
  }
  if (link === '/user/login') {
    router.push('/user/login')
    return
  }
  router.push(link)
}

function goToHero(index: number) {
  currentHeroIndex.value = index
  startHeroAutoplay()
}

function switchHero() {
  currentHeroIndex.value = (currentHeroIndex.value + 1) % heroSlides.length
}

function prevHero() {
  currentHeroIndex.value = (currentHeroIndex.value - 1 + heroSlides.length) % heroSlides.length
  startHeroAutoplay()
}

function nextHero() {
  switchHero()
  startHeroAutoplay()
}

function onSecondaryAction(link: string) {
  if (link.startsWith('#')) {
    const id = link.slice(1)
    if (isPortalHomeSectionId(id)) {
      scrollToPortalHomeSection(id)
    }
    return
  }
  router.push(link)
}

function scrollToRouteHomeHash() {
  if (sessionStorage.getItem('portal:pending-restore')) return
  const id = route.hash.replace(/^#/, '')
  if (!isPortalHomeSectionId(id)) return
  requestAnimationFrame(() => scrollToPortalHomeSection(id))
}

watch(
  () => route.hash,
  () => {
    if (route.name !== 'portal-home') return
    scrollToRouteHomeHash()
  },
)

function startHeroAutoplay() {
  stopHeroAutoplay()
  heroTimer = setInterval(() => {
    switchHero()
  }, 5000)
}

function stopHeroAutoplay() {
  if (heroTimer) {
    clearInterval(heroTimer)
    heroTimer = null
  }
}

onMounted(async () => {
  startHeroAutoplay()
  await Promise.all([loadNewArrivals(), loadHotRanking()])
  await nextTick()
  tryRestorePendingPortalScroll('home')
  scrollToRouteHomeHash()
})

onUnmounted(() => {
  stopHeroAutoplay()
})
</script>

<style scoped>
.portal-hero-img {
  transform: scale(1.06);
}
</style>

