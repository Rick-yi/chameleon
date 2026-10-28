<template>
	<div>
		<page-header title="教务报表" desc="出勤、满班率、教师工作量与学员课时进度" />
		<a-card :bordered="false" class="report-filter">
			<a-form layout="inline">
				<a-form-item label="周期">
					<period-switch v-model="period" allow-custom @change="onPeriodChange" />
				</a-form-item>
				<a-form-item label="时间">
					<a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" allow-clear @change="onDateRangeChange" />
				</a-form-item>
				<a-form-item label="校区">
					<a-select
						v-model:value="query.campusId"
						placeholder="全部校区"
						:options="campusOptions"
						allow-clear
						style="width: 150px"
					/>
				</a-form-item>
				<a-form-item label="课程">
					<a-select
						v-model:value="query.courseId"
						placeholder="全部课程"
						:options="courseOptions"
						allow-clear
						style="width: 170px"
					/>
				</a-form-item>
				<a-form-item label="班级">
					<a-select
						v-model:value="query.classId"
						placeholder="全部班级"
						:options="classOptions"
						allow-clear
						style="width: 170px"
					/>
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="loadAll">查询</a-button>
						<a-button @click="reset">重置</a-button>
						<a-dropdown v-if="hasPerm('/course/report/academic/export')">
							<a-button>
								导出
								<down-outlined />
							</a-button>
							<template #overlay>
								<a-menu @click="onExport">
									<a-menu-item key="ATTENDANCE">出勤汇总</a-menu-item>
									<a-menu-item key="CLASS_FILL">班级满班率</a-menu-item>
									<a-menu-item key="TEACHER">教师课时</a-menu-item>
									<a-menu-item key="LESSON_PROGRESS">学员课消进度</a-menu-item>
									<a-menu-item key="RISK_STUDENT">风险学员</a-menu-item>
									<a-menu-divider />
									<a-menu-item key="__records">导出记录</a-menu-item>
								</a-menu>
							</template>
						</a-dropdown>
					</a-space>
				</a-form-item>
			</a-form>
			<div class="data-updated">
				数据更新时间：{{ refreshedAt || '—' }}（点名提交后即时生效；导出记录可查历史任务）
				<a-tooltip>
					<template #title>
						<div>到课率分母为已点名人次，出勤率分母为应到人次，两者口径不同。</div>
						<div>未点名 = 应到 − 已点名；未点名大于 0 时出勤率会低于到课率。</div>
					</template>
					<question-circle-outlined class="metric-help" />
				</a-tooltip>
			</div>
		</a-card>

		<export-records-drawer v-model:open="recordsOpen" export-type="COURSE_ACADEMIC" />

		<a-spin :spinning="loading">
			<a-row :gutter="16" style="margin-top: 16px">
				<a-col :xs="12" :sm="8" :md="8" :xl="6">
					<a-card :bordered="false" class="kpi-link" @click="router.push('/course/attend')"
						><a-statistic title="到课人次" :value="attendance.attendCount" suffix="人次" /><kpi-delta
							:current="attendance.attendCount"
							:previous="prevAttendance.attendCount"
					/></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"
						><a-statistic
							title="请假人次"
							:value="attendance.leaveCount"
							suffix="人次"
							:value-style="{ color: '#faad14' }" /><kpi-delta
							:current="attendance.leaveCount"
							:previous="prevAttendance.leaveCount"
							invert
					/></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"
						><a-statistic
							title="缺勤人次"
							:value="attendance.absentCount"
							suffix="人次"
							:value-style="{ color: '#fa541c' }" /><kpi-delta
							:current="attendance.absentCount"
							:previous="prevAttendance.absentCount"
							invert
					/></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false" class="kpi-link" @click="router.push('/course/class')"
						><a-statistic title="应到人次" :value="attendance.expectedCount" suffix="人次" /><kpi-delta
							:current="attendance.expectedCount"
							:previous="prevAttendance.expectedCount"
					/></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false">
						<a-statistic title="到课率" :value="attendance.attendRate" :precision="2" suffix="%">
							<template #title>
								到课率
								<a-tooltip>
									<template #title>
										<div>公式：到课率 = 到课人次 ÷ 已点名人次 × 100%。</div>
										<div>已点名 = 到课 + 请假 + 缺勤，只统计筛选范围内已提交点名的记录。</div>
										<div>数据更新：点名提交后即时生效；页面顶部显示本次加载时间。</div>
									</template>
									<question-circle-outlined class="metric-help" />
								</a-tooltip>
							</template>
						</a-statistic>
						<kpi-delta :current="attendance.attendRate" :previous="prevAttendance.attendRate" />
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false">
						<a-statistic title="出勤率" :value="attendance.attendanceRate" :precision="2" suffix="%">
							<template #title>
								出勤率
								<a-tooltip>
									<template #title>
										<div>公式：出勤率 = 到课人次 ÷ 应到人次 × 100%。</div>
										<div>应到人次 = 筛选范围内排课次数 × 班级在学学员数，含尚未点名的课节。</div>
										<div>数据更新：点名提交后即时生效；页面顶部显示本次加载时间。</div>
									</template>
									<question-circle-outlined class="metric-help" />
								</a-tooltip>
							</template>
						</a-statistic>
					</a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false">
						<a-statistic
							title="未点名人次"
							:value="unmarkedCount"
							suffix="人次"
							:value-style="{ color: unmarkedCount > 0 ? '#faad14' : undefined }"
						>
							<template #title>
								未点名人次
								<a-tooltip>
									<template #title>
										<div>公式：未点名人次 = 应到人次 − 已点名人次。</div>
										<div>已点名 = 到课 + 请假 + 缺勤；大于 0 表示仍有课节/学员未提交点名。</div>
										<div>到课率分母是已点名，出勤率分母是应到，因此两个百分比不同属于正常口径差异。</div>
										<div>数据更新：点名提交后即时生效。</div>
									</template>
									<question-circle-outlined class="metric-help" />
								</a-tooltip>
							</template>
						</a-statistic>
					</a-card>
				</a-col>
			</a-row>

			<a-alert
				v-if="attendanceMismatch"
				type="warning"
				show-icon
				style="margin-top: 12px"
				message="点名明细与应到人次不一致"
				:description="`已点名 ${markedCount} 人次，大于应到 ${attendance.expectedCount} 人次，请检查班级在学人数或补点/撤销记录。`"
			/>

			<a-card :bordered="false" style="margin-top: 16px">
				<a-tabs v-model:activeKey="activeTab">
					<a-tab-pane key="classFill" tab="班级满班率">
						<a-table
							:columns="classFillColumns"
							:data-source="classFillList"
							:row-key="(r) => r.classId"
							:pagination="{ pageSize: 10, showSizeChanger: true }"
							bordered
							size="middle"
						>
							<template #bodyCell="{ column, record }">
								<template v-if="column.dataIndex === 'className'">
									<a @click="goSchedule(record)">{{ record.className }}</a>
								</template>
							</template>
						</a-table>
					</a-tab-pane>
					<a-tab-pane key="teacher" tab="教师课时">
						<a-table
							:columns="teacherColumns"
							:data-source="teacherList"
							:row-key="(r) => r.teacherId"
							:pagination="{ pageSize: 10, showSizeChanger: true }"
							bordered
							size="middle"
						/>
					</a-tab-pane>
					<a-tab-pane key="progress" tab="学员课消进度">
						<a-table
							:columns="progressColumns"
							:data-source="progressList"
							:row-key="(r) => `${r.studentId}-${r.courseId}`"
							:pagination="{ pageSize: 10, showSizeChanger: true }"
							bordered
							size="middle"
						>
							<template #bodyCell="{ column, record }">
								<template v-if="column.dataIndex === 'studentName'">
									<a @click="goStudent(record)">{{ record.studentName }}</a>
								</template>
							</template>
						</a-table>
					</a-tab-pane>
					<a-tab-pane key="risk" tab="风险学员">
						<a-space style="margin-bottom: 12px">
							<a-select v-model:value="riskType" :options="riskTypeOptions" style="width: 160px" @change="loadRisk" />
							<a-button @click="loadRisk">刷新</a-button>
						</a-space>
						<a-table
							:columns="riskColumns"
							:data-source="riskList"
							:row-key="(r) => `${r.studentId}-${r.courseId}-${r.riskType}`"
							:pagination="{ pageSize: 10, showSizeChanger: true }"
							bordered
							size="middle"
						>
							<template #bodyCell="{ column, record }">
								<template v-if="column.dataIndex === 'studentName'">
									<a @click="goStudent(record)">{{ record.studentName }}</a>
								</template>
							</template>
						</a-table>
					</a-tab-pane>
				</a-tabs>
			</a-card>
		</a-spin>
	</div>
</template>

<script setup name="reportAcademic">
	import { computed, onMounted, reactive, ref } from 'vue'
	import { useRouter } from 'vue-router'
	import dayjs from 'dayjs'
	import reportApi from '@/api/report/reportApi'
	import courseApi from '@/api/course/courseApi'
	import { runAsyncExport } from '@/utils/asyncExport'
	import ExportRecordsDrawer from '@/components/ExportRecordsDrawer/index.vue'
	import KpiDelta from '@/components/KpiDelta/index.vue'
	import { previousRange, rangeToQuery } from '@/utils/reportCompare'
	import { periodDateRange } from '@/utils/reportPeriod'
	import PeriodSwitch from '@/components/PeriodSwitch/index.vue'

	const router = useRouter()

	const loading = ref(false)
	const refreshedAt = ref('')
	const activeTab = ref('classFill')
	const period = ref('MONTH')
	const dateRange = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
	const query = reactive({ campusId: undefined, courseId: undefined, classId: undefined })
	const riskType = ref('LOW_LESSON')
	const campusOptions = ref([])
	const courseOptions = ref([])
	const classOptions = ref([])

	const attendance = ref({
		attendCount: 0,
		leaveCount: 0,
		absentCount: 0,
		expectedCount: 0,
		attendRate: 0,
		attendanceRate: 0
	})
	const prevAttendance = ref({ attendCount: 0, leaveCount: 0, absentCount: 0, expectedCount: 0, attendRate: 0 })
	// 已点名 = 到课 + 请假 + 缺勤；未点名 = 应到 - 已点名（小于 0 说明数据异常，单独提示）
	const markedCount = computed(
		() =>
			Number(attendance.value.attendCount || 0) +
			Number(attendance.value.leaveCount || 0) +
			Number(attendance.value.absentCount || 0)
	)
	const unmarkedCount = computed(() => Math.max(0, Number(attendance.value.expectedCount || 0) - markedCount.value))
	const attendanceMismatch = computed(() => markedCount.value > Number(attendance.value.expectedCount || 0))
	const classFillList = ref([])
	const teacherList = ref([])
	const progressList = ref([])
	const riskList = ref([])

	const riskTypeOptions = [
		{ value: 'LOW_LESSON', label: '低剩余课时' },
		{ value: 'NO_ATTEND', label: '长期未消课' },
		{ value: 'STOPPED', label: '已停课' }
	]

	const buildQuery = () => {
		const [start, end] = dateRange.value || []
		return {
			startTime: start ? `${start} 00:00:00` : undefined,
			endTime: end ? `${end} 23:59:59` : undefined,
			campusId: query.campusId,
			courseId: query.courseId,
			classId: query.classId
		}
	}
	const onPeriodChange = (value) => {
		dateRange.value = periodDateRange(value)
		loadAll()
	}
	const onDateRangeChange = () => {
		period.value = 'CUSTOM'
	}

	// 上期出勤：只用于 KPI 环比
	const loadPrevAttendance = () => {
		const range = previousRange(dateRange.value?.[0], dateRange.value?.[1])
		if (!range) {
			prevAttendance.value = {}
			return Promise.resolve()
		}
		return reportApi
			.academicAttendance({ ...buildQuery(), ...rangeToQuery(range) })
			.then((data) => {
				prevAttendance.value = data || {}
			})
			.catch(() => {
				prevAttendance.value = {}
			})
	}

	const loadAll = () => {
		loading.value = true
		const params = buildQuery()
		Promise.all([
			reportApi.academicAttendance(params),
			reportApi.academicClassFill(params),
			reportApi.academicTeacherWorkload(params),
			reportApi.academicLessonProgress(params),
			reportApi.academicRiskStudents({ ...params, type: riskType.value }),
			loadPrevAttendance()
		])
			.then(([attendanceData, classFillData, teacherData, progressData, riskData]) => {
				attendance.value = attendanceData || attendance.value
				classFillList.value = classFillData || []
				teacherList.value = teacherData || []
				progressList.value = progressData || []
				riskList.value = riskData || []
			})
			.finally(() => {
				loading.value = false
				refreshedAt.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
			})
	}

	const loadRisk = () => {
		reportApi.academicRiskStudents({ ...buildQuery(), type: riskType.value }).then((data) => {
			riskList.value = data || []
		})
	}

	const reset = () => {
		period.value = 'MONTH'
		dateRange.value = [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
		query.campusId = undefined
		query.courseId = undefined
		query.classId = undefined
		riskType.value = 'LOW_LESSON'
		loadAll()
	}

	const loadOptions = () => {
		reportApi.campusOptions().then((data) => {
			campusOptions.value = (data || []).map((item) => ({ value: item.value, label: item.label }))
		})
		courseApi.coursePage({ current: 1, size: 100 }).then((data) => {
			courseOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
		courseApi.classPage({ current: 1, size: 100 }).then((data) => {
			classOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}

	const classFillColumns = [
		{ title: '班级', dataIndex: 'className' },
		{ title: '课程', dataIndex: 'courseName' },
		{ title: '校区', dataIndex: 'campusName' },
		{ title: '教师', dataIndex: 'teacherName' },
		{ title: '容量', dataIndex: 'capacity' },
		{ title: '在学人数', dataIndex: 'studyingCount' },
		{ title: '满班率', dataIndex: 'fillRate', customRender: ({ text }) => `${Number(text || 0).toFixed(2)}%` }
	]
	const teacherColumns = [
		{ title: '教师', dataIndex: 'teacherName' },
		{ title: '排课次数', dataIndex: 'scheduleCount' },
		{ title: '计划课时', dataIndex: 'plannedLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '实际课消课时', dataIndex: 'actualLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '到课人次', dataIndex: 'attendCount' }
	]
	const progressColumns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '课程', dataIndex: 'courseName' },
		{ title: '总课时', dataIndex: 'totalLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '已消课时', dataIndex: 'usedLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '剩余课时', dataIndex: 'remainLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '课消进度', dataIndex: 'progressRate', customRender: ({ text }) => `${Number(text || 0).toFixed(2)}%` },
		{ title: '到期时间', dataIndex: 'expireTime' },
		{ title: '状态', dataIndex: 'status' }
	]
	const riskColumns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '风险类型', dataIndex: 'riskType', customRender: ({ text }) => riskTypeName(text) },
		{ title: '课程', dataIndex: 'courseName' },
		{ title: '剩余课时', dataIndex: 'remainLessons', customRender: ({ text }) => Number(text || 0).toFixed(2) },
		{ title: '到期时间', dataIndex: 'expireTime' },
		{ title: '最近消课', dataIndex: 'lastAttendTime' },
		{ title: '未消课天数', dataIndex: 'daysWithoutAttend' },
		{ title: '说明', dataIndex: 'riskDesc' }
	]

	const riskTypeName = (type) => {
		const item = riskTypeOptions.find((option) => option.value === type)
		return item ? item.label : type
	}

	const recordsOpen = ref(false)
	const onExport = ({ key }) => {
		if (key === '__records') {
			recordsOpen.value = true
			return
		}
		// 大报表走后台生成，避免同步导出超时
		runAsyncExport('COURSE_ACADEMIC', { ...buildQuery(), reportType: key })
	}

	// 下钻：班级 → 排课课表
	const goSchedule = (record) => {
		router.push({ path: '/course/schedule', query: { classId: record.classId } })
	}

	// 下钻：学员 → 学员详情
	const goStudent = (record) => {
		router.push({ path: '/student', query: { id: record.studentId } })
	}

	onMounted(() => {
		loadOptions()
		loadAll()
	})
</script>

<style scoped>
	.report-filter {
		margin-bottom: 8px;
	}
	.metric-help {
		margin-left: 4px;
		font-size: 12px;
		color: var(--text-color-tertiary);
		cursor: help;
	}
	.data-updated {
		margin-top: 4px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
</style>
