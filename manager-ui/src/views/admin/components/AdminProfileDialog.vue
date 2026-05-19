<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  displayName: string
  avatarUrl: string
  roleName: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', payload: { avatarUrl: string }): void
  (e: 'change-password', payload: { oldPassword: string; newPassword: string }): void
}>()

const editAvatar = ref('')
const fileInputKey = ref(0)
const oldPassword = ref('')
const newPassword = ref('')
const pwdMsg = ref('')

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      editAvatar.value = props.avatarUrl
      oldPassword.value = ''
      newPassword.value = ''
      pwdMsg.value = ''
    }
  },
  { immediate: true },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function onSave() {
  emit('save', {
    avatarUrl: editAvatar.value,
  })
  closeDialog()
}

function onChangePassword() {
  if (!oldPassword.value.trim() || !newPassword.value.trim()) {
    pwdMsg.value = '请输入旧密码和新密码'
    return
  }
  emit('change-password', {
    oldPassword: oldPassword.value,
    newPassword: newPassword.value,
  })
  oldPassword.value = ''
  newPassword.value = ''
  pwdMsg.value = '修改请求已提交'
}

function onAvatarFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) return
  const reader = new FileReader()
  reader.onload = () => {
    if (typeof reader.result === 'string') {
      editAvatar.value = reader.result
    }
  }
  reader.readAsDataURL(file)
}

function clearSelectedFile() {
  fileInputKey.value += 1
}
</script>

<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 z-[80] flex items-center justify-center bg-black/40 px-4"
    @click.self="closeDialog"
  >
    <div class="w-full max-w-[560px] rounded-xl bg-white p-6 shadow-2xl">
      <h3 class="mb-1 text-xl font-semibold text-primary">个人信息设置</h3>
      <p class="mb-5 text-sm text-stone-500">姓名与数据库保持一致，可修改头像与密码</p>
      <div class="mb-6 flex items-center gap-4 rounded-lg bg-stone-50 p-4">
        <img :src="editAvatar" alt="头像预览" class="h-14 w-14 rounded-full border border-stone-200 object-cover" />
        <div>
          <p class="font-medium text-stone-900">{{ displayName || '管理员' }}</p>
          <p class="text-xs text-stone-500">{{ roleName }}</p>
        </div>
      </div>
      <div class="space-y-4">
        <div>
          <label class="mb-1 block text-sm text-stone-600">姓名（只读）</label>
          <div class="w-full rounded-lg border border-stone-200 bg-stone-50 px-3 py-2 text-sm text-stone-700">
            {{ displayName || '管理员' }}
          </div>
        </div>
        <div>
          <label class="mb-1 block text-sm text-stone-600">上传头像</label>
          <div class="rounded-lg border border-stone-200 p-3">
            <input
              :key="fileInputKey"
              type="file"
              accept="image/*"
              class="block w-full text-sm text-stone-600 file:mr-3 file:rounded-md file:border-0 file:bg-stone-100 file:px-3 file:py-2 file:text-sm file:text-stone-700 hover:file:bg-stone-200"
              @change="onAvatarFileChange"
            />
            <p class="mt-2 text-xs text-stone-500">支持 JPG/PNG/GIF/WebP，选择后将自动预览</p>
            <button
              type="button"
              class="mt-2 text-xs text-stone-500 underline underline-offset-2 hover:text-stone-700"
              @click="clearSelectedFile"
            >
              重新选择
            </button>
          </div>
        </div>
        <div class="rounded-lg border border-stone-200 p-3">
          <p class="mb-3 text-sm font-medium text-stone-700">修改密码</p>
          <div class="space-y-3">
            <input
              v-model="oldPassword"
              type="password"
              class="w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="请输入旧密码"
            />
            <input
              v-model="newPassword"
              type="password"
              class="w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-secondary focus:outline-none"
              placeholder="请输入新密码（至少6位）"
            />
          </div>
          <p v-if="pwdMsg" class="mt-2 text-xs text-stone-500">{{ pwdMsg }}</p>
          <div class="mt-3 flex justify-end">
            <button
              type="button"
              class="rounded-lg bg-secondary px-4 py-2 text-sm text-white transition hover:opacity-90"
              @click="onChangePassword"
            >
              修改密码
            </button>
          </div>
        </div>
      </div>
      <div class="mt-6 flex justify-end gap-3">
        <button
          type="button"
          class="rounded-lg border border-stone-200 px-4 py-2 text-sm text-stone-600 transition hover:bg-stone-50"
          @click="closeDialog"
        >
          取消
        </button>
        <button
          type="button"
          class="rounded-lg bg-primary px-4 py-2 text-sm text-white transition hover:opacity-90"
          @click="onSave"
        >
          保存
        </button>
      </div>
    </div>
  </div>
</template>
