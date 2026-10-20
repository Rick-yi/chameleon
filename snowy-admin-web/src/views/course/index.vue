<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="课程名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入课程名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="课程大类" name="categoryId">
						<a-select v-model:value="searchFormState.categoryId" placeholder="请选择课程大类" :options="categoryOptions" allow-clear />
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
				<a-space>
					<a-button type="primary" @click="onOpenAdd">
						<template #icon><plus-outlined /></template>
						新增课程
					</a-button>
					<a-button @click="onOpenAddCategory">
						<template #icon><plus-outlined /></template>
						新增大类
					</a-button>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'categoryId'">
					{{ categoryName(record.categoryId) }}
				</template>
				<template v-if="column.dataIndex === 'classType'">
					{{ classTypeText(record.classType) }}
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="record.status === 'ENABLE' ? 'green' : 'red'">
						{{ record.status === 'ENABLE' ? '启用' : '停用' }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
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

	<a-modal v-model:open="addOpen" :title="formData.id ? '编辑课程' : '新增课程'" :width="600" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="课程名称" name="name" :rules="[{ required: true, message: '请输入课程名称' }]">
						<a-input v-model:value="formData.name" placeholder="请输入课程名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="课程大类" name="categoryId">
						<a-select v-model:value="formData.categoryId" placeholder="请选择课程大类" :options="categoryOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="班型" name="classType">
						<a-select v-model:value="formData.classType" placeholder="请选择班型" :options="classTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="适合年龄" name="ageRange">
						<a-input v-model:value="formData.ageRange" placeholder="如：6-9岁" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="每课扣课时数" name="lessonsPerClass">
						<a-input-number v-model:value="formData.lessonsPerClass" :min="0.5" :step="0.5" style="width: 100%" />
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

	<a-modal v-model:open="categoryOpen" title="新增课程大类" :width="400" :destroy-on-close="true" @ok="onSubmitCategory">
		<a-form ref="categoryFormRef" :model="categoryFormData" layout="vertical">
			<a-form-item label="大类名称" name="name" :rules="[{ required: true, message: '请输入大类名称' }]">
				<a-input v-model:value="categoryFormData.name" placeholder="如：少儿美术" allow-clear />
			</a-form-item>
			<a-form-item label="排序码" name="sortCode">
				<a-input-number v-model:value="categoryFormData.sortCode" style="width: 100%" />
			</a-form-item>
		</a-form>
	</a-modal>
</template>

<script setup name="courseIndex">
	import courseApi from '@/api/course/courseApi'
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const addOpen = ref(false)
	const categoryOpen = ref(false)
	const categoryFormRef = ref()
	const categoryFormData = ref({})
	const categoryOptions = ref([])
	const classTypeOptions = [
		{ value: 'SMALL', label: '小班' },
		{ value: 'BIG', label: '大班' },
		{ value: 'ONE2ONE', label: '一对一' }
	]
	const statusOptions = [
		{ value: 'ENABLE', label: '启用' },
		{ value: 'DISABLE', label: '停用' }
	]
	const columns = [
		{ title: '课程名称', dataIndex: 'name' },
		{ title: '课程大类', dataIndex: 'categoryId' },
		{ title: '班型', dataIndex: 'classType' },
		{ title: '适合年龄', dataIndex: 'ageRange' },
		{ title: '每课扣课时', dataIndex: 'lessonsPerClass' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const loadData = (parameter) => {
		return courseApi.coursePage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const loadCategories = () => {
		courseApi.categoryList().then((data) => {
			categoryOptions.value = (data || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const categoryName = (id) => {
		const item = categoryOptions.value.find((i) => i.value === id)
		return item ? item.label : '-'
	}
	const classTypeText = (type) => {
		const item = classTypeOptions.find((i) => i.value === type)
		return item ? item.label : type || '-'
	}
	const onOpenAdd = () => {
		formData.value = { status: 'ENABLE', lessonsPerClass: 1 }
		addOpen.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		addOpen.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? courseApi.courseEdit : courseApi.courseAdd
			api(formData.value).then(() => {
				addOpen.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		courseApi.courseDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	const onOpenAddCategory = () => {
		categoryFormData.value = {}
		categoryOpen.value = true
	}
	const onSubmitCategory = () => {
		categoryFormRef.value.validate().then(() => {
			courseApi.categoryAdd(categoryFormData.value).then(() => {
				categoryOpen.value = false
				loadCategories()
			})
		})
	}
	loadCategories()
</script>
