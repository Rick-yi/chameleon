<template>
	<div>
		<page-header title="招生线索" desc="线索录入、跟进记录、试听预约与转学员">
			<template #extra>
				<a-button v-if="viewMode === 'lead' && hasPerm('/crm/lead/add')" type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增线索
				</a-button>
				<a-button v-else-if="viewMode === 'trial'" @click="trialTableRef?.refresh(true)">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
			</template>
		</page-header>

		<!-- 线索转化概览：复用招生报表接口，无成本字段所以只展示转化率 -->
		<a-card v-if="viewMode === 'lead' && showOverview" :bordered="false" size="small" class="crm-overview">
			<a-row :gutter="16">
				<a-col v-if="hasPerm('/crm/report/funnel')" :xs="24" :md="14">
					<div class="crm-overview-title">
						<span>线索转化漏斗</span>
						<a @click="goReport">查看报表 ›</a>
					</div>
					<div class="crm-funnel">
						<div v-for="item in funnelList" :key="item.stage" class="crm-funnel-item">
							<div class="crm-funnel-label">{{ item.stageName }}</div>
							<div class="crm-funnel-value">{{ item.count }}</div>
							<div class="crm-funnel-rate">转化 {{ Number(item.conversionRate || 0).toFixed(1) }}%</div>
						</div>
					</div>
				</a-col>
				<a-col v-if="hasPerm('/crm/report/source')" :xs="24" :md="10">
					<div class="crm-overview-title">
						<span>来源转化</span>
						<span class="crm-overview-hint">暂无渠道成本，不含 ROI</span>
					</div>
					<a-table
						:columns="sourceColumns"
						:data-source="sourceList"
						:row-key="(r) => r.source"
						:pagination="false"
						size="small"
						bordered
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'conversionRate'">
								{{ Number(record.conversionRate || 0).toFixed(1) }}%
							</template>
						</template>
					</a-table>
				</a-col>
			</a-row>
		</a-card>

		<div class="ent-toolbar">
			<a-radio-group v-model:value="viewMode" button-style="solid" @change="onViewChange">
				<a-radio-button value="lead">线索</a-radio-button>
				<a-radio-button value="trial">试听</a-radio-button>
			</a-radio-group>

			<template v-if="viewMode === 'lead'">
				<div class="crm-quick-filter">
					<a-radio-group v-model:value="todoFilter" button-style="solid" @change="onTodoFilterChange">
						<a-radio-button value="">全部</a-radio-button>
						<a-radio-button value="pending">待跟进</a-radio-button>
						<a-radio-button value="today">今日跟进</a-radio-button>
						<a-radio-button value="overdue">已逾期</a-radio-button>
					</a-radio-group>
				</div>
				<a-form layout="inline" :model="searchFormState">
					<a-form-item label="姓名/手机">
						<a-input
							v-model:value="searchFormState.searchKey"
							placeholder="请输入姓名或家长手机"
							allow-clear
							style="width: 220px"
						/>
					</a-form-item>
					<a-form-item label="状态">
						<a-select
							v-model:value="searchFormState.status"
							placeholder="全部状态"
							:options="statusOptions"
							allow-clear
							style="width: 140px"
						/>
					</a-form-item>
					<a-form-item label="来源">
						<a-select
							v-model:value="searchFormState.source"
							placeholder="全部来源"
							:options="sourceOptions"
							allow-clear
							style="width: 140px"
						/>
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
			</template>

			<template v-else>
				<a-form layout="inline" :model="trialSearchFormState">
					<a-form-item label="试听状态">
						<a-select
							v-model:value="trialSearchFormState.status"
							placeholder="全部状态"
							:options="trialStatusOptions"
							allow-clear
							style="width: 140px"
						/>
					</a-form-item>
					<a-form-item>
						<a-space>
							<a-button type="primary" @click="trialTableRef.refresh(true)">
								<template #icon><SearchOutlined /></template>
								查询
							</a-button>
							<a-button @click="resetTrial">
								<template #icon><redo-outlined /></template>
								重置
							</a-button>
						</a-space>
					</a-form-item>
				</a-form>
			</template>
		</div>
		<div class="ent-body">
			<template v-if="viewMode === 'lead'">
				<a-alert
					v-if="todoFilter"
					type="warning"
					show-icon
					style="margin-bottom: 12px"
					:message="todoFilterMessage"
					closable
					@close="setTodoFilter('')"
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
							<span :style="overdueStyle(record.nextFollowTime)">{{ fmtDateTime(record.nextFollowTime) }}</span>
						</template>
						<template v-if="column.dataIndex === 'action'">
							<a-space>
								<a v-if="hasPerm('/crm/follow/add')" @click="onOpenFollow(record)">跟进</a>
								<a-divider type="vertical" />
								<table-more :actions="leadMoreActions" @select="(action) => onLeadMore(action, record)" />
							</a-space>
						</template>
					</template>
				</s-table>
			</template>

			<s-table
				v-else
				ref="trialTableRef"
				:columns="trialColumns"
				:data="loadTrialData"
				bordered
				:row-key="(record) => record.id"
				:scroll="{ x: 'max-content' }"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'trialTime'">
						{{ fmtDateTime(record.trialTime) }}
					</template>
					<template v-if="column.dataIndex === 'courseId'">
						{{ courseName(record.courseId) }}
					</template>
					<template v-if="column.dataIndex === 'status'">
						<a-tag :color="trialStatusColor(record.status)">{{ trialStatusText(record.status) }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'result'">
						<a-tag :color="trialResultColor(record.result)">{{ trialResultText(record.result) }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'action'">
						<a-space>
							<a @click="onOpenTrialResult(record)">{{ record.status === 'PENDING' ? '记录结果' : '修改结果' }}</a>
							<a-popconfirm
								v-if="record.status === 'PENDING'"
								title="确定取消该试听预约吗？"
								@confirm="onCancelTrial(record)"
							>
								<a>取消</a>
							</a-popconfirm>
							<a v-if="record.leadStatus !== 'ENROLLED'" @click="onOpenConvertFromTrial(record)">转学员</a>
						</a-space>
					</template>
				</template>
			</s-table>
		</div>
	</div>

	<!-- 线索表单 -->
	<a-modal
		v-model:open="open"
		:title="formData.id ? '编辑线索' : '新增线索'"
		:width="600"
		:destroy-on-close="true"
		@ok="onSubmit"
	>
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="学员姓名" name="name" :rules="[{ required: true, message: '请输入学员姓名' }]">
						<a-input v-model:value="formData.name" placeholder="请输入学员姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="家长手机（选填）" name="parentPhone">
						<a-input
							v-model:value="formData.parentPhone"
							placeholder="用于查重，可留空"
							allow-clear
							@blur="checkDuplicatePhone"
						/>
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="意向课程" name="intentCourseId">
						<a-select
							v-model:value="formData.intentCourseId"
							placeholder="请选择意向课程"
							:options="courseOptions"
							allow-clear
						/>
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
			<a-alert v-if="duplicateLeads.length && !duplicateDismissed" type="warning" show-icon style="margin-bottom: 12px">
				<template #message>该手机号已存在线索</template>
				<template #description>
					<div v-for="dup in duplicateLeads" :key="dup.id">
						已存在：{{ dup.name || '-' }} / {{ statusText(dup.status) }} / {{ dup.followUserName || '未分配' }}
					</div>
				</template>
				<template #action>
					<a-space direction="vertical">
						<a-button v-if="!formData.id" size="small" @click="duplicateDismissed = true">继续新建</a-button>
						<a-button size="small" type="primary" @click="onViewDuplicate(duplicateLeads[0].parentPhone)"
							>查看已有线索</a-button
						>
					</a-space>
				</template>
			</a-alert>
		</a-form>
	</a-modal>

	<!-- 跟进 -->
	<a-modal
		v-model:open="followOpen"
		:title="`跟进记录 · ${currentLead.name || ''}`"
		:width="820"
		:destroy-on-close="true"
		:footer="null"
	>
		<div class="crm-follow-filter">
			<a-radio-group v-model:value="followFilterType" size="small" button-style="solid" @change="onFollowFilterChange">
				<a-radio-button value="">全部</a-radio-button>
				<a-radio-button value="PHONE">电话</a-radio-button>
				<a-radio-button value="WECHAT">微信</a-radio-button>
				<a-radio-button value="VISIT">到店</a-radio-button>
			</a-radio-group>
			<span class="crm-follow-total">共 {{ followPagination.total }} 条</span>
		</div>
		<a-table
			:columns="followColumns"
			:data-source="followList"
			:row-key="(r) => r.id"
			:pagination="followPagination"
			size="small"
			bordered
			style="margin-bottom: 16px"
			@change="onFollowTableChange"
		>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'followType'">{{ followTypeText(record.followType) }}</template>
				<template v-if="column.dataIndex === 'createTime'">{{ fmtDateTime(record.createTime) }}</template>
				<template v-if="column.dataIndex === 'nextFollowTime'">{{ fmtDateTime(record.nextFollowTime) }}</template>
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
						<a-date-picker
							v-model:value="followFormData.nextFollowTime"
							show-time
							value-format="YYYY-MM-DD HH:mm:ss"
							style="width: 100%"
						/>
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="跟进内容" name="content" :rules="[{ required: true, message: '请输入跟进内容' }]">
						<a-textarea
							v-model:value="followFormData.content"
							:auto-size="{ minRows: 2, maxRows: 4 }"
							placeholder="沟通内容、家长反馈、下一步计划"
						/>
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="更新线索状态" name="status">
						<a-select
							v-model:value="followFormData.status"
							placeholder="默认置为已跟进"
							:options="statusOptions"
							allow-clear
						/>
					</a-form-item>
				</a-col>
			</a-row>
			<div style="display: flex; justify-content: flex-end">
				<a-button type="primary" @click="onSubmitFollow">保存跟进</a-button>
			</div>
		</a-form>
	</a-modal>

	<!-- 试听预约 -->
	<a-modal
		v-model:open="trialOpen"
		:title="`预约试听 · ${currentLead.name || ''}`"
		:width="480"
		:destroy-on-close="true"
		@ok="onSubmitTrial"
	>
		<a-form ref="trialFormRef" :model="trialFormData" layout="vertical">
			<a-form-item label="试听课程" name="courseId">
				<a-select
					v-model:value="trialFormData.courseId"
					placeholder="请选择试听课程"
					:options="courseOptions"
					allow-clear
				/>
			</a-form-item>
			<a-form-item label="试听时间" name="trialTime" :rules="[{ required: true, message: '请选择试听时间' }]">
				<a-date-picker
					v-model:value="trialFormData.trialTime"
					show-time
					value-format="YYYY-MM-DD HH:mm:ss"
					style="width: 100%"
				/>
			</a-form-item>
			<a-form-item label="备注" name="remark">
				<a-input v-model:value="trialFormData.remark" placeholder="请输入备注" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 试听结果 -->
	<a-modal
		v-model:open="trialResultOpen"
		:title="`试听结果 · ${trialResultLeadName || ''}`"
		:width="480"
		:destroy-on-close="true"
		@ok="onSubmitTrialResult"
	>
		<a-form ref="trialResultFormRef" :model="trialResultFormData" layout="vertical">
			<a-form-item label="试听状态">
				<a-select v-model:value="trialResultFormData.status" :options="trialStatusOptions" />
			</a-form-item>
			<a-form-item label="试听结果">
				<a-select v-model:value="trialResultFormData.result" :options="trialResultOptions" />
			</a-form-item>
			<a-form-item label="备注">
				<a-input v-model:value="trialResultFormData.remark" placeholder="如：家长对课程不满意" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 转学员 -->
	<a-modal
		v-model:open="convertOpen"
		:title="`转学员 · ${currentLead.name || ''}`"
		:width="480"
		:destroy-on-close="true"
		@ok="onSubmitConvert"
	>
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
			<div style="color: rgba(0, 0, 0, 0.45); font-size: 12px">
				转入后自动创建学员档案并将线索置为「已报名」；选了课包会直接开单并弹出收款登记
			</div>
		</a-form>
	</a-modal>

	<pay-modal ref="payModalRef" :order="payOrder" @success="tableRef.refresh()" />
</template>

<script setup name="crmLead">
	import { useRoute, useRouter } from 'vue-router'
	import { message } from 'ant-design-vue'
	import crmApi from '@/api/crm/crmApi'
	import reportApi from '@/api/report/reportApi'
	import { hasPerm } from '@/utils/permission/index'
	import { CRM_SOURCE_OPTIONS, crmSourceText, crmSourceColor } from '@/utils/crmSource'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const searchFormState = ref({})
	const trialSearchFormState = ref({})
	const route = useRoute()
	const router = useRouter()
	const viewMode = ref('lead')
	const todoFilter = ref('')
	const todoFilterMessage = computed(
		() =>
			({
				pending: '当前显示「待跟进」线索：下次跟进时间已到或已过期，且未报名/未流失',
				today: '当前显示「今日跟进」线索：下次跟进时间在今天，且未报名/未流失',
				overdue: '当前显示「已逾期」线索：下次跟进时间早于今天，且未报名/未流失'
			})[todoFilter.value] || ''
	)
	const tableRef = ref()
	const trialTableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const courseOptions = ref([])
	const currentLead = ref({})
	const duplicateLeads = ref([])
	const duplicateDismissed = ref(false)
	const followOpen = ref(false)
	const followFormRef = ref()
	const followFormData = ref({})
	const followList = ref([])
	const followFilterType = ref('')
	const followPagination = reactive({
		current: 1,
		pageSize: 10,
		total: 0,
		showSizeChanger: true,
		pageSizeOptions: ['10', '20', '50'],
		showTotal: (total) => `共 ${total} 条`
	})
	const trialOpen = ref(false)
	const trialFormRef = ref()
	const trialFormData = ref({})
	const trialResultOpen = ref(false)
	const trialResultFormRef = ref()
	const trialResultFormData = ref({})
	const trialResultLeadName = ref('')
	const convertOpen = ref(false)
	const convertFormRef = ref()
	const convertFormData = ref({})
	const packageOptions = ref([])
	const packageRaw = ref([])
	const payModalRef = ref()
	const payOrder = ref({})
	const funnelList = ref([])
	const sourceList = ref([])
	const statusOptions = [
		{ value: 'NEW', label: '新线索' },
		{ value: 'FOLLOWED', label: '已跟进' },
		{ value: 'TRIALED', label: '已试听' },
		{ value: 'ENROLLED', label: '已报名' },
		{ value: 'LOST', label: '已流失' }
	]
	const sourceOptions = CRM_SOURCE_OPTIONS
	const followTypeOptions = [
		{ value: 'PHONE', label: '电话' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'VISIT', label: '到店' }
	]
	const trialStatusOptions = [
		{ value: 'PENDING', label: '待试听' },
		{ value: 'DONE', label: '已完成' },
		{ value: 'CANCELED', label: '已取消' }
	]
	const trialResultOptions = [
		{ value: 'PENDING', label: '待定' },
		{ value: 'INTERESTED', label: '有意向' },
		{ value: 'NOT_INTERESTED', label: '无意向' }
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
	const trialColumns = [
		{ title: '试听时间', dataIndex: 'trialTime', width: 150 },
		{ title: '学员', dataIndex: 'leadName', width: 120 },
		{ title: '家长手机', dataIndex: 'parentPhone', width: 130 },
		{ title: '试听课程', dataIndex: 'courseId', width: 150 },
		{ title: '顾问', dataIndex: 'followUserName', width: 100 },
		{ title: '状态', dataIndex: 'status', width: 100 },
		{ title: '结果', dataIndex: 'result', width: 100 },
		{ title: '备注', dataIndex: 'remark' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right', width: 200 }
	]
	const followColumns = [
		{ title: '时间', dataIndex: 'createTime', width: 150 },
		{ title: '方式', dataIndex: 'followType', width: 80 },
		{ title: '内容', dataIndex: 'content' },
		{ title: '下次跟进', dataIndex: 'nextFollowTime', width: 150 }
	]
	const sourceColumns = [
		{ title: '来源', dataIndex: 'sourceName' },
		{ title: '线索数', dataIndex: 'leadCount', width: 80 },
		{ title: '报名数', dataIndex: 'enrolledCount', width: 80 },
		{ title: '转化率', dataIndex: 'conversionRate', width: 90 }
	]
	const loadData = (parameter) => {
		return crmApi.leadPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const loadTrialData = (parameter) => {
		return crmApi.trialPage(Object.assign(parameter, trialSearchFormState.value)).then((data) => data)
	}
	// 支持从报表/工作台带参下钻（状态 / 来源 / 待跟进）
	const pad = (n) => String(n).padStart(2, '0')
	const dateText = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
	const applyTodoFilter = (value) => {
		delete searchFormState.value.nextFollowAfter
		delete searchFormState.value.nextFollowBefore
		delete searchFormState.value.excludeStatus
		if (!value) return
		const now = new Date()
		const todayStart = `${dateText(now)} 00:00:00`
		const todayEnd = `${dateText(now)} 23:59:59`
		const yesterday = new Date(now)
		yesterday.setDate(now.getDate() - 1)
		const yesterdayEnd = `${dateText(yesterday)} 23:59:59`
		searchFormState.value.excludeStatus = 'ENROLLED,LOST'
		if (value === 'pending') {
			searchFormState.value.nextFollowBefore = todayEnd
		} else if (value === 'today') {
			searchFormState.value.nextFollowAfter = todayStart
			searchFormState.value.nextFollowBefore = todayEnd
		} else if (value === 'overdue') {
			searchFormState.value.nextFollowBefore = yesterdayEnd
		}
	}
	const onTodoFilterChange = (event) => {
		const value = event?.target?.value ?? event
		todoFilter.value = value
		applyTodoFilter(value)
		tableRef.value?.refresh(true)
	}
	const setTodoFilter = (value) => {
		todoFilter.value = value
		applyTodoFilter(value)
		tableRef.value?.refresh(true)
	}
	const onViewChange = () => {
		if (viewMode.value === 'trial') {
			nextTick(() => trialTableRef.value?.refresh(true))
		}
	}
	const applyDrillParams = () => {
		if (route.query.status) {
			searchFormState.value.status = route.query.status
		}
		if (route.query.source) {
			searchFormState.value.source = route.query.source
		}
		if (route.query.todo === '1') {
			todoFilter.value = 'pending'
			applyTodoFilter('pending')
		}
	}
	applyDrillParams()
	const reset = () => {
		todoFilter.value = ''
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const resetTrial = () => {
		trialSearchFormState.value = {}
		trialTableRef.value.refresh(true)
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
	const fmtDateTime = (value) => (value ? String(value).slice(0, 16).replace('T', ' ') : '-')
	const statusText = (s) => (statusOptions.find((i) => i.value === s) || {}).label || s
	const statusColor = (s) =>
		({ NEW: 'blue', FOLLOWED: 'cyan', TRIALED: 'orange', ENROLLED: 'green', LOST: 'red' })[s] || 'default'
	const sourceText = crmSourceText
	const sourceColor = crmSourceColor
	const followTypeText = (t) => (followTypeOptions.find((i) => i.value === t) || {}).label || t
	const trialStatusText = (s) => (trialStatusOptions.find((i) => i.value === s) || {}).label || s || '-'
	const trialStatusColor = (s) => ({ PENDING: 'blue', DONE: 'green', CANCELED: 'default' })[s] || 'default'
	const trialResultText = (r) => (trialResultOptions.find((i) => i.value === r) || {}).label || r || '-'
	const trialResultColor = (r) => ({ PENDING: 'default', INTERESTED: 'green', NOT_INTERESTED: 'red' })[r] || 'default'
	// 下次跟进已过期：标红提示今天必须跟进
	const overdueStyle = (nextFollowTime) => {
		if (!nextFollowTime) return {}
		const due = new Date(String(nextFollowTime).replace(/-/g, '/'))
		return due.getTime() < Date.now() ? { color: '#ff4d4f', fontWeight: 600 } : {}
	}

	// 线索转化概览（无渠道成本字段，仅展示漏斗与来源转化率）
	const showOverview = computed(() => hasPerm(['/crm/report/funnel', '/crm/report/source']))
	const goReport = () => router.push({ path: '/report/crm' })
	const loadOverview = () => {
		if (!showOverview.value) return
		if (hasPerm('/crm/report/funnel')) {
			reportApi
				.crmFunnel({})
				.then((data) => (funnelList.value = data || []))
				.catch(() => (funnelList.value = []))
		}
		if (hasPerm('/crm/report/source')) {
			reportApi
				.crmSource({ limit: 10 })
				.then((data) => (sourceList.value = data || []))
				.catch(() => (sourceList.value = []))
		}
	}

	const onOpenAdd = () => {
		formData.value = { source: 'GROUND' }
		duplicateLeads.value = []
		duplicateDismissed.value = false
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		duplicateLeads.value = []
		duplicateDismissed.value = false
		open.value = true
	}
	// 手机号失焦查重：仅提示，不阻断保存；无手机号直接跳过
	const checkDuplicatePhone = () => {
		duplicateDismissed.value = false
		duplicateLeads.value = []
		const phone = String(formData.value.parentPhone || '').trim()
		if (!phone) return
		crmApi
			.leadPage({ current: 1, size: 10, searchKey: phone })
			.then((data) => {
				duplicateLeads.value = (data.records || []).filter(
					(item) => item.parentPhone === phone && item.id !== formData.value.id
				)
			})
			.catch(() => (duplicateLeads.value = []))
	}
	const onViewDuplicate = (phone) => {
		open.value = false
		duplicateLeads.value = []
		todoFilter.value = ''
		viewMode.value = 'lead'
		searchFormState.value = { searchKey: phone }
		nextTick(() => tableRef.value?.refresh(true))
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? crmApi.leadEdit : crmApi.leadAdd
			api(formData.value).then(() => {
				open.value = false
				duplicateLeads.value = []
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
		{
			key: 'delete',
			label: '删除',
			danger: true,
			confirmTitle: '确定删除该线索吗？',
			confirmContent: '删除后线索及其跟进记录将不再展示。'
		}
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
	const loadFollowPage = () => {
		if (!currentLead.value.id) return
		crmApi
			.followPage({
				current: followPagination.current,
				size: followPagination.pageSize,
				leadId: currentLead.value.id,
				followType: followFilterType.value || undefined
			})
			.then((data) => {
				followList.value = data.records || []
				followPagination.total = Number(data.total || 0)
			})
	}
	const onFollowFilterChange = () => {
		followPagination.current = 1
		loadFollowPage()
	}
	const onFollowTableChange = (pagination) => {
		followPagination.current = pagination.current
		followPagination.pageSize = pagination.pageSize
		loadFollowPage()
	}
	const onOpenFollow = (record) => {
		currentLead.value = record
		followFormData.value = { leadId: record.id, followType: 'PHONE' }
		followFilterType.value = ''
		followPagination.current = 1
		followPagination.total = 0
		followList.value = []
		followOpen.value = true
		loadFollowPage()
	}
	const onSubmitFollow = () => {
		followFormRef.value.validate().then(() => {
			crmApi.followAdd(followFormData.value).then(() => {
				message.success('跟进已保存')
				followFormData.value = { leadId: currentLead.value.id, followType: followFormData.value.followType || 'PHONE' }
				followPagination.current = 1
				loadFollowPage()
				tableRef.value?.refresh()
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
				message.success('试听预约已创建')
				trialOpen.value = false
				tableRef.value?.refresh()
				trialTableRef.value?.refresh()
			})
		})
	}
	const onOpenTrialResult = (record) => {
		trialResultLeadName.value = record.leadName
		trialResultFormData.value = {
			id: record.id,
			status: record.status === 'PENDING' ? 'DONE' : record.status,
			result: record.result || 'PENDING',
			remark: record.remark
		}
		trialResultOpen.value = true
	}
	const onSubmitTrialResult = () => {
		crmApi.trialUpdate(trialResultFormData.value).then(() => {
			message.success('试听结果已保存')
			trialResultOpen.value = false
			trialTableRef.value?.refresh()
		})
	}
	const onCancelTrial = (record) => {
		crmApi.trialCancel({ id: record.id }).then(() => {
			message.success('试听预约已取消')
			trialTableRef.value?.refresh()
		})
	}
	const onOpenConvertFromTrial = (record) => {
		crmApi.leadDetail({ id: record.leadId }).then((lead) => {
			if (lead) {
				onOpenConvert(lead)
			}
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
				const studentId = res
				const afterLead = () =>
					crmApi.leadEdit({ ...currentLead.value, status: 'ENROLLED' }).then(() => {
						convertOpen.value = false
						tableRef.value?.refresh()
						trialTableRef.value?.refresh()
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
							id: orderRes,
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
	loadOverview()

	// 工作台待办直达：打开线索跟进弹窗（兼容旧跳转）
	const applyWorkbenchAction = () => {
		const raw = sessionStorage.getItem('workbench:action')
		if (!raw) return
		sessionStorage.removeItem('workbench:action')
		try {
			const action = JSON.parse(raw)
			if (action.type === 'follow' && action.record) {
				onOpenFollow(action.record)
			}
		} catch (e) {
			// 忽略损坏的跳转参数，保持页面正常
		}
	}
	applyWorkbenchAction()
</script>

<style lang="less" scoped>
	.crm-overview {
		margin-bottom: 12px;
	}
	.crm-overview-title {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		margin-bottom: 10px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.crm-overview-hint {
		font-size: 11px;
		font-weight: 400;
		color: var(--text-color-tertiary);
	}
	.crm-funnel {
		display: flex;
		gap: 10px;
		flex-wrap: wrap;
	}
	.crm-funnel-item {
		flex: 1;
		min-width: 110px;
		padding: 8px 12px;
		border-radius: var(--radius-control);
		background: var(--background-color-light);
	}
	.crm-funnel-label {
		font-size: 11px;
		color: var(--text-color-secondary);
	}
	.crm-funnel-value {
		font-size: 20px;
		font-weight: 600;
		color: var(--text-color);
		font-variant-numeric: tabular-nums;
	}
	.crm-funnel-rate {
		font-size: 11px;
		color: var(--text-color-tertiary);
	}
	.crm-quick-filter {
		display: flex;
		align-items: center;
	}
	.crm-follow-filter {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		margin-bottom: 10px;
	}
	.crm-follow-total {
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
</style>
