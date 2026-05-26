import { userRequest } from './http'
import type { GuestbookMessage } from '../portal/guestbook'

export function userGuestbookPost(content: string) {
  return userRequest<GuestbookMessage>('/user/guestbook/messages', {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

export function userGuestbookDelete(id: number) {
  return userRequest<{ message?: string }>(`/user/guestbook/messages/${id}`, {
    method: 'DELETE',
  })
}
