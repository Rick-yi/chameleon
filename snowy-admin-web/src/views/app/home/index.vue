<template>
	<div class="m-home">
		<!-- 页头 -->
		<div class="m-wb-head">
			<div class="m-wb-date">{{ todayLabel }}</div>
			<a-button v-if="hasPerm('/student/add')" type="primary" size="small" @click="enrollOpen = true">
				录入学员
			</a-button>
		</div>

		<!-- 关键指标：与 PC 工作台一致 -->
		<div class="m-stat-grid">
			<div
				v-for="item in kpiList"
				:key="item.key"
				class="m-stat"
				:class="{ link: appStore.canVisit(item.path) }"
				@click="go(item.path)"
			>
				<div class="m-stat-label">{{ item.label }}</div>
				<div class="m-stat-value" :style="{ color: statTextColor(item.color) }">{{ item.value }}</div>
				<div class="m-stat-sub">{{ item.sub }}</div>
			</div>
		</div>

		<!-- 快捷入口 -->
		<div class="m-section-title">快捷操作</div>
		<div class="m-quick-grid">
			<div v-for="item in quickList" :key="item.key" class="m-quick" @click="onQuick(item)">
				<div class="m-quick-icon" :style="{ background: item.color }">
					<component :is="item.icon" />
				</div>
				<div class="m-quick-text">{{ item.text }}</div>
			</div>
		</div>

		<!-- 待办与经营明细 -->
		<div v-if="canSeeCashier" class="m-section-title">待收款订单</div>
		<div v-if="canSeeCashier" class="m-wb-card">
			<div class="m-wb-card-head">
				<span>待收款订单</span>
				<a-tag :color="unpaid.total > 0 ? 'orange' : 'default'">{{ unpaid.total || 0 }}</a-tag>
				<a class="m-wb-all" @click="go('/app/cashier/orders?status=UNPAID')">全部 ›</a>
			</div>
			<div v-if="!unpaid.list.length" class="m-wb-empty">没有待收款订单</div>
			<div v-for="item in unpaid.list" :key="item.id" class="m-wb-row" @click="openPay(item)">
				<div class="m-wb-main">
					<div class="m-wb-name">{{ item.studentName || '学员' }}</div>
					<div class="m-wb-sub">{{ item.packageName || '课包' }}</div>
				</div>
				<div class="m-wb-side">
					<div class="m-wb-amount">¥{{ fmtMoney(item.amount) }}</div>
					<div class="m-wb-time">{{ formatTime(item.createTime) }}</div>
					<div class="m-wb-actions">
						<a-tag v-if="overdueDays(item.createTime) > 0" color="red">逾期 {{ overdueDays(item.createTime) }} 天</a-tag>
						<a-button v-if="hasPerm('/student/order/offlinePay')" class="m-wb-action" size="small" @click.stop="openPay(item)">收款</a-button>
					</div>
				</div>
			</div>
		</div>

		<div v-if="canSeeTeaching" class="m-section-title">今日课节</div>
		<div v-if="canSeeTeaching" class="m-wb-card">
			<div class="m-wb-card-head">
				<span>今日课节</span>
				<a-tag :color="todaySchedule.length ? 'blue' : 'default'">{{ todaySchedule.length }}</a-tag>
				<a class="m-wb-all" @click="go('/app/teach/schedule')">课表 ›</a>
			</div>
			<div v-if="!todaySchedule.length" class="m-wb-empty">
				今天没有排课
				<a-button type="link" size="small" @click="go('/app/teach/schedule')">查看本周课表</a-button>
			</div>
			<div v-for="item in sortedSchedule" :key="item.id" class="m-wb-row" @click="goAttend(item)">
				<div class="m-wb-main">
					<div class="m-wb-name">
						{{ item.className || '未命名班级' }}
						<a-tag v-if="item.attendStatus !== 'DONE'" color="red" style="margin-left: 4px">待点名</a-tag>
					</div>
					<div class="m-wb-sub">
						{{ item.startTime }} - {{ item.endTime }} · {{ item.classroom || '未定教室' }}
						{{ item.teacherName ? ' · ' + item.teacherName : '' }}
					</div>
				</div>
				<div class="m-wb-side">
					<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
					<div class="m-wb-time">应到 {{ item.studentCount || 0 }} · 已点 {{ item.attendCount || 0 }}</div>
					<a-button v-if="item.attendStatus !== 'DONE'" class="m-wb-action" size="small" @click.stop="goAttend(item)">去点名</a-button>
				</div>
			</div>
		</div>

		<div v-if="canSeeRefund" class="m-section-title">待审批退费</div>
		<div v-if="canSeeRefund" class="m-wb-card">
			<div class="m-wb-card-head">
				<span>待审批退费</span>
				<a-tag :color="pendingRefund.total > 0 ? 'red' : 'default'">{{ pendingRefund.total || 0 }}</a-tag>
				<a class="m-wb-all" @click="go('/app/cashier/orders?tab=refund')">全部 ›</a>
			</div>
			<div v-if="!pendingRefund.list.length" class="m-wb-empty">没有待审批退费</div>
			<div v-for="item in pendingRefund.list" :key="item.id" class="m-wb-row" @click="openApprove(item)">
				<div class="m-wb-main">
					<div class="m-wb-name">{{ item.studentName || '学员' }}</div>
					<div class="m-wb-sub">可退 {{ item.refundLessons || 0 }} 课时</div>
				</div>
				<div class="m-wb-side">
					<div class="m-wb-amount">¥{{ fmtMoney(item.refundAmount) }}</div>
					<div class="m-wb-time">{{ formatTime(item.createTime) }}</div>
					<div class="m-wb-actions">
						<a-tag v-if="overdueDays(item.createTime) > 0" color="red">逾期 {{ overdueDays(item.createTime) }} 天</a-tag>
						<a-button v-if="hasPerm('/student/refund/approve')" class="m-wb-action" size="small" @click.stop="openApprove(item)">审批</a-button>
					</div>
				</div>
			</div>
		</div>

		<div v-if="canSeeEnroll" class="m-section-title">待跟进线索</div>
		<div v-if="canSeeEnroll" class="m-wb-card">
			<div class="m-wb-card-head">
				<span>待跟进线索</span>
				<a-tag :color="leadFollow.total > 0 ? 'purple' : 'default'">{{ leadFollow.total || 0 }}</a-tag>
				<a class="m-wb-all" @click="go('/app/enroll/leads')">全部 ›</a>
			</div>
			<div v-if="!leadFollow.list.length" class="m-wb-empty">没有待跟进线索</div>
			<div v-for="item in leadFollow.list" :key="item.id" class="m-wb-row" @click="openFollow(item)">
				<div class="m-wb-main">
					<div class="m-wb-name">{{ item.name }}</div>
					<div class="m-wb-sub">{{ item.parentPhone || '无电话' }} · 跟进人 {{ item.followUserName || '-' }}</div>
				</div>
				<div class="m-wb-side">
					<a-tag v-if="followOverdueDays(item.nextFollowTime) > 0" color="red">
						逾期 {{ followOverdueDays(item.nextFollowTime) }} 天
					</a-tag>
					<a-tag v-else-if="followOverdueDays(item.nextFollowTime) === 0" color="orange">今日跟进</a-tag>
					<div class="m-wb-time">{{ formatTime(item.nextFollowTime) }}</div>
					<a-button v-if="hasPerm('/crm/follow/add')" class="m-wb-action" size="small" @click.stop="openFollow(item)">跟进</a-button>
				</div>
			</div>
		</div>

		<div v-if="canSeeTeaching" class="m-section-title">续费预警</div>
		<div v-if="canSeeTeaching" class="m-wb-card">
			<div class="m-wb-card-head">
				<span>续费预警</span>
				<a-tag :color="renewWarn.total > 0 ? 'gold' : 'default'">{{ renewWarn.total || 0 }}</a-tag>
				<a class="m-wb-all" @click="go('/app/student/renew')">全部 ›</a>
			</div>
			<div v-if="!renewWarn.list.length" class="m-wb-empty">没有续费预警</div>
			<div v-for="item in renewWarn.list" :key="item.id" class="m-wb-row" @click="go('/app/student/renew')">
				<div class="m-wb-main">
					<div class="m-wb-name">{{ item.studentName || '学员' }}</div>
					<div class="m-wb-sub">
						{{ item.warnReason === 'LESSON_LOW' ? '课时不足' : '课包即将到期' }} · 剩余 {{ item.remainLessons || 0 }} 课时
					</div>
				</div>
				<div class="m-wb-side">
					<div class="m-wb-time">{{ formatTime(item.createTime) }}</div>
					<a-button class="m-wb-action" size="small" @click.stop="go('/app/student/renew')">处理</a-button>
				</div>
			</div>
		</div>

		<div v-if="canSeeCashier" class="m-section-title">今日收款</div>
		<div v-if="canSeeCashier" class="m-wb-card">
			<div class="m-wb-card-head">
				<span>今日收款</span>
				<a-tag color="green">¥{{ fmtMoney(todayPaid) }}</a-tag>
				<a class="m-wb-all" @click="go('/app/cashier/orders?status=PAID')">收款台 ›</a>
			</div>
			<div v-if="!todayPaidList.length" class="m-wb-empty">今天还没有收款</div>
			<div v-for="item in todayPaidList" :key="item.id" class="m-wb-row" @click="go('/app/cashier/orders?status=PAID')">
				<div class="m-wb-main">
					<div class="m-wb-name">{{ item.studentName || '学员' }}</div>
					<div class="m-wb-sub">{{ item.packageName || '课包' }}</div>
				</div>
				<div class="m-wb-side">
					<div class="m-wb-amount">¥{{ fmtMoney(item.paidAmount) }}</div>
					<div class="m-wb-time">{{ formatTime(item.payTime) }}</div>
				</div>
			</div>
		</div>

		<enrollment-flow v-model:open="enrollOpen" @success="loadAll" />
	</div>
</template>

<script setup name="appHome">
	import { useRouter } from 'vue-router'
	import { useAppStore } from '@/store/app'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	import crmApi from '@/api/crm/crmApi'
	import reportApi from '@/api/report/reportApi'
	import {
		UserAddOutlined,
		ShoppingCartOutlined,
		WalletOutlined,
		CalendarOutlined,
		CheckSquareOutlined,
		PhoneOutlined,
		BarChartOutlined
	} from '@ant-design/icons-vue'

	const router = useRouter()
	const appStore = useAppStore()
	const enrollOpen = ref(false)

	const unpaid = ref({ total: 0, list: [] })
	const pendingRefund = ref({ total: 0, list: [] })
	const renewWarn = ref({ total: 0, list: [] })
	const leadFollow = ref({ total: 0, list: [] })
	const todaySchedule = ref([])
	const todayPaidList = ref([])
	const todayPaid = ref(0)
	const monthSummary = ref({})
	const studyingCount = ref(0)
	const newLeadTotal = ref(0)

	const canSeeCashier = computed(() => ['all', 'cashier', 'enroll'].includes(appStore.view))
	const canSeeRefund = computed(() => ['all', 'cashier'].includes(appStore.view))
	const canSeeTeaching = computed(() => ['all', 'teach'].includes(appStore.view))
	const canSeeEnroll = computed(() => ['all', 'enroll'].includes(appStore.view))

	const pad = (n) => String(n).padStart(2, '0')
	const today = () => {
		const d = new Date()
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}
	const todayStart = () => `${today()} 00:00:00`
	const todayEnd = () => `${today()} 23:59:59`
	const todayLabel = computed(() => {
		const names = '日一二三四五六'
		const d = new Date()
		return `星期${names[d.getDay()]} · ${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	})
	const fmtMoney = (value) => Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
	const formatTime = (value) => (value ? String(value).slice(5, 16).replace('T', ' ') : '-')
	const statTextColor = (color) => {
		if (color === '#d89614') return 'var(--warning-text)'
		if (color === '#e5484d') return 'var(--danger-text)'
		if (color === '#2ba471' || color === '#4bb964') return 'var(--success-text)'
		return color
	}
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')
	const dayDiff = (value) => {
		if (!value) return null
		const normalized = String(value).replace(/-/g, '/').replace('T', ' ').slice(0, 19)
		const date = new Date(normalized)
		if (Number.isNaN(date.getTime())) return null
		const startOfToday = new Date()
		startOfToday.setHours(0, 0, 0, 0)
		const target = new Date(date)
		target.setHours(0, 0, 0, 0)
		return Math.floor((startOfToday.getTime() - target.getTime()) / (24 * 60 * 60 * 1000))
	}
	const overdueDays = (value) => {
		const days = dayDiff(value)
		return days === null ? -1 : days
	}
	const followOverdueDays = (value) => {
		if (!value) return -1
		return dayDiff(value)
	}

	const teaching = computed(() => {
		let expected = 0
		let attended = 0
		let pending = 0
		for (const item of todaySchedule.value) {
			expected += Number(item.studentCount || 0)
			attended += Number(item.attendCount || 0)
			if (item.attendStatus !== 'DONE') pending += 1
		}
		return {
			expected,
			attended,
			pending,
			progress: expected ? Math.round((attended * 100) / expected) : 0
		}
	})
	const sortedSchedule = computed(() => {
		const weight = (status) => (status === 'NONE' ? 0 : status === 'PARTIAL' ? 1 : 2)
		return [...todaySchedule.value].sort((a, b) => {
			const byStatus = weight(a.attendStatus) - weight(b.attendStatus)
			return byStatus !== 0 ? byStatus : String(a.startTime || '').localeCompare(String(b.startTime || ''))
		})
	})

	// 与 PC 工作台一致的指标；按当前视角裁剪
	const kpiList = computed(() => {
			if (appStore.view === 'teach') {
				return [
					{ key: 'studying', label: '在读学员', value: studyingCount.value || 0, sub: '人', color: '#123B2B', path: '/app/teach/students' },
				{ key: 'expected', label: '今日应到', value: teaching.value.expected, sub: '人', color: '#4bb964', path: '/app/teach/schedule' },
				{ key: 'attended', label: '已点名', value: teaching.value.attended, sub: '人', color: '#2ba471', path: '/app/teach/attend' },
				{ key: 'pending', label: '未点名课节', value: teaching.value.pending, sub: '节', color: '#d89614', path: '/app/teach/attend' },
				{ key: 'progress', label: '点名进度', value: teaching.value.progress, sub: '%', color: '#123B2B', path: '/app/teach/attend' }
			]
		}
		if (appStore.view === 'enroll') {
			return [
				{ key: 'lead', label: '在跟线索', value: leadFollow.value.total || 0, sub: '条', color: '#6f5bd8', path: '/app/enroll/leads' },
				{ key: 'newLead', label: '新线索', value: newLeadTotal.value || 0, sub: '条', color: '#4bb964', path: '/app/enroll/leads' },
				{ key: 'unpaid', label: '待收款', value: unpaid.value.total || 0, sub: '笔', color: '#d89614', path: '/app/cashier/orders?status=UNPAID' },
				{ key: 'todayPaid', label: '今日收款', value: `¥${fmtMoney(todayPaid.value)}`, sub: `${todayPaidList.value.length} 笔`, color: '#2ba471', path: '/app/cashier/orders?status=PAID' }
			]
		}
		if (appStore.view === 'cashier') {
			return [
				{ key: 'todayPaid', label: '今日收款', value: `¥${fmtMoney(todayPaid.value)}`, sub: `${todayPaidList.value.length} 笔`, color: '#2ba471', path: '/app/cashier/orders?status=PAID' },
				{ key: 'monthPaid', label: '本月收款', value: `¥${fmtMoney(monthSummary.value.paidAmount)}`, sub: '本月', color: '#4bb964', path: '/app/report' },
				{ key: 'unpaid', label: '待收款', value: unpaid.value.total || 0, sub: '笔', color: '#d89614', path: '/app/cashier/orders?status=UNPAID' },
				{ key: 'refund', label: '待审批退费', value: pendingRefund.value.total || 0, sub: '笔', color: '#e5484d', path: '/app/cashier/orders?tab=refund' }
			]
		}
		return [
			{ key: 'todayPaid', label: '今日收款', value: `¥${fmtMoney(todayPaid.value)}`, sub: `${todayPaidList.value.length} 笔`, color: '#2ba471', path: '/app/cashier/orders?status=PAID' },
			{ key: 'monthPaid', label: '本月收款', value: `¥${fmtMoney(monthSummary.value.paidAmount)}`, sub: '本月', color: '#4bb964', path: '/app/report' },
			{ key: 'unpaid', label: '待收款订单', value: unpaid.value.total || 0, sub: '笔', color: '#d89614', path: '/app/cashier/orders?status=UNPAID' },
			{ key: 'studying', label: '在读学员', value: studyingCount.value || 0, sub: '人', color: '#123B2B', path: '/app/teach/students' },
			{ key: 'expected', label: '今日应到', value: teaching.value.expected, sub: '人', color: '#4bb964', path: '/app/teach/schedule' },
			{ key: 'attended', label: '已点名', value: teaching.value.attended, sub: '人', color: '#2ba471', path: '/app/teach/attend' },
			{ key: 'pending', label: '未点名课节', value: teaching.value.pending, sub: '节', color: '#d89614', path: '/app/teach/attend' },
			{ key: 'progress', label: '点名进度', value: teaching.value.progress, sub: '%', color: '#123B2B', path: '/app/teach/attend' }
		]
	})

	const quickList = computed(() => {
		const list = [
			{ key: 'quickEnroll', text: '录入学员', icon: UserAddOutlined, color: '#fff1e6', action: 'enroll', perm: '/student/add' },
			{ key: 'enrollOrder', text: '报名购课', icon: ShoppingCartOutlined, color: '#eaf2fd', path: '/app/teach/students?enroll=1' },
			{ key: 'cashier', text: '收款台', icon: WalletOutlined, color: '#e8f7f0', path: '/app/cashier/orders' },
			{ key: 'schedule', text: '我的课表', icon: CalendarOutlined, color: '#f3edfd', path: '/app/teach/schedule' },
			{ key: 'attend', text: '点名消课', icon: CheckSquareOutlined, color: '#fdf3e6', path: '/app/teach/attend' },
			{ key: 'leads', text: '招生线索', icon: PhoneOutlined, color: '#e8f4f8', path: '/app/enroll/leads' },
			{ key: 'report', text: '经营看板', icon: BarChartOutlined, color: '#fdedf0', path: '/app/report' }
		]
		return list.filter((item) => (!item.perm || hasPerm(item.perm)) && appStore.canVisit(item.path))
	})

	const go = (path) => {
		if (path && appStore.canVisit(path)) router.push(path)
	}
	const onQuick = (item) => {
		if (item.action === 'enroll') {
			enrollOpen.value = true
			return
		}
		go(item.path)
	}
	const openPay = (item) => {
		router.push({ path: '/app/cashier/orders', query: { status: 'UNPAID', orderId: item.id } })
	}
	const openApprove = (item) => {
		router.push({ path: '/app/cashier/orders', query: { tab: 'refund', refundId: item.id } })
	}
	const openFollow = (item) => {
		router.push({ path: '/app/enroll/lead', query: { id: item.id, action: 'follow' } })
	}
	const goAttend = (item) => {
		router.push({ path: '/app/teach/attend', query: { scheduleId: item.id, classId: item.classId } })
	}

	const safe = (promise, fallback) => promise.catch(() => fallback)
	const emptyPage = () => ({ total: 0, records: [] })
	const loadAll = async () => {
		const dayStart = todayStart()
		const dayEnd = todayEnd()
		const now = new Date()
		const monthStart = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-01`
		const needReport = ['all', 'cashier'].includes(appStore.view)
		const needOrder = canSeeCashier.value
		const needRefund = canSeeRefund.value
		const needTeaching = canSeeTeaching.value
		const needEnroll = canSeeEnroll.value
		try {
			const [
				order, refund, warn, lead, schedule, paid, dashboard, todayDashboard, student, newLead
			] = await Promise.all([
				needOrder
					? safe(studentApi.orderPage({ current: 1, size: 5, status: 'UNPAID' }), emptyPage())
					: Promise.resolve(emptyPage()),
				needRefund
					? safe(studentApi.refundPage({ current: 1, size: 5, status: 'PENDING' }), emptyPage())
					: Promise.resolve(emptyPage()),
				needTeaching
					? safe(studentApi.renewWarnPage({ current: 1, size: 5, status: 'PENDING' }), emptyPage())
					: Promise.resolve(emptyPage()),
				needEnroll
					? safe(crmApi.leadPage({ current: 1, size: 5, nextFollowBefore: dayEnd, excludeStatus: 'ENROLLED,LOST' }), emptyPage())
					: Promise.resolve(emptyPage()),
				needTeaching
					? safe(courseApi.scheduleWeek({ startDate: dayStart, endDate: dayEnd }), [])
					: Promise.resolve([]),
				needOrder
					? safe(studentApi.orderPage({ current: 1, size: 20, status: 'PAID' }), emptyPage())
					: Promise.resolve(emptyPage()),
				needReport
					? safe(reportApi.dashboard({ startTime: `${monthStart} 00:00:00`, endTime: dayEnd }), {})
					: Promise.resolve({}),
				needReport
					? safe(reportApi.dashboard({ startTime: dayStart, endTime: dayEnd }), null)
					: Promise.resolve(null),
				safe(studentApi.studentPage({ current: 1, size: 1, status: 'STUDYING' }), emptyPage()),
				needEnroll
					? safe(crmApi.leadPage({ current: 1, size: 1, status: 'NEW', excludeStatus: 'ENROLLED,LOST' }), emptyPage())
					: Promise.resolve(emptyPage())
			])
			unpaid.value = { total: order.total || 0, list: order.records || [] }
			pendingRefund.value = { total: refund.total || 0, list: refund.records || [] }
			renewWarn.value = { total: warn.total || 0, list: warn.records || [] }
			leadFollow.value = { total: lead.total || 0, list: lead.records || [] }
			todaySchedule.value = schedule || []
			todayPaidList.value = (paid.records || []).filter((item) => item.payTime && String(item.payTime).slice(0, 10) === today())
			const paidFromList = todayPaidList.value.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0)
			todayPaid.value = Number(todayDashboard?.summary?.paidAmount ?? paidFromList)
			monthSummary.value = dashboard?.summary || {}
			studyingCount.value = student?.total || 0
			newLeadTotal.value = newLead?.total || 0
		}
	}

	onMounted(loadAll)
	watch(() => appStore.view, loadAll)
</script>

<style lang="less" scoped>
	.m-home {
		padding-bottom: 8px;
	}
	.m-wb-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 10px;
		margin: 0 2px 10px;
	}
	.m-wb-title {
		font-size: 20px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-wb-date {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-stat-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 10px;
	}
	.m-stat {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
	}
	.m-stat.link {
		cursor: pointer;
	}
	.m-stat-label {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-stat-value {
		margin-top: 4px;
		font-size: 22px;
		font-weight: 600;
		line-height: 28px;
	}
	.m-stat-sub {
		margin-top: 2px;
		font-size: 11px;
		color: var(--text-color-tertiary);
	}
	.m-section-title {
		margin: 16px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-quick-grid {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 10px;
	}
	.m-quick {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 6px;
		padding: 10px 4px;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
	}
	.m-quick-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 36px;
		height: 36px;
		border-radius: 10px;
		font-size: 17px;
		color: var(--primary-6);
	}
	.m-quick-text {
		font-size: 12px;
		color: var(--text-color);
		text-align: center;
	}
	.m-wb-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 4px;
	}
	.m-wb-card-head {
		display: flex;
		align-items: center;
		gap: 8px;
		padding-bottom: 8px;
		border-bottom: 1px solid var(--border-color-split);
		font-size: 14px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-wb-all {
		margin-left: auto;
		font-size: 12px;
		font-weight: 400;
	}
	.m-wb-empty {
		padding: 14px 0;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-wb-row {
		display: flex;
		align-items: flex-start;
		gap: 10px;
		padding: 10px 0;
		border-bottom: 1px dashed var(--border-color-split);
		&:last-child {
			border-bottom: none;
			padding-bottom: 2px;
		}
	}
	.m-wb-main {
		flex: 1;
		min-width: 0;
	}
	.m-wb-name {
		font-size: 14px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-wb-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.m-wb-side {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 2px;
		min-width: 96px;
	}
	.m-wb-amount {
		font-size: 14px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-wb-time {
		font-size: 11px;
		color: var(--text-color-tertiary);
	}
	.m-wb-actions {
		display: flex;
		align-items: center;
		gap: 4px;
		flex-wrap: wrap;
		justify-content: flex-end;
	}
	.m-wb-action {
		min-height: 40px;
		padding: 0 10px;
		font-size: 13px;
	}
</style>
