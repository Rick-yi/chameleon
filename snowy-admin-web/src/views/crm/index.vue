<template>
	<div>
		<page-header title="招生线索" desc="线索录入、跟进记录、试听预约与转学员">
			<template #extra>
				<a-button type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增线索
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
				<a-form-item label="来源">
					<a-select v-model:value="searchFormState.source" placeholder="全部来源" :options="sourceOptions" allow-clear style="width: 140px" />
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
		<a-alert
			v-if="searchFormState.excludeStatus"
			type="warning"
			show-icon
			style="margin-bottom: 12px"
			message="当前显示「待跟进」线索：下次跟进时间已到或已过期，且未报名/未流失"
			closable
			@close="reset"
		/>
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:scroll="{ x: 'max-content' }"
		>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'intentCourseId'">
					{{ courseName(record.intentCourseId) }}
				</template>
				<template v-if="column.dataIndex === 'source'">
					<a-tag :color="sourceColor(record.source)">{{ sourceText(record.source) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'nextFollowTime'">
					<span :style="overdueStyle(record.nextFollowTime)">{{ record.nextFollowTime ? record.nextFollowTime.slice(0, 16).replace('T', ' ') : '-' }}</span>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="onOpenFollow(record)">跟进</a>
						<a-divider type="vertical" />
						<table-more :actions="leadMoreActions" @select="(action) => onLeadMore(action, record)" />
					</a-space>
				</template>
			</template>
		</s-table>
		</div>
	</div>

	<!-- 线索表单 -->
	<a-modal v-model:open="open" :title="formData.id ? '编辑线索' : '新增线索'" :width="600" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="学员姓名" name="name" :rules="[{ required: true, message: '请输入学员姓名' }]">
						<a-input v-model:value="formData.name" placeholder="请输入学员姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="家长手机" name="parentPhone" :rules="[{ required: true, message: '请输入家长手机' }]">
						<a-input v-model:value="formData.parentPhone" placeholder="请输入家长手机" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="意向课程" name="intentCourseId">
						<a-select v-model:value="formData.intentCourseId" placeholder="请选择意向课程" :options="courseOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="来源" name="source">
						<a-select v-model:value="formData.source" placeholder="请选择来源" :options="sourceOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12" v-if="formData.id">
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

	<!-- 跟进 -->
	<a-modal v-model:open="followOpen" :title="`跟进记录 · ${currentLead.name || ''}`" :width="760" :destroy-on-close="true" :footer="null">
		<a-table :columns="followColumns" :data-source="followList" :row-key="(r) => r.id" :pagination="false" size="small" bordered style="margin-bottom: 16px">
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'followType'">{{ followTypeText(record.followType) }}</template>
			</template>
		</a-table>
		<a-divider style="margin: 8px 0 16px">新增跟进</a-divider>
		<a-form ref="followFormRef" :model="followFormData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="跟进方式" name="followType">
						<a-select v-model:value="followFormData.followType" placeholder="请选择" :options="followTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="下次跟进时间" name="nextFollowTime">
						<a-date-picker v-model:value="followFormData.nextFollowTime" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="跟进内容" name="content" :rules="[{ required: true, message: '请输入跟进内容' }]">
						<a-textarea v-model:value="followFormData.content" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="沟通内容、家长反馈、下一步计划" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="更新线索状态" name="status">
						<a-select v-model:value="followFormData.status" placeholder="默认置为已跟进" :options="statusOptions" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>
			<div style="display: flex; justify-content: flex-end">
				<a-button type="primary" @click="onSubmitFollow">保存跟进</a-button>
			</div>
		</a-form>
	</a-modal>

	<!-- 试听预约 -->
	<a-modal v-model:open="trialOpen" :title="`预约试听 · ${currentLead.name || ''}`" :width="480" :destroy-on-close="true" @ok="onSubmitTrial">
		<a-form ref="trialFormRef" :model="trialFormData" layout="vertical">
			<a-form-item label="试听课程" name="courseId">
				<a-select v-model:value="trialFormData.courseId" placeholder="请选择试听课程" :options="courseOptions" allow-clear />
			</a-form-item>
			<a-form-item label="试听时间" name="trialTime" :rules="[{ required: true, message: '请选择试听时间' }]">
				<a-date-picker v-model:value="trialFormData.trialTime" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
			</a-form-item>
			<a-form-item label="备注" name="remark">
				<a-input v-model:value="trialFormData.remark" placeholder="请输入备注" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 转学员 -->
	<a-modal v-model:open="convertOpen" :title="`转学员 · ${currentLead.name || ''}`" :width="480" :destroy-on-close="true" @ok="onSubmitConvert">
		<a-form ref="convertFormRef" :model="convertFormData" layout="vertical">
			<a-form-item label="学员姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
				<a-input v-model:value="convertFormData.name" />
			</a-form-item>
			<a-form-item label="家长手机" name="parentPhone" :rules="[{ required: true, message: '请输入家长手机' }]">
				<a-input v-model:value="convertFormData.parentPhone" />
			</a-form-item>
			<a-form-item label="家长姓名" name="parentName">
				<a-input v-model:value="convertFormData.parentName" allow-clear />
			</a-form-item>
			<a-form-item label="报名课包">
				<a-select
					v-model:value="convertFormData.coursePackageId"
					placeholder="可选：选后立即开报名单并登记收款"
					:options="packageOptions"
					allow-clear
					show-search
					option-filter-prop="label"
				/>
			</a-form-item>
			<div style="color: rgba(0,0,0,.45); font-size: 12px">
				转入后自动创建学员档案并将线索置为「已报名」；选了课包会直接开单并弹出收款登记
			</div>
		</a-form>
	</a-modal>

	<pay-modal ref="payModalRef" :order="payOrder" @success="tableRef.refresh()" />
</template>

<script setup name="crmLead">
	import { useRoute } from 'vue-router'
	import crmApi from '@/api/crm/crmApi'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const searchFormState = ref({})
	const route = useRoute()
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const courseOptions = ref([])
	const currentLead = ref({})
	const followOpen = ref(false)
	const followFormRef = ref()
	const followFormData = ref({})
	const followList = ref([])
	const trialOpen = ref(false)
	const trialFormRef = ref()
	const trialFormData = ref({})
	const convertOpen = ref(false)
	const convertFormRef = ref()
	const convertFormData = ref({})
	const packageOptions = ref([])
	const packageRaw = ref([])
	const payModalRef = ref()
	const payOrder = ref({})
	const statusOptions = [
		{ value: 'NEW', label: '新线索' },
		{ value: 'FOLLOWED', label: '已跟进' },
		{ value: 'TRIALED', label: '已试听' },
		{ value: 'ENROLLED', label: '已报名' },
		{ value: 'LOST', label: '已流失' }
	]
	const sourceOptions = [
		{ value: 'GROUND', label: '地推' },
		{ value: 'REFERRAL', label: '转介绍' },
		{ value: 'ONLINE', label: '线上' },
		{ value: 'VISIT', label: '自然到访' }
	]
	const followTypeOptions = [
		{ value: 'PHONE', label: '电话' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'VISIT', label: '到店' }
	]
	const columns = [
		{ title: '学员姓名', dataIndex: 'name' },
		{ title: '家长手机', dataIndex: 'parentPhone' },
		{ title: '意向课程', dataIndex: 'intentCourseId' },
		{ title: '来源', dataIndex: 'source' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '跟进人', dataIndex: 'followUserName' },
		{ title: '下次跟进', dataIndex: 'nextFollowTime' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const followColumns = [
		{ title: '时间', dataIndex: 'createTime', width: 160 },
		{ title: '方式', dataIndex: 'followType', width: 80 },
		{ title: '内容', dataIndex: 'content' },
		{ title: '下次跟进', dataIndex: 'nextFollowTime', width: 160 }
	]
	const loadData = (parameter) => {
		return crmApi.leadPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	// 支持从报表/工作台带参下钻（状态 / 来源 / 待跟进）
	const applyDrillParams = () => {
		if (route.query.status) {
			searchFormState.value.status = route.query.status
		}
		if (route.query.source) {
			searchFormState.value.source = route.query.source
		}
		if (route.query.todo === '1') {
			// 待跟进：今天到期及以前需要再次跟进的线索
			const now = new Date()
			const pad = (n) => String(n).padStart(2, '0')
			searchFormState.value.nextFollowBefore = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} 23:59:59`
			searchFormState.value.excludeStatus = 'ENROLLED,LOST'
		}
	}
	applyDrillParams()
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
	const statusText = (s) => (statusOptions.find((i) => i.value === s) || {}).label || s
	const statusColor = (s) => ({ NEW: 'blue', FOLLOWED: 'cyan', TRIALED: 'orange', ENROLLED: 'green', LOST: 'red' }[s] || 'default')
	const sourceText = (s) => (sourceOptions.find((i) => i.value === s) || {}).label || s
	const sourceColor = (s) => ({ GROUND: 'blue', REFERRAL: 'green', ONLINE: 'purple', VISIT: 'default' }[s] || 'default')
	const followTypeText = (t) => (followTypeOptions.find((i) => i.value === t) || {}).label || t
	// 下次跟进已过期：标红提示今天必须跟进
	const overdueStyle = (nextFollowTime) => {
		if (!nextFollowTime) return {}
		const due = new Date(String(nextFollowTime).replace(/-/g, '/'))
		return due.getTime() < Date.now() ? { color: '#ff4d4f', fontWeight: 600 } : {}
	}

	const onOpenAdd = () => {
		formData.value = { source: 'GROUND' }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? crmApi.leadEdit : crmApi.leadAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		crmApi.leadDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
	}
	// 操作列：次要操作收进「更多」下拉
	const leadMoreActions = [
		{ key: 'trial', label: '安排试听' },
		{ key: 'convert', label: '转为学员' },
		{ key: 'edit', label: '编辑' },
		{ key: 'delete', label: '删除', danger: true, confirmTitle: '确定删除该线索吗？', confirmContent: '删除后线索及其跟进记录将不再展示。' }
	]
	const onLeadMore = (action, record) => {
		if (action.key === 'trial') {
			onOpenTrial(record)
		} else if (action.key === 'convert') {
			onOpenConvert(record)
		} else if (action.key === 'edit') {
			onOpenEdit(record)
		} else if (action.key === 'delete') {
			onDelete(record)
		}
	}
	const onOpenFollow = (record) => {
		currentLead.value = record
		followFormData.value = { leadId: record.id, followType: 'PHONE' }
		followOpen.value = true
		crmApi.followPage({ current: 1, size: 50, leadId: record.id }).then((data) => {
			followList.value = data.records || []
		})
	}
	const onSubmitFollow = () => {
		followFormRef.value.validate().then(() => {
			crmApi.followAdd(followFormData.value).then(() => {
				followOpen.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onOpenTrial = (record) => {
		currentLead.value = record
		trialFormData.value = { leadId: record.id, courseId: record.intentCourseId }
		trialOpen.value = true
	}
	const onSubmitTrial = () => {
		trialFormRef.value.validate().then(() => {
			crmApi.trialAdd(trialFormData.value).then(() => {
				trialOpen.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onOpenConvert = (record) => {
		currentLead.value = record
		convertFormData.value = { name: record.name, parentPhone: record.parentPhone, coursePackageId: undefined }
		convertOpen.value = true
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
	const onSubmitConvert = () => {
		convertFormRef.value.validate().then(() => {
			const { coursePackageId, ...studentInfo } = convertFormData.value
			studentApi.studentAdd({ ...studentInfo, gender: '男', status: 'STUDYING' }).then((res) => {
				const studentId = res?.data
				const afterLead = () =>
					crmApi.leadEdit({ ...currentLead.value, status: 'ENROLLED' }).then(() => {
						convertOpen.value = false
						tableRef.value.refresh()
					})
				if (!coursePackageId) {
					afterLead()
					return
				}
				// 转学员 → 直接开报名单 → 弹出收款登记（一步完成）
				studentApi
					.orderAdd({ studentId, coursePackageId, payChannel: 'OFFLINE' })
					.then((orderRes) => {
						const pkg = packageRaw.value.find((p) => p.id === coursePackageId)
						afterLead()
						payOrder.value = {
							id: orderRes?.data,
							studentName: studentInfo.name,
							packageName: pkg?.name || '',
							amount: pkg?.price || 0
						}
						payModalRef.value.open(payOrder.value)
					})
					.catch(() => {
						afterLead()
					})
			})
		})
	}
	loadCourses()
	loadPackages()
</script>
