import { adminRequest } from './http'

export type AdminOrder = {
  id: number
  orderNo: string
  customerId: number
  customerNickname?: string | null
  customerUsername?: string | null
  customerAvatarUrl?: string | null
  totalAmount: number
  status: string
  receiverName?: string | null
  receiverPhone?: string | null
  shippingAddress?: string | null
  paymentMethod?: string | null
  itemCount?: number | null
  createdAt?: string | null
}

export type AdminOrderItem = {
  orderId: number
  bookId: number
  title: string
  author?: string | null
  coverUrl?: string | null
  quantity: number
  unitPrice: number
}

export type AdminOrderDetail = AdminOrder & {
  items: AdminOrderItem[]
}

export const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING_PAY: '待付款',
  PENDING_SHIP: '待发货',
  SHIPPED: '已发货',
  DONE: '已完成',
  CANCELLED: '已取消',
}

export const PAYMENT_METHOD_LABEL: Record<string, string> = {
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
  CARD: '银行卡',
}

export function orderStatusLabel(status: string) {
  return ORDER_STATUS_LABEL[status] || status
}

export function paymentMethodLabel(method?: string | null) {
  if (!method?.trim()) return '—'
  return PAYMENT_METHOD_LABEL[method] || method
}

export function adminOrdersList(params: {
  page: number
  size: number
  keyword?: string
  status?: string
}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page))
  q.set('size', String(params.size))
  if (params.keyword?.trim()) q.set('keyword', params.keyword.trim())
  if (params.status?.trim()) q.set('status', params.status.trim())
  return adminRequest<{
    list: AdminOrder[]
    total: number
    page: number
    size: number
    statusCounts: Record<string, number>
    paidTotalAmount: number
  }>(`/admin/orders?${q.toString()}`, { method: 'GET' })
}

export function adminOrderDetail(id: number) {
  return adminRequest<AdminOrderDetail>(`/admin/orders/${id}`, { method: 'GET' })
}

export function adminOrderShip(id: number) {
  return adminRequest<{ status: string }>(`/admin/orders/${id}/ship`, { method: 'POST' })
}

export function adminOrderComplete(id: number) {
  return adminRequest<{ status: string }>(`/admin/orders/${id}/complete`, { method: 'POST' })
}
