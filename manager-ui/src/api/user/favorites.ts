import { userRequest } from './http'

export type FavoriteBook = {
  favoriteId: number
  favoritedAt?: string | null
  id: number
  categoryId?: number | null
  categoryName?: string | null
  title: string
  author?: string | null
  isbn?: string | null
  price: number
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF' | string
  coverUrl?: string | null
  description?: string | null
}

export function userFavoritesList() {
  return userRequest<FavoriteBook[]>('/user/favorites', { method: 'GET' })
}

export function userFavoriteCheck(bookId: number) {
  return userRequest<{ favorited: boolean }>(`/user/favorites/check/${bookId}`, { method: 'GET' })
}

export function userFavoriteAdd(bookId: number) {
  return userRequest<{ favorited: boolean; message?: string }>('/user/favorites', {
    method: 'POST',
    body: JSON.stringify({ bookId }),
  })
}

export function userFavoriteRemove(bookId: number) {
  return userRequest<{ favorited: boolean; message?: string }>(`/user/favorites/${bookId}`, {
    method: 'DELETE',
  })
}
