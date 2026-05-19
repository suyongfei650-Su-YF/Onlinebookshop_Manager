import { createHash } from 'node:crypto'
import fs from 'node:fs/promises'
import path from 'node:path'

const PROJECT_ROOT = path.resolve(process.cwd())
const TARGET_DIR = path.join(PROJECT_ROOT, 'src', 'assets', 'remote')
const MANIFEST_PATH = path.join(TARGET_DIR, 'remote-images.manifest.json')

const SCAN_DIRS = [
  path.join(PROJECT_ROOT, 'src'),
  path.join(PROJECT_ROOT, 'index.html'),
]

const TEXT_FILE_EXTS = new Set(['.vue', '.ts', '.tsx', '.js', '.jsx', '.css', '.scss', '.less', '.html'])

const CONTENT_TYPE_TO_EXT = new Map([
  ['image/jpeg', '.jpg'],
  ['image/jpg', '.jpg'],
  ['image/png', '.png'],
  ['image/gif', '.gif'],
  ['image/webp', '.webp'],
  ['image/svg+xml', '.svg'],
  ['image/x-icon', '.ico'],
  ['image/vnd.microsoft.icon', '.ico'],
])

function sha1(input) {
  return createHash('sha1').update(input).digest('hex')
}

function safeBaseName(name) {
  return name
    .replace(/[/\\?%*:|"<>]/g, '_')
    .replace(/\s+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 120) || 'image'
}

async function fileExists(p) {
  try {
    await fs.access(p)
    return true
  } catch {
    return false
  }
}

async function listFilesRecursively(entry) {
  const st = await fs.stat(entry)
  if (st.isFile()) return [entry]
  const out = []
  const dirents = await fs.readdir(entry, { withFileTypes: true })
  for (const d of dirents) {
    if (d.name === 'node_modules' || d.name === 'dist' || d.name.startsWith('.')) continue
    const full = path.join(entry, d.name)
    if (d.isDirectory()) out.push(...(await listFilesRecursively(full)))
    else out.push(full)
  }
  return out
}

async function collectUrls() {
  const files = []
  for (const entry of SCAN_DIRS) {
    if (await fileExists(entry)) files.push(...(await listFilesRecursively(entry)))
  }

  const urls = new Set()
  const urlRe = /https?:\/\/[^\s"'()<>]+/gi
  for (const f of files) {
    const ext = path.extname(f).toLowerCase()
    if (!TEXT_FILE_EXTS.has(ext)) continue
    const text = await fs.readFile(f, 'utf8')
    const matches = text.match(urlRe) || []
    for (let u of matches) {
      if (u.startsWith('http://localhost') || u.startsWith('http://127.0.0.1')) continue
      if (u.startsWith('https://vite.dev') || u.startsWith('https://vuejs.org') || u.startsWith('https://github.com') || u.startsWith('https://chat.vite.dev') || u.startsWith('https://x.com') || u.startsWith('https://bsky.app')) continue
      if (u.startsWith('data:image/')) continue
      // Trim trailing punctuation that often appears in CSS/TS.
      u = u.replace(/[),.;]+$/g, '')
      urls.add(u)
    }
  }
  return [...urls]
}

function inferExtFromUrl(u) {
  try {
    const url = new URL(u)
    const base = path.basename(url.pathname)
    const ext = path.extname(base).toLowerCase()
    if (ext && ext.length <= 6) return ext
  } catch {
    // ignore
  }
  return ''
}

function inferNameFromUrl(u) {
  try {
    const url = new URL(u)
    const base = path.basename(url.pathname) || ''
    const noQuery = base.split('?')[0]
    const stem = noQuery.replace(path.extname(noQuery), '')
    const cleaned = safeBaseName(stem || 'image')
    return cleaned
  } catch {
    return safeBaseName('image')
  }
}

function hasExtension(p) {
  return Boolean(path.extname(p))
}

async function ensureDir(p) {
  await fs.mkdir(p, { recursive: true })
}

async function downloadOne(url, existingMap) {
  const res = await fetch(url, { redirect: 'follow' })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)

  const contentType = (res.headers.get('content-type') || '').split(';')[0].trim().toLowerCase()
  if (!contentType.startsWith('image/')) {
    return { url, skipped: true, reason: `not image (${contentType || 'unknown'})` }
  }

  const extFromType = CONTENT_TYPE_TO_EXT.get(contentType) || ''
  const extFromUrl = inferExtFromUrl(url)
  const ext = extFromType || extFromUrl || '.img'

  // Use a short deterministic name to avoid Windows path/length issues.
  const id = sha1(url).slice(0, 16)
  let filename = `${id}${ext}`
  filename = safeBaseName(filename)

  const absPath = path.join(TARGET_DIR, filename)
  const relPath = path.posix.join('src/assets/remote', filename)

  // If we've already downloaded it in a previous run, don't re-download.
  if (existingMap[url]) {
    const existingRel = existingMap[url]
    const existingAbs = path.join(PROJECT_ROOT, existingRel.replace(/\//g, path.sep))
    if ((await fileExists(existingAbs)) && hasExtension(existingRel)) {
      return { url, skipped: true, reason: 'already downloaded', relPath: existingRel }
    }
  }
  if (await fileExists(absPath)) {
    return { url, skipped: true, reason: 'file exists', relPath }
  }

  const buf = Buffer.from(await res.arrayBuffer())
  await fs.writeFile(absPath, buf)
  return { url, skipped: false, relPath, bytes: buf.length, contentType }
}

async function runPool(items, worker, concurrency) {
  const results = new Array(items.length)
  let idx = 0
  const runners = new Array(concurrency).fill(0).map(async () => {
    while (true) {
      const i = idx++
      if (i >= items.length) return
      results[i] = await worker(items[i])
    }
  })
  await Promise.all(runners)
  return results
}

async function readExistingManifest() {
  try {
    const text = await fs.readFile(MANIFEST_PATH, 'utf8')
    const json = JSON.parse(text)
    if (json && typeof json === 'object' && json.urlToLocal && typeof json.urlToLocal === 'object') return json
  } catch {
    // ignore
  }
  return { generatedAt: '', urlToLocal: {} }
}

async function main() {
  await ensureDir(TARGET_DIR)

  const urls = await collectUrls()
  console.log(`[scan] found ${urls.length} unique http(s) urls`)

  const existing = await readExistingManifest()
  const existingMap = existing.urlToLocal || {}

  const results = await runPool(
    urls,
    async (u) => {
      try {
        return await downloadOne(u, existingMap)
      } catch (e) {
        return { url: u, skipped: true, reason: e?.message || String(e) }
      }
    },
    6,
  )

  const urlToLocal = { ...existingMap }
  let downloaded = 0
  let skipped = 0
  const failures = []

  for (const r of results) {
    if (!r) continue
    if (r.relPath) urlToLocal[r.url] = r.relPath
    if (r.skipped) {
      skipped++
      if (r.reason && !String(r.reason).startsWith('not image') && r.reason !== 'already downloaded' && r.reason !== 'file exists') {
        failures.push({ url: r.url, reason: r.reason })
      }
    } else {
      downloaded++
    }
  }

  const manifest = {
    generatedAt: new Date().toISOString(),
    totalUrls: urls.length,
    downloaded,
    skipped,
    urlToLocal,
    failures,
  }
  await fs.writeFile(MANIFEST_PATH, JSON.stringify(manifest, null, 2), 'utf8')

  console.log(`[done] downloaded=${downloaded} skipped=${skipped}`)
  if (failures.length) {
    console.log(`[warn] ${failures.length} urls failed (see manifest failures)`)
  }
  console.log(`[manifest] ${path.relative(PROJECT_ROOT, MANIFEST_PATH)}`)
}

main().catch((e) => {
  console.error(e)
  process.exitCode = 1
})
