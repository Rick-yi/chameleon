/**
 * 报表接口压测基线脚本
 *
 * 用法：
 *   node scripts/bench-report.mjs [baseUrl] [并发档位,逗号分隔]
 * 例：node scripts/bench-report.mjs http://127.0.0.1:82 1,4,8,16
 *
 * 输出：每个并发档位的 P50/P95/最大耗时与错误数，用于观察报表查询的性能基线。
 */
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '..', 'snowy-admin-web', 'package.json'))
const sm = require('sm-crypto')

const BASE = process.argv[2] || 'http://127.0.0.1:82'
const LEVELS = (process.argv[3] || '1,4,8').split(',').map((n) => Number(n)).filter((n) => n > 0)
const REQUESTS_PER_WORKER = Number(process.env.BENCH_REQUESTS || 20)
const TENANT = process.env.BENCH_TENANT || 'demo'
const ACCOUNT = process.env.BENCH_ACCOUNT || 'superAdmin'
const PASSWORD = process.env.BENCH_PWD || 'Chameleon@2026!'
const PUBLIC_KEY = process.env.BENCH_PUBLIC_KEY
	|| '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

// 覆盖读接口：报表汇总/趋势/排行/看板/明细/教务/招生
const PATHS = [
	'/finance/report/summary',
	'/finance/report/trend?dimension=DAY',
	'/finance/report/courseRank?limit=10',
	'/finance/report/campusRank',
	'/finance/report/consume',
	'/finance/report/dashboard',
	'/finance/payment/page?current=1&size=20',
	'/finance/refund/page?current=1&size=20',
	'/course/report/academic/attendance',
	'/course/report/academic/classFill',
	'/course/report/academic/teacherWorkload',
	'/crm/report/funnel',
	'/crm/report/source'
]

const call = async (path, token) => {
	const started = Date.now()
	try {
		const res = await fetch(BASE + path, { headers: token ? { token } : {}, signal: AbortSignal.timeout(20000) })
		const text = await res.text()
		let code = null
		try {
			code = JSON.parse(text).code
		} catch (e) {
			code = null
		}
		return { ms: Date.now() - started, ok: res.ok && code === 200 }
	} catch (e) {
		return { ms: Date.now() - started, ok: false, error: e.message }
	}
}

const loginRes = await fetch(BASE + '/auth/b/doLogin', {
	method: 'POST',
	headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': TENANT },
	body: JSON.stringify({ account: ACCOUNT, password: sm.sm2.doEncrypt(PASSWORD, PUBLIC_KEY, 1) })
})
const token = JSON.parse(await loginRes.text()).data
if (!token) {
	console.error('登录失败，请检查账号密码与机构编码')
	process.exit(1)
}

// 预热（触发缓存与 JIT）
for (const path of PATHS) {
	await call(path, token)
}

console.log(`目标：${BASE}  接口数：${PATHS.length}  每并发请求：${REQUESTS_PER_WORKER}`)
const summary = []
for (const level of LEVELS) {
	const results = []
	const errors = []
	await Promise.all(Array.from({ length: level }, async () => {
		for (let i = 0; i < REQUESTS_PER_WORKER; i++) {
			const path = PATHS[Math.floor(Math.random() * PATHS.length)]
			const res = await call(path, token)
			results.push(res.ms)
			if (!res.ok) errors.push({ path, ...res })
		}
	}))
	results.sort((a, b) => a - b)
	const pct = (p) => results[Math.min(results.length - 1, Math.floor(results.length * p))]
	const row = {
		并发: level,
		请求数: results.length,
		P50: `${pct(0.5)}ms`,
		P95: `${pct(0.95)}ms`,
		最大: `${results[results.length - 1]}ms`,
		错误: errors.length
	}
	summary.push(row)
	console.log(`  并发 ${String(level).padStart(2)} | 请求 ${row.请求数} | P50 ${row.P50} | P95 ${row.P95} | 最大 ${row.最大} | 错误 ${row.错误}`)
	if (errors.length) {
		console.log('    错误样例：', errors.slice(0, 3))
	}
}
console.log('\n基线汇总：')
console.table(summary)
