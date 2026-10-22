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

		<!-- 订单列表 -->
		<template v-if="tab === 'UNPAID' || tab === 'PAID'">
			<div v-if="loading" class="m-empty">加载中…</div>
			<div v-else-if="!orderList.length" class="m-empty">没有{{ tab === 'UNPAID' ? '待收款' : '已收款' }}订单</div>
			<div v-for="item in orderList" :key="item.id" class="m-card m-order">
				<div class="m-order-head">
					<div class="m-order-name">{{ item.studentName || '学员' }}</div>
					<div class="m-order-amount">¥{{ Number(item.amount || 0).toFixed(2) }}</div>
				</div>
				<div class="m-order-sub">{{ item.packageName || '课包' }}</div>
				<div class="m-order-foot">
					<span>{{ String(item.createTime || '').slice(5, 16) }}</span>
					<a-button
						v-if="item.status === 'UNPAID' && hasPerm('/student/order/offlinePay')"
						type="primary"
						size="small"
						@click.stop="openPay(item)"
					>
						收款
					</a-button>
					<a-tag v-else color="green">已收款</a-tag>
				</div>
			</div>
		</template>

		<!-- 退费单 -->
		<template v-else>
			<div v-if="loading" class="m-empty">加载中…</div>
			<div v-else-if="!refundList.length" class="m-empty">没有退费单</div>
			<div v-for="item in refundList" :key="item.id" class="m-card m-order">
				<div class="m-order-head">
					<div class="m-order-name">{{ item.studentName || '学员' }}</div>
					<div class="m-order-amount">¥{{ Number(item.refundAmount || 0).toFixed(2) }}</div>
				</div>
				<div class="m-order-sub">可退 {{ item.refundLessons || 0 }} 课时</div>
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
					<a-radio-group v-model:value="payForm.payChannel" :options="payChannelOptions" button-style="solid" />
				</a-form-item>
				<a-form-item label="实收金额">
					<a-input-number v-model:value="payForm.paidAmount" :min="0" :precision="2" style="width: 100%" />
				</a-form-item>
				<a-form-item label="备注">
					<a-input v-model:value="payForm.remark" placeholder="收款人 / 流水号" />
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="appCashierOrders">
	import { useRoute } from 'vue-router'
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'

	const route = useRoute()
	const loading = ref(false)
	const tab = ref(route.query.status || 'UNPAID')
	const orderList = ref([])
	const refundList = ref([])
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
		{ value: 'WECHAT', label: '微信' }
	]

	const refundText = (s) =>
		({ PENDING: '待审批', TO_REFUND: '待退款', REFUNDED: '已退款', REJECTED: '已驳回', CANCELLED: '已撤销' }[s] || s)
	const refundColor = (s) =>
		({ PENDING: 'orange', TO_REFUND: 'blue', REFUNDED: 'green', REJECTED: 'red', CANCELLED: 'default' }[s] || 'default')

	const load = async () => {
		loading.value = true
		try {
			if (tab.value === 'REFUND') {
				const res = await studentApi.refundPage({ current: 1, size: 50 }).catch(() => ({}))
				refundList.value = res?.records || []
				tabs.value[2].count = res?.total || 0
			} else {
				const res = await studentApi
					.orderPage({ current: 1, size: 50, status: tab.value })
					.catch(() => ({}))
				orderList.value = res?.records || []
				if (tab.value === 'UNPAID') tabs.value[0].count = res?.total || 0
				else tabs.value[1].count = res?.total || 0
			}
		} finally {
			loading.value = false
		}
	}

	const onTab = (value) => {
		tab.value = value
		load()
	}

	const openPay = (item) => {
		payOrder.value = item
		payForm.value = { payChannel: 'OFFLINE', paidAmount: Number(item.amount || 0), remark: '' }
		payOpen.value = true
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

	const onApprove = async (item) => {
		await studentApi.refundApprove({ id: item.id })
		message.success('已同意退费')
		load()
	}

	// 退费单没有独立的「驳回」接口：审批通过后再撤销 = 驳回
	const onReject = async (item) => {
		await studentApi.refundCancel({ id: item.id })
		message.success('已驳回')
		load()
	}

	onMounted(() => {
		if (route.query.tab === 'refund') tab.value = 'REFUND'
		load()
	})
</script>

<style lang="less" scoped>
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
		font-size: 17px;
		font-weight: 600;
		color: var(--text-color);
		font-variant-numeric: tabular-nums;
	}
	.m-order-sub {
		margin-top: 2px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-order-foot {
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 12px;
		color: #86909c;
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
