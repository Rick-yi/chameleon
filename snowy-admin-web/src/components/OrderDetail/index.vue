<template>
	<a-drawer
		:open="open"
		:width="drawerWidth"
		title="订单详情"
		placement="right"
		:body-style="{ padding: isMobile ? '12px' : '20px' }"
		@close="close"
	>
		<a-spin :spinning="loading">
			<template v-if="order">
				<div class="od-head">
					<div>
						<div class="od-amount">¥{{ money(order.paidAmount ?? order.amount) }}</div>
						<div class="od-head-sub">
							{{ order.studentName || '学员' }} · {{ order.packageName || '课包' }}
						</div>
					</div>
					<a-tag :color="statusColor(order.status)">{{ statusText(order.status) }}</a-tag>
				</div>

				<div class="od-section-title">学员信息</div>
				<div class="od-grid">
					<div class="od-row"><span>学员</span><b>{{ order.studentName || '-' }}</b></div>
					<div class="od-row"><span>家长手机</span><b>{{ order.parentPhone || student?.parentPhone || '-' }}</b></div>
					<div class="od-row"><span>校区</span><b>{{ campusName || '-' }}</b></div>
					<div class="od-row"><span>所在班级</span><b>{{ student?.classNames || '-' }}</b></div>
					<div class="od-row"><span>剩余课时</span><b>{{ remainText }}</b></div>
					<div class="od-row"><span>课包到期</span><b>{{ date(student?.accountExpireTime) }}</b></div>
				</div>

				<div class="od-section-title">订单信息</div>
				<div class="od-grid">
					<div class="od-row od-wide"><span>订单号</span><b class="od-order-no">{{ order.id }}</b></div>
					<div class="od-row"><span>课包</span><b>{{ order.packageName || '-' }}</b></div>
					<div class="od-row"><span>合计课时</span><b>{{ totalLessons }}</b></div>
					<div class="od-row"><span>正课</span><b>{{ lessons(order.packageLessons) }}</b></div>
					<div class="od-row"><span>赠课</span><b>{{ lessons(order.packageGiftLessons) }}</b></div>
					<div class="od-row"><span>有效期</span><b>{{ order.packageValidityDays ? order.packageValidityDays + ' 天' : '-' }}</b></div>
					<div class="od-row"><span>应收</span><b>¥{{ money(order.amount) }}</b></div>
					<div class="od-row"><span>优惠</span><b class="od-discount">{{ discount > 0 ? '-¥' + money(discount) : '-' }}</b></div>
					<div class="od-row"><span>实收</span><b class="od-paid">¥{{ money(order.paidAmount) }}</b></div>
					<div class="od-row"><span>收款渠道</span><b>{{ payChannelText(order.payChannel) }}</b></div>
					<div class="od-row"><span>下单时间</span><b>{{ date(order.createTime) }}</b></div>
					<div class="od-row"><span>收款时间</span><b>{{ date(order.payTime) }}</b></div>
					<div class="od-row"><span>经办人</span><b>{{ order.operatorName || '-' }}</b></div>
					<div class="od-row od-wide"><span>备注</span><b>{{ order.remark || '-' }}</b></div>
				</div>
			</template>
			<a-empty v-else-if="!loading" description="订单不存在" />
		</a-spin>

		<template #footer>
			<div class="od-actions">
				<a-button @click="close">关闭</a-button>
				<a-button v-if="order?.status === 'UNPAID' && canPay" type="primary" @click="onPay">去收款</a-button>
				<a-button v-else-if="order?.studentId && showStudentLink" type="primary" @click="onViewStudent">查看学员</a-button>
			</div>
		</template>
	</a-drawer>
</template>

<script setup name="orderDetailDrawer">
	import { globalStore } from '@/store'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import { payChannelText } from '@/utils/payChannel'

	const props = defineProps({
		open: { type: Boolean, default: false },
		orderId: { type: String, default: '' },
		canPay: { type: Boolean, default: false },
		showStudentLink: { type: Boolean, default: true }
	})
	const emit = defineEmits(['update:open', 'pay', 'view-student'])

	const store = globalStore()
	const isMobile = computed(() => store.isMobile)
	const drawerWidth = computed(() => (isMobile.value ? '100%' : 560))
	const loading = ref(false)
	const order = ref(null)
	const student = ref(null)
	const campusMap = ref({})

	const campusName = computed(() => {
		const campusId = order.value?.campusId || student.value?.campusId
		return campusMap.value[campusId] || '-'
	})
	const remainText = computed(() => {
		if (!student.value) return '-'
		if (order.value?.status === 'UNPAID') return '待收款后入账'
		if (student.value.hasLessonAccount === false) return '未报名'
		return `${lessons(student.value.remainLessons)} 课时`
	})
	const totalLessons = computed(() => lessons(Number(order.value?.packageLessons || 0) + Number(order.value?.packageGiftLessons || 0)))
	const discount = computed(() => {
		// 待支付订单未收款，不能把应收差额显示成优惠
		if (order.value?.status !== 'PAID' && order.value?.status !== 'REFUNDED') return 0
		const diff = Number(order.value?.amount || 0) - Number(order.value?.paidAmount || 0)
		return diff > 0 ? diff : 0
	})

	const money = (value) => (Number(value) || 0).toFixed(2)
	const lessons = (value) => {
		const number = Number(value || 0)
		return Number.isInteger(number) ? String(number) : number.toFixed(2)
	}
	const date = (value) => (value ? String(value).slice(0, 19).replace('T', ' ') : '-')
	const statusText = (value) =>
		({ UNPAID: '待支付', PAID: '已支付', REFUNDED: '已退费', CANCELED: '已取消' })[value] || value || '-'
	const statusColor = (value) =>
		value === 'PAID' ? 'green' : value === 'UNPAID' ? 'orange' : value === 'REFUNDED' ? 'red' : 'default'

	const load = async () => {
		if (!props.open || !props.orderId) return
		loading.value = true
		order.value = null
		student.value = null
		try {
			const [orderData, campusData] = await Promise.all([
				studentApi.orderDetail({ id: props.orderId }),
				courseApi.campusOptions().catch(() => [])
			])
			order.value = orderData || null
			campusMap.value = Object.fromEntries(
				(campusData || []).map((item) => [item.value ?? item.id, item.label ?? item.name])
			)
			if (orderData?.studentId) {
				student.value = await studentApi.studentDetail({ id: orderData.studentId }).catch(() => null)
			}
		} catch (e) {
			order.value = null
		} finally {
			loading.value = false
		}
	}

	const close = () => emit('update:open', false)
	const onPay = () => emit('pay', order.value)
	const onViewStudent = () => emit('view-student', order.value?.studentId)

	watch(
		() => [props.open, props.orderId],
		() => load(),
		{ immediate: true }
	)
</script>

<style lang="less" scoped>
	.od-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
		padding: 4px 0 14px;
		border-bottom: 1px solid var(--border-color-split);
	}
	.od-amount {
		font-size: 28px;
		font-weight: 700;
		line-height: 1.1;
		color: var(--text-color);
		font-variant-numeric: tabular-nums;
	}
	.od-head-sub {
		margin-top: 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.od-section-title {
		margin: 16px 0 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
		padding-left: 8px;
		border-left: 3px solid var(--primary-6);
		line-height: 16px;
	}
	.od-grid {
		padding: 4px 12px;
		background: var(--background-color-light);
		border: 1px solid var(--border-color-split);
		border-radius: var(--radius-control);
	}
	.od-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
		padding: 8px 0;
		border-bottom: 1px dashed var(--border-color-split);
		font-size: 13px;
		&:last-child {
			border-bottom: none;
		}
		span {
			flex: none;
			color: var(--text-color-secondary);
		}
		b {
			min-width: 0;
			text-align: right;
			font-weight: 500;
			color: var(--text-color);
			word-break: break-all;
		}
	}
	.od-wide {
		grid-column: 1 / -1;
	}
	.od-order-no {
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
		font-size: 12px;
		color: var(--text-color-secondary) !important;
	}
	.od-discount {
		color: var(--accent-text) !important;
	}
	.od-paid {
		color: var(--accent-text) !important;
		font-weight: 600 !important;
	}
	.od-actions {
		display: flex;
		justify-content: flex-end;
		gap: 8px;
	}
	@media (max-width: 576px) {
		.od-actions {
			display: grid;
			grid-template-columns: 1fr 1fr;
		}
		.od-actions :deep(.ant-btn) {
			height: 42px;
		}
	}
</style>
