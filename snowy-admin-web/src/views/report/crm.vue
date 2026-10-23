<template>
	<div>
		<page-header title="招生报表" desc="线索漏斗、来源渠道、跟进排行与试听转化" />
		<a-card :bordered="false" class="report-filter">
			<a-form layout="inline">
				<a-form-item label="时间">
					<a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" allow-clear />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="loadAll">查询</a-button>
						<a-button @click="reset">重置</a-button>
						<a-dropdown v-if="hasPerm('/crm/report/export')">
							<a-button>
								导出
								<down-outlined />
							</a-button>
							<template #overlay>
								<a-menu @click="onExport">
									<a-menu-item key="FUNNEL">线索漏斗</a-menu-item>
									<a-menu-item key="SOURCE">来源渠道</a-menu-item>
									<a-menu-item key="FOLLOW_RANK">跟进排行</a-menu-item>
									<a-menu-item key="TRIAL">试听转化</a-menu-item>
								</a-menu>
							</template>
						</a-dropdown>
					</a-space>
				</a-form-item>
			</a-form>
		</a-card>

		<a-spin :spinning="loading">
			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="12" :sm="8" :md="8" :xl="6">
					<a-card :bordered="false" class="kpi-link" @click="router.push({ path: '/crm', query: { status: 'NEW' } })"><a-statistic title="线索量" :value="leadCount" suffix="条" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="kpi-link" @click="router.push({ path: '/crm', query: { status: 'ENROLLED' } })"><a-statistic title="已报名" :value="enrolledCount" suffix="人" :value-style="{ color: '#52c41a' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="报名转化率" :value="enrollRate" :precision="2" suffix="%" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="试听数" :value="trial.trialCount" suffix="次" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="试听报名数" :value="trial.enrolledCount" suffix="人" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="试听转化率" :value="trial.conversionRate" :precision="2" suffix="%" /></a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="14">
					<a-card title="线索漏斗" :bordered="false">
						<div id="crmFunnelChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="10">
					<a-card title="来源渠道" :bordered="false">
						<div id="crmSourceChart" class="report-chart"></div>
					</a-card>
				</a-col>
			</a-row>

			<a-card title="来源渠道明细" :bordered="false" style="margin-top: 16px">
				<a-table
					:columns="sourceColumns"
					:data-source="sourceList"
					:row-key="(r) => r.source"
					:pagination="false"
					bordered
					size="middle"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'sourceName'">
							<a @click="goLead(record)">{{ record.sourceName }}</a>
						</template>
					</template>
				</a-table>
			</a-card>

			<a-card title="跟进排行" :bordered="false" style="margin-top: 16px">
				<a-table
					:columns="followColumns"
					:data-source="followRankList"
					:row-key="(r) => r.userId"
					:pagination="{ pageSize: 10, showSizeChanger: true }"
					bordered
					size="middle"
				/>
			</a-card>
		</a-spin>
	</div>
</template>

<script setup name="reportCrm">
	import { computed, nextTick, onMounted, ref } from 'vue'
	import { useRouter } from 'vue-router'
	import { Funnel, Pie } from '@antv/g2plot'
	import dayjs from 'dayjs'
	import reportApi from '@/api/report/reportApi'
	import { runAsyncExport } from '@/utils/asyncExport'

	const router = useRouter()

	const loading = ref(false)
	const dateRange = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
	const funnelList = ref([])
	const sourceList = ref([])
	const followRankList = ref([])
	const trial = ref({
		trialCount: 0,
		doneCount: 0,
		interestedCount: 0,
		notInterestedCount: 0,
		enrolledCount: 0,
		conversionRate: 0
	})

	let funnelChart
	let sourceChart

	const leadCount = computed(() => (funnelList.value[0]?.count ? Number(funnelList.value[0].count) : 0))
	const enrolledCount = computed(() => (funnelList.value[3]?.count ? Number(funnelList.value[3].count) : 0))
	const enrollRate = computed(() => (funnelList.value[3]?.conversionRate ? Number(funnelList.value[3].conversionRate) : 0))

	const buildQuery = () => {
		const [start, end] = dateRange.value || []
		return {
			startTime: start ? `${start} 00:00:00` : undefined,
			endTime: end ? `${end} 23:59:59` : undefined
		}
	}

	const renderCharts = () => {
		funnelChart?.destroy()
		sourceChart?.destroy()
		const funnelData = funnelList.value.map((item) => ({
			stage: item.stageName,
			count: Number(item.count || 0)
		}))
		if (funnelData.length) {
			funnelChart = new Funnel('crmFunnelChart', {
				data: funnelData,
				xField: 'stage',
				yField: 'count',
				height: 320,
				legend: false,
				label: { position: 'inside', content: '{name} {value}' }
			})
			funnelChart.render()
		}

		const sourceData = sourceList.value.map((item) => ({
			type: item.sourceName || item.source,
			value: Number(item.leadCount || 0)
		}))
		if (sourceData.length) {
			sourceChart = new Pie('crmSourceChart', {
				data: sourceData,
				angleField: 'value',
				colorField: 'type',
				color: ['#1668dc', '#2ba471', '#d89614', '#6f5bd8', '#2ba8b8', '#e5484d'],
				height: 320,
				radius: 0.8,
				label: { type: 'outer', content: '{name} {percentage}' },
				legend: { position: 'bottom' }
			})
			sourceChart.render()
		}
	}

	const loadAll = () => {
		loading.value = true
		const params = buildQuery()
		Promise.all([
			reportApi.crmFunnel(params),
			reportApi.crmSource(params),
			reportApi.crmFollowRank(params),
			reportApi.crmTrial(params)
		])
			.then(([funnelData, sourceData, followData, trialData]) => {
				funnelList.value = funnelData || []
				sourceList.value = sourceData || []
				followRankList.value = followData || []
				trial.value = trialData || trial.value
				nextTick(renderCharts)
			})
			.finally(() => {
				loading.value = false
			})
	}

	const reset = () => {
		dateRange.value = [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
		loadAll()
	}

	const onExport = ({ key }) => {
		// 走通用异步导出，避免同步导出超时
		runAsyncExport('CRM_REPORT', { ...buildQuery(), reportType: key })
	}

	// 下钻：来源 → 招生线索
	const goLead = (record) => {
		router.push({ path: '/crm', query: { source: record.source } })
	}

	const sourceColumns = [
		{ title: '来源', dataIndex: 'sourceName' },
		{ title: '线索数', dataIndex: 'leadCount' },
		{ title: '报名数', dataIndex: 'enrolledCount' },
		{ title: '转化率', dataIndex: 'conversionRate', customRender: ({ text }) => `${Number(text || 0).toFixed(2)}%` }
	]
	const followColumns = [
		{ title: '跟进人', dataIndex: 'userName' },
		{ title: '跟进次数', dataIndex: 'followCount' },
		{ title: '跟进线索数', dataIndex: 'leadCount' },
		{ title: '报名数', dataIndex: 'enrolledCount' }
	]

	onMounted(loadAll)
</script>

<style scoped>
	.report-filter {
		margin-bottom: 8px;
	}
	.report-chart {
		width: 100%;
		min-height: 320px;
	}
</style>
