<template>
	<div>
		<a-card :loading="loading" style="margin-bottom: 16px">
			<div style="display: flex; align-items: center; gap: 16px">
				<a-avatar size="large" style="background: #1677ff">{{ (student.name || '?').slice(0, 1) }}</a-avatar>
				<div>
					<div style="font-size: 16px; font-weight: 600">{{ student.name }} <a-tag :color="statusColor" style="margin-left: 8px">{{ statusText }}</a-tag></div>
					<div style="color: rgba(0,0,0,.45); font-size: 13px; margin-top: 4px">家长：{{ student.parentName }} {{ student.parentPhone }} · 生日：{{ student.birthday || '-' }}</div>
				</div>
				<div style="margin-left: auto">
					<a-button type="primary" @click="orderOpen = true">报名购课</a-button>
				</div>
			</div>
		</a-card>

		<a-card>
			<a-tabs v-model:activeKey="activeTab">
				<a-tab-pane key="account" tab="课时账户">
					<a-alert
						type="info"
						show-icon
						style="margin-bottom: 12px"
						message="课时账户按「学员 + 课程」维度：同一课程下的多个班级共用一个余额，任一班点名都会扣减它"
					/>
					<a-table :columns="accountColumns" :data-source="accounts" :row-key="(r) => r.id" :pagination="false" bordered size="middle">
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
					<a-table :columns="flowColumns" :data-source="flows" :row-key="(r) => r.id" :pagination="false" bordered size="middle">
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
					<a-table :columns="orderColumns" :data-source="orders" :row-key="(r) => r.id" :pagination="false" bordered size="middle">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="orderStatusColor(record.status)">{{ orderStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-space>
									<a-button v-if="record.status === 'UNPAID'" type="link" size="small" @click="onOpenPay(record)">线下收款</a-button>
									<a-button v-if="record.status === 'UNPAID'" type="link" size="small" @click="onCancelOrder(record)">取消</a-button>
									<a-button v-if="record.status === 'PAID'" type="link" size="small" @click="onOpenRefund(record)">退费</a-button>
								</a-space>
							</template>
						</template>
					</a-table>
				</a-tab-pane>
				<a-tab-pane key="attend" tab="出勤记录">
					<a-table :columns="attendColumns" :data-source="attends" :row-key="(r) => r.id" :pagination="false" bordered size="middle">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'attendStatus'">
								<a-tag :color="attendStatusColor(record.attendStatus)">{{ attendStatusText(record.attendStatus) }}</a-tag>
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
				<a-tab-pane key="refund" tab="退费单">
					<a-table :columns="refundColumns" :data-source="refunds" :row-key="(r) => r.id" :pagination="false" bordered size="middle">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'status'">
								<a-tag :color="refundStatusColor(record.status)">{{ refundStatusText(record.status) }}</a-tag>
							</template>
							<template v-if="column.dataIndex === 'action'">
								<a-button v-if="record.status === 'PENDING'" type="link" size="small" @click="onOpenApprove(record)">审批</a-button>
								<a-button v-if="record.status === 'TO_REFUND'" type="link" size="small" @click="onConfirmRefund(record)">确认退款</a-button>
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

		<a-modal v-model:open="orderOpen" title="报名购课" :width="480" :destroy-on-close="true" @ok="onSubmitOrder">
			<a-form ref="orderFormRef" :model="orderFormData" layout="vertical">
				<a-form-item label="课包" name="coursePackageId" :rules="[{ required: true, message: '请选择课包' }]">
					<a-select v-model:value="orderFormData.coursePackageId" placeholder="请选择课包" :options="packageOptions" />
				</a-form-item>
				<a-form-item label="支付渠道" name="payChannel">
					<a-radio-group v-model:value="orderFormData.payChannel" :options="[{ value: 'WECHAT', label: '微信' }, { value: 'OFFLINE', label: '线下' }]" />
				</a-form-item>
			</a-form>
		</a-modal>

		<a-modal v-model:open="payOpen" title="线下收款登记" :width="480" :destroy-on-close="true" @ok="onSubmitPay">
			<a-form ref="payFormRef" :model="payFormData" layout="vertical">
				<a-alert type="info" show-icon message="v1 暂未接入线上支付，请在此登记线下/现金收款" style="margin-bottom: 16px" />
				<a-form-item label="收款渠道" name="payChannel">
					<a-select
						v-model:value="payFormData.payChannel"
						:options="[
							{ value: 'OFFLINE', label: '线下转账' },
							{ value: 'CASH', label: '现金' }
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
				<a-alert type="info" show-icon message="正数为补录/赠送课时，负数为收回课时；收回不能超过剩余课时" style="margin-bottom: 16px" />
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
	</div>
</template>

<script setup name="studentDetail">
	import { useRoute } from 'vue-router'
	import studentApi from '@/api/student/studentApi'
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
	const activeTab = ref('account')
	const courseMap = ref({})
	const packageOptions = ref([])
	const packageRaw = ref([])

	const orderOpen = ref(false)
	const orderFormRef = ref()
	const orderFormData = ref({})
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

	const statusText = computed(() => {
		return student.value.status === 'STUDYING' ? '在读' : student.value.status === 'STOP' ? '停课' : '已退费'
	})
	const statusColor = computed(() => {
		return student.value.status === 'STUDYING' ? 'green' : student.value.status === 'STOP' ? 'orange' : 'red'
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
	const attendStatusText = (status) => ({ ATTEND: '到课', LEAVE: '请假', ABSENT: '缺勤' }[status] || status || '-')
	const attendStatusColor = (status) => ({ ATTEND: 'green', LEAVE: 'orange', ABSENT: 'red' }[status] || 'default')
	const refundColumns = [
		{ title: '退费单号', dataIndex: 'id' },
		{ title: '可退课时', dataIndex: 'refundLessons' },
		{ title: '退费金额', dataIndex: 'refundAmount' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '审批意见', dataIndex: 'opinion' },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]

	const flowTypeText = (t) => ({ SIGNUP: '购课入账', ATTEND: '点名消课', RESTORE: '回补', REFUND: '退费销课', ADJUST: '人工调整' }[t] || t)
	const orderStatusText = (s) => ({ UNPAID: '待支付', PAID: '已支付', REFUNDED: '已退费', CANCELED: '已取消' }[s] || s)
	const orderStatusColor = (s) => (s === 'PAID' ? 'green' : s === 'UNPAID' ? 'orange' : s === 'REFUNDED' ? 'red' : 'default')
	const refundStatusText = (s) =>
		({ PENDING: '待审批', TO_REFUND: '待退款', REFUNDED: '已退款', REJECTED: '已驳回', CANCELLED: '已撤销' }[s] || s)
	const refundStatusColor = (s) =>
		s === 'REFUNDED' ? 'green' : s === 'PENDING' ? 'orange' : s === 'REJECTED' || s === 'CANCELLED' ? 'red' : 'blue'

	const loadAll = () => {
		loading.value = true
		Promise.all([
			studentApi.studentDetail({ id: currentStudentId.value }),
			studentApi.lessonAccountPage({ current: 1, size: 50, studentId: currentStudentId.value }),
			studentApi.lessonFlowPage({ current: 1, size: 50, studentId: currentStudentId.value }),
			studentApi.orderPage({ current: 1, size: 50, studentId: currentStudentId.value }),
			studentApi.refundPage({ current: 1, size: 50, studentId: currentStudentId.value }),
			courseApi.coursePage({ current: 1, size: 100 }),
			courseApi.packagePage({ current: 1, size: 100 }),
			courseApi.studentAttendPage({ current: 1, size: 50, studentId: currentStudentId.value })
		]).then(([stu, acc, flow, order, refund, course, pkg, attend]) => {
			student.value = stu
			accounts.value = acc.records || []
			flows.value = (flow.records || []).map((f) => ({ ...f, flowType: flowTypeText(f.flowType) }))
			orders.value = order.records || []
			attends.value = (attend?.records || []).map((a) => ({ ...a }))
			refunds.value = refund.records || []
			const cmap = {}
			for (const c of course.records || []) cmap[c.id] = c.name
			courseMap.value = cmap
			packageRaw.value = pkg.records || []
			packageOptions.value = packageRaw.value.map((p) => ({ value: p.id, label: `${p.name}（${p.lessons}课时 ${p.price}元）` }))
		}).finally(() => (loading.value = false))
	}

	const onSubmitOrder = () => {
		orderFormRef.value.validate().then(() => {
			studentApi
				.orderAdd({ studentId, coursePackageId: orderFormData.value.coursePackageId, payChannel: orderFormData.value.payChannel || 'WECHAT' })
				.then((res) => {
					orderOpen.value = false
					message.success('下单成功，请登记收款')
					// 下单 → 收款 一步完成
					payFormData.value = {
						orderId: res?.data,
						studentName: student.value.name,
						packageName: packageLabel(orderFormData.value.coursePackageId),
						amount: packagePrice(orderFormData.value.coursePackageId)
					}
					payOpen.value = true
					loadAll()
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
				loadAll()
			})
		})
	}
	const onCancelOrder = (record) => {
		studentApi.orderCancel({ id: record.id }).then(() => loadAll())
	}
	const onOpenRefund = (record) => {
		refundFormData.value = { orderId: record.id }
		refundOpen.value = true
	}
	const onOpenAdjust = (record) => {
		adjustFormData.value = {
			studentId,
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
					loadAll()
				})
		})
	}
	const onSubmitRefund = () => {
		refundFormRef.value.validate().then(() => {
			studentApi.refundApply(refundFormData.value).then(() => {
				refundOpen.value = false
				loadAll()
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
			loadAll()
		})
	}
	const onConfirmRefund = (record) => {
		studentApi.refundConfirm({ id: record.id }).then(() => loadAll())
	}
	const onCancelRefund = (record) => {
		studentApi.refundCancel({ id: record.id }).then(() => loadAll())
	}
	watch(currentStudentId, () => loadAll(), { immediate: true })
</script>
