import { createRequire } from 'node:module'
const require = createRequire('D:/IdeaProject/chameleon/snowy-admin-web/package.json')
const sm = require('sm-crypto')

const BASE = 'http://127.0.0.1:82'
const KEY = '04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'
const TENANT = process.argv[2] || 'demo'

let pass = 0
let fail = 0
const check = (name, ok, extra = '') => {
	ok ? pass++ : fail++
	console.log(`${ok ? '[PASS]' : '[FAIL]'} ${name} ${extra}`)
}

const call = async (method, path, body, token) => {
	const res = await fetch(BASE + path, {
		method,
		headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) },
		body: body ? JSON.stringify(body) : undefined
	})
	const text = await res.text()
	let json = null
	try {
		json = JSON.parse(text)
	} catch (e) {}
	return { status: res.status, json }
}

const login = async (account) => {
	const res = await call('POST', '/auth/b/doLogin', {
		account,
		password: sm.sm2.doEncrypt('Chameleon@2026!', KEY, 1)
	}, null)
	return res.json?.data
}

const get = async (path, token) => {
	const res = await call('GET', path, null, token)
	return res.json || {}
}

/** 各角色应该看到 / 不该看到的接口 */
const EXPECT = {
	teacher01: {
		allow: ['/course/schedule/week', '/course/attend/page', '/course/classStudent/page', '/student/page', '/student/detail?id=s_ux_001'],
		deny: ['/crm/lead/page', '/student/order/page', '/course/class/page', '/finance/report/summary', '/biz/automation/rule']
	},
	sales01: {
		allow: ['/crm/lead/page', '/crm/follow/page?leadId=1', '/student/page', '/student/order/page', '/crm/report/funnel'],
		deny: ['/course/schedule/week', '/course/attend/page', '/finance/report/summary', '/biz/automation/rule']
	},
	cashier01: {
		allow: ['/student/order/page', '/student/refund/page', '/student/renewWarn/page', '/finance/report/summary'],
		deny: ['/crm/lead/page', '/course/schedule/week', '/course/class/page', '/biz/automation/rule']
	},
	principal01: {
		allow: ['/crm/lead/page', '/course/schedule/week', '/student/order/page', '/finance/report/summary', '/course/report/academic/attendance'],
		deny: ['/biz/automation/rule']
	}
}

console.log(`===== 角色权限与数据范围验证（租户 ${TENANT}） =====\n`)

// 1) 接口权限
for (const [account, rule] of Object.entries(EXPECT)) {
	const token = await login(account)
	if (!token) {
		check(`${account} 登录`, false)
		continue
	}
	const me = await get('/auth/b/getLoginUser', token)
	check(`${account} 登录`, true, `角色=${JSON.stringify(me.data?.roleCodeList)}`)
	for (const path of rule.allow) {
		const res = await get(path, token)
		check(`  ${account} 可访问 ${path}`, res.code === 200, res.code === 200 ? '' : `→ ${res.msg}`)
	}
	for (const path of rule.deny) {
		const res = await get(path, token)
		check(`  ${account} 不可访问 ${path}`, res.code !== 200, res.code === 200 ? '← 越权了！' : '')
	}
}

// 2) 数据范围：老师只看自己的课
{
	const token = await login('teacher01')
	const week = await get('/course/schedule/week?startDate=2026-10-19&endDate=2026-10-25', token)
	const list = week.data || []
	// 老师可见的课 = 课节授课人是自己，或课属于自己带的班级
	const allMine = list.length > 0 && list.every((item) => item.teacherId === 'u_teacher_001' || item.classId === 'cc_ux_001')
	check('老师课表只含自己的课', allMine, `共 ${list.length} 节`)

	const principalToken = await login('principal01')
	const principalWeek = await get('/course/schedule/week?startDate=2026-10-19&endDate=2026-10-25', principalToken)
	check('校长能看到全部课节', (principalWeek.data || []).length >= list.length, `校长 ${(principalWeek.data || []).length} 节`)
}

// 3) 数据范围：顾问只看自己的线索
{
	const token = await login('sales01')
	const leads = await get('/crm/lead/page?current=1&size=100', token)
	const list = leads.data?.records || []
	const allMine = list.every((item) => item.followUserId === 'u_sales_001')
	check('招生顾问只看自己的线索', allMine && list.length > 0, `共 ${list.length} 条`)

	const principalToken = await login('principal01')
	const principalLeads = await get('/crm/lead/page?current=1&size=100', principalToken)
	check('校长能看到全部线索', (principalLeads.data?.records || []).length > list.length, `校长 ${(principalLeads.data?.records || []).length} 条`)
}

// 4) 数据范围：老师只看自己班的学员
{
	const token = await login('teacher01')
	const students = await get('/student/page?current=1&size=100', token)
	const list = students.data?.records || []
	const principalToken = await login('principal01')
	const principalStudents = await get('/student/page?current=1&size=100', principalToken)
	check(
		'老师学员数 ≤ 校长学员数',
		list.length <= (principalStudents.data?.records || []).length,
		`老师 ${list.length} 人 / 校长 ${(principalStudents.data?.records || []).length} 人`
	)
}

// 5) 菜单可见性：角色只能看到自己被授权的菜单
{
	const EXPECT_MENU = {
		teacher01: { allow: ['/course/schedule', '/course/attend', '/student'], deny: ['/crm', '/student/order', '/course/class'] },
		sales01: { allow: ['/crm', '/student', '/student/order'], deny: ['/course/schedule', '/course/class'] },
		cashier01: { allow: ['/student/order', '/student/renewWarn'], deny: ['/crm', '/course/schedule'] },
		principal01: { allow: ['/crm', '/course/schedule', '/student/order', '/report/dashboard'], deny: ['/automation'] }
	}
	for (const [account, rule] of Object.entries(EXPECT_MENU)) {
		const token = await login(account)
		const menu = await get('/sys/userCenter/loginMenu', token)
		const menuText = JSON.stringify(menu.data || '')
		for (const path of rule.allow) {
			check(`  ${account} 菜单可见 ${path}`, menuText.includes(path))
		}
		for (const path of rule.deny) {
			check(`  ${account} 菜单不可见 ${path}`, !menuText.includes('"path":"' + path + '"'))
		}
	}
}

console.log(`\n===== 权限验证: PASS ${pass} / FAIL ${fail} =====`)
if (fail > 0) process.exit(1)
