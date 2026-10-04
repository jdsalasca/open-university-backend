const { spawn } = require('node:child_process')
const c = spawn('C:\\Users\\jdsal\\AppData\\Local\\ms-playwright\\chromium-1187\\chrome-win\\chrome.exe',
  ['--headless=new','--disable-gpu','--hide-scrollbars','--remote-debugging-port=9355','--window-size=1440,900','--user-data-dir=C:\\Users\\jdsal\\AppData\\Local\\Temp\\opencode\\prof-fin','about:blank'], {stdio:'ignore'})
const sleep = (ms)=>new Promise(r=>setTimeout(r,ms))
;(async () => {
  await sleep(3500)
  const r = await fetch('http://127.0.0.1:9355/json/list'); const p = await r.json()
  const ws = new WebSocket(p[0].webSocketDebuggerUrl)
  const pending = new Map(); let id = 0
  ws.addEventListener('message', e => { const m = JSON.parse(e.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id) } })
  await new Promise(res => ws.addEventListener('open', res, {once:true}))
  const send = (method, params={}) => { const i = ++id; const pr = new Promise(r => pending.set(i, r)); ws.send(JSON.stringify({id:i,method,params})); return pr }
  await send('Page.enable'); await send('Runtime.enable')
  await send('Page.addScriptToEvaluateOnNewDocument', { source: `window.__d=[]
    new PerformanceObserver((l)=>{for(const e of l.getEntries()){if(e.hadRecentInput)continue
      window.__d.push({v:Number(e.value.toFixed(4)),t:Math.round(e.startTime),
        src:(e.sources||[]).map(s=>(s.node?(s.node.className||s.node.tagName||'').toString().slice(0,40):'?')),
        prev:e.sources?.[0]?.previousRect?{y:Math.round(s.previousRect.y),h:Math.round(s.previousRect.height)}:null,
        cur:e.sources?.[0]?.currentRect?{y:Math.round(s.currentRect.y),h:Math.round(s.currentRect.height)}:null})}}).observe({type:'layout-shift',buffered:true})` })
  await send('Page.navigate', { url: 'http://localhost:5195/#programas' })
  await sleep(12000)
  const out = await send('Runtime.evaluate', { returnByValue: true, expression: `({ cls: Number(window.__d.reduce((a,b)=>a+b.v,0).toFixed(4)), shifts: window.__d })` })
  console.log(JSON.stringify(out.result.value, null, 1))
  ws.close(); c.kill(); process.exit(0)
})().catch(e => { console.error('error', e.message); c.kill(); process.exit(1) })
