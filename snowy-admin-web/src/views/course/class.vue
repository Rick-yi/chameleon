<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="班级名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入班级名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="课程" name="courseId">
						<a-select v-model:value="searchFormState.courseId" placeholder="请选择课程" :options="courseOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
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
				</a-col>
			</a-row>
		</a-form>
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:scroll="{ x: 'max-content' }"
		>
			<template #operator>
				<a-button type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增班级
				</a-button>
			</template>
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
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a-popconfirm title="确定要删除吗？" @confirm="onDelete(record)">
							<a-button type="link" danger size="small">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</xn-panel>

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
					<a-form-item label="校区id" name="campusId">
						<a-input v-model:value="formData.campusId" placeholder="可选" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="教师id" name="teacherId">
						<a-input v-model:value="formData.teacherId" placeholder="系统用户id" allow-clear />
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
					<a-form-item label="周课表模板(JSON数组)" name="weekTemplateJson">
						<a-textarea
							v-model:value="formData.weekTemplateJson"
							:auto-size="{ minRows: 3, maxRows: 6 }"
							placeholder='[{"dayOfWeek":2,"startTime":"09:00","endTime":"10:30","classroom":"A101","teacherId":""}]'
						/>
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
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const courseOptions = ref([])
	const studentOptions = ref([])
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
		{ title: '状态', dataIndex: 'status' },
		{ title: '入班时间', dataIndex: 'createTime' },
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
	const onOpenAdd = () => {
		formData.value = { status: 'ENABLE', capacity: 15 }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? courseApi.classEdit : courseApi.classAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		courseApi.classDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	const onOpenStudent = (record) => {
		currentClass.value = record
		addStudentIds.value = []
		studentOpen.value = true
		loadStudents()
	}
	const loadClassStudents = (parameter) => {
		return courseApi.classStudentPage({ current: 1, size: 100, classId: currentClass.value.id }).then((data) => data)
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
</script>
