import { adminRequest } from './http'

export type BookAlertBook = {
  bookId: number
  title: string
  author?: string | null
  coverUrl?: string | null
  categoryName?: string | null
  isbn?: string | null
  price?: number | null
  stock: number
  status?: string | null
  soldQty?: number | null
}

export type BookAlertCategory = {
  categoryId: number
  categoryName: string
  soldQty: number
  bookCount: number
  stockTotal: number
}

export type PieSlice = { name: string; value: number }

export type CategoryBarPoint = {
  categoryName: string
  soldQty: number
  bookCount: number
  stockTotal: number
}

export type BookAlertReport = {
  date: string
  dateLabel: string
  lowStockThreshold: number
  outOfStock: BookAlertBook[]
  lowStock: BookAlertBook[]
  hotSelling: BookAlertBook[]
  reduceCandidates: BookAlertBook[]
  suggestAddCategories: BookAlertCategory[]
  categoryStats: BookAlertCategory[]
  counts: {
    outOfStock: number
    lowStock: number
    sufficient: number
    hotSelling: number
    reduceCandidates: number
    alertTotal: number
  }
  stockStatusPie: PieSlice[]
  alertTypePie: PieSlice[]
  categorySalesPie: PieSlice[]
  categoryBarChart: CategoryBarPoint[]
  ruleAnalysis?: string | null
}

export type BookAlertAiResult = {
  report: BookAlertReport
  aiEnabled: boolean
  model?: string | null
  analysis?: string | null
  hint?: string | null
}

export function adminBookAlertsReport() {
  return adminRequest<BookAlertReport>('/admin/book-alerts', { method: 'GET' })
}

export function adminBookAlertsAiAnalyze() {
  return adminRequest<BookAlertAiResult>('/admin/book-alerts/ai-analyze', { method: 'POST' })
}
