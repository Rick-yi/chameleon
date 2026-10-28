<template>
	<div>
		<a-card :loading="loading" style="margin-bottom: 16px">
			<div style="display: flex; align-items: center; gap: 16px; flex-wrap: wrap">
				<a-avatar size="large" style="background: #4bb964; flex: none">{{
					(student.name || '?').slice(0, 1)
				}}</a-avatar>
				<div>
					<div style="font-size: 16px; font-weight: 600">
						{{ student.name }} <a-tag :color="statusColor" style="margin-left: 8px">{{ statusText }}</a-tag>
					</div>
					<div style="color: rgba(0, 0, 0, 0.45); font-size: 13px; margin-top: 4px">
						家长：{{ student.parentName }} {{ student.parentPhone }} · 生日：{{
							student.birthday ? String(student.birthday).slice(0, 10) : '-'
						}}
					</div>
					<div class="detail-tags">
						<a-tag v-for="tag in student.tagList || []" :key="tag.id" :color="tag.color || 'blue'">{{
							tag.name
						}}</a-tag>
						<span v-if="student.statusReason" class="detail-reason">状态原因：{{ student.statusReason }}</span>
					</div>
				</div>
				<div style="margin-left: auto">
					<a-space wrap>
						<a-button @click="openStatus">状态变更</a-button>
						<a-button @click="openTagBind">标签</a-button>
						<a-button type="primary" @click="enrollOpen = true">报名购课</a-button>
					</a-space>
				</div>
			</div>
			<div class="detail-kpis">
				<div class="kpi">
					<span>剩余课时</span>
					<b :class="remainTone">{{ remainText }}</b>
				</div>
				<div class="kpi">
					<span>累计已缴</span>
					<b>{{ formatMoney(student.totalPaidAmount) }}</b>
				</div>
				<div class="kpi">
					<span>学杂费已收</span>
					<b :class="{ 'kpi-warning': Number(student.sundryUnpaidAmount) > 0 }">{{
						formatMoney(student.sundryPaidAmount)
					}}</b>
					<em v-if="Number(student.sundryUnpaidAmount) > 0" class="kpi-sub"
						>待收 {{ formatMoney(student.sundryUnpaidAmount) }}</em
					>
				</div>
				<div class="kpi">
					<span>下次上课</span>
					<b class="kpi-text">{{ nextScheduleText }}</b>
				</div>
				<div class="kpi">
					<span>课包到期</span>
					<b :class="{ 'kpi-danger': isExpired(student.accountExpireTime) }">{{
						formatDate(student.accountExpireTime)
					}}</b>
				</div>
				<div class="kpi">
					<span>待收款</span>
					<b :class="{ 'kpi-warning': Number(student.unpaidAmount) > 0 }">{{ formatMoney(student.unpaidAmount) }}</b>
				</div>
				<div class="kpi">
					<span>所在班级</span>
					<b class="kpi-text">{{ student.classNames || '-' }}</b>
				</div>
				<div class="kpi">
					<span>最近到课</span>
					<b class="kpi-text">{{ lastAttendDate || '-' }}</b>
				</div>
			</div>
			<div v-if="riskTags.length" class="risk-tags">
				<a-tag v-for="tag in riskTags" :key="tag.text" :color="tag.color">{{ tag.text }}</a-tag>
			</div>
		</a-card>

		<a-card>
			<a-tabs v-model:activeKey="activeTab">
				<a-tab-pane key="timeline" tab="全部动态">
					<a-spin :spinning="timelineLoading">
						<a-timeline v-if="timeline.length" class="timeline-list">
							<a-timeline-item
								v-for="(item, index) in timeline"
								:key="`${item.type}-${item.refId}-${index}`"
								:color="timelineColor(item.type)"
							>
								<div class="timeline-head">
									<b>{{ item.typeName }}</b>
									<span>{{ formatDateTime(item.time) }}</span>
								</div>
								<div class="timeline-title">{{ item.title }}</div>
								<div v-if="item.content" class="timeline-content">{{ item.content }}</div>
								<div class="timeline-meta">
									<span v-if="item.amount !== null && item.amount !== undefined"
										>金额 ¥{{ Number(item.amount).toFixed(2) }}</span
									>
									<span v-if="item.lessons !== null && item.lessons !== undefined">课时 {{ item.lessons }}</span>
									<span v-if="item.operator">操作人 {{ item.operator }}</span>
								</div>
							</a-timeline-item>
						</a-timeline>
						<a-empty v-else description="暂时没有动态记录" />
					</a-spin>
				</a-tab-pane>
				<a-tab-pane key="account" tab="课时账户">
					<a-alert
						type="info"
						show-icon
						style="margin-bottom: 12px"
						message="课时账户按「学员 + 课程」维度：同一课程下的多个班级共用一个余额，任一班点名都会扣减它"
					/>
					<a-table
						:columns="accountColumns"
						:data-source="accounts"
						:row-key="(r) => r.id"
						:loading="tabLoading.account"
						:pagination="pageProps('account')"
						@change="(p) => onTabPageChange('account', p)"
						bordered
						size="middle"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'courseId'">
								{{ courseMap[record.courseId] || record.courseId }}
							</template>
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="record.status === 'NORMAL' ? 'green' : record.status === 'FROZEN' ? 'orange' : 'red'">
									{{ record.status === 'NORMAL' ? '正常' : record.status === 'FROZEN' ? '冻结' : '已清零' }}
								</a-tag>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-button type="link" size="small" @click="onOpenAdjust(record)">调整课时</a-button>
							</template>
						</template>
					</a-table>
				</a-tab-pane>
				<a-tab-pane key="flow" tab="课时流水">
					<a-table
						:columns="flowColumns"
						:data-source="flows"
						:row-key="(r) => r.id"
						:loading="tabLoading.flow"
						:pagination="pageProps('flow')"
						@change="(p) => onTabPageChange('flow', p)"
						bordered
						size="middle"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'courseId'">
								{{ courseMap[record.courseId] || '-' }}
							</template>
							<template v-if="column.dataIndex === 'className'">
								{{ record.className || '-' }}
							</template>
						</template>
					</a-table>
				</a-tab-pane>
				<a-tab-pane key="order" tab="报名订单">
					<a-table
						:columns="orderColumns"
						:data-source="orders"
						:row-key="(r) => r.id"
						:loading="tabLoading.order"
						:pagination="pageProps('order')"
						@change="(p) => onTabPageChange('order', p)"
						bordered
						size="middle"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="orderStatusColor(record.status)">{{ orderStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-space>
									<a-button v-if="record.status === 'UNPAID'" type="link" size="small" @click="onOpenPay(record)"
										>线下收款</a-button
									>
									<a-button v-if="record.status === 'UNPAID'" type="link" size="small" @click="onCancelOrder(record)"
										>取消</a-button
									>
									<a-button v-if="record.status === 'PAID'" type="link" size="small" @click="onOpenRefund(record)"
										>退费</a-button
									>
								</a-space>
							</template>
						</template>
					</a-table>
				</a-tab-pane>
				<a-tab-pane key="attend" tab="出勤记录">
					<a-table
						:columns="attendColumns"
						:data-source="attends"
						:row-key="(r) => r.id"
						:loading="tabLoading.attend"
						:pagination="pageProps('attend')"
						@change="(p) => onTabPageChange('attend', p)"
						bordered
						size="middle"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'attendStatus'">
								<a-tag :color="attendStatusColor(record.attendStatus)">{{
									attendStatusText(record.attendStatus)
								}}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'lessonsDeducted'">
								{{ record.lessonsDeducted ?? 0 }}
							</template>
							<template v-if="column.dataIndex === 'remark'">
								{{ record.remark || '-' }}
							</template>
						</template>
					</a-table>
					<a-alert
						v-if="attends.length"
						type="info"
						show-icon
						style="margin-top: 12px"
						message="请假、缺勤是否扣课时由课包/课程规则决定；未消课的记录会写明原因"
					/>
				</a-tab-pane>
				<a-tab-pane key="sundry" tab="学杂费">
					<div class="tab-toolbar">
						<span class="tab-hint">校服、教材、活动费等一次性收费；与课包分账，不产生课时</span>
						<a-button v-if="hasPerm('/student/sundry/add')" type="primary" size="small" @click="openSundry"
							>新增学杂费</a-button
						>
					</div>
					<a-table
						:columns="sundryColumns"
						:data-source="sundries"
						:row-key="(r) => r.id"
						:loading="tabLoading.sundry"
						:pagination="pageProps('sundry')"
						@change="(p) => onTabPageChange('sundry', p)"
						bordered
						size="middle"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'feeType'">
								{{ feeTypeText(record.feeType) }}
							</template>
							<template v-if="column.dataIndex === 'quantity'">{{ Number(record.quantity || 0) }}</template>
							<template v-if="column.dataIndex === 'unitPrice'">{{ formatMoney(record.unitPrice) }}</template>
							<template v-if="column.dataIndex === 'amount'">{{ formatMoney(record.amount) }}</template>
							<template v-if="column.dataIndex === 'paidAmount'">{{ formatMoney(record.paidAmount) }}</template>
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="sundryStatusColor(record.status)">{{ sundryStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'createTime'">{{ formatDate(record.createTime) }}</template>
							<template v-if="column.dataIndex === 'action'">
								<a-button
									v-if="record.status === 'UNPAID' && hasPerm('/student/sundry/pay')"
									type="link"
									size="small"
									@click="openSundryPay(record)"
									>收款</a-button
								>
								<a-popconfirm
									v-if="record.status === 'PAID' && hasPerm('/student/sundry/refund')"
									title="确认对该笔学杂费办理退费？"
									@confirm="onRefundSundry(record)"
								>
									<a class="text-danger">退费</a>
								</a-popconfirm>
								<a-popconfirm
									v-if="record.status === 'UNPAID' && hasPerm('/student/sundry/delete')"
									title="确认删除该单据？"
									@confirm="onDeleteSundry(record)"
								>
									<a class="text-danger">删除</a>
								</a-popconfirm>
							</template>
						</template>
					</a-table>
					<a-empty v-if="!sundries.length" description="暂无学杂费记录" />
				</a-tab-pane>
				<a-tab-pane key="refund" tab="退费单">
					<a-table
						:columns="refundColumns"
						:data-source="refunds"
						:row-key="(r) => r.id"
						:loading="tabLoading.refund"
						:pagination="pageProps('refund')"
						@change="(p) => onTabPageChange('refund', p)"
						bordered
						size="middle"
					>
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="refundStatusColor(record.status)">{{ refundStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-button v-if="record.status === 'PENDING'" type="link" size="small" @click="onOpenApprove(record)"
									>审批</a-button
								>
								<a-button v-if="record.status === 'TO_REFUND'" type="link" size="small" @click="onConfirmRefund(record)"
									>确认退款</a-button
								>
								<a-popconfirm
									v-if="record.status === 'PENDING' || record.status === 'TO_REFUND'"
									title="确定撤销该退费单吗？"
									@confirm="onCancelRefund(record)"
								>
									<a-button type="link" danger size="small">撤销</a-button>
								</a-popconfirm>
							</template>
						</template>
					</a-table>
				</a-tab-pane>
			</a-tabs>
		</a-card>

		<enrollment-flow v-model:open="enrollOpen" :student="student" @success="loadAll" />

		<a-modal v-model:open="payOpen" title="线下收款登记" :width="480" :destroy-on-close="true" @ok="onSubmitPay">
			<a-form ref="payFormRef" :model="payFormData" layout="vertical">
				<a-alert
					type="info"
					show-icon
					message="v1 暂未接入线上支付，请在此登记线下/现金收款"
					style="margin-bottom: 16px"
				/>
				<a-form-item label="收款渠道" name="payChannel">
					<a-select
						v-model:value="payFormData.payChannel"
						:options="[
							{ value: 'OFFLINE', label: '线下转账' },
							{ value: 'CASH', label: '现金' },
							{ value: 'WECHAT', label: '微信' },
							{ value: 'ALIPAY', label: '支付宝' }
						]"
					/>
				</a-form-item>
				<a-form-item label="实收金额" name="paidAmount">
					<a-input-number v-model:value="payFormData.paidAmount" :min="0" :precision="2" style="width: 100%" />
				</a-form-item>
				<a-form-item label="备注" name="remark">
					<a-input v-model:value="payFormData.remark" placeholder="如：收款人、转账流水号" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal v-model:open="sundryOpen" title="新增学杂费" :width="520" :destroy-on-close="true" @ok="onSubmitSundry">
			<a-form ref="sundryFormRef" :model="sundryForm" layout="vertical">
				<a-form-item label="费用类型" name="feeType">
					<a-select v-model:value="sundryForm.feeType" :options="feeTypeOptions" />
				</a-form-item>
				<a-form-item label="名称" name="itemName" :rules="[{ required: true, message: '请填写物品或费用名称' }]">
					<a-input v-model:value="sundryForm.itemName" placeholder="如：夏季校服 M 码 / 数学教材" allow-clear />
				</a-form-item>
				<a-row :gutter="12">
					<a-col :span="8">
						<a-form-item label="数量" name="quantity">
							<a-input-number v-model:value="sundryForm.quantity" :min="0.01" :precision="2" style="width: 100%" />
						</a-form-item>
					</a-col>
					<a-col :span="8">
						<a-form-item label="单价" name="unitPrice">
							<a-input-number v-model:value="sundryForm.unitPrice" :min="0" :precision="2" style="width: 100%" />
						</a-form-item>
					</a-col>
					<a-col :span="8">
						<a-form-item label="应收合计">
							<b>{{ formatMoney(sundryAmount) }}</b>
						</a-form-item>
					</a-col>
				</a-row>
				<a-form-item label="立即收款" name="payNow">
					<a-switch v-model:checked="sundryForm.payNow" />
					<span class="form-hint">关闭则先记应收，后续在列表中收款</span>
				</a-form-item>
				<a-form-item v-if="sundryForm.payNow" label="收款渠道" name="payChannel">
					<a-select
						v-model:value="sundryForm.payChannel"
						:options="[
							{ value: 'CASH', label: '现金' },
							{ value: 'WECHAT', label: '微信' },
							{ value: 'ALIPAY', label: '支付宝' },
							{ value: 'OFFLINE', label: '线下转账' }
						]"
					/>
				</a-form-item>
				<a-form-item label="备注" name="remark">
					<a-input v-model:value="sundryForm.remark" placeholder="如：尺码、领取人" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal
			v-model:open="sundryPayOpen"
			title="学杂费收款"
			:width="480"
			:destroy-on-close="true"
			@ok="onSubmitSundryPay"
		>
			<a-form :model="sundryPayForm" layout="vertical">
				<a-form-item label="收款渠道" name="payChannel">
					<a-select
						v-model:value="sundryPayForm.payChannel"
						:options="[
							{ value: 'CASH', label: '现金' },
							{ value: 'WECHAT', label: '微信' },
							{ value: 'ALIPAY', label: '支付宝' },
							{ value: 'OFFLINE', label: '线下转账' }
						]"
					/>
				</a-form-item>
				<a-form-item label="实收金额" name="paidAmount">
					<a-input-number v-model:value="sundryPayForm.paidAmount" :min="0" :precision="2" style="width: 100%" />
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal v-model:open="refundOpen" title="退费申请" :width="480" :destroy-on-close="true" @ok="onSubmitRefund">
			<a-form ref="refundFormRef" :model="refundFormData" layout="vertical">
				<a-form-item label="备注/原因" name="remark">
					<a-input v-model:value="refundFormData.remark" placeholder="请输入退费原因" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal v-model:open="approveOpen" title="退费审批" :width="480" :destroy-on-close="true" @ok="onSubmitApprove">
			<a-form ref="approveFormRef" :model="approveFormData" layout="vertical">
				<a-form-item label="审批意见" name="opinion">
					<a-input v-model:value="approveFormData.opinion" placeholder="请输入审批意见" allow-clear />
				</a-form-item>
				<div style="display: flex; gap: 8px">
					<a-button type="primary" @click="onApprove(true)">通过（冻结账户）</a-button>
					<a-button danger @click="onApprove(false)">驳回</a-button>
				</div>
			</a-form>
		</a-modal>

		<a-modal v-model:open="adjustOpen" title="人工调整课时" :width="480" :destroy-on-close="true" @ok="onSubmitAdjust">
			<a-form ref="adjustFormRef" :model="adjustFormData" layout="vertical">
				<a-alert
					type="info"
					show-icon
					message="正数为补录/赠送课时，负数为收回课时；收回不能超过剩余课时"
					style="margin-bottom: 16px"
				/>
				<a-form-item label="课程">
					<a-input :value="courseMap[adjustFormData.courseId] || adjustFormData.courseId" disabled />
				</a-form-item>
				<a-form-item label="当前剩余课时">
					<a-input :value="adjustFormData.remainLessons" disabled />
				</a-form-item>
				<a-form-item label="调整课时数" name="lessons" :rules="[{ required: true, message: '请输入调整课时数' }]">
					<a-input-number v-model:value="adjustFormData.lessons" :step="0.5" style="width: 100%" />
				</a-form-item>
				<a-form-item label="调整原因" name="reason" :rules="[{ required: true, message: '请填写调整原因' }]">
					<a-input v-model:value="adjustFormData.reason" placeholder="如：赠送 2 课时活动 / 扣错补正" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal v-model:open="statusOpen" title="学员状态变更" :width="480" :destroy-on-close="true" @ok="onSubmitStatus">
			<a-form :model="statusForm" layout="vertical">
				<a-alert
					type="info"
					show-icon
					style="margin-bottom: 12px"
					message="停课/休学后恢复上课请选择「在读」完成复课；每次变更都会写入动态时间线。"
				/>
				<a-form-item label="目标状态" required>
					<a-select v-model:value="statusForm.status" :options="statusChangeOptions" />
				</a-form-item>
				<a-form-item label="变更原因" :required="statusForm.status !== 'STUDYING'">
					<a-textarea
						v-model:value="statusForm.reason"
						:auto-size="{ minRows: 3, maxRows: 5 }"
						:placeholder="
							statusForm.status === 'STUDYING' ? '复课备注（选填）' : '请填写原因，如：家长申请、搬迁、课程结束'
						"
					/>
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal v-model:open="tagBindOpen" title="设置学员标签" :width="480" :destroy-on-close="true" @ok="onSubmitTags">
			<a-form layout="vertical">
				<a-form-item label="标签（可多选）">
					<a-select
						v-model:value="tagBindForm.tagIdList"
						mode="multiple"
						:options="tagOptions"
						placeholder="请选择标签，可多选"
						allow-clear
						option-filter-prop="label"
					/>
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="studentDetail">
	import { useRoute } from 'vue-router'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'
	import studentApi from '@/api/student/studentApi'
	import sundryApi from '@/api/student/sundryApi'
	import courseApi from '@/api/course/courseApi'
	import { message } from 'ant-design-vue'
	// 既可作为独立页面（取路由参数），也可嵌入抽屉（外部传入 studentId）
	const props = defineProps({
		studentId: { type: String, default: '' }
	})
	const route = useRoute()
	const currentStudentId = computed(() => props.studentId || route.query.id || '')
	const loading = ref(false)
	const student = ref({})
	const accounts = ref([])
	const flows = ref([])
	const orders = ref([])
	const attends = ref([])
	const refunds = ref([])
	const sundries = ref([])
	const sundryOpen = ref(false)
	const sundryFormRef = ref()
	const sundryForm = ref({ feeType: 'UNIFORM', quantity: 1, unitPrice: 0, payNow: true, payChannel: 'CASH' })
	const sundryPayOpen = ref(false)
	const sundryPayForm = ref({ id: '', payChannel: 'CASH', paidAmount: null })
	const feeTypeOptions = [
		{ value: 'UNIFORM', label: '校服' },
		{ value: 'BOOK', label: '教材' },
		{ value: 'ACTIVITY', label: '活动费' },
		{ value: 'MEAL', label: '餐费' },
		{ value: 'OTHER', label: '其他' }
	]
	const sundryColumns = [
		{ title: '费用类型', dataIndex: 'feeType', width: 100 },
		{ title: '名称', dataIndex: 'itemName' },
		{ title: '数量', dataIndex: 'quantity', width: 80 },
		{ title: '单价', dataIndex: 'unitPrice', width: 100 },
		{ title: '应收', dataIndex: 'amount', width: 100 },
		{ title: '实收', dataIndex: 'paidAmount', width: 100 },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '创建时间', dataIndex: 'createTime', width: 160 },
		{ title: '操作', dataIndex: 'action', width: 160 }
	]
	const timeline = ref([])
	const timelineLoading = ref(false)
	const activeTab = ref('timeline')
	const courseMap = ref({})
	const enrollOpen = ref(false)

	// ---------- 页签分页与懒加载 ----------
	// 详情页原先打开就并发拉 10 个请求（含全量课程列表与时间线聚合），
	// 且各页签固定 50 条、无分页，超出部分静默看不到。
	// 改为：只加载基础信息 + 当前页签；每个页签独立服务端分页。
	const tabPage = reactive({
		account: { current: 1, size: 10, total: 0 },
		flow: { current: 1, size: 10, total: 0 },
		order: { current: 1, size: 10, total: 0 },
		attend: { current: 1, size: 10, total: 0 },
		sundry: { current: 1, size: 10, total: 0 },
		refund: { current: 1, size: 10, total: 0 }
	})
	const tabLoaded = reactive({
		timeline: false,
		account: false,
		flow: false,
		order: false,
		attend: false,
		sundry: false,
		refund: false
	})
	const tabLoading = reactive({
		account: false,
		flow: false,
		order: false,
		attend: false,
		sundry: false,
		refund: false
	})
	const courseMapLoaded = ref(false)

	// 课程名映射按需加载一次（用于课时账户/流水/订单页签展示课程名）
	const ensureCourseMap = () => {
		if (courseMapLoaded.value) return Promise.resolve()
		return courseApi.coursePage({ current: 1, size: 200 }).then((data) => {
			const cmap = {}
			for (const c of data.records || []) cmap[c.id] = c.name
			courseMap.value = cmap
			courseMapLoaded.value = true
		})
	}
	const pageProps = (key) => ({
		current: tabPage[key].current,
		pageSize: tabPage[key].size,
		total: tabPage[key].total,
		showSizeChanger: true,
		pageSizeOptions: ['10', '20', '50'],
		showTotal: (total) => `共 ${total} 条`
	})
	const onTabPageChange = (key, pagination) => {
		tabPage[key].current = pagination.current
		tabPage[key].size = pagination.pageSize
		loadTab(key)
	}

	// 各页签的数据加载器
	const tabLoaders = {
		account: () =>
			studentApi
				.lessonAccountPage({
					current: tabPage.account.current,
					size: tabPage.account.size,
					studentId: currentStudentId.value
				})
				.then((d) => {
					accounts.value = d.records || []
					tabPage.account.total = d.total || 0
				}),
		flow: () =>
			studentApi
				.lessonFlowPage({ current: tabPage.flow.current, size: tabPage.flow.size, studentId: currentStudentId.value })
				.then((d) => {
					flows.value = (d.records || []).map((f) => ({ ...f, flowType: flowTypeText(f.flowType) }))
					tabPage.flow.total = d.total || 0
				}),
		order: () =>
			studentApi
				.orderPage({ current: tabPage.order.current, size: tabPage.order.size, studentId: currentStudentId.value })
				.then((d) => {
					orders.value = d.records || []
					tabPage.order.total = d.total || 0
				}),
		attend: () =>
			courseApi
				.studentAttendPage({
					current: tabPage.attend.current,
					size: tabPage.attend.size,
					studentId: currentStudentId.value
				})
				.then((d) => {
					attends.value = (d.records || []).map((a) => ({ ...a }))
					tabPage.attend.total = d.total || 0
				}),
		sundry: () =>
			sundryApi
				.page({ current: tabPage.sundry.current, size: tabPage.sundry.size, studentId: currentStudentId.value })
				.then((d) => {
					sundries.value = d.records || []
					tabPage.sundry.total = d.total || 0
				}),
		refund: () =>
			studentApi
				.refundPage({ current: tabPage.refund.current, size: tabPage.refund.size, studentId: currentStudentId.value })
				.then((d) => {
					refunds.value = d.records || []
					tabPage.refund.total = d.total || 0
				})
	}
	const loadTab = (key) => {
		// 依赖课程名的页签先确保映射就绪
		if (['account', 'flow', 'order'].includes(key)) {
			ensureCourseMap().catch(() => {})
		}
		if (!tabLoaders[key]) return Promise.resolve()
		tabLoading[key] = true
		return (
			tabLoaders[key]()
				.then(() => {
					tabLoaded[key] = true
				})
				// 加载失败要把 Promise 错误接住：页签是切换时自动触发的，
				// 不处理会变成未捕获的 rejection（控制台报 Uncaught (in promise)）
				.catch((err) => {
					message.error(err?.msg || '页签数据加载失败，请重试')
				})
				.finally(() => {
					tabLoading[key] = false
				})
		)
	}
	const loadTimeline = () => {
		if (tabLoaded.timeline) return Promise.resolve()
		timelineLoading.value = true
		return studentApi
			.studentTimeline({ studentId: currentStudentId.value })
			.then((data) => {
				timeline.value = data || []
				tabLoaded.timeline = true
			})
			.catch((err) => {
				message.error(err?.msg || '动态加载失败，请重试')
			})
			.finally(() => {
				timelineLoading.value = false
			})
	}
	// 切换页签时才加载该页签数据
	watch(activeTab, (key) => {
		if (key === 'timeline') {
			loadTimeline()
		} else if (!tabLoaded[key]) {
			loadTab(key)
		}
	})

	const payOpen = ref(false)
	const payFormRef = ref()
	const payFormData = ref({})
	const refundOpen = ref(false)
	const refundFormRef = ref()
	const refundFormData = ref({})
	const approveOpen = ref(false)
	const approveFormData = ref({})
	const currentRefund = ref({})
	const adjustOpen = ref(false)
	const adjustFormRef = ref()
	const adjustFormData = ref({})
	// 状态变更
	const statusOpen = ref(false)
	const statusForm = ref({ studentId: '', status: 'STOP', reason: '' })
	const statusOptions = [
		{ value: 'STUDYING', label: '在读' },
		{ value: 'STOP', label: '停课' },
		{ value: 'SUSPEND', label: '休学' },
		{ value: 'WITHDRAWN', label: '退学' },
		{ value: 'GRADUATED', label: '结课' },
		{ value: 'REFUNDED', label: '已退费' }
	]
	const statusChangeOptions = statusOptions.filter((item) => item.value !== 'REFUNDED')
	// 标签
	const tagList = ref([])
	const tagOptions = computed(() => tagList.value.map((item) => ({ value: item.id, label: item.name })))
	const tagBindOpen = ref(false)
	const tagBindForm = ref({ studentId: '', tagIdList: [] })

	const statusText = computed(() => {
		const item = statusOptions.find((option) => option.value === student.value.status)
		return item ? item.label : student.value.status || '-'
	})
	const statusColor = computed(() => {
		const status = student.value.status
		if (status === 'STUDYING') return 'green'
		if (status === 'STOP') return 'orange'
		if (status === 'SUSPEND') return 'gold'
		if (status === 'GRADUATED') return 'blue'
		return 'red'
	})
	const hasLessonAccount = computed(() => student.value.hasLessonAccount !== false)
	const remainNumber = computed(() => Number(student.value.remainLessons || 0))
	const remainText = computed(() => {
		if (!hasLessonAccount.value) return '未报名'
		return Number.isInteger(remainNumber.value) ? String(remainNumber.value) : remainNumber.value.toFixed(2)
	})
	const remainTone = computed(() => {
		if (!hasLessonAccount.value) return 'kpi-muted'
		if (remainNumber.value <= 0) return 'kpi-danger'
		if (remainNumber.value < 3) return 'kpi-warning'
		return ''
	})
	const parseDate = (value) => {
		if (!value) return null
		const date = new Date(String(value).replace(/-/g, '/'))
		return Number.isNaN(date.getTime()) ? null : date
	}
	const formatDate = (value) => (value ? String(value).slice(0, 10) : '-')
	const formatMoney = (value) => `¥${Number(value || 0).toFixed(2)}`
	const isExpired = (value) => {
		const date = parseDate(value)
		return !!date && date < new Date()
	}
	const lastAttendDate = computed(() => {
		const dates = attends.value
			.map((item) => item.scheduleDate)
			.filter(Boolean)
			.sort()
		return dates.length ? String(dates[dates.length - 1]).slice(0, 10) : ''
	})
	// 下次上课：展示「周几 时间 班级」，无排课时提示未安排
	const nextScheduleText = computed(() => {
		const time = student.value.nextScheduleTime
		if (!time) return '暂无排课'
		const date = parseDate(time)
		if (!date) return '暂无排课'
		const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
		const hhmm = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
		const count = Number(student.value.weekRemainScheduleCount || 0)
		return count > 0 ? `${weekday} ${hhmm}（本周 ${count} 节）` : `${weekday} ${hhmm}`
	})
	const riskTags = computed(() => {
		const tags = []
		if (!hasLessonAccount.value) {
			tags.push({ text: '未报名', color: 'default' })
		} else if (remainNumber.value <= 0) {
			tags.push({ text: '无剩余课时', color: 'red' })
		} else if (remainNumber.value < 3) {
			tags.push({ text: '课时不足 3', color: 'orange' })
		}
		if (isExpired(student.value.accountExpireTime)) {
			tags.push({ text: '课包已到期', color: 'red' })
		}
		const unpaid = Number(student.value.unpaidAmount || 0)
		if (unpaid > 0) {
			tags.push({ text: `待收款 ${formatMoney(unpaid)}`, color: 'orange' })
		}
		const last = parseDate(lastAttendDate.value)
		if (last && (Date.now() - last.getTime()) / (24 * 60 * 60 * 1000) > 30) {
			tags.push({ text: '久未到课', color: 'volcano' })
		}
		return tags
	})
	const accountColumns = [
		{ title: '课程', dataIndex: 'courseId' },
		{ title: '总课时', dataIndex: 'totalLessons' },
		{ title: '赠课', dataIndex: 'giftLessons' },
		{ title: '已用', dataIndex: 'usedLessons' },
		{ title: '剩余', dataIndex: 'remainLessons' },
		{ title: '课包到期', dataIndex: 'expireTime' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]
	const flowColumns = [
		{ title: '时间', dataIndex: 'createTime', width: 170 },
		{ title: '类型', dataIndex: 'flowType', width: 110 },
		{ title: '课程', dataIndex: 'courseId' },
		{ title: '班级', dataIndex: 'className', width: 150 },
		{ title: '课时', dataIndex: 'lessons', width: 90 },
		{ title: '备注', dataIndex: 'remark' }
	]
	const orderColumns = [
		{ title: '课包', dataIndex: 'packageName' },
		{ title: '金额', dataIndex: 'amount', width: 100 },
		{ title: '实收', dataIndex: 'paidAmount', width: 100 },
		{ title: '下单时间', dataIndex: 'createTime', width: 170 },
		{ title: '状态', dataIndex: 'status', width: 100 },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]
	const attendColumns = [
		{ title: '上课日期', dataIndex: 'scheduleDate', width: 120 },
		{ title: '时间段', dataIndex: 'timeText', width: 130 },
		{ title: '班级', dataIndex: 'className' },
		{ title: '出勤状态', dataIndex: 'attendStatus', width: 100 },
		{ title: '扣减课时', dataIndex: 'lessonsDeducted', width: 100 },
		{ title: '说明', dataIndex: 'remark' }
	]
	const attendStatusText = (status) => ({ ATTEND: '到课', LEAVE: '请假', ABSENT: '缺勤' })[status] || status || '-'
	const attendStatusColor = (status) => ({ ATTEND: 'green', LEAVE: 'orange', ABSENT: 'red' })[status] || 'default'
	const refundColumns = [
		{ title: '退费单号', dataIndex: 'id' },
		{ title: '可退课时', dataIndex: 'refundLessons' },
		{ title: '退费金额', dataIndex: 'refundAmount' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '审批意见', dataIndex: 'opinion' },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]

	const flowTypeText = (t) =>
		({ SIGNUP: '购课入账', ATTEND: '点名消课', RESTORE: '回补', REFUND: '退费销课', ADJUST: '人工调整' })[t] || t
	const orderStatusText = (s) => ({ UNPAID: '待支付', PAID: '已支付', REFUNDED: '已退费', CANCELED: '已取消' })[s] || s
	const orderStatusColor = (s) =>
		s === 'PAID' ? 'green' : s === 'UNPAID' ? 'orange' : s === 'REFUNDED' ? 'red' : 'default'
	const refundStatusText = (s) =>
		({ PENDING: '待审批', TO_REFUND: '待退款', REFUNDED: '已退款', REJECTED: '已驳回', CANCELLED: '已撤销' })[s] || s
	const refundStatusColor = (s) =>
		s === 'REFUNDED' ? 'green' : s === 'PENDING' ? 'orange' : s === 'REJECTED' || s === 'CANCELLED' ? 'red' : 'blue'

	// 只加载学员基础信息与标签；页签数据由 onOpen 后的 watch 按需加载
	const loadStudent = () => {
		loading.value = true
		return Promise.all([studentApi.studentDetail({ id: currentStudentId.value }), studentApi.tagList()])
			.then(([stu, tags]) => {
				student.value = stu
				tagList.value = tags || []
			})
			.catch((err) => {
				message.error(err?.msg || '学员详情加载失败，请重试')
			})
			.finally(() => {
				loading.value = false
			})
	}

	// 打开详情：重置分页与懒加载状态，加载基础信息与当前页签
	const loadAll = () => {
		Object.keys(tabPage).forEach((key) => {
			tabPage[key].current = 1
			tabPage[key].total = 0
			tabLoaded[key] = false
		})
		tabLoaded.timeline = false
		timeline.value = []
		accounts.value = []
		flows.value = []
		orders.value = []
		attends.value = []
		refunds.value = []
		sundries.value = []
		return loadStudent().then(() => {
			if (activeTab.value === 'timeline') {
				loadTimeline()
			} else {
				loadTab(activeTab.value)
			}
		})
	}

	// 操作后只刷新相关页签 + 基础信息（原来每次都全量重拉 10 个请求）
	const refreshAfterAction = (tabKeys) => {
		const keys = Array.isArray(tabKeys) ? tabKeys : [tabKeys]
		return loadStudent().then(() => {
			const tasks = keys.filter(Boolean).map((key) => loadTab(key))
			// 时间线是默认页签，已加载过就一并刷新，保证操作记录能立即看到
			if (tabLoaded.timeline) {
				tabLoaded.timeline = false
				tasks.push(loadTimeline())
			}
			return Promise.all(tasks)
		})
	}

	const onOpenPay = (record) => {
		payFormData.value = {
			orderId: record.id,
			studentName: record.studentName || student.value.name,
			packageName: record.packageName,
			amount: record.amount,
			paidAmount: record.amount,
			payChannel: 'OFFLINE'
		}
		payOpen.value = true
	}
	const onSubmitPay = () => {
		payFormRef.value.validate().then(() => {
			studentApi.orderOfflinePay(payFormData.value).then(() => {
				payOpen.value = false
				refreshAfterAction(['order', 'account', 'flow'])
			})
		})
	}
	const onCancelOrder = (record) => {
		studentApi.orderCancel({ id: record.id }).then(() => refreshAfterAction('order'))
	}

	// ---------- 学杂费 ----------
	const sundryAmount = computed(() => Number(sundryForm.value.quantity || 0) * Number(sundryForm.value.unitPrice || 0))
	const feeTypeText = (value) => {
		const found = feeTypeOptions.find((item) => item.value === value)
		return found ? found.label : value || '其他'
	}
	const sundryStatusText = (value) =>
		({
			UNPAID: '待收款',
			PENDING: '待确认',
			PAID: '已收款',
			REFUNDING: '退款中',
			REFUNDED: '已退款',
			CANCELED: '已取消'
		})[value] || value
	const sundryStatusColor = (value) =>
		value === 'PAID'
			? 'green'
			: value === 'UNPAID'
				? 'orange'
				: value === 'REFUNDED'
					? 'default'
					: value === 'CANCELED'
						? 'red'
						: 'blue'
	const openSundry = () => {
		sundryForm.value = {
			feeType: 'UNIFORM',
			quantity: 1,
			unitPrice: 0,
			payNow: true,
			payChannel: 'CASH',
			itemName: '',
			remark: ''
		}
		sundryOpen.value = true
	}
	const onSubmitSundry = () => {
		sundryFormRef.value.validate().then(() => {
			sundryApi
				.add({
					...sundryForm.value,
					studentId: currentStudentId.value,
					amount: sundryAmount.value
				})
				.then(() => {
					sundryOpen.value = false
					refreshAfterAction('sundry')
				})
		})
	}
	const openSundryPay = (record) => {
		sundryPayForm.value = { id: record.id, payChannel: 'CASH', paidAmount: Number(record.amount || 0) }
		sundryPayOpen.value = true
	}
	const onSubmitSundryPay = () => {
		sundryApi.pay(sundryPayForm.value).then(() => {
			sundryPayOpen.value = false
			refreshAfterAction('sundry')
		})
	}
	const onRefundSundry = (record) => {
		sundryApi
			.refund({ id: record.id, refundAmount: Number(record.paidAmount || 0), reason: '学杂费退费' })
			.then(() => refreshAfterAction('sundry'))
	}
	const onDeleteSundry = (record) => {
		sundryApi.del(record.id).then(() => refreshAfterAction('sundry'))
	}
	const onOpenRefund = (record) => {
		refundFormData.value = { orderId: record.id }
		refundOpen.value = true
	}
	const onOpenAdjust = (record) => {
		adjustFormData.value = {
			studentId: currentStudentId.value,
			courseId: record.courseId,
			remainLessons: record.remainLessons,
			lessons: 1,
			reason: ''
		}
		adjustOpen.value = true
	}
	const onSubmitAdjust = () => {
		adjustFormRef.value.validate().then(() => {
			studentApi
				.lessonAccountAdjust({
					studentId: adjustFormData.value.studentId,
					courseId: adjustFormData.value.courseId,
					lessons: adjustFormData.value.lessons,
					reason: adjustFormData.value.reason
				})
				.then(() => {
					adjustOpen.value = false
					refreshAfterAction(['account', 'flow'])
				})
		})
	}
	const onSubmitRefund = () => {
		refundFormRef.value.validate().then(() => {
			studentApi.refundApply(refundFormData.value).then(() => {
				refundOpen.value = false
				refreshAfterAction(['refund', 'order'])
			})
		})
	}
	const onOpenApprove = (record) => {
		currentRefund.value = record
		approveFormData.value = { refundId: record.id }
		approveOpen.value = true
	}
	const onApprove = (pass) => {
		studentApi.refundApprove({ ...approveFormData.value, pass }).then(() => {
			approveOpen.value = false
			refreshAfterAction(['refund', 'account'])
		})
	}
	const onConfirmRefund = (record) => {
		studentApi.refundConfirm({ id: record.id }).then(() => refreshAfterAction(['refund', 'account', 'flow', 'order']))
	}
	const onCancelRefund = (record) => {
		studentApi.refundCancel({ id: record.id }).then(() => refreshAfterAction(['refund', 'account']))
	}
	const formatDateTime = (value) => {
		if (!value) return '-'
		const date = new Date(String(value).replace(/-/g, '/'))
		if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
		const pad = (n) => String(n).padStart(2, '0')
		return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
	}
	const timelineColor = (type) => {
		if (type === 'PAY') return 'green'
		if (type === 'REFUND' || type === 'REFUND_APPLY' || type === 'REFUND_APPROVE') return 'red'
		if (type === 'ATTEND') return 'blue'
		if (type === 'STATUS') return 'orange'
		if (type === 'FOLLOW') return 'gray'
		return 'blue'
	}
	const openStatus = () => {
		statusForm.value = {
			studentId: currentStudentId.value,
			status: student.value.status === 'STUDYING' ? 'STOP' : 'STUDYING',
			reason: ''
		}
		statusOpen.value = true
	}
	const onSubmitStatus = () => {
		const form = statusForm.value
		if (form.status !== 'STUDYING' && !String(form.reason || '').trim()) {
			message.warning('请填写状态变更原因')
			return
		}
		studentApi.studentStatusChange(form).then(() => {
			statusOpen.value = false
			refreshAfterAction()
		})
	}
	const openTagBind = () => {
		tagBindForm.value = {
			studentId: currentStudentId.value,
			tagIdList: (student.value.tagList || []).map((tag) => tag.id)
		}
		tagBindOpen.value = true
	}
	const onSubmitTags = () => {
		studentApi.tagBind(tagBindForm.value).then(() => {
			tagBindOpen.value = false
			refreshAfterAction()
		})
	}
	watch(currentStudentId, () => loadAll(), { immediate: true })
</script>

<style scoped lang="less">
	.detail-kpis {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
		gap: 12px;
		margin-top: 16px;
		padding-top: 16px;
		border-top: 1px dashed var(--border-color-split);
	}
	.kpi {
		display: flex;
		flex-direction: column;
		gap: 4px;
		span {
			font-size: 12px;
			color: var(--text-color-secondary);
		}
		b {
			font-size: 20px;
			font-weight: 700;
			line-height: 1.2;
		}
		.kpi-sub {
			font-size: 12px;
			font-style: normal;
			color: #d46b08;
		}
	}
	.kpi-text {
		font-size: 14px !important;
		font-weight: 600 !important;
	}
	.kpi-danger {
		color: #cf1322;
	}
	.kpi-warning {
		color: var(--accent-text);
	}
	.kpi-muted {
		font-size: 14px !important;
		color: var(--text-color-secondary);
	}
	.risk-tags {
		display: flex;
		flex-wrap: wrap;
		gap: 6px;
		margin-top: 12px;
	}
	.detail-tags {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 4px;
		margin-top: 6px;
	}
	.detail-reason {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.timeline-list {
		padding-top: 8px;
	}
	.timeline-head {
		display: flex;
		align-items: baseline;
		gap: 8px;
		justify-content: space-between;
		b {
			color: var(--text-color);
		}
		span {
			font-size: 12px;
			color: var(--text-color-secondary);
		}
	}
	.timeline-title {
		margin-top: 2px;
		font-size: 13px;
		font-weight: 500;
	}
	.timeline-content {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
		word-break: break-all;
	}
	.timeline-meta {
		display: flex;
		flex-wrap: wrap;
		gap: 12px;
		margin-top: 4px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	@media (max-width: 576px) {
		.detail-kpis {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	/* 页签工具条：左侧说明 + 右侧操作 */
	.tab-toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		margin-bottom: 12px;

		.tab-hint {
			font-size: 12px;
			color: var(--text-color-secondary);
		}
	}

	.form-hint {
		margin-left: 8px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
</style>
