<template>
	<div>
		<a-card :bordered="false" class="report-filter">
			<a-space>
				<span>到期范围</span>
				<a-select v-model:value="expiringDays" :options="dayOptions" style="width: 120px" @change="loadExpiring" />
				<a-button type="primary" @click="loadAll">刷新</a-button>
				<a-dropdown v-if="hasPerm('reportPlatformExport')">
					<a-button>
						导出
						<down-outlined />
					</a-button>
					<template #overlay>
						<a-menu @click="onExport">
							<a-menu-item key="OVERVIEW">平台概览</a-menu-item>
							<a-menu-item key="TREND">租户增长趋势</a-menu-item>
							<a-menu-item key="PACKAGE">套餐分布</a-menu-item>
							<a-menu-item key="EXPIRING">到期租户</a-menu-item>
							<a-menu-item key="RANK">租户活跃度排行</a-menu-item>
						</a-menu>
					</template>
				</a-dropdown>
			</a-space>
		</a-card>

		<a-spin :spinning="loading">
			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="租户总数" :value="overview.tenantTotal" suffix="家" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="本月新增" :value="overview.newTenantCount" suffix="家" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="活跃租户(30天登录)" :value="overview.activeTenantCount" suffix="家" :value-style="{ color: '#52c41a' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="订单活跃租户" :value="overview.orderActiveTenantCount" suffix="家" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="30 天内到期" :value="overview.expiringTenantCount" suffix="家" :value-style="{ color: '#faad14' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="学员总数" :value="overview.studentTotal" suffix="人" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="预估 ARR" :value="overview.estimatedArr" :precision="2" prefix="¥" /></a-card>
				</a-col>
			</a-row>

			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="24" :xl="16">
					<a-card title="租户增长趋势" :bordered="false">
						<div id="platformTenantTrendChart" class="report-chart"></div>
					</a-card>
				</a-col>
				<a-col :xs="24" :xl="8">
					<a-card title="套餐分布" :bordered="false">
						<div id="platformPackageChart" class="report-chart"></div>
					</a-card>
				</a-col>
			</a-row>

			<a-card title="到期租户" :bordered="false" style="margin-top: 16px">
				<a-table
					:columns="expiringColumns"
					:data-source="expiringList"
					:row-key="(r) => r.tenantId"
					:pagination="{ pageSize: 10, showSizeChanger: true }"
					bordered
					size="middle"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'name'">
							<a @click="goTenant(record)">{{ record.name }}</a>
						</template>
					</template>
				</a-table>
			</a-card>

			<a-card title="租户活跃度排行" :bordered="false" style="margin-top: 16px">
				<a-table
					:columns="rankColumns"
					:data-source="rankList"
					:row-key="(r) => r.tenantId"
					:pagination="{ pageSize: 10, showSizeChanger: true }"
					bordered
					size="middle"
				/>
			</a-card>
		</a-spin>
	</div>
</template>

<script setup name="platformDashboard">
	import { nextTick, onMounted, ref } from 'vue'
	import { useRouter } from 'vue-router'
	import { Line, Pie } from '@antv/g2plot'
	import platformReportApi from '@/api/platform/reportApi'
	import downloadUtil from '@/utils/downloadUtil'

	const router = useRouter()

	const loading = ref(false)
	const expiringDays = ref(30)
	const dayOptions = [
		{ value: 7, label: '7 天内' },
		{ value: 30, label: '30 天内' },
		{ value: 90, label: '90 天内' }
	]
	const overview = ref({
		tenantTotal: 0,
		newTenantCount: 0,
		activeTenantCount: 0,
		orderActiveTenantCount: 0,
		expiringTenantCount: 0,
		studentTotal: 0,
		orderCount: 0,
		paidAmount: 0,
		estimatedArr: 0,
		packageCount: 0
	})
	const trendList = ref([])
	const packageList = ref([])
	const expiringList = ref([])
	const rankList = ref([])

	let trendChart
	let packageChart

	const renderCharts = () => {
		trendChart?.destroy()
		packageChart?.destroy()
		const trendData = []
		trendList.value.forEach((item) => {
			trendData.push({ period: item.period, series: '当月新增', value: Number(item.newCount || 0) })
			trendData.push({ period: item.period, series: '累计租户', value: Number(item.totalCount || 0) })
		})
		if (trendData.length) {
			trendChart = new Line('platformTenantTrendChart', {
				data: trendData,
				xField: 'period',
				yField: 'value',
				seriesField: 'series',
				height: 320,
				color: ['#1677ff', '#52c41a'],
				smooth: true,
				legend: { position: 'top' }
			})
			trendChart.render()
		}

		const packageData = packageList.value.map((item) => ({
			type: item.packageName || item.packageId || '未分配套餐',
			value: Number(item.tenantCount || 0)
		}))
		if (packageData.length) {
			packageChart = new Pie('platformPackageChart', {
				data: packageData,
				angleField: 'value',
				colorField: 'type',
				height: 320,
				radius: 0.8,
				label: { type: 'outer', content: '{name} {percentage}' },
				legend: { position: 'bottom' }
			})
			packageChart.render()
		}
	}

	const loadAll = () => {
		loading.value = true
		Promise.all([
			platformReportApi.overview(),
			platformReportApi.tenantTrend({ months: 12 }),
			platformReportApi.packageDistribution(),
			platformReportApi.expiring({ days: expiringDays.value }),
			platformReportApi.tenantRank({ limit: 20 })
		])
			.then(([overviewData, trendData, packageData, expiringData, rankData]) => {
				overview.value = overviewData || overview.value
				trendList.value = trendData || []
				packageList.value = packageData || []
				expiringList.value = expiringData || []
				rankList.value = rankData || []
				nextTick(renderCharts)
			})
			.finally(() => {
				loading.value = false
			})
	}

	const loadExpiring = () => {
		platformReportApi.expiring({ days: expiringDays.value }).then((data) => {
			expiringList.value = data || []
		})
	}

	const onExport = ({ key }) => {
		platformReportApi.exportReport({ type: key }).then((res) => {
			downloadUtil.resultDownload(res)
		})
	}

	// 下钻：到期租户 → 租户管理
	const goTenant = (record) => {
		router.push({ path: '/platform/tenant', query: { code: record.code } })
	}

	const expiringColumns = [
		{ title: '机构编码', dataIndex: 'code' },
		{ title: '机构名称', dataIndex: 'name' },
		{ title: '套餐', dataIndex: 'packageName' },
		{ title: '到期时间', dataIndex: 'expireTime' },
		{
			title: '剩余天数',
			dataIndex: 'daysLeft',
			customRender: ({ text }) => {
				const days = Number(text ?? 0)
				return days < 0 ? `已过期 ${Math.abs(days)} 天` : `${days} 天`
			}
		},
		{ title: '状态', dataIndex: 'status' }
	]
	const rankColumns = [
		{ title: '机构名称', dataIndex: 'tenantName' },
		{ title: '学员数', dataIndex: 'studentCount' },
		{ title: '订单数', dataIndex: 'orderCount' },
		{ title: '收款金额', dataIndex: 'paidAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '最近支付时间', dataIndex: 'lastPayTime' },
		{ title: '最近登录时间', dataIndex: 'lastLoginTime' }
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
