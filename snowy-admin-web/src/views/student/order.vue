<template>
	<div>
		<page-header title="收款台" desc="订单收款、退费审批与订单查询">
			<template #extra>
				<a-button @click="loadStatusCount">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
			</template>
		</page-header>

		<!-- 状态概览：点击即筛选 -->
		<a-row :gutter="12" style="margin-bottom: 12px">
			<a-col v-for="item in statusCards" :key="item.value" :xs="12" :sm="8" :md="6">
				<div class="ent-stat status-card" :class="{ active: isCardActive(item) }" @click="pickStatus(item)">
					<div class="ent-stat-label">{{ item.label }}</div>
					<div class="ent-stat-value" :style="{ color: item.color }">{{ item.count ?? '-' }}</div>
				</div>
			</a-col>
		</a-row>

		<div class="ent-toolbar" style="border-radius: 6px 6px 0 0">
			<a-tabs v-model:activeKey="activeTab" :tabBarStyle="{ margin: 0 }">
				<!-- 订单 -->
				<a-tab-pane key="order" tab="订单">
					<a-form layout="inline" style="margin-bottom: 12px">
						<a-form-item label="学员">
							<a-input v-model:value="queryForm.searchKey" placeholder="姓名或家长手机" allow-clear style="width: 200px" @pressEnter="reload" />
						</a-form-item>
						<a-form-item label="状态">
							<a-select v-model:value="queryForm.status" placeholder="全部状态" :options="orderStatusOptions" allow-clear style="width: 140px" />
						</a-form-item>
						<a-form-item label="时间">
							<a-range-picker v-model:value="queryForm.dateRange" show-time value-format="YYYY-MM-DD HH:mm:ss" />
						</a-form-item>
						<a-form-item>
							<a-space>
								<a-button type="primary" @click="reload">查询</a-button>
								<a-button @click="reset">重置</a-button>
							</a-space>
						</a-form-item>
					</a-form>
					<a-table
						:columns="orderColumns"
						:data-source="orderList"
						:row-key="(r) => r.id"
						:loading="loading"
						bordered
						size="middle"
						:pagination="{ current: orderPage.current, pageSize: orderPage.size, total: orderPage.total, onChange: onOrderPageChange }"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="orderStatusColor(record.status)">{{ orderStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'amount'">¥{{ record.amount }}</template>
							<template v-if="column.dataIndex === 'paidAmount'">¥{{ record.paidAmount || 0 }}</template>
							<template v-if="column.dataIndex === 'studentName'">
								<a @click="goStudent(record)">{{ record.studentName || record.studentId }}</a>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-space>
									<a-button v-if="record.status === 'UNPAID' && hasPerm('/student/order/offlinePay')" type="link" size="small" @click="onOpenPay(record)">收款</a-button>
									<a-button v-if="record.status === 'UNPAID' && hasPerm('/student/order/cancel')" type="link" size="small" @click="onCancelOrder(record)">取消</a-button>
									<a-button v-if="record.status === 'PAID' && hasPerm('/student/refund/apply')" type="link" size="small" @click="onOpenRefund(record)">退费</a-button>
									<a-button type="link" size="small" @click="goStudent(record)">学员</a-button>
								</a-space>
							</template>
						</template>
					</a-table>
				</a-tab-pane>

				<!-- 退费单 -->
				<a-tab-pane key="refund" tab="退费单">
					<a-form layout="inline" style="margin-bottom: 12px">
						<a-form-item label="学员">
							<a-input v-model:value="refundQuery.searchKey" placeholder="姓名或家长手机" allow-clear style="width: 200px" @pressEnter="loadRefund" />
						</a-form-item>
						<a-form-item label="状态">
							<a-select v-model:value="refundQuery.status" placeholder="全部状态" :options="refundStatusOptions" allow-clear style="width: 140px" />
						</a-form-item>
						<a-form-item>
							<a-space>
								<a-button type="primary" @click="loadRefund">查询</a-button>
								<a-button @click="resetRefund">重置</a-button>
							</a-space>
						</a-form-item>
					</a-form>
					<a-table
						:columns="refundColumns"
						:data-source="refundList"
						:row-key="(r) => r.id"
						:loading="loading"
						bordered
						size="middle"
						:pagination="{ current: refundPage.current, pageSize: refundPage.size, total: refundPage.total, onChange: onRefundPageChange }"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="refundStatusColor(record.status)">{{ refundStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'refundAmount'">¥{{ record.refundAmount }}</template>
							<template v-if="column.dataIndex === 'studentName'">
								<a @click="goStudent(record)">{{ record.studentName || record.studentId }}</a>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-space>
									<a-button v-if="record.status === 'PENDING' && hasPerm('/student/refund/approve')" type="link" size="small" @click="onOpenApprove(record)">审批</a-button>
									<a-button v-if="record.status === 'TO_REFUND' && hasPerm('/student/refund/confirm')" type="link" size="small" @click="onConfirmRefund(record)">确认退款</a-button>
									<a-popconfirm v-if="record.status === 'PENDING' || record.status === 'TO_REFUND'" title="确定撤销该退费单吗？" @confirm="onCancelRefund(record)">
										<a-button type="link" danger size="small">撤销</a-button>
									</a-popconfirm>
								</a-space>
							</template>
						</template>
					</a-table>
				</a-tab-pane>
			</a-tabs>
		</div>
	</div>

	<!-- 收款登记 -->
	<a-modal v-model:open="payOpen" title="线下收款登记" :width="480" :destroy-on-close="true" @ok="onSubmitPay">
		<a-form ref="payFormRef" :model="payFormData" layout="vertical">
			<a-alert type="info" show-icon message="请核对实收金额，确认后课时立即入账" style="margin-bottom: 16px" />
			<a-form-item label="订单">
				<a-input :value="`${payFormData.studentName || ''} · ${payFormData.packageName || ''}`" disabled />
			</a-form-item>
			<a-form-item label="应收金额">
				<a-input :value="`¥${payFormData.amount || 0}`" disabled />
			</a-form-item>
			<a-form-item label="收款渠道" name="payChannel">
				<a-select v-model:value="payFormData.payChannel" :options="payChannelOptions" />
			</a-form-item>
			<a-form-item label="实收金额" name="paidAmount" :rules="[{ required: true, message: '请输入实收金额' }]">
				<a-input-number v-model:value="payFormData.paidAmount" :min="0" :precision="2" style="width: 100%" />
			</a-form-item>
			<a-form-item label="备注" name="remark">
				<a-input v-model:value="payFormData.remark" placeholder="如：收款人、转账流水号" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 退费申请 -->
	<a-modal v-model:open="refundOpen" title="退费申请" :width="480" :destroy-on-close="true" @ok="onSubmitRefund">
		<a-form ref="refundFormRef" :model="refundFormData" layout="vertical">
			<a-alert type="warning" show-icon message="退费只退该订单已购买但未消耗的课时，赠课随订单作废" style="margin-bottom: 16px" />
			<a-form-item label="订单">
				<a-input :value="`${refundFormData.studentName || ''} · ${refundFormData.packageName || ''}`" disabled />
			</a-form-item>
			<a-form-item label="退费原因" name="remark">
				<a-input v-model:value="refundFormData.remark" placeholder="请输入退费原因" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 退费审批 -->
	<a-modal v-model:open="approveOpen" title="退费审批" :width="480" :destroy-on-close="true" :footer="null">
		<a-form ref="approveFormRef" :model="approveFormData" layout="vertical">
			<a-form-item label="审批意见" name="opinion">
				<a-input v-model:value="approveFormData.opinion" placeholder="请输入审批意见" allow-clear />
			</a-form-item>
			<div style="display: flex; gap: 8px; justify-content: flex-end">
				<a-button type="primary" @click="onApprove(true)">通过（冻结账户）</a-button>
				<a-button danger @click="onApprove(false)">驳回</a-button>
			</div>
		</a-form>
	</a-modal>
</template>

<script setup name="studentOrder">
	import { useRoute, useRouter } from 'vue-router'
	import studentApi from '@/api/student/studentApi'
	import { payChannelText } from '@/utils/payChannel'
	const route = useRoute()
	const router = useRouter()
	const loading = ref(false)
	const activeTab = ref('order')
	const queryForm = ref({ searchKey: undefined, status: undefined, dateRange: undefined })
	const refundQuery = ref({ searchKey: undefined, status: undefined })
	const orderList = ref([])
	const orderPage = ref({ current: 1, size: 10, total: 0 })
	const refundList = ref([])
	const refundPage = ref({ current: 1, size: 10, total: 0 })
	const statusCount = ref({})

	const orderStatusOptions = [
		{ value: 'UNPAID', label: '待支付' },
		{ value: 'PAID', label: '已支付' },
		{ value: 'REFUNDED', label: '已退费' },
		{ value: 'CANCELED', label: '已取消' }
	]
	const refundStatusOptions = [
		{ value: 'PENDING', label: '待审批' },
		{ value: 'TO_REFUND', label: '待退款' },
		{ value: 'REFUNDED', label: '已退款' },
		{ value: 'REJECTED', label: '已驳回' }
	]
	const payChannelOptions = [
		{ value: 'OFFLINE', label: '线下转账' },
		{ value: 'CASH', label: '现金' }
	]
	const orderColumns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '课包', dataIndex: 'packageName' },
		{ title: '应收', dataIndex: 'amount', width: 110 },
		{ title: '实收', dataIndex: 'paidAmount', width: 110 },
		{ title: '渠道', dataIndex: 'payChannel', width: 90, customRender: ({ text }) => payChannelText(text) },
		{ title: '下单时间', dataIndex: 'createTime', width: 170 },
		{ title: '状态', dataIndex: 'status', width: 100 },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 220 }
	]
	const refundColumns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '退费课时', dataIndex: 'refundLessons', width: 100 },
		{ title: '退费金额', dataIndex: 'refundAmount', width: 110 },
		{ title: '状态', dataIndex: 'status', width: 100 },
		{ title: '审批意见', dataIndex: 'opinion' },
		{ title: '创建时间', dataIndex: 'createTime', width: 170 },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 200 }
	]
	// 统计卡：订单与退费单是两张表，指标要分别取，否则「已退费」永远是 0
	const statusCards = ref([
		{ scope: 'order', value: 'UNPAID', label: '待收款订单', color: '#d89614', count: null },
		{ scope: 'order', value: 'PAID', label: '已收款订单', color: '#2ba471', count: null },
		{ scope: 'refund', value: 'PENDING', label: '待审批退费', color: '#e5484d', count: null },
		{ scope: 'refund', value: 'REFUNDED', label: '已退费单', color: '#86909c', count: null }
	])

	const orderStatusText = (s) => ({ UNPAID: '待支付', PAID: '已支付', REFUNDED: '已退费', CANCELED: '已取消' }[s] || s)
	const orderStatusColor = (s) => (s === 'PAID' ? 'green' : s === 'UNPAID' ? 'orange' : s === 'REFUNDED' ? 'red' : 'default')
	const refundStatusText = (s) =>
		({ PENDING: '待审批', TO_REFUND: '待退款', REFUNDED: '已退款', REJECTED: '已驳回', CANCELLED: '已撤销' }[s] || s)
	const refundStatusColor = (s) =>
		s === 'REFUNDED' ? 'green' : s === 'PENDING' ? 'orange' : s === 'REJECTED' || s === 'CANCELLED' ? 'red' : 'blue'

	const isCardActive = (card) => {
		return card.scope === 'order'
			? queryForm.value.status === card.value
			: activeTab.value === 'refund' && refundQuery.value.status === card.value
	}
	const pickStatus = (card) => {
		if (card.scope === 'refund') {
			activeTab.value = 'refund'
			refundQuery.value.status = refundQuery.value.status === card.value ? undefined : card.value
			refundPage.value.current = 1
			loadRefund()
			return
		}
		queryForm.value.status = queryForm.value.status === card.value ? undefined : card.value
		orderPage.value.current = 1
		loadOrder()
	}

	const loadOrder = () => {
		loading.value = true
		const params = {
			current: orderPage.value.current,
			size: orderPage.value.size,
			searchKey: queryForm.value.searchKey,
			status: queryForm.value.status,
			startTime: queryForm.value.dateRange ? queryForm.value.dateRange[0] : undefined,
			endTime: queryForm.value.dateRange ? queryForm.value.dateRange[1] : undefined
		}
		studentApi
			.orderPage(params)
			.then((data) => {
				orderList.value = data.records || []
				orderPage.value.total = data.total || 0
			})
			.finally(() => (loading.value = false))
	}
	const loadRefund = () => {
		loading.value = true
		studentApi
			.refundPage({
				current: refundPage.value.current,
				size: refundPage.value.size,
				searchKey: refundQuery.value.searchKey,
				status: refundQuery.value.status
			})
			.then((data) => {
				refundList.value = data.records || []
				refundPage.value.total = data.total || 0
			})
			.finally(() => (loading.value = false))
	}
	const loadStatusCount = () => {
		Promise.all(
			statusCards.value.map((item) => {
				const request =
					item.scope === 'refund'
						? studentApi.refundPage({ current: 1, size: 1, status: item.value })
						: studentApi.orderPage({ current: 1, size: 1, status: item.value })
				return request
					.then((data) => ({ key: `${item.scope}-${item.value}`, total: data.total || 0 }))
					.catch(() => ({ key: `${item.scope}-${item.value}`, total: 0 }))
			})
		).then((list) => {
			const map = {}
			for (const item of list) map[item.key] = item.total
			statusCount.value = map
			statusCards.value.forEach((item) => (item.count = map[`${item.scope}-${item.value}`]))
		})
	}
	const reload = () => {
		orderPage.value.current = 1
		loadOrder()
	}
	const reset = () => {
		queryForm.value = { searchKey: undefined, status: undefined, dateRange: undefined }
		reload()
	}
	const resetRefund = () => {
		refundQuery.value = { searchKey: undefined, status: undefined }
		refundPage.value.current = 1
		loadRefund()
	}
	const onOrderPageChange = (page, pageSize) => {
		orderPage.value.current = page
		orderPage.value.size = pageSize
		loadOrder()
	}
	const onRefundPageChange = (page, pageSize) => {
		refundPage.value.current = page
		refundPage.value.size = pageSize
		loadRefund()
	}
	const goStudent = (record) => {
		if (record.studentId) {
			router.push({ path: '/student', query: { id: record.studentId } })
		}
	}

	// 收款
	const payOpen = ref(false)
	const payFormRef = ref()
	const payFormData = ref({})
	const onOpenPay = (record) => {
		payFormData.value = { ...record, payChannel: 'OFFLINE', paidAmount: record.amount, remark: '' }
		payOpen.value = true
	}
	const onSubmitPay = () => {
		payFormRef.value.validate().then(() => {
			studentApi
				.orderOfflinePay({
					orderId: payFormData.value.id,
					paidAmount: payFormData.value.paidAmount,
					payChannel: payFormData.value.payChannel,
					remark: payFormData.value.remark
				})
				.then(() => {
					payOpen.value = false
					loadOrder()
					loadStatusCount()
				})
		})
	}
	const onCancelOrder = (record) => {
		studentApi.orderCancel({ id: record.id }).then(() => {
			loadOrder()
			loadStatusCount()
		})
	}

	// 退费
	const refundOpen = ref(false)
	const refundFormRef = ref()
	const refundFormData = ref({})
	const onOpenRefund = (record) => {
		refundFormData.value = { orderId: record.id, studentName: record.studentName, packageName: record.packageName, remark: '' }
		refundOpen.value = true
	}
	const onSubmitRefund = () => {
		refundFormRef.value.validate().then(() => {
			studentApi.refundApply({ orderId: refundFormData.value.orderId, remark: refundFormData.value.remark }).then(() => {
				refundOpen.value = false
				loadRefund()
			})
		})
	}
	const approveOpen = ref(false)
	const approveFormRef = ref()
	const approveFormData = ref({})
	const onOpenApprove = (record) => {
		approveFormData.value = { refundId: record.id, opinion: '' }
		approveOpen.value = true
	}
	const onApprove = (pass) => {
		studentApi.refundApprove({ ...approveFormData.value, pass }).then(() => {
			approveOpen.value = false
			loadRefund()
		})
	}
	const onConfirmRefund = (record) => {
		studentApi.refundConfirm({ id: record.id }).then(() => loadRefund())
	}
	const onCancelRefund = (record) => {
		studentApi.refundCancel({ id: record.id }).then(() => loadRefund())
	}

	// 支持从工作台 / 报表带参进入
	const applyRouteQuery = () => {
		if (route.query.status) {
			queryForm.value.status = route.query.status
			refundQuery.value.status = route.query.status
		}
		if (route.query.tab === 'refund') {
			activeTab.value = 'refund'
		}
	}
	applyRouteQuery()
	loadOrder()
	loadRefund()
	loadStatusCount()
</script>

<style lang="less" scoped>
	.status-card {
		cursor: pointer;
		transition: all 0.15s;
		&:hover {
			border-color: #9dc2f4;
		}
		&.active {
			border-color: #1668dc;
			box-shadow: 0 0 0 1px #1668dc;
		}
	}
</style>
