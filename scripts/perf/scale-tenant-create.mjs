import { createRequire } from 'node:module'
const require = createRequire(process.cwd() + '/snowy-admin-web/package.json')
const sm = require('sm-crypto')

const BASE = 'http://127.0.0.1:82'
const KEY = '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

const call = async (method, path, body, token, tenantCode) => {
	const res = await fetch(BASE + path, {
		method,
		headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}), ...(tenantCode ? { 'X-Tenant-Code': tenantCode } : {}) },
		body: body ? JSON.stringify(body) : undefined
	})
	const text = await res.text()
	try {
		return { status: res.status, json: JSON.parse(text) }
	} catch (e) {
		return { status: res.status, text }
	}
}

// 10 个模拟机构（贴近真实教培机构的名字）
const TENANTS = [
	['scale01', '星海艺术培训中心'],
	['scale02', '启航少儿英语'],
	['scale03', '明德书法学堂'],
	['scale04', '小荷钢琴教室'],
	['scale05', '博雅数理思维'],
	['scale06', '新航道国际英语'],
	['scale07', '青云美术教育'],
	['scale08', '乐高创意学院'],
	['scale09', '大千书法教育'],
	['scale10', '优学培优']
]

const login = await call('POST', '/auth/b/doLogin', { account: 'platformAdmin', password: sm.sm2.doEncrypt('Chameleon@2026!', KEY, 1) }, null, 'platform')
const token = login.json?.data
if (!token) {
	console.error('平台运营登录失败：', login.json?.msg)
	process.exit(1)
}
console.log('平台运营登录成功')

const expireTime = new Date(Date.now() + 365 * 24 * 3600 * 1000)
const pad = (n) => String(n).padStart(2, '0')
const expireText = `${expireTime.getFullYear()}-${pad(expireTime.getMonth() + 1)}-${pad(expireTime.getDate())} 00:00:00`

const created = []
for (const [code, name] of TENANTS) {
	const res = await call('POST', '/platform/tenant/add', {
		code,
		name,
		contactName: '张经理',
		contactPhone: '13800000000',
		packageId: 'pkg_demo',
		expireTime: expireText,
		adminAccount: 'admin',
		adminName: '机构管理员',
		adminPassword: 'Chameleon@2026!',
		campusName: '总校区',
		remark: '性能压测租户'
	}, token)
	if (res.json?.code === 200) {
		created.push([code, name, res.json.data])
		console.log(`创建租户 ${code} ${name} -> ${res.json.data}`)
	} else {
		console.log(`创建租户 ${code} 失败：${res.json?.msg}`)
	}
}
console.log(`\n共创建 ${created.length} 个租户`)
console.log(JSON.stringify(created))
