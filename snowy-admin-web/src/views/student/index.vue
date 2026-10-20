<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="姓名/手机" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入姓名或家长手机" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="searchFormState.status" placeholder="请选择状态" :options="statusOptions" allow-clear />
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
					新增学员
				</a-button>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="goDetail(record)">详情</a>
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

	<a-modal v-model:open="open" :title="formData.id ? '编辑学员' : '新增学员'" :width="600" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="学员姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
						<a-input v-model:value="formData.name" placeholder="请输入学员姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="性别" name="gender">
						<a-radio-group v-model:value="formData.gender" :options="[{ value: '男', label: '男' }, { value: '女', label: '女' }]" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="生日" name="birthday">
						<a-date-picker v-model:value="formData.birthday" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="家长姓名" name="parentName">
						<a-input v-model:value="formData.parentName" placeholder="请输入家长姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="家长手机" name="parentPhone" :rules="[{ required: true, message: '请输入家长手机' }]">
						<a-input v-model:value="formData.parentPhone" placeholder="用于家长端绑定与通知" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="状态" name="status">
						<a-radio-group v-model:value="formData.status" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="备注" name="remark">
						<a-textarea v-model:value="formData.remark" :auto-size="{ minRows: 2, maxRows: 4 }" />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>
</template>

<script setup name="studentIndex">
	import { useRouter } from 'vue-router'
	import studentApi from '@/api/student/studentApi'
	const router = useRouter()
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const statusOptions = [
		{ value: 'STUDYING', label: '在读' },
		{ value: 'STOP', label: '停课' },
		{ value: 'REFUNDED', label: '已退费' }
	]
	const columns = [
		{ title: '学员姓名', dataIndex: 'name' },
		{ title: '性别', dataIndex: 'gender' },
		{ title: '家长手机', dataIndex: 'parentPhone' },
		{ title: '家长姓名', dataIndex: 'parentName' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '创建时间', dataIndex: 'createTime' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const loadData = (parameter) => {
		return studentApi.studentPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const statusText = (status) => {
		const item = statusOptions.find((i) => i.value === status)
		return item ? item.label : status
	}
	const statusColor = (status) => {
		return status === 'STUDYING' ? 'green' : status === 'STOP' ? 'orange' : 'red'
	}
	const onOpenAdd = () => {
		formData.value = { status: 'STUDYING', gender: '男' }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? studentApi.studentEdit : studentApi.studentAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		studentApi.studentDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	const goDetail = (record) => {
		router.push({ path: '/student/detail', query: { id: record.id } })
	}
</script>
