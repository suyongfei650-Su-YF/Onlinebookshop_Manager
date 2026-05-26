/** node scripts/test-user-login.mjs */
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
  return { status: res.status, text: await res.text() }
}

const cap = await req('GET', '/user/captcha')
const code = [...cap.text.matchAll(/>([23456789A-Z])<\/text>/gi)].map((m) => m[1]).join('')
console.log('captcha', code, 'cookie', cookie ? 'ok' : 'missing')

for (const account of ['zhangsan', 'zhangsan@example.com', '13800009021']) {
  const cap2 = await req('GET', '/user/captcha')
  const code2 = [...cap2.text.matchAll(/>([23456789A-Z])<\/text>/gi)].map((m) => m[1]).join('')
  const login = await req('POST', '/user/login', {
    account,
    password: '123456',
    captcha: code2,
  })
  console.log('login', account, login.status, login.text.slice(0, 100))
}

const me = await req('GET', '/user/me')
console.log('me', me.status, me.text.slice(0, 120))
