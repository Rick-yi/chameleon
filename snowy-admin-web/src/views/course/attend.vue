<template>
	<div>
		<page-header title="点名消课" desc="选择课节即可点名，按课包规则自动扣减课时" />
		<!-- 今日/所选日期的课节：点一下直接进入点名 -->
		<a-card :bordered="false" title="课节列表" style="margin-bottom: 12px">
			<div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 12px">
				<a-date-picker v-model:value="date" value-format="YYYY-MM-DD" style="width: 160px" @change="loadSchedules" />
				<a-select v-model:value="classId" placeholder="全部班级" :options="classOptions" style="width: 220px" allow-clear @change="loadSchedules" />
				<a-button type="primary" @click="loadSchedules">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
				<span style="color: rgba(0,0,0,.45); font-size: 12px">点击课节即可点名消课</span>
			</div>
			<div v-if="!scheduleList.length" class="ent-empty">该日期没有排课</div>
			<a-row v-else :gutter="12">
				<a-col v-for="item in scheduleList" :key="item.id" :xs="24" :sm="12" :xl="8">
					<div class="attend-slot" :class="{ done: item.attendStatus === 'DONE' }" @click="selectSchedule(item)">
						<div class="attend-slot-title">{{ item.className || '未命名班级' }}</div>
						<div class="attend-slot-sub">
							{{ item.startTime }} - {{ item.endTime }} · {{ item.classroom || '未定教室' }}
							{{ item.teacherName ? ' · ' + item.teacherName : '' }}
						</div>
						<div class="attend-slot-tag">
							<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
							<span v-if="item.studentCount" style="font-size: 12px; color: rgba(0,0,0,.45)">应到 {{ item.studentCount }} 人</span>
						</div>
					</div>
				</a-col>
			</a-row>
		</a-card>

		<a-card :bordered="false">
		<div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap; border-bottom: 1px solid #f0f0f0; padding-bottom: 12px">
			<a-select v-model:value="scheduleId" placeholder="选择上课节次" :options="scheduleOptions" style="width: 320px" @change="loadAttendPage" />
			<div style="margin-left: auto">
				<span v-if="scheduleId" style="color: rgba(0,0,0,.45); font-size: 12px">点击状态按钮标记出勤，到课/缺勤将按课程规则扣课时</span>
			</div>
		</div>
		<div style="padding: 16px 0">
			<div v-if="!scheduleId" class="ent-empty">请先选择上面的上课节次</div>
			<template v-else>
				<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
					<span style="font-weight: 500">应到 {{ students.length }} 人</span>
					<a-button type="primary" @click="onConfirm">确认点名并扣课</a-button>
				</div>
				<a-table
					:columns="columns"
					:data-source="students"
					:row-key="(record) => record.studentId"
					:paginating="false"
					:pagination="false"
					size="middle"
					bordered
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'studentId'">
							{{ studentName(record.studentId) }}
						</template>
						<template v-if="column.dataIndex === 'remain'">
							<a-tag v-if="!balanceMap[record.studentId]" color="red">未购课</a-tag>
							<a-tag v-else-if="balanceMap[record.studentId].remainLessons < 1" color="orange">
								剩 {{ balanceMap[record.studentId].remainLessons }}
							</a-tag>
							<span v-else>剩 {{ balanceMap[record.studentId].remainLessons }}</span>
						</template>
						<template v-if="column.dataIndex === 'status'">
							<a-radio-group v-model:value="record.attendStatus" :disabled="!!record.attendId" button-style="solid">
								<a-radio-button value="ATTEND">到课</a-radio-button>
								<a-radio-button value="LEAVE">请假</a-radio-button>
								<a-radio-button value="ABSENT">缺勤</a-radio-button>
							</a-radio-group>
						</template>
						<template v-if="column.dataIndex === 'action'">
							<a-popconfirm v-if="record.attendId" title="撤销点名并回补课时？" @confirm="onCancelAttend(record)">
								<a-button type="link" danger size="small">撤销</a-button>
							</a-popconfirm>
							<span v-else>-</span>
						</template>
					</template>
				</a-table>
			</template>
		</div>
		</a-card>
	</div>
</template>

<script setup name="courseAttend">
	import { useRoute } from 'vue-router'
	import { message } from 'ant-design-vue'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const route = useRoute()
	const classId = ref()
	const date = ref(new Date().toISOString().slice(0, 10))
	const scheduleId = ref()
	const classOptions = ref([])
	const classRecords = ref([])
	const scheduleOptions = ref([])
	const scheduleList = ref([])
	const students = ref([])
	const studentMap = ref({})
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')
	const selectSchedule = (item) => {
		scheduleId.value = item.id
		classId.value = item.classId
		loadAttendPage()
	}
	const columns = [
		{ title: '学员', dataIndex: 'studentId' },
		{ title: '剩余课时', dataIndex: 'remain', width: 100 },
		{ title: '点名状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 100 }
	]
	const loadClasses = () => {
		courseApi.classPage({ current: 1, size: 200 }).then((data) => {
			classRecords.value = data.records || []
			classOptions.value = classRecords.value.map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const loadSchedules = () => {
		const dayStart = `${date.value} 00:00:00`
		const dayEnd = `${date.value} 23:59:59`
		courseApi
			.scheduleWeek({ classId: classId.value, startDate: dayStart, endDate: dayEnd })
			.then((data) => {
				scheduleList.value = data || []
				scheduleOptions.value = scheduleList.value.map((item) => ({
					value: item.id,
					label: `${item.className || '未命名班级'} ${item.startTime}-${item.endTime} ${item.classroom || ''}`
				}))
				// 支持从工作台/课表直达：自动选中目标课节
				const targetId = route.query.scheduleId
				if (targetId) {
					const target = scheduleList.value.find((item) => item.id === targetId)
					if (target) {
						selectSchedule(target)
					}
				}
			})
	}
	const loadStudents = () => {
		studentApi.studentPage({ current: 1, size: 200 }).then((data) => {
			const map = {}
			for (const s of data.records || []) map[s.id] = s
			studentMap.value = map
		})
	}
	const studentName = (id) => {
		const s = studentMap.value[id]
		return s ? `${s.name}(${s.parentPhone})` : id
	}
	const balanceMap = ref({})
	// 加载学员在该课程下的剩余课时（没有余额 = 还没买这门课）
	const loadBalance = (studentIdList, courseId) => {
		if (!studentIdList.length || !courseId) {
			balanceMap.value = {}
			return Promise.resolve()
		}
		return studentApi
			.lessonBalance({ studentIds: studentIdList.join(','), courseId })
			.then((list) => {
				const map = {}
				for (const item of list || []) map[item.studentId] = item
				balanceMap.value = map
			})
			.catch(() => (balanceMap.value = {}))
	}
	const loadAttendPage = () => {
		if (!scheduleId.value) return
		Promise.all([
			courseApi.classStudentPage({ current: 1, size: 200, classId: classId.value }),
			courseApi.attendPage({ current: 1, size: 200, scheduleId: scheduleId.value })
		]).then(([classStudentData, attendData]) => {
			const attendMap = {}
			for (const a of attendData.records || []) attendMap[a.studentId] = a
			const list = (classStudentData.records || []).filter((cs) => cs.status === 'STUDYING').map((cs) => ({
				studentId: cs.studentId,
				attendStatus: attendMap[cs.studentId] ? attendMap[cs.studentId].attendStatus : 'ATTEND',
				attendId: attendMap[cs.studentId] ? attendMap[cs.studentId].id : undefined
			}))
			students.value = list
			const cls = classRecords.value.find((item) => item.id === classId.value)
			return loadBalance(list.map((item) => item.studentId), cls?.courseId)
		})
	}
	const onConfirm = () => {
		const attendList = students.value
			.filter((s) => !s.attendId)
			.map((s) => ({ studentId: s.studentId, attendStatus: s.attendStatus }))
		if (attendList.length === 0) {
			message.info('本节没有需要点名的学员')
			return
		}
		courseApi
			.attendConfirm({ scheduleId: scheduleId.value, attendList })
			.then((res) => {
				const result = res?.data
				const skipped = result?.skippedList || []
				if (skipped.length) {
					// 课时不足的学员不阻断整班点名，单独提示
					message.warning(
						`点名完成，${result.deductCount} 人已消课；${skipped.length} 人未消课：` +
							skipped.map((item) => `${item.studentName}（${item.reason}）`).join('；')
					)
				} else {
					message.success(`点名完成，${result?.deductCount ?? attendList.length} 人已消课`)
				}
				loadAttendPage()
			})
	}
	const onCancelAttend = (record) => {
		courseApi.attendCancel({ id: record.attendId }).then(() => loadAttendPage())
	}
	loadClasses()
	loadStudents()
	loadSchedules()
</script>

<style lang="less" scoped>
	.attend-slot {
		border: 1px solid #f0f0f0;
		border-radius: 8px;
		padding: 10px 12px;
		margin-bottom: 12px;
		cursor: pointer;
		transition: all 0.2s;
		&:hover {
			border-color: #1677ff;
			box-shadow: 0 2px 8px rgba(22, 119, 255, 0.12);
		}
		&.done {
			background: #f6ffed;
			border-color: #b7eb8f;
		}
	}
	.attend-slot-title {
		font-size: 14px;
		font-weight: 600;
	}
	.attend-slot-sub {
		font-size: 12px;
		color: rgba(0, 0, 0, 0.45);
		margin: 4px 0 6px;
	}
	.attend-slot-tag {
		display: flex;
		align-items: center;
		gap: 8px;
	}
</style>
