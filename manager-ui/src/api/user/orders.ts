import { userRequest } from './http'

export type UserOrderItem = {
  orderId: number
  bookId: number
  title: string
  author?: string | null
  coverUrl?: string | null
  quantity: number
  unitPrice: number
}

export type UserOrder = {
  id: number
  orderNo: string
  totalAmount: number
  status: string
  createdAt?: string | null
  items: UserOrderItem[]
}

export const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING_PAY: '待付款',
  PENDING_SHIP: '待发货',
  SHIPPED: '已发货',
  DONE: '已完成',
  CANCELLED: '已取消',
}

export function orderStatusLabel(status: string) {
  return ORDER_STATUS_LABEL[status] || status
}

export function userOrdersList() {
  return userRequest<UserOrder[]>('/user/orders', { method: 'GET' })
}

export function userOrderCancel(orderId: number) {
  return userRequest<{ message?: string; status?: string }>(`/user/orders/${orderId}/cancel`, {
    method: 'POST',
  })
}

export type CreateOrderPayload = {
  bookIds: number[]
  receiverName: string
  receiverPhone: string
  shippingAddress: string
  paymentMethod: 'WECHAT' | 'ALIPAY' | 'CARD' | string
  /** 演示：提交后直接模拟支付成功 */
  payNow?: boolean
}

export function userOrderCreate(body: CreateOrderPayload) {
  return userRequest<UserOrder>('/user/orders', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function userOrderPay(orderId: number) {
  return userRequest<{ message?: string; status?: string }>(`/user/orders/${orderId}/pay`, {
    method: 'POST',
  })
}
