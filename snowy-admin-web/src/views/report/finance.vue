<template>
	<div>
		<a-card :bordered="false" class="report-filter">
			<a-form layout="inline">
				<a-form-item label="时间">
					<a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" allow-clear />
				</a-form-item>
				<a-form-item label="校区">
					<a-select v-model:value="query.campusId" placeholder="全部校区" :options="campusOptions" allow-clear style="width: 160px" />
				</a-form-item>
				<a-form-item label="课程">
					<a-select v-model:value="query.courseId" placeholder="全部课程" :options="courseOptions" allow-clear style="width: 180px" />
				</a-form-item>
				<a-form-item label="渠道">
					<a-select v-model:value="query.channel" placeholder="全部渠道" :options="channelOptions" allow-clear style="width: 140px" />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="loadAll">查询</a-button>
						<a-button @click="reset">重置</a-button>
						<a-dropdown v-if="hasPerm('reportExport')">
							<a-button :loading="exportLoading">
								导出
								<down-outlined />
							</a-button>
							<template #overlay>
								<a-menu @click="onExport">
									<a-menu-item key="PAYMENT">收款明细</a-menu-item>
									<a-menu-item key="REFUND">退费明细</a-menu-item>
									<a-menu-item key="COURSE_SUMMARY">课程汇总</a-menu-item>
								</a-menu>
							</template>
						</a-dropdown>
					</a-space>
				</a-form-item>
			</a-form>
		</a-card>

		<a-spin :spinning="loading">
			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="收款" :value="summary.paidAmount" :precision="2" prefix="¥" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="退费" :value="summary.refundAmount" :precision="2" prefix="¥" :value-style="{ color: '#fa541c' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="净营收" :value="summary.netAmount" :precision="2" prefix="¥" :value-style="{ color: '#52c41a' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="课消金额" :value="summary.recognizedAmount" :precision="2" prefix="¥" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="订单数" :value="summary.orderCount" suffix="笔" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="客单价" :value="summary.avgOrderAmount" :precision="2" prefix="¥" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="生均收入" :value="summary.perStudentRevenue" :precision="2" prefix="¥" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="退费率" :value="summary.refundRate" :precision="2" suffix="%" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="实收率" :value="summary.collectionRate" :precision="2" suffix="%" /></a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="16">
					<a-card title="收支趋势" :bordered="false">
						<div id="financeTrendChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="8">
					<a-card title="收款渠道" :bordered="false">
						<div id="financeChannelChart" class="report-chart"></div>
					</a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="16">
					<a-card title="近 30 天课消趋势" :bordered="false">
						<div id="financeAttendChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="8">
					<a-card title="校区经营对比" :bordered="false">
						<a-table
							:columns="campusColumns"
							:data-source="campusRankList"
							:row-key="(r) => r.campusId"
							:pagination="false"
							size="small"
							bordered
						/>
					</a-card>
				</a-col>
			</a-row>

			<a-card title="课程收款排行" :bordered="false" style="margin-top: 16px">
				<a-table :columns="rankColumns" :data-source="courseRankList" :pagination="false" bordered size="middle" />
			</a-card>

			<a-card title="课消与收入确认" :bordered="false" style="margin-top: 16px">
				<a-table
					:columns="consumeColumns"
					:data-source="consumeList"
					:row-key="(r) => `${r.courseId}-${r.coursePackageId}`"
					:pagination="false"
					bordered
					size="middle"
				/>
			</a-card>

			<a-card :bordered="false" style="margin-top: 16px" v-if="hasPerm('reportDetail')">
				<a-tabs v-model:activeKey="activeTab">
					<a-tab-pane key="payment" tab="收款明细">
						<a-table
							:columns="paymentColumns"
							:data-source="paymentList"
							:loading="detailLoading"
							:row-key="(r) => r.id"
							bordered
							size="middle"
							:pagination="paymentPage"
							@change="onPaymentPageChange"
						/>
					</a-tab-pane>
					<a-tab-pane key="refund" tab="退费明细">
						<a-table
							:columns="refundColumns"
							:data-source="refundList"
							:loading="detailLoading"
							:row-key="(r) => r.id"
							bordered
							size="middle"
							:pagination="refundPage"
							@change="onRefundPageChange"
						/>
					</a-tab-pane>
					<a-tab-pane key="daily" tab="日汇总">
						<a-table
							:columns="dailyColumns"
							:data-source="dailyList"
							:row-key="(r) => r.statDate"
							:pagination="{ pageSize: 10, showSizeChanger: true }"
							bordered
							size="middle"
						/>
					</a-tab-pane>
				</a-tabs>
			</a-card>

			<a-card title="数据对账" :bordered="false" style="margin-top: 16px">
				<a-space style="margin-bottom: 12px">
					<a-button @click="loadReconcile">重新对账</a-button>
					<a-button @click="rebuildDaily">重算当日汇总</a-button>
					<a-button @click="clearCache">清理报表缓存</a-button>
				</a-space>
				<a-alert
					:type="reconcile.consistent ? 'success' : 'warning'"
					show-icon
					:message="reconcile.consistent ? '报表数字与业务明细一致' : '存在差异，请核对下列说明'"
					:description="(reconcile.messages || []).join('；') || '收款、退费、课消口径一致'"
				/>
			</a-card>
		</a-spin>
	</div>
</template>

<script setup name="reportFinance">
	import { h, nextTick, onMounted, reactive, ref } from 'vue'
	import { Line, Pie } from '@antv/g2plot'
	import { message } from 'ant-design-vue'
	import dayjs from 'dayjs'
	import reportApi from '@/api/report/reportApi'
	import financeApi from '@/api/finance/financeApi'
	import courseApi from '@/api/course/courseApi'
	import downloadUtil from '@/utils/downloadUtil'

	const loading = ref(false)
	const detailLoading = ref(false)
	const activeTab = ref('payment')
	const dateRange = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
	const query = reactive({ campusId: undefined, courseId: undefined, channel: undefined })
	const campusOptions = ref([])
	const courseOptions = ref([])
	const channelOptions = [
		{ value: 'OFFLINE', label: '线下转账' },
		{ value: 'CASH', label: '现金' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'ALIPAY', label: '支付宝' }
	]

	const summary = ref({
		paidAmount: 0,
		refundAmount: 0,
		netAmount: 0,
		recognizedAmount: 0,
		orderCount: 0,
		avgOrderAmount: 0,
		perStudentRevenue: 0,
		refundRate: 0,
		collectionRate: 0
	})
	const trendList = ref([])
	const channelList = ref([])
	const courseRankList = ref([])
	const campusRankList = ref([])
	const attendTrendList = ref([])
	const paymentList = ref([])
	const refundList = ref([])
	const dailyList = ref([])
	const consumeList = ref([])
	const reconcile = ref({ consistent: true, messages: [] })
	const exportLoading = ref(false)
	const paymentPage = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
	const refundPage = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

	let trendChart
	let channelChart
	let attendChart

	const money = (value) => Number(value || 0)
	const emptySummary = () => ({
		paidAmount: 0,
		refundAmount: 0,
		netAmount: 0,
		recognizedAmount: 0,
		orderCount: 0,
		avgOrderAmount: 0,
		perStudentRevenue: 0,
		refundRate: 0,
		collectionRate: 0
	})

	const buildQuery = () => {
		const [start, end] = dateRange.value || []
		return {
			startTime: start ? `${start} 00:00:00` : undefined,
			endTime: end ? `${end} 23:59:59` : undefined,
			campusId: query.campusId,
			courseId: query.courseId,
			channel: query.channel
		}
	}

	const renderCharts = () => {
		trendChart?.destroy()
		channelChart?.destroy()
		attendChart?.destroy()
		const trendData = []
		trendList.value.forEach((item) => {
			trendData.push({ period: item.period, series: '收款', value: money(item.paidAmount) })
			trendData.push({ period: item.period, series: '退费', value: money(item.refundAmount) })
			trendData.push({ period: item.period, series: '净营收', value: money(item.netAmount) })
		})
		if (trendData.length) {
			trendChart = new Line('financeTrendChart', {
				data: trendData,
				xField: 'period',
				yField: 'value',
				seriesField: 'series',
				height: 300,
				color: ['#1677ff', '#fa541c', '#52c41a'],
				legend: { position: 'top' },
				smooth: true
			})
			trendChart.render()
		}

		const channelData = channelList.value.map((item) => ({ type: item.channel, value: money(item.paidAmount) }))
		if (channelData.length) {
			channelChart = new Pie('financeChannelChart', {
				data: channelData,
				angleField: 'value',
				colorField: 'type',
				height: 300,
				radius: 0.8,
				label: { type: 'outer', content: '{name} {percentage}' },
				legend: { position: 'bottom' }
			})
			channelChart.render()
		}

		const attendData = attendTrendList.value.map((item) => ({
			period: item.period,
			value: money(item.attendLessons)
		}))
		if (attendData.length) {
			attendChart = new Line('financeAttendChart', {
				data: attendData,
				xField: 'period',
				yField: 'value',
				height: 300,
				color: '#722ed1',
				smooth: true,
				appendPadding: [8, 0, 0, 0]
			})
			attendChart.render()
		}
	}

	const loadSummary = () => {
		const params = buildQuery()
		return Promise.all([
			reportApi.summary(params),
			reportApi.trend({ ...params, dimension: 'DAY' }),
			reportApi.channel(params),
			reportApi.courseRank(params),
			reportApi.campusRank(params),
			reportApi.attendTrend(params),
			reportApi.consume(params)
		]).then(([summaryData, trendData, channelData, rankData, campusData, attendData, consumeData]) => {
			summary.value = summaryData || emptySummary()
			trendList.value = trendData || []
			channelList.value = channelData || []
			courseRankList.value = rankData || []
			campusRankList.value = campusData || []
			attendTrendList.value = attendData || []
			consumeList.value = consumeData || []
			nextTick(renderCharts)
		})
	}

	const loadPayment = () => {
		detailLoading.value = true
		financeApi
			.paymentPage({ ...buildQuery(), current: paymentPage.current, size: paymentPage.pageSize })
			.then((data) => {
				paymentList.value = data.records || []
				paymentPage.total = data.total || 0
			})
			.finally(() => {
				detailLoading.value = false
			})
	}

	const loadRefund = () => {
		detailLoading.value = true
		financeApi
			.refundPage({ ...buildQuery(), current: refundPage.current, size: refundPage.pageSize })
			.then((data) => {
				refundList.value = data.records || []
				refundPage.total = data.total || 0
			})
			.finally(() => {
				detailLoading.value = false
			})
	}

	const loadAll = () => {
		loading.value = true
		Promise.all([loadSummary(), loadPayment(), loadRefund(), loadDaily()]).finally(() => {
			loading.value = false
		})
		loadReconcile()
	}

	const onExport = ({ key }) => {
		exportLoading.value = true
		reportApi
			.exportAsync({ ...buildQuery(), type: key })
			.then((task) => {
				pollExportTask(task.id)
			})
			.finally(() => {
				exportLoading.value = false
			})
	}

	// 异步导出：轮询任务状态，完成后自动下载
	const pollExportTask = (taskId) => {
		const timer = setInterval(() => {
			reportApi.exportAsyncStatus({ id: taskId }).then((task) => {
				if (task.status === 'SUCCESS') {
					clearInterval(timer)
					reportApi.exportAsyncDownload({ id: taskId }).then((res) => {
						downloadUtil.resultDownload(res)
						message.success('导出完成，已开始下载')
					})
				} else if (task.status === 'FAILED') {
					clearInterval(timer)
					message.error(task.errorMsg || '导出失败')
				}
			})
		}, 1500)
	}

	// 对账
	const loadReconcile = () => {
		reportApi.reconcile(buildQuery()).then((data) => {
			reconcile.value = data || { consistent: true, messages: [] }
		})
	}

	// 重算当日汇总（用于数据修正后回补）
	const rebuildDaily = () => {
		const today = dayjs().format('YYYY-MM-DD')
		reportApi.rebuildDailyStat({ date: today }).then(() => {
			loadDaily()
			loadReconcile()
		})
	}

	// 清理报表缓存
	const clearCache = () => {
		reportApi.clearCache().then(() => {
			loadAll()
			loadReconcile()
		})
	}

	const loadDaily = () => {
		const [start, end] = dateRange.value || []
		reportApi.dailyStat({ startDate: start, endDate: end }).then((data) => {
			dailyList.value = data || []
		})
	}

	// 下钻：按课程查看明细
	const drillCourse = (record) => {
		query.courseId = record.courseId
		loadAll()
	}

	// 下钻：按校区查看明细
	const drillCampus = (record) => {
		query.campusId = record.campusId
		loadAll()
	}

	const reset = () => {
		dateRange.value = [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
		query.campusId = undefined
		query.courseId = undefined
		query.channel = undefined
		paymentPage.current = 1
		refundPage.current = 1
		loadAll()
	}

	const onPaymentPageChange = (pagination) => {
		paymentPage.current = pagination.current
		paymentPage.pageSize = pagination.pageSize
		loadPayment()
	}
	const onRefundPageChange = (pagination) => {
		refundPage.current = pagination.current
		refundPage.pageSize = pagination.pageSize
		loadRefund()
	}

	const rankColumns = [
		{ title: '课程', dataIndex: 'courseName' },
		{ title: '收款', dataIndex: 'paidAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '退费', dataIndex: 'refundAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '净营收', dataIndex: 'netAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '课消课时', dataIndex: 'attendLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '订单数', dataIndex: 'orderCount' },
		{ title: '操作', dataIndex: 'action', align: 'center', customRender: ({ record }) => h('a', { onClick: () => drillCourse(record) }, '查看明细') }
	]
	const campusColumns = [
		{ title: '校区', dataIndex: 'campusName' },
		{ title: '收款', dataIndex: 'paidAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '退费', dataIndex: 'refundAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '净营收', dataIndex: 'netAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '操作', dataIndex: 'action', align: 'center', customRender: ({ record }) => h('a', { onClick: () => drillCampus(record) }, '查看明细') }
	]
	const paymentColumns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '课包', dataIndex: 'packageName' },
		{ title: '实收', dataIndex: 'paidAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '渠道', dataIndex: 'payChannel' },
		{ title: '支付时间', dataIndex: 'payTime' },
		{ title: '状态', dataIndex: 'status' }
	]
	const refundColumns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '退费金额', dataIndex: 'refundAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '退费课时', dataIndex: 'refundLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '状态', dataIndex: 'status' },
		{ title: '审批时间', dataIndex: 'approveTime' }
	]
	const dailyColumns = [
		{ title: '日期', dataIndex: 'statDate' },
		{ title: '收款', dataIndex: 'paidAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '退费', dataIndex: 'refundAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '净营收', dataIndex: 'netAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '订单数', dataIndex: 'orderCount' },
		{ title: '净课消课时', dataIndex: 'netLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '课消金额', dataIndex: 'recognizedAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '新增学员', dataIndex: 'newStudentCount' }
	]
	const consumeColumns = [
		{ title: '课程', dataIndex: 'courseName' },
		{ title: '课包', dataIndex: 'coursePackageName' },
		{ title: '消课课时', dataIndex: 'attendLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '课消金额', dataIndex: 'recognizedAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '订单数', dataIndex: 'orderCount' },
		{ title: '学员数', dataIndex: 'studentCount' }
	]

	const loadCourses = () => {
		courseApi.coursePage({ current: 1, size: 100 }).then((data) => {
			courseOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}

	const loadCampusOptions = () => {
		reportApi.campusOptions().then((data) => {
			campusOptions.value = (data || []).map((item) => ({ value: item.value, label: item.label }))
		})
	}

	onMounted(() => {
		loadCourses()
		loadCampusOptions()
		loadAll()
	})
</script>

<style scoped>
	.report-filter {
		margin-bottom: 8px;
	}
	.report-chart {
		width: 100%;
		min-height: 300px;
	}
</style>
