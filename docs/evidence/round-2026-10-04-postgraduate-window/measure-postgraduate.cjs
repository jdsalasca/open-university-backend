const { spawn } = require('node:child_process')
const fs = require('node:fs')
const c = spawn('C:\\Users\\jdsal\\AppData\\Local\\ms-playwright\\chromium-1187\\chrome-win\\chrome.exe',
  ['--headless=new','--disable-gpu','--hide-scrollbars','--remote-debugging-port=9371','--window-size=1440,900','--user-data-dir=C:\\Users\\jdsal\\AppData\\Local\\Temp\\opencode\\prof-post2','about:blank'], {stdio:'ignore'})
const sleep = (ms)=>new Promise(r=>setTimeout(r,ms))
;(async () => {
  await sleep(3500)
  const r = await fetch('http://127.0.0.1:9371/json/list'); const p = await r.json()
  const ws = new WebSocket(p[0].webSocketDebuggerUrl)
  const pending = new Map(); let id = 0
  ws.addEventListener('message', e => { const m = JSON.parse(e.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id) } })
  await new Promise(res => ws.addEventListener('open', res, {once:true}))
  const send = (method, params={}) => { const i = ++id; const pr = new Promise(r => pending.set(i, r)); ws.send(JSON.stringify({id:i,method,params})); return pr }
  await send('Page.enable'); await send('Runtime.enable')
  await send('Page.addScriptToEvaluateOnNewDocument', { source: `window.__d=[]
    new PerformanceObserver((l)=>{for(const e of l.getEntries()){if(e.hadRecentInput)continue
      window.__d.push({v:Number(e.value.toFixed(4)),
        src:(e.sources||[]).map(s=>(s.node?(s.node.className||s.node.tagName||'').toString().slice(0,40):'?'))})}}).observe({type:'layout-shift',buffered:true})` })
  await send('Page.navigate', { url: 'http://localhost:5196/#programas' })
  await sleep(13000)
  // Cambia a posgrado y deja que renderice.
  await send('Runtime.evaluate', { expression: `Array.from(document.querySelectorAll('.public-program-directory-switch button')).find(b=>b.textContent.trim()==='Posgrado')?.click()` })
  await sleep(9000)
  const a = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    const dir = document.querySelector('.public-program-directory')
    return {
      clsSwitch: Number(window.__d.reduce((x,y)=>x+y.v,0).toFixed(4)),
      shifts: window.__d.sort((x,y)=>y.v-x.v).slice(0,4),
      dirHeight: dir ? Math.round(dir.getBoundingClientRect().height) : null,
      cards: document.querySelectorAll('.public-program-card').length,
      more: Boolean(document.querySelector('.public-program-more')),
      totalHeight: Math.round(document.documentElement.scrollHeight),
    }
  })()` })
  console.log(JSON.stringify(a.result.value, null, 1))
  const s = await send('Page.captureScreenshot', { format: 'png' })
  fs.writeFileSync('C:\\Users\\jdsal\\AppData\\Local\\Temp\\opencode\\post-before.png', Buffer.from(s.data, 'base64'))
  ws.close(); c.kill(); process.exit(0)
})().catch(e => { console.error('error', e.message); c.kill(); process.exit(1) })

