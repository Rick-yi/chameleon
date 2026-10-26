<template>
	<a-modal
		:open="open"
		:title="dialogTitle"
		:width="720"
		wrap-class-name="qe-modal"
		:mask-closable="false"
		:destroy-on-close="true"
		@cancel="onClose"
	>
		<!-- 第一步：学员、家长、课包 -->
		<a-form v-if="step === 'form'" ref="formRef" :model="form" layout="vertical">
			<a-alert
				v-if="restoredFromDraft"
				style="margin-bottom: 12px"
				type="info"
				show-icon
				closable
				message="已恢复上次未提交的报名内容"
				@close="restoredFromDraft = false"
			/>
			<div v-if="isExisting" class="qe-existing">
				<a-avatar :size="42" style="background: var(--primary-6)">{{ (student?.name || '?').slice(0, 1) }}</a-avatar>
				<div>
					<div class="qe-existing-name">{{ student?.name }}</div>
					<div class="qe-existing-sub">{{ student?.parentName || '家长' }} · {{ student?.parentPhone || '-' }}</div>
				</div>
			</div>
			<template v-if="!isExisting">
			<div class="qe-section-title">学员信息</div>
			<a-row :gutter="12">
				<a-col :xs="24" :sm="12">
					<a-form-item label="学员姓名" name="name" :rules="[{ required: true, message: '请输入学员姓名' }]">
						<a-input v-model:value="form.name" placeholder="请输入学员姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12">
					<a-form-item label="性别" name="gender">
						<a-radio-group v-model:value="form.gender">
							<a-radio-button value="MALE">男</a-radio-button>
							<a-radio-button value="FEMALE">女</a-radio-button>
						</a-radio-group>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12">
					<a-form-item label="生日" name="birthday">
						<a-date-picker v-model:value="form.birthday" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选填" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12">
					<a-form-item label="校区" name="campusId" :rules="[{ required: true, message: '请选择校区' }]">
						<a-select
							v-model:value="form.campusId"
							:options="campusOptions"
							placeholder="请选择校区"
							show-search
							option-filter-prop="label"
						/>
					</a-form-item>
				</a-col>
			</a-row>

			<div class="qe-section-title">家长信息</div>
			<a-row :gutter="12">
				<a-col :xs="24" :sm="12">
					<a-form-item label="家长姓名" name="parentName">
						<a-input v-model:value="form.parentName" placeholder="如：王妈妈" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12">
					<a-form-item
						label="家长手机"
						name="parentPhone"
						:rules="[
							{
								validator: (_rule, value, callback) => {
									if (!value || /^1\d{10}$/.test(value)) callback()
									else callback('请输入 11 位手机号')
								}
							}
						]"
					>
						<a-input
							v-model:value="form.parentPhone"
							placeholder="选填，用于联系与后续续费"
							allow-clear
							@blur="checkParentPhone"
						/>
					</a-form-item>
				</a-col>
			</a-row>

			<a-alert v-if="samePhoneStudents.length" class="qe-duplicate" type="warning" show-icon>
				<template #message>
					该手机号已有学员：{{ samePhoneNames }}
					<a class="qe-duplicate-link" @click="onViewExistingStudent">查看</a>
				</template>
			</a-alert>
			</template>

			<div class="qe-section-title">报名与收款</div>
			<a-row :gutter="12">
				<a-col :span="24">
					<a-form-item
						label="报名课包"
						name="coursePackageId"
						:rules="isExisting ? [{ required: true, message: '请选择课包' }] : []"
					>
						<a-select
							v-model:value="form.coursePackageId"
							:options="packageOptions"
							:placeholder="isExisting ? '请选择课包' : '不选则仅建档，稍后再报名'"
							show-search
							option-filter-prop="label"
							allow-clear
							@change="onPackageChange"
						/>
					</a-form-item>
				</a-col>
				<template v-if="form.coursePackageId">
					<a-col :xs="24" :sm="12">
						<a-form-item label="报读班级" name="classId">
							<a-select
								v-model:value="form.classId"
								:options="visibleClassOptions"
								:loading="classLoading"
								:placeholder="classPlaceholder"
								show-search
								option-filter-prop="label"
								allow-clear
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="12">
						<a-form-item label="立即收款">
							<a-switch v-model:checked="form.payNow" />
							<span class="qe-switch-tip">{{ form.payNow ? '下一步确认收款' : '仅生成待收款订单' }}</span>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="12">
						<a-form-item label="应收金额（元）">
							<a-input-number v-model:value="packagePrice" disabled style="width: 100%" />
						</a-form-item>
					</a-col>
					<a-col v-if="form.payNow" :xs="24" :sm="12">
						<a-form-item label="实收金额（元）" name="paidAmount">
							<a-input-number v-model:value="form.paidAmount" :min="0.01" :precision="2" style="width: 100%" />
							<a v-if="moneyOff > 0" class="qe-money-off" @click="onMoneyOff">抹零，收 ¥{{ Math.floor(form.paidAmount) }}</a>
						</a-form-item>
					</a-col>
				</template>
				<a-col v-if="!isExisting" :span="24">
					<a-form-item label="备注" name="remark">
						<a-input v-model:value="form.remark" placeholder="选填：意向、接送安排、优惠说明等" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>

			<a-alert
				v-if="!isExisting && !form.coursePackageId"
				type="info"
				show-icon
				message="未选择课包时只创建学员档案，不会生成订单；之后可在学员详情里报名。"
			/>
		</a-form>

		<!-- 第二步：确认收款 -->
		<div v-else-if="step === 'confirm'" class="qe-confirm">
			<div class="qe-student">
				<a-avatar :size="42" style="background: var(--primary-6)">{{ form.name.slice(0, 1) }}</a-avatar>
				<div>
					<div class="qe-student-name">{{ form.name }}</div>
					<div class="qe-student-sub">{{ form.parentPhone || '未填手机号' }}</div>
				</div>
			</div>
			<div class="qe-summary">
				<div class="qe-summary-row"><span>报名课包</span><b>{{ selectedPackage ? selectedPackage.name : '-' }}</b></div>
				<div class="qe-summary-row"><span>报读班级</span><b>{{ classLabel || '未分班' }}</b></div>
				<div class="qe-summary-row"><span>授课老师</span><b>{{ classTeacherName || '未指定' }}</b></div>
				<div class="qe-summary-row"><span>应收</span><b>¥{{ fmt(packagePrice) }}</b></div>
				<div class="qe-summary-row"><span>优惠</span><b class="qe-discount">-¥{{ fmt(discount) }}</b></div>
				<div class="qe-summary-row qe-payable"><span>实收</span><b>¥{{ fmt(form.paidAmount) }}</b></div>
			</div>
			<div class="qe-section-title">收款方式</div>
			<div class="qe-channels">
				<div
					v-for="item in channelOptions"
					:key="item.value"
					class="qe-channel"
					:class="{ active: form.payChannel === item.value }"
					@click="form.payChannel = item.value"
				>
					<component :is="item.icon" class="qe-channel-icon" />
					<span>{{ item.label }}</span>
				</div>
			</div>
			<div class="qe-meta">
				<span>经办人：{{ operatorName }}</span>
				<span>收款时间：{{ nowText }}</span>
			</div>
			<a-form-item label="收款备注" style="margin-top: 12px; margin-bottom: 0">
				<a-input v-model:value="form.payRemark" placeholder="选填，如转账尾号、优惠原因" allow-clear />
			</a-form-item>
		</div>

		<!-- 第三步：报名结果/订单详情 -->
		<div v-else class="qe-order-detail">
			<div class="qe-success-head">
				<check-circle-filled class="qe-success-icon" />
				<div class="qe-success-main">
					<div class="qe-success-title">{{ successOrder ? '报名成功' : '学员建档成功' }}</div>
					<div class="qe-success-sub">{{ successSummary }}</div>
				</div>
			</div>

			<a-spin :spinning="detailLoading">
				<div class="qe-detail-section">学员信息</div>
				<div class="qe-detail-grid">
					<div class="qe-detail-row"><span>学员</span><b>{{ successStudent?.name || form.name }}</b></div>
					<div class="qe-detail-row"><span>家长</span><b>{{ successStudent?.parentName || form.parentName || '-' }}</b></div>
					<div class="qe-detail-row"><span>手机</span><b>{{ successStudent?.parentPhone || form.parentPhone || '-' }}</b></div>
					<div class="qe-detail-row"><span>校区</span><b>{{ campusLabel || '-' }}</b></div>
					<div class="qe-detail-row"><span>班级</span><b>{{ classLabel || successStudent?.classNames || '未分班' }}</b></div>
					<div class="qe-detail-row"><span>授课老师</span><b>{{ classTeacherName || successStudent?.teacherNames || '-' }}</b></div>
					<div class="qe-detail-row"><span>剩余课时</span><b>{{ successRemainText }}</b></div>
					<div class="qe-detail-row"><span>课包到期</span><b>{{ formatDate(successStudent?.accountExpireTime) }}</b></div>
				</div>

				<template v-if="successOrder">
					<div class="qe-detail-section">订单信息</div>
					<div class="qe-detail-grid">
						<div class="qe-detail-row qe-detail-wide">
							<span>订单号</span>
							<b class="qe-order-no">{{ successOrder.id }}</b>
						</div>
						<div class="qe-detail-row"><span>课包</span><b>{{ successOrder.packageName || '-' }}</b></div>
						<div class="qe-detail-row"><span>正课课时</span><b>{{ formatLessons(successOrder.packageLessons) }}</b></div>
						<div class="qe-detail-row"><span>赠课课时</span><b>{{ formatLessons(successOrder.packageGiftLessons) }}</b></div>
						<div class="qe-detail-row"><span>合计课时</span><b>{{ totalLessons }}</b></div>
						<div class="qe-detail-row"><span>有效期</span><b>{{ successOrder.packageValidityDays ? successOrder.packageValidityDays + ' 天' : '-' }}</b></div>
						<div class="qe-detail-row"><span>应收</span><b>¥{{ fmt(successOrder.amount) }}</b></div>
						<div class="qe-detail-row"><span>优惠</span><b class="qe-discount">{{ orderDiscount > 0 ? '-¥' + fmt(orderDiscount) : '-' }}</b></div>
						<div class="qe-detail-row"><span>实收</span><b class="qe-payable-text">¥{{ fmt(successOrder.paidAmount) }}</b></div>
						<div class="qe-detail-row"><span>收款渠道</span><b>{{ channelText(successOrder.payChannel) }}</b></div>
						<div class="qe-detail-row"><span>订单状态</span><b><a-tag :color="orderStatusColor">{{ orderStatusText }}</a-tag></b></div>
						<div class="qe-detail-row"><span>收款时间</span><b>{{ formatDate(successOrder.payTime) }}</b></div>
						<div class="qe-detail-row"><span>经办人</span><b>{{ successOrder.operatorName || operatorName }}</b></div>
						<div class="qe-detail-row qe-detail-wide"><span>备注</span><b>{{ successOrder.remark || form.payRemark || '-' }}</b></div>
					</div>
				</template>
				<a-alert
					v-else
					type="info"
					show-icon
					message="本次只创建学员档案，未生成订单；之后可在学员详情里报名。"
					style="margin-top: 4px"
				/>
			</a-spin>

			<div class="qe-success-actions">
				<a-button v-if="!isExisting" @click="onEnrollAgain">继续录入</a-button>
				<a-button type="primary" @click="onViewStudent">查看学员</a-button>
			</div>
		</div>

		<template #footer>
			<div v-if="step === 'form'" class="qe-footer">
				<a-button :disabled="submitting" @click="onClose">取消</a-button>
				<a-button v-if="canConfirmPay" type="primary" :disabled="submitting" @click="onNext">核对收款</a-button>
				<a-button
					v-else
					type="primary"
					:loading="submitting"
					:disabled="isExisting && !form.coursePackageId"
					@click="submit"
				>
					{{ isExisting ? '生成订单' : '保存' }}
				</a-button>
			</div>
			<div v-else-if="step === 'confirm'" class="qe-footer">
				<a-button :disabled="submitting" @click="step = 'form'">上一步</a-button>
				<a-button type="primary" :loading="submitting" @click="submit">确认收款</a-button>
			</div>
			<div v-else class="qe-footer qe-footer-single">
				<span class="qe-footer-hint">{{ successFooterHint }}</span>
				<a-button type="primary" @click="onClose">完成</a-button>
			</div>
		</template>
	</a-modal>
</template>

<script setup name="quickEnrollModal">
	import { message } from 'ant-design-vue'
	import { useRoute, useRouter } from 'vue-router'
	import {
		CheckCircleFilled,
		WalletOutlined,
		WechatOutlined,
		AlipayCircleOutlined,
		BankOutlined
	} from '@ant-design/icons-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import { globalStore } from '@/store'

	const props = defineProps({
		open: { type: Boolean, default: false },
		// 传入学员时进入“已有学员报名”模式，只选课包/班级/收款
		student: { type: Object, default: null }
	})
	const emit = defineEmits(['update:open', 'success'])
	const route = useRoute()
	const router = useRouter()
	const store = globalStore()
	const isExisting = computed(() => !!props.student?.id)
	const dialogTitle = computed(() => {
		if (step.value === 'success') return successOrder.value ? '订单详情' : '学员详情'
		return isExisting.value ? `报名购课 · ${props.student?.name || ''}` : '快速录入学员'
	})

	const formRef = ref()
	const step = ref('form')
	const submitting = ref(false)
	const campusOptions = ref([])
	const packageOptions = ref([])
	const packageRaw = ref([])
	const classOptions = ref([])
	const teacherMap = ref({})
	const classLoading = ref(false)
	const samePhoneStudents = ref([])
	const restoredFromDraft = ref(false)
	const channelOptions = [
		{ value: 'CASH', label: '现金', icon: WalletOutlined },
		{ value: 'WECHAT', label: '微信', icon: WechatOutlined },
		{ value: 'ALIPAY', label: '支付宝', icon: AlipayCircleOutlined },
		{ value: 'OFFLINE', label: '银行转账', icon: BankOutlined }
	]
	const PAY_CHANNEL_KEY = 'enroll:lastPayChannel'
	const defaultPayChannel = () => {
		const saved = localStorage.getItem(PAY_CHANNEL_KEY)
		return channelOptions.some((item) => item.value === saved) ? saved : 'WECHAT'
	}
	const rememberPayChannel = () => {
		if (form.value.payChannel) localStorage.setItem(PAY_CHANNEL_KEY, form.value.payChannel)
	}
	const packagePrice = ref(0)
	const nowText = ref('')
	const lastStudentId = ref('')
	const lastOrderId = ref('')
	const successStudent = ref(null)
	const successOrder = ref(null)
	const detailLoading = ref(false)
	const operatorName = computed(() => store.userInfo?.name || store.userInfo?.account || '当前用户')

	const channelText = (value) => channelOptions.find((item) => item.value === value)?.label || value || '-'
	const formatLessons = (value) => {
		const number = Number(value || 0)
		return Number.isInteger(number) ? String(number) : number.toFixed(2)
	}
	const formatDate = (value) => (value ? String(value).slice(0, 19).replace('T', ' ') : '-')
	const campusLabel = computed(() => {
		const campusId = successStudent.value?.campusId || form.value.campusId
		return campusOptions.value.find((item) => item.value === campusId)?.label || '-'
	})
	const successRemainText = computed(() => {
		if (!successStudent.value) return '-'
		if (successOrder.value?.status === 'UNPAID') return '待收款后入账'
		if (successStudent.value.hasLessonAccount === false) return '未报名'
		return `${formatLessons(successStudent.value.remainLessons)} 课时`
	})
	const totalLessons = computed(() => {
		if (!successOrder.value) return '-'
		return formatLessons(Number(successOrder.value.packageLessons || 0) + Number(successOrder.value.packageGiftLessons || 0))
	})
	const orderDiscount = computed(() => {
		if (!successOrder.value) return 0
		// 待支付订单还没收款，未收金额不能当成优惠
		if (successOrder.value.status !== 'PAID' && successOrder.value.status !== 'REFUNDED') return 0
		const diff = Number(successOrder.value.amount || 0) - Number(successOrder.value.paidAmount || 0)
		return diff > 0 ? diff : 0
	})
	const orderStatusText = computed(() => {
		const status = successOrder.value?.status
		return ({ UNPAID: '待支付', PAID: '已支付', REFUNDED: '已退费', CANCELED: '已取消' })[status] || status || '-'
	})
	const orderStatusColor = computed(() => {
		const status = successOrder.value?.status
		return status === 'PAID' ? 'green' : status === 'UNPAID' ? 'orange' : status === 'REFUNDED' ? 'red' : 'default'
	})
	const successSummary = computed(() => {
		if (!successOrder.value) return '学员档案已创建，可稍后报名'
		return `${successOrder.value.packageName || '课包'} · 实收 ¥${fmt(successOrder.value.paidAmount)} · ${orderStatusText.value}`
	})
	const successFooterHint = computed(() => {
		if (!successOrder.value) return '学员档案已创建'
		return successOrder.value.status === 'UNPAID' ? '订单已生成，待收款' : '已生成订单与课时账户'
	})

	/** 报名成功后加载学员与订单详情，成功页直接作为订单详情页 */
	const loadSuccessDetail = async (studentId, orderId) => {
		detailLoading.value = true
		try {
			const [student, order] = await Promise.all([
				studentApi.studentDetail({ id: studentId }).catch(() => null),
				orderId ? studentApi.orderDetail({ id: orderId }).catch(() => null) : Promise.resolve(null)
			])
			successStudent.value = student
			successOrder.value = order
		} finally {
			detailLoading.value = false
		}
	}
	const showSuccess = (studentId, orderId) => {
		lastStudentId.value = studentId
		lastOrderId.value = orderId
		step.value = 'success'
		loadSuccessDetail(studentId, orderId)
	}

	const emptyForm = () => ({
		name: '',
		gender: 'MALE',
		birthday: null,
		parentName: '',
		parentPhone: '',
		campusId: undefined,
		remark: '',
		coursePackageId: undefined,
		classId: undefined,
		payNow: true,
		paidAmount: null,
		payChannel: defaultPayChannel(),
		payRemark: ''
	})
	const form = ref(emptyForm())
	let optionsLoaded = false
	let draftEnabled = false

	// 报名草稿：登录失效或误关页面后，重新打开可接着填
	// 草稿按“租户 + 用户”隔离，避免换账号后看到其他人的未提交内容
	const draftKey = () =>
		`quickEnroll:draft:${store.userInfo?.tenantId || store.userInfo?.orgId || 'default'}:${store.userInfo?.id || 'anonymous'}`
	const clearDraft = () => sessionStorage.removeItem(draftKey())
	const saveDraft = () => {
		if (!draftEnabled || isExisting.value || step.value !== 'form' || submitting.value) return
		// 只保存用户真正填过的内容，自动带出的课包/校区不算草稿
		const meaningful = form.value.name || form.value.parentPhone
		if (!meaningful) {
			sessionStorage.removeItem(draftKey())
			return
		}
		sessionStorage.setItem(draftKey(), JSON.stringify(form.value))
	}
	const readDraft = () => {
		try {
			const raw = sessionStorage.getItem(draftKey())
			if (!raw) return null
			const draft = JSON.parse(raw)
			return draft && (draft.name || draft.parentPhone || draft.coursePackageId) ? draft : null
		} catch (e) {
			return null
		}
	}
	const newRequestId = () =>
		typeof crypto !== 'undefined' && crypto.randomUUID
			? crypto.randomUUID()
			: `${Date.now()}-${Math.random().toString(16).slice(2)}`

	watch(form, saveDraft, { deep: true })

	const selectedPackage = computed(() => packageRaw.value.find((p) => p.id === form.value.coursePackageId) || null)
	const classLabel = computed(() => classOptions.value.find((c) => c.value === form.value.classId)?.className || '')
	const classTeacherName = computed(() => classOptions.value.find((c) => c.value === form.value.classId)?.teacherName || '')
	// 校区改变后只展示本校区的班级；老数据没有校区时保留，避免班级直接消失
	const visibleClassOptions = computed(() => {
		const campusId = form.value.campusId
		if (!campusId) return classOptions.value
		return classOptions.value.filter((item) => !item.campusId || item.campusId === campusId)
	})
	const classPlaceholder = computed(() => {
		if (classLoading.value) return '班级加载中…'
		if (!visibleClassOptions.value.length) return '该校区暂无可选班级，可稍后分班'
		return '选填，报名后老师点名可见'
	})
	const discount = computed(() => {
		const price = Number(packagePrice.value) || 0
		const paid = Number(form.value.paidAmount) || 0
		return Math.max(price - paid, 0)
	})
	const moneyOff = computed(() => {
		const paid = Number(form.value.paidAmount) || 0
		return paid > Math.floor(paid) ? paid - Math.floor(paid) : 0
	})
	const canConfirmPay = computed(() => !!form.value.coursePackageId && form.value.payNow)
	const samePhoneNames = computed(() => samePhoneStudents.value.map((item) => item.name).join('、'))

	/** 只有一个校区时自动选中，避免每次报名都手点 */
	const applyDefaultCampus = () => {
		if (!form.value.campusId && campusOptions.value.length === 1) {
			form.value.campusId = campusOptions.value[0].value
		}
	}

	/** 课包只有一个时直接带出，价格和班级一起准备 */
	const applyDefaultPackage = () => {
		if (form.value.coursePackageId || packageRaw.value.length !== 1) return
		onPackageChange(packageRaw.value[0].id)
	}

	/** 当前校区只有一个班时直接选中，少一次下拉 */
	const applyDefaultClass = () => {
		// 校区还没选时不要跨校区猜班级，避免选错校区后又把班级清掉
		if (!form.value.campusId) return
		const options = visibleClassOptions.value
		if (form.value.classId && !options.some((item) => item.value === form.value.classId)) {
			form.value.classId = undefined
		}
		if (!form.value.classId && options.length === 1) {
			form.value.classId = options[0].value
		}
	}

	watch(() => form.value.campusId, () => applyDefaultClass())
	watch(packageRaw, () => {
		applyDefaultPackage()
		applyDefaultClass()
	})
	watch(campusOptions, () => {
		applyDefaultCampus()
		applyDefaultClass()
	})

	const loadOptions = () => {
		if (optionsLoaded) {
			syncPackageFromForm()
			applyDefaultCampus()
			applyDefaultPackage()
			applyDefaultClass()
			return
		}
		optionsLoaded = true
		courseApi
			.campusOptions()
			.then((records) => {
				campusOptions.value = (records || []).map((item) => ({
					value: item.value ?? item.id,
					label: item.label ?? item.name
				}))
				applyDefaultCampus()
				applyDefaultClass()
			})
			.catch(() => {})
		// 任何一个下拉接口失败都不能拖住报名主流程，独立兜底
		Promise.all([
			courseApi.coursePage({ current: 1, size: 200 }).catch(() => ({ records: [] })),
			courseApi.packagePage({ current: 1, size: 200 }).catch(() => ({ records: [] })),
			courseApi.teacherOptions({}).catch(() => [])
		]).then(([courseData, packageData, teacherData]) => {
			const courseMap = {}
			for (const item of courseData.records || []) courseMap[item.id] = item.name
			teacherMap.value = Object.fromEntries((teacherData || []).map((item) => [item.value, item.label]))
			packageRaw.value = packageData.records || []
			packageOptions.value = packageRaw.value.map((item) => ({
				value: item.id,
				label: `${courseMap[item.courseId] || '课程'} · ${item.name}（¥${item.price}）`
			}))
			syncPackageFromForm()
			applyDefaultPackage()
		})
	}

	// 草稿恢复或重复打开时，按已选课包补齐价格与班级
	const syncPackageFromForm = () => {
		const packageId = form.value.coursePackageId
		if (!packageId) {
			applyDefaultPackage()
			return
		}
		const item = packageRaw.value.find((p) => p.id === packageId)
		if (!item) {
			form.value.coursePackageId = undefined
			form.value.paidAmount = null
			packagePrice.value = 0
			applyDefaultPackage()
			return
		}
		packagePrice.value = Number(item.price)
		if (form.value.payNow && !form.value.paidAmount) {
			form.value.paidAmount = Number(item.price)
		}
		loadClasses(item.courseId).then(() => {
			applyDefaultClass()
		})
	}

	const loadClasses = (courseId) => {
		classOptions.value = []
		if (!courseId) return Promise.resolve()
		classLoading.value = true
		return courseApi
			.classPage({ current: 1, size: 200, courseId })
			.then((data) => {
				classOptions.value = (data.records || []).map((item) => {
					const className = `${item.name}${item.classroom ? ' · ' + item.classroom : ''}`
					const teacherName = teacherMap.value[item.teacherId] || ''
					return {
						value: item.id,
						campusId: item.campusId,
						className,
						teacherName,
						label: `${className}${teacherName ? ' · ' + teacherName : ''}`
					}
				})
				applyDefaultClass()
			})
			.finally(() => (classLoading.value = false))
	}

	watch(
		() => props.open,
		(open) => {
			if (open) {
				draftEnabled = true
				step.value = 'form'
				lastOrderId.value = ''
				successStudent.value = null
				successOrder.value = null
				detailLoading.value = false
				if (isExisting.value) {
					// 已有学员：只补课包/班级/收款，不修改档案
					form.value = {
						...emptyForm(),
						name: props.student?.name || '',
						parentName: props.student?.parentName || '',
						parentPhone: props.student?.parentPhone || '',
						campusId: props.student?.campusId
					}
					lastStudentId.value = props.student.id
					restoredFromDraft.value = false
					samePhoneStudents.value = []
				} else {
					const draft = readDraft()
					form.value = draft ? { ...emptyForm(), ...draft } : emptyForm()
					restoredFromDraft.value = !!draft
					samePhoneStudents.value = []
				}
				packagePrice.value = 0
				loadOptions()
				applyDefaultCampus()
				if (!isExisting.value && form.value.parentPhone) {
					checkParentPhone()
				}
			}
		}
	)

	const checkParentPhone = () => {
		if (isExisting.value) return
		const phone = String(form.value.parentPhone || '').trim()
		samePhoneStudents.value = []
		if (!/^1\d{10}$/.test(phone)) return
		studentApi
			.studentPage({ current: 1, size: 5, parentPhone: phone })
			.then((data) => {
				// 手机号可能被家长共用，只提示不阻断
				samePhoneStudents.value = (data.records || []).filter((item) => item.parentPhone === phone)
			})
			.catch(() => {})
	}

	const onPackageChange = (packageId) => {
		// 代码自动带出课包时 v-model 还没更新，这里补上；手动选择时是幂等赋值
		if (form.value.coursePackageId !== packageId) form.value.coursePackageId = packageId
		const item = packageRaw.value.find((p) => p.id === packageId)
		packagePrice.value = item ? Number(item.price) : 0
		form.value.paidAmount = item ? Number(item.price) : null
		form.value.payNow = !!packageId
		form.value.classId = undefined
		loadClasses(item?.courseId)
	}

	const onMoneyOff = () => {
		form.value.paidAmount = Math.floor(Number(form.value.paidAmount))
	}

	const onNext = () => {
		formRef.value.validate().then(() => {
			nowText.value = new Date().toLocaleString('zh-CN', { hour12: false })
			step.value = 'confirm'
		})
	}

	const submit = () => {
		if (step.value === 'form') {
			formRef.value.validate().then(doSubmit)
		} else {
			doSubmit()
		}
	}

	const doSubmit = () => {
		if (submitting.value) return
		if (isExisting.value && !form.value.coursePackageId) {
			message.warning('请选择课包')
			return
		}
		rememberPayChannel()
		submitting.value = true
		if (isExisting.value) {
			submitExisting()
		} else {
			submitCreate()
		}
	}

	const assignClass = (studentId) => {
		if (!form.value.classId) return
		courseApi
			.classStudentAdd({ classId: form.value.classId, studentIdList: [studentId] })
			.catch(() => message.warning('报名已成功，但分班失败，请到班级管理手动添加'))
	}

	const submitCreate = () => {
		// classId 只用于报名后的分班，不进入建档接口
		const { classId, ...rest } = form.value
		const payload = {
			...rest,
			birthday: form.value.birthday ? `${form.value.birthday} 00:00:00` : undefined,
			requestId: newRequestId()
		}
		if (!payload.coursePackageId) {
			payload.payNow = false
		}
		studentApi
			.quickEnroll(payload)
			.then((data) => {
				const studentId = data?.studentId
				// 没有学员编号就不是真正的成功，避免出现“提示成功但没落库”
				if (!studentId) {
					message.error('保存失败：服务端未返回学员编号，请重试')
					return
				}
				lastStudentId.value = studentId
				clearDraft()
				assignClass(studentId)
				emit('success')
				// 建档后统一进入详情页：有课包展示订单，没有课包展示学员信息
				showSuccess(studentId, data?.orderId || null)
			})
			.catch(() => {})
			.finally(() => {
				submitting.value = false
			})
	}

	const submitExisting = () => {
		lastStudentId.value = props.student.id
		studentApi
			.orderAdd({
				studentId: props.student.id,
				coursePackageId: form.value.coursePackageId,
				payChannel: form.value.payChannel
			})
			.then((orderId) => {
				if (!orderId) {
					message.error('下单失败，请重试')
					return
				}
				lastOrderId.value = orderId
				if (!form.value.payNow) {
					assignClass(props.student.id)
					emit('success')
					showSuccess(props.student.id, orderId)
					return
				}
				return studentApi
					.orderOfflinePay({
						orderId,
						paidAmount: form.value.paidAmount,
						payChannel: form.value.payChannel,
						remark: form.value.payRemark
					})
					.then(() => {
						assignClass(props.student.id)
						emit('success')
						showSuccess(props.student.id, orderId)
					})
					.catch(() => {
						// 订单已生成，收款失败时仍展示订单详情，并引导去收款台
						message.warning('订单已生成，请到收款台完成收款')
						emit('success')
						showSuccess(props.student.id, orderId)
					})
			})
			.catch(() => {})
			.finally(() => {
				submitting.value = false
			})
	}

	const onEnrollAgain = () => {
		draftEnabled = true
		const campusId = form.value.campusId
		const payChannel = form.value.payChannel
		step.value = 'form'
		form.value = { ...emptyForm(), campusId, payChannel }
		packagePrice.value = 0
		classOptions.value = []
		samePhoneStudents.value = []
		restoredFromDraft.value = false
		lastOrderId.value = ''
		successStudent.value = null
		successOrder.value = null
		clearDraft()
		applyDefaultCampus()
		applyDefaultPackage()
	}

	const goStudent = (studentId) => {
		router.push({
			path: route.path.startsWith('/app') ? '/app/teach/students' : '/student',
			query: { id: studentId }
		})
	}

	const onViewStudent = () => {
		onClose()
		goStudent(lastStudentId.value)
	}

	// 已有学员先去看详情，草稿保留，回来后还能继续录入
	const onViewExistingStudent = () => {
		const target = samePhoneStudents.value[0]
		if (!target) return
		emit('update:open', false)
		goStudent(target.id)
	}

	const onClose = () => {
		draftEnabled = false
		clearDraft()
		emit('update:open', false)
	}

	const fmt = (value) => (Number(value) || 0).toFixed(2)
</script>

<style lang="less" scoped>
	.qe-section-title {
		margin: 6px 0 10px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
		padding-left: 8px;
		border-left: 3px solid var(--primary-6);
		line-height: 16px;
	}
	.qe-existing {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 0 14px;
		margin-bottom: 12px;
		border-bottom: 1px dashed var(--border-color-split);
	}
	.qe-existing-name {
		font-size: 15px;
		font-weight: 600;
	}
	.qe-existing-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.qe-switch-tip {
		margin-left: 8px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.qe-duplicate {
		margin-bottom: 12px;
	}
	.qe-duplicate-link {
		margin-left: 8px;
		font-weight: 500;
	}
	.qe-money-off {
		margin-left: 8px;
		font-size: 12px;
		color: var(--accent-text);
	}
	.qe-footer {
		display: flex;
		justify-content: flex-end;
		gap: 8px;
	}
	.qe-footer-single {
		justify-content: space-between;
		align-items: center;
	}
	.qe-footer-hint {
		font-size: 12px;
		color: var(--text-color-secondary);
	}

	/* 确认收款 */
	.qe-student {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 0 14px;
		border-bottom: 1px solid var(--border-color-split);
	}
	.qe-student-name {
		font-size: 15px;
		font-weight: 600;
	}
	.qe-student-sub {
		font-size: 12px;
		color: var(--text-color-secondary);
		margin-top: 2px;
	}
	.qe-summary {
		margin: 12px 0 16px;
		padding: 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
	}
	.qe-summary-row {
		display: flex;
		justify-content: space-between;
		font-size: 13px;
		padding: 4px 0;
		color: var(--text-color-secondary);
		b {
			color: var(--text-color);
			font-weight: 500;
		}
	}
	.qe-discount {
		color: var(--accent-text) !important;
	}
	.qe-payable {
		border-top: 1px dashed var(--border-color-base);
		margin-top: 6px;
		padding-top: 8px;
		b {
			font-size: 18px;
			color: var(--accent-text) !important;
		}
	}
	.qe-channels {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 8px;
	}
	.qe-channel {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 6px;
		padding: 12px 4px;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
		cursor: pointer;
		font-size: 13px;
		transition: all 0.15s;
		&:hover {
			border-color: var(--primary-4);
		}
		&.active {
			border-color: var(--primary-6);
			background: var(--primary-1);
			color: var(--primary-7);
			font-weight: 500;
		}
	}
	.qe-channel-icon {
		font-size: 20px;
	}
	.qe-meta {
		display: flex;
		justify-content: space-between;
		gap: 12px;
		margin-top: 12px;
		font-size: 12px;
		color: var(--text-color-secondary);
		flex-wrap: wrap;
	}

	/* 报名结果/订单详情 */
	.qe-order-detail {
		max-height: 66vh;
		overflow-y: auto;
		padding-right: 4px;
	}
	.qe-success-head {
		display: flex;
		align-items: center;
		gap: 12px;
		margin-bottom: 8px;
	}
	.qe-success-icon {
		font-size: 42px;
		color: var(--primary-6);
		flex: none;
	}
	.qe-success-main {
		min-width: 0;
	}
	.qe-success-title {
		font-size: 18px;
		font-weight: 600;
		color: var(--text-color);
	}
	.qe-success-sub {
		margin-top: 4px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.qe-detail-section {
		margin: 14px 0 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
		padding-left: 8px;
		border-left: 3px solid var(--primary-6);
		line-height: 16px;
	}
	.qe-detail-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0 18px;
		padding: 6px 12px;
		background: var(--background-color-light);
		border: 1px solid var(--border-color-split);
		border-radius: var(--radius-control);
	}
	.qe-detail-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
		padding: 7px 0;
		border-bottom: 1px dashed var(--border-color-split);
		font-size: 13px;
		span {
			flex: none;
			color: var(--text-color-secondary);
		}
		b {
			min-width: 0;
			text-align: right;
			font-weight: 500;
			color: var(--text-color);
			word-break: break-all;
		}
	}
	.qe-detail-row:last-child,
	.qe-detail-row:nth-last-child(2) {
		border-bottom: none;
	}
	.qe-detail-wide {
		grid-column: 1 / -1;
	}
	.qe-order-no {
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
		font-size: 12px;
		color: var(--text-color-secondary) !important;
	}
	.qe-payable-text {
		color: var(--accent-text) !important;
		font-weight: 600 !important;
	}
	.qe-success-actions {
		display: flex;
		justify-content: center;
		gap: 10px;
		margin-top: 16px;
	}
	@media (max-width: 576px) {
		.qe-detail-grid {
			grid-template-columns: 1fr;
		}
		.qe-order-detail {
			max-height: none;
		}
		.qe-success-actions {
			display: grid;
			grid-template-columns: 1fr 1fr;
		}
	}
</style>

<style lang="less">
	/* 手机端：报名流程用全屏页，底部按钮固定，贴近原生 App */
	@media (max-width: 576px) {
		.qe-modal {
			top: 0;
		}
		.qe-modal .ant-modal {
			top: 0;
			margin: 0;
			padding-bottom: 0;
			width: 100vw;
			max-width: 100vw;
		}
		.qe-modal .ant-modal-content {
			min-height: 100vh;
			border-radius: 0;
			display: flex;
			flex-direction: column;
		}
		.qe-modal .ant-modal-body {
			flex: 1;
			overflow-y: auto;
			padding: 16px;
		}
		.qe-modal .ant-modal-footer {
			margin: 0;
			padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
			border-top: 1px solid var(--border-color-split);
			background: #fff;
			position: sticky;
			bottom: 0;
		}
		.qe-channel {
			padding: 14px 4px;
		}
	}
</style>
