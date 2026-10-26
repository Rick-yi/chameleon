<template>
	<div class="m-home">
		<!-- 指标块 -->
		<div class="m-stat-grid">
			<div v-for="item in statList" :key="item.key" class="m-stat" :class="{ link: appStore.canVisit(item.path) }" @click="go(item.path)">
				<div class="m-stat-label">{{ item.label }}</div>
				<div class="m-stat-value" :style="{ color: statTextColor(item.color) }">{{ item.value }}</div>
				<div class="m-stat-sub">{{ item.sub }}</div>
			</div>
		</div>

		<!-- 今日待办 -->
		<div class="m-section-title">今日待办</div>
		<div v-if="loading && !todoList.length" class="m-card m-skeleton" v-for="i in 3" :key="`s${i}`"></div>
		<div v-else-if="!todoList.length" class="m-card m-empty">今天没有待办事项</div>
		<div
			v-for="item in todoList"
			:key="item.key"
			class="m-card m-todo"
			@click="go(item.path)"
		>
			<div class="m-todo-left">
				<div class="m-todo-time">{{ item.time }}</div>
				<div class="m-todo-title">{{ item.title }}</div>
				<div class="m-todo-sub">{{ item.sub }}</div>
			</div>
			<a-button v-if="item.actionLabel" class="m-todo-action" size="small" @click.stop="onTodoAction(item)">
				{{ item.actionLabel }}
			</a-button>
			<a-icon class="m-todo-arrow"><right-outlined /></a-icon>
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

		<enrollment-flow v-model:open="enrollOpen" @success="loadAll" />
	</div>
</template>

<script setup name="appHome">
	import { useRouter } from 'vue-router'
	import { useAppStore } from '@/store/app'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	import crmApi from '@/api/crm/crmApi'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'
	import {
		CalendarOutlined,
		TeamOutlined,
		PhoneOutlined,
		BarChartOutlined,
		WalletOutlined,
		AuditOutlined,
		UserAddOutlined,
		ShoppingCartOutlined,
		UserOutlined
	} from '@ant-design/icons-vue'

	const router = useRouter()
	const appStore = useAppStore()
	const loading = ref(false)
	const enrollOpen = ref(false)

	const statList = ref([])
	// 指标数字用语义文字色，图标背景继续用品牌色
	const statTextColor = (color) => {
		if (color === '#d89614') return 'var(--warning-text)'
		if (color === '#e5484d') return 'var(--danger-text)'
		if (color === '#2ba471' || color === '#4bb964') return 'var(--success-text)'
		return color
	}
	const todoList = ref([])
	const quickList = ref([])

	const pad = (n) => String(n).padStart(2, '0')
	const today = () => {
		const d = new Date()
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}
	const todayStart = () => `${today()} 00:00:00`
	const todayEnd = () => `${today()} 23:59:59`

	/** 各视角的指标与待办 */
	const loadTeach = async () => {
		const [scheduleRes, unpaidRes, paidRes, leadRes, renewRes] = await Promise.all([
			courseApi.scheduleWeek({ startDate: todayStart(), endDate: todayEnd() }).catch(() => []),
			studentApi.orderPage({ current: 1, size: 5, status: 'UNPAID' }).catch(() => ({})),
			studentApi.orderPage({ current: 1, size: 20, status: 'PAID' }).catch(() => ({})),
			crmApi.leadPage({ current: 1, size: 5, nextFollowBefore: todayEnd(), excludeStatus: 'ENROLLED,LOST' }).catch(() => ({})),
			studentApi.renewWarnPage({ current: 1, size: 5, status: 'PENDING' }).catch(() => ({}))
		])
		const scheduleList = scheduleRes || []
		const unpaidTotal = unpaidRes?.total || 0
		const leadTotal = leadRes?.total || 0
		const paidList = (paidRes?.records || []).filter((item) => item.payTime && String(item.payTime).slice(0, 10) === today())
		const todayPaid = paidList.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0)

		statList.value = [
			{ key: 'paid', label: '今日收款', value: `¥${todayPaid.toLocaleString('zh-CN')}`, sub: `${paidList.length} 笔`, color: '#2ba471', path: '/app/cashier/orders' },
			{ key: 'class', label: '今日课节', value: `${scheduleList.length} 节`, sub: '待点名优先', color: '#4bb964', path: '/app/teach/schedule' },
			{ key: 'unpaid', label: '待收款', value: `${unpaidTotal} 笔`, sub: '需跟进', color: '#d89614', path: '/app/cashier/orders' },
			{ key: 'lead', label: '待跟进线索', value: `${leadTotal} 条`, sub: '今日到期', color: '#6f5bd8', path: '/app/enroll/leads' }
		]
		todoList.value = [
			...scheduleList.map((item) => ({
				key: `s-${item.id}`,
				time: `${String(item.startTime || '').slice(0, 5)}`,
				title: `${item.className || '未命名班级'} · ${item.classroom || '未定教室'}`,
				sub: item.attendStatus === 'DONE' ? '已点名' : '待点名',
				path: `/app/teach/attend?scheduleId=${item.id}&classId=${item.classId || ''}`,
				actionLabel: item.attendStatus === 'DONE' ? undefined : '去点名',
				action: 'attend',
				targetId: item.id,
				classId: item.classId
			})),
			...(leadRes?.records || []).slice(0, 3).map((item) => ({
				key: `l-${item.id}`,
				time: String(item.nextFollowTime || '').slice(5, 16),
				title: `${item.name}（${item.parentPhone || '无电话'}）`,
				sub: '线索待跟进',
				path: `/app/enroll/lead?id=${item.id}`,
				actionLabel: '跟进',
				action: 'follow',
				targetId: item.id
			})),
			...(renewRes?.records || []).slice(0, 3).map((item) => ({
				key: `w-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} · 续费预警`,
				sub: item.warnReason === 'LESSON_LOW' ? '课时不足，请及时续费' : '课包即将到期',
				path: '/app/student/renew',
				actionLabel: '处理',
				action: 'renew'
			}))
		]
		quickList.value = filterQuick([
			{ key: 'enroll', text: '录入学员', icon: UserAddOutlined, color: '#fff1e6', action: 'enroll', perm: '/student/add' },
			{ key: 'schedule', text: '我的课表', icon: CalendarOutlined, color: '#eaf2fd', path: '/app/teach/schedule' },
			{ key: 'attend', text: '点名消课', icon: AuditOutlined, color: '#f3edfd', path: '/app/teach/attend' },
			{ key: 'students', text: '学员名册', icon: TeamOutlined, color: '#e8f7f0', path: '/app/teach/students' },
			{ key: 'leads', text: '招生线索', icon: PhoneOutlined, color: '#e8f4f8', path: '/app/enroll/leads' },
			{ key: 'renew', text: '续费预警', icon: AuditOutlined, color: '#fdf3e6', path: '/app/student/renew' },
			{ key: 'report', text: '经营报表', icon: BarChartOutlined, color: '#fdf3e6', path: '/app/report' }
		])
	}

	const loadEnroll = async () => {
		const [leadRes, trialRes, unpaidRes] = await Promise.all([
			crmApi.leadPage({ current: 1, size: 5, excludeStatus: 'ENROLLED,LOST' }).catch(() => ({})),
			crmApi.trialPage({ current: 1, size: 5 }).catch(() => ({})),
			studentApi.orderPage({ current: 1, size: 5, status: 'UNPAID' }).catch(() => ({}))
		])
		const newLead = leadRes?.records?.filter((item) => item.status === 'NEW').length || 0
		statList.value = [
			{ key: 'lead', label: '在跟线索', value: `${leadRes?.total || 0} 条`, sub: `新线索 ${newLead}`, color: '#6f5bd8', path: '/app/enroll/leads' },
			{ key: 'trial', label: '试听预约', value: `${trialRes?.total || 0} 场`, sub: '待到店', color: '#4bb964', path: '/app/enroll/leads' },
			{ key: 'unpaid', label: '待收款', value: `${unpaidRes?.total || 0} 笔`, sub: '需跟进', color: '#d89614', path: '/app/cashier/orders' },
			{ key: 'report', label: '招生转化', value: '查看', sub: '漏斗/来源', color: '#2ba471', path: '/app/report' }
		]
		todoList.value = [
			...(leadRes?.records || []).map((item) => ({
				key: `l-${item.id}`,
				time: String(item.nextFollowTime || '').slice(5, 16) || '待安排',
				title: `${item.name}（${item.parentPhone || '无电话'}）`,
				sub: item.status === 'NEW' ? '新线索待首次跟进' : '继续跟进',
				path: `/app/enroll/lead?id=${item.id}`,
				actionLabel: '跟进',
				action: 'follow',
				targetId: item.id
			})),
			...(unpaidRes?.records || []).slice(0, 3).map((item) => ({
				key: `o-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} · ${item.packageName || '课包'}`,
				sub: `待收 ¥${Number(item.amount || 0).toFixed(2)}`,
				path: '/app/cashier/orders?status=UNPAID',
				actionLabel: '收款',
				action: 'pay',
				targetId: item.id
			}))
		]
		quickList.value = filterQuick([
			{ key: 'enroll', text: '录入学员', icon: UserAddOutlined, color: '#fff1e6', action: 'enroll', perm: '/student/add' },
			{ key: 'leads', text: '我的线索', icon: PhoneOutlined, color: '#f3edfd', path: '/app/enroll/leads' },
			{ key: 'students', text: '学员名册', icon: TeamOutlined, color: '#e8f7f0', path: '/app/teach/students' },
			{ key: 'order', text: '收款台', icon: WalletOutlined, color: '#eaf2fd', path: '/app/cashier/orders' },
			{ key: 'report', text: '招生报表', icon: BarChartOutlined, color: '#fdf3e6', path: '/app/report' }
		])
	}

	const loadCashier = async () => {
		const [unpaidRes, paidRes, refundRes] = await Promise.all([
			studentApi.orderPage({ current: 1, size: 5, status: 'UNPAID' }).catch(() => ({})),
			studentApi.orderPage({ current: 1, size: 20, status: 'PAID' }).catch(() => ({})),
			studentApi.refundPage({ current: 1, size: 5, status: 'PENDING' }).catch(() => ({}))
		])
		const paidList = (paidRes?.records || []).filter((item) => item.payTime && String(item.payTime).slice(0, 10) === today())
		const todayPaid = paidList.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0)
		statList.value = [
			{ key: 'paid', label: '今日收款', value: `¥${todayPaid.toLocaleString('zh-CN')}`, sub: `${paidList.length} 笔`, color: '#2ba471', path: '/app/cashier/orders' },
			{ key: 'unpaid', label: '待收款', value: `${unpaidRes?.total || 0} 笔`, sub: '需催收', color: '#d89614', path: '/app/cashier/orders' },
			{ key: 'refund', label: '待审批退费', value: `${refundRes?.total || 0} 笔`, sub: '待处理', color: '#e5484d', path: '/app/cashier/orders' },
			{ key: 'report', label: '收款报表', value: '查看', sub: '趋势/渠道', color: '#4bb964', path: '/app/report' }
		]
		todoList.value = [
			...(unpaidRes?.records || []).map((item) => ({
				key: `o-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} · ${item.packageName || '课包'}`,
				sub: `待收 ¥${Number(item.amount || 0).toFixed(2)}`,
				path: '/app/cashier/orders?status=UNPAID',
				actionLabel: '收款',
				action: 'pay',
				targetId: item.id
			})),
			...(refundRes?.records || []).map((item) => ({
				key: `r-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} 退费申请`,
				sub: `可退 ${item.refundLessons || 0} 课时`,
				path: '/app/cashier/orders?tab=refund',
				actionLabel: '审批',
				action: 'refund',
				targetId: item.id
			}))
		]
		quickList.value = filterQuick([
			{ key: 'order', text: '收款台', icon: WalletOutlined, color: '#eaf2fd', path: '/app/cashier/orders' },
			{ key: 'refund', text: '退费审批', icon: AuditOutlined, color: '#fdedf0', path: '/app/cashier/orders?tab=refund' },
			{ key: 'renew', text: '续费预警', icon: AuditOutlined, color: '#fdf3e6', path: '/app/student/renew' },
			{ key: 'report', text: '收款报表', icon: BarChartOutlined, color: '#e8f7f0', path: '/app/report' },
			{ key: 'mine', text: '我的', icon: UserOutlined, color: '#f3edfd', path: '/app/mine' }
		])
	}

	/** 经营总览：保持 APP 的指标 + 待办结构，功能覆盖工作台 */
	const loadOverview = async () => {
		const [unpaidRes, refundRes, warnRes, leadRes, scheduleRes, paidRes] = await Promise.all([
			studentApi.orderPage({ current: 1, size: 5, status: 'UNPAID' }).catch(() => ({})),
			studentApi.refundPage({ current: 1, size: 5, status: 'PENDING' }).catch(() => ({})),
			studentApi.renewWarnPage({ current: 1, size: 5, status: 'PENDING' }).catch(() => ({})),
			crmApi.leadPage({ current: 1, size: 5, nextFollowBefore: todayEnd(), excludeStatus: 'ENROLLED,LOST' }).catch(() => ({})),
			courseApi.scheduleWeek({ startDate: todayStart(), endDate: todayEnd() }).catch(() => []),
			studentApi.orderPage({ current: 1, size: 20, status: 'PAID' }).catch(() => ({}))
		])
		const scheduleList = scheduleRes || []
		const paidList = (paidRes?.records || []).filter((item) => item.payTime && String(item.payTime).slice(0, 10) === today())
		const todayPaid = paidList.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0)
		statList.value = [
			{ key: 'paid', label: '今日收款', value: `¥${todayPaid.toLocaleString('zh-CN')}`, sub: `${paidList.length} 笔`, color: '#2ba471', path: '/app/cashier/orders' },
			{ key: 'unpaid', label: '待收款', value: `${unpaidRes?.total || 0} 笔`, sub: '需跟进', color: '#d89614', path: '/app/cashier/orders' },
			{ key: 'class', label: '今日课节', value: `${scheduleList.length} 节`, sub: '待点名优先', color: '#4bb964', path: '/app/teach/schedule' },
			{ key: 'lead', label: '待跟进线索', value: `${leadRes?.total || 0} 条`, sub: '今日到期', color: '#6f5bd8', path: '/app/enroll/leads' }
		]
		todoList.value = [
			...(unpaidRes?.records || []).slice(0, 3).map((item) => ({
				key: `o-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} · ${item.packageName || '课包'}`,
				sub: `待收 ¥${Number(item.amount || 0).toFixed(2)}`,
				path: '/app/cashier/orders?status=UNPAID',
				actionLabel: '收款',
				action: 'pay',
				targetId: item.id
			})),
			...scheduleList.slice(0, 3).map((item) => ({
				key: `s-${item.id}`,
				time: `${String(item.startTime || '').slice(0, 5)}`,
				title: `${item.className || '未命名班级'} · ${item.classroom || '未定教室'}`,
				sub: item.attendStatus === 'DONE' ? '已点名' : '待点名',
				path: `/app/teach/attend?scheduleId=${item.id}&classId=${item.classId || ''}`,
				actionLabel: item.attendStatus === 'DONE' ? undefined : '去点名',
				action: 'attend',
				targetId: item.id,
				classId: item.classId
			})),
			...(refundRes?.records || []).slice(0, 3).map((item) => ({
				key: `r-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} 退费申请`,
				sub: `可退 ${item.refundLessons || 0} 课时`,
				path: '/app/cashier/orders?tab=refund',
				actionLabel: '审批',
				action: 'refund',
				targetId: item.id
			})),
			...(leadRes?.records || []).slice(0, 3).map((item) => ({
				key: `l-${item.id}`,
				time: String(item.nextFollowTime || '').slice(5, 16),
				title: `${item.name}（${item.parentPhone || '无电话'}）`,
				sub: '线索待跟进',
				path: `/app/enroll/lead?id=${item.id}`,
				actionLabel: '跟进',
				action: 'follow',
				targetId: item.id
			})),
			...(warnRes?.records || []).slice(0, 3).map((item) => ({
				key: `w-${item.id}`,
				time: String(item.createTime || '').slice(5, 16),
				title: `${item.studentName || '学员'} · 续费预警`,
				sub: item.warnReason === 'LESSON_LOW' ? '课时不足，请及时续费' : '课包即将到期',
				path: '/app/student/renew',
				actionLabel: '处理',
				action: 'renew'
			}))
		]
		quickList.value = filterQuick([
			{ key: 'enroll', text: '录入学员', icon: UserAddOutlined, color: '#fff1e6', action: 'enroll', perm: '/student/add' },
			{ key: 'order', text: '收款台', icon: WalletOutlined, color: '#eaf2fd', path: '/app/cashier/orders' },
			{ key: 'schedule', text: '我的课表', icon: CalendarOutlined, color: '#f3edfd', path: '/app/teach/schedule' },
			{ key: 'attend', text: '点名消课', icon: AuditOutlined, color: '#fdf3e6', path: '/app/teach/attend' },
			{ key: 'students', text: '学员名册', icon: TeamOutlined, color: '#e8f7f0', path: '/app/teach/students' },
			{ key: 'leads', text: '招生线索', icon: PhoneOutlined, color: '#e8f4f8', path: '/app/enroll/leads' },
			{ key: 'renew', text: '续费预警', icon: AuditOutlined, color: '#fdf3e6', path: '/app/student/renew' },
			{ key: 'report', text: '经营看板', icon: BarChartOutlined, color: '#fdedf0', path: '/app/report' }
		])
	}

	const loadAll = async () => {
		loading.value = true
		try {
			if (appStore.view === 'teach') await loadTeach()
			else if (appStore.view === 'enroll') await loadEnroll()
			else if (appStore.view === 'cashier') await loadCashier()
			else await loadOverview()
		} finally {
			loading.value = false
		}
	}

	/** 过滤当前视角不可访问的快捷入口 */
	const filterQuick = (list) => list.filter((item) => (!item.perm || hasPerm(item.perm)) && appStore.canVisit(item.path))

	const go = (path) => {
		// 越权页面不跳转（视角内不可见的入口直接忽略）
		if (path && appStore.canVisit(path)) router.push(path)
	}

	const onQuick = (item) => {
		if (item.action === 'enroll') {
			enrollOpen.value = true
			return
		}
		go(item.path)
	}

	const onTodoAction = (item) => {
		if (item.action === 'pay') {
			router.push({ path: '/app/cashier/orders', query: { status: 'UNPAID', orderId: item.targetId } })
		} else if (item.action === 'refund') {
			router.push({ path: '/app/cashier/orders', query: { tab: 'refund', refundId: item.targetId } })
		} else if (item.action === 'follow') {
			router.push({ path: '/app/enroll/lead', query: { id: item.targetId, action: 'follow' } })
		} else if (item.action === 'attend') {
			router.push({ path: '/app/teach/attend', query: { scheduleId: item.targetId, classId: item.classId } })
		} else if (item.action === 'renew') {
			router.push('/app/student/renew')
		} else {
			go(item.path)
		}
	}

	onMounted(loadAll)
	// 切换视角后重新取数
	watch(() => appStore.view, loadAll)
</script>

<style lang="less" scoped>
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
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-todo {
		display: flex;
		align-items: center;
		gap: 10px;
	}
	.m-todo-left {
		flex: 1;
		min-width: 0;
	}
	.m-todo-action {
		flex-shrink: 0;
		min-height: 36px;
	}
	.m-todo-time {
		font-size: 12px;
		color: var(--primary-6);
		font-variant-numeric: tabular-nums;
	}
	.m-todo-title {
		margin-top: 2px;
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.m-todo-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-todo-arrow {
		color: var(--text-color-tertiary);
	}
	.m-empty,
	.m-skeleton {
		height: 62px;
		color: var(--text-color-secondary);
		text-align: center;
		line-height: 38px;
	}
	.m-skeleton {
		background: linear-gradient(90deg, #f2f3f5 25%, #e9ebee 37%, #f2f3f5 63%);
		background-size: 400% 100%;
		animation: m-skeleton 1.2s ease infinite;
	}
	@keyframes m-skeleton {
		0% {
			background-position: 100% 50%;
		}
		100% {
			background-position: 0 50%;
		}
	}
	.m-quick-grid {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 10px;
	}
	.m-quick {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 4px;
		text-align: center;
	}
	.m-quick-icon {
		width: 36px;
		height: 36px;
		margin: 0 auto 6px;
		border-radius: 10px;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 17px;
		color: var(--text-color);
	}
	.m-quick-text {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
</style>
