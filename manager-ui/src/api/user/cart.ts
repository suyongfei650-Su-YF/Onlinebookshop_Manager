import { userRequest } from './http'

export type CartBook = {
  cartItemId: number
  bookId: number
  title: string
  author?: string | null
  coverUrl?: string | null
  price: number
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF' | string
  quantity: number
}

export function userCartList() {
  return userRequest<CartBook[]>('/user/cart', { method: 'GET' })
}

export function userCartAdd(bookId: number, quantity = 1) {
  return userRequest<{ message?: string; quantity?: number; cartCount?: number }>('/user/cart', {
    method: 'POST',
    body: JSON.stringify({ bookId, quantity }),
  })
}

export function userCartUpdateQuantity(bookId: number, quantity: number) {
  return userRequest<{ message?: string; quantity?: number }>(`/user/cart/${bookId}`, {
    method: 'PUT',
    body: JSON.stringify({ quantity }),
  })
}

export function userCartRemove(bookId: number) {
  return userRequest<{ message?: string; cartCount?: number }>(`/user/cart/${bookId}`, {
    method: 'DELETE',
  })
}
