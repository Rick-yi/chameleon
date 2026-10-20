<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="机构名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入机构名称/联系人" allow-clear />
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
					开通租户
				</a-button>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a-popconfirm title="确定删除该租户吗？" @confirm="onDelete(record)">
							<a-button type="link" danger size="small">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</xn-panel>

	<a-modal v-model:open="open" :title="formData.id ? '编辑租户' : '开通租户'" :width="560" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="机构名称" name="name" :rules="[{ required: true, message: '请输入机构名称' }]">
						<a-input v-model:value="formData.name" placeholder="请输入机构名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="联系人" name="contactName">
						<a-input v-model:value="formData.contactName" placeholder="请输入联系人" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="联系电话" name="contactPhone">
						<a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="套餐id" name="packageId">
						<a-input v-model:value="formData.packageId" placeholder="如：pkg_demo" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="到期时间" name="expireTime">
						<a-date-picker v-model:value="formData.expireTime" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="formData.status" placeholder="请选择状态" :options="statusOptions" />
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

<script setup name="platformTenant">
	import tenantApi from '@/api/platform/tenantApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const statusOptions = [
		{ value: 'TRIAL', label: '试用' },
		{ value: 'NORMAL', label: '正常' },
		{ value: 'STOP', label: '停用' }
	]
	const columns = [
		{ title: '机构名称', dataIndex: 'name' },
		{ title: '联系人', dataIndex: 'contactName' },
		{ title: '联系电话', dataIndex: 'contactPhone' },
		{ title: '套餐id', dataIndex: 'packageId' },
		{ title: '到期时间', dataIndex: 'expireTime' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const loadData = (parameter) => {
		return tenantApi.tenantPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const statusText = (s) => ({ TRIAL: '试用', NORMAL: '正常', STOP: '停用' }[s] || s)
	const statusColor = (s) => (s === 'NORMAL' ? 'green' : s === 'TRIAL' ? 'orange' : 'red')
	const onOpenAdd = () => {
		formData.value = { status: 'TRIAL' }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? tenantApi.tenantEdit : tenantApi.tenantAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		tenantApi.tenantDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
</script>
