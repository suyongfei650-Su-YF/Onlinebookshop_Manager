import { userRequest } from '../user/http'

export type PortalAiBookCard = {
  id: number
  title: string
  author?: string | null
  price?: number | string | null
  coverUrl?: string | null
  categoryName?: string | null
  soldQty?: number | null
}

export type PortalAiAction = {
  type:
    | 'LOGIN_REQUIRED'
    | 'ADD_TO_CART'
    | 'VIEW_CART'
    | 'ADD_TO_FAVORITE'
    | 'REMOVE_FAVORITE'
    | 'VIEW_FAVORITES'
    | string
  success: boolean
  message?: string | null
  bookId?: number | null
  bookTitle?: string | null
  cartCount?: number | null
}

export type PortalAiChatResult = {
  reply: string
  books?: PortalAiBookCard[]
  usedAi?: boolean
  hint?: string | null
  loggedIn?: boolean
  guest?: boolean
  userNickname?: string | null
  cartCount?: number | null
  favoriteCount?: number | null
  favoriteBookIds?: number[]
  actions?: PortalAiAction[]
}

export type PortalAiStatus = {
  enabled: boolean
  model?: string
  baseUrl?: string
  providerHint?: string
  loggedIn?: boolean
  guest?: boolean
  userNickname?: string | null
  cartCount?: number | null
  favoriteCount?: number | null
  favoriteBookIds?: number[]
}

export type AiChatMessage = {
  role: 'user' | 'assistant'
  content: string
  books?: PortalAiBookCard[]
  hint?: string | null
  usedAi?: boolean
  actions?: PortalAiAction[]
}

export function portalAiStatus() {
  return userRequest<PortalAiStatus>('/portal/ai/status', { method: 'GET' })
}

export function portalAiChat(message: string, history: { role: string; content: string }[]) {
  return userRequest<PortalAiChatResult>('/portal/ai/chat', {
    method: 'POST',
    body: JSON.stringify({ message, history }),
  })
}
