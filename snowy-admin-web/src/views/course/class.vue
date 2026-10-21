<template>
	<div>
		<page-header title="班级管理" desc="班级、授课教师与每周固定课表">
			<template #extra>
				<a-button type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增班级
				</a-button>
			</template>
		</page-header>
		<div class="ent-toolbar">
			<a-form layout="inline" :model="searchFormState">
				<a-form-item label="班级名称">
					<a-input v-model:value="searchFormState.searchKey" placeholder="请输入班级名称" allow-clear style="width: 220px" />
				</a-form-item>
				<a-form-item label="课程">
					<a-select v-model:value="searchFormState.courseId" placeholder="全部课程" :options="courseOptions" allow-clear style="width: 200px" />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="tableRef.refresh(true)">
							<template #icon><SearchOutlined /></template>
							查询
						</a-button>
						<a-button @click="reset">
							<template #icon><redo-outlined /></template>
							重置
						</a-button>
					</a-space>
				</a-form-item>
			</a-form>
		</div>
		<div class="ent-body">
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:scroll="{ x: 'max-content' }"
		>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'courseId'">
					{{ courseName(record.courseId) }}
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="record.status === 'ENABLE' ? 'green' : 'red'">
						{{ record.status === 'ENABLE' ? '启用' : '停用' }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="onOpenStudent(record)">学员</a>
						<a-divider type="vertical" />
						<a @click="goSchedule(record)">排课</a>
						<a-divider type="vertical" />
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a-popconfirm title="确定要删除吗？" @confirm="onDelete(record)">
							<a-button type="link" danger size="small">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
		</div>
	</div>

	<a-modal v-model:open="open" :title="formData.id ? '编辑班级' : '新增班级'" :width="700" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="课程" name="courseId" :rules="[{ required: true, message: '请选择课程' }]">
						<a-select v-model:value="formData.courseId" placeholder="请选择课程" :options="courseOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="班级名称" name="name" :rules="[{ required: true, message: '请输入班级名称' }]">
						<a-input v-model:value="formData.name" placeholder="如：少儿美术启蒙 A1" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="校区" name="campusId">
						<a-select v-model:value="formData.campusId" placeholder="请选择校区" :options="campusOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="教师" name="teacherId">
						<a-select v-model:value="formData.teacherId" placeholder="请选择教师" :options="teacherOptions" show-search option-filter-prop="label" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="默认教室" name="classroom">
						<a-input v-model:value="formData.classroom" placeholder="如：A101" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="容量(人)" name="capacity">
						<a-input-number v-model:value="formData.capacity" :min="1" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="状态" name="status">
						<a-radio-group v-model:value="formData.status" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="每周固定课表">
						<week-template-editor ref="templateEditorRef" :model-value="weekTemplate" />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>

	<a-modal v-model:open="studentOpen" :title="`班级学员 · ${currentClass?.name || ''}`" :width="760" :destroy-on-close="true" :footer="null">
		<div style="margin-bottom: 12px; display: flex; gap: 8px">
			<a-select v-model:value="addStudentIds" mode="multiple" placeholder="选择学员加入班级" :options="studentOptions" style="flex: 1" allow-clear />
			<a-button type="primary" :disabled="!addStudentIds || addStudentIds.length === 0" @click="onAddStudents">加入班级</a-button>
		</div>
		<a-alert
			type="info"
			show-icon
			style="margin-bottom: 12px"
			message="课时余额按「课程」计算：同一课程下的多个班级共用一个课时账户，任一班消耗都会扣减同一余额"
		/>
		<s-table
			ref="studentTableRef"
			:columns="studentColumns"
			:data="loadClassStudents"
			bordered
			:row-key="(record) => record.id"
			size="small"
			:pagination="false"
		>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'studentId'">
					{{ studentName(record.studentId) }}
				</template>
				<template v-if="column.dataIndex === 'remain'">
					<a-tag v-if="!balanceMap[record.studentId]" color="red">未购课</a-tag>
					<a-tag v-else-if="balanceMap[record.studentId].remainLessons <= 0" color="red">课时已用完</a-tag>
					<a-tag v-else-if="balanceMap[record.studentId].remainLessons <= 3" color="orange">
						剩 {{ balanceMap[record.studentId].remainLessons }}
					</a-tag>
					<span v-else>剩 {{ balanceMap[record.studentId].remainLessons }}</span>
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="record.status === 'STUDYING' ? 'green' : 'default'">
						{{ record.status === 'STUDYING' ? '在读' : '退班' }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-popconfirm title="确定退班吗？" @confirm="onQuit(record)">
						<a-button type="link" danger size="small">退班</a-button>
					</a-popconfirm>
				</template>
			</template>
		</s-table>
	</a-modal>
</template>

<script setup name="courseClass">
	import { useRouter } from 'vue-router'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const router = useRouter()
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const courseOptions = ref([])
	const classRecords = ref([])
	const studentOptions = ref([])
	const campusOptions = ref([])
	const teacherOptions = ref([])
	const weekTemplate = ref([])
	const templateEditorRef = ref()
	const studentOpen = ref(false)
	const studentTableRef = ref()
	const currentClass = ref({})
	const addStudentIds = ref([])
	const statusOptions = [
		{ value: 'ENABLE', label: '启用' },
		{ value: 'DISABLE', label: '停用' }
	]
	const columns = [
		{ title: '班级名称', dataIndex: 'name' },
		{ title: '课程', dataIndex: 'courseId' },
		{ title: '教室', dataIndex: 'classroom' },
		{ title: '容量', dataIndex: 'capacity' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const studentColumns = [
		{ title: '学员', dataIndex: 'studentId' },
		{ title: '剩余课时', dataIndex: 'remain', width: 110 },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '入班时间', dataIndex: 'createTime', width: 170 },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]
	const loadData = (parameter) => {
		return courseApi.classPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const loadCourses = () => {
		courseApi.coursePage({ current: 1, size: 100 }).then((data) => {
			courseOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
		courseApi.classPage({ current: 1, size: 200 }).then((data) => {
			classRecords.value = data.records || []
		})
	}
	const loadStudents = () => {
		studentApi.studentPage({ current: 1, size: 100 }).then((data) => {
			studentOptions.value = (data.records || []).map((item) => ({ value: item.id, label: `${item.name}(${item.parentPhone})` }))
		})
	}
	const courseName = (id) => {
		const item = courseOptions.value.find((i) => i.value === id)
		return item ? item.label : '-'
	}
	const studentName = (id) => {
		const item = studentOptions.value.find((i) => i.value === id)
		return item ? item.label : id
	}
	const loadOptions = () => {
		courseApi
			.campusOptions()
			.then((data) => {
				campusOptions.value = data || []
			})
			.catch(() => (campusOptions.value = []))
		courseApi
			.teacherOptions({})
			.then((data) => {
				teacherOptions.value = data || []
			})
			.catch(() => (teacherOptions.value = []))
	}
	const parseTemplate = (json) => {
		if (!json) return []
		try {
			const list = typeof json === 'string' ? JSON.parse(json) : json
			return Array.isArray(list) ? list : []
		} catch (e) {
			return []
		}
	}
	const onOpenAdd = () => {
		formData.value = { status: 'ENABLE', capacity: 15 }
		weekTemplate.value = []
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		weekTemplate.value = parseTemplate(record.weekTemplateJson)
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? courseApi.classEdit : courseApi.classAdd
			const weekTemplateValue = templateEditorRef.value ? templateEditorRef.value.getValue() : weekTemplate.value
			const body = { ...formData.value, weekTemplateJson: JSON.stringify(weekTemplateValue || []) }
			api(body).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		courseApi.classDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	const goSchedule = (record) => {
		router.push({ path: '/course/schedule', query: { classId: record.id } })
	}
	const onOpenStudent = (record) => {
		currentClass.value = record
		addStudentIds.value = []
		studentOpen.value = true
		loadStudents()
	}
	const balanceMap = ref({})
	const loadClassStudents = (parameter) => {
		return courseApi
			.classStudentPage({ current: 1, size: 100, classId: currentClass.value.id })
			.then((data) => {
				// 课时余额按课程维度查询：让老师一眼看出谁还有课可上
				const cls = classRecords.value.find((item) => item.id === currentClass.value.id)
				const studentIdList = (data.records || []).map((item) => item.studentId)
				if (cls?.courseId && studentIdList.length) {
					studentApi
						.lessonBalance({ studentIds: studentIdList.join(','), courseId: cls.courseId })
						.then((list) => {
							const map = {}
							for (const item of list || []) map[item.studentId] = item
							balanceMap.value = map
						})
						.catch(() => (balanceMap.value = {}))
				} else {
					balanceMap.value = {}
				}
				return data
			})
	}
	const onAddStudents = () => {
		courseApi.classStudentAdd({ classId: currentClass.value.id, studentIdList: addStudentIds.value }).then(() => {
			addStudentIds.value = []
			studentTableRef.value.refresh()
		})
	}
	const onQuit = (record) => {
		courseApi.classStudentDelete([{ id: record.id }]).then(() => studentTableRef.value.refresh())
	}
	loadCourses()
	loadOptions()
</script>
