<template>
	<div>
		<a-card :bordered="false" class="report-filter">
			<a-form layout="inline">
				<a-form-item label="时间">
					<a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" allow-clear />
				</a-form-item>
				<a-form-item label="校区">
					<a-select v-model:value="query.campusId" placeholder="全部校区" :options="campusOptions" allow-clear style="width: 150px" />
				</a-form-item>
				<a-form-item label="课程">
					<a-select v-model:value="query.courseId" placeholder="全部课程" :options="courseOptions" allow-clear style="width: 170px" />
				</a-form-item>
				<a-form-item label="班级">
					<a-select v-model:value="query.classId" placeholder="全部班级" :options="classOptions" allow-clear style="width: 170px" />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="loadAll">查询</a-button>
						<a-button @click="reset">重置</a-button>
						<a-dropdown v-if="hasPerm('reportAcademicExport')">
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
					<a-card :bordered="false"><a-statistic title="到课人次" :value="attendance.attendCount" suffix="人次" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="请假人次" :value="attendance.leaveCount" suffix="人次" :value-style="{ color: '#faad14' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="缺勤人次" :value="attendance.absentCount" suffix="人次" :value-style="{ color: '#fa541c' }" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="应到人次" :value="attendance.expectedCount" suffix="人次" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="到课率" :value="attendance.attendRate" :precision="2" suffix="%" /></a-card>
				</a-col>
				<a-col :xs="24" :sm="12" :md="8" :xl="4">
					<a-card :bordered="false"><a-statistic title="出勤率" :value="attendance.attendanceRate" :precision="2" suffix="%" /></a-card>
				</a-col>
			</a-row>

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
	import { onMounted, reactive, ref } from 'vue'
	import { useRouter } from 'vue-router'
	import dayjs from 'dayjs'
	import reportApi from '@/api/report/reportApi'
	import courseApi from '@/api/course/courseApi'
	import downloadUtil from '@/utils/downloadUtil'

	const router = useRouter()

	const loading = ref(false)
	const activeTab = ref('classFill')
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

	const loadAll = () => {
		loading.value = true
		const params = buildQuery()
		Promise.all([
			reportApi.academicAttendance(params),
			reportApi.academicClassFill(params),
			reportApi.academicTeacherWorkload(params),
			reportApi.academicLessonProgress(params),
			reportApi.academicRiskStudents({ ...params, type: riskType.value })
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
			})
	}

	const loadRisk = () => {
		reportApi.academicRiskStudents({ ...buildQuery(), type: riskType.value }).then((data) => {
			riskList.value = data || []
		})
	}

	const reset = () => {
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

	const onExport = ({ key }) => {
		reportApi.academicExport({ ...buildQuery(), type: key }).then((res) => {
			downloadUtil.resultDownload(res)
		})
	}

	// 下钻：班级 → 排课课表
	const goSchedule = (record) => {
		router.push({ path: '/course/schedule', query: { classId: record.classId } })
	}

	// 下钻：学员 → 学员详情
	const goStudent = (record) => {
		router.push({ path: '/student/detail', query: { id: record.studentId } })
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
</style>
