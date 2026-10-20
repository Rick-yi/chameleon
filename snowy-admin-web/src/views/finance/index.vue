<template>
	<div>
		<!-- 月度统计 -->
		<a-row :gutter="16" style="margin-bottom: 16px">
			<a-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<div style="color: rgba(0, 0, 0, 0.45); font-size: 13px">{{ monthLabel }}收款</div>
					<div style="font-size: 24px; font-weight: 600; margin-top: 6px">¥{{ formatMoney(monthTotal.paidAmount) }}</div>
				</a-card>
			</a-col>
			<a-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<div style="color: rgba(0, 0, 0, 0.45); font-size: 13px">{{ monthLabel }}退费</div>
					<div style="font-size: 24px; font-weight: 600; margin-top: 6px; color: #fa541c">¥{{ formatMoney(monthTotal.refundAmount) }}</div>
				</a-card>
			</a-col>
			<a-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<div style="color: rgba(0, 0, 0, 0.45); font-size: 13px">{{ monthLabel }}消课课时</div>
					<div style="font-size: 24px; font-weight: 600; margin-top: 6px">{{ formatMoney(monthTotal.attendLessons) }}</div>
				</a-card>
			</a-col>
			<a-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<div style="color: rgba(0, 0, 0, 0.45); font-size: 13px">{{ monthLabel }}净营收</div>
					<div style="font-size: 24px; font-weight: 600; margin-top: 6px; color: #52c41a">¥{{ formatMoney(monthTotal.netAmount) }}</div>
				</a-card>
			</a-col>
		</a-row>

		<a-card>
			<a-tabs v-model:activeKey="activeTab">
				<a-tab-pane key="payment" tab="收款单">
					<a-form layout="inline" style="margin-bottom: 16px">
						<a-form-item label="订单号/课包">
							<a-input v-model:value="queryForm.searchKey" placeholder="请输入" allow-clear style="width: 180px" />
						</a-form-item>
						<a-form-item label="渠道">
							<a-select v-model:value="queryForm.channel" placeholder="全部渠道" :options="channelOptions" allow-clear style="width: 140px" />
						</a-form-item>
						<a-form-item>
							<a-range-picker v-model:value="queryForm.dateRange" show-time value-format="YYYY-MM-DD HH:mm:ss" />
						</a-form-item>
						<a-form-item>
							<a-button type="primary" @click="loadPayment">查询</a-button>
						</a-form-item>
					</a-form>
					<a-table :columns="paymentColumns" :data-source="paymentList" :row-key="(r) => r.id" :loading="loading" bordered size="middle"
						:pagination="{ current: paymentPage.current, pageSize: paymentPage.size, total: paymentPage.total, onChange: onPaymentPageChange }">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="record.status === 'PAID' ? 'green' : 'red'">{{ record.status === 'PAID' ? '已支付' : '已退费' }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'payChannel'">{{ channelText(record.payChannel) }}</template>
						</template>
					</a-table>
				</a-tab-pane>

				<a-tab-pane key="refund" tab="退费单">
					<a-form layout="inline" style="margin-bottom: 16px">
						<a-form-item label="状态">
							<a-select v-model:value="refundQuery.status" placeholder="全部状态" :options="refundStatusOptions" allow-clear style="width: 140px" />
						</a-form-item>
						<a-form-item>
							<a-range-picker v-model:value="refundQuery.dateRange" show-time value-format="YYYY-MM-DD HH:mm:ss" />
						</a-form-item>
						<a-form-item>
							<a-button type="primary" @click="loadRefund">查询</a-button>
						</a-form-item>
					</a-form>
					<a-table :columns="refundColumns" :data-source="refundList" :row-key="(r) => r.id" :loading="loading" bordered size="middle"
						:pagination="{ current: refundPage.current, pageSize: refundPage.size, total: refundPage.total, onChange: onRefundPageChange }">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="refundStatusColor(record.status)">{{ refundStatusText(record.status) }}</a-tag>
							</template>
						</template>
					</a-table>
				</a-tab-pane>

				<a-tab-pane key="stat" tab="经营报表">
					<div style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px">
						<a-range-picker v-model:value="statQuery.dateRange" show-time value-format="YYYY-MM-DD HH:mm:ss" />
						<a-button type="primary" @click="loadStat">查询</a-button>
						<span style="color: rgba(0, 0, 0, 0.45); font-size: 12px">按课程汇总：收款 - 退费 = 净营收；消课课时来自点名扣课流水</span>
					</div>
					<a-table :columns="statColumns" :data-source="statList" :row-key="(r) => r.courseId" bordered size="middle" :pagination="false">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'courseId'">{{ courseName(record.courseId) }}</template>
						</template>
					</a-table>
				</a-tab-pane>
			</a-tabs>
		</a-card>
	</div>
</template>

<script setup name="financeIndex">
	import financeApi from '@/api/finance/financeApi'
	import courseApi from '@/api/course/courseApi'
	const activeTab = ref('payment')
	const loading = ref(false)
	const monthLabel = ref('本月')
	const monthTotal = ref({ paidAmount: 0, refundAmount: 0, attendLessons: 0, netAmount: 0 })
	const courseMap = ref({})
	const queryForm = ref({ searchKey: '', channel: undefined, dateRange: undefined })
	const refundQuery = ref({ status: undefined, dateRange: undefined })
	const statQuery = ref({ dateRange: undefined })
	const paymentList = ref([])
	const paymentPage = ref({ current: 1, size: 10, total: 0 })
	const refundList = ref([])
	const refundPage = ref({ current: 1, size: 10, total: 0 })
	const statList = ref([])
	const channelOptions = [
		{ value: 'WECHAT', label: '微信' },
		{ value: 'ALIPAY', label: '支付宝' },
		{ value: 'OFFLINE', label: '线下' }
	]
	const refundStatusOptions = [
		{ value: 'PENDING', label: '待审批' },
		{ value: 'TO_REFUND', label: '待退款' },
		{ value: 'REFUNDED', label: '已退款' },
		{ value: 'REJECTED', label: '已驳回' }
	]
	const paymentColumns = [
		{ title: '订单号', dataIndex: 'id' },
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '课包', dataIndex: 'packageName' },
		{ title: '应收', dataIndex: 'amount' },
		{ title: '实收', dataIndex: 'paidAmount' },
		{ title: '渠道', dataIndex: 'payChannel' },
		{ title: '支付时间', dataIndex: 'payTime' },
		{ title: '状态', dataIndex: 'status' }
	]
	const refundColumns = [
		{ title: '退费单号', dataIndex: 'id' },
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '退费课时', dataIndex: 'refundLessons' },
		{ title: '退费金额', dataIndex: 'refundAmount' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '审批意见', dataIndex: 'opinion' },
		{ title: '创建时间', dataIndex: 'createTime' }
	]
	const statColumns = [
		{ title: '课程', dataIndex: 'courseId' },
		{ title: '收款金额', dataIndex: 'paidAmount' },
		{ title: '退费金额', dataIndex: 'refundAmount' },
		{ title: '消课课时', dataIndex: 'attendLessons' },
		{ title: '净营收', dataIndex: 'netAmount' }
	]
	const channelText = (c) => ({ WECHAT: '微信', ALIPAY: '支付宝', OFFLINE: '线下' }[c] || c)
	const refundStatusText = (s) => ({ PENDING: '待审批', TO_REFUND: '待退款', REFUNDED: '已退款', REJECTED: '已驳回' }[s] || s)
	const refundStatusColor = (s) => ({ REFUNDED: 'green', PENDING: 'orange', TO_REFUND: 'blue', REJECTED: 'red' }[s] || 'default')
	const formatMoney = (v) => (v === undefined || v === null ? '0.00' : Number(v).toFixed(2))
	const courseName = (id) => courseMap.value[id] || (id === 'UNKNOWN' ? '未知课程' : id)

	const monthRange = () => {
		const now = new Date()
		const start = new Date(now.getFullYear(), now.getMonth(), 1)
		const pad = (n) => String(n).padStart(2, '0')
		const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
		return [fmt(start), fmt(now)]
	}
	const toQuery = (form) => ({
		startTime: form.dateRange && form.dateRange[0] ? form.dateRange[0] : undefined,
		endTime: form.dateRange && form.dateRange[1] ? form.dateRange[1] : undefined
	})

	const loadCourses = () => {
		courseApi.coursePage({ current: 1, size: 100 }).then((data) => {
			const map = {}
			for (const c of data.records || []) map[c.id] = c.name
			courseMap.value = map
		})
	}
	const loadPayment = () => {
		loading.value = true
		financeApi
			.paymentPage({ current: paymentPage.value.current, size: paymentPage.value.size, searchKey: queryForm.value.searchKey, channel: queryForm.value.channel, ...toQuery(queryForm.value) })
			.then((data) => {
				paymentList.value = data.records || []
				paymentPage.value.total = data.total || 0
			})
			.finally(() => (loading.value = false))
	}
	const onPaymentPageChange = (page, pageSize) => {
		paymentPage.value.current = page
		paymentPage.value.size = pageSize
		loadPayment()
	}
	const loadRefund = () => {
		loading.value = true
		financeApi
			.refundPage({ current: refundPage.value.current, size: refundPage.value.size, status: refundQuery.value.status, ...toQuery(refundQuery.value) })
			.then((data) => {
				refundList.value = data.records || []
				refundPage.value.total = data.total || 0
			})
			.finally(() => (loading.value = false))
	}
	const onRefundPageChange = (page, pageSize) => {
		refundPage.value.current = page
		refundPage.value.size = pageSize
		loadRefund()
	}
	const loadStat = () => {
		financeApi.stat(toQuery(statQuery.value)).then((data) => {
			statList.value = data || []
		})
	}
	const loadMonthStat = () => {
		const [start, end] = monthRange()
		financeApi.stat({ startTime: start, endTime: end }).then((data) => {
			const total = { paidAmount: 0, refundAmount: 0, attendLessons: 0, netAmount: 0 }
			for (const item of data || []) {
				total.paidAmount += Number(item.paidAmount || 0)
				total.refundAmount += Number(item.refundAmount || 0)
				total.attendLessons += Number(item.attendLessons || 0)
				total.netAmount += Number(item.netAmount || 0)
			}
			monthTotal.value = total
		})
	}

	loadCourses()
	loadPayment()
	loadRefund()
	loadStat()
	loadMonthStat()
</script>
