<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4"
    @click.self="close"
  >
    <div
      class="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-outline-variant bg-surface-container-lowest shadow-2xl"
      role="dialog"
      aria-modal="true"
    >
      <header class="sticky top-0 z-10 flex items-center justify-between border-b border-outline-variant bg-surface-container-lowest px-6 py-4">
        <h3 class="font-headline-md text-lg text-primary">{{ title }}</h3>
        <button type="button" class="rounded-full p-2 text-outline hover:bg-surface-container-high" @click="close">
          <span class="material-symbols-outlined">close</span>
        </button>
      </header>

      <form class="space-y-4 p-6" @submit.prevent="submit">
        <p v-if="formError" class="rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ formError }}</p>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">分类名称 <span class="text-error">*</span></span>
          <input
            v-model="form.name"
            type="text"
            maxlength="128"
            required
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="如：文学与艺术"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">英文名称</span>
          <input
            v-model="form.nameEn"
            type="text"
            maxlength="128"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="Literature"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">分类编码</span>
          <input
            v-model="form.code"
            type="text"
            maxlength="32"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="ARCHIVE_B"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">父级分类</span>
          <select
            v-model="parentIdStr"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            :disabled="!!fixedParentId"
          >
            <option value="">无（顶级分类）</option>
            <option v-for="p in parentOptions" :key="p.id" :value="String(p.id)">{{ p.name }}</option>
          </select>
        </label>

        <div class="grid grid-cols-2 gap-4">
          <label class="block">
            <span class="mb-1 block text-xs text-on-surface-variant">排序权重</span>
            <input
              v-model.number="form.sortWeight"
              type="number"
              min="0"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            />
            <span class="mt-1 block text-[10px] text-stone-400">数值越小越靠前</span>
          </label>
          <label class="block">
            <span class="mb-1 block text-xs text-on-surface-variant">展示状态</span>
            <select
              v-model.number="form.visible"
              class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            >
              <option :value="1">显示中</option>
              <option :value="0">已隐藏</option>
            </select>
          </label>
        </div>

        <footer class="flex justify-end gap-3 border-t border-outline-variant pt-4">
          <button type="button" class="rounded-lg border border-outline-variant px-4 py-2 text-sm" @click="close">取消</button>
          <button
            type="submit"
            class="rounded-lg bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-50"
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
import type { AdminCategory, AdminCategoryForm } from '../../../api/admin/categories'

const props = defineProps<{
  modelValue: boolean
  category: AdminCategory | null
  parentOptions: AdminCategory[]
  fixedParentId?: number | null
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: AdminCategoryForm]
}>()

const formError = ref('')
const form = reactive({
  name: '',
  nameEn: '',
  code: '',
  sortWeight: 0,
  visible: 1,
})

const parentIdStr = ref('')

const editing = computed(() => !!props.category)
const title = computed(() => {
  if (props.fixedParentId && !props.category) return '新增子分类'
  return editing.value ? '编辑分类' : '新增类目'
})

const parentOptions = computed(() =>
  props.parentOptions.filter((p) => !props.category || p.id !== props.category.id),
)

watch(
  () => [props.modelValue, props.category, props.fixedParentId] as const,
  ([open]) => {
    if (!open) return
    formError.value = ''
    if (props.category) {
      form.name = props.category.name
      form.nameEn = props.category.nameEn || ''
      form.code = props.category.code || ''
      form.sortWeight = props.category.sortWeight ?? 0
      form.visible = props.category.visible ?? 1
      parentIdStr.value = props.category.parentId ? String(props.category.parentId) : ''
    } else {
      form.name = ''
      form.nameEn = ''
      form.code = ''
      form.sortWeight = 0
      form.visible = 1
      parentIdStr.value = props.fixedParentId ? String(props.fixedParentId) : ''
    }
  },
  { immediate: true },
)

function close() {
  emit('update:modelValue', false)
}

function submit() {
  formError.value = ''
  const name = form.name.trim()
  if (!name) {
    formError.value = '请填写分类名称'
    return
  }
  const parentId = parentIdStr.value ? Number(parentIdStr.value) : null
  emit('submit', {
    name,
    nameEn: form.nameEn.trim(),
    code: form.code.trim(),
    parentId: Number.isFinite(parentId as number) ? parentId : null,
    sortWeight: form.sortWeight ?? 0,
    visible: form.visible ?? 1,
  })
}
</script>
