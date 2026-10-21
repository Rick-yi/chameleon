<template>
	<div>
		<!-- 快捷入口 -->
		<a-card :bordered="false" style="margin-bottom: 16px">
			<div class="wb-shortcut">
				<div v-for="item in shortcuts" :key="item.key" class="wb-shortcut-item" @click="go(item)">
					<div class="wb-shortcut-icon" :style="{ background: item.color }">
						{{ item.icon }}
					</div>
					<div class="wb-shortcut-text">{{ item.text }}</div>
				</div>
			</div>
		</a-card>

		<a-spin :spinning="loading">
			<a-row :gutter="16">
				<!-- 待收款 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="wb-card">
						<template #title>
							<div class="wb-card-title">
								<span>待收款订单</span>
								<a-tag :color="unpaid.total > 0 ? 'orange' : 'green'">{{ unpaid.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/order', query: { status: 'UNPAID' } })">全部 ›</a>
						</template>
						<a-empty v-if="!unpaid.list.length" description="没有待收款订单" :image="simpleImage" />
						<ul v-else class="wb-list">
							<li v-for="item in unpaid.list" :key="item.id" @click="go({ path: '/student/order', query: { status: 'UNPAID' } })">
								<div class="wb-list-main">
									<span class="wb-list-title">{{ item.studentName || '学员' }}</span>
									<span class="wb-list-sub">{{ item.packageName }}</span>
								</div>
								<div class="wb-list-extra">
									<span class="wb-list-amount">¥{{ item.amount }}</span>
									<span class="wb-list-time">{{ formatTime(item.createTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 今日课节 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="wb-card">
						<template #title>
							<div class="wb-card-title">
								<span>今日课节</span>
								<a-tag :color="todaySchedule.length ? 'blue' : 'green'">{{ todaySchedule.length }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/course/schedule' })">课表 ›</a>
						</template>
						<a-empty v-if="!todaySchedule.length" description="今天没有排课" :image="simpleImage" />
						<ul v-else class="wb-list">
							<li v-for="item in todaySchedule" :key="item.id" @click="goAttend(item)">
								<div class="wb-list-main">
									<span class="wb-list-title">{{ item.className }}</span>
									<span class="wb-list-sub">
										{{ item.startTime }} - {{ item.endTime }} · {{ item.classroom || '未定教室' }}
										{{ item.teacherName ? ' · ' + item.teacherName : '' }}
									</span>
								</div>
								<div class="wb-list-extra">
									<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 待审批退费 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="wb-card">
						<template #title>
							<div class="wb-card-title">
								<span>待审批退费</span>
								<a-tag :color="pendingRefund.total > 0 ? 'red' : 'green'">{{ pendingRefund.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/order', query: { tab: 'refund', status: 'PENDING' } })">全部 ›</a>
						</template>
						<a-empty v-if="!pendingRefund.list.length" description="没有待审批退费" :image="simpleImage" />
						<ul v-else class="wb-list">
							<li v-for="item in pendingRefund.list" :key="item.id" @click="go({ path: '/student/order', query: { tab: 'refund', status: 'PENDING' } })">
								<div class="wb-list-main">
									<span class="wb-list-title">{{ item.studentName || '学员' }}</span>
									<span class="wb-list-sub">可退 {{ item.refundLessons }} 课时</span>
								</div>
								<div class="wb-list-extra">
									<span class="wb-list-amount">¥{{ item.refundAmount }}</span>
									<span class="wb-list-time">{{ formatTime(item.createTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 待跟进线索 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="wb-card">
						<template #title>
							<div class="wb-card-title">
								<span>待跟进线索</span>
								<a-tag :color="leadFollow.total > 0 ? 'purple' : 'green'">{{ leadFollow.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/crm', query: { todo: '1' } })">全部 ›</a>
						</template>
						<a-empty v-if="!leadFollow.list.length" description="没有待跟进线索" :image="simpleImage" />
						<ul v-else class="wb-list">
							<li v-for="item in leadFollow.list" :key="item.id" @click="go({ path: '/crm', query: { todo: '1' } })">
								<div class="wb-list-main">
									<span class="wb-list-title">{{ item.name }}</span>
									<span class="wb-list-sub">{{ item.parentPhone }} · 跟进人 {{ item.followUserName || '-' }}</span>
								</div>
								<div class="wb-list-extra">
									<span class="wb-list-time">{{ formatTime(item.nextFollowTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 续费预警 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="wb-card">
						<template #title>
							<div class="wb-card-title">
								<span>续费预警</span>
								<a-tag :color="renewWarn.total > 0 ? 'gold' : 'green'">{{ renewWarn.total || 0 }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/student/renewWarn' })">全部 ›</a>
						</template>
						<a-empty v-if="!renewWarn.list.length" description="没有续费预警" :image="simpleImage" />
						<ul v-else class="wb-list">
							<li v-for="item in renewWarn.list" :key="item.id" @click="go({ path: '/student/renewWarn' })">
								<div class="wb-list-main">
									<span class="wb-list-title">{{ item.studentName || '学员' }}</span>
									<span class="wb-list-sub">
										{{ item.warnReason === 'LESSON_LOW' ? '课时不足' : '课包即将到期' }} · 剩余 {{ item.remainLessons }} 课时
									</span>
								</div>
								<div class="wb-list-extra">
									<span class="wb-list-time">{{ formatTime(item.createTime) }}</span>
								</div>
							</li>
						</ul>
					</a-card>
				</a-col>

				<!-- 今日收款概览 -->
				<a-col :xs="24" :xl="12">
					<a-card :bordered="false" class="wb-card">
						<template #title>
							<div class="wb-card-title">
								<span>今日收款</span>
								<a-tag color="green">¥{{ todayPaid }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/finance' })">财务 ›</a>
						</template>
						<a-empty v-if="!todayPaidList.length" description="今天还没有收款" :image="simpleImage" />
						<ul v-else class="wb-list">
							<li v-for="item in todayPaidList" :key="item.id" @click="go({ path: '/finance' })">
								<div class="wb-list-main">
									<span class="wb-list-title">{{ item.studentName || '学员' }}</span>
									<span class="wb-list-sub">{{ item.packageName }}</span>
								</div>
								<div class="wb-list-extra">
									<span class="wb-list-amount">¥{{ item.paidAmount }}</span>
									<span class="wb-list-time">{{ formatTime(item.payTime) }}</span>
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
	import { Empty } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import crmApi from '@/api/crm/crmApi'
	const router = useRouter()
	const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
	const loading = ref(false)

	const unpaid = ref({ total: 0, list: [] })
	const pendingRefund = ref({ total: 0, list: [] })
	const renewWarn = ref({ total: 0, list: [] })
	const leadFollow = ref({ total: 0, list: [] })
	const todaySchedule = ref([])
	const todayPaidList = ref([])
	const todayPaid = ref(0)

	const shortcuts = [
		{ key: 'order', text: '报名购课', icon: '🛒', color: '#1677ff', path: '/student' },
		{ key: 'pay', text: '收款台', icon: '💰', color: '#52c41a', path: '/student/order' },
		{ key: 'schedule', text: '排课表', icon: '📅', color: '#722ed1', path: '/course/schedule' },
		{ key: 'attend', text: '点名消课', icon: '✅', color: '#13c2c2', path: '/course/attend' },
		{ key: 'lead', text: '招生线索', icon: '📞', color: '#fa8c16', path: '/crm' },
		{ key: 'report', text: '经营看板', icon: '📊', color: '#eb2f96', path: '/report/dashboard' }
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
	const formatTime = (time) => {
		if (!time) return '-'
		return String(time).slice(5, 16).replace('T', ' ')
	}
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')

	// 单个板块取数失败不影响其它板块（无权限/无数据时降级为空）
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
	.wb-shortcut {
		display: flex;
		flex-wrap: wrap;
		gap: 28px;
	}
	.wb-shortcut-item {
		display: flex;
		align-items: center;
		gap: 10px;
		cursor: pointer;
		padding: 6px 10px;
		border-radius: 8px;
		transition: background 0.2s;
		&:hover {
			background: #f5f7fa;
		}
	}
	.wb-shortcut-icon {
		width: 36px;
		height: 36px;
		border-radius: 8px;
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 18px;
	}
	.wb-shortcut-text {
		font-size: 14px;
		color: rgba(0, 0, 0, 0.75);
	}
	.wb-card-title {
		display: flex;
		align-items: center;
		gap: 8px;
	}
	.wb-list {
		list-style: none;
		margin: 0;
		padding: 0;
		li {
			display: flex;
			align-items: center;
			justify-content: space-between;
			padding: 9px 4px;
			border-bottom: 1px dashed #f0f0f0;
			cursor: pointer;
			&:last-child {
				border-bottom: none;
			}
			&:hover .wb-list-title {
				color: #1677ff;
			}
		}
	}
	.wb-list-main {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}
	.wb-list-title {
		font-size: 13px;
		color: rgba(0, 0, 0, 0.88);
	}
	.wb-list-sub {
		font-size: 12px;
		color: rgba(0, 0, 0, 0.45);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
	.wb-list-extra {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 2px;
		flex-shrink: 0;
		margin-left: 12px;
	}
	.wb-list-amount {
		font-size: 13px;
		font-weight: 600;
		color: #52c41a;
	}
	.wb-list-time {
		font-size: 12px;
		color: rgba(0, 0, 0, 0.45);
	}
</style>
