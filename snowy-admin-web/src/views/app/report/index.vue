<template>
	<div>
		<!-- 视角切换 -->
		<div class="m-view-tabs">
			<div
				v-for="item in viewTabs"
				:key="item.value"
				class="m-view-tab"
				:class="{ active: view === item.value }"
				@click="onView(item.value)"
			>
				{{ item.text }}
			</div>
		</div>

		<!-- 时间筛选 -->
		<div class="m-filter-row">
			<a-radio-group v-model:value="range" size="small" button-style="solid" @change="load">
				<a-radio-button value="7">近 7 天</a-radio-button>
				<a-radio-button value="30">近 30 天</a-radio-button>
				<a-radio-button value="90">近 90 天</a-radio-button>
			</a-radio-group>
		</div>

		<a-spin :spinning="loading">
			<!-- 经营总览 -->
			<template v-if="view === 'all'">
				<div class="m-stat-grid">
					<div class="m-stat">
						<div class="m-stat-label">收款</div>
						<div class="m-stat-value">¥{{ money(summary.paidAmount) }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">退费</div>
						<div class="m-stat-value" style="color: var(--danger-text)">¥{{ money(summary.refundAmount) }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">净营收</div>
						<div class="m-stat-value" style="color: var(--success-text)">¥{{ money(summary.netAmount) }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">课消课时</div>
						<div class="m-stat-value">{{ money(summary.attendLessons) }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">在读学员</div>
						<div class="m-stat-value">{{ summary.studyingStudentCount || 0 }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">续费预警</div>
						<div class="m-stat-value" style="color: var(--warning-text)">{{ summary.renewWarnCount || 0 }}</div>
					</div>
				</div>

				<div class="m-section-title">收款渠道</div>
				<div class="m-card">
					<div v-for="item in channelList" :key="item.channel" class="m-bar-row">
						<div class="m-bar-label">{{ channelText(item.channel) }}</div>
						<div class="m-bar-value">¥{{ money(item.paidAmount) }}</div>
						<div class="m-bar-track">
							<div class="m-bar-fill" :style="{ width: barWidth(item.ratio) }"></div>
						</div>
					</div>
					<div v-if="!channelList.length" class="m-empty-inline">暂无数据</div>
				</div>

				<div class="m-section-title">校区排行</div>
				<div class="m-card">
					<div v-for="(item, index) in campusList" :key="item.campusId" class="m-rank-row">
						<span class="m-rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
						<span class="m-rank-name">{{ item.campusName }}</span>
						<span class="m-rank-value">¥{{ money(item.paidAmount) }}</span>
					</div>
					<div v-if="!campusList.length" class="m-empty-inline">暂无数据</div>
				</div>
			</template>

			<!-- 教学质量 -->
			<template v-else-if="view === 'teach'">
				<div class="m-stat-grid">
					<div class="m-stat">
						<div class="m-stat-label">到课人次</div>
						<div class="m-stat-value">{{ attendance.attendCount || 0 }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">请假人次</div>
						<div class="m-stat-value">{{ attendance.leaveCount || 0 }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">缺勤人次</div>
						<div class="m-stat-value" style="color: var(--danger-text)">{{ attendance.absentCount || 0 }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">到课率</div>
						<div class="m-stat-value" style="color: var(--success-text)">{{ attendance.attendRate || 0 }}%</div>
					</div>
				</div>

				<div class="m-section-title">满班率排行</div>
				<div class="m-card">
					<div v-for="(item, index) in classFillList" :key="item.classId" class="m-rank-row">
						<span class="m-rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
						<span class="m-rank-name">{{ item.className }}</span>
						<span class="m-rank-value">{{ item.fillRate || 0 }}%</span>
					</div>
					<div v-if="!classFillList.length" class="m-empty-inline">暂无数据</div>
				</div>

				<div class="m-section-title">教师课时负荷</div>
				<div class="m-card">
					<div v-for="(item, index) in teacherList" :key="item.teacherId" class="m-rank-row">
						<span class="m-rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
						<span class="m-rank-name">{{ item.teacherName }}</span>
						<span class="m-rank-value">{{ item.totalLessons || 0 }} 课时</span>
					</div>
					<div v-if="!teacherList.length" class="m-empty-inline">暂无数据</div>
				</div>
			</template>

			<!-- 招生转化 -->
			<template v-else-if="view === 'enroll'">
				<div class="m-section-title">招生漏斗</div>
				<div class="m-card">
					<div v-for="(item, index) in funnelList" :key="item.stage" class="m-funnel-row">
						<div class="m-funnel-name">{{ item.stageName || item.stage }}</div>
						<div class="m-funnel-count">{{ item.count || 0 }}</div>
						<div class="m-funnel-rate">
							{{ index === 0 ? '起点' : `转化 ${item.conversionRate || 0}%` }}
						</div>
					</div>
					<div v-if="!funnelList.length" class="m-empty-inline">暂无数据</div>
				</div>

				<div class="m-section-title">来源渠道</div>
				<div class="m-card">
					<div v-for="item in sourceList" :key="item.source" class="m-rank-row">
						<span class="m-rank-name">{{ item.source || '未知' }}</span>
						<span class="m-rank-value">{{ item.count || 0 }} 条</span>
					</div>
					<div v-if="!sourceList.length" class="m-empty-inline">暂无数据</div>
				</div>
			</template>

			<!-- 财务收款 -->
			<template v-else>
				<div class="m-stat-grid">
					<div class="m-stat">
						<div class="m-stat-label">收款笔数</div>
						<div class="m-stat-value">{{ summary.orderCount || 0 }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">客单价</div>
						<div class="m-stat-value">¥{{ money(summary.avgOrderAmount) }}</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">收款率</div>
						<div class="m-stat-value" style="color: var(--success-text)">{{ summary.collectionRate || 0 }}%</div>
					</div>
					<div class="m-stat">
						<div class="m-stat-label">退费率</div>
						<div class="m-stat-value" style="color: var(--danger-text)">{{ summary.refundRate || 0 }}%</div>
					</div>
				</div>

				<div class="m-section-title">课包收款排行</div>
				<div class="m-card">
					<div v-for="(item, index) in courseList" :key="item.courseId" class="m-rank-row">
						<span class="m-rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
						<span class="m-rank-name">{{ item.courseName }}</span>
						<span class="m-rank-value">¥{{ money(item.paidAmount) }}</span>
					</div>
					<div v-if="!courseList.length" class="m-empty-inline">暂无数据</div>
				</div>
			</template>
		</a-spin>
	</div>
</template>

<script setup name="appReport">
	import reportApi from '@/api/report/reportApi'
	import { useAppStore } from '@/store/app'

	const appStore = useAppStore()
	const loading = ref(false)
	const view = ref('all')
	const range = ref('30')

	const viewTabs = [
		{ value: 'all', text: '经营' },
		{ value: 'teach', text: '教学' },
		{ value: 'enroll', text: '招生' },
		{ value: 'finance', text: '财务' }
	]

	const summary = ref({})
	const channelList = ref([])
	const campusList = ref([])
	const attendance = ref({})
	const classFillList = ref([])
	const teacherList = ref([])
	const funnelList = ref([])
	const sourceList = ref([])
	const courseList = ref([])

	const money = (v) => Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
	const barWidth = (ratio) => `${Math.min(Math.round(Number(ratio || 0)), 100)}%`
	const channelText = (c) => ({ WECHAT: '微信', CASH: '现金', OFFLINE: '线下转账', ALIPAY: '支付宝' }[c] || c || '-')

	const dateRange = () => {
		const end = new Date()
		const start = new Date(end.getTime() - Number(range.value) * 24 * 3600 * 1000)
		const pad = (n) => String(n).padStart(2, '0')
		const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
		return { startTime: `${fmt(start)} 00:00:00`, endTime: `${fmt(end)} 23:59:59` }
	}

	const load = async () => {
		loading.value = true
		try {
			const params = dateRange()
			if (view.value === 'all' || view.value === 'finance') {
				const [summaryRes, channelRes, campusRes, courseRes] = await Promise.all([
					reportApi.summary(params).catch(() => ({})),
					reportApi.channel(params).catch(() => []),
					reportApi.campusRank(params).catch(() => []),
					reportApi.courseRank(params).catch(() => [])
				])
				summary.value = summaryRes || {}
				channelList.value = channelRes || []
				campusList.value = campusRes || []
				courseList.value = courseRes || []
			}
			if (view.value === 'teach') {
				const [attendanceRes, classFillRes, teacherRes] = await Promise.all([
					reportApi.academicAttendance(params).catch(() => ({})),
					reportApi.academicClassFill(params).catch(() => []),
					reportApi.academicTeacherWorkload(params).catch(() => [])
				])
				attendance.value = attendanceRes || {}
				classFillList.value = classFillRes || []
				teacherList.value = teacherRes || []
			}
			if (view.value === 'enroll') {
				const [funnelRes, sourceRes] = await Promise.all([
					reportApi.crmFunnel(params).catch(() => []),
					reportApi.crmSource(params).catch(() => [])
				])
				funnelList.value = funnelRes || []
				sourceList.value = sourceRes || []
			}
		} finally {
			loading.value = false
		}
	}

	const onView = (value) => {
		view.value = value
		load()
	}

	onMounted(load)
</script>

<style lang="less" scoped>
	.m-view-tabs {
		display: flex;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 4px;
		gap: 4px;
		margin-bottom: 12px;
	}
	.m-view-tab {
		flex: 1;
		text-align: center;
		padding: 8px 0;
		font-size: 14px;
		color: var(--text-color-secondary);
		border-radius: var(--radius-control);
		&.active {
			background: var(--primary-1);
			color: var(--primary-6);
			font-weight: 600;
		}
	}
	.m-filter-row {
		margin-bottom: 12px;
	}
	.m-stat-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 10px;
	}
	.m-stat {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
	}
	.m-stat-label {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-stat-value {
		margin-top: 4px;
		font-size: 20px;
		font-weight: 600;
		color: var(--text-color);
		font-variant-numeric: tabular-nums;
	}
	.m-section-title {
		margin: 16px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
	}
	.m-bar-row {
		margin-bottom: 10px;
		&:last-child {
			margin-bottom: 0;
		}
	}
	.m-bar-label {
		font-size: 13px;
		color: var(--text-color);
	}
	.m-bar-value {
		float: right;
		font-size: 13px;
		color: var(--text-color-secondary);
		font-variant-numeric: tabular-nums;
	}
	.m-bar-track {
		margin-top: 5px;
		height: 6px;
		border-radius: 3px;
		background: var(--background-color-base);
		overflow: hidden;
	}
	.m-bar-fill {
		height: 100%;
		background: var(--primary-6);
	}
	.m-rank-row {
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 8px 0;
		border-bottom: 1px solid var(--border-color-split);
		&:last-child {
			border-bottom: none;
		}
	}
	.m-rank-no {
		width: 20px;
		height: 20px;
		line-height: 20px;
		text-align: center;
		border-radius: 50%;
		background: var(--background-color-base);
		color: var(--text-color-secondary);
		font-size: 12px;
		&.top {
			background: var(--primary-1);
			color: var(--primary-6);
			font-weight: 600;
		}
	}
	.m-rank-name {
		flex: 1;
		font-size: 14px;
		color: var(--text-color);
	}
	.m-rank-value {
		font-size: 13px;
		color: var(--text-color-secondary);
		font-variant-numeric: tabular-nums;
	}
	.m-funnel-row {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 7px 0;
		border-bottom: 1px solid var(--border-color-split);
		&:last-child {
			border-bottom: none;
		}
	}
	.m-funnel-name {
		flex: 1;
		font-size: 14px;
		color: var(--text-color);
	}
	.m-funnel-count {
		font-size: 15px;
		font-weight: 600;
		color: var(--primary-6);
		font-variant-numeric: tabular-nums;
	}
	.m-funnel-rate {
		width: 78px;
		text-align: right;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-empty-inline {
		padding: 14px 0;
		text-align: center;
		font-size: 13px;
		color: var(--text-color-tertiary);
	}
</style>
