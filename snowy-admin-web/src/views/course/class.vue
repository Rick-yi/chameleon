<template>
	<div>
		<page-header title="班级管理" desc="班级、授课教师与每周固定课表" />
		<div class="class-toolbar">
			<a-tabs v-model:activeKey="activeClassType" class="class-type-tabs" @change="onClassTabChange">
				<a-tab-pane key="GROUP" tab="一对多" />
				<a-tab-pane key="ONE2ONE" tab="一对一" />
			</a-tabs>
			<div class="class-status-row">
				<a-radio-group v-model:value="statusFilter" size="small" button-style="solid" @change="onStatusChange">
					<a-radio-button value="ENABLE">在课</a-radio-button>
					<a-radio-button value="DISABLE">已停用</a-radio-button>
				</a-radio-group>
				<a-button v-if="hasPerm('/course/class/add')" type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增班级
				</a-button>
			</div>
			<div class="class-filter-row">
				<a-input-search
					v-model:value="searchFormState.searchKey"
					placeholder="班级名称"
					allow-clear
					style="width: 220px"
					@search="onSearchChange"
				/>
				<a class="class-filter-reset" @click="reset">重置</a>
				<a-select
					v-model:value="searchFormState.teacherId"
					placeholder="班级老师"
					:options="teacherOptions"
					:bordered="false"
					allow-clear
					show-search
					option-filter-prop="label"
					style="width: 140px"
					@change="(value) => onSelectChange('teacherId', value)"
				/>
				<a-select
					v-model:value="searchFormState.courseId"
					placeholder="关联课程"
					:options="courseOptions"
					:bordered="false"
					allow-clear
					style="width: 160px"
					@change="(value) => onSelectChange('courseId', value)"
				/>
				<a-select
					v-model:value="searchFormState.classroom"
					placeholder="教室"
					:options="classroomOptions"
					:bordered="false"
					allow-clear
					style="width: 120px"
					@change="(value) => onSelectChange('classroom', value)"
				/>
				<a-checkbox v-model:checked="onlyFull" @change="(event) => onCheckboxChange('full', event)">满班</a-checkbox>
				<a-checkbox v-model:checked="onlyScheduled" @change="(event) => onCheckboxChange('schedule', event)"
					>已排课</a-checkbox
				>
			</div>
		</div>
		<div class="ent-body">
			<s-table
				ref="tableRef"
				:columns="columns"
				:data="loadData"
				:row-key="(record) => record.id"
				:scroll="{ x: 'max-content' }"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'courseId'">
						{{ courseName(record.courseId) }}
					</template>
					<template v-if="column.dataIndex === 'studentName'">
						<span>{{ record.studentName || '未绑定学员' }}</span>
					</template>
					<template v-if="column.dataIndex === 'teacherId'">
						{{ teacherName(record.teacherId) }}
					</template>
					<template v-if="column.dataIndex === 'studentCount'">
						<a-tooltip :title="capacityTitle(record)">
							<a-tag :color="capacityColor(record)"
								>{{ record.studentCount || 0 }} / {{ record.capacity || '-' }}</a-tag
							>
						</a-tooltip>
					</template>
					<template v-if="column.dataIndex === 'weekTemplateJson'">
						<span v-if="templateSummary(record)">{{ templateSummary(record) }}</span>
						<span v-else style="color: rgba(0, 0, 0, 0.45)">未设置</span>
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
							<a v-if="hasPerm('/course/schedule/generate')" @click="goSchedule(record)">排课</a>
							<a-divider type="vertical" />
							<table-more :actions="classMoreActions" @select="(action) => onClassMore(action, record)" />
						</a-space>
					</template>
				</template>
			</s-table>
		</div>
	</div>

	<a-modal
		v-model:open="open"
		:title="formData.id ? '编辑班级' : '新增班级'"
		:width="700"
		:destroy-on-close="true"
		@ok="onSubmit"
	>
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="课程" name="courseId" :rules="[{ required: true, message: '请选择课程' }]">
						<a-select
							v-model:value="formData.courseId"
							placeholder="请选择课程"
							:options="courseOptions"
							@change="onCourseChange"
						/>
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="班级名称" name="name" :rules="[{ required: true, message: '请输入班级名称' }]">
						<a-input v-model:value="formData.name" placeholder="如：少儿美术启蒙 A1 / 张三钢琴一对一" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="班型" name="classType">
						<a-select
							v-model:value="formData.classType"
							:options="classTypeOptions"
							:disabled="selectedCourseIsOne2One"
							@change="onClassTypeChange"
						/>
					</a-form-item>
				</a-col>
				<a-col v-if="isOne2One" :span="16">
					<a-form-item
						label="绑定学员"
						name="studentId"
						extra="一对一班容量固定为 1，保存后该学员自动进入班级名册"
						:rules="[{ required: true, message: '请选择一对一绑定学员' }]"
					>
						<a-select
							v-model:value="formData.studentId"
							placeholder="搜索并选择学员"
							:options="studentOptions"
							show-search
							option-filter-prop="label"
							allow-clear
						/>
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="校区" name="campusId">
						<a-select v-model:value="formData.campusId" placeholder="请选择校区" :options="campusOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="教师" name="teacherId">
						<a-select
							v-model:value="formData.teacherId"
							placeholder="请选择教师"
							:options="teacherOptions"
							show-search
							option-filter-prop="label"
							allow-clear
						/>
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="默认教室" name="classroom">
						<a-input v-model:value="formData.classroom" placeholder="如：A101" allow-clear />
					</a-form-item>
				</a-col>
				<a-col v-if="!isOne2One" :span="12">
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

	<a-modal
		v-model:open="studentOpen"
		:title="`班级学员 · ${currentClass?.name || ''}`"
		:width="760"
		:destroy-on-close="true"
		:footer="null"
	>
		<div v-if="currentClass.classType !== 'ONE2ONE'" style="margin-bottom: 12px; display: flex; gap: 8px">
			<a-select
				v-model:value="addStudentIds"
				mode="multiple"
				placeholder="选择学员加入班级"
				:options="studentOptions"
				style="flex: 1"
				allow-clear
			/>
			<a-button
				v-if="hasPerm('/course/classStudent/add')"
				type="primary"
				:disabled="!addStudentIds || addStudentIds.length === 0"
				@click="onAddStudents"
				>加入班级</a-button
			>
		</div>
		<a-alert
			v-else
			type="info"
			show-icon
			style="margin-bottom: 12px"
			:message="`一对一班级已绑定学员：${currentClass.studentName || '未命名学员'}，如需换绑请编辑班级`"
		/>
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
			:show-pagination="false"
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
					<a-popconfirm v-if="currentClass.classType !== 'ONE2ONE'" title="确定退班吗？" @confirm="onQuit(record)">
						<a-button type="link" danger size="small">退班</a-button>
					</a-popconfirm>
					<span v-else style="color: rgba(0, 0, 0, 0.45)">请在编辑班级中换绑</span>
				</template>
			</template>
		</s-table>
	</a-modal>
</template>

<script setup name="courseClass">
	import { useRouter } from 'vue-router'
	import { Modal } from 'ant-design-vue'
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
	const activeClassType = ref('GROUP')
	const classTypeCount = ref({ GROUP: 0, ONE2ONE: 0 })
	const groupCount = computed(() => classTypeCount.value.GROUP)
	const one2oneCount = computed(() => classTypeCount.value.ONE2ONE)
	const statusFilter = ref('ENABLE')
	const onlyFull = ref(false)
	const onlyScheduled = ref(false)
	const classroomOptions = ref([])
	const refreshTable = () => {
		if (tableRef.value) tableRef.value.refresh(true)
	}
	const onClassTabChange = (key) => {
		activeClassType.value = key
		refreshTable()
	}
	const onStatusChange = (event) => {
		statusFilter.value = event?.target?.value || 'ENABLE'
		refreshTable()
	}
	const onSearchChange = () => refreshTable()
	const onSelectChange = (key, value) => {
		searchFormState.value = { ...searchFormState.value, [key]: value }
		refreshTable()
	}
	const onCheckboxChange = (key, event) => {
		const checked = !!event?.target?.checked
		if (key === 'full') onlyFull.value = checked
		if (key === 'schedule') onlyScheduled.value = checked
		refreshTable()
	}
	const statusOptions = [
		{ value: 'ENABLE', label: '启用' },
		{ value: 'DISABLE', label: '停用' }
	]
	const classTypeOptions = [
		{ value: 'GROUP', label: '一对多' },
		{ value: 'ONE2ONE', label: '一对一' }
	]
	const isOne2One = computed(() => formData.value.classType === 'ONE2ONE')
	const selectedCourseIsOne2One = computed(
		() => courseOptions.value.find((item) => item.value === formData.value.courseId)?.classType === 'ONE2ONE'
	)
	const columns = computed(() =>
		activeClassType.value === 'ONE2ONE'
			? [
					{ title: '班级名称', dataIndex: 'name' },
					{ title: '课程', dataIndex: 'courseId' },
					{ title: '绑定学员', dataIndex: 'studentName', width: 140 },
					{ title: '授课教师', dataIndex: 'teacherId', width: 120 },
					{ title: '教室', dataIndex: 'classroom', width: 110 },
					{ title: '固定课表', dataIndex: 'weekTemplateJson' },
					{ title: '状态', dataIndex: 'status', width: 90 },
					{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
				]
			: [
					{ title: '班级名称', dataIndex: 'name' },
					{ title: '课程', dataIndex: 'courseId' },
					{ title: '授课教师', dataIndex: 'teacherId', width: 120 },
					{ title: '教室', dataIndex: 'classroom', width: 110 },
					{ title: '在学/容量', dataIndex: 'studentCount', width: 120 },
					{ title: '固定课表', dataIndex: 'weekTemplateJson' },
					{ title: '状态', dataIndex: 'status', width: 90 },
					{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
				]
	)
	const studentColumns = [
		{ title: '学员', dataIndex: 'studentId' },
		{ title: '剩余课时', dataIndex: 'remain', width: 110 },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '入班时间', dataIndex: 'createTime', width: 170 },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]
	const loadData = (parameter) => {
		const params = Object.assign({}, parameter, searchFormState.value, {
			classType: activeClassType.value,
			status: statusFilter.value,
			full: onlyFull.value || undefined,
			hasSchedule: onlyScheduled.value || undefined
		})
		return courseApi.classPage(params).then((data) => data)
	}
	const loadTypeCounts = () => {
		Promise.all([
			courseApi.classPage({ current: 1, size: 1, classType: 'GROUP' }).catch(() => ({})),
			courseApi.classPage({ current: 1, size: 1, classType: 'ONE2ONE' }).catch(() => ({}))
		]).then(([group, one2one]) => {
			classTypeCount.value = { GROUP: group.total || 0, ONE2ONE: one2one.total || 0 }
		})
	}
	const loadClassrooms = () => {
		courseApi.classPage({ current: 1, size: 500 }).then((data) => {
			const values = [...new Set((data.records || []).map((item) => item.classroom).filter(Boolean))]
			classroomOptions.value = values.map((value) => ({ value, label: value }))
		})
	}
	const reset = () => {
		searchFormState.value = {}
		activeClassType.value = 'GROUP'
		statusFilter.value = 'ENABLE'
		onlyFull.value = false
		onlyScheduled.value = false
		tableRef.value.refresh(true)
	}
	const loadCourses = () => {
		courseApi.coursePage({ current: 1, size: 100 }).then((data) => {
			courseOptions.value = (data.records || []).map((item) => ({
				value: item.id,
				label: item.name,
				classType: item.classType
			}))
		})
		courseApi.classPage({ current: 1, size: 200 }).then((data) => {
			classRecords.value = data.records || []
		})
		courseApi.teacherOptions({}).then((data) => {
			teacherOptions.value = data || []
		})
	}
	const loadStudents = () => {
		studentApi.studentPage({ current: 1, size: 500 }).then((data) => {
			studentOptions.value = (data.records || []).map((item) => ({
				value: item.id,
				label: `${item.name}${item.parentPhone ? `（${item.parentPhone}）` : ''}`
			}))
			rememberStudentOption(formData.value.studentId, formData.value.studentName)
		})
	}
	const rememberStudentOption = (studentId, studentName) => {
		if (!studentId || !studentName) return
		if (!studentOptions.value.some((item) => item.value === studentId)) {
			studentOptions.value.unshift({ value: studentId, label: studentName })
		}
	}
	const courseName = (id) => {
		const item = courseOptions.value.find((i) => i.value === id)
		return item ? item.label : '-'
	}
	const teacherName = (id) => {
		const item = teacherOptions.value.find((i) => i.value === id)
		return item ? item.label : '未指定'
	}
	// 容量提示：满班红色、80% 以上橙色，避免前台继续加人
	const capacityTitle = (record) => {
		const capacity = Number(record.capacity || 0)
		if (!capacity) return '未设置容量'
		const remain = capacity - Number(record.studentCount || 0)
		return remain > 0 ? `剩余 ${remain} 个名额` : '已满班'
	}
	const capacityColor = (record) => {
		const capacity = Number(record.capacity || 0)
		const count = Number(record.studentCount || 0)
		if (!capacity) return 'default'
		if (count >= capacity) return 'red'
		if (count / capacity >= 0.8) return 'orange'
		return 'blue'
	}
	// 固定课表摘要：周二 09:00-10:30 等
	const templateSummary = (record) => {
		if (!record.weekTemplateJson) return ''
		try {
			const list =
				typeof record.weekTemplateJson === 'string' ? JSON.parse(record.weekTemplateJson) : record.weekTemplateJson
			if (!Array.isArray(list) || !list.length) return ''
			const names = '一二三四五六日'
			return list.map((item) => `周${names[item.dayOfWeek - 1]} ${item.startTime}-${item.endTime}`).join('、')
		} catch (e) {
			return ''
		}
	}
	const studentName = (id) => {
		const item = studentOptions.value.find((i) => i.value === id)
		if (item) return item.label
		if (id && id === currentClass.value.studentId) return currentClass.value.studentName || id
		return id
	}
	const loadOptions = () => {
		courseApi
			.campusOptions()
			.then((data) => {
				campusOptions.value = data || []
				applyDefaultCampus()
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
	/** 只有一个校区时默认选中 */
	const applyDefaultCampus = () => {
		if (!formData.value.campusId && campusOptions.value.length === 1) {
			formData.value.campusId = campusOptions.value[0].value
		}
	}
	const onClassTypeChange = (value) => {
		if (value === 'ONE2ONE') {
			formData.value.capacity = 1
			loadStudents()
			return
		}
		formData.value.studentId = undefined
		if (!formData.value.capacity || formData.value.capacity < 2) {
			formData.value.capacity = 15
		}
	}
	const onCourseChange = (courseId) => {
		const course = courseOptions.value.find((item) => item.value === courseId)
		if (course?.classType === 'ONE2ONE') {
			formData.value.classType = 'ONE2ONE'
			formData.value.capacity = 1
			loadStudents()
		} else if (!formData.value.classType) {
			formData.value.classType = 'GROUP'
		}
	}
	const onOpenAdd = () => {
		const classType = activeClassType.value
		formData.value = { status: 'ENABLE', capacity: classType === 'ONE2ONE' ? 1 : 15, classType }
		weekTemplate.value = []
		applyDefaultCampus()
		loadStudents()
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record, classType: record.classType || 'GROUP' }
		rememberStudentOption(record.studentId, record.studentName)
		weekTemplate.value = parseTemplate(record.weekTemplateJson)
		applyDefaultCampus()
		loadStudents()
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? courseApi.classEdit : courseApi.classAdd
			const weekTemplateValue = templateEditorRef.value ? templateEditorRef.value.getValue() : weekTemplate.value
			const body = {
				...formData.value,
				capacity: formData.value.classType === 'ONE2ONE' ? 1 : formData.value.capacity,
				studentId: formData.value.classType === 'ONE2ONE' ? formData.value.studentId : undefined,
				weekTemplateJson: JSON.stringify(weekTemplateValue || [])
			}
			api(body).then(() => {
				open.value = false
				activeClassType.value = body.classType === 'ONE2ONE' ? 'ONE2ONE' : 'GROUP'
				statusFilter.value = 'ENABLE'
				tableRef.value.refresh()
				loadTypeCounts()
				loadClassrooms()
			})
		})
	}
	const onDelete = (record) => {
		courseApi.classDelete([{ id: record.id }]).then(() => {
			tableRef.value.refresh()
			loadTypeCounts()
		})
	}
	// 操作列：次要操作收进「更多」下拉
	const classMoreActions = [
		{ key: 'edit', label: '编辑' },
		{
			key: 'delete',
			label: '删除',
			danger: true,
			confirmTitle: '确定删除该班级吗？',
			confirmContent: '删除后班内学员与点名记录会保留，但不再出现在班级列表。'
		}
	]
	const onClassMore = (action, record) => {
		if (action.key === 'edit') {
			onOpenEdit(record)
		} else if (action.key === 'delete') {
			onDelete(record)
		}
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
		return courseApi.classStudentPage({ current: 1, size: 100, classId: currentClass.value.id }).then((data) => {
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
	const doAddStudents = () => {
		const addCount = addStudentIds.value.length
		const currentCount = Number(currentClass.value.studentCount || 0)
		return courseApi
			.classStudentAdd({ classId: currentClass.value.id, studentIdList: addStudentIds.value })
			.then(() => {
				addStudentIds.value = []
				// 当前弹窗和背后的列表都同步人数，避免连续加人时用旧容量判断
				currentClass.value = { ...currentClass.value, studentCount: currentCount + addCount }
				studentTableRef.value.refresh()
				tableRef.value?.refresh()
			})
	}
	const onAddStudents = () => {
		const capacity = Number(currentClass.value.capacity || 0)
		const currentCount = Number(currentClass.value.studentCount || 0)
		const addCount = addStudentIds.value.length
		if (capacity > 0 && currentCount + addCount > capacity) {
			const exceed = currentCount + addCount - capacity
			Modal.confirm({
				title: '超出班级容量',
				content: `当前 ${currentCount}/${capacity} 人，本次加入 ${addCount} 人，将超出 ${exceed} 人。仍要加入吗？`,
				okText: '仍然加入',
				cancelText: '取消',
				onOk: doAddStudents
			})
			return
		}
		doAddStudents()
	}
	const onQuit = (record) => {
		courseApi.classStudentDelete([{ id: record.id }]).then(() => studentTableRef.value.refresh())
	}
	loadCourses()
	loadOptions()
	loadTypeCounts()
	loadClassrooms()
</script>

<style scoped>
	.class-toolbar {
		background: #fff;
		border-radius: var(--radius-card);
		padding: 0 16px 12px;
		margin-bottom: 12px;
	}
	.class-type-tabs :deep(.ant-tabs-nav) {
		margin-bottom: 8px;
	}
	.class-status-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 8px;
	}
	.class-filter-row {
		display: flex;
		align-items: center;
		gap: 12px;
		flex-wrap: wrap;
	}
	.class-filter-reset {
		color: var(--text-color-secondary);
		font-size: 13px;
	}
</style>
