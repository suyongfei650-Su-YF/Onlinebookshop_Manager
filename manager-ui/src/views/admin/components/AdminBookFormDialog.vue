<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4"
    @click.self="close"
  >
    <div
      class="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl border border-outline-variant bg-surface-container-lowest shadow-2xl"
      role="dialog"
      aria-modal="true"
    >
      <header class="sticky top-0 z-10 flex items-center justify-between border-b border-outline-variant bg-surface-container-lowest px-6 py-4">
        <h3 class="font-headline-md text-lg text-primary">{{ editing ? '编辑图书' : '新增图书' }}</h3>
        <button type="button" class="rounded-full p-2 text-outline hover:bg-surface-container-high" @click="close">
          <span class="material-symbols-outlined">close</span>
        </button>
      </header>

      <form class="space-y-5 p-6" @submit.prevent="submit">
        <p v-if="formError" class="rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ formError }}</p>

        <div class="grid gap-5 sm:grid-cols-2">
          <label class="block sm:col-span-2">
            <span class="mb-1 block text-xs text-on-surface-variant">书名 <span class="text-error">*</span></span>
            <input
              v-model="form.title"
              type="text"
              maxlength="256"
              required
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="请输入书名"
            />
          </label>

          <label class="block">
            <span class="mb-1 block text-xs text-on-surface-variant">作者</span>
            <input
              v-model="form.author"
              type="text"
              maxlength="128"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="作者姓名"
            />
          </label>

          <label class="block">
            <span class="mb-1 block text-xs text-on-surface-variant">ISBN</span>
            <input
              v-model="form.isbn"
              type="text"
              maxlength="32"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="978-..."
            />
          </label>

          <label class="block sm:col-span-2">
            <span class="mb-1 block text-xs text-on-surface-variant">分类</span>
            <select
              v-model="categoryIdStr"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            >
              <option value="">未分类</option>
              <option v-for="opt in categoryOptions" :key="opt.value" :value="String(opt.value)">
                {{ opt.label }}
              </option>
            </select>
          </label>

          <label class="block">
            <span class="mb-1 block text-xs text-on-surface-variant">价格（元）<span class="text-error">*</span></span>
            <input
              v-model.number="form.price"
              type="number"
              min="0"
              step="0.01"
              required
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            />
          </label>

          <label class="block">
            <span class="mb-1 block text-xs text-on-surface-variant">库存 <span class="text-error">*</span></span>
            <input
              v-model.number="form.stock"
              type="number"
              min="0"
              step="1"
              required
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            />
          </label>

          <label class="block sm:col-span-2">
            <span class="mb-1 block text-xs text-on-surface-variant">上架状态</span>
            <select
              v-model="form.status"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            >
              <option value="ON_SHELF">上架（ON_SHELF）</option>
              <option value="OFF_SHELF">下架（OFF_SHELF）</option>
            </select>
          </label>

          <div class="block sm:col-span-2">
            <span class="mb-1 block text-xs text-on-surface-variant">封面图片</span>
            <div class="rounded-lg border border-dashed border-outline-variant bg-surface-bright p-4">
              <input
                :key="coverFileInputKey"
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                class="block w-full text-sm text-on-surface file:mr-3 file:rounded-md file:border-0 file:bg-surface-container-high file:px-3 file:py-2 file:text-xs file:text-on-surface hover:file:bg-surface-container-highest"
                :disabled="coverUploading || saving"
                @change="onCoverFileChange"
              />
              <p class="mt-2 text-[11px] text-outline">支持 JPG / PNG / GIF / WebP，单张不超过 5MB，选择后自动上传</p>
              <p v-if="coverUploading" class="mt-2 text-xs text-secondary">封面上传中…</p>
              <p v-else-if="form.coverUrl" class="mt-2 truncate text-xs text-on-surface-variant">{{ form.coverUrl }}</p>
              <button
                v-if="form.coverUrl && !coverUploading"
                type="button"
                class="mt-2 text-xs text-error hover:underline"
                :disabled="saving"
                @click="clearCover"
              >
                移除封面
              </button>
            </div>
          </div>

          <div v-if="coverPreview" class="sm:col-span-2 flex items-start gap-4">
            <img
              class="h-28 w-20 rounded border border-outline-variant object-cover"
              :src="coverPreview"
              alt="封面预览"
              @error="onCoverPreviewError"
            />
            <p class="text-xs text-on-surface-variant leading-relaxed">封面预览</p>
          </div>

          <label class="block sm:col-span-2">
            <span class="mb-1 block text-xs text-on-surface-variant">内容简介</span>
            <textarea
              v-model="form.description"
              rows="4"
              class="w-full resize-y rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="图书简介，将展示在用户端详情页"
            />
          </label>
        </div>

        <footer class="flex justify-end gap-3 border-t border-outline-variant pt-4">
          <button
            type="button"
            class="rounded-lg border border-outline-variant px-5 py-2 text-sm text-on-surface hover:bg-surface-container-low"
            :disabled="saving"
            @click="close"
          >
            取消
          </button>
          <button
            type="submit"
            class="rounded-lg bg-primary px-5 py-2 text-sm font-medium text-on-primary hover:opacity-90 disabled:opacity-50"
            :disabled="saving"
          >
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { AdminBook, AdminBookForm } from '../../../api/admin/books'
import { adminBookCoverUpload, resolveAdminBookCover } from '../../../api/admin/books'
import type { AdminCategory } from '../../../api/admin/categories'

const props = defineProps<{
  modelValue: boolean
  book: AdminBook | null
  categories: AdminCategory[]
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [AdminBookForm]
}>()

const formError = ref('')
const coverPreviewBroken = ref(false)
const coverUploading = ref(false)
const coverFileInputKey = ref(0)
const MAX_COVER_BYTES = 5 * 1024 * 1024

const emptyForm = (): AdminBookForm => ({
  categoryId: null,
  title: '',
  author: '',
  isbn: '',
  price: 0,
  stock: 0,
  status: 'ON_SHELF',
  coverUrl: '',
  description: '',
})

const form = reactive<AdminBookForm>(emptyForm())
const categoryIdStr = ref('')

const editing = computed(() => props.book != null)

const categoryOptions = computed(() => {
  const opts: { value: number; label: string }[] = []
  const parents = props.categories.filter((c) => !c.parentId)
  for (const p of parents) {
    opts.push({ value: p.id, label: p.name })
    for (const child of props.categories.filter((c) => c.parentId === p.id)) {
      opts.push({ value: child.id, label: `　└ ${child.name}` })
    }
  }
  for (const c of props.categories) {
    if (!opts.some((o) => o.value === c.id)) {
      opts.push({ value: c.id, label: c.name })
    }
  }
  return opts
})

const coverPreview = computed(() => {
  if (coverPreviewBroken.value || !form.coverUrl.trim()) return ''
  return resolveAdminBookCover(form.coverUrl)
})

watch(
  () => form.coverUrl,
  () => {
    coverPreviewBroken.value = false
  },
)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    formError.value = ''
    coverPreviewBroken.value = false
    coverUploading.value = false
    coverFileInputKey.value += 1
    if (props.book) {
      form.categoryId = props.book.categoryId
      form.title = props.book.title
      form.author = props.book.author || ''
      form.isbn = props.book.isbn || ''
      form.price = Number(props.book.price) || 0
      form.stock = Number(props.book.stock) || 0
      form.status = props.book.status === 'OFF_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
      form.coverUrl = props.book.coverUrl || ''
      form.description = props.book.description || ''
      categoryIdStr.value = props.book.categoryId != null ? String(props.book.categoryId) : ''
    } else {
      Object.assign(form, emptyForm())
      categoryIdStr.value = ''
    }
  },
)

watch(categoryIdStr, (v) => {
  form.categoryId = v === '' ? null : Number(v)
})

function onCoverPreviewError() {
  coverPreviewBroken.value = true
}

async function onCoverFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  formError.value = ''
  if (!file.type.startsWith('image/')) {
    formError.value = '请选择图片文件'
    coverFileInputKey.value += 1
    return
  }
  if (file.size > MAX_COVER_BYTES) {
    formError.value = '图片不能超过 5MB'
    coverFileInputKey.value += 1
    return
  }
  coverUploading.value = true
  try {
    const res = await adminBookCoverUpload(file)
    if (res.success && res.data?.coverUrl) {
      form.coverUrl = res.data.coverUrl
      coverPreviewBroken.value = false
    } else {
      formError.value = res.message || '封面上传失败'
    }
  } catch {
    formError.value = '封面上传失败，请确认已登录且后端已启动'
  } finally {
    coverUploading.value = false
    coverFileInputKey.value += 1
  }
}

function clearCover() {
  form.coverUrl = ''
  coverPreviewBroken.value = false
  coverFileInputKey.value += 1
}

function close() {
  emit('update:modelValue', false)
}

function submit() {
  formError.value = ''
  if (coverUploading.value) {
    formError.value = '封面上传中，请稍候'
    return
  }
  if (!form.title.trim()) {
    formError.value = '书名不能为空'
    return
  }
  if (form.price < 0 || Number.isNaN(form.price)) {
    formError.value = '请输入有效价格'
    return
  }
  if (form.stock < 0 || Number.isNaN(form.stock)) {
    formError.value = '请输入有效库存'
    return
  }
  emit('submit', {
    categoryId: form.categoryId,
    title: form.title.trim(),
    author: form.author.trim(),
    isbn: form.isbn.trim(),
    price: Number(form.price),
    stock: Math.floor(Number(form.stock)),
    status: form.status,
    coverUrl: form.coverUrl.trim(),
    description: form.description.trim(),
  })
}
</script>
