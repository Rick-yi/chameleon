<template>
	<xn-panel>
		<div style="padding: 16px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap; border-bottom: 1px solid #f0f0f0">
			<a-select v-model:value="classId" placeholder="选择班级" :options="classOptions" style="width: 220px" @change="loadSchedules" />
			<a-date-picker v-model:value="date" value-format="YYYY-MM-DD" style="width: 160px" @change="loadSchedules" />
			<a-select v-model:value="scheduleId" placeholder="选择上课节次" :options="scheduleOptions" style="width: 300px" @change="loadAttendPage" />
			<div style="margin-left: auto">
				<span v-if="scheduleId" style="color: rgba(0,0,0,.45); font-size: 12px">点击状态按钮标记出勤，到课/缺勤将按课程规则扣课时</span>
			</div>
		</div>
		<div style="padding: 16px">
			<a-empty v-if="!scheduleId" description="请先选择班级、日期与节次" />
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
	</xn-panel>
</template>

<script setup name="courseAttend">
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const classId = ref()
	const date = ref(new Date().toISOString().slice(0, 10))
	const scheduleId = ref()
	const classOptions = ref([])
	const scheduleOptions = ref([])
	const students = ref([])
	const studentMap = ref({})
	const columns = [
		{ title: '学员', dataIndex: 'studentId' },
		{ title: '点名状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 100 }
	]
	const loadClasses = () => {
		courseApi.classPage({ current: 1, size: 100 }).then((data) => {
			classOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const loadSchedules = () => {
		scheduleId.value = undefined
		if (!classId.value || !date.value) return
		courseApi
			.schedulePage({ current: 1, size: 50, classId: classId.value, startDate: `${date.value} 00:00:00`, endDate: `${date.value} 00:00:00` })
			.then((data) => {
				scheduleOptions.value = (data.records || []).map((item) => ({
					value: item.id,
					label: `${item.scheduleDate.slice(0, 10)} ${item.startTime.slice(0, 5)}-${item.endTime.slice(0, 5)} ${item.classroom || ''}`
				}))
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
		})
	}
	const onConfirm = () => {
		const attendList = students.value
			.filter((s) => !s.attendId)
			.map((s) => ({ studentId: s.studentId, attendStatus: s.attendStatus }))
		if (attendList.length === 0) return
		courseApi.attendConfirm({ scheduleId: scheduleId.value, attendList }).then(() => loadAttendPage())
	}
	const onCancelAttend = (record) => {
		courseApi.attendCancel({ id: record.attendId }).then(() => loadAttendPage())
	}
	loadClasses()
	loadStudents()
</script>
