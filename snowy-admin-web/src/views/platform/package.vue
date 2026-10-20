<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="套餐名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入套餐名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
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
					新增套餐
				</a-button>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a-popconfirm title="确定删除该套餐吗？" @confirm="onDelete(record)">
							<a-button type="link" danger size="small">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</xn-panel>

	<a-modal v-model:open="open" :title="formData.id ? '编辑套餐' : '新增套餐'" :width="600" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="套餐名称" name="name" :rules="[{ required: true, message: '请输入套餐名称' }]">
						<a-input v-model:value="formData.name" placeholder="如：专业版" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="价格(元)" name="price">
						<a-input-number v-model:value="formData.price" :min="0" :precision="2" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="有效期(天)" name="validityDays">
						<a-input-number v-model:value="formData.validityDays" :min="1" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="排序码" name="sortCode">
						<a-input-number v-model:value="formData.sortCode" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="学员数上限" name="studentLimit">
						<a-input-number v-model:value="formData.studentLimit" :min="0" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="校区数上限" name="campusLimit">
						<a-input-number v-model:value="formData.campusLimit" :min="0" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="备注" name="remark">
						<a-input v-model:value="formData.remark" placeholder="请输入备注" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>
</template>

<script setup name="platformPackage">
	import tenantApi from '@/api/platform/tenantApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const columns = [
		{ title: '套餐名称', dataIndex: 'name' },
		{ title: '价格(元)', dataIndex: 'price' },
		{ title: '有效期(天)', dataIndex: 'validityDays' },
		{ title: '学员上限', dataIndex: 'studentLimit' },
		{ title: '校区上限', dataIndex: 'campusLimit' },
		{ title: '排序', dataIndex: 'sortCode' },
		{ title: '备注', dataIndex: 'remark' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const loadData = (parameter) => {
		return tenantApi.packagePage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const onOpenAdd = () => {
		formData.value = { validityDays: 365, sortCode: 1 }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? tenantApi.packageEdit : tenantApi.packageAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		tenantApi.packageDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
</script>
