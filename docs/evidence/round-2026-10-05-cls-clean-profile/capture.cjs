// Captura el fallback de carga y la pagina ya resuelta para revisar que la
// reserva de altura no deja un hueco roto. Escribe PNG en esta carpeta.
// Ralentiza la CPU para poder observar el fallback sin bloquear la red.
const { spawn } = require('node:child_process')
const { writeFileSync } = require('node:fs')
const path = require('node:path')

const CHROME = 'C:\\Users\\jdsal\\AppData\\Local\\ms-playwright\\chromium-1187\\chrome-win\\chrome.exe'
const PORT = 9382
const PROFILE = path.join(require('node:os').tmpdir(), 'opencode', 'cls-shot2')
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const here = __dirname

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
  const shot = async (file) => {
    const r = await send('Page.captureScreenshot', { format: 'png' })
    writeFileSync(path.join(here, file), Buffer.from(r.data, 'base64'))
    console.log('captura', file)
  }
  const evalJs = async (expression) =>
    (await send('Runtime.evaluate', { returnByValue: true, expression })).result.value
  const probe = `(() => {
    const fb = document.querySelector('.module-loading')
    const main = document.querySelector('#catalog-page-content')
    const footer = document.querySelector('.page-footer')
    return {
      fallback: fb ? { clase: fb.className, alto: Math.round(fb.getBoundingClientRect().height) } : null,
      contenido: main ? Math.round(main.getBoundingClientRect().height) : null,
      footerY: footer ? Math.round(footer.getBoundingClientRect().top) : null,
    }
  })()`

  await send('Page.enable')
  await send('Runtime.enable')
  await send('Emulation.setCPUThrottlingRate', { rate: 20 })
  await send('Page.navigate', { url: 'http://localhost:5173/#programas' })
  await sleep(2200)
  console.log('durante-carga', JSON.stringify(await evalJs(probe)))
  await shot('programas-fallback.png')

  await send('Emulation.setCPUThrottlingRate', { rate: 1 })
  await sleep(9000)
  console.log('resuelto', JSON.stringify(await evalJs(probe)))
  await shot('programas-resuelto.png')

  ws.close(); c.kill(); process.exit(0)
})().catch((e) => { console.error('error', e.message); c.kill(); process.exit(1) })
