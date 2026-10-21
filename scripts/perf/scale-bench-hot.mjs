import { createRequire } from 'node:module'
const require = createRequire('D:/IdeaProject/chameleon/snowy-admin-web/package.json')
const sm = require('sm-crypto')

const BASE = 'http://127.0.0.1:82'
const KEY = '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

const call = async (method, path, body, token, tenant) => {
	const start = Date.now()
	const res = await fetch(BASE + path, {
		method,
		headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}), ...(tenant ? { 'X-Tenant-Code': tenant } : {}) },
		body: body ? JSON.stringify(body) : undefined
	})
	const text = await res.text()
	const cost = Date.now() - start
	let json = null
	try { json = JSON.parse(text) } catch (e) {}
	return { cost, json }
}

const HOT = [
	['教务课消进度', '/course/report/academic/lessonProgress'],
	['财务看板', '/finance/report/dashboard'],
	['财务校区', '/finance/report/campusRank'],
	['学员列表', '/student/page?current=1&size=10'],
	['课时流水', '/student/lessonFlow/page?current=1&size=10'],
	['排课周视图', '/course/schedule/week?startDate=2026-10-19&endDate=2026-10-25'],
	['财务概览', '/finance/report/summary'],
	['埋点统计', '/biz/event/summary']
]

const tenant = process.argv[2] || 'scale01'
const RUNS = Number(process.argv[3] || 8)

const login = await call('POST', '/auth/b/doLogin', { account: 'admin', password: sm.sm2.doEncrypt('Chameleon@2026!', KEY, 1) }, null, tenant)
const token = login.json?.data
if (!token) { console.error('登录失败', login.json?.msg); process.exit(1) }

console.log(`租户 ${tenant}：${RUNS} 次连测（含首次预热）`)
for (const [name, path] of HOT) {
	const costs = []
	let code = null
	let msg = ''
	for (let i = 0; i < RUNS; i++) {
		const r = await call('GET', path, null, token, tenant)
		costs.push(r.cost)
		code = r.json?.code
		if (code !== 200) msg = r.json?.msg
	}
	const warm = costs.slice(1)
	const sorted = [...warm].sort((a, b) => a - b)
	const p50 = sorted[Math.floor(sorted.length * 0.5)]
	const p95 = sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * 0.95))]
	console.log(`${name.padEnd(12)} 首调用=${String(costs[0]).padStart(5)}ms  稳态p50=${String(p50).padStart(5)}ms  p95=${String(p95).padStart(5)}ms  max=${String(sorted[sorted.length-1]).padStart(5)}ms  ${code !== 200 ? 'ERR:' + msg : ''}`)
}
