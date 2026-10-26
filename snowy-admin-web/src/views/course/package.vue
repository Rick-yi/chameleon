<template>
	<div>
		<page-header title="课包管理" desc="售卖课包、课时数、赠课与有效期">
			<template #extra>
				<a-button v-if="hasPerm('/course/package/add')" type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增课包
				</a-button>
			</template>
		</page-header>
		<div class="ent-toolbar">
			<a-form layout="inline" :model="searchFormState">
				<a-form-item label="课包名称">
					<a-input v-model:value="searchFormState.searchKey" placeholder="请输入课包名称" allow-clear style="width: 220px" />
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
			<template #operator>
				<a-button type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增课包
				</a-button>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'courseId'">
					{{ courseName(record.courseId) }}
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="record.status === 'ENABLE' ? 'green' : 'default'">
						{{ record.status === 'ENABLE' ? '上架' : '下架' }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a v-if="hasPerm('/course/package/edit')" @click="onOpenEdit(record)">编辑</a>
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

	<a-modal v-model:open="open" :title="formData.id ? '编辑课包' : '新增课包'" :width="600" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="课程" name="courseId" :rules="[{ required: true, message: '请选择课程' }]">
						<a-select v-model:value="formData.courseId" placeholder="请选择课程" :options="courseOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="课包名称" name="name" :rules="[{ required: true, message: '请输入课包名称' }]">
						<a-input v-model:value="formData.name" placeholder="如：30课时包" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="课时数" name="lessons">
						<a-input-number v-model:value="formData.lessons" :min="1" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="赠课课时" name="giftLessons" extra="赠课不限；正课课时不能超过60">
						<a-input-number v-model:value="formData.giftLessons" :min="0" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="价格(元)" name="price">
						<a-input-number v-model:value="formData.price" :min="0" :precision="2" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="有效期(天)" name="validityDays">
						<a-input-number v-model:value="formData.validityDays" :min="0" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="赠送时长(天)" name="giftValidityDays">
						<a-input-number v-model:value="formData.giftValidityDays" :min="0" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="每课扣课时数" name="lessonsPerClass">
						<a-input-number v-model:value="formData.lessonsPerClass" :min="0.5" :step="0.5" placeholder="沿用课程配置" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="请假是否扣课" name="leaveDeduct">
						<a-switch v-model:value="formData.leaveDeduct" checked-children="扣课" un-checked-children="不扣" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="缺勤是否扣课" name="absentDeduct">
						<a-switch v-model:value="formData.absentDeduct" checked-children="扣课" un-checked-children="不扣" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="状态" name="status">
						<a-radio-group v-model:value="formData.status" :options="statusOptions" />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>
</template>

<script setup name="coursePackage">
	import { message } from 'ant-design-vue'
	import courseApi from '@/api/course/courseApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const courseOptions = ref([])
	const statusOptions = [
		{ value: 'ENABLE', label: '上架' },
		{ value: 'DISABLE', label: '下架' }
	]
	const columns = [
		{ title: '课包名称', dataIndex: 'name' },
		{ title: '课程', dataIndex: 'courseId' },
		{ title: '课时', dataIndex: 'lessons' },
		{ title: '赠课', dataIndex: 'giftLessons' },
		{ title: '价格(元)', dataIndex: 'price' },
		{ title: '有效期(天)', dataIndex: 'validityDays' },
		{ title: '赠送时长(天)', dataIndex: 'giftValidityDays' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const loadData = (parameter) => {
		return courseApi.packagePage(Object.assign(parameter, searchFormState.value)).then((data) => data)
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
	const courseName = (id) => {
		const item = courseOptions.value.find((i) => i.value === id)
		return item ? item.label : '-'
	}
	const onOpenAdd = () => {
		formData.value = { status: 'ENABLE', giftLessons: 0, giftValidityDays: 0 }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = {
			...record,
			leaveDeduct: record.leaveDeduct === true,
			absentDeduct: record.absentDeduct === null || record.absentDeduct === undefined ? true : record.absentDeduct
		}
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			if (Number(formData.value.lessons || 0) > 60) {
				message.warning('正课课时不能超过60课时')
				return
			}
			const edit = !!formData.value.id
			const api = edit ? courseApi.packageEdit : courseApi.packageAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		courseApi.packageDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	loadCourses()
</script>
