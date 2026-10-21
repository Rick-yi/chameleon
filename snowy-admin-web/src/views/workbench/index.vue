<template>
	<div>
		<!-- 页头 -->
		<page-header title="工作台" :desc="todayLabel">
			<template #extra>
				<a-button @click="loadAll">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
			</template>
		</page-header>

		<!-- 快捷入口 -->
		<div class="ent-shortcut" style="margin-bottom: 12px">
			<div v-for="item in shortcuts" :key="item.key" class="ent-shortcut-item" @click="go(item)">
				<div class="ent-shortcut-icon">
					<component :is="item.icon" />
				</div>
				<div class="ent-shortcut-text">{{ item.text }}</div>
			</div>
		</div>

		<a-spin :spinning="loading">
			<a-row :gutter="12">
				<!-- 待收款 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="ent-card">
						<template #title>
							<div class="ent-card-title">
								<span>待收款订单</span>
								<a-tag :color="unpaid.total > 0 ? 'orange' : 'green'">{{ unpaid.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/order', query: { status: 'UNPAID' } })">全部 ›</a>
						</template>
						<div v-if="!unpaid.list.length" class="ent-empty">没有待收款订单</div>
						<ul v-else class="ent-list">
							<li v-for="item in unpaid.list" :key="item.id" class="ent-list-item" @click="go({ path: '/student/order', query: { status: 'UNPAID' } })">
								<div>
									<div class="ent-list-title">{{ item.studentName || '学员' }}</div>
									<div class="ent-list-sub">{{ item.packageName }}</div>
								</div>
								<div class="ent-list-extra">
									<span class="ent-list-amount">¥{{ item.amount }}</span>
									<span class="ent-list-time">{{ formatTime(item.createTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 今日课节 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="ent-card">
						<template #title>
							<div class="ent-card-title">
								<span>今日课节</span>
								<a-tag :color="todaySchedule.length ? 'blue' : 'green'">{{ todaySchedule.length }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/course/schedule' })">课表 ›</a>
						</template>
						<div v-if="!todaySchedule.length" class="ent-empty">今天没有排课</div>
						<ul v-else class="ent-list">
							<li v-for="item in todaySchedule" :key="item.id" class="ent-list-item" @click="goAttend(item)">
								<div>
									<div class="ent-list-title">{{ item.className }}</div>
									<div class="ent-list-sub">
										{{ item.startTime }} - {{ item.endTime }} · {{ item.classroom || '未定教室' }}
										{{ item.teacherName ? ' · ' + item.teacherName : '' }}
									</div>
								</div>
								<div class="ent-list-extra">
									<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 待审批退费 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="ent-card">
						<template #title>
							<div class="ent-card-title">
								<span>待审批退费</span>
								<a-tag :color="pendingRefund.total > 0 ? 'red' : 'green'">{{ pendingRefund.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/order', query: { tab: 'refund', status: 'PENDING' } })">全部 ›</a>
						</template>
						<div v-if="!pendingRefund.list.length" class="ent-empty">没有待审批退费</div>
						<ul v-else class="ent-list">
							<li v-for="item in pendingRefund.list" :key="item.id" class="ent-list-item" @click="go({ path: '/student/order', query: { tab: 'refund', status: 'PENDING' } })">
								<div>
									<div class="ent-list-title">{{ item.studentName || '学员' }}</div>
									<div class="ent-list-sub">可退 {{ item.refundLessons }} 课时</div>
								</div>
								<div class="ent-list-extra">
									<span class="ent-list-amount">¥{{ item.refundAmount }}</span>
									<span class="ent-list-time">{{ formatTime(item.createTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 待跟进线索 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="ent-card">
						<template #title>
							<div class="ent-card-title">
								<span>待跟进线索</span>
								<a-tag :color="leadFollow.total > 0 ? 'purple' : 'green'">{{ leadFollow.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/crm', query: { todo: '1' } })">全部 ›</a>
						</template>
						<div v-if="!leadFollow.list.length" class="ent-empty">没有待跟进线索</div>
						<ul v-else class="ent-list">
							<li v-for="item in leadFollow.list" :key="item.id" class="ent-list-item" @click="go({ path: '/crm', query: { todo: '1' } })">
								<div>
									<div class="ent-list-title">{{ item.name }}</div>
									<div class="ent-list-sub">{{ item.parentPhone }} · 跟进人 {{ item.followUserName || '-' }}</div>
								</div>
								<div class="ent-list-extra">
									<span class="ent-list-time">{{ formatTime(item.nextFollowTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 续费预警 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="ent-card">
						<template #title>
							<div class="ent-card-title">
								<span>续费预警</span>
								<a-tag :color="renewWarn.total > 0 ? 'gold' : 'green'">{{ renewWarn.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/renewWarn' })">全部 ›</a>
						</template>
						<div v-if="!renewWarn.list.length" class="ent-empty">没有续费预警</div>
						<ul v-else class="ent-list">
							<li v-for="item in renewWarn.list" :key="item.id" class="ent-list-item" @click="go({ path: '/student/renewWarn' })">
								<div>
									<div class="ent-list-title">{{ item.studentName || '学员' }}</div>
									<div class="ent-list-sub">
										{{ item.warnReason === 'LESSON_LOW' ? '课时不足' : '课包即将到期' }} · 剩余 {{ item.remainLessons }} 课时
									</div>
								</div>
								<div class="ent-list-extra">
									<span class="ent-list-time">{{ formatTime(item.createTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 今日收款 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="ent-card">
						<template #title>
							<div class="ent-card-title">
								<span>今日收款</span>
								<a-tag color="green">¥{{ todayPaid }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/order' })">收款台 ›</a>
						</template>
						<div v-if="!todayPaidList.length" class="ent-empty">今天还没有收款</div>
						<ul v-else class="ent-list">
							<li v-for="item in todayPaidList" :key="item.id" class="ent-list-item" @click="go({ path: '/student/order' })">
								<div>
									<div class="ent-list-title">{{ item.studentName || '学员' }}</div>
									<div class="ent-list-sub">{{ item.packageName }}</div>
								</div>
								<div class="ent-list-extra">
									<span class="ent-list-amount">¥{{ item.paidAmount }}</span>
									<span class="ent-list-time">{{ formatTime(item.payTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>
			</a-row>
		</a-spin>
	</div>
</template>

<script setup name="workbenchIndex">
	import { useRouter } from 'vue-router'
	import {
		ShoppingCartOutlined,
		WalletOutlined,
		CalendarOutlined,
		CheckSquareOutlined,
		PhoneOutlined,
		BarChartOutlined
	} from '@ant-design/icons-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import crmApi from '@/api/crm/crmApi'
	const router = useRouter()
	const loading = ref(false)

	const unpaid = ref({ total: 0, list: [] })
	const pendingRefund = ref({ total: 0, list: [] })
	const renewWarn = ref({ total: 0, list: [] })
	const leadFollow = ref({ total: 0, list: [] })
	const todaySchedule = ref([])
	const todayPaidList = ref([])
	const todayPaid = ref(0)

	const shortcuts = [
		{ key: 'order', text: '报名购课', icon: ShoppingCartOutlined, path: '/student' },
		{ key: 'pay', text: '收款台', icon: WalletOutlined, path: '/student/order' },
		{ key: 'schedule', text: '排课表', icon: CalendarOutlined, path: '/course/schedule' },
		{ key: 'attend', text: '点名消课', icon: CheckSquareOutlined, path: '/course/attend' },
		{ key: 'lead', text: '招生线索', icon: PhoneOutlined, path: '/crm' },
		{ key: 'report', text: '经营看板', icon: BarChartOutlined, path: '/report/dashboard' }
	]

	const go = (item) => {
		router.push(item.path ? { path: item.path, query: item.query } : item)
	}
	const goAttend = (item) => {
		router.push({ path: '/course/attend', query: { classId: item.classId, scheduleId: item.id } })
	}

	const pad = (n) => String(n).padStart(2, '0')
	const today = () => {
		const d = new Date()
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}
	const todayLabel = computed(() => {
		const names = '日一二三四五六'
		const d = new Date()
		return `星期${names[d.getDay()]} · ${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	})
	const formatTime = (time) => {
		if (!time) return '-'
		return String(time).slice(5, 16).replace('T', ' ')
	}
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')

	// 单个板块取数失败不影响其它板块（无权限时降级为空）
	const safe = (promise, fallback) => promise.catch(() => fallback)

	const loadAll = () => {
		loading.value = true
		const todayStr = today()
		const dayStart = `${todayStr} 00:00:00`
		const dayEnd = `${todayStr} 23:59:59`
		Promise.all([
			safe(studentApi.orderPage({ current: 1, size: 5, status: 'UNPAID' }), { total: 0, records: [] }),
			safe(studentApi.refundPage({ current: 1, size: 5, status: 'PENDING' }), { total: 0, records: [] }),
			safe(studentApi.renewWarnPage({ current: 1, size: 5, status: 'PENDING' }), { total: 0, records: [] }),
			safe(
				crmApi.leadPage({
					current: 1,
					size: 5,
					nextFollowBefore: dayEnd,
					excludeStatus: 'ENROLLED,LOST'
				}),
				{ total: 0, records: [] }
			),
			safe(courseApi.scheduleWeek({ startDate: dayStart, endDate: dayEnd }), []),
			safe(studentApi.orderPage({ current: 1, size: 5, status: 'PAID' }), { total: 0, records: [] })
		])
			.then(([order, refund, warn, lead, schedule, paid]) => {
				unpaid.value = { total: order.total || 0, list: order.records || [] }
				pendingRefund.value = { total: refund.total || 0, list: refund.records || [] }
				renewWarn.value = { total: warn.total || 0, list: warn.records || [] }
				leadFollow.value = { total: lead.total || 0, list: lead.records || [] }
				todaySchedule.value = schedule || []
				todayPaidList.value = (paid.records || []).filter((item) => item.payTime && String(item.payTime).slice(0, 10) === todayStr)
				todayPaid.value = todayPaidList.value.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0)
			})
			.finally(() => (loading.value = false))
	}
	loadAll()
</script>

<style lang="less" scoped>
	.ent-card-title {
		display: flex;
		align-items: center;
		gap: 8px;
	}
</style>
