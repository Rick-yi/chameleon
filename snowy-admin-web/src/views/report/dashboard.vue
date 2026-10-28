<template>
	<div>
		<page-header title="经营看板" desc="机构经营概览：收款、退费、课消与学员情况">
			<template #extra>
				<period-switch v-model="period" @change="loadData" />
			</template>
		</page-header>
		<a-spin :spinning="loading">
			<a-row :gutter="12">
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillFinance('payment')">
						<div class="ent-stat-label">{{ periodLabel }}收款</div>
						<div class="ent-stat-value">¥{{ fmt(summary.paidAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillFinance('refund')">
						<div class="ent-stat-label">{{ periodLabel }}退费</div>
						<div class="ent-stat-value" style="color: var(--danger-text)">¥{{ fmt(summary.refundAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillFinance('payment')">
						<div class="ent-stat-label">{{ periodLabel }}净营收</div>
						<div class="ent-stat-value" style="color: var(--success-text)">¥{{ fmt(summary.netAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillAcademic()">
						<div class="ent-stat-label">{{ periodLabel }}课消课时</div>
						<div class="ent-stat-value">
							{{ fmt(summary.attendLessons) }}
							<span style="font-size: 13px; color: var(--text-color-tertiary)">课时</span>
						</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillAcademic()">
						<div class="ent-stat-label">{{ periodLabel }}课消金额</div>
						<div class="ent-stat-value">¥{{ fmt(summary.recognizedAmount) }}</div>
					</div>
				</a-col>
				<a-col :xs="12" :sm="12" :md="8" :xl="6">
					<div class="ent-stat kpi-link" @click="drillStudent('STUDYING')">
						<div class="ent-stat-label">{{ periodLabel }}新增学员</div>
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
						<div class="ent-stat-value" style="color: var(--warning-text)">{{ summary.renewWarnCount || 0 }}</div>
					</div>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="16">
					<a-card :title="attendTitle" :bordered="false">
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
					<a-card :title="trendTitle" :bordered="false">
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
	import { payChannelText } from '@/utils/payChannel'
	import { PERIOD_LABEL, periodDateRange } from '@/utils/reportPeriod'
	import PeriodSwitch from '@/components/PeriodSwitch/index.vue'

	const router = useRouter()
	const loading = ref(false)
	const period = ref('MONTH')
	const periodLabel = computed(() => PERIOD_LABEL[period.value] || '本月')
	const periodRange = computed(() => {
		const [start, end] = periodDateRange(period.value)
		return { startTime: `${start} 00:00:00`, endTime: `${end} 23:59:59` }
	})
	const trendTitle = computed(
		() =>
			({
				DAY: '近 30 天收支趋势',
				MONTH: '近 12 个月收支趋势',
				YEAR: '近 5 年收支趋势'
			})[period.value] || '收支趋势'
	)
	const attendTitle = computed(
		() =>
			({
				DAY: '近 30 天课消趋势',
				MONTH: '近 12 个月课消趋势',
				YEAR: '近 5 年课消趋势'
			})[period.value] || '课消趋势'
	)
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
				color: ['#4bb964', '#e5484d', '#f37021'],
				legend: { position: 'top' },
				smooth: true,
				appendPadding: [8, 0, 0, 0]
			})
			trendChart.render()
		}

		const channelData = channelList.value.map((item) => ({
			type: payChannelText(item.channel),
			value: money(item.paidAmount)
		}))
		if (channelData.length) {
			channelChart = new Pie('reportChannelChart', {
				data: channelData,
				angleField: 'value',
				colorField: 'type',
				color: ['#4bb964', '#f37021', '#2ba471', '#6f5bd8', '#2ba8b8', '#e5484d'],
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
				color: '#4bb964',
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
		router.push({ path: '/report/finance', query: { tab, range: period.value.toLowerCase() } })
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
			.dashboard({ ...periodRange.value, dimension: period.value })
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
	.report-chart {
		width: 100%;
		min-height: 300px;
	}
</style>
