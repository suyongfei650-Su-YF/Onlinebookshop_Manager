import { adminRequest } from './http'
import type { AdminOrder } from './orders'

export type DashboardDayPoint = {
  day: string
  orderCount: number
  amount?: number
}

export type DashboardTopBook = {
  bookId: number
  title: string
  author?: string | null
  coverUrl?: string | null
  categoryName?: string | null
  isbn?: string | null
  price: number
  stock: number
  status: string
  soldQty: number
}

export type DashboardStats = {
  totalBooks: number
  totalOrders: number
  totalCustomers: number
  lowStockCount: number
  pendingShipCount: number
  pendingPayCount: number
  orderAmountSum: number
  todayOrders: number
  todayAmount: number
  newCustomersToday: number
  salesLast7Days: DashboardDayPoint[]
  orderTrend7Days: DashboardDayPoint[]
  topBooks: DashboardTopBook[]
  recentOrders: AdminOrder[]
}

export function adminDashboardStats() {
  return adminRequest<DashboardStats>('/admin/dashboard', { method: 'GET' })
}
