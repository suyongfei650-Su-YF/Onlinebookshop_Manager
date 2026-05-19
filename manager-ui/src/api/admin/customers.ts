import { adminRequest } from './http'

export type AdminCustomer = {
  id: number
  nickname: string
  username?: string | null
  email?: string | null
  phone?: string | null
  avatarUrl?: string | null
  roleTag?: string | null
  status: 'ACTIVE' | 'DISABLED' | string
  orderCount?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type AdminCustomerForm = {
  nickname: string
  username: string
  email: string
  phone: string
  password: string
  roleTag: string
  status: 'ACTIVE' | 'DISABLED'
}

export const CUSTOMER_STATUS_LABEL: Record<string, string> = {
  ACTIVE: '账号正常',
  DISABLED: '已被禁用',
}

export function customerStatusLabel(status: string) {
  return CUSTOMER_STATUS_LABEL[status] || status
}

export function adminCustomersList(params: {
  page: number
  size: number
  keyword?: string
  status?: string
  roleTag?: string
}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page))
  q.set('size', String(params.size))
  if (params.keyword?.trim()) q.set('keyword', params.keyword.trim())
  if (params.status?.trim()) q.set('status', params.status.trim())
  if (params.roleTag?.trim()) q.set('roleTag', params.roleTag.trim())
  return adminRequest<{
    list: AdminCustomer[]
    total: number
    page: number
    size: number
    statusCounts: Record<string, number>
  }>(`/admin/customers?${q.toString()}`, { method: 'GET' })
}

export function adminCustomerDetail(id: number) {
  return adminRequest<AdminCustomer>(`/admin/customers/${id}`, { method: 'GET' })
}

export function adminCustomerCreate(body: Partial<AdminCustomerForm>) {
  return adminRequest<AdminCustomer>('/admin/customers', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function adminCustomerUpdate(id: number, body: Partial<AdminCustomerForm> & { password?: string }) {
  return adminRequest<AdminCustomer>(`/admin/customers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function adminCustomerToggleStatus(id: number) {
  return adminRequest<AdminCustomer>(`/admin/customers/${id}/toggle-status`, { method: 'POST' })
}
