<template>
	<div>
		<page-header title="经营看板" desc="机构经营概览：收款、退费、课消与学员情况" />
		<a-spin :spinning="loading">
			<a-row :gutter="12">
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillFinance('payment')">
						<div class="ent-stat-label">本月收款</div>
						<div class="ent-stat-value">¥{{ fmt(summary.paidAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillFinance('refund')">
						<div class="ent-stat-label">本月退费</div>
						<div class="ent-stat-value" style="color: #e5484d">¥{{ fmt(summary.refundAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillFinance('payment')">
						<div class="ent-stat-label">净营收</div>
						<div class="ent-stat-value" style="color: #2ba471">¥{{ fmt(summary.netAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillAcademic()">
						<div class="ent-stat-label">课消课时</div>
						<div class="ent-stat-value">{{ fmt(summary.attendLessons) }} <span style="font-size: 13px; color: #86909c">课时</span></div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillAcademic()">
						<div class="ent-stat-label">课消金额</div>
						<div class="ent-stat-value">¥{{ fmt(summary.recognizedAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillStudent('STUDYING')">
						<div class="ent-stat-label">本月新增学员</div>
						<div class="ent-stat-value">{{ summary.newStudentCount || 0 }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillStudent('STUDYING')">
						<div class="ent-stat-label">在读学员</div>
						<div class="ent-stat-value">{{ summary.studyingStudentCount || 0 }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillRenewWarn()">
						<div class="ent-stat-label">待续费预警</div>
						<div class="ent-stat-value" style="color: #d89614">{{ summary.renewWarnCount || 0 }}</div>
					</div>
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
	// 金额/数字统一格式（千分位 + 两位小数）
	const fmt = (value) => {
		const num = Number(value || 0)
		return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
	}
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
				color: ['#1668dc', '#e5484d', '#2ba471'],
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
				color: ['#1668dc', '#2ba471', '#d89614', '#6f5bd8', '#2ba8b8', '#e5484d'],
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
				color: '#1668dc',
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
				color: '#6f5bd8',
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
				color: '#2ba8b8',
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
			router.push({ path: '/student', query: { id: item.studentId } })
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