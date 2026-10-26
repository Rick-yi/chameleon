<template>
	<div>
		<!-- 状态页签 -->
		<div class="m-filter">
			<div
				v-for="item in tabs"
				:key="item.value"
				class="m-filter-item"
				:class="{ active: tab === item.value }"
				@click="onTab(item.value)"
			>
				{{ item.text }}
				<span v-if="item.count !== undefined" class="m-filter-count">{{ item.count }}</span>
			</div>
		</div>

		<!-- 前台高频：按学员/手机/订单号直接搜，不用在 50 条里翻 -->
		<div class="m-search">
			<a-input-search
				v-model:value="keyword"
				placeholder="姓名 / 家长手机 / 订单号"
				allow-clear
				enter-button="搜索"
				@search="onSearch"
			/>
		</div>

		<!-- 订单列表 -->
		<template v-if="tab === 'UNPAID' || tab === 'PAID'">
			<div v-if="loading" class="m-empty">加载中…</div>
			<div v-else-if="!orderList.length" class="m-empty">
				{{ keyword ? '没有匹配的订单' : `没有${tab === 'UNPAID' ? '待收款' : '已收款'}订单` }}
			</div>
			<div v-for="item in orderList" :key="item.id" class="m-card m-order" @click="openDetail(item)">
				<div class="m-order-head">
					<div class="m-order-name">{{ item.studentName || '学员' }}</div>
					<div class="m-order-amount">
						<span class="m-order-amount-label">{{ item.status === 'PAID' ? '实收' : '应收' }}</span>
						¥{{ orderAmount(item).toFixed(2) }}
					</div>
				</div>
				<div class="m-order-sub">
					{{ item.packageName || '课包' }}
					<template v-if="item.status === 'PAID' && item.payChannel"> · {{ payChannelText(item.payChannel) }}</template>
				</div>
				<div class="m-order-foot">
					<span>{{ shortTime(item.createTime) }}</span>
					<div class="m-order-actions">
						<a-button
							v-if="item.status === 'UNPAID' && hasPerm('/student/order/offlinePay')"
							type="primary"
							size="small"
							@click.stop="openPay(item)"
						>
							收款
						</a-button>
						<a-tag v-else color="green">已收款</a-tag>
						<a-icon class="m-order-arrow"><right-outlined /></a-icon>
					</div>
				</div>
			</div>
		</template>

		<!-- 退费单 -->
		<template v-else>
			<div v-if="loading" class="m-empty">加载中…</div>
			<div v-else-if="!refundList.length" class="m-empty">{{ keyword ? '没有匹配的退费单' : '没有退费单' }}</div>
			<div v-for="item in refundList" :key="item.id" class="m-card m-order">
				<div class="m-order-head">
					<div class="m-order-name">{{ item.studentName || '学员' }}</div>
					<div class="m-order-amount">¥{{ Number(item.refundAmount || 0).toFixed(2) }}</div>
				</div>
				<div class="m-order-sub">
					可退 {{ item.refundLessons || 0 }} 课时
					<template v-if="item.createTime"> · 申请 {{ shortTime(item.createTime) }}</template>
				</div>
				<div v-if="item.applyRemark" class="m-order-opinion">申请原因：{{ item.applyRemark }}</div>
				<div v-if="item.opinion" class="m-order-opinion">审批意见：{{ item.opinion }}</div>
				<div class="m-order-foot">
					<a-tag :color="refundColor(item.status)">{{ refundText(item.status) }}</a-tag>
					<a-space v-if="item.status === 'PENDING'">
						<a-button v-if="hasPerm('/student/refund/approve')" size="small" @click="onApprove(item)">同意</a-button>
						<a-button size="small" danger @click="onReject(item)">驳回</a-button>
					</a-space>
				</div>
			</div>
		</template>

		<!-- 收款弹窗 -->
		<a-modal v-model:open="payOpen" title="收款" :width="320" @ok="onPay">
			<div class="m-pay-amount">¥{{ Number(payOrder?.amount || 0).toFixed(2) }}</div>
			<a-form layout="vertical">
				<a-form-item label="收款渠道">
					<a-select v-model:value="payForm.payChannel" :options="payChannelOptions" style="width: 100%" />
				</a-form-item>
				<a-form-item label="实收金额">
					<a-input-number v-model:value="payForm.paidAmount" :min="0" :precision="2" style="width: 100%" />
				</a-form-item>
				<a-form-item label="备注">
					<a-input v-model:value="payForm.remark" placeholder="收款人 / 流水号" />
				</a-form-item>
			</a-form>
		</a-modal>
		<!-- 订单详情：列表点卡片直接看，不必再跳页面 -->
		<order-detail-drawer
			v-model:open="detailOpen"
			:order-id="detailOrderId"
			:can-pay="hasPerm('/student/order/offlinePay')"
			:show-student-link="false"
			@pay="onPayFromDetail"
		/>
	</div>
</template>

<script setup name="appCashierOrders">
	import { useRoute } from 'vue-router'
	import { message, Modal } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import OrderDetailDrawer from '@/components/OrderDetail/index.vue'
	import { payChannelText } from '@/utils/payChannel'

	const route = useRoute()
	const loading = ref(false)
	const tab = ref(route.query.status || 'UNPAID')
	const keyword = ref('')
	const orderList = ref([])
	const refundList = ref([])
	const detailOpen = ref(false)
	const detailOrderId = ref('')
	const tabs = ref([
		{ value: 'UNPAID', text: '待收款' },
		{ value: 'PAID', text: '已收款' },
		{ value: 'REFUND', text: '退费单' }
	])

	const payOpen = ref(false)
	const payOrder = ref(null)
	const payForm = ref({ payChannel: 'OFFLINE', paidAmount: 0, remark: '' })
	const payChannelOptions = [
		{ value: 'OFFLINE', label: '线下' },
		{ value: 'CASH', label: '现金' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'ALIPAY', label: '支付宝' }
	]

	const refundText = (s) =>
		({ PENDING: '待审批', TO_REFUND: '待退款', REFUNDED: '已退款', REJECTED: '已驳回', CANCELLED: '已撤销' }[s] || s)
	const refundColor = (s) =>
		({ PENDING: 'orange', TO_REFUND: 'blue', REFUNDED: 'green', REJECTED: 'red', CANCELLED: 'default' }[s] || 'default')
	const orderAmount = (item) => Number((item.status === 'PAID' ? item.paidAmount : item.amount) || 0)
	const shortTime = (value) => (value ? String(value).slice(5, 16) : '')

	const load = async () => {
		loading.value = true
		try {
			if (tab.value === 'REFUND') {
				const res = await studentApi
					.refundPage({ current: 1, size: 50, searchKey: keyword.value.trim() || undefined })
					.catch(() => ({}))
				refundList.value = res?.records || []
				if (!keyword.value.trim()) tabs.value[2].count = res?.total || 0
			} else {
				const res = await studentApi
					.orderPage({
						current: 1,
						size: 50,
						status: tab.value,
						searchKey: keyword.value.trim() || undefined
					})
					.catch(() => ({}))
				orderList.value = res?.records || []
				// 搜索时保留顶部全局状态数量，避免把“搜索结果数”误当成全部待办数
				if (!keyword.value.trim()) {
					if (tab.value === 'UNPAID') tabs.value[0].count = res?.total || 0
					else tabs.value[1].count = res?.total || 0
				}
			}
		} finally {
			loading.value = false
		}
	}

	const onSearch = () => load()

	const onTab = (value) => {
		tab.value = value
		load()
	}

	const openPay = (item) => {
		payOrder.value = item
		payForm.value = { payChannel: 'OFFLINE', paidAmount: Number(item.amount || 0), remark: '' }
		payOpen.value = true
	}

	const openDetail = (item) => {
		detailOrderId.value = item.id
		detailOpen.value = true
	}

	const onPayFromDetail = (order) => {
		detailOpen.value = false
		if (order?.id) openPay(order)
	}

	const onPay = async () => {
		await studentApi.orderOfflinePay({
			orderId: payOrder.value.id,
			payChannel: payForm.value.payChannel,
			paidAmount: payForm.value.paidAmount,
			remark: payForm.value.remark
		})
		message.success('收款成功，课时已入账')
		payOpen.value = false
		load()
	}

	const onApprove = (item) => {
		Modal.confirm({
			title: '确认同意退费？',
			content: `学员：${item.studentName || '学员'}，可退 ${item.refundLessons || 0} 课时。同意后将冻结课时账户。`,
			okText: '同意',
			cancelText: '取消',
			onOk: async () => {
				await studentApi.refundApprove({ id: item.id })
				message.success('已同意退费')
				load()
			}
		})
	}

	// 退费单没有独立的「驳回」接口：审批通过后再撤销 = 驳回
	const onReject = (item) => {
		Modal.confirm({
			title: '确认驳回退费？',
			content: '驳回后该退费单将撤销，学员课时账户保持不变。',
			okText: '驳回',
			okButtonProps: { danger: true },
			cancelText: '取消',
			onOk: async () => {
				await studentApi.refundCancel({ id: item.id })
				message.success('已驳回')
				load()
			}
		})
	}

	/** 工作台行内动作直达：?orderId=... 打开收款，?refundId=... 打开审批 */
	const applyRouteAction = () => {
		const orderId = route.query.orderId
		const refundId = route.query.refundId
		if (orderId) {
			const item = orderList.value.find((row) => row.id === orderId)
			if (item && item.status === 'UNPAID') openPay(item)
			else if (item) openDetail(item)
			else {
				// 不在当前列表也允许按订单号直接看详情
				detailOrderId.value = String(orderId)
				detailOpen.value = true
			}
		}
		if (refundId) {
			const item = refundList.value.find((row) => row.id === refundId)
			if (item) onApprove(item)
			else message.warning('该退费单不在当前列表中，请重新搜索')
		}
	}

	onMounted(async () => {
		if (route.query.tab === 'refund' || route.query.refundId) tab.value = 'REFUND'
		await load()
		applyRouteAction()
	})
</script>

<style lang="less" scoped>
	.m-search {
		margin-bottom: 12px;
		:deep(.ant-input) {
			font-size: 16px;
		}
		:deep(.ant-btn) {
			height: 40px;
		}
	}
	.m-filter {
		display: flex;
		gap: 8px;
		margin-bottom: 12px;
	}
	.m-filter-item {
		flex: 1;
		text-align: center;
		padding: 8px 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
		&.active {
			color: #fff;
			background: var(--primary-6);
			border-color: var(--primary-6);
		}
	}
	.m-filter-count {
		margin-left: 4px;
		opacity: 0.8;
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-order {
		cursor: pointer;
		transition: border-color 0.15s;
		&:active {
			border-color: var(--primary-4);
		}
	}
	.m-order-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 10px;
	}
	.m-order-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-order-amount {
		display: flex;
		align-items: baseline;
		gap: 4px;
		font-size: 17px;
		font-weight: 600;
		color: var(--text-color);
		font-variant-numeric: tabular-nums;
	}
	.m-order-amount-label {
		font-size: 11px;
		font-weight: 400;
		color: var(--text-color-secondary);
	}
	.m-order-sub {
		margin-top: 2px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-order-opinion {
		margin-top: 8px;
		padding: 6px 8px;
		border-radius: var(--radius-control);
		background: var(--background-color-light);
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-order-foot {
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.m-order-actions {
		display: flex;
		align-items: center;
		gap: 8px;
	}
	.m-order-arrow {
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.m-pay-amount {
		text-align: center;
		font-size: 28px;
		font-weight: 600;
		color: var(--text-color);
		margin-bottom: 12px;
		font-variant-numeric: tabular-nums;
	}
	.m-empty {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 30px 12px;
		text-align: center;
		color: var(--text-color-secondary);
	}
</style>
