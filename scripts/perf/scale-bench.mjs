import { createRequire } from 'node:module'
const require = createRequire('D:/IdeaProject/chameleon/snowy-admin-web/package.json')
const sm = require('sm-crypto')

const BASE = 'http://127.0.0.1:82'
const KEY = '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

const call = async (method, path, body, token, tenant) => {
	const start = Date.now()
	const res = await fetch(BASE + path, {
		method,
		headers: {
			'Content-Type': 'application/json',
			...(token ? { token } : {}),
			...(tenant ? { 'X-Tenant-Code': tenant } : {})
		},
		body: body ? JSON.stringify(body) : undefined
	})
	const text = await res.text()
	const cost = Date.now() - start
	let json = null
	try {
		json = JSON.parse(text)
	} catch (e) {}
	return { cost, status: res.status, json }
}

// 关键接口：覆盖学员、收款、课表、教务、财务、招生、埋点、自动化
const CASES = [
	['学员列表(首页)', 'GET', '/student/page?current=1&size=10'],
	['学员搜索', 'GET', '/student/page?current=1&size=10&searchKey=%E5%BC%A0'],
	['学员深分页(第80页)', 'GET', '/student/page?current=80&size=10'],
	['学员状态筛选', 'GET', '/student/page?current=1&size=10&status=STUDYING'],
	['订单列表', 'GET', '/student/order/page?current=1&size=10'],
	['订单状态筛选', 'GET', '/student/order/page?current=1&size=10&status=PAID'],
	['退费单列表', 'GET', '/student/refund/page?current=1&size=10'],
	['课时账户列表', 'GET', '/student/lessonAccount/page?current=1&size=10'],
	['课时流水', 'GET', '/student/lessonFlow/page?current=1&size=10'],
	['续费预警列表', 'GET', '/student/renewWarn/page?current=1&size=10'],
	['线索列表', 'GET', '/crm/lead/page?current=1&size=10'],
	['跟进记录', 'GET', '/crm/follow/page?current=1&size=10'],
	['试听列表', 'GET', '/crm/trial/page?current=1&size=10'],
	['课程列表', 'GET', '/course/page?current=1&size=10'],
	['课包列表', 'GET', '/course/package/page?current=1&size=10'],
	['班级列表', 'GET', '/course/class/page?current=1&size=10'],
	['班级学员', 'GET', '/course/classStudent/page?current=1&size=10'],
	['排课周视图', 'GET', '/course/schedule/week?startDate=2026-10-19&endDate=2026-10-25'],
	['排课分页', 'GET', '/course/schedule/page?current=1&size=10'],
	['点名记录', 'GET', '/course/attend/page?current=1&size=10'],
	['调课记录', 'GET', '/course/scheduleChange/page?current=1&size=10'],
	['财务概览', 'GET', '/finance/report/summary'],
	['财务看板', 'GET', '/finance/report/dashboard'],
	['财务趋势', 'GET', '/finance/report/trend'],
	['财务渠道', 'GET', '/finance/report/channel'],
	['财务排行', 'GET', '/finance/report/courseRank'],
	['财务校区', 'GET', '/finance/report/campusRank'],
	['财务课消', 'GET', '/finance/report/attendTrend'],
	['财务校区选项', 'GET', '/finance/report/campusOptions'],
	['教务出勤', 'GET', '/course/report/academic/attendance'],
	['教务满班率', 'GET', '/course/report/academic/classFill'],
	['教务教师负荷', 'GET', '/course/report/academic/teacherWorkload'],
	['教务课消进度', 'GET', '/course/report/academic/lessonProgress'],
	['教务风险学员', 'GET', '/course/report/academic/riskStudents'],
	['招生漏斗', 'GET', '/crm/report/funnel'],
	['招生来源', 'GET', '/crm/report/source'],
	['招生跟进排行', 'GET', '/crm/report/followRank'],
	['招生试听', 'GET', '/crm/report/trial'],
	['埋点统计', 'GET', '/biz/event/summary'],
	['埋点流水(首页)', 'GET', '/biz/event/page?current=1&size=10'],
	['埋点流水(深分页)', 'GET', '/biz/event/page?current=150&size=10'],
	['埋点码字典', 'GET', '/biz/event/codes'],
	['自动化规则', 'GET', '/biz/automation/rule'],
	['自动化记录', 'GET', '/biz/automation/log?current=1&size=20'],
	['校区可选项', 'GET', '/course/class/campusOptions'],
	['教师可选项', 'GET', '/course/schedule/teacherOptions'],
	['学员总数(统计)', 'GET', '/student/page?current=1&size=1']
]

const TENANTS = process.argv[2] ? [process.argv[2]] : ['scale01', 'scale05', 'scale10']
const RUNS = Number(process.argv[3] || 3)

const login = async (tenant) => {
	const res = await call('POST', '/auth/b/doLogin', { account: 'admin', password: sm.sm2.doEncrypt('Chameleon@2026!', KEY, 1) }, null, tenant)
	return res.json?.data
}

const stats = (arr) => {
	const sorted = [...arr].sort((a, b) => a - b)
	const p = (q) => sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * q))]
	return { min: sorted[0], p50: p(0.5), p95: p(0.95), max: sorted[sorted.length - 1] }
}

for (const tenant of TENANTS) {
	const token = await login(tenant)
	if (!token) {
		console.log(`\n===== ${tenant} 登录失败`)
		continue
	}
	console.log(`\n===== 租户 ${tenant}（800 学员 / 1000 订单 / 2 万埋点）`)
	const rows = []
	for (const [name, method, path] of CASES) {
		const costs = []
		let code = null
		let err = ''
		for (let i = 0; i < RUNS; i++) {
			const r = await call(method, path, null, token, tenant)
			costs.push(r.cost)
			code = r.json?.code
			if (r.json?.code && r.json.code !== 200) err = r.json.msg || ''
		}
		const s = stats(costs)
		rows.push({ name, ...s, code, err })
	}
	rows.sort((a, b) => b.p95 - a.p95)
	console.log('接口'.padEnd(22) + 'p50(ms)'.padStart(9) + 'p95(ms)'.padStart(9) + 'max(ms)'.padStart(9) + '  结果')
	for (const r of rows) {
		const flag = r.code !== 200 ? 'ERR:' + (r.err || r.code) : r.p95 > 1000 ? '慢' : ''
		console.log(r.name.padEnd(20) + String(r.p50).padStart(9) + String(r.p95).padStart(9) + String(r.max).padStart(9) + '  ' + flag)
	}
	const slow = rows.filter((r) => r.p95 > 500)
	console.log(`\n慢接口（p95 > 500ms）：${slow.length} 个 -> ${slow.map((r) => r.name).join(', ') || '无'}`)
}
