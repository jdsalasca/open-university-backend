// Mide la altura real del contenido de /#programas y el peso del chunk de la
// pagina, para decidir entre reservar altura en el fallback o quitar el lazy.
const { spawn } = require('node:child_process')
const { existsSync, readdirSync, statSync } = require('node:fs')
const path = require('node:path')

const CHROME = 'C:\\Users\\jdsal\\AppData\\Local\\ms-playwright\\chromium-1187\\chrome-win\\chrome.exe'
const PORT = 9371
const PROFILE = path.join(require('node:os').tmpdir(), 'opencode', 'cls-shape')
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const c = spawn(
  CHROME,
  ['--headless=new', '--disable-gpu', '--hide-scrollbars', `--remote-debugging-port=${PORT}`,
   '--window-size=1440,900', `--user-data-dir=${PROFILE}`, 'about:blank'],
  { stdio: 'ignore' },
)

;(async () => {
  await sleep(3500)
  const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
  const ws = new WebSocket(list[0].webSocketDebuggerUrl)
  const pending = new Map()
  let id = 0
  ws.addEventListener('message', (e) => {
    const m = JSON.parse(e.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id) }
  })
  await new Promise((res) => ws.addEventListener('open', res, { once: true }))
  const send = (method, params = {}) => {
    const i = ++id
    const pr = new Promise((r) => pending.set(i, r))
    ws.send(JSON.stringify({ id: i, method, params }))
    return pr
  }
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Page.navigate', { url: 'http://localhost:5173/#programas' })
  await sleep(9000)
  const out = await send('Runtime.evaluate', {
    returnByValue: true,
    expression: `(() => {
      const main = document.querySelector('#catalog-page-content') || document.querySelector('main')
      const footer = document.querySelector('.page-footer')
      return {
        altoContenido: main ? Math.round(main.getBoundingClientRect().height) : null,
        altoMain: main ? Math.round(main.scrollHeight) : null,
        altoFooter: footer ? Math.round(footer.getBoundingClientRect().height) : null,
        altoBody: document.body.scrollHeight,
      }
    })()`,
  })
  console.log('MEDICION', JSON.stringify(out.result.value))
  ws.close(); c.kill()

  const dist = path.resolve(__dirname, '..', '..', '..', 'frontend', 'dist')
  if (existsSync(dist)) {
    const assets = path.join(dist, 'assets')
    const big = readdirSync(assets)
      .filter((f) => f.endsWith('.js'))
      .map((f) => ({ f, kb: Math.round(statSync(path.join(assets, f)).size / 1024) }))
      .sort((a, b) => b.kb - a.kb)
      .slice(0, 6)
    console.log('ASSETS', JSON.stringify(big))
  } else {
    console.log('ASSETS sin dist; ejecuta npm run build primero')
  }
  process.exit(0)
})().catch((e) => { console.error('error', e.message); c.kill(); process.exit(1) })
