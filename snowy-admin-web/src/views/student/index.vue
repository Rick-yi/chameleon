<template>
	<div>
		<page-header title="学员管理" desc="学员档案、课时账户与报名入口">
			<template #extra>
				<a-button v-if="hasPerm('/student/add')" type="primary" @click="onOpenAdd">
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
				<template v-if="column.dataIndex === 'remainLessons'">
					<span :class="lessonTone(record)">{{ lessonText(record) }}</span>
				</template>
				<template v-if="column.dataIndex === 'accountExpireTime'">
					<span :class="{ 'text-danger': isExpired(record.accountExpireTime) }">{{ formatDate(record.accountExpireTime) }}</span>
				</template>
				<template v-if="column.dataIndex === 'classNames'">
					{{ record.classNames || '-' }}
				</template>
				<template v-if="column.dataIndex === 'unpaidAmount'">
					<span :class="{ 'text-warning': Number(record.unpaidAmount) > 0 }">{{ formatMoney(record.unpaidAmount) }}</span>
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="openDetail(record)">详情</a>
						<a-divider type="vertical" />
						<a v-if="hasPerm('/student/order/add')" @click="onOpenSignup(record)">报名</a>
						<a-divider type="vertical" />
						<table-more :actions="studentMoreActions" @select="(action) => onStudentMore(action, record)" />
					</a-space>
				</template>
			</template>
		</s-table>
		</div>
	</div>

	<pay-modal ref="payModalRef" :order="payOrder" @success="tableRef.refresh()" />

	<!-- 学员详情：抽屉内展示，保留列表上下文，避免独立页面来回切换 -->
	<a-drawer v-model:open="detailOpen" :width="880" :title="detailStudentName || '学员详情'" destroy-on-close>
		<student-detail v-if="detailOpen" :student-id="detailStudentId" />
	</a-drawer>

	<a-modal v-model:open="signupOpen" :title="`报名购课 · ${signupFormData.studentName || ''}`" :width="480" :destroy-on-close="true" @ok="onSubmitSignup">
		<a-form ref="signupFormRef" :model="signupFormData" layout="vertical">
			<a-form-item label="课包" name="coursePackageId" :rules="[{ required: true, message: '请选择课包' }]">
				<a-select v-model:value="signupFormData.coursePackageId" placeholder="请选择课包" :options="packageOptions" show-search option-filter-prop="label" />
			</a-form-item>
			<a-form-item label="支付渠道" name="payChannel">
				<a-radio-group v-model:value="signupFormData.payChannel" :options="[{ value: 'WECHAT', label: '微信' }, { value: 'ALIPAY', label: '支付宝' }, { value: 'CASH', label: '现金' }, { value: 'OFFLINE', label: '线下转账' }]" />
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
	import StudentDetail from './detail.vue'
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
	const payModalRef = ref()
	const payOrder = ref({})
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
		{ title: '剩余课时', dataIndex: 'remainLessons', width: 110, align: 'right' },
		{ title: '课包到期', dataIndex: 'accountExpireTime', width: 120 },
		{ title: '所在班级', dataIndex: 'classNames', width: 160 },
		{ title: '待收款', dataIndex: 'unpaidAmount', width: 110, align: 'right' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '创建时间', dataIndex: 'createTime' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const formatLessons = (value) => {
		const number = Number(value || 0)
		return Number.isInteger(number) ? String(number) : number.toFixed(2)
	}
	const lessonText = (record) => (record.hasLessonAccount === false ? '未报名' : formatLessons(record.remainLessons))
	const lessonTone = (record) => {
		if (record.hasLessonAccount === false) return 'text-muted'
		const number = Number(record.remainLessons || 0)
		if (number <= 0) return 'text-danger'
		if (number < 3) return 'text-warning'
		return ''
	}
	const formatDate = (value) => (value ? String(value).slice(0, 10) : '-')
	const isExpired = (value) => {
		if (!value) return false
		const date = new Date(String(value).replace(/-/g, '/'))
		return !Number.isNaN(date.getTime()) && date < new Date()
	}
	const formatMoney = (value) => `¥${Number(value || 0).toFixed(2)}`
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
					const orderId = res
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
		payOrder.value = order
		payModalRef.value.open(order)
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
	// 操作列：次要操作收进「更多」下拉
	const studentMoreActions = [
		{ key: 'edit', label: '编辑' },
		{ key: 'delete', label: '删除', danger: true, confirmTitle: '确定删除该学员吗？', confirmContent: '删除后该学员不再出现在列表中。' }
	]
	const onStudentMore = (action, record) => {
		if (action.key === 'edit') {
			onOpenEdit(record)
		} else if (action.key === 'delete') {
			onDelete(record)
		}
	}
	// 详情抽屉
	const detailOpen = ref(false)
	const detailStudentId = ref('')
	const detailStudentName = ref('')
	const openDetail = (record) => {
		detailStudentId.value = record.id
		detailStudentName.value = record.name
		detailOpen.value = true
	}
	// 支持从其它页面带 ?id= 直接打开详情抽屉
	if (route.query.id) {
		detailStudentId.value = String(route.query.id)
		detailOpen.value = true
	}
	loadPackages()
</script>

<style scoped lang="less">
	.text-danger {
		color: #cf1322;
		font-weight: 600;
	}
	.text-warning {
		color: var(--accent-6);
		font-weight: 600;
	}
	.text-muted {
		color: var(--text-color-secondary);
	}
</style>
