/**
 * 演示数据生成脚本（通过业务接口写入，保证课时账户/流水/报表联动）
 *
 * 用法：
 *   node scripts/seed-demo-data.mjs --base https://aiglobe.xin/api \
 *     --tenant aiglobe --account admin --password '***'
 *
 * 说明：
 *   - 仅用于空租户；检测到已有课程会直接退出，避免重复灌数据；
 *   - 生成：4 门课程/课包、4 个班级、40 名学员、40 笔报名收款、
 *     4 周课表、每个班级 1 次点名、25 条线索（含跟进与试听）。
 */
import { createRequire } from 'node:module'

const require = createRequire(new URL('../snowy-admin-web/package.json', import.meta.url))
const sm = require('sm-crypto')

const KEY =
	'04298364ec840088475eae92a591e01284d1abefcda348b47eb324bb521bb03b0b2a5bc393f6b71dabb8f15c99a0050818b56b23f31743b93df9cf8948f15ddb54'

const args = process.argv.slice(2)
const arg = (name, fallback) => {
	const idx = args.indexOf(`--${name}`)
	return idx >= 0 ? args[idx + 1] : fallback
}

const BASE = arg('base', 'https://aiglobe.xin/api').replace(/\/$/, '')
const TENANT = arg('tenant', 'aiglobe')
const ACCOUNT = arg('account', 'admin')
const PASSWORD = arg('password')
if (!PASSWORD) {
	console.error('缺少 --password')
	process.exit(1)
}

let token = ''
const call = async (method, path, body) => {
	const res = await fetch(BASE + path, {
		method,
		headers: {
			'Content-Type': 'application/json',
			'X-Tenant-Code': TENANT,
			...(token ? { token } : {})
		},
		body: body ? JSON.stringify(body) : undefined
	})
	const json = await res.json().catch(() => ({}))
	if (json.code !== 200) {
		throw new Error(`${method} ${path} -> ${json.code} ${json.msg}`)
	}
	return json.data
}

const pad = (n, width = 2) => String(n).padStart(width, '0')
const fmtDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const fmtDateTime = (d) =>
	`${fmtDate(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`

const login = async () => {
	const data = await call('POST', '/auth/b/doLogin', {
		account: ACCOUNT,
		password: sm.sm2.doEncrypt(PASSWORD, KEY, 1)
	})
	token = data
	return await call('GET', '/auth/b/getLoginUser')
}

const courseSeed = [
	{ name: '创意美术', price: 3600, lessons: 20, gift: 2, room: 'B201', teacher: 1, day: 1, start: '18:00', end: '19:30' },
	{ name: '素描基础', price: 4200, lessons: 24, gift: 2, room: 'B202', teacher: 2, day: 2, start: '18:00', end: '19:30' },
	{ name: '硬笔书法', price: 2800, lessons: 16, gift: 2, room: 'A301', teacher: 3, day: 6, start: '09:00', end: '10:30' },
	{ name: '少儿编程', price: 4800, lessons: 24, gift: 4, room: 'A302', teacher: 4, day: 7, start: '09:00', end: '10:30' }
]

const surnames = ['张', '王', '李', '赵', '陈', '刘', '杨', '黄', '周', '吴']
const givenNames = ['小明', '雨桐', '子轩', '思远', '若曦', '浩然', '欣怡', '嘉乐', '诗涵', '一鸣']

const main = async () => {
	console.log('>> 登录', TENANT, ACCOUNT)
	const me = await login()
	console.log('   当前用户：', me.name, me.id)

	const coursePage = await call('GET', '/course/page?current=1&size=1')
	if (Number(coursePage.total) > 0) {
		console.warn('!! 该租户已有课程数据，已跳过（避免重复灌数据）')
		return
	}

	const orgPage = await call('GET', '/sys/org/page?current=1&size=50')
	const campus = (orgPage.records || [])[0]
	const campusId = campus?.id
	console.log('   校区：', campus?.name || '(无)')

	// 1. 课程与课包
	const courseList = []
	for (const item of courseSeed) {
		await call('POST', '/course/add', {
			name: item.name,
			classType: 'GROUP',
			lessonsPerClass: 1.5,
			leaveDeduct: true,
			absentDeduct: true,
			status: 'ENABLE'
		})
		const page = await call('GET', `/course/page?current=1&size=50&searchKey=${encodeURIComponent(item.name)}`)
		const course = (page.records || []).find((row) => row.name === item.name)
		await call('POST', '/course/package/add', {
			courseId: course.id,
			name: `${item.name} ${item.lessons}课时`,
			lessons: item.lessons,
			giftLessons: item.gift,
			price: item.price,
			validityDays: 365,
			status: 'ENABLE'
		})
		const pkgPage = await call(
			'GET',
			`/course/package/page?current=1&size=50&courseId=${course.id}`
		)
		const pkg = (pkgPage.records || [])[0]
		courseList.push({ ...item, courseId: course.id, packageId: pkg.id })
		console.log('   课程/课包：', item.name, '→', pkg.name)
	}

	// 2. 学员
	const studentList = []
	for (let i = 0; i < 40; i++) {
		const name = `${surnames[i % surnames.length]}${givenNames[i % givenNames.length]}${i >= 10 ? i : ''}`
		const phone = `1390000${pad(i + 1, 4)}`
		await call('POST', '/student/add', {
			name,
			gender: i % 2 === 0 ? 'MALE' : 'FEMALE',
			parentName: `${surnames[(i + 3) % surnames.length]}家长`,
			parentPhone: phone,
			campusId,
			remark: '演示数据'
		})
		studentList.push({ name, phone })
	}
	const studentPage = await call('GET', '/student/page?current=1&size=100')
	const students = studentPage.records || []
	console.log('   学员：', students.length, '名')

	// 3. 报名 + 收款（40 名学员，每 10 人对应一门课程，与后续分班一致）
	let paidCount = 0
	for (let i = 0; i < students.length; i++) {
		const student = students[i]
		const course = courseList[Math.floor(i / 10)]
		const orderId = await call('POST', '/student/order/add', {
			studentId: student.id,
			coursePackageId: course.packageId,
			payChannel: 'CASH'
		})
		await call('POST', '/student/order/offlinePay', {
			orderId,
			paidAmount: course.price,
			payChannel: 'CASH',
			remark: '演示数据收款'
		})
		paidCount++
	}
	console.log('   报名收款：', paidCount, '笔')

	// 4. 班级 + 学员分班
	const classList = []
	for (let i = 0; i < courseList.length; i++) {
		const item = courseList[i]
		await call('POST', '/course/class/add', {
			courseId: item.courseId,
			name: `${item.name} ${['A', 'B', 'C', 'D'][i]}班`,
			campusId,
			teacherId: me.id,
			classroom: item.room,
			capacity: 20,
			weekTemplateJson: JSON.stringify([
				{ dayOfWeek: item.day, startTime: item.start, endTime: item.end }
			])
		})
		const classPage = await call('GET', '/course/class/page?current=1&size=50')
		const cls = (classPage.records || []).find((row) => row.name === `${item.name} ${['A', 'B', 'C', 'D'][i]}班`)
		const members = students.slice(i * 10, i * 10 + 10)
		await call('POST', '/course/classStudent/add', {
			classId: cls.id,
			studentIdList: members.map((s) => s.id)
		})
		classList.push({ ...item, classId: cls.id, students: members })
		console.log('   班级：', cls.name, '学员', members.length, '名')
	}

	// 5. 生成 4 周课表（从本周周一开始）
	const now = new Date()
	const monday = new Date(now)
	monday.setDate(now.getDate() - ((now.getDay() + 6) % 7))
	for (const cls of classList) {
		await call('POST', '/course/schedule/generate', {
			classId: cls.classId,
			startDate: `${fmtDate(monday)} 00:00:00`,
			weekCount: 4
		})
	}
	console.log('   课表：4 周 × 4 个班级')

	// 6. 每个班级给第一节已过去的课点名（全员到课）
	const weekEnd = new Date(monday)
	weekEnd.setDate(monday.getDate() + 6)
	const week = await call(
		'GET',
		`/course/schedule/week?startDate=${fmtDate(monday)}&endDate=${fmtDate(weekEnd)}`
	)
	const today = fmtDate(now)
	let attendSessions = 0
	for (const cls of classList) {
		const target = (week || [])
			.filter((row) => row.classId === cls.classId && row.scheduleDate.slice(0, 10) < today)
			.sort((a, b) => (a.scheduleDate + a.startTime).localeCompare(b.scheduleDate + b.startTime))[0]
		if (!target) continue
		await call('POST', '/course/attend/confirm', {
			scheduleId: target.id,
			attendList: cls.students.map((s) => ({ studentId: s.id, attendStatus: 'ATTEND' }))
		})
		attendSessions++
	}
	console.log('   点名消课：', attendSessions, '节课')

	// 7. 招生线索 + 跟进 + 试听
	const sources = ['WALK_IN', 'REFERRAL', 'DOUYIN', 'MEITUAN', 'PHONE']
	const leads = []
	for (let i = 0; i < 25; i++) {
		const name = `${surnames[(i + 2) % surnames.length]}${givenNames[(i + 4) % givenNames.length]}${i}`
		await call('POST', '/crm/lead/add', {
			name,
			parentPhone: `1380000${pad(i + 1, 4)}`,
			intentCourseId: courseList[i % courseList.length].courseId,
			source: sources[i % sources.length],
			followUserId: me.id,
			remark: '演示数据'
		})
		leads.push(name)
	}
	const leadPage = await call('GET', '/crm/lead/page?current=1&size=100')
	const leadRows = leadPage.records || []
	for (let i = 0; i < 12; i++) {
		await call('POST', '/crm/follow/add', {
			leadId: leadRows[i].id,
			followType: 'PHONE',
			content: ['电话沟通，家长想了解课程安排', '已加微信，发送了课程介绍', '预约周末到校参观'][i % 3]
		})
	}
	for (let i = 12; i < 18; i++) {
		const trialTime = new Date()
		trialTime.setDate(trialTime.getDate() - (i % 3 === 0 ? 2 : 0) + (i % 3 === 1 ? 1 : 0))
		trialTime.setHours(10 + (i % 5), 0, 0, 0)
		await call('POST', '/crm/trial/add', {
			leadId: leadRows[i].id,
			courseId: courseList[i % courseList.length].courseId,
			trialTime: fmtDateTime(trialTime),
			remark: '演示数据试听'
		})
	}
	console.log('   线索：', leadRows.length, '条（12 条跟进，6 条试听）')

	console.log('\n===== 演示数据生成完成 =====')
	console.log(`租户：${TENANT}，课程 ${courseList.length} 门，班级 ${classList.length} 个，学员 ${students.length} 名，收款 ${paidCount} 笔`)
}

main().catch((e) => {
	console.error('生成失败：', e.message)
	process.exit(1)
})
