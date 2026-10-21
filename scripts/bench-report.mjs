/**
 * 报表接口压测基线脚本
 *
 * 用法：
 *   node scripts/bench-report.mjs [baseUrl] [并发数] [每并发请求数]
 * 例：node scripts/bench-report.mjs http://127.0.0.1:82 8 20
 *
 * 说明：
 *   - 自动用默认账号登录（机构编码 demo / superAdmin），密码需与环境变量 REPORT_PWD 一致（默认 Chameleon@2026!）；
 *   - 压的是读接口（报表汇总/趋势/明细），输出 P50/P95/最大耗时与错误数；
 *   - 结果用于观察报表查询在并发下的响应基线，异常时先看数据库慢查询与 Redis 缓存命中情况。
 */
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const require = createRequire(resolve(here, '../snowy-admin-web/package.json'))
const sm = require('sm-crypto')

const BASE = process.argv[2] || 'http://127.0.0.1:82'
const CONCURRENCY = Number(process.argv[3] || 8)
const REQUESTS_PER_WORKER = Number(process.argv[4] || 20)
const TENANT = process.env.REPORT_TENANT || 'demo'
const ACCOUNT = process.env.REPORT_ACCOUNT || 'superAdmin'
const PASSWORD = process.env.REPORT_PWD || 'Chameleon@2026!'
const PUBLIC_KEY = process.env.REPORT_PUBLIC_KEY
	|| '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

const PATHS = [
	'/finance/report/summary',
	'/finance/report/trend?dimension=DAY',
	'/finance/report/courseRank?limit=10',
	'/finance/report/dashboard',
	'/course/report/academic/classFill',
	'/crm/report/funnel'
]

const call = async (path, token) => {
	const started = Date.now()
	const res = await fetch(BASE + path, { headers: token ? { token } : {} })
	const text = await res.text()
	return { ms: Date.now() - started, ok: res.ok, code: JSON.parse(text || '{}').code }
}

const loginRes = await fetch(BASE + '/auth/b/doLogin', {
	method: 'POST',
	headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': TENANT },
	body: JSON.stringify({ account: ACCOUNT, password: sm.sm2.doEncrypt(PASSWORD, PUBLIC_KEY, 1) })
})
const token = JSON.parse(await loginRes.text()).data
if (!token) {
	console.error('登录失败，请检查 REPORT_PWD / 机构编码')
	process.exit(1)
}

// 预热（触发缓存与 JIT）
for (const path of PATHS) {
	await call(path, token)
}

const results = []
const errors = []
const workers = Array.from({ length: CONCURRENCY }, async () => {
	for (let i = 0; i < REQUESTS_PER_WORKER; i++) {
		const path = PATHS[Math.floor(Math.random() * PATHS.length)]
		const res = await call(path, token)
		results.push(res.ms)
		if (!res.ok || (res.code !== undefined && res.code !== 200)) {
			errors.push({ path, ...res })
		}
	}
})
await Promise.all(workers)

results.sort((a, b) => a - b)
const pct = (p) => results[Math.min(results.length - 1, Math.floor(results.length * p))]
console.log(`目标：${BASE}  并发：${CONCURRENCY}  每并发请求：${REQUESTS_PER_WORKER}  总请求：${results.length}`)
console.log(`P50=${pct(0.5)}ms  P95=${pct(0.95)}ms  最大=${results[results.length - 1]}ms  错误=${errors.length}`)
if (errors.length) {
	console.log('错误样例：', errors.slice(0, 5))
	process.exit(1)
}
