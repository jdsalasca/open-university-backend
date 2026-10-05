// Corrida de CLS con perfil de navegador NUEVO en cada ejecucion.
// El script anterior reutilizaba un --user-data-dir fijo, de modo que la segunda
// y tercera corrida heredaban la cache del bundle y no reproducian el salto de
// una visita sin cache. Cada corrida borra su perfil y usa puerto CDP propio.
const { spawn } = require('node:child_process')
const { rmSync } = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const CHROME = 'C:\\Users\\jdsal\\AppData\\Local\\ms-playwright\\chromium-1187\\chrome-win\\chrome.exe'
const APP = process.env.CLS_URL || 'http://localhost:5173/#programas'
const RUNS = Number(process.env.CLS_RUNS || 3)
const PORT = Number(process.env.CLS_CDP_PORT || 9360)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// ponytail: perfil unico por corrida basta para el aislamiento que nos importa;
// no hace falta limpiar el resto de artefactos de Chromium.
const dropProfile = (dir) => {
  try {
    rmSync(dir, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 })
  } catch {
    // Chromium puede seguir sujetando el directorio un instante; el perfil vive
    // en Temp y el nombre es unico por corrida, asi que no compromete la limpieza.
  }
}

async function runOnce(index) {
  const profile = path.join(os.tmpdir(), 'opencode', `cls-${process.pid}-${index}`)
  dropProfile(profile)
  const c = spawn(
    CHROME,
    [
      '--headless=new',
      '--disable-gpu',
      '--hide-scrollbars',
      `--remote-debugging-port=${PORT + index}`,
      '--window-size=1440,900',
      `--user-data-dir=${profile}`,
      'about:blank',
    ],
    { stdio: 'ignore' },
  )
  try {
    await sleep(3500)
    const list = await (await fetch(`http://127.0.0.1:${PORT + index}/json/list`)).json()
    const ws = new WebSocket(list[0].webSocketDebuggerUrl)
    const pending = new Map()
    let id = 0
    ws.addEventListener('message', (e) => {
      const m = JSON.parse(e.data)
      if (m.id && pending.has(m.id)) {
        pending.get(m.id)(m.result)
        pending.delete(m.id)
      }
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
    await send('Page.addScriptToEvaluateOnNewDocument', {
      source: `window.__d=[]
        new PerformanceObserver((l)=>{for(const e of l.getEntries()){if(e.hadRecentInput)continue
          window.__d.push({v:Number(e.value.toFixed(4)),t:Math.round(e.startTime),
            src:(e.sources||[]).map(s=>(s.node?(s.node.className||s.node.tagName||'').toString().slice(0,40):'?'))})}}).observe({type:'layout-shift',buffered:true})`,
    })
    await send('Page.navigate', { url: APP })
    await sleep(12000)
    const out = await send('Runtime.evaluate', {
      returnByValue: true,
      expression: `({ cls: Number(window.__d.reduce((a,b)=>a+b.v,0).toFixed(4)), shifts: window.__d })`,
    })
    ws.close()
    return out.result.value
  } finally {
    c.kill()
    await sleep(500)
    dropProfile(profile)
  }
}

;(async () => {
  const rows = []
  for (let i = 1; i <= RUNS; i++) {
    const r = await runOnce(i)
    rows.push({ corrida: i, cls: r.cls, fuentes: r.shifts.filter((s) => s.v > 0.001) })
  }
  console.log(JSON.stringify(rows, null, 1))
  process.exit(0)
})().catch((e) => {
  console.error('error', e.message)
  process.exit(1)
})
