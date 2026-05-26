/** 本地排查管理员登录 Session（node scripts/test-admin-login.mjs） */
const base = process.env.API_BASE || 'http://127.0.0.1:5173/api'
let cookie = ''

async function req(method, path, body) {
  const res = await fetch(`${base}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(cookie ? { Cookie: cookie } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const setCookie = res.headers.get('set-cookie')
  if (setCookie) {
    const m = setCookie.match(/JSESSIONID=[^;]+/)
    if (m) cookie = m[0]
  }
  const text = await res.text()
  return { status: res.status, text }
}

const cap = await req('GET', '/admin/captcha')
console.log('captcha', cap.status, 'cookie:', cookie || '(none)')
const chars = [...cap.text.matchAll(/>([23456789A-Z])<\/text>/gi)].map((m) => m[1])
const code = chars.join('')
console.log('parsed captcha:', code || '(parse failed)')

const login = await req('POST', '/admin/login', {
  username: 'admin',
  password: 'admin123',
  captcha: code || 'XXXX',
  rememberMe: false,
})
console.log('login', login.status, login.text)

const sess = await req('GET', '/admin/session')
console.log('session after login', sess.text)
