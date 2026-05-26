import { adminRequest } from './http'

export type AdminGuestbookMessage = {
  id: number
  customerId: number
  content: string
  status?: string | null
  adminReply?: string | null
  adminReplyAt?: string | null
  replierName?: string | null
  createdAt?: string | null
  nickname?: string | null
  avatarUrl?: string | null
}

export type AdminGuestbookListResult = {
  list: AdminGuestbookMessage[]
  total: number
  page: number
  size: number
  pendingCount: number
}

export function adminGuestbookList(params: {
  page?: number
  size?: number
  keyword?: string
  replyFilter?: '' | 'pending' | 'replied'
}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page ?? 1))
  q.set('size', String(params.size ?? 10))
  if (params.keyword?.trim()) q.set('keyword', params.keyword.trim())
  if (params.replyFilter) q.set('replyFilter', params.replyFilter)
  return adminRequest<AdminGuestbookListResult>(`/admin/guestbook/messages?${q}`, { method: 'GET' })
}

export function adminGuestbookReply(id: number, reply: string) {
  return adminRequest<AdminGuestbookMessage>(`/admin/guestbook/messages/${id}/reply`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reply }),
  })
}

export function adminGuestbookClearReply(id: number) {
  return adminRequest<AdminGuestbookMessage>(`/admin/guestbook/messages/${id}/reply`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ clear: true }),
  })
}
