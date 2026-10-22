import { spawn } from 'node:child_process'
import { writeFileSync, mkdirSync } from 'node:fs'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const BASE = 'http://localhost:81'
const SHOT_DIR = 'D:/IdeaProject/chameleon/gui-test-screenshots/btnperm'
mkdirSync(SHOT_DIR, { recursive: true })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const TENANT = 'demo'

const chrome = spawn(CHROME, ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--remote-debugging-port=9455', '--user-data-dir=D:/IdeaProject/chameleon/.jdk/chrome-profile-btn', '--window-size=1600,1000', 'about:blank'], { stdio: 'ignore' })
let ws, msgId = 0
const pending = new Map()
const send = (method, params = {}) => new Promise((resolve, reject) => { const id = ++msgId; pending.set(id, { resolve, reject }); ws.send(JSON.stringify({ id, method, params })) })
const evaluate = async (e) => { const r = await send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) return 'EXC'; return r.result?.value }
const clickAt = async (x, y) => { await send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 }); await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 }) }
const typeInto = async (ph, val) => {
	const b = await evaluate(`(() => { const el = Array.from(document.querySelectorAll('input')).find(i => (i.placeholder||'').includes(${JSON.stringify(ph)})); if(!el) return null; el.focus(); const r = el.getBoundingClientRect(); return { x:r.left+r.width/2, y:r.top+r.height/2 } })()`)
	if (!b) return false
	await clickAt(b.x, b.y)
	await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers: 2 })
	await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers: 2 })
	await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Delete', code: 'Delete', windowsVirtualKeyCode: 46 })
	await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Delete', code: 'Delete', windowsVirtualKeyCode: 46 })
	await sleep(150); await send('Input.insertText', { text: val }); await sleep(250); return true
}
const clickText = async (t) => {
	const f = await evaluate(`(() => { const h = Array.from(document.querySelectorAll('span,div,a,li,button')).filter(el => { if ((el.textContent||'').trim() !== ${JSON.stringify(t)}) return false; const r = el.getBoundingClientRect(); if (r.width<=0||r.height<=0) return false; return true }); if(!h.length) return null; const r = h[h.length-1].getBoundingClientRect(); return { x:r.left+r.width/2, y:r.top+r.height/2 } })()`)
	if (!f) return false
	await clickAt(f.x, f.y); await sleep(1500); return true
}
const shot = async (name) => {
	const r = await send('Page.captureScreenshot', { format: 'png' })
	writeFileSync(`${SHOT_DIR}/${name}.png`, Buffer.from(r.data, 'base64'))
}

/** 页面上的按钮文案 */
const buttonsOf = () =>
	evaluate(`(() => Array.from(document.querySelectorAll('.ant-btn, a')).map(e => (e.textContent||'').trim()).filter(t => t && t.length < 8).join('|'))()`)

try {
	let target = null
	for (let i = 0; i < 20 && !target; i++) { await sleep(1000); try { const l = await (await fetch('http://127.0.0.1:9455/json/list')).json(); target = l.find((t) => t.type === 'page') } catch (e) {} }
	ws = new WebSocket(target.webSocketDebuggerUrl)
	await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej })
	ws.onmessage = (e) => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { const p = pending.get(m.id); pending.delete(m.id); m.error ? p.reject(new Error(m.error.message)) : p.resolve(m.result) } }
	await send('Page.enable'); await send('Runtime.enable')

	const loginAs = async (account) => {
		await send('Page.navigate', { url: BASE + '/login' }); await sleep(2000)
		await evaluate("(() => { localStorage.clear(); sessionStorage.clear(); return true })()")
		await send('Page.navigate', { url: BASE + '/login' }); await sleep(3000)
		await typeInto('机构编码', TENANT)
		await typeInto('账号', account)
		await typeInto('密码', 'Chameleon@2026!')
		await clickText('登 录')
		await sleep(7000)
	}

	// 老师：学员管理页不应出现「新增学员」，点名页应有「确认点名」
	await loginAs('teacher01')
	await send('Page.navigate', { url: BASE + '/student' }); await sleep(3500)
	let btns = await buttonsOf()
	console.log('老师/学员管理 页按钮：', btns)
	console.log('  不含「新增学员」：', !btns.includes('新增学员') ? 'PASS' : 'FAIL')
	await shot('老师-学员管理')

	await send('Page.navigate', { url: BASE + '/course/attend' }); await sleep(3500)
	btns = await buttonsOf()
	console.log('老师/点名消课 页按钮：', btns)
	console.log('  含「确认点名并扣课」：', btns.includes('确认点名并扣课') ? 'PASS' : 'FAIL')

	// 顾问：招生线索页应有「新增线索」，收款台不应有「审批」
	await loginAs('sales01')
	await send('Page.navigate', { url: BASE + '/crm' }); await sleep(3500)
	btns = await buttonsOf()
	console.log('顾问/招生线索 页按钮：', btns)
	console.log('  含「新增线索」：', btns.includes('新增线索') ? 'PASS' : 'FAIL')
	await shot('顾问-招生线索')

	// 前台：收款台应有「收款」，不应有「新增学员」类按钮
	await loginAs('cashier01')
	await send('Page.navigate', { url: BASE + '/student/order' }); await sleep(3500)
	btns = await buttonsOf()
	console.log('前台/收款台 页按钮：', btns)
	console.log('  含「收款」：', btns.includes('收款') ? 'PASS' : 'FAIL')
	await shot('前台-收款台')

	// 管理员：所有按钮都在
	await loginAs('superAdmin')
	await send('Page.navigate', { url: BASE + '/student' }); await sleep(3500)
	btns = await buttonsOf()
	console.log('管理员/学员管理 页按钮：', btns)
	console.log('  含「新增学员」：', btns.includes('新增学员') ? 'PASS' : 'FAIL')
} catch (e) { console.error('异常：', e.message) } finally { try { ws?.close() } catch (e) {} chrome.kill() }
