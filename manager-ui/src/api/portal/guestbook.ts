import { userRequest } from '../user/http'

export type GuestbookMessage = {
  id: number
  customerId: number
  content: string
  createdAt?: string | null
  nickname?: string | null
  avatarUrl?: string | null
  mine?: boolean
  adminReply?: string | null
  adminReplyAt?: string | null
  replierName?: string | null
}

export type GuestbookListResult = {
  list: GuestbookMessage[]
  total: number
  page: number
  size: number
  loggedIn?: boolean
}

export function portalGuestbookList(page = 1, size = 20) {
  return userRequest<GuestbookListResult>(`/portal/guestbook/messages?page=${page}&size=${size}`, {
    method: 'GET',
  })
}
