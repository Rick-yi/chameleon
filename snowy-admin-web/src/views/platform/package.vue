<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="套餐名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入套餐名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="6" :xl="6">
					<a-form-item label="上下架" name="status">
						<a-select v-model:value="searchFormState.status" placeholder="全部" :options="statusOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="10" :xl="10">
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
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="record.status === 'OFF' ? 'default' : 'green'">{{ record.status === 'OFF' ? '已下架' : '已上架' }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'limits'">
					<div class="limit-cell">
						<span>学员 {{ record.studentLimit ?? '不限' }}</span>
						<span>校区 {{ record.campusLimit ?? '不限' }}</span>
						<span>账号 {{ record.accountLimit ?? '不限' }}</span>
					</div>
				</template>
				<template v-if="column.dataIndex === 'features'">
					<a-space wrap :size="[4, 4]">
						<a-tag v-for="item in funcOptions" :key="item.value" :color="isFuncOn(record, item.value) ? 'blue' : 'default'">
							{{ item.label }}
						</a-tag>
					</a-space>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space :size="4">
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a @click="onCopy(record)">复制</a>
						<a-divider type="vertical" />
						<a v-if="record.status === 'OFF'" @click="onChangeStatus(record, 'ON')">上架</a>
						<a v-else @click="onChangeStatus(record, 'OFF')">下架</a>
						<a-divider type="vertical" />
						<a-popconfirm title="确定删除该套餐吗？已被租户使用时不允许删除。" @confirm="onDelete(record)">
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
				<a-col :span="12">
					<a-form-item label="账号数上限" name="accountLimit">
						<a-input-number v-model:value="formData.accountLimit" :min="0" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="上下架" name="status">
						<a-radio-group v-model:value="formData.status" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="功能开关" name="funcConfig">
						<a-space wrap>
							<a-tag v-for="item in funcOptions" :key="item.value" :color="funcConfig[item.value] ? 'green' : 'default'" :closable="false">
								{{ item.label }}
								<a-switch
									v-model:checked="funcConfig[item.value]"
									size="small"
									:checked-children="开"
									:unchecked-children="关"
									style="margin-left: 4px"
								/>
							</a-tag>
						</a-space>
						<div style="color: rgba(0, 0, 0, 0.45); font-size: 12px; margin-top: 6px">
							报表 / 自动化 / 家长端 / 支付为增值功能开关；招生、学员、课务、财务控制对应菜单是否下发。关闭后该租户看不到对应入口。
						</div>
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
	import { message } from 'ant-design-vue'
	import tenantApi from '@/api/platform/tenantApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const funcConfig = ref({})
	const open = ref(false)
	const statusOptions = [
		{ value: 'ON', label: '上架' },
		{ value: 'OFF', label: '下架' }
	]
	const columns = [
		{ title: '套餐名称', dataIndex: 'name' },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '价格(元)', dataIndex: 'price' },
		{ title: '有效期(天)', dataIndex: 'validityDays' },
		{ title: '套餐限额', dataIndex: 'limits' },
		{ title: '功能开关', dataIndex: 'features' },
		{ title: '排序', dataIndex: 'sortCode', width: 70 },
		{ title: '备注', dataIndex: 'remark' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right', width: 220 }
	]
	const loadData = (parameter) => {
		return tenantApi.packagePage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const funcOptions = [
		{ value: 'report', label: '报表' },
		{ value: 'automation', label: '自动化' },
		{ value: 'parent', label: '家长端' },
		{ value: 'payment', label: '支付' },
		{ value: 'crm', label: '招生' },
		{ value: 'student', label: '学员' },
		{ value: 'course', label: '课务' },
		{ value: 'finance', label: '财务' }
	]
	const defaultFuncConfig = () => {
		const config = {}
		funcOptions.forEach((item) => (config[item.value] = true))
		return config
	}
	const parseFuncConfig = (raw) => {
		const config = defaultFuncConfig()
		if (!raw) return config
		try {
			const parsed = JSON.parse(raw)
			Object.keys(config).forEach((key) => {
				if (parsed && parsed[key] === false) {
					config[key] = false
				}
			})
		} catch (e) {
			// 解析失败时按全开处理
		}
		return config
	}
	const isFuncOn = (record, key) => parseFuncConfig(record.funcConfig)[key] !== false
	const onOpenAdd = () => {
		formData.value = { validityDays: 365, sortCode: 1, status: 'ON' }
		funcConfig.value = defaultFuncConfig()
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		funcConfig.value = parseFuncConfig(record.funcConfig)
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? tenantApi.packageEdit : tenantApi.packageAdd
			const payload = { ...formData.value, funcConfig: JSON.stringify(funcConfig.value) }
			api(payload).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		tenantApi.packageDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	const onCopy = (record) => {
		tenantApi.packageCopy({ id: record.id }).then(() => {
			message.success('已复制套餐，副本默认下架，请按需调整后上架')
			tableRef.value.refresh()
		})
	}
	const onChangeStatus = (record, status) => {
		tenantApi.packageStatus({ id: record.id, status }).then(() => {
			message.success(status === 'ON' ? '套餐已上架' : '套餐已下架')
			tableRef.value.refresh()
		})
	}
</script>

<style scoped>
	.limit-cell {
		display: flex;
		gap: 10px;
		white-space: nowrap;
		color: var(--text-color-secondary);
	}
</style>
