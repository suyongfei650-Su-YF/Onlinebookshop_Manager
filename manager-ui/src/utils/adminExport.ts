import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import type { BookAlertCategory, BookAlertReport } from '../api/admin/bookAlerts'
import type { DashboardStats, DashboardTopBook } from '../api/admin/dashboard'
import { ORDER_STATUS_LABEL, PAYMENT_METHOD_LABEL } from '../api/admin/orders'

type SheetRow = Record<string, string | number>

type SheetInput = {
  name: string
  rows: SheetRow[]
}

type BookLike = {
  bookId?: number
  title?: string
  author?: string | null
  isbn?: string | null
  categoryName?: string | null
  price?: number | null
  stock?: number
  status?: string | null
  soldQty?: number | null
}

const URGENCY_ORDER: Record<string, number> = { 紧急: 0, 高: 1, 中: 2, 低: 3 }

export function exportTimestamp(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}_${p(d.getHours())}${p(d.getMinutes())}`
}

function downloadSheets(baseName: string, sheets: SheetInput[]) {
  const wb = XLSX.utils.book_new()
  for (const sheet of sheets) {
    const ws = XLSX.utils.json_to_sheet(sheet.rows.length ? sheet.rows : [{ 提示: '暂无数据' }])
    XLSX.utils.book_append_sheet(wb, ws, sheet.name.slice(0, 31))
  }
  const buf = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  saveAs(
    new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    `${baseName}_${exportTimestamp()}.xlsx`,
  )
}

function stockStatusLabel(stock: number, status?: string | null): string {
  if (status !== 'ON_SHELF') return '已下架'
  if (stock <= 0) return '售空断货'
  if (stock < 10) return '低库存'
  return '库存充足'
}

function suggestRestockQty(stock: number, sold: number, threshold: number): number {
  if (stock <= 0) {
    return Math.max(threshold * 2, sold + threshold, 15)
  }
  if (stock < threshold) {
    return Math.max(threshold - stock + 5, sold > 0 ? Math.ceil(sold / 2) : 10)
  }
  return 0
}

function suggestAction(alertType: string, stock: number, sold: number, threshold: number): string {
  if (alertType === '售空') {
    return sold > 0
      ? '立即补货！该书有购买记录，断货将影响下单'
      : '立即补货或暂时下架，避免读者下单失败'
  }
  if (alertType === '低库存') {
    return `尽快补货，建议补至 ${threshold} 册以上（当前仅 ${stock} 册）`
  }
  if (alertType === '畅销大卖') {
    if (stock <= 0) return '畅销且已断货，优先紧急补货并首页推荐'
    if (stock < threshold) return '畅销且库存偏低，建议加印并加大推广'
    return '畅销品，可首页推荐或适度加印'
  }
  if (alertType === '建议减量') {
    return `库存 ${stock} 册但仅售 ${sold} 册，建议减少进货或促销清仓`
  }
  return ''
}

function urgencyLevel(alertType: string, stock: number, sold: number): string {
  if (alertType === '售空') return '紧急'
  if (alertType === '低库存') {
    if (stock <= 2 || sold >= 10) return '高'
    return '中'
  }
  if (alertType === '畅销大卖') {
    if (stock <= 0) return '紧急'
    if (stock < 10) return '高'
    return '中'
  }
  if (alertType === '建议减量') return '低'
  return '中'
}

function estimateDaysLeft(stock: number, sold: number): string | number {
  if (stock <= 0) return 0
  if (sold <= 0) return '暂无销量参考'
  const daily = sold / 30
  if (daily <= 0) return '—'
  return Math.max(1, Math.floor(stock / daily))
}

function mapDetailedBook(b: BookLike, alertType: string, threshold: number): SheetRow {
  const stock = b.stock ?? 0
  const sold = b.soldQty ?? 0
  const needRestock = alertType === '售空' || alertType === '低库存' || (alertType === '畅销大卖' && stock < threshold)
  const restockQty = needRestock ? suggestRestockQty(stock, sold, threshold) : ''
  const gap = stock < threshold ? Math.max(0, threshold - stock) : stock <= 0 ? threshold : ''

  return {
    紧急程度: urgencyLevel(alertType, stock, sold),
    预警类型: alertType,
    建议操作: suggestAction(alertType, stock, sold, threshold),
    图书ID: b.bookId ?? '',
    ISBN: b.isbn ?? '',
    书名: b.title ?? '',
    作者: b.author ?? '',
    分类: b.categoryName ?? '',
    定价: b.price ?? '',
    当前库存: stock,
    库存状态: stockStatusLabel(stock, b.status),
    库存缺口: gap,
    建议补货数量: restockQty,
    累计销量: sold,
    预估可售天数: estimateDaysLeft(stock, sold),
    上架状态: b.status === 'ON_SHELF' ? '在售' : '已下架',
  }
}

function buildRestockActionList(report: BookAlertReport): SheetRow[] {
  const threshold = report.lowStockThreshold
  const rows: SheetRow[] = [
    ...report.outOfStock.map((b) => mapDetailedBook(b, '售空', threshold)),
    ...report.lowStock.map((b) => mapDetailedBook(b, '低库存', threshold)),
  ]
  return rows.sort((a, b) => {
    const ua = URGENCY_ORDER[String(a.紧急程度)] ?? 9
    const ub = URGENCY_ORDER[String(b.紧急程度)] ?? 9
    if (ua !== ub) return ua - ub
    return Number(a.当前库存) - Number(b.当前库存)
  })
}

function mapCategorySuggest(c: BookAlertCategory): SheetRow {
  let action = '关注该分类动销'
  if (c.bookCount === 0) {
    action = '该分类无在售图书，建议尽快上架首批书目'
  } else if (c.soldQty >= 8 && c.bookCount < 5) {
    action = `销量 ${c.soldQty} 册但仅 ${c.bookCount} 种在售，建议扩充书目`
  } else if (c.soldQty >= 3 && c.bookCount <= 2) {
    action = '读者有需求但可选书目太少，建议增加同类新书'
  }
  return {
    分类ID: c.categoryId,
    分类名称: c.categoryName,
    在售种类: c.bookCount,
    累计销量: c.soldQty,
    库存合计: c.stockTotal,
    建议操作: action,
    优先级: c.soldQty >= 8 && c.bookCount < 5 ? '高' : c.bookCount <= 2 ? '中' : '低',
  }
}

function mapPieRows(title: string, slices: { name: string; value: number }[]): SheetRow[] {
  return slices.map((s) => ({ 图表: title, 名称: s.name, 数值: s.value }))
}

function mapDashboardTopBook(b: DashboardTopBook, i: number, threshold = 10): SheetRow {
  const alertType =
    b.status !== 'ON_SHELF' ? '已下架' : b.stock <= 0 ? '售空' : b.stock < threshold ? '低库存' : '畅销'
  const typeForDetail = b.stock <= 0 ? '售空' : b.stock < threshold ? '低库存' : '畅销大卖'
  const base = mapDetailedBook(b, typeForDetail, threshold)
  return { 排名: i + 1, ...base, 预警类型: alertType }
}

export function exportDashboardExcel(stats: DashboardStats, alertReport?: BookAlertReport | null) {
  const threshold = alertReport?.lowStockThreshold ?? 10
  const sheets: SheetInput[] = [
    {
      name: '概览统计',
      rows: [
        { 指标: '导出时间', 数值: new Date().toLocaleString('zh-CN') },
        { 指标: '图书总数', 数值: stats.totalBooks },
        { 指标: '订单总数', 数值: stats.totalOrders },
        { 指标: '读者总数', 数值: stats.totalCustomers },
        { 指标: '低库存图书(<' + threshold + ')', 数值: stats.lowStockCount },
        { 指标: '待发货订单', 数值: stats.pendingShipCount },
        { 指标: '待付款订单', 数值: stats.pendingPayCount },
        { 指标: '累计成交额(元)', 数值: stats.orderAmountSum },
        { 指标: '今日订单', 数值: stats.todayOrders },
        { 指标: '今日成交额(元)', 数值: stats.todayAmount },
        { 指标: '今日新读者', 数值: stats.newCustomersToday },
      ],
    },
  ]

  if (alertReport) {
    sheets.push({
      name: '补货行动清单',
      rows: buildRestockActionList(alertReport),
    })
    sheets.push({
      name: '断货明细',
      rows: alertReport.outOfStock.map((b) => mapDetailedBook(b, '售空', threshold)),
    })
    sheets.push({
      name: '低库存补货清单',
      rows: alertReport.lowStock.map((b) => mapDetailedBook(b, '低库存', threshold)),
    })
  } else {
    const riskBooks = (stats.topBooks || []).filter(
      (b) => b.status === 'ON_SHELF' && (b.stock <= 0 || b.stock < threshold),
    )
    if (riskBooks.length) {
      sheets.push({
        name: '库存风险图书',
        rows: riskBooks.map((b, i) => mapDashboardTopBook(b, i, threshold)),
      })
    }
  }

  sheets.push(
    {
      name: '近7日销售',
      rows: (stats.salesLast7Days || []).map((p) => ({
        日期: p.day,
        订单数: p.orderCount,
        成交额: p.amount ?? 0,
        说明: '不含已取消与待付款订单',
      })),
    },
    {
      name: '近7日订单趋势',
      rows: (stats.orderTrend7Days || []).map((p) => ({
        日期: p.day,
        订单数: p.orderCount,
      })),
    },
    {
      name: '畅销图书明细',
      rows: (stats.topBooks || []).map((b, i) => mapDashboardTopBook(b, i, threshold)),
    },
    {
      name: '最新订单',
      rows: (stats.recentOrders || []).map((o) => ({
        订单ID: o.id,
        订单号: o.orderNo,
        读者昵称: o.customerNickname || '',
        用户名: o.customerUsername || '',
        订单金额: o.totalAmount,
        订单状态: ORDER_STATUS_LABEL[o.status] || o.status,
        支付方式: o.paymentMethod ? PAYMENT_METHOD_LABEL[o.paymentMethod] || o.paymentMethod : '',
        商品件数: o.itemCount ?? '',
        收货人: o.receiverName ?? '',
        联系电话: o.receiverPhone ?? '',
        收货地址: o.shippingAddress ?? '',
        下单时间: o.createdAt ?? '',
      })),
    },
  )

  if (alertReport) {
    sheets.push(
      {
        name: '畅销需加印',
        rows: alertReport.hotSelling.map((b) => mapDetailedBook(b, '畅销大卖', threshold)),
      },
      {
        name: '建议减量明细',
        rows: alertReport.reduceCandidates.map((b) => mapDetailedBook(b, '建议减量', threshold)),
      },
      {
        name: '建议扩充品类',
        rows: alertReport.suggestAddCategories.map(mapCategorySuggest),
      },
    )
  }

  downloadSheets('馆藏书屋_仪表盘', sheets)
}

export function exportBookAlertsExcel(report: BookAlertReport, aiAnalysis?: string) {
  const threshold = report.lowStockThreshold
  const sheets: SheetInput[] = [
    {
      name: '预警概览',
      rows: [
        { 指标: '统计日期', 数值: report.dateLabel },
        { 指标: '导出时间', 数值: new Date().toLocaleString('zh-CN') },
        { 指标: '售空断货(种)', 数值: report.counts.outOfStock, 说明: '库存为0的在售图书，需立即处理' },
        { 指标: '低库存(种)', 数值: report.counts.lowStock, 说明: `库存 1~${threshold - 1} 册，建议补货` },
        { 指标: '库存充足(种)', 数值: report.counts.sufficient },
        { 指标: '畅销大卖(种)', 数值: report.counts.hotSelling, 说明: '近30天热销，可考虑加印' },
        { 指标: '建议减量(种)', 数值: report.counts.reduceCandidates, 说明: '库存高销量低，建议减进货' },
        { 指标: '预警合计', 数值: report.counts.alertTotal },
      ],
    },
    {
      name: '补货行动清单',
      rows: buildRestockActionList(report),
    },
    {
      name: '断货明细',
      rows: report.outOfStock.map((b) => mapDetailedBook(b, '售空', threshold)),
    },
    {
      name: '低库存补货清单',
      rows: report.lowStock.map((b) => mapDetailedBook(b, '低库存', threshold)),
    },
    {
      name: '畅销需加印',
      rows: report.hotSelling.map((b) => mapDetailedBook(b, '畅销大卖', threshold)),
    },
    {
      name: '建议减量明细',
      rows: report.reduceCandidates.map((b) => mapDetailedBook(b, '建议减量', threshold)),
    },
    {
      name: '建议扩充品类',
      rows: report.suggestAddCategories.map(mapCategorySuggest),
    },
    {
      name: '分类统计',
      rows: report.categoryStats.map((c) => ({
        ...mapCategorySuggest(c),
        动销评价: c.soldQty >= 10 ? '热销分类' : c.soldQty > 0 ? '有销量' : '暂无销量',
      })),
    },
    {
      name: '图表-库存状态',
      rows: mapPieRows('库存状态分布', report.stockStatusPie),
    },
    {
      name: '图表-预警类型',
      rows: mapPieRows('预警类型占比', report.alertTypePie),
    },
    {
      name: '图表-分类销量',
      rows: mapPieRows('分类销量占比', report.categorySalesPie),
    },
    {
      name: '图表-分类对比',
      rows: report.categoryBarChart.map((c) => ({
        分类: c.categoryName,
        销量: c.soldQty,
        在售种类: c.bookCount,
        库存合计: c.stockTotal,
      })),
    },
  ]

  const analysis = (aiAnalysis || report.ruleAnalysis || '').trim()
  if (analysis) {
    sheets.push({
      name: '经营建议',
      rows: analysis.split('\n').filter(Boolean).map((line, i) => ({
        序号: i + 1,
        内容: line,
      })),
    })
  }

  downloadSheets('馆藏书屋_图书预警', sheets)
}
