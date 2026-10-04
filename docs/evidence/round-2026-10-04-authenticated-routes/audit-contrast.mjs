const { spawn } = require('node:child_process')
const c = spawn('C:\\Users\\jdsal\\AppData\\Local\\ms-playwright\\chromium-1187\\chrome-win\\chrome.exe',
  ['--headless=new','--disable-gpu','--hide-scrollbars','--remote-debugging-port=9344','--window-size=1440,900','--user-data-dir=C:\\Users\\jdsal\\AppData\\Local\\Temp\\opencode\\prof-full','about:blank'], {stdio:'ignore'})
const sleep = (ms)=>new Promise(r=>setTimeout(r,ms))
const fs = require('node:fs')

const audit = `(() => {
  document.documentElement.setAttribute('data-theme','dark')
  const lum=(c)=>{const m=c.match(/[\\d.]+/g);if(!m||m.length<3)return null;const [r,g,b]=m.slice(0,3).map(Number).map(v=>{v/=255;return v<=0.03928?v/12.92:Math.pow((v+0.055)/1.055,2.4)});return 0.2126*r+0.7152*g+0.0722*b}
  const ratio=(a,b)=>{const l1=lum(a),l2=lum(b);if(l1===null||l2===null)return null;return (Math.max(l1,l2)+0.05)/(Math.min(l1,l2)+0.05)}
  const eff=(el)=>{let n=el;while(n&&n!==document.documentElement){const cs=getComputedStyle(n);const bg=cs.backgroundColor;const m=bg.match(/[\\d.]+/g);if(m&&(m.length<4||Number(m[3])>0.5))return bg;n=n.parentElement}return'rgb(21,24,20)'}
  const bad=[]
  document.querySelectorAll('main *').forEach(el=>{const t=Array.from(el.childNodes).filter(n=>n.nodeType===3).map(n=>n.textContent.trim()).join('').trim();if(!t)return;const cs=getComputedStyle(el);if(cs.visibility==='hidden'||cs.display==='none')return;const r=ratio(cs.color,eff(el));if(r!==null&&r<4.5)bad.push({cls:(el.className||'').toString().slice(0,45),text:t.slice(0,28),color:cs.color,bg:eff(el),ratio:Number(r.toFixed(2))})})
  const tiny=[]
  document.querySelectorAll('main *').forEach(el=>{const t=Array.from(el.childNodes).filter(n=>n.nodeType===3).map(n=>n.textContent.trim()).join('').trim();if(!t)return;const fs2=parseFloat(getComputedStyle(el).fontSize);if(fs2<10)tiny.push({cls:(el.className||'').toString().slice(0,40),text:t.slice(0,25),size:fs2})})
  return { contraste: bad, fuentesPequenas: tiny.slice(0,8) }
})()`

;(async () => {
  await sleep(3500)
  const r = await fetch('http://127.0.0.1:9344/json/list'); const p = await r.json()
  const ws = new WebSocket(p[0].webSocketDebuggerUrl)
  const pending = new Map(); let id = 0
  ws.addEventListener('message', e => { const m = JSON.parse(e.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id) } })
  await new Promise(res => ws.addEventListener('open', res, {once:true}))
  const send = (method, params={}) => { const i = ++id; const pr = new Promise(r => pending.set(i, r)); ws.send(JSON.stringify({id:i,method,params})); return pr }
  await send('Page.enable'); await send('Runtime.enable')
  await send('Page.navigate', { url: 'http://localhost:5194/#avisos' })
  await sleep(2500)
  await send('Runtime.evaluate', { expression: `Array.from(document.querySelectorAll('button')).find(b=>b.textContent.includes('Entrar al preview local'))?.click()` })
  await sleep(4000)
  const report = []
  for (const route of ['avisos','avisos-admin','biblioteca']) {
    await send('Page.navigate', { url: `http://localhost:5194/#${route}` })
    await sleep(3200)
    const a = await send('Runtime.evaluate', { expression: audit, returnByValue: true })
    report.push({ route, ...a.result.value })
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(`C:\\Users\\jdsal\\AppData\\Local\\Temp\\opencode\\full-${route}.png`, Buffer.from(shot.data, 'base64'))
  }
  console.log(JSON.stringify(report, null, 1))
  ws.close(); c.kill(); process.exit(0)
})().catch(e => { console.error('error', e.message); c.kill(); process.exit(1) })
