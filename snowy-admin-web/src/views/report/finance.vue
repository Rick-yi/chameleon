<template>
	<div>
		<page-header title="财务报表" desc="收款、退费、课消与收入确认口径分析" />
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
				<a-form-item label="对比">
					<a-select v-model:value="compareMode" :options="compareModeOptions" style="width: 130px" @change="onCompareModeChange" />
				</a-form-item>
				<a-form-item v-if="compareMode === 'CUSTOM'" label="对比期">
					<a-range-picker v-model:value="customCompareRange" value-format="YYYY-MM-DD" allow-clear @change="loadCompare" />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="loadAll">查询</a-button>
						<a-button @click="reset">重置</a-button>
						<a-dropdown v-if="hasPerm('/finance/report/export')">
							<a-button :loading="exportLoading">
								导出
								<down-outlined />
							</a-button>
							<template #overlay>
								<a-menu @click="onExport">
									<a-menu-item key="PAYMENT">收款明细</a-menu-item>
									<a-menu-item key="REFUND">退费明细</a-menu-item>
									<a-menu-item key="COURSE_SUMMARY">课程汇总</a-menu-item>
									<a-menu-divider />
									<a-menu-item key="__records">导出记录</a-menu-item>
								</a-menu>
							</template>
						</a-dropdown>
					</a-space>
				</a-form-item>
			</a-form>
			<div class="data-updated">
				数据更新时间：{{ dataUpdatedAt || '—' }}
				<span v-if="metaInfo.dailyStatLatestDate"> · 最近日汇总：{{ metaInfo.dailyStatLatestDate }}</span>
				<span v-if="metaInfo.dailyStatUpdatedAt">（{{ metaInfo.dailyStatUpdatedAt }}）</span>
				<a-tooltip>
					<template #title>
						<div>实时聚合：收款、退费、点名提交后即时生效。</div>
						<div>日汇总：每天 00:30 汇总前一天，已完整覆盖的区间优先走日汇总。</div>
						<div>未覆盖的区间自动回落实时聚合，保证报表与业务明细一致。</div>
					</template>
					<question-circle-outlined class="metric-help" />
				</a-tooltip>
				<span v-if="exportTask" class="export-progress">
					<a-tag :color="exportStatusColor(exportTask.status)">
						{{ exportTypeText(exportTask.exportType) }}：{{ exportStatusText(exportTask.status) }}
					</a-tag>
					<a @click="openExportRecords">查看导出记录</a>
				</span>
			</div>
		</a-card>

		<a-drawer v-model:open="recordsOpen" title="导出记录" :width="560" :body-style="{ padding: '12px 16px' }">
			<a-spin :spinning="exportRecordsLoading">
				<a-empty v-if="!exportRecordsLoading && !exportRecords.length" description="暂无导出记录" />
				<div v-for="item in exportRecords" :key="item.id" class="export-rec-row">
					<div class="export-rec-main">
						<div class="export-rec-title">{{ item.fileName || exportTypeText(item.exportType) }}</div>
						<div class="export-rec-sub">
							<a-tag :color="exportStatusColor(item.status)" style="margin-right: 6px">{{ exportStatusText(item.status) }}</a-tag>
							{{ item.createTime }}
							<span v-if="item.rowCount"> · {{ item.rowCount }} 行</span>
						</div>
						<div v-if="item.status === 'FAILED' && item.errorMsg" class="export-rec-err">{{ item.errorMsg }}</div>
					</div>
					<a-button v-if="item.status === 'SUCCESS'" type="link" size="small" @click="downloadExportRecord(item)">下载</a-button>
				</div>
			</a-spin>
		</a-drawer>

		<a-alert
			v-if="compareMode === 'CUSTOM' && compareData.compareStartTime"
			type="info"
			show-icon
			style="margin-top: 12px"
			:message="`自定义对比期：${formatRange(compareData.compareStartTime, compareData.compareEndTime)}`"
			:description="`对比期收款 ¥${money(customSummary.paidAmount).toFixed(2)}，退费 ¥${money(customSummary.refundAmount).toFixed(2)}，净营收 ¥${money(customSummary.netAmount).toFixed(2)}，已支付订单 ${customSummary.orderCount || 0} 笔`"
		/>

		<a-spin :spinning="loading">
			<a-row :gutter="12" style="margin-top: 12px">
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat kpi-link" @click="activeTab = 'payment'">
						<div class="ent-stat-label">
							收款金额
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.paidAmount" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">¥{{ money(summary.paidAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
						<delta-group
							:current="summary.paidAmount"
							:previous="prevSummary.paidAmount"
							:yoy="yoySummary.paidAmount"
							:custom="compareMode === 'CUSTOM' ? customSummary.paidAmount : undefined"
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat kpi-link" @click="activeTab = 'refund'">
						<div class="ent-stat-label">
							退费金额
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.refundAmount" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value" style="color: var(--danger-text)">¥{{ money(summary.refundAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
						<delta-group
							:current="summary.refundAmount"
							:previous="prevSummary.refundAmount"
							:yoy="yoySummary.refundAmount"
							:custom="compareMode === 'CUSTOM' ? customSummary.refundAmount : undefined" invert
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat">
						<div class="ent-stat-label">
							净营收
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.netAmount" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value" style="color: var(--success-text)">¥{{ money(summary.netAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
						<delta-group
							:current="summary.netAmount"
							:previous="prevSummary.netAmount"
							:yoy="yoySummary.netAmount"
							:custom="compareMode === 'CUSTOM' ? customSummary.netAmount : undefined"
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat kpi-link" @click="router.push('/report/academic')">
						<div class="ent-stat-label">
							课消金额
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.recognizedAmount" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">¥{{ money(summary.recognizedAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
						<delta-group
							:current="summary.recognizedAmount"
							:previous="prevSummary.recognizedAmount"
							:yoy="yoySummary.recognizedAmount"
							:custom="compareMode === 'CUSTOM' ? customSummary.recognizedAmount : undefined"
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat kpi-link" @click="activeTab = 'payment'">
						<div class="ent-stat-label">
							已支付订单数
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.orderCount" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">{{ summary.orderCount }}笔</div>
						<delta-group
							:current="summary.orderCount"
							:previous="prevSummary.orderCount"
							:yoy="yoySummary.orderCount"
							:custom="compareMode === 'CUSTOM' ? customSummary.orderCount : undefined"
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat">
						<div class="ent-stat-label">
							客单价
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.avgOrderAmount" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">¥{{ money(summary.avgOrderAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
						<delta-group
							:current="summary.avgOrderAmount"
							:previous="prevSummary.avgOrderAmount"
							:yoy="yoySummary.avgOrderAmount"
							:custom="compareMode === 'CUSTOM' ? customSummary.avgOrderAmount : undefined"
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat">
						<div class="ent-stat-label">
							生均收入
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.perStudentRevenue" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">¥{{ money(summary.perStudentRevenue).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
						<delta-group
							:current="summary.perStudentRevenue"
							:previous="prevSummary.perStudentRevenue"
							:yoy="yoySummary.perStudentRevenue"
							:custom="compareMode === 'CUSTOM' ? customSummary.perStudentRevenue : undefined"
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat">
						<div class="ent-stat-label">
							退费率
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.refundRate" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">{{ summary.refundRate }}%</div>
						<delta-group
							:current="summary.refundRate"
							:previous="prevSummary.refundRate"
							:yoy="yoySummary.refundRate"
							:custom="compareMode === 'CUSTOM' ? customSummary.refundRate : undefined" invert
						/>
					</div>
				</a-col>
				<a-col :xs="12" :sm="8" :md="8" :xl="8">
					<div class="ent-stat">
						<div class="ent-stat-label">
							实收率
							<a-tooltip><template #title><div v-for="(line, i) in metricTips.collectionRate" :key="i">{{ line }}</div></template><question-circle-outlined class="metric-help" /></a-tooltip>
						</div>
						<div class="ent-stat-value">{{ summary.collectionRate }}%</div>
						<delta-group
							:current="summary.collectionRate"
							:previous="prevSummary.collectionRate"
							:yoy="yoySummary.collectionRate"
							:custom="compareMode === 'CUSTOM' ? customSummary.collectionRate : undefined"
						/>
					</div>
				</a-col>
			</a-row>

			<a-row :gutter="12" style="margin-top: 12px">
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

			<a-row :gutter="12" style="margin-top: 12px">
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

			<a-card :bordered="false" style="margin-top: 16px" v-if="hasPerm('/finance/report/consume')">
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
				<!-- 重算/清缓存属于运维操作，仅系统管理员与平台运营可见 -->
				<a-space v-if="isOpsRole" style="margin-bottom: 12px">
					<a-button @click="loadReconcile">重新对账</a-button>
					<a-button @click="rebuildDaily">重算当日汇总</a-button>
					<a-button @click="clearCache">清理报表缓存</a-button>
				</a-space>
				<div v-else class="data-updated" style="margin-bottom: 12px">
					对账结果自动刷新；重算日汇总、清理报表缓存为运维操作，如需处理请联系系统管理员或平台运营。
				</div>
				<a-alert
					:type="reconcile.consistent ? 'success' : 'warning'"
					show-icon
					:message="reconcile.consistent ? '对账正常：报表数字与业务明细一致' : '存在差异，请核对下列说明'"
					:description="(reconcile.messages || []).join('；') || '收款、退费、课消口径一致'"
				/>
			</a-card>
		</a-spin>
	</div>
</template>

<script setup name="reportFinance">
	import { h, nextTick, onMounted, reactive, ref } from 'vue'
	import { useRoute, useRouter } from 'vue-router'
	import { Line, Pie } from '@antv/g2plot'
	import { message } from 'ant-design-vue'
	import dayjs from 'dayjs'
	import reportApi from '@/api/report/reportApi'
	import financeApi from '@/api/finance/financeApi'
	import courseApi from '@/api/course/courseApi'
	import { payChannelText } from '@/utils/payChannel'
	import downloadUtil from '@/utils/downloadUtil'
	import tool from '@/utils/tool'

	const router = useRouter()
	const route = useRoute()
	const loading = ref(false)
	const detailLoading = ref(false)
	const activeTab = ref('payment')
	const dateRange = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
	// 对比期：默认环比，可切换同比或自定义区间
	const compareMode = ref('MOM')
	const customCompareRange = ref()
	const compareData = ref({})
	const customSummary = ref({})
	const dataUpdatedAt = ref('')
	const metaInfo = ref({})
	// 危险操作仅对运维/平台角色开放；业务角色只读对账结论
	const roleCodeList = tool.data.get('USER_INFO')?.roleCodeList || []
	const isOpsRole = roleCodeList.includes('superAdmin') || roleCodeList.includes('platformAdmin')
	const query = reactive({ campusId: undefined, courseId: undefined, channel: undefined })
	const campusOptions = ref([])
	const courseOptions = ref([])
	const channelOptions = [
		{ value: 'OFFLINE', label: '线下转账' },
		{ value: 'CASH', label: '现金' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'ALIPAY', label: '支付宝' }
	]
	const compareModeOptions = [
		{ value: 'MOM', label: '环比（上期）' },
		{ value: 'YOY', label: '同比（去年同期）' },
		{ value: 'CUSTOM', label: '自定义对比期' }
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
	const exportTask = ref(null)
	const exportRecords = ref([])
	const exportRecordsLoading = ref(false)
	const recordsOpen = ref(false)
	const paymentPage = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
	const refundPage = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

	let trendChart
	let channelChart
	let attendChart

	const money = (value) => Number(value || 0)
	const formatRange = (start, end) => `${(start || '').slice(0, 10)} ~ ${(end || '').slice(0, 10)}`
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

	// 对比涨跌幅：分母为 0 时不展示百分比，避免出现无穷大
	const deltaPct = (current, previous) => {
		const cur = Number(current) || 0
		const prev = Number(previous) || 0
		if (!prev) return null
		return ((cur - prev) / prev) * 100
	}
	const deltaText = (current, previous) => {
		const pct = deltaPct(current, previous)
		if (pct === null) return '—'
		return `${pct > 0 ? '+' : ''}${pct.toFixed(1)}%`
	}
	const deltaTone = (current, previous, invert = false) => {
		const pct = deltaPct(current, previous)
		if (pct === null || Math.abs(pct) < 0.05) return 'flat'
		const good = invert ? pct < 0 : pct > 0
		return good ? 'up' : 'down'
	}

	// KPI 对比行：环比、同比固定展示；选择自定义对比期时追加一行
	const DeltaGroup = (props) => {
		const rows = [
			{ label: '环比', previous: props.previous },
			{ label: '同比', previous: props.yoy }
		]
		if (props.custom !== undefined && props.custom !== null) {
			rows.push({ label: '对比期', previous: props.custom })
		}
		const children = []
		rows.forEach((row) => {
			children.push(h('span', { class: 'kpi-delta-label' }, row.label))
			children.push(
				h(
					'span',
					{ class: ['delta-value', deltaTone(props.current, row.previous, props.invert)] },
					deltaText(props.current, row.previous)
				)
			)
		})
		return h('div', { class: 'kpi-deltas' }, children)
	}
	DeltaGroup.props = {
		current: { type: [Number, String], default: 0 },
		previous: { type: [Number, String], default: 0 },
		yoy: { type: [Number, String], default: 0 },
		custom: { type: [Number, String], default: undefined },
		invert: { type: Boolean, default: false }
	}

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
				color: ['#4bb964', '#e5484d', '#f37021'],
				legend: { position: 'top' },
				smooth: true
			})
			trendChart.render()
		}

		const channelData = channelList.value.map((item) => ({ type: payChannelText(item.channel), value: money(item.paidAmount) }))
		if (channelData.length) {
			channelChart = new Pie('financeChannelChart', {
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
				color: '#6f5bd8',
				smooth: true,
				appendPadding: [8, 0, 0, 0]
			})
			attendChart.render()
		}
	}

	const prevSummary = ref({})
	const yoySummary = ref({})

	// 指标口径：公式、统计范围、数据更新时间统一在此维护，避免各页面说法不一致
	const metricTips = {
		paidAmount: [
			'公式：筛选范围内已支付订单的实收金额合计。',
			'范围：按支付时间（缺失时用创建时间）落在所选区间，受校区/课程/渠道筛选影响。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		refundAmount: [
			'公式：筛选范围内已退款成功的退费单金额合计。',
			'范围：按退款时间（缺失时依次取审批/更新时间/创建时间）落在所选区间。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		netAmount: [
			'公式：净营收 = 收款金额 − 退费金额。',
			'范围：与收款、退费同区间、同筛选条件。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		recognizedAmount: [
			'公式：课消课时 × 订单购买课时单价（权责口径），回补课时冲减。',
			'范围：按课时流水时间落在所选区间，与点名消课明细一致。',
			'数据更新：点名提交后即时生效；日汇总每日 00:30 回补前一天数据。'
		],
		orderCount: [
			'公式：筛选范围内状态为已支付/已退费的订单笔数。',
			'范围：按支付时间（缺失时用创建时间）落在所选区间。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		avgOrderAmount: [
			'公式：客单价 = 收款金额 ÷ 已支付订单数。',
			'范围：与收款金额同区间、同筛选条件；订单数为 0 时显示 0。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		perStudentRevenue: [
			'公式：生均收入 = 收款金额 ÷ 去重付费学员数。',
			'范围：分母为所选区间内实际有支付订单的学员，不是全部在读学员。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		refundRate: [
			'公式：退费率 = 退费金额 ÷ 收款金额 × 100%。',
			'范围：与收款、退费同区间、同筛选条件；收款为 0 时显示 0。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		],
		collectionRate: [
			'公式：实收率 = 收款金额 ÷（收款金额 + 待收款金额）× 100%。',
			'范围：待收款取所选区间内创建的未支付订单，受校区/课程筛选影响。',
			'数据更新：实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新。'
		]
	}

	// 切换对比方式：选择自定义但未选区间时，默认带出与当前区间等长的上一周期
	const onCompareModeChange = (mode) => {
		if (mode === 'CUSTOM' && (!customCompareRange.value || customCompareRange.value.length !== 2)) {
			const [start, end] = dateRange.value || []
			if (start && end) {
				const days = dayjs(end).diff(dayjs(start), 'day') + 1
				customCompareRange.value = [
					dayjs(start).subtract(days, 'day').format('YYYY-MM-DD'),
					dayjs(start).subtract(1, 'day').format('YYYY-MM-DD')
				]
			}
		}
		loadCompare()
	}

	// 对比期数据：环比/同比由后端统一计算，自定义对比期同时返回，失败不影响主报表
	const loadCompare = () => {
		const [start, end] = customCompareRange.value || []
		if (compareMode.value === 'CUSTOM' && (!start || !end)) {
			return Promise.resolve()
		}
		const params = {
			...buildQuery(),
			compareMode: compareMode.value,
			compareStartTime: compareMode.value === 'CUSTOM' ? `${start} 00:00:00` : undefined,
			compareEndTime: compareMode.value === 'CUSTOM' ? `${end} 23:59:59` : undefined
		}
		return reportApi
			.compare(params)
			.then((data) => {
				compareData.value = data || {}
				prevSummary.value = data?.previousSummary || {}
				yoySummary.value = data?.yoySummary || {}
				customSummary.value = data?.compareSummary || {}
				if (data?.generatedAt) {
					dataUpdatedAt.value = data.generatedAt
				}
			})
			.catch(() => {
				compareData.value = {}
				prevSummary.value = {}
				yoySummary.value = {}
				customSummary.value = {}
			})
	}

	// 报表元信息：数据更新时间与最近日汇总状态
	const loadMeta = () => {
		return reportApi
			.meta()
			.then((data) => {
				metaInfo.value = data || {}
				if (data?.generatedAt) {
					dataUpdatedAt.value = data.generatedAt
				}
			})
			.catch(() => {
				metaInfo.value = {}
			})
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
		Promise.all([loadSummary(), loadPayment(), loadRefund(), loadDaily(), loadCompare(), loadMeta()]).finally(() => {
			loading.value = false
		})
		loadReconcile()
	}

	const onExport = ({ key }) => {
		if (key === '__records') {
			openExportRecords()
			return
		}
		exportLoading.value = true
		// 导出沿用当前筛选条件（时间/校区/课程/渠道），后台生成后可继续操作页面
		reportApi
			.exportAsync({ ...buildQuery(), type: key })
			.then((task) => {
				exportTask.value = task
				pollExportTask(task.id)
			})
			.finally(() => {
				exportLoading.value = false
			})
	}

	const exportStatusText = (status) =>
		({ PENDING: '排队中', RUNNING: '生成中', SUCCESS: '已完成', FAILED: '失败' })[status] || status || '—'
	const exportStatusColor = (status) =>
		({ PENDING: 'default', RUNNING: 'processing', SUCCESS: 'success', FAILED: 'error' })[status] || 'default'
	const exportTypeText = (type) =>
		({ PAYMENT: '收款明细', REFUND: '退费明细', COURSE_SUMMARY: '课程汇总' })[type] || type || '导出文件'

	// 异步导出：轮询任务状态，完成后自动下载
	const pollExportTask = (taskId) => {
		const timer = setInterval(() => {
			reportApi.exportAsyncStatus({ id: taskId }).then((task) => {
				exportTask.value = task
				if (task.status === 'SUCCESS') {
					clearInterval(timer)
					reportApi.exportAsyncDownload({ id: taskId }).then((res) => {
						downloadUtil.resultDownload(res)
						message.success(`导出完成，共 ${task.rowCount || 0} 行，已开始下载`)
						loadExportRecords()
					})
				} else if (task.status === 'FAILED') {
					clearInterval(timer)
					message.error(task.errorMsg || '导出失败')
					loadExportRecords()
				}
			})
		}, 1500)
	}

	const loadExportRecords = () => {
		exportRecordsLoading.value = true
		reportApi
			.exportAsyncPage({ current: 1, size: 30 })
			.then((data) => {
				exportRecords.value = data?.records || []
			})
			.finally(() => {
				exportRecordsLoading.value = false
			})
	}

	const openExportRecords = () => {
		recordsOpen.value = true
		loadExportRecords()
	}

	const downloadExportRecord = (item) => {
		reportApi.exportAsyncDownload({ id: item.id }).then((res) => {
			downloadUtil.resultDownload(res)
		})
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
		compareMode.value = 'MOM'
		customCompareRange.value = undefined
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
		{ title: '渠道', dataIndex: 'payChannel', customRender: ({ text }) => payChannelText(text) },
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

	// 支持从其他页面带参下钻（tab=payment/refund、range=month）
	const applyDrillParams = () => {
		if (route.query.tab) {
			activeTab.value = route.query.tab
		}
		if (route.query.range === 'month') {
			dateRange.value = [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
		}
	}

	onMounted(() => {
		loadCourses()
		loadCampusOptions()
		applyDrillParams()
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
	.metric-help {
		margin-left: 4px;
		font-size: 12px;
		color: var(--text-color-tertiary);
		cursor: help;
	}
	.kpi-deltas {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 2px 8px;
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.kpi-delta-label {
		color: var(--text-color-tertiary);
	}
	.delta-value {
		font-weight: 500;
	}
	.delta-value.up {
		color: var(--success-text);
	}
	.delta-value.down {
		color: var(--danger-text);
	}
	.delta-value.flat {
		color: var(--text-color-secondary);
	}
	.data-updated {
		margin-top: 4px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.export-progress {
		margin-left: 12px;
	}
	.export-rec-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 0;
		border-bottom: 1px solid var(--border-color-split);
	}
	.export-rec-main {
		flex: 1;
		min-width: 0;
	}
	.export-rec-title {
		font-size: 14px;
		font-weight: 500;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.export-rec-sub {
		margin-top: 4px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.export-rec-err {
		margin-top: 4px;
		font-size: 12px;
		color: var(--danger-text);
	}
</style>
