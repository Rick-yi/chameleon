/**
 * chameleon 健康检查脚本
 *
 * 用法：node scripts/health-check.mjs [baseUrl]
 * 检查项：
 *   1. 后端存活（/doc.html 返回 200）
 *   2. 登录可用（机构编码 + 超管）
 *   3. 关键业务接口可用（学员/订单/报表/平台概览）
 *   4. 报表对账接口可用
 * 退出码：0 全部通过；1 存在失败项
 */
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '..', 'snowy-admin-web', 'package.json'))
const sm = require('sm-crypto')

const BASE = process.argv[2] || process.env.HEALTH_BASE_URL || 'http://127.0.0.1:82'
const TENANT = process.env.HEALTH_TENANT || 'demo'
const ACCOUNT = process.env.HEALTH_ACCOUNT || 'superAdmin'
const PASSWORD = process.env.HEALTH_PWD || 'Chameleon@2026!'
const PUBLIC_KEY = process.env.HEALTH_PUBLIC_KEY
	|| '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

let failed = 0
const check = (name, ok, extra = '') => {
	console.log(`${ok ? '[OK]  ' : '[FAIL]'} ${name} ${extra}`)
	if (!ok) failed++
}

const call = async (method, path, body, token, headers = {}) => {
	const started = Date.now()
	try {
		const res = await fetch(BASE + path, {
			method,
			headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}), ...headers },
			body: body ? JSON.stringify(body) : undefined,
			signal: AbortSignal.timeout(30000)
		})
		const text = await res.text()
		let json = null
		try {
			json = JSON.parse(text)
		} catch (e) {
			json = null
		}
		return { ms: Date.now() - started, status: res.status, json }
	} catch (e) {
		return { ms: Date.now() - started, status: 0, json: null, error: e.message }
	}
}

// /doc.html 在本部署中需要鉴权（返回 401），只要有响应即说明进程存活
const alive = await call('GET', '/doc.html')
check('后端存活', alive.status > 0, `status=${alive.status} ${alive.error || ''}`)

const login = await call('POST', '/auth/b/doLogin',
	{ account: ACCOUNT, password: sm.sm2.doEncrypt(PASSWORD, PUBLIC_KEY, 1) }, null, { 'X-Tenant-Code': TENANT })
const token = login.json?.data
check('登录可用', !!token, login.json?.msg || '')

if (token) {
	const apis = [
		['学员列表', '/student/page?current=1&size=5'],
		['订单列表', '/student/order/page?current=1&size=5'],
		['课时账户', '/student/lessonAccount/page?current=1&size=5'],
		['财务报表汇总', '/finance/report/summary'],
		['报表看板', '/finance/report/dashboard'],
		['课消与收入确认', '/finance/report/consume'],
		['教务报表', '/course/report/academic/attendance'],
		['招生报表', '/crm/report/funnel'],
		['报表对账', '/finance/report/reconcile'],
		['日汇总', '/finance/report/dailyStat']
	]
	for (const [name, path] of apis) {
		const res = await call('GET', path, null, token)
		check(`接口 ${name}`, res.json?.code === 200, `code=${res.json?.code} ${res.json?.msg || ''} ${res.ms}ms`)
	}
}

console.log(failed === 0 ? '\n全部健康检查通过' : `\n存在 ${failed} 项失败`)
process.exit(failed === 0 ? 0 : 1)
