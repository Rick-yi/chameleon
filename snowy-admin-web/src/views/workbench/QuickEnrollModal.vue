<template>
	<a-modal
		:open="open"
		:title="step === 'success' ? '报名成功' : '快速录入学员'"
		:width="720"
		wrap-class-name="qe-modal"
		:mask-closable="false"
		:destroy-on-close="true"
		@cancel="onClose"
	>
		<!-- 第一步：学员、家长、课包 -->
		<a-form v-if="step === 'form'" ref="formRef" :model="form" layout="vertical">
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
					<a-form-item label="校区" name="campusId">
						<a-select v-model:value="form.campusId" :options="campusOptions" placeholder="默认当前校区" allow-clear />
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
							{ required: true, message: '请输入家长手机' },
							{ pattern: /^1\d{10}$/, message: '请输入 11 位手机号' }
						]"
					>
						<a-input v-model:value="form.parentPhone" placeholder="用于联系与后续续费" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>

			<div class="qe-section-title">报名与收款</div>
			<a-row :gutter="12">
				<a-col :span="24">
					<a-form-item label="报名课包" name="coursePackageId">
						<a-select
							v-model:value="form.coursePackageId"
							:options="packageOptions"
							placeholder="不选则仅建档，稍后再报名"
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
								:options="classOptions"
								:loading="classLoading"
								placeholder="选填，报名后老师点名可见"
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
				<a-col :span="24">
					<a-form-item label="备注" name="remark">
						<a-input v-model:value="form.remark" placeholder="选填：意向、接送安排、优惠说明等" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>

			<a-alert
				v-if="!form.coursePackageId"
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
					<div class="qe-student-sub">{{ form.parentPhone }}</div>
				</div>
			</div>
			<div class="qe-summary">
				<div class="qe-summary-row"><span>报名课包</span><b>{{ selectedPackage ? selectedPackage.name : '-' }}</b></div>
				<div class="qe-summary-row"><span>报读班级</span><b>{{ classLabel || '未分班' }}</b></div>
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

		<!-- 第三步：成功 -->
		<div v-else class="qe-success">
			<check-circle-filled class="qe-success-icon" />
			<div class="qe-success-title">{{ form.name }} 报名成功</div>
			<div class="qe-success-amount">实收 ¥{{ fmt(form.paidAmount) }}</div>
			<div class="qe-success-sub">
				{{ selectedPackage ? selectedPackage.name : '' }}{{ classLabel ? ' · ' + classLabel : '' }}
			</div>
			<div class="qe-success-actions">
				<a-button @click="onEnrollAgain">继续录入</a-button>
				<a-button type="primary" @click="onViewStudent">查看学员</a-button>
			</div>
		</div>

		<template #footer>
			<div v-if="step === 'form'" class="qe-footer">
				<a-button @click="onClose">取消</a-button>
				<a-button v-if="canConfirmPay" type="primary" @click="onNext">下一步</a-button>
				<a-button v-else type="primary" :loading="submitting" @click="submit">保存</a-button>
			</div>
			<div v-else-if="step === 'confirm'" class="qe-footer">
				<a-button @click="step = 'form'">上一步</a-button>
				<a-button type="primary" :loading="submitting" @click="submit">确认收款</a-button>
			</div>
			<div v-else class="qe-footer qe-footer-single">
				<span class="qe-footer-hint">已生成订单与课时账户</span>
				<a-button type="primary" @click="onClose">完成</a-button>
			</div>
		</template>
	</a-modal>
</template>

<script setup name="quickEnrollModal">
	import { message } from 'ant-design-vue'
	import { useRouter } from 'vue-router'
	import {
		CheckCircleFilled,
		WalletOutlined,
		WechatOutlined,
		AlipayCircleOutlined,
		BankOutlined
	} from '@ant-design/icons-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import orgApi from '@/api/sys/orgApi'
	import { globalStore } from '@/store'

	const props = defineProps({
		open: { type: Boolean, default: false }
	})
	const emit = defineEmits(['update:open', 'success'])
	const router = useRouter()
	const store = globalStore()

	const formRef = ref()
	const step = ref('form')
	const submitting = ref(false)
	const campusOptions = ref([])
	const packageOptions = ref([])
	const packageRaw = ref([])
	const classOptions = ref([])
	const classLoading = ref(false)
	const channelOptions = [
		{ value: 'CASH', label: '现金', icon: WalletOutlined },
		{ value: 'WECHAT', label: '微信', icon: WechatOutlined },
		{ value: 'ALIPAY', label: '支付宝', icon: AlipayCircleOutlined },
		{ value: 'OFFLINE', label: '银行转账', icon: BankOutlined }
	]
	const packagePrice = ref(0)
	const nowText = ref('')
	const lastStudentId = ref('')
	const operatorName = computed(() => store.userInfo?.name || store.userInfo?.account || '当前用户')

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
		payChannel: 'WECHAT',
		payRemark: ''
	})
	const form = ref(emptyForm())
	let optionsLoaded = false

	const selectedPackage = computed(() => packageRaw.value.find((p) => p.id === form.value.coursePackageId) || null)
	const classLabel = computed(() => classOptions.value.find((c) => c.value === form.value.classId)?.label || '')
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

	const loadOptions = () => {
		if (optionsLoaded) return
		optionsLoaded = true
		orgApi.orgPage({ current: 1, size: 100 }).then((data) => {
			const records = data.records || []
			campusOptions.value = records.map((item) => ({ value: item.id, label: item.name }))
			if (!form.value.campusId && records.length === 1) {
				form.value.campusId = records[0].id
			}
		})
		Promise.all([
			courseApi.coursePage({ current: 1, size: 200 }),
			courseApi.packagePage({ current: 1, size: 200 })
		]).then(([courseData, packageData]) => {
			const courseMap = {}
			for (const item of courseData.records || []) courseMap[item.id] = item.name
			packageRaw.value = packageData.records || []
			packageOptions.value = packageRaw.value.map((item) => ({
				value: item.id,
				label: `${courseMap[item.courseId] || '课程'} · ${item.name}（¥${item.price}）`
			}))
		})
	}

	const loadClasses = (courseId) => {
		classOptions.value = []
		if (!courseId) return
		classLoading.value = true
		courseApi
			.classPage({ current: 1, size: 200, courseId })
			.then((data) => {
				classOptions.value = (data.records || []).map((item) => ({
					value: item.id,
					label: `${item.name}${item.classroom ? ' · ' + item.classroom : ''}`
				}))
			})
			.finally(() => (classLoading.value = false))
	}

	watch(
		() => props.open,
		(open) => {
			if (open) {
				step.value = 'form'
				form.value = emptyForm()
				packagePrice.value = 0
				loadOptions()
			}
		}
	)

	const onPackageChange = (packageId) => {
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
		submitting.value = true
		// classId 只用于报名后的分班，不进入建档接口
		const { classId, ...rest } = form.value
		const payload = {
			...rest,
			birthday: form.value.birthday ? `${form.value.birthday} 00:00:00` : undefined
		}
		if (!payload.coursePackageId) {
			payload.payNow = false
		}
		studentApi
			.quickEnroll(payload)
			.then((data) => {
				lastStudentId.value = data?.studentId || ''
				// 分班是附加动作：失败不影响建档/收款，只提示补办
				if (form.value.classId && lastStudentId.value) {
					courseApi
						.classStudentAdd({ classId: form.value.classId, studentIdList: [lastStudentId.value] })
						.catch(() => message.warning('学员已录入，但分班失败，请到班级管理手动添加'))
				}
				emit('success')
				if (payload.coursePackageId && payload.payNow) {
					step.value = 'success'
				} else {
					message.success(`已录入学员：${form.value.name}`)
					onClose()
				}
			})
			.catch(() => {})
			.finally(() => {
				submitting.value = false
			})
	}

	const onEnrollAgain = () => {
		step.value = 'form'
		form.value = emptyForm()
		packagePrice.value = 0
		classOptions.value = []
	}

	const onViewStudent = () => {
		onClose()
		router.push({ path: '/student', query: { id: lastStudentId.value } })
	}

	const onClose = () => emit('update:open', false)

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
	.qe-switch-tip {
		margin-left: 8px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.qe-money-off {
		margin-left: 8px;
		font-size: 12px;
		color: var(--accent-6);
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
		color: var(--accent-6) !important;
	}
	.qe-payable {
		border-top: 1px dashed var(--border-color-base);
		margin-top: 6px;
		padding-top: 8px;
		b {
			font-size: 18px;
			color: var(--accent-6) !important;
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

	/* 成功 */
	.qe-success {
		text-align: center;
		padding: 20px 0 8px;
	}
	.qe-success-icon {
		font-size: 64px;
		color: var(--primary-6);
	}
	.qe-success-title {
		margin-top: 12px;
		font-size: 18px;
		font-weight: 600;
	}
	.qe-success-amount {
		margin-top: 8px;
		font-size: 26px;
		font-weight: 700;
		color: var(--accent-6);
	}
	.qe-success-sub {
		margin-top: 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.qe-success-actions {
		display: flex;
		justify-content: center;
		gap: 10px;
		margin-top: 22px;
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
