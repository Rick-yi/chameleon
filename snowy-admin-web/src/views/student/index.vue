<template>
	<div class="student-page">
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
						<a-button type="primary" @click="onSearch">
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
			v-if="!isMobile"
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
						<a v-if="hasPerm('/student/order/add')" @click="openEnrollment(record)">报名</a>
						<a-divider type="vertical" />
						<table-more :actions="studentMoreActions" @select="(action) => onStudentMore(action, record)" />
					</a-space>
				</template>
			</template>
		</s-table>
			<div v-else class="student-mobile">
				<a-spin :spinning="mobileLoading">
					<div v-if="!mobileStudents.length" class="ent-empty">
						<div>没有符合条件的学员</div>
						<a-button v-if="hasPerm('/student/add')" type="primary" style="margin-top: 10px" @click="onOpenAdd">新增学员</a-button>
					</div>
					<div v-for="item in mobileStudents" :key="item.id" class="student-card">
						<div class="student-card-head">
							<div class="student-card-name">
								{{ item.name }}
								<a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
							</div>
							<div class="student-card-remain" :class="lessonTone(item)">
								{{ lessonText(item) }}<span v-if="item.hasLessonAccount !== false">课时</span>
							</div>
						</div>
						<div class="student-card-line">
							<span>{{ item.parentName || '家长' }} · {{ item.parentPhone || '-' }}</span>
							<a v-if="item.parentPhone" :href="'tel:' + item.parentPhone">拨号</a>
						</div>
						<div class="student-card-grid">
							<div>
								<span>课包到期</span>
								<b :class="{ 'text-danger': isExpired(item.accountExpireTime) }">{{ formatDate(item.accountExpireTime) }}</b>
							</div>
							<div>
								<span>所在班级</span>
								<b>{{ item.classNames || '-' }}</b>
							</div>
							<div>
								<span>待收款</span>
								<b :class="{ 'text-warning': Number(item.unpaidAmount) > 0 }">{{ formatMoney(item.unpaidAmount) }}</b>
							</div>
						</div>
						<div class="student-card-actions">
							<a-button size="small" @click="openDetail(item)">详情</a-button>
							<a-button v-if="hasPerm('/student/order/add')" size="small" @click="openEnrollment(item)">报名</a-button>
							<table-more :actions="studentMoreActions" @select="(action) => onStudentMore(action, item)" />
						</div>
					</div>
					<div v-if="mobileTotal > mobilePage.size" class="student-mobile-pager">
						<a-pagination
							simple
							:current="mobilePage.current"
							:page-size="mobilePage.size"
							:total="mobileTotal"
							@change="onMobilePageChange"
						/>
					</div>
				</a-spin>
			</div>
		</div>
	</div>

	<enrollment-flow v-model:open="enrollOpen" :student="enrollStudent" @success="refreshList" />

	<!-- 学员详情：抽屉内展示，保留列表上下文，避免独立页面来回切换 -->
	<a-drawer v-model:open="detailOpen" :width="880" :title="detailStudentName || '学员详情'" destroy-on-close>
		<student-detail v-if="detailOpen" :student-id="detailStudentId" />
	</a-drawer>

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
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'
	import studentApi from '@/api/student/studentApi'
	import { globalStore } from '@/store'
	const router = useRouter()
	const route = useRoute()
	const store = globalStore()
	const isMobile = computed(() => store.isMobile)
	const searchFormState = ref({})
	const tableRef = ref()
	const mobileStudents = ref([])
	const mobileTotal = ref(0)
	const mobilePage = ref({ current: 1, size: 10 })
	const mobileLoading = ref(false)
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const enrollOpen = ref(false)
	const enrollStudent = ref(null)
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
	// 手机端卡片列表：与桌面表格共用筛选条件，独立分页
	const loadMobile = (page = 1) => {
		mobileLoading.value = true
		const parameter = { current: page, size: mobilePage.value.size, ...searchFormState.value }
		return studentApi
			.studentPage(parameter)
			.then((data) => {
				mobileStudents.value = data.records || []
				mobileTotal.value = data.total || 0
				mobilePage.value.current = page
			})
			.catch(() => {
				mobileStudents.value = []
				mobileTotal.value = 0
			})
			.finally(() => {
				mobileLoading.value = false
			})
	}
	const onSearch = () => {
		if (isMobile.value) {
			loadMobile(1)
		} else {
			tableRef.value?.refresh(true)
		}
	}
	const refreshList = () => {
		if (isMobile.value) {
			return loadMobile(mobilePage.value.current)
		}
		tableRef.value?.refresh()
	}
	const onMobilePageChange = (page) => loadMobile(page)
	watch(
		isMobile,
		(value) => {
			if (value) loadMobile(1)
		},
		{ immediate: true }
	)
	// 支持从报表带参下钻（状态）
	const applyDrillParams = () => {
		if (route.query.status) {
			searchFormState.value.status = route.query.status
		}
	}
	applyDrillParams()
	const reset = () => {
		searchFormState.value = {}
		onSearch()
	}
	const statusText = (status) => {
		const item = statusOptions.find((i) => i.value === status)
		return item ? item.label : status
	}
	const statusColor = (status) => {
		return status === 'STUDYING' ? 'green' : status === 'STOP' ? 'orange' : 'red'
	}
	const openEnrollment = (record) => {
		enrollStudent.value = record
		enrollOpen.value = true
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
				// 新增学员回到第一页，确保能看到刚建档的记录
				if (!edit) mobilePage.value.current = 1
				refreshList()
			})
		})
	}
	const onDelete = (record) => {
		studentApi.studentDelete([{ id: record.id }]).then(() => refreshList())
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
</script>

<style scoped lang="less">
	.text-danger {
		color: #cf1322;
		font-weight: 600;
	}
	.text-warning {
		color: var(--accent-text);
		font-weight: 600;
	}
	.text-muted {
		color: var(--text-color-secondary);
	}
	.student-mobile {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	.student-card {
		padding: 12px;
		background: #fff;
		border: 1px solid var(--border-color-split);
		border-radius: var(--radius-card);
	}
	.student-card-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 8px;
	}
	.student-card-name {
		display: flex;
		align-items: center;
		gap: 6px;
		min-width: 0;
		font-size: 15px;
		font-weight: 600;
	}
	.student-card-remain {
		font-size: 22px;
		font-weight: 700;
		line-height: 1;
		white-space: nowrap;
	}
	.student-card-remain span {
		margin-left: 2px;
		font-size: 11px;
		font-weight: 400;
		color: var(--text-color-secondary);
	}
	.student-card-line {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		margin-top: 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.student-card-line a {
		font-weight: 500;
	}
	.student-card-grid {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 8px;
		margin-top: 10px;
		padding: 10px 0;
		border-top: 1px dashed var(--border-color-split);
		border-bottom: 1px dashed var(--border-color-split);
	}
	.student-card-grid > div {
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
	}
	.student-card-grid span {
		font-size: 11px;
		color: var(--text-color-secondary);
	}
	.student-card-grid b {
		font-size: 13px;
		font-weight: 600;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
	.student-card-actions {
		display: flex;
		align-items: center;
		gap: 8px;
		margin-top: 10px;
	}
	.student-card-actions :deep(.ant-btn) {
		flex: 1;
		height: 40px;
	}
	.student-card-actions :deep(.table-more) {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		height: 40px;
		padding: 0 12px;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
	}
	.student-mobile-pager {
		display: flex;
		justify-content: center;
		padding: 12px 0 4px;
	}
	@media (max-width: 576px) {
		.student-page :deep(.ent-toolbar .ant-input),
		.student-page :deep(.ent-toolbar .ant-select-selection-search-input),
		.student-page :deep(.ent-toolbar .ant-select-selection-item) {
			font-size: 16px;
		}
		.student-page :deep(.ent-toolbar .ant-btn),
		.student-page :deep(.page-header .ant-btn) {
			height: 44px;
		}
		.student-card-actions :deep(.ant-btn),
		.student-card-actions :deep(.table-more) {
			height: 44px;
		}
	}
</style>
