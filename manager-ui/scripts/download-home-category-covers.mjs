/**
 * 下载首页三个分类封面到 public/categories/
 * 运行: node scripts/download-home-category-covers.mjs
 */
import fs from 'node:fs/promises'
import path from 'node:path'

const OUT_DIR = path.join(process.cwd(), 'public', 'categories')

const ITEMS = [
  {
    file: 'art-design.jpg',
    title: '艺术与设计',
    url: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA7rk5Yng7YH4ZPh5tqmNCthUr62nNuebHrcODxDkTJTNa8nSK7B1mchFOOZgPf0FwQZ-qwKRtlWcoir2Jue5b1KkKL5mVrHEznNJd_x7PcEGow7ppo4KqUCCaoih70Bc4OkcB4pXVfhbSJFhVAAARQceuTo__AdhQWn2uFF-q0rhsU6sstLXos59UtelDYCDIV3j5CJHFueEFN5xZCgfcWYKpUGLHXeCoZ6ObF5pPx1WEhHcNH8Lcq1mF48cZ0OgeARHTL8ZyoDQ',
  },
  {
    file: 'classic-literature.jpg',
    title: '经典文学',
    url: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCrsNzK3o9d2zZyf7BzgGGKcltpM6qo85Baz2kP7jlrc--9G14HTvwA31nduvBNiApL0cvIU0l6nYLRCwbJfrSehYmMRPsPygYljJzzvlhuvb-PNvR23FdmBNDZrKM_fX4Vsezd60i0KbNs_SoIKV623dyTELwPh6vj1hE-tN5fmrVvPDk_5wARBWrOfDAVQdJBjI5v8s2ZWfbzMWk35qbar97zNZn9H1sti44qsHV60b1qhlwZ2n73ELyMganxT6U6_ETiXeo_Bw',
  },
  {
    file: 'frontier-science.jpg',
    title: '前沿科技',
    url: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBn-ljaO5Aoymc_ZOXEuRAQaSxWu5nQo7_bk2yzHxgPtBBRg-2SwZyZ3v_t5otLPB85dsnVwylgdNm6Dlvv3Wz156D2MxmDuPRSUBF960FwJOWMbWe4VjQcQ8_oUdmMEDj7Z_76OkLoNdiUMB5GDgXDuhEemsHD3tR9-XM27RyiGEQ5LCvjDMcxci8Zee-g0ZzVGlpd6lFsdZotsB85UH6NokyXqqahvXx0XIyV4dstPRmtoAnj--d48HjXwsQw4Pqma3b73_Z0zg',
  },
]

function extFromContentType(ct) {
  if (!ct) return '.jpg'
  if (ct.includes('png')) return '.png'
  if (ct.includes('webp')) return '.webp'
  if (ct.includes('gif')) return '.gif'
  return '.jpg'
}

async function downloadOne(item) {
  const res = await fetch(item.url, {
    headers: { 'User-Agent': 'Onlinebookshop-GraduationProject/1.0' },
    redirect: 'follow',
  })
  if (!res.ok) throw new Error(`${item.title} HTTP ${res.status}`)
  const buf = Buffer.from(await res.arrayBuffer())
  if (buf.length < 500) throw new Error(`${item.title} 文件过小`)
  const ext = extFromContentType(res.headers.get('content-type') || '')
  const base = item.file.replace(/\.[^.]+$/, '')
  const outPath = path.join(OUT_DIR, base + ext)
  await fs.writeFile(outPath, buf)
  return { ...item, saved: path.relative(process.cwd(), outPath).replace(/\\/g, '/'), bytes: buf.length }
}

await fs.mkdir(OUT_DIR, { recursive: true })
console.log('保存目录:', OUT_DIR)
for (const item of ITEMS) {
  const r = await downloadOne(item)
  console.log(`[OK] ${r.title} -> ${r.saved} (${r.bytes} bytes)`)
}
console.log('完成')
