/** 解析读者头像 URL（支持 data URL、绝对路径、外链） */
export function resolveUserAvatar(url?: string | null): string {
  if (!url || !url.trim()) {
    return ''
  }
  const u = url.trim()
  if (
    u.startsWith('data:') ||
    u.startsWith('http://') ||
    u.startsWith('https://') ||
    u.startsWith('//') ||
    u.startsWith('/')
  ) {
    return u
  }
  return `/${u.replace(/^\/+/, '')}`
}

export function hasUserAvatar(url?: string | null): boolean {
  return !!resolveUserAvatar(url)
}

/** 昵称/用户名的展示用首字（用于无头像时的圆形占位） */
export function userAvatarInitials(nickname?: string | null, username?: string | null): string {
  const raw = (nickname || username || '读').trim()
  if (!raw) return '读'
  const first = [...raw][0]
  return first || '读'
}
