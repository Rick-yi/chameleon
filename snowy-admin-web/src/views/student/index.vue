<template>
	<div>
		<page-header title="学员管理" desc="学员档案、课时账户与报名入口">
			<template #extra>
				<a-button type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增学员
				</a-button>
			</template>
		</page-header>
		<div class="ent-toolbar">
			<a-form layout="inline" :model="searchFormState">
				<a-form-item label="姓名/手机">
					<a-input v-model:value="searchFormState.searchKey" placeholder="请输入姓名或家长手机" allow-clear style="width: 220px" />
				</a-form-item>
				<a-form-item label="状态">
					<a-select v-model:value="searchFormState.status" placeholder="全部状态" :options="statusOptions" allow-clear style="width: 140px" />
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
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="goDetail(record)">详情</a>
						<a-divider type="vertical" />
						<a @click="onOpenSignup(record)">报名</a>
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

	<a-modal v-model:open="payOpen" title="线下收款登记" :width="480" :destroy-on-close="true" @ok="onSubmitPay">
		<a-form ref="payFormRef" :model="payFormData" layout="vertical">
			<a-alert type="info" show-icon message="请核对实收金额，确认后课时立即入账" style="margin-bottom: 16px" />
			<a-form-item label="订单">
				<a-input :value="`${payFormData.studentName || ''} · ${payFormData.packageName || ''}`" disabled />
			</a-form-item>
			<a-form-item label="应收金额">
				<a-input :value="`¥${payFormData.amount || 0}`" disabled />
			</a-form-item>
			<a-form-item label="收款渠道" name="payChannel">
				<a-select v-model:value="payFormData.payChannel" :options="payChannelOptions" />
			</a-form-item>
			<a-form-item label="实收金额" name="paidAmount" :rules="[{ required: true, message: '请输入实收金额' }]">
				<a-input-number v-model:value="payFormData.paidAmount" :min="0" :precision="2" style="width: 100%" />
			</a-form-item>
			<a-form-item label="备注" name="remark">
				<a-input v-model:value="payFormData.remark" placeholder="如：收款人、转账流水号" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<a-modal v-model:open="signupOpen" :title="`报名购课 · ${signupFormData.studentName || ''}`" :width="480" :destroy-on-close="true" @ok="onSubmitSignup">
		<a-form ref="signupFormRef" :model="signupFormData" layout="vertical">
			<a-form-item label="课包" name="coursePackageId" :rules="[{ required: true, message: '请选择课包' }]">
				<a-select v-model:value="signupFormData.coursePackageId" placeholder="请选择课包" :options="packageOptions" show-search option-filter-prop="label" />
			</a-form-item>
			<a-form-item label="支付渠道" name="payChannel">
				<a-radio-group v-model:value="signupFormData.payChannel" :options="[{ value: 'WECHAT', label: '微信' }, { value: 'OFFLINE', label: '线下' }]" />
			</a-form-item>
			<div style="color: rgba(0,0,0,.45); font-size: 12px">下单后立即登记收款，确认即到账入账；也可稍后在「收款台」处理</div>
		</a-form>
	</a-modal>

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
	import { useRoute, useRouter } from 'vue-router'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import { message } from 'ant-design-vue'
	const router = useRouter()
	const route = useRoute()
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const signupOpen = ref(false)
	const signupFormRef = ref()
	const signupFormData = ref({})
	const packageOptions = ref([])
	const packageRaw = ref([])
	const payOpen = ref(false)
	const payFormRef = ref()
	const payFormData = ref({})
	const payChannelOptions = [
		{ value: 'OFFLINE', label: '线下转账' },
		{ value: 'CASH', label: '现金' }
	]
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
	// 支持从报表带参下钻（状态）
	const applyDrillParams = () => {
		if (route.query.status) {
			searchFormState.value.status = route.query.status
		}
	}
	applyDrillParams()
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
	const loadPackages = () => {
		courseApi.packagePage({ current: 1, size: 100 }).then((data) => {
			packageRaw.value = data.records || []
			packageOptions.value = packageRaw.value.map((item) => ({
				value: item.id,
				label: `${item.name}（${item.lessons}课时 ${item.price}元）`
			}))
		})
	}
	const onOpenSignup = (record) => {
		signupFormData.value = { studentId: record.id, studentName: record.name, coursePackageId: undefined, payChannel: 'WECHAT' }
		signupOpen.value = true
	}
	const onSubmitSignup = () => {
		signupFormRef.value.validate().then(() => {
			studentApi
				.orderAdd({
					studentId: signupFormData.value.studentId,
					coursePackageId: signupFormData.value.coursePackageId,
					payChannel: signupFormData.value.payChannel
				})
				.then((res) => {
					const orderId = res?.data
					signupOpen.value = false
					message.success('下单成功，请登记收款')
					// 下单 → 收款 一步完成：直接打开收款弹窗
					openPayModal({
						id: orderId,
						studentName: signupFormData.value.studentName,
						packageName: packageLabel(signupFormData.value.coursePackageId),
						amount: packagePrice(signupFormData.value.coursePackageId)
					})
					tableRef.value.refresh()
				})
		})
	}
	const packageLabel = (id) => {
		const item = packageOptions.value.find((p) => p.value === id)
		return item ? item.label : ''
	}
	const packagePrice = (id) => {
		const item = packageRaw.value.find((p) => p.id === id)
		return item ? item.price : 0
	}
	const openPayModal = (order) => {
		payFormData.value = { ...order, payChannel: 'OFFLINE', paidAmount: order.amount, remark: '' }
		payOpen.value = true
	}
	const onSubmitPay = () => {
		payFormRef.value.validate().then(() => {
			studentApi
				.orderOfflinePay({
					orderId: payFormData.value.id,
					paidAmount: payFormData.value.paidAmount,
					payChannel: payFormData.value.payChannel,
					remark: payFormData.value.remark
				})
				.then(() => {
					payOpen.value = false
					message.success('收款已登记，课时已入账')
					tableRef.value.refresh()
				})
		})
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
	loadPackages()
</script>
