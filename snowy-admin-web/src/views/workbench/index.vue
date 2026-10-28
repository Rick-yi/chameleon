<template>
	<div>
		<!-- 页头 -->
		<page-header title="工作台" :desc="todayLabel">
			<template #extra>
				<period-switch v-model="period" style="margin-right: 8px" @change="loadAll" />
				<a-button v-if="hasPerm('/student/add')" type="primary" @click="enrollOpen = true">
					<template #icon><user-add-outlined /></template>
					录入学员
				</a-button>
				<a-button @click="loadAll">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
			</template>
		</page-header>

		<!-- 经营指标（随日/月/年切换） -->
		<a-row :gutter="12" class="wb-kpi">
			<a-col :xs="12" :sm="8" :md="8" :xl="4">
				<div class="ent-stat kpi-link" @click="go({ path: '/report/finance', query: { tab: 'payment', range: period.toLowerCase() } })">
					<div class="ent-stat-label">{{ periodScopeLabel }}收款</div>
					<div class="ent-stat-value">¥{{ fmtMoney(periodSummary.paidAmount) }}</div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="8" :md="8" :xl="4">
				<div class="ent-stat kpi-link" @click="go({ path: '/report/finance', query: { tab: 'refund', range: period.toLowerCase() } })">
					<div class="ent-stat-label">{{ periodScopeLabel }}退费</div>
					<div class="ent-stat-value" style="color: var(--danger-text)">¥{{ fmtMoney(periodSummary.refundAmount) }}</div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="8" :md="8" :xl="4">
				<div class="ent-stat kpi-link" @click="go({ path: '/student' })">
					<div class="ent-stat-label">{{ periodScopeLabel }}新增学员</div>
					<div class="ent-stat-value">{{ periodSummary.newStudentCount || 0 }}</div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="8" :md="8" :xl="4">
				<div class="ent-stat kpi-link" @click="go({ path: '/report/academic' })">
					<div class="ent-stat-label">{{ periodScopeLabel }}课消</div>
					<div class="ent-stat-value">
						{{ fmtMoney(periodSummary.attendLessons) }}
						<span class="wb-unit">课时</span>
					</div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="8" :md="8" :xl="4">
				<div class="ent-stat kpi-link" @click="go({ path: '/student/order', query: { status: 'UNPAID' } })">
					<div class="ent-stat-label">待收款订单</div>
					<div class="ent-stat-value" :style="{ color: unpaid.total > 0 ? 'var(--warning-text)' : undefined }">
						{{ unpaid.total || 0 }}
						<span style="font-size: 13px; color: var(--text-color-tertiary)">笔</span>
					</div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="8" :md="8" :xl="4">
				<div class="ent-stat kpi-link" @click="go({ path: '/student' })">
					<div class="ent-stat-label">在读学员</div>
					<div class="ent-stat-value">{{ periodSummary.studyingStudentCount || 0 }}</div>
				</div>
			</a-col>
		</a-row>

		<!-- 今日教学指标 -->
		<a-row :gutter="12" class="wb-kpi">
			<a-col :xs="12" :sm="12" :md="6" :xl="6">
				<div class="ent-stat kpi-link" @click="go({ path: '/course/schedule' })">
					<div class="ent-stat-label">今日应到</div>
					<div class="ent-stat-value">{{ teaching.expected }}<span class="wb-unit">人</span></div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="12" :md="6" :xl="6">
				<div class="ent-stat kpi-link" @click="go({ path: '/course/attend' })">
					<div class="ent-stat-label">已点名</div>
					<div class="ent-stat-value">{{ teaching.attended }}<span class="wb-unit">人</span></div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="12" :md="6" :xl="6">
				<div class="ent-stat kpi-link" @click="go({ path: '/course/attend' })">
					<div class="ent-stat-label">未点名课节</div>
					<div class="ent-stat-value" :style="{ color: teaching.pending > 0 ? 'var(--warning-text)' : undefined }">
						{{ teaching.pending }}<span class="wb-unit">节</span>
					</div>
				</div>
			</a-col>
			<a-col :xs="12" :sm="12" :md="6" :xl="6">
				<div class="ent-stat kpi-link" @click="go({ path: '/course/attend' })">
					<div class="ent-stat-label">点名进度</div>
					<div class="ent-stat-value">{{ teaching.progress }}<span class="wb-unit">%</span></div>
				</div>
			</a-col>
		</a-row>

		<!-- 快捷入口 -->
		<div class="ent-shortcut" style="margin-bottom: 12px">
			<div v-for="item in visibleShortcuts" :key="item.key" class="ent-shortcut-item" @click="onShortcut(item)">
				<div class="ent-shortcut-icon" :style="{ background: item.color }">
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
								<a-tag :color="unpaid.total > 0 ? 'orange' : 'default'">{{ unpaid.total || 0 }}</a-tag>
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
									<div class="ent-list-actions">
										<a-tag v-if="overdueDays(item.createTime) > 0" color="red">逾期 {{ overdueDays(item.createTime) }} 天</a-tag>
										<a-button v-if="hasPerm('/student/order/offlinePay')" type="link" size="small" @click.stop="openPay(item)">收款</a-button>
									</div>
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
								<a-tag :color="todaySchedule.length ? 'blue' : 'default'">{{ todaySchedule.length }}</a-tag>
							</div>
						</template>
						<template #extra>
							<a @click="go({ path: '/course/schedule' })">课表 ›</a>
						</template>
						<div v-if="!todaySchedule.length" class="ent-empty">
							<div>今天没有排课</div>
							<a-space style="margin-top: 8px">
								<a-button size="small" type="primary" @click="go({ path: '/course/schedule' })">去排课</a-button>
								<a-button size="small" @click="go({ path: '/course/schedule' })">查看本周课表</a-button>
							</a-space>
						</div>
						<ul v-else class="ent-list">
							<li v-for="item in sortedTodaySchedule" :key="item.id" class="ent-list-item" @click="goAttend(item)">
								<div>
									<div class="ent-list-title">
										{{ item.className }}
										<a-tag v-if="item.attendStatus !== 'DONE'" color="red" style="margin-left: 6px">待点名</a-tag>
									</div>
									<div class="ent-list-sub">
										{{ item.startTime }} - {{ item.endTime }} · {{ item.classroom || '未定教室' }}
										{{ item.teacherName ? ' · ' + item.teacherName : '' }}
									</div>
								</div>
								<div class="ent-list-extra">
									<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
									<span class="ent-list-time">应到 {{ item.studentCount || 0 }} · 已点 {{ item.attendCount || 0 }}</span>
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
								<a-tag :color="pendingRefund.total > 0 ? 'red' : 'default'">{{ pendingRefund.total || 0 }}</a-tag>
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
									<div class="ent-list-actions">
										<a-tag v-if="overdueDays(item.createTime) > 0" color="red">逾期 {{ overdueDays(item.createTime) }} 天</a-tag>
										<a-button v-if="hasPerm('/student/refund/approve')" type="link" size="small" @click.stop="openApprove(item)">审批</a-button>
									</div>
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
								<a-tag :color="leadFollow.total > 0 ? 'purple' : 'default'">{{ leadFollow.total || 0 }}</a-tag>
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
									<div class="ent-list-actions">
										<a-tag v-if="followOverdueDays(item.nextFollowTime) > 0" color="red">
											逾期 {{ followOverdueDays(item.nextFollowTime) }} 天
										</a-tag>
										<a-tag v-else-if="followOverdueDays(item.nextFollowTime) === 0" color="orange">今日跟进</a-tag>
										<a-button v-if="hasPerm('/crm/follow/add')" type="link" size="small" @click.stop="openFollow(item)">跟进</a-button>
									</div>
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
								<a-tag :color="renewWarn.total > 0 ? 'gold' : 'default'">{{ renewWarn.total || 0 }}</a-tag>
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

		<!-- 工作台内直接收款，处理完立即刷新待办 -->
		<a-modal v-model:open="payOpen" title="收款" :width="480" :destroy-on-close="true" @ok="onSubmitPay">
			<div class="wb-pay-amount">¥{{ Number(payForm.amount || 0).toFixed(2) }}</div>
			<a-form ref="payFormRef" :model="payForm" layout="vertical">
				<a-form-item label="订单">
					<a-input :value="`${payForm.studentName || ''} · ${payForm.packageName || ''}`" disabled />
				</a-form-item>
				<a-form-item label="收款渠道">
					<a-select v-model:value="payForm.payChannel" :options="payChannelOptions" />
				</a-form-item>
				<a-form-item label="实收金额" name="paidAmount" :rules="[{ required: true, message: '请输入实收金额' }]">
					<a-input-number v-model:value="payForm.paidAmount" :min="0.01" :precision="2" style="width: 100%" />
				</a-form-item>
				<a-form-item label="备注">
					<a-input v-model:value="payForm.remark" placeholder="如：收款人、转账流水号" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<!-- 工作台内直接审批退费，处理完立即刷新待办 -->
		<a-modal v-model:open="approveOpen" title="退费审批" :width="520" :destroy-on-close="true" :footer="null">
			<div class="wb-refund-summary">
				<div><span>学员</span><b>{{ approveForm.studentName || '-' }}</b></div>
				<div><span>可退课时</span><b>{{ approveForm.refundLessons || 0 }} 课时</b></div>
				<div><span>预计退款</span><b>¥{{ Number(approveForm.refundAmount || 0).toFixed(2) }}</b></div>
			</div>
			<a-alert v-if="approveForm.applyRemark" type="info" show-icon :message="`申请原因：${approveForm.applyRemark}`" style="margin-bottom: 12px" />
			<a-form layout="vertical">
				<a-form-item label="审批意见">
					<a-input v-model:value="approveForm.opinion" placeholder="请输入审批意见" allow-clear />
				</a-form-item>
				<div class="wb-approve-actions">
					<a-button type="primary" @click="onApprove(true)">通过（冻结账户）</a-button>
					<a-button danger @click="onApprove(false)">驳回</a-button>
				</div>
			</a-form>
		</a-modal>

		<!-- 工作台内直接写跟进，保存后刷新待办 -->
		<a-modal v-model:open="followOpen" :title="`跟进 · ${followForm.name || ''}`" :width="520" :destroy-on-close="true" @ok="onSubmitFollow">
			<a-form ref="followFormRef" :model="followForm" layout="vertical">
				<a-form-item label="跟进方式">
					<a-select v-model:value="followForm.followType" :options="followTypeOptions" />
				</a-form-item>
				<a-form-item label="跟进内容" name="content" :rules="[{ required: true, message: '请输入跟进内容' }]">
					<a-textarea v-model:value="followForm.content" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="沟通内容、家长反馈、下一步计划" />
				</a-form-item>
				<a-form-item label="下次跟进时间">
					<a-date-picker v-model:value="followForm.nextFollowTime" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
				</a-form-item>
				<a-form-item label="更新线索状态">
					<a-select v-model:value="followForm.status" placeholder="默认置为已跟进" :options="leadStatusOptions" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<enrollment-flow v-model:open="enrollOpen" @success="onEnrollSuccess" />
	</div>
</template>

<script setup name="workbenchIndex">
	import { useRouter } from 'vue-router'
	import { message } from 'ant-design-vue'
	import {
		ShoppingCartOutlined,
		WalletOutlined,
		CalendarOutlined,
		CheckSquareOutlined,
		PhoneOutlined,
		BarChartOutlined,
		UserAddOutlined
	} from '@ant-design/icons-vue'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'
	import PeriodSwitch from '@/components/PeriodSwitch/index.vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import crmApi from '@/api/crm/crmApi'
	import reportApi from '@/api/report/reportApi'
	import { PERIOD_LABEL, periodDateRange } from '@/utils/reportPeriod'
	const router = useRouter()
	const loading = ref(false)

	const unpaid = ref({ total: 0, list: [] })
	const pendingRefund = ref({ total: 0, list: [] })
	const renewWarn = ref({ total: 0, list: [] })
	const leadFollow = ref({ total: 0, list: [] })
	const todaySchedule = ref([])
	const todayPaidList = ref([])
	const todayPaid = ref(0)
	const period = ref('MONTH')
	const periodScopeLabel = computed(() => PERIOD_LABEL[period.value] || '本月')
	const periodRange = computed(() => {
		const [start, end] = periodDateRange(period.value)
		return { startTime: `${start} 00:00:00`, endTime: `${end} 23:59:59` }
	})
	// 当前所选周期的经营概览（财务看板汇总，取不到时降级为空）
	const periodSummary = ref({})

	const enrollOpen = ref(false)
	const payOpen = ref(false)
	const payFormRef = ref()
	const payForm = ref({})
	const payChannelOptions = [
		{ value: 'OFFLINE', label: '线下转账' },
		{ value: 'CASH', label: '现金' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'ALIPAY', label: '支付宝' }
	]
	const approveOpen = ref(false)
	const approveForm = ref({})
	const followOpen = ref(false)
	const followFormRef = ref()
	const followForm = ref({})
	const followTypeOptions = [
		{ value: 'PHONE', label: '电话' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'VISIT', label: '到店' }
	]
	const leadStatusOptions = [
		{ value: 'NEW', label: '新线索' },
		{ value: 'FOLLOWED', label: '已跟进' },
		{ value: 'TRIALED', label: '已试听' },
		{ value: 'ENROLLED', label: '已报名' },
		{ value: 'LOST', label: '已流失' }
	]
	const shortcuts = [
		{ key: 'quickEnroll', text: '录入学员', icon: UserAddOutlined, color: '#fff1e6', action: 'quickEnroll', perm: '/student/add' },
		{ key: 'order', text: '报名购课', icon: ShoppingCartOutlined, path: '/student', color: '#eaf2fd' },
		{ key: 'pay', text: '收款台', icon: WalletOutlined, path: '/student/order', color: '#e8f7f0' },
		{ key: 'schedule', text: '排课表', icon: CalendarOutlined, path: '/course/schedule', color: '#f3edfd' },
		{ key: 'attend', text: '点名消课', icon: CheckSquareOutlined, path: '/course/attend', color: '#fdf3e6' },
		{ key: 'lead', text: '招生线索', icon: PhoneOutlined, path: '/crm', color: '#e8f4f8' },
		{ key: 'report', text: '经营看板', icon: BarChartOutlined, path: '/report/dashboard', color: '#fdedf0' }
	]

	const visibleShortcuts = computed(() => shortcuts.filter((item) => !item.perm || hasPerm(item.perm)))
	const go = (item) => {
		router.push(item.path ? { path: item.path, query: item.query } : item)
	}
	const onShortcut = (item) => {
		if (item.action === 'quickEnroll') {
			enrollOpen.value = true
			return
		}
		go(item)
	}
	const onEnrollSuccess = () => {
		loadAll()
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
	const fmtMoney = (value) => {
		const num = Number(value || 0)
		return num.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
	}
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')

	// 今日教学汇总：应到/已点名/未点名课节/点名进度
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
	// 待点名的课节排前面
	const sortedTodaySchedule = computed(() => {
		const weight = (status) => (status === 'NONE' ? 0 : status === 'PARTIAL' ? 1 : 2)
		return [...todaySchedule.value].sort((a, b) => {
			const byStatus = weight(a.attendStatus) - weight(b.attendStatus)
			return byStatus !== 0 ? byStatus : String(a.startTime || '').localeCompare(String(b.startTime || ''))
		})
	})
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
	const followOverdueDays = overdueDays

	const openPay = (item) => {
		payForm.value = {
			orderId: item.id,
			studentName: item.studentName,
			packageName: item.packageName,
			amount: item.amount,
			paidAmount: Number(item.amount || 0),
			payChannel: 'OFFLINE',
			remark: ''
		}
		payOpen.value = true
	}
	const onSubmitPay = () => {
		payFormRef.value.validate().then(() => {
			studentApi
				.orderOfflinePay({
					orderId: payForm.value.orderId,
					paidAmount: payForm.value.paidAmount,
					payChannel: payForm.value.payChannel,
					remark: payForm.value.remark
				})
				.then(() => {
					message.success('收款成功，课时已入账')
					payOpen.value = false
					loadAll()
				})
		})
	}
	const openApprove = (item) => {
		approveForm.value = {
			refundId: item.id,
			opinion: '',
			studentName: item.studentName,
			refundLessons: item.refundLessons,
			refundAmount: item.refundAmount,
			applyRemark: item.applyRemark
		}
		approveOpen.value = true
	}
	const onApprove = (pass) => {
		studentApi
			.refundApprove({
				refundId: approveForm.value.refundId,
				pass,
				opinion: approveForm.value.opinion
			})
			.then(() => {
				message.success(pass ? '已通过退费，账户已冻结' : '已驳回退费')
				approveOpen.value = false
				loadAll()
			})
	}
	// 线索跟进直接在工作台完成，保存后刷新待办，不再跳转 CRM
	const openFollow = (item) => {
		followForm.value = {
			leadId: item.id,
			name: item.name,
			followType: 'PHONE',
			content: '',
			nextFollowTime: undefined,
			status: undefined
		}
		followOpen.value = true
	}
	const onSubmitFollow = () => {
		followFormRef.value.validate().then(() => {
			crmApi
				.followAdd({
					leadId: followForm.value.leadId,
					followType: followForm.value.followType,
					content: followForm.value.content,
					nextFollowTime: followForm.value.nextFollowTime,
					status: followForm.value.status
				})
				.then(() => {
					message.success('跟进已保存')
					followOpen.value = false
					loadAll()
				})
		})
	}

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
			safe(studentApi.orderPage({ current: 1, size: 20, status: 'PAID' }), { total: 0, records: [] }),
			safe(reportApi.dashboard({ ...periodRange.value, dimension: period.value }), {}),
			safe(reportApi.dashboard({ startTime: dayStart, endTime: dayEnd }), null)
		])
			.then(([order, refund, warn, lead, schedule, paid, periodDashboard, todayDashboard]) => {
				unpaid.value = { total: order.total || 0, list: order.records || [] }
				pendingRefund.value = { total: refund.total || 0, list: refund.records || [] }
				renewWarn.value = { total: warn.total || 0, list: warn.records || [] }
				leadFollow.value = { total: lead.total || 0, list: lead.records || [] }
				todaySchedule.value = schedule || []
				todayPaidList.value = (paid.records || []).filter((item) => item.payTime && String(item.payTime).slice(0, 10) === todayStr)
				const paidFromList = todayPaidList.value.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0)
				// 今日收款以报表日汇总为准，避免只统计到前几条订单；取不到时退回列表合计
				todayPaid.value = Number(todayDashboard?.summary?.paidAmount ?? paidFromList)
				periodSummary.value = periodDashboard?.summary || {}
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
	/* 空态与列表左对齐，避免卡片里出现居中的孤零零一句话 */
	.ent-card :deep(.ent-empty) {
		text-align: left;
		padding: 10px 0;
		color: var(--text-color-secondary);
	}
	.wb-kpi {
		margin-bottom: 12px;
	}
	.wb-unit {
		margin-left: 2px;
		font-size: 13px;
		color: var(--text-color-tertiary);
		font-weight: 400;
	}
	.ent-list-actions {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 6px;
		margin-top: 2px;
	}
	.wb-pay-amount {
		text-align: center;
		font-size: 28px;
		font-weight: 600;
		color: var(--text-color);
		margin-bottom: 12px;
		font-variant-numeric: tabular-nums;
	}
	.wb-refund-summary {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 8px;
		margin-bottom: 12px;
		padding: 10px 12px;
		border-radius: var(--radius-control);
		background: var(--background-color-light);
		div {
			display: flex;
			flex-direction: column;
			gap: 2px;
		}
		span {
			font-size: 11px;
			color: var(--text-color-secondary);
		}
		b {
			font-size: 14px;
			color: var(--text-color);
		}
	}
	.wb-approve-actions {
		display: flex;
		justify-content: flex-end;
		gap: 8px;
	}
</style>
