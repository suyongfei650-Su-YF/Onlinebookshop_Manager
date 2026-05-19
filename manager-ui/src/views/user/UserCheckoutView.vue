<template>
  <div class="min-h-screen bg-surface-bright font-body-md text-on-surface">
    <TopNav />
    <main class="mx-auto max-w-[1280px] px-margin-page pb-24 pt-24">
      <header class="mb-10 flex flex-wrap items-center justify-between gap-4 border-b border-outline-variant pb-6">
        <div>
          <RouterLink
            to="/portal/cart"
            class="mb-2 inline-flex items-center gap-1 text-sm text-outline hover:text-secondary"
          >
            <span class="material-symbols-outlined text-base">arrow_back</span>
            返回购物车
          </RouterLink>
          <h1 class="font-display-xl text-display-xl text-primary">订单结算</h1>
          <p class="mt-2 text-on-surface-variant">请确认收货信息与商品清单后提交订单</p>
        </div>
        <div class="flex items-center gap-2 text-xs uppercase tracking-widest text-outline">
          <span class="material-symbols-outlined text-secondary text-lg">verified_user</span>
          安全支付保障
        </div>
      </header>

      <div v-if="loading" class="py-24 text-center text-outline">正在加载结算信息…</div>

      <div v-else-if="loadError" class="py-24 text-center">
        <p class="mb-4 text-error">{{ loadError }}</p>
        <button type="button" class="border border-primary px-6 py-2 text-primary" @click="loadCheckout">重试</button>
      </div>

      <div v-else-if="checkoutItems.length === 0" class="py-24 text-center">
        <p class="text-on-surface-variant">没有可结算的商品</p>
        <RouterLink to="/portal/cart" class="mt-6 inline-block border border-secondary px-6 py-2.5 text-secondary">
          返回购物车
        </RouterLink>
      </div>

      <div v-else class="checkout-grid">
        <div class="space-y-10">
          <section class="border border-outline-variant bg-surface-container-lowest p-6">
            <h2 class="mb-6 font-headline-lg text-primary">收货信息</h2>
            <div class="grid gap-4 sm:grid-cols-2">
              <label class="block sm:col-span-1">
                <span class="mb-1 block text-xs uppercase tracking-wider text-outline">收货人</span>
                <input
                  v-model="form.receiverName"
                  type="text"
                  maxlength="64"
                  class="w-full border border-outline-variant bg-surface-bright px-3 py-2.5 outline-none focus:border-secondary"
                  placeholder="请输入收货人姓名"
                />
              </label>
              <label class="block sm:col-span-1">
                <span class="mb-1 block text-xs uppercase tracking-wider text-outline">联系电话</span>
                <input
                  v-model="form.receiverPhone"
                  type="tel"
                  maxlength="32"
                  class="w-full border border-outline-variant bg-surface-bright px-3 py-2.5 outline-none focus:border-secondary"
                  placeholder="11 位手机号"
                />
              </label>
              <label class="block sm:col-span-2">
                <span class="mb-1 block text-xs uppercase tracking-wider text-outline">详细地址</span>
                <textarea
                  v-model="form.shippingAddress"
                  rows="3"
                  maxlength="512"
                  class="w-full resize-none border border-outline-variant bg-surface-bright px-3 py-2.5 outline-none focus:border-secondary"
                  placeholder="省市区、街道、门牌号等"
                />
              </label>
            </div>
          </section>

          <section>
            <h2 class="mb-6 font-headline-lg text-primary">确认商品清单</h2>
            <div class="divide-y divide-outline-variant border border-outline-variant">
              <article
                v-for="item in checkoutItems"
                :key="item.cartItemId"
                class="flex gap-6 bg-surface-container-lowest p-5"
              >
                <RouterLink :to="bookDetailPath(item.bookId)" class="h-32 w-24 flex-shrink-0 overflow-hidden bg-surface-container-low">
                  <img
                    class="h-full w-full object-cover"
                    :src="resolveBookCover(item.coverUrl)"
                    :alt="item.title"
                    @error="onCoverError"
                  />
                </RouterLink>
                <div class="flex min-w-0 flex-1 flex-col justify-between">
                  <div>
                    <h3 class="line-clamp-2 font-headline-md text-primary">{{ item.title }}</h3>
                    <p class="mt-1 text-sm text-outline">{{ item.author || '未知作者' }}</p>
                  </div>
                  <div class="flex items-end justify-between gap-4">
                    <span class="text-sm text-outline">数量：{{ item.quantity }}</span>
                    <span class="font-semibold text-primary">¥ {{ formatPrice(lineTotal(item)) }}</span>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <section>
            <h2 class="mb-6 font-headline-lg text-primary">支付方式</h2>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <label
                v-for="opt in paymentOptions"
                :key="opt.value"
                class="relative cursor-pointer"
              >
                <input v-model="form.paymentMethod" class="peer sr-only" type="radio" :value="opt.value" />
                <div
                  class="flex flex-col items-center gap-2 border border-outline-variant p-5 transition-colors peer-checked:border-secondary peer-checked:bg-secondary-container/20"
                >
                  <span class="material-symbols-outlined text-3xl" :class="opt.iconClass">{{ opt.icon }}</span>
                  <span class="text-xs uppercase tracking-wider">{{ opt.label }}</span>
                </div>
              </label>
            </div>
          </section>
        </div>

        <aside class="relative">
          <div class="sticky top-24 border border-outline-variant bg-surface-container-low p-8">
            <h2 class="mb-6 border-b border-outline-variant pb-3 font-headline-md text-primary">费用摘要</h2>
            <div class="mb-6 space-y-3 text-on-surface-variant">
              <div class="flex justify-between">
                <span>商品总额</span>
                <span>¥ {{ formatPrice(subtotal) }}</span>
              </div>
              <div class="flex justify-between">
                <span>运费</span>
                <span>¥ 0.00</span>
              </div>
            </div>
            <div class="mb-8 border-t border-outline-variant pt-4">
              <div class="flex items-baseline justify-between">
                <span class="font-headline-md text-primary">应付金额</span>
                <span class="text-2xl font-bold text-error">¥ {{ formatPrice(subtotal) }}</span>
              </div>
            </div>
            <button
              type="button"
              class="w-full bg-primary py-4 text-lg tracking-wider text-white transition-colors hover:bg-primary/90 disabled:opacity-50"
              :disabled="submitting"
              @click="submitOrder"
            >
              {{ submitting ? '提交中…' : '确认并提交订单' }}
            </button>
            <p class="mt-4 text-center text-[10px] leading-relaxed text-outline">
              提交后将模拟支付成功，订单进入待发货状态；可在「我的订单」查看。
            </p>
          </div>
        </aside>
      </div>

      <p v-if="toastMsg" class="fixed bottom-8 right-8 z-50 rounded bg-[#2C2420] px-4 py-2 text-sm text-white shadow-lg">
        {{ toastMsg }}
      </p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { formatPrice, resolveBookCover } from '../../api/portal/catalog'
import { userCartList, type CartBook } from '../../api/user/cart'
import { userMe } from '../../api/user/auth'
import { userOrderCreate } from '../../api/user/orders'
import TopNav from './components/UserTopNav.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const toastMsg = ref('')
const allCartItems = ref<CartBook[]>([])

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  paymentMethod: 'WECHAT' as 'WECHAT' | 'ALIPAY' | 'CARD',
})

const paymentOptions = [
  { value: 'WECHAT' as const, label: '微信支付', icon: 'payments', iconClass: 'text-green-600' },
  { value: 'ALIPAY' as const, label: '支付宝', icon: 'account_balance_wallet', iconClass: 'text-blue-500' },
  { value: 'CARD' as const, label: '银行卡', icon: 'credit_card', iconClass: 'text-primary' },
]

const selectedBookIds = computed(() => {
  const raw = route.query.ids
  const s = Array.isArray(raw) ? raw.join(',') : raw || ''
  if (!s.trim()) return [] as number[]
  return s
    .split(',')
    .map((x) => Number(x.trim()))
    .filter((id) => Number.isFinite(id) && id > 0)
})

function canCheckout(item: CartBook) {
  return item.status === 'ON_SHELF' && item.stock > 0 && item.quantity <= item.stock
}

const checkoutItems = computed(() => {
  const list = allCartItems.value.filter(canCheckout)
  if (selectedBookIds.value.length === 0) return list
  const set = new Set(selectedBookIds.value)
  return list.filter((i) => set.has(i.bookId))
})

const subtotal = computed(() =>
  checkoutItems.value.reduce((sum, i) => sum + Number(i.price) * i.quantity, 0),
)

function bookDetailPath(id: number) {
  return { name: 'portal-book', params: { id: String(id) } }
}

function lineTotal(item: CartBook) {
  return Number(item.price) * item.quantity
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = resolveBookCover(null)
}

function showToast(msg: string) {
  toastMsg.value = msg
  setTimeout(() => {
    toastMsg.value = ''
  }, 2500)
}

async function loadCheckout() {
  loading.value = true
  loadError.value = ''
  try {
    const meRes = await userMe()
    if (!(meRes.success && meRes.data)) {
      loadError.value = '请先登录后再结算'
      router.push({ path: '/user/login', query: { redirect: route.fullPath } })
      return
    }
    const profile = meRes.data
    if (!form.receiverName) form.receiverName = profile.nickname || ''
    if (!form.receiverPhone) form.receiverPhone = profile.phone || ''

    const cartRes = await userCartList()
    if (cartRes.success && cartRes.data) {
      allCartItems.value = cartRes.data
    } else {
      loadError.value = cartRes.message || '加载购物车失败'
      allCartItems.value = []
    }
  } catch {
    loadError.value = '无法连接服务器'
    allCartItems.value = []
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (submitting.value || checkoutItems.value.length === 0) return

  if (!form.receiverName.trim() || !form.receiverPhone.trim() || !form.shippingAddress.trim()) {
    showToast('请填写完整收货信息')
    return
  }
  if (!/^1\d{10}$/.test(form.receiverPhone.trim())) {
    showToast('请输入正确的 11 位手机号')
    return
  }

  submitting.value = true
  try {
    const res = await userOrderCreate({
      bookIds: checkoutItems.value.map((i) => i.bookId),
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone.trim(),
      shippingAddress: form.shippingAddress.trim(),
      paymentMethod: form.paymentMethod,
      payNow: true,
    })
    if (res.success && res.data) {
      showToast('订单提交成功')
      await router.push({ path: '/portal/orders', query: { placed: String(res.data.id) } })
    } else {
      showToast(res.message || '提交订单失败')
    }
  } catch {
    showToast('网络异常，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCheckout)
</script>

<style scoped>
.checkout-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 48px;
}
@media (max-width: 1024px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
}
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
}
</style>
