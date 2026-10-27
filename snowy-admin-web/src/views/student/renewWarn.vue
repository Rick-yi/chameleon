<template>
	<div>
		<page-header title="续费预警" desc="课时不足与课包到期提醒，可在此直接跟进、提醒、转顾问与续费">
			<template #extra>
				<a-space>
					<a-button @click="openConfig">
						<template #icon><setting-outlined /></template>
						预警配置
					</a-button>
					<a-button v-if="hasPerm('/student/renewWarn/scan')" @click="onScan">
						<template #icon><thunderbolt-outlined /></template>
						立即扫描
					</a-button>
				</a-space>
			</template>
		</page-header>

		<a-tabs v-model:activeKey="activeTab">
			<a-tab-pane key="warn" tab="预警列表">
				<div class="renew-stats">
					<div v-for="item in statCards" :key="item.label" class="renew-stat">
						<div class="renew-stat-label">{{ item.label }}</div>
						<div class="renew-stat-value">{{ item.value }}</div>
						<div class="renew-stat-sub">{{ item.sub }}</div>
					</div>
				</div>
				<div class="ent-toolbar">
					<a-form layout="inline" :model="warnSearch">
						<a-form-item label="学员">
							<a-input v-model:value="warnSearch.searchKey" placeholder="姓名或家长手机" allow-clear style="width: 220px" />
						</a-form-item>
						<a-form-item label="状态">
							<a-select v-model:value="warnSearch.status" placeholder="全部状态" :options="statusOptions" allow-clear style="width: 140px" />
						</a-form-item>
						<a-form-item>
							<a-space>
								<a-button type="primary" @click="warnTableRef.refresh(true)">
									<template #icon><SearchOutlined /></template>
									查询
								</a-button>
								<a-button @click="resetWarn">
									<template #icon><redo-outlined /></template>
									重置
								</a-button>
							</a-space>
						</a-form-item>
					</a-form>
				</div>
				<s-table
					ref="warnTableRef"
					:columns="warnColumns"
					:data="loadWarnData"
					bordered
					:row-key="(record) => record.id"
					:scroll="{ x: 'max-content' }"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'studentName'">
							<div>{{ record.studentName || '学员' }}</div>
							<div class="renew-student-sub">{{ record.parentPhone || '' }}</div>
						</template>
						<template v-if="column.dataIndex === 'courseName'">
							<div>{{ record.courseName || '课程' }}</div>
							<div class="renew-student-sub">{{ record.classNames || '' }}</div>
						</template>
						<template v-if="column.dataIndex === 'remainLessons'">
							{{ fmtLessons(record.remainLessons) }}
						</template>
						<template v-if="column.dataIndex === 'currentRemainLessons'">
							<div>{{ record.currentRemainLessons == null ? '-' : fmtLessons(record.currentRemainLessons) }}</div>
							<div v-if="record.currentRenewed && record.status === 'PENDING'">
								<a-tag color="green">课时已补充</a-tag>
							</div>
							<div v-else-if="record.currentRemainLessons != null && Number(record.currentRemainLessons) <= 0">
								<a-tag color="red">已用完</a-tag>
							</div>
						</template>
						<template v-if="column.dataIndex === 'warnReason'">
							<a-tag :color="reasonColor(record.warnReason)">{{ reasonText(record.warnReason) }}</a-tag>
						</template>
						<template v-if="column.dataIndex === 'status'">
							<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
						</template>
						<template v-if="column.dataIndex === 'followUserName'">
							{{ record.followUserName || '-' }}
						</template>
						<template v-if="column.dataIndex === 'lastFollowTime'">
							<a-tooltip :title="record.lastFollowContent || ''">
								{{ formatTime(record.lastFollowTime) }}
							</a-tooltip>
						</template>
						<template v-if="column.dataIndex === 'lastNotifyTime'">
							{{ formatTime(record.lastNotifyTime) }}
						</template>
						<template v-if="column.dataIndex === 'createTime'">
							{{ formatTime(record.createTime) }}
						</template>
						<template v-if="column.dataIndex === 'action'">
							<a-space v-if="record.status === 'PENDING'" wrap size="small">
								<a-button type="link" size="small" @click="onOpenRenew(record)">续费</a-button>
								<a-button type="link" size="small" @click="onNotify(record)">提醒</a-button>
								<a-button type="link" size="small" @click="onAssign(record)">转顾问</a-button>
								<a-button type="link" size="small" @click="onWarnFollow(record)">跟进</a-button>
								<a-popconfirm title="确定标记为已处理吗？" @confirm="onProcess(record)">
									<a-button type="link" size="small">已处理</a-button>
								</a-popconfirm>
								<a-button type="link" size="small" @click="onWarnLoss(record)">流失</a-button>
								<a-popconfirm title="确定忽略该预警吗？" @confirm="onIgnore(record)">
									<a-button type="link" size="small" danger>忽略</a-button>
								</a-popconfirm>
							</a-space>
							<span v-else>-</span>
						</template>
					</template>
				</s-table>
			</a-tab-pane>

			<a-tab-pane key="risk" tab="风险学员">
				<div class="ent-toolbar">
					<a-form layout="inline" :model="riskSearch">
						<a-form-item label="风险类型">
							<a-select v-model:value="riskSearch.type" :options="riskTypeOptions" style="width: 160px" />
						</a-form-item>
						<a-form-item label="课程">
							<a-select
								v-model:value="riskSearch.courseId"
								:options="courseOptions"
								placeholder="全部课程"
								allow-clear
								show-search
								option-filter-prop="label"
								style="width: 220px"
							/>
						</a-form-item>
						<a-form-item>
							<a-space>
								<a-button type="primary" @click="riskTableRef.refresh(true)">
									<template #icon><SearchOutlined /></template>
									查询
								</a-button>
								<a-button @click="resetRisk">
									<template #icon><redo-outlined /></template>
									重置
								</a-button>
							</a-space>
						</a-form-item>
					</a-form>
				</div>
				<s-table
					ref="riskTableRef"
					:columns="riskColumns"
					:data="loadRiskData"
					bordered
					:row-key="(record) => `${record.studentId}-${record.courseId || ''}`"
					:scroll="{ x: 'max-content' }"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'studentName'">
							<div>{{ record.studentName || '学员' }}</div>
							<div class="renew-student-sub">{{ record.campusId ? '校区：' + record.campusId : '' }}</div>
						</template>
						<template v-if="column.dataIndex === 'courseName'">
							<div>{{ record.courseName || '课程' }}</div>
							<div class="renew-student-sub">{{ record.classNames || '' }}</div>
						</template>
						<template v-if="column.dataIndex === 'remainLessons'">
							{{ fmtLessons(record.remainLessons) }}
						</template>
						<template v-if="column.dataIndex === 'expireTime'">
							{{ formatTime(record.expireTime) }}
						</template>
						<template v-if="column.dataIndex === 'riskDesc'">
							<a-tag :color="riskColor(record.riskType)">{{ record.riskDesc || record.riskType }}</a-tag>
						</template>
						<template v-if="column.dataIndex === 'lastFollowTime'">
							<a-tooltip :title="record.lastFollowContent || ''">
								{{ formatTime(record.lastFollowTime) }}
							</a-tooltip>
							<div class="renew-student-sub">{{ record.followUserName || '' }}</div>
						</template>
						<template v-if="column.dataIndex === 'action'">
							<a-space wrap size="small">
								<a-button type="link" size="small" @click="onRiskFollow(record)">联系家长</a-button>
								<a-button type="link" size="small" @click="onRiskNotify(record)">发提醒</a-button>
								<a-button type="link" size="small" :disabled="record.hasPendingWarn" @click="onRiskTodo(record)">
									{{ record.hasPendingWarn ? '已有待办' : '生成待办' }}
								</a-button>
								<a-button type="link" size="small" @click="onAssign(record)">转顾问</a-button>
								<a-button type="link" size="small" @click="onRiskLoss(record)">登记流失</a-button>
							</a-space>
						</template>
					</template>
				</s-table>
			</a-tab-pane>
		</a-tabs>

		<!-- 续费 -->
		<a-modal v-model:open="renewOpen" title="学员续费" :width="480" :destroy-on-close="true" @ok="onSubmitRenew">
			<a-form ref="renewFormRef" :model="renewFormData" layout="vertical">
				<a-alert type="info" show-icon message="选择课包并确认收款后，课时立即入账，该预警自动标记为已续费" style="margin-bottom: 16px" />
				<a-form-item label="学员">
					<a-input :value="`${renewFormData.studentName || ''}（${renewFormData.courseName || ''}）`" disabled />
				</a-form-item>
				<a-form-item label="续费课包" name="coursePackageId" :rules="[{ required: true, message: '请选择课包' }]">
					<a-select v-model:value="renewFormData.coursePackageId" placeholder="请选择课包" :options="renewPackageOptions" show-search option-filter-prop="label" />
				</a-form-item>
				<a-form-item label="收款渠道" name="payChannel">
					<a-select v-model:value="renewFormData.payChannel" :options="payChannelOptions" />
				</a-form-item>
				<a-form-item label="实收金额" name="paidAmount" :rules="[{ required: true, message: '请输入实收金额' }]">
					<a-input-number v-model:value="renewFormData.paidAmount" :min="0" :precision="2" style="width: 100%" />
				</a-form-item>
				<a-form-item label="备注" name="remark">
					<a-input v-model:value="renewFormData.remark" placeholder="如：收款人、转账流水号" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<!-- 预警阈值配置 -->
		<a-modal v-model:open="configOpen" title="续费预警阈值配置" :width="860" :footer="null" :destroy-on-close="true">
			<a-alert
				type="info"
				show-icon
				message="生效顺序：班级 > 课程 > 机构默认。未配置时沿用系统旧阈值（剩余≤3课时或7天内到期）。"
				style="margin-bottom: 12px"
			/>
			<a-form ref="configFormRef" :model="configForm" layout="inline" class="config-form">
				<a-form-item label="范围" name="scopeType" :rules="[{ required: true, message: '请选择范围' }]">
					<a-select v-model:value="configForm.scopeType" :options="scopeTypeOptions" style="width: 110px" @change="onConfigScopeChange" />
				</a-form-item>
				<a-form-item v-if="configForm.scopeType === 'COURSE'" label="课程" name="scopeId" :rules="[{ required: true, message: '请选择课程' }]">
					<a-select
						v-model:value="configForm.scopeId"
						:options="courseOptions"
						placeholder="选择课程"
						show-search
						option-filter-prop="label"
						style="width: 180px"
					/>
				</a-form-item>
				<a-form-item v-if="configForm.scopeType === 'CLASS'" label="班级" name="scopeId" :rules="[{ required: true, message: '请选择班级' }]">
					<a-select
						v-model:value="configForm.scopeId"
						:options="classOptions"
						placeholder="选择班级"
						show-search
						option-filter-prop="label"
						style="width: 180px"
					/>
				</a-form-item>
				<a-form-item label="剩余课时阈值" name="lessonThreshold" :rules="[{ required: true, message: '请输入阈值' }]">
					<a-input-number v-model:value="configForm.lessonThreshold" :min="0" :precision="2" style="width: 110px" />
				</a-form-item>
				<a-form-item label="到期前(天)" name="expireDays" :rules="[{ required: true, message: '请输入天数' }]">
					<a-input-number v-model:value="configForm.expireDays" :min="0" :max="365" :precision="0" style="width: 100px" />
				</a-form-item>
				<a-form-item label="重复提醒(天)" name="repeatDays" :rules="[{ required: true, message: '请输入间隔' }]">
					<a-input-number v-model:value="configForm.repeatDays" :min="1" :max="365" :precision="0" style="width: 100px" />
				</a-form-item>
				<a-form-item label="启用">
					<a-switch v-model:checked="configEnabled" checked-children="是" un-checked-children="否" />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="saveConfig">{{ editingConfigId ? '保存修改' : '新增配置' }}</a-button>
						<a-button v-if="editingConfigId" @click="resetConfigForm">取消编辑</a-button>
					</a-space>
				</a-form-item>
			</a-form>
			<a-table
				:columns="configColumns"
				:data-source="configList"
				:pagination="false"
				size="small"
				:row-key="(record) => record.id || `${record.scopeType}-${record.scopeId}`"
				:scroll="{ x: 'max-content' }"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'scopeType'">
						{{ scopeTypeText(record.scopeType) }}
					</template>
					<template v-if="column.dataIndex === 'enabled'">
						<a-tag :color="record.enabled === 'NO' ? 'default' : 'green'">{{ record.enabled === 'NO' ? '停用' : '启用' }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'action'">
						<a-space>
							<a-button type="link" size="small" @click="editConfig(record)">编辑</a-button>
							<a-popconfirm v-if="record.id" title="确定删除该配置吗？" @confirm="deleteConfig(record)">
								<a-button type="link" size="small" danger>删除</a-button>
							</a-popconfirm>
						</a-space>
					</template>
				</template>
			</a-table>
		</a-modal>

		<!-- 转顾问 -->
		<a-modal v-model:open="assignOpen" title="转顾问" :width="420" :destroy-on-close="true" @ok="onAssignSubmit">
			<a-form ref="assignFormRef" :model="assignForm" layout="vertical">
				<a-form-item label="学员">
					<a-input :value="assignTarget.studentName || assignTarget.studentId || ''" disabled />
				</a-form-item>
				<a-form-item label="跟进顾问" name="followUserId" :rules="[{ required: true, message: '请选择跟进顾问' }]">
					<a-select
						v-model:value="assignForm.followUserId"
						:options="assigneeOptions"
						placeholder="选择招生顾问"
						show-search
						option-filter-prop="label"
					/>
				</a-form-item>
				<a-form-item label="备注" name="remark">
					<a-input v-model:value="assignForm.remark" placeholder="如：家长希望周末回电" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<!-- 跟进/流失登记 -->
		<a-modal v-model:open="followOpen" :title="followTitle" :width="480" :destroy-on-close="true" @ok="onFollowSubmit">
			<a-form ref="followFormRef" :model="followForm" layout="vertical">
				<a-alert v-if="followMode === 'LOSS'" type="warning" show-icon message="登记流失只记录原因与回访，不自动改变学员在读状态，避免与退费/停课流程冲突" style="margin-bottom: 12px" />
				<a-form-item label="学员">
					<a-input :value="followForm.target?.studentName || followForm.target?.studentId || ''" disabled />
				</a-form-item>
				<a-form-item label="跟进方式" name="followType">
					<a-select v-model:value="followForm.followType" :options="followTypeOptions" />
				</a-form-item>
				<a-form-item v-if="followMode === 'LOSS'" label="流失类型" name="lossType" :rules="[{ required: true, message: '请选择流失类型' }]">
					<a-select v-model:value="followForm.lossType" :options="lossTypeOptions" />
				</a-form-item>
				<a-form-item v-if="followMode === 'LOSS'" label="流失原因" name="lossReason">
					<a-input v-model:value="followForm.lossReason" placeholder="如：搬家、价格、课程不合适" allow-clear />
				</a-form-item>
				<a-form-item :label="followMode === 'LOSS' ? '回访记录' : '跟进内容'" name="content" :rules="[{ required: true, message: '请填写内容' }]">
					<a-textarea v-model:value="followForm.content" :rows="4" placeholder="记录家长反馈、约定事项等" />
				</a-form-item>
				<a-form-item label="下次跟进时间" name="nextFollowTime">
					<a-date-picker v-model:value="followForm.nextFollowTime" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="renewWarn">
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import renewApi from '@/api/student/renewApi'
	import { PAY_CHANNEL_OPTIONS } from '@/utils/payChannel'

	const activeTab = ref('warn')
	const warnTableRef = ref()
	const riskTableRef = ref()
	const warnSearch = ref({})
	const riskSearch = ref({ type: 'LOW_LESSON' })
	const stats = ref({})
	const courseOptions = ref([])
	const classOptions = ref([])

	const statusOptions = [
		{ value: 'PENDING', label: '待处理' },
		{ value: 'PROCESSED', label: '已处理' },
		{ value: 'RENEWED', label: '已续费' },
		{ value: 'IGNORED', label: '已忽略' }
	]
	const riskTypeOptions = [
		{ value: 'LOW_LESSON', label: '课时不足' },
		{ value: 'NO_ATTEND', label: '长期未消课' },
		{ value: 'STOPPED', label: '停课' }
	]
	const followTypeOptions = [
		{ value: 'PHONE', label: '电话' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'VISIT', label: '到店' },
		{ value: 'OTHER', label: '其他' }
	]
	const lossTypeOptions = [
		{ value: 'REFUND', label: '退学/退费' },
		{ value: 'GRADUATE', label: '结课' },
		{ value: 'TRANSFER', label: '转校/转班' },
		{ value: 'OTHER', label: '其他' }
	]
	const scopeTypeOptions = [
		{ value: 'DEFAULT', label: '机构默认' },
		{ value: 'COURSE', label: '按课程' },
		{ value: 'CLASS', label: '按班级' }
	]

	const warnColumns = [
		{ title: '学员', dataIndex: 'studentName', width: 130 },
		{ title: '课程/班级', dataIndex: 'courseName', width: 160 },
		{ title: '预警时剩余', dataIndex: 'remainLessons', width: 100 },
		{ title: '当前剩余', dataIndex: 'currentRemainLessons', width: 120 },
		{ title: '预警原因', dataIndex: 'warnReason', width: 100 },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '跟进人', dataIndex: 'followUserName', width: 90 },
		{ title: '最近跟进', dataIndex: 'lastFollowTime', width: 150 },
		{ title: '最近通知', dataIndex: 'lastNotifyTime', width: 150 },
		{ title: '生成时间', dataIndex: 'createTime', width: 150 },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 320, fixed: 'right' }
	]
	const riskColumns = [
		{ title: '学员', dataIndex: 'studentName', width: 130 },
		{ title: '家长手机', dataIndex: 'parentPhone', width: 120 },
		{ title: '课程/班级', dataIndex: 'courseName', width: 160 },
		{ title: '剩余课时', dataIndex: 'remainLessons', width: 100 },
		{ title: '到期时间', dataIndex: 'expireTime', width: 150 },
		{ title: '风险', dataIndex: 'riskDesc', width: 150 },
		{ title: '最近跟进', dataIndex: 'lastFollowTime', width: 150 },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 320, fixed: 'right' }
	]
	const configColumns = [
		{ title: '范围', dataIndex: 'scopeType', width: 90 },
		{ title: '对象', dataIndex: 'scopeName', width: 200 },
		{ title: '剩余课时阈值', dataIndex: 'lessonThreshold', width: 120 },
		{ title: '到期前(天)', dataIndex: 'expireDays', width: 100 },
		{ title: '重复提醒(天)', dataIndex: 'repeatDays', width: 120 },
		{ title: '启用', dataIndex: 'enabled', width: 80 },
		{ title: '操作', dataIndex: 'action', width: 120 }
	]

	const statCards = computed(() => [
		{
			label: '预警总数',
			value: stats.value.warnTotal || 0,
			sub: `涉及学员 ${stats.value.warnStudentCount || 0}`
		},
		{ label: '待处理', value: stats.value.pendingCount || 0, sub: `已续费 ${stats.value.renewedCount || 0}` },
		{ label: '续费率', value: `${stats.value.renewedRate || 0}%`, sub: '已续费 / 预警总数' },
		{ label: '流失率', value: `${stats.value.churnRate || 0}%`, sub: `流失登记 ${stats.value.lossCount || 0}` }
	])

	const reasonText = (value) =>
		({ LESSON_LOW: '课时不足', EXPIRE_SOON: '即将到期', NO_ATTEND: '长期未消课', LOSS_RISK: '停课/流失风险' }[value] || value || '-')
	const reasonColor = (value) =>
		value === 'LESSON_LOW' ? 'red' : value === 'EXPIRE_SOON' ? 'orange' : value === 'NO_ATTEND' ? 'volcano' : 'purple'
	const statusText = (value) =>
		({ PENDING: '待处理', PROCESSED: '已处理', RENEWED: '已续费', IGNORED: '已忽略' }[value] || value || '-')
	const statusColor = (value) =>
		value === 'PENDING' ? 'orange' : value === 'RENEWED' ? 'green' : value === 'PROCESSED' ? 'blue' : 'default'
	const riskColor = (value) => (value === 'LOW_LESSON' ? 'red' : value === 'NO_ATTEND' ? 'volcano' : 'purple')
	const scopeTypeText = (value) => ({ DEFAULT: '机构默认', COURSE: '课程', CLASS: '班级' }[value] || value)
	const fmtLessons = (value) => (value == null || value === '' ? '-' : Number(value).toString())
	const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '-')

	const loadWarnData = (parameter) => renewApi.renewWarnPage(Object.assign({}, parameter, warnSearch.value))
	const loadRiskData = (parameter) => renewApi.renewWarnRiskPage(Object.assign({}, parameter, riskSearch.value))

	const loadStats = () => {
		renewApi
			.renewWarnStats({})
			.then((data) => {
				stats.value = data || {}
			})
			.catch(() => {})
	}
	const refreshWarn = () => {
		warnTableRef.value?.refresh(true)
		loadStats()
	}
	const refreshRisk = () => {
		riskTableRef.value?.refresh(true)
	}

	const resetWarn = () => {
		warnSearch.value = {}
		warnTableRef.value.refresh(true)
	}
	const resetRisk = () => {
		riskSearch.value = { type: 'LOW_LESSON' }
		riskTableRef.value.refresh(true)
	}
	const onScan = () => {
		renewApi.renewWarnScan().then(() => {
			message.success('扫描完成')
			refreshWarn()
			refreshRisk()
		})
	}
	const onNotify = (record) => {
		renewApi.renewWarnNotify({ id: record.id }).then(() => {
			message.success('提醒已记录')
			refreshWarn()
		})
	}
	const onProcess = (record) => {
		renewApi.renewWarnProcess({ id: record.id }).then(() => {
			message.success('已标记处理')
			refreshWarn()
		})
	}
	const onIgnore = (record) => {
		renewApi.renewWarnIgnore({ id: record.id }).then(() => {
			message.success('已忽略')
			refreshWarn()
		})
	}

	// 一键续费：下单 → 收款 → 课时入账 → 预警自动置为已续费
	const renewOpen = ref(false)
	const renewFormRef = ref()
	const renewFormData = ref({})
	const renewPackageOptions = ref([])
	const payChannelOptions = PAY_CHANNEL_OPTIONS
	const loadPackages = (courseId) => {
		courseApi.packagePage({ current: 1, size: 100 }).then((data) => {
			const list = (data.records || []).filter((item) => !courseId || item.courseId === courseId)
			renewPackageOptions.value = list.map((item) => ({ value: item.id, label: `${item.name}（${item.lessons}课时 ${item.price}元）` }))
		})
	}
	const onOpenRenew = (record) => {
		renewFormData.value = {
			warnId: record.id,
			studentId: record.studentId,
			studentName: record.studentName,
			courseId: record.courseId,
			courseName: record.courseName,
			coursePackageId: undefined,
			payChannel: 'OFFLINE',
			paidAmount: undefined,
			remark: ''
		}
		loadPackages(record.courseId)
		renewOpen.value = true
	}
	// 选课包后自动带出价格
	watch(
		() => renewFormData.value.coursePackageId,
		(id) => {
			if (!id) return
			const item = renewPackageOptions.value.find((p) => p.value === id)
			if (item) {
				const price = item.label.match(/(\d+(?:\.\d+)?)元/)
				if (price) renewFormData.value.paidAmount = Number(price[1])
			}
		}
	)
	const onSubmitRenew = () => {
		renewFormRef.value.validate().then(() => {
			// 下单 + 收款一次完成（后端同一事务）
			studentApi
				.orderRenew({
					studentId: renewFormData.value.studentId,
					coursePackageId: renewFormData.value.coursePackageId,
					paidAmount: renewFormData.value.paidAmount,
					payChannel: renewFormData.value.payChannel,
					remark: renewFormData.value.remark
				})
				.then(() => {
					renewOpen.value = false
					message.success('续费成功，课时已入账')
					refreshWarn()
					refreshRisk()
				})
		})
	}

	// 转顾问
	const assignOpen = ref(false)
	const assignFormRef = ref()
	const assignForm = ref({})
	const assignTarget = ref({})
	const assigneeOptions = ref([])
	const onAssign = (record) => {
		assignTarget.value = record
		assignForm.value = { followUserId: undefined, remark: '' }
		renewApi.renewWarnAssigneeOptions({}).then((data) => {
			assigneeOptions.value = (data || []).map((item) => ({ value: item.id, label: item.name || item.id }))
		})
		assignOpen.value = true
	}
	const onAssignSubmit = () => {
		assignFormRef.value.validate().then(() => {
			const target = assignTarget.value || {}
			const payload = {
				followUserId: assignForm.value.followUserId,
				remark: assignForm.value.remark,
				riskType: target.riskType
			}
			if (target.id) {
				payload.id = target.id
			} else {
				payload.studentId = target.studentId
				payload.courseId = target.courseId
			}
			renewApi.renewWarnAssign(payload).then(() => {
				assignOpen.value = false
				message.success('已转给顾问跟进')
				refreshWarn()
				refreshRisk()
			})
		})
	}

	// 跟进 / 流失登记
	const followOpen = ref(false)
	const followFormRef = ref()
	const followForm = ref({})
	const followMode = ref('FOLLOW')
	const followTitle = computed(() => (followMode.value === 'LOSS' ? '登记流失与回访' : '记录跟进'))
	const openFollow = (target, mode, bizType) => {
		followMode.value = mode
		followForm.value = {
			target,
			bizType,
			followType: 'PHONE',
			content: '',
			nextFollowTime: undefined,
			lossType: mode === 'LOSS' ? 'REFUND' : undefined,
			lossReason: ''
		}
		followOpen.value = true
	}
	const onWarnFollow = (record) => openFollow(record, 'FOLLOW', 'RENEW_WARN')
	const onWarnLoss = (record) => openFollow(record, 'LOSS', 'LOSS')
	const onRiskFollow = (record) => openFollow(record, 'FOLLOW', 'RISK')
	const onRiskLoss = (record) => openFollow(record, 'LOSS', 'LOSS')
	const onFollowSubmit = () => {
		followFormRef.value.validate().then(() => {
			const target = followForm.value.target || {}
			const payload = {
				studentId: target.studentId,
				warnId: target.id || undefined,
				bizType: followForm.value.bizType,
				followType: followForm.value.followType,
				content: followForm.value.content,
				nextFollowTime: followForm.value.nextFollowTime
			}
			if (followMode.value === 'LOSS') {
				payload.bizType = 'LOSS'
				payload.lossType = followForm.value.lossType
				payload.lossReason = followForm.value.lossReason
			}
			renewApi.renewWarnFollowAdd(payload).then(() => {
				followOpen.value = false
				message.success(followMode.value === 'LOSS' ? '流失原因与回访已记录' : '跟进已记录')
				refreshWarn()
				refreshRisk()
			})
		})
	}

	// 风险学员动作
	const onRiskNotify = (record) => {
		renewApi.renewWarnNotify({ studentId: record.studentId, courseId: record.courseId }).then(() => {
			message.success('提醒已记录')
		})
	}
	const onRiskTodo = (record) => {
		renewApi
			.renewWarnRiskTodo({
				studentId: record.studentId,
				courseId: record.courseId,
				riskType: record.riskType,
				remainLessons: record.remainLessons
			})
			.then(() => {
				message.success('已生成续费待办')
				refreshWarn()
				refreshRisk()
			})
	}

	// 阈值配置
	const configOpen = ref(false)
	const configFormRef = ref()
	const configList = ref([])
	const editingConfigId = ref(undefined)
	const configForm = ref({ scopeType: 'DEFAULT', scopeId: '0', lessonThreshold: 3, expireDays: 7, repeatDays: 7 })
	const configEnabled = computed({
		get: () => (configForm.value.enabled || 'YES') !== 'NO',
		set: (value) => {
			configForm.value.enabled = value ? 'YES' : 'NO'
		}
	})
	const resetConfigForm = () => {
		editingConfigId.value = undefined
		configForm.value = { scopeType: 'DEFAULT', scopeId: '0', lessonThreshold: 3, expireDays: 7, repeatDays: 7, enabled: 'YES' }
	}
	const loadConfigList = () => {
		renewApi.renewWarnConfigList().then((data) => {
			configList.value = data || []
		})
	}
	const loadCourseOptions = () => {
		if (courseOptions.value.length) return
		courseApi.coursePage({ current: 1, size: 200 }).then((data) => {
			courseOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const loadClassOptions = () => {
		if (classOptions.value.length) return
		courseApi.classPage({ current: 1, size: 500 }).then((data) => {
			classOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const openConfig = () => {
		loadConfigList()
		loadCourseOptions()
		loadClassOptions()
		resetConfigForm()
		configOpen.value = true
	}
	const onConfigScopeChange = (value) => {
		configForm.value.scopeId = value === 'DEFAULT' ? '0' : undefined
	}
	const editConfig = (record) => {
		editingConfigId.value = record.id
		configForm.value = {
			scopeType: record.scopeType,
			scopeId: record.scopeId,
			lessonThreshold: record.lessonThreshold,
			expireDays: record.expireDays,
			repeatDays: record.repeatDays,
			enabled: record.enabled || 'YES'
		}
	}
	const saveConfig = () => {
		configFormRef.value.validate().then(() => {
			const payload = Object.assign({}, configForm.value, { id: editingConfigId.value })
			if (payload.scopeType === 'DEFAULT') payload.scopeId = '0'
			renewApi.renewWarnConfigSave(payload).then(() => {
				message.success('配置已保存')
				loadConfigList()
				resetConfigForm()
			})
		})
	}
	const deleteConfig = (record) => {
		renewApi.renewWarnConfigDelete({ id: record.id }).then(() => {
			message.success('已删除，回落到上一级配置')
			loadConfigList()
		})
	}

	loadStats()
	loadCourseOptions()
</script>

<style lang="less" scoped>
	.renew-stats {
		display: flex;
		gap: 12px;
		flex-wrap: wrap;
		margin-bottom: 16px;
	}
	.renew-stat {
		flex: 1;
		min-width: 150px;
		padding: 12px 14px;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
	}
	.renew-stat-label {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.renew-stat-value {
		margin-top: 4px;
		font-size: 22px;
		font-weight: 600;
		color: var(--text-color);
	}
	.renew-stat-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.renew-student-sub {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.config-form {
		margin-bottom: 12px;
		row-gap: 8px;
	}
</style>
