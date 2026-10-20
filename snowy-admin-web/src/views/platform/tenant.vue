<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="机构名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入机构名称/编码/联系人" allow-clear />
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
				<a-space>
					<a-button type="primary" @click="onOpenAdd">
						<template #icon><plus-outlined /></template>
						开通租户
					</a-button>
					<a-button @click="onCheckExpired">到期扫描</a-button>
				</a-space>
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

	<a-modal v-model:open="open" :title="formData.id ? '编辑租户' : '开通租户'" :width="640" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="机构编码" name="code" :rules="[{ required: true, message: '请输入机构编码' }]">
						<a-input v-model:value="formData.code" placeholder="登录时使用，如 demo" allow-clear />
					</a-form-item>
				</a-col>
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
					<a-form-item label="套餐" name="packageId">
						<a-select v-model:value="formData.packageId" placeholder="请选择套餐" :options="packageOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="到期时间" name="expireTime">
						<a-date-picker v-model:value="formData.expireTime" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12" v-if="formData.id">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="formData.status" placeholder="请选择状态" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<template v-if="!formData.id">
					<a-col :span="24">
						<a-alert
							type="info"
							show-icon
							message="开通后系统会自动创建机构管理员、默认校区、管理员角色与业务菜单授权"
							style="margin-bottom: 16px"
						/>
					</a-col>
					<a-col :span="12">
						<a-form-item label="管理员账号" name="adminAccount">
							<a-input v-model:value="formData.adminAccount" placeholder="默认 admin" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="管理员姓名" name="adminName">
							<a-input v-model:value="formData.adminName" placeholder="默认 机构管理员" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="管理员初始密码" name="adminPassword">
							<a-input-password v-model:value="formData.adminPassword" placeholder="留空使用系统默认密码" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="默认校区名称" name="campusName">
							<a-input v-model:value="formData.campusName" placeholder="默认 默认校区" allow-clear />
						</a-form-item>
					</a-col>
				</template>
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
	import { message } from 'ant-design-vue'
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
	const packageOptions = ref([])
	const loadPackages = () => {
		tenantApi.packagePage({ current: 1, size: 100 }).then((data) => {
			packageOptions.value = (data.records || []).map((p) => ({ value: p.id, label: `${p.name}（${p.validityDays || 365}天）` }))
		})
	}
	const onCheckExpired = () => {
		tenantApi.tenantCheckExpired().then((count) => {
			tableRef.value.refresh()
			if (count > 0) {
				message.success(`已停用 ${count} 个到期租户`)
			} else {
				message.info('没有到期租户')
			}
		})
	}
	const columns = [
		{ title: '机构编码', dataIndex: 'code' },
		{ title: '机构名称', dataIndex: 'name' },
		{ title: '联系人', dataIndex: 'contactName' },
		{ title: '联系电话', dataIndex: 'contactPhone' },
		{ title: '套餐', dataIndex: 'packageId', customRender: ({ record }) => packageName(record.packageId) },
		{ title: '到期时间', dataIndex: 'expireTime' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const packageName = (id) => {
		const item = packageOptions.value.find((p) => p.value === id)
		return item ? item.label : id
	}
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
		formData.value = { status: 'TRIAL', adminAccount: 'admin', adminName: '机构管理员', campusName: '默认校区' }
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
	loadPackages()
</script>
