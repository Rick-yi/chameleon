<template>
	<div>
		<page-header title="运营埋点" desc="业务动作与功能使用的真实数据：招生漏斗、交付动作、页面使用" />

		<!-- 漏斗 -->
		<a-card :bordered="false" title="招生到收款转化漏斗" style="margin-bottom: 12px">
			<a-row :gutter="12">
				<a-col v-for="(node, index) in summary.funnelList" :key="node.eventCode" :xs="12" :sm="8" :md="4">
					<div class="funnel-item">
						<div class="funnel-name">{{ node.name }}</div>
						<div class="funnel-count">{{ node.count }}</div>
						<div v-if="node.rate !== null && node.rate !== undefined" class="funnel-rate">
							转化 {{ node.rate }}%
						</div>
						<div v-else class="funnel-rate">起点</div>
						<div class="funnel-bar">
							<span :style="{ width: funnelWidth(index) }"></span>
						</div>
					</div>
				</a-col>
			</a-row>
		</a-card>

		<a-row :gutter="12">
			<!-- 事件计数 -->
			<a-col :xs="24" :xl="10">
				<a-card :bordered="false" title="事件计数" style="margin-bottom: 12px">
					<a-table
						:columns="countColumns"
						:data-source="countList"
						:row-key="(r) => r.eventCode"
						size="small"
						:pagination="false"
						:scroll="{ y: 420 }"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'count'">
								<span :class="{ 'zero-count': !record.count }">{{ record.count }}</span>
							</template>
						</template>
					</a-table>
				</a-card>
			</a-col>

			<!-- 趋势 -->
			<a-col :xs="24" :xl="14">
				<a-card :bordered="false" title="近 30 天动作趋势" style="margin-bottom: 12px">
					<a-empty v-if="!trendList.length" description="暂无埋点数据" />
					<div v-else id="eventTrendChart" class="report-chart"></div>
				</a-card>
			</a-col>
		</a-row>

		<!-- 流水 -->
		<a-card :bordered="false" title="埋点流水">
			<template #extra>
				<a-space>
					<a-select v-model:value="queryForm.eventCode" placeholder="全部事件" :options="codeOptions" allow-clear style="width: 180px" @change="loadStream" />
					<a-button @click="loadStream">
						<template #icon><reload-outlined /></template>
						刷新
					</a-button>
				</a-space>
			</template>
			<a-table
				:columns="streamColumns"
				:data-source="streamList"
				:row-key="(r) => r.id"
				size="small"
				:loading="loading"
				:pagination="{ current: page.current, pageSize: page.size, total: page.total, onChange: onPageChange }"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'eventName'">
						<a-tag>{{ record.eventName || record.eventCode }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'eventTime'">
						{{ (record.eventTime || '').slice(0, 19).replace('T', ' ') }}
					</template>
					<template v-if="column.dataIndex === 'props'">
						<span style="font-size: 12px; color: rgba(0,0,0,.55)">{{ record.props || '-' }}</span>
					</template>
				</template>
			</a-table>
		</a-card>
	</div>
</template>

<script setup name="reportEventAnalysis">
	import { Line } from '@antv/g2plot'
	import { message } from 'ant-design-vue'
	import eventApi from '@/api/platform/eventApi'

	const loading = ref(false)
	const summary = ref({ funnelList: [], eventCountList: [], trendList: [] })
	const countList = ref([])
	const trendList = ref([])
	const streamList = ref([])
	const codeOptions = ref([])
	const queryForm = ref({ eventCode: undefined })
	const page = ref({ current: 1, size: 10, total: 0 })

	const countColumns = [
		{ title: '事件', dataIndex: 'eventName' },
		{ title: '次数', dataIndex: 'count', width: 90 }
	]
	const streamColumns = [
		{ title: '时间', dataIndex: 'eventTime', width: 170 },
		{ title: '事件', dataIndex: 'eventName', width: 120 },
		{ title: '对象', dataIndex: 'targetName', width: 140 },
		{ title: '操作人', dataIndex: 'userName', width: 100 },
		{ title: '属性', dataIndex: 'props' }
	]

	// 关键事件（趋势图只画这几条，避免噪声）
	const TREND_EVENTS = ['LEAD_CREATE', 'LEAD_FOLLOW', 'LEAD_CONVERT', 'ORDER_CREATE', 'ORDER_PAY', 'ATTEND_CONFIRM', 'CLASS_ENROLL', 'SCHEDULE_GENERATE']

	const loadSummary = () => {
		eventApi
			.summary({ days: 30 })
			.then((data) => {
				summary.value = data || { funnelList: [], eventCountList: [], trendList: [] }
				countList.value = (summary.value.eventCountList || []).filter((item) => item.count > 0)
				trendList.value = (summary.value.trendList || []).filter((item) => TREND_EVENTS.includes(item.eventCode))
				renderTrend()
			})
			.catch(() => message.error('埋点统计加载失败'))
	}

	// 漏斗条宽度：以第一个环节为基准
	const funnelWidth = (index) => {
		const list = summary.value.funnelList || []
		const first = list.length ? Number(list[0].count || 0) : 0
		if (!first) return '0%'
		const current = Number(list[index]?.count || 0)
		return `${Math.max(Math.round((current / first) * 100), current > 0 ? 4 : 0)}%`
	}

	// 事件码 → 中文名（图例用）
	const eventNameMap = computed(() => {
		const map = {}
		for (const item of codeOptions.value) {
			map[item.value] = item.label
		}
		return map
	})
	const renderTrend = async () => {
		if (!trendList.value.length) return
		// 等 DOM 更新后再渲染图表
		await nextTick()
		const chart = new Line('eventTrendChart', {
			data: trendList.value.map((item) => ({
				date: item.date,
				type: eventNameMap.value[item.eventCode] || item.eventCode,
				value: item.count
			})),
			xField: 'date',
			yField: 'value',
			seriesField: 'type',
			color: ['#1668dc', '#2ba471', '#d89614', '#e5484d', '#6f5bd8', '#2ba8b8'],
			height: 320,
			smooth: true,
			legend: { position: 'top' },
			appendPadding: [8, 0, 0, 0]
		})
		chart.render()
	}

	const loadStream = () => {
		loading.value = true
		eventApi
			.eventPage({ current: page.value.current, size: page.value.size, eventCode: queryForm.value.eventCode })
			.then((data) => {
				streamList.value = data?.records || []
				page.value.total = data?.total || 0
			})
			.finally(() => (loading.value = false))
	}
	const onPageChange = (current, size) => {
		page.value.current = current
		page.value.size = size
		loadStream()
	}
	const loadCodes = () => {
		eventApi.codes().then((data) => {
			codeOptions.value = data || []
		})
	}

	loadCodes()
	loadSummary()
	loadStream()
</script>

<style lang="less" scoped>
	.funnel-item {
		padding: 12px 14px;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
		background: #fff;
	}
	.funnel-name {
		font-size: 13px;
		color: #86909c;
	}
	.funnel-count {
		margin-top: 4px;
		font-size: 20px;
		font-weight: 600;
		color: #1d2129;
	}
	.funnel-rate {
		margin-top: 2px;
		font-size: 12px;
		color: #4e5969;
	}
	/* 漏斗条：直观看出各环节的留存宽度 */
	.funnel-bar {
		margin-top: 8px;
		height: 4px;
		border-radius: 2px;
		background: var(--background-color-base);
		overflow: hidden;
		span {
			display: block;
			height: 100%;
			border-radius: 2px;
			background: var(--primary-6);
			transition: width 0.3s;
		}
	}
	.zero-count {
		color: #c9cdd4;
	}
</style>
