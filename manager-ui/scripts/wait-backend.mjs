/**
 * 启动 Vite 前等待本机后端就绪
 * --strict：超时则退出，避免只开前端导致 API 502
 */
import http from 'node:http'

const STRICT = process.argv.includes('--strict') || process.env.VITE_WAIT_BACKEND_STRICT === '1'
const HOST = process.env.VITE_PROXY_HOST || '127.0.0.1'
const PORT = Number(process.env.VITE_PROXY_PORT || 8080)
const CTX = (process.env.VITE_BACKEND_CONTEXT || '/Onlinebookshop_Manager').replace(/\/$/, '')
const MAX_WAIT_MS = Number(process.env.VITE_WAIT_BACKEND_MS || 180000)
const INTERVAL_MS = 2000

const PATHS = [
  `${CTX}/api/portal/categories`,
  `${CTX}/api/admin/captcha`,
]

function probe(path) {
  return new Promise((resolve) => {
    const req = http.get({ host: HOST, port: PORT, path, timeout: 3000 }, (res) => {
      res.resume()
      resolve(res.statusCode && res.statusCode >= 200 && res.statusCode < 500)
    })
    req.on('error', () => resolve(false))
    req.on('timeout', () => {
      req.destroy()
      resolve(false)
    })
  })
}

async function backendReady() {
  for (const path of PATHS) {
    if (await probe(path)) return path
  }
  return null
}

async function main() {
  const start = Date.now()
  console.log(`[wait-backend] 等待 http://${HOST}:${PORT} （最多 ${MAX_WAIT_MS / 1000}s）…`)

  while (Date.now() - start < MAX_WAIT_MS) {
    const ok = await backendReady()
    if (ok) {
      console.log(`[wait-backend] 后端就绪: ${ok}`)
      return
    }
    await new Promise((r) => setTimeout(r, INTERVAL_MS))
  }

  const msg = [
    '[wait-backend] 超时：8080 上没有可用的后端 API。',
    '  1) IDEA 运行「启动后端 SpringBoot」或「Tomcat 9.0.93」',
    '  2) 或运行 scripts\\检查端口.bat 查看 8080 是否在监听',
    '  3) 确认 MySQL 3306 已启动并已导入 sql/bookshop_admin.sql',
  ].join('\n')

  if (STRICT) {
    console.error(msg)
    process.exit(1)
  }
  console.warn(msg)
  console.warn('[wait-backend] 仍将启动 Vite，页面可能出现 HTTP 502')
}

await main()
