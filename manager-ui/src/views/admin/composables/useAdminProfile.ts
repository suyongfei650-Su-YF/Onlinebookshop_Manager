import { computed, ref } from 'vue'
import { adminChangePassword, adminMe, adminUpdateMe } from '../../../api/admin/auth'

type AdminProfile = {
  displayName: string
  roleName: string
  avatarUrl: string
}

const STORAGE_KEY = 'manager_admin_profile'
const DEFAULT_PROFILE: AdminProfile = {
  displayName: '管理员',
  roleName: 'Archivist',
  avatarUrl:
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBZpnEV-1I63pq7R5sn_Ukqo8ZpZTFLxaBLhI_JbtN6ucrdrBywcTWgcJ2nGCc_WfboWuGrpccVllOrostnpMX_lfajkBL-EBrb88g67b665rjMNqGJK3Sj6uo_Ls34sR-0sR8DlG5SanKd5shj8ddkxzIA51dhs2ACSxPiR9ggOYQk7wANj0jPm5380ksWzjE4FstPrz48Ky0ghkWzCzl890HZ9ycyHVitLAKxHW5cZGjcFLDCr2gXPgm_rkqi3GRKK-yqJay2RA',
}

const profile = ref<AdminProfile>(loadProfile())
let loadedFromServer = false

function loadProfile(): AdminProfile {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULT_PROFILE }
    const parsed = JSON.parse(raw) as Partial<AdminProfile>
    return {
      displayName: parsed.displayName?.trim() || DEFAULT_PROFILE.displayName,
      roleName: parsed.roleName?.trim() || DEFAULT_PROFILE.roleName,
      avatarUrl: parsed.avatarUrl?.trim() || DEFAULT_PROFILE.avatarUrl,
    }
  } catch {
    return { ...DEFAULT_PROFILE }
  }
}

function persistProfile(next: AdminProfile) {
  profile.value = next
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

export function useAdminProfile() {
  if (!loadedFromServer) {
    loadedFromServer = true
    void syncFromServer()
  }

  const displayName = computed(() => profile.value.displayName)
  const roleName = computed(() => profile.value.roleName)
  const avatarUrl = computed(() => profile.value.avatarUrl)

  async function updateProfile(payload: { avatarUrl: string }) {
    const next: AdminProfile = {
      displayName: profile.value.displayName || DEFAULT_PROFILE.displayName,
      roleName: profile.value.roleName || DEFAULT_PROFILE.roleName,
      avatarUrl: payload.avatarUrl.trim() || DEFAULT_PROFILE.avatarUrl,
    }
    const res = await adminUpdateMe({
      avatarUrl: next.avatarUrl,
    })
    if (res.success) {
      persistProfile({
        displayName: res.data?.displayName?.trim() || next.displayName,
        roleName: DEFAULT_PROFILE.roleName,
        avatarUrl: res.data?.avatarUrl?.trim() || next.avatarUrl,
      })
      return
    }
    window.alert(res.message || '头像保存失败')
  }

  async function changePassword(payload: { oldPassword: string; newPassword: string }) {
    return adminChangePassword(payload)
  }

  return {
    displayName,
    roleName,
    avatarUrl,
    updateProfile,
    changePassword,
  }
}

async function syncFromServer() {
  try {
    const res = await adminMe()
    if (!res.success || !res.data) return
    persistProfile({
      displayName: res.data.displayName?.trim() || DEFAULT_PROFILE.displayName,
      roleName: DEFAULT_PROFILE.roleName,
      avatarUrl: res.data.avatarUrl?.trim() || DEFAULT_PROFILE.avatarUrl,
    })
  } catch {
    // keep local cache when backend unavailable
  }
}
