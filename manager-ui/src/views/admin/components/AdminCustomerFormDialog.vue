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
        <h3 class="font-headline-md text-lg text-primary">{{ editing ? '编辑用户' : '新增用户' }}</h3>
        <button type="button" class="rounded-full p-2 text-outline hover:bg-surface-container-high" @click="close">
          <span class="material-symbols-outlined">close</span>
        </button>
      </header>

      <form class="space-y-4 p-6" @submit.prevent="submit">
        <p v-if="formError" class="rounded-lg bg-error-container px-3 py-2 text-sm text-on-error-container">{{ formError }}</p>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">昵称 <span class="text-error">*</span></span>
          <input
            v-model="form.nickname"
            type="text"
            maxlength="64"
            required
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="读者昵称"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">用户名</span>
          <input
            v-model="form.username"
            type="text"
            maxlength="32"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="3-32位字母/数字/下划线"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">邮箱</span>
          <input
            v-model="form.email"
            type="email"
            maxlength="128"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="user@example.com"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">手机号</span>
          <input
            v-model="form.phone"
            type="tel"
            maxlength="11"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            placeholder="11位手机号"
          />
        </label>

        <p class="text-[11px] text-outline">用户名、邮箱、手机号至少填写一项，用于登录</p>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">{{ editing ? '重置密码（留空不修改）' : '登录密码' }}</span>
          <input
            v-model="form.password"
            type="password"
            maxlength="128"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
            :placeholder="editing ? '不修改请留空' : '默认 123456'"
          />
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">读者标签</span>
          <select
            v-model="form.roleTag"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
          >
            <option value="普通读者">普通读者</option>
            <option value="高级读者">高级读者</option>
            <option value="认证学者">认证学者</option>
            <option value="资深会员">资深会员</option>
          </select>
        </label>

        <label class="block">
          <span class="mb-1 block text-xs text-on-surface-variant">账号状态</span>
          <select
            v-model="form.status"
            class="w-full rounded-lg border border-outline-variant bg-surface-bright px-3 py-2 text-sm focus:border-secondary focus:outline-none"
          >
            <option value="ACTIVE">正常（ACTIVE）</option>
            <option value="DISABLED">禁用（DISABLED）</option>
          </select>
        </label>

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
import type { AdminCustomer, AdminCustomerForm } from '../../../api/admin/customers'

const props = defineProps<{
  modelValue: boolean
  customer: AdminCustomer | null
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [AdminCustomerForm & { password?: string }]
}>()

const formError = ref('')

const emptyForm = (): AdminCustomerForm => ({
  nickname: '',
  username: '',
  email: '',
  phone: '',
  password: '',
  roleTag: '普通读者',
  status: 'ACTIVE',
})

const form = reactive<AdminCustomerForm>(emptyForm())
const editing = computed(() => props.customer != null)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    formError.value = ''
    if (props.customer) {
      form.nickname = props.customer.nickname || ''
      form.username = props.customer.username || ''
      form.email = props.customer.email || ''
      form.phone = props.customer.phone || ''
      form.password = ''
      form.roleTag = props.customer.roleTag || '普通读者'
      form.status = props.customer.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE'
    } else {
      Object.assign(form, emptyForm())
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

function submit() {
  formError.value = ''
  if (!form.nickname.trim()) {
    formError.value = '昵称不能为空'
    return
  }
  if (!form.username.trim() && !form.email.trim() && !form.phone.trim()) {
    formError.value = '请至少填写用户名、邮箱或手机号之一'
    return
  }
  const payload: AdminCustomerForm & { password?: string } = {
    nickname: form.nickname.trim(),
    username: form.username.trim(),
    email: form.email.trim(),
    phone: form.phone.trim(),
    password: form.password.trim(),
    roleTag: form.roleTag,
    status: form.status,
  }
  if (editing.value && !payload.password) {
    delete payload.password
  }
  if (!editing.value && !payload.password) {
    payload.password = '123456'
  }
  emit('submit', payload)
}
</script>
