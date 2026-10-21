<template>
	<div>
		<a-spin :spinning="loading">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillFinance('payment')">
						<a-statistic title="本月收款" :value="summary.paidAmount" :precision="2" prefix="¥" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillFinance('refund')">
						<a-statistic title="本月退费" :value="summary.refundAmount" :precision="2" prefix="¥" :value-style="{ color: '#fa541c' }" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillFinance('payment')">
						<a-statistic title="净营收" :value="summary.netAmount" :precision="2" prefix="¥" :value-style="{ color: '#52c41a' }" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillAcademic()">
						<a-statistic title="课消课时" :value="summary.attendLessons" :precision="2" suffix="课时" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillAcademic()">
						<a-statistic title="课消金额" :value="summary.recognizedAmount" :precision="2" prefix="¥" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillStudent('STUDYING')">
						<a-statistic title="本月新增学员" :value="summary.newStudentCount" suffix="人" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillStudent('STUDYING')">
						<a-statistic title="在读学员" :value="summary.studyingStudentCount" suffix="人" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="report-kpi kpi-link" @click="drillRenewWarn()">
						<a-statistic title="待续费预警" :value="summary.renewWarnCount" suffix="条" :value-style="{ color: '#faad14' }" />
					</a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="16">
					<a-card title="近 30 天课消趋势" :bordered="false">
						<div id="reportAttendChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="8">
					<a-card title="校区经营对比" :bordered="false">
						<div id="reportCampusChart" class="report-chart"></div>
					</a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="16">
					<a-card title="近 6 个月收支趋势" :bordered="false">
						<div id="reportTrendChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="8">
					<a-card title="收款渠道" :bordered="false">
						<div id="reportChannelChart" class="report-chart"></div>
					</a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="12">
					<a-card title="课程收款排行" :bordered="false">
						<div id="reportRankChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="6">
					<a-card title="续费预警" :bordered="false">
						<a-list :data-source="renewWarnList" size="small" :locale="{ emptyText: '暂无待处理预警' }">
							<template #renderItem="{ item }">
								<a-list-item class="kpi-link" @click="goStudentDetail(item)">
									<a-list-item-meta :title="item.studentName || item.studentId" :description="item.content" />
								</a-list-item>
							</template>
						</a-list>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="6">
					<a-card title="待收款订单" :bordered="false">
						<a-list :data-source="unpaidOrderList" size="small" :locale="{ emptyText: '暂无待收款订单' }">
							<template #renderItem="{ item }">
								<a-list-item class="kpi-link" @click="goStudentDetail(item)">
									<a-list-item-meta :title="item.studentName || item.studentId" :description="item.content" />
								</a-list-item>
							</template>
						</a-list>
					</a-card>
				</a-col>
			</a-row>
		</a-spin>
	</div>
</template>

<script setup name="reportDashboard">
	import { nextTick, onMounted, ref } from 'vue'
	import { useRouter } from 'vue-router'
	import { Bar, Line, Pie } from '@antv/g2plot'
	import reportApi from '@/api/report/reportApi'

	const router = useRouter()
	const loading = ref(false)
	const summary = ref({
		paidAmount: 0,
		refundAmount: 0,
		netAmount: 0,
		recognizedAmount: 0,
		attendLessons: 0,
		newStudentCount: 0,
		studyingStudentCount: 0,
		renewWarnCount: 0
	})
	const trendList = ref([])
	const channelList = ref([])
	const courseRankList = ref([])
	const campusRankList = ref([])
	const attendTrendList = ref([])
	const renewWarnList = ref([])
	const unpaidOrderList = ref([])

	let trendChart
	let channelChart
	let rankChart
	let attendChart
	let campusChart

	const money = (value) => Number(value || 0)
	const emptySummary = () => ({
		paidAmount: 0,
		refundAmount: 0,
		netAmount: 0,
		recognizedAmount: 0,
		attendLessons: 0,
		newStudentCount: 0,
		studyingStudentCount: 0,
		renewWarnCount: 0
	})

	const renderCharts = () => {
		trendChart?.destroy()
		channelChart?.destroy()
		rankChart?.destroy()
		attendChart?.destroy()
		campusChart?.destroy()

		const trendData = []
		trendList.value.forEach((item) => {
			trendData.push({ period: item.period, series: '收款', value: money(item.paidAmount) })
			trendData.push({ period: item.period, series: '退费', value: money(item.refundAmount) })
			trendData.push({ period: item.period, series: '净营收', value: money(item.netAmount) })
		})
		if (trendData.length) {
			trendChart = new Line('reportTrendChart', {
				data: trendData,
				xField: 'period',
				yField: 'value',
				seriesField: 'series',
				height: 300,
				color: ['#1677ff', '#fa541c', '#52c41a'],
				legend: { position: 'top' },
				smooth: true,
				appendPadding: [8, 0, 0, 0]
			})
			trendChart.render()
		}

		const channelData = channelList.value.map((item) => ({ type: item.channel, value: money(item.paidAmount) }))
		if (channelData.length) {
			channelChart = new Pie('reportChannelChart', {
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

		const rankData = courseRankList.value.map((item) => ({
			name: item.courseName || item.courseId || '未关联课程',
			amount: money(item.paidAmount)
		}))
		if (rankData.length) {
			rankChart = new Bar('reportRankChart', {
				data: rankData.sort((a, b) => a.amount - b.amount),
				xField: 'amount',
				yField: 'name',
				height: 300,
				color: '#1677ff',
				label: { position: 'right' }
			})
			rankChart.render()
		}

		const attendData = attendTrendList.value.map((item) => ({
			period: item.period,
			value: money(item.attendLessons)
		}))
		if (attendData.length) {
			attendChart = new Line('reportAttendChart', {
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

		const campusData = campusRankList.value.map((item) => ({
			name: item.campusName || item.campusId || '未分配校区',
			amount: money(item.paidAmount)
		}))
		if (campusData.length) {
			campusChart = new Bar('reportCampusChart', {
				data: campusData.sort((a, b) => a.amount - b.amount),
				xField: 'amount',
				yField: 'name',
				height: 300,
				color: '#13c2c2',
				label: { position: 'right' }
			})
			campusChart.render()
		}
	}

	// 指标下钻
	const drillFinance = (tab) => {
		router.push({ path: '/report/finance', query: { tab, range: 'month' } })
	}
	const drillAcademic = () => {
		router.push({ path: '/report/academic' })
	}
	const drillStudent = (status) => {
		router.push({ path: '/student', query: { status } })
	}
	const drillRenewWarn = () => {
		router.push({ path: '/student/renewWarn' })
	}
	const goStudentDetail = (item) => {
		if (item?.studentId) {
			router.push({ path: '/student/detail', query: { id: item.studentId } })
		}
	}

	const loadData = () => {
		loading.value = true
		reportApi
			.dashboard({})
			.then((data) => {
				summary.value = data?.summary || emptySummary()
				trendList.value = data?.trendList || []
				channelList.value = data?.channelList || []
				courseRankList.value = data?.courseRankList || []
				campusRankList.value = data?.campusRankList || []
				attendTrendList.value = data?.attendTrendList || []
				renewWarnList.value = data?.renewWarnList || []
				unpaidOrderList.value = data?.unpaidOrderList || []
				nextTick(renderCharts)
			})
			.finally(() => {
				loading.value = false
			})
	}

	onMounted(loadData)
</script>

<style scoped>
	.report-kpi {
		margin-bottom: 8px;
	}
	.kpi-link {
		cursor: pointer;
		transition: box-shadow 0.2s;
	}
	.kpi-link:hover {
		box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
	}
	.report-chart {
		width: 100%;
		min-height: 300px;
	}
</style>