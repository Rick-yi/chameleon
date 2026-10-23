<template>
	<a-modal
		:open="open"
		title="快速录入学员"
		:width="720"
		:confirm-loading="submitting"
		ok-text="保存"
		cancel-text="取消"
		:destroy-on-close="true"
		@ok="onSubmit"
		@cancel="onClose"
	>
		<a-form ref="formRef" :model="form" layout="vertical">
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
				<a-col :xs="24" :sm="14">
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
				<a-col :xs="24" :sm="10">
					<a-form-item label="立即收款">
						<a-switch v-model:checked="form.payNow" :disabled="!form.coursePackageId" />
						<span class="qe-switch-tip">{{ payTip }}</span>
					</a-form-item>
				</a-col>
				<template v-if="form.coursePackageId && form.payNow">
					<a-col :xs="24" :sm="12">
						<a-form-item label="收款渠道" name="payChannel">
							<a-radio-group v-model:value="form.payChannel">
								<a-radio-button v-for="item in channelOptions" :key="item.value" :value="item.value">
									{{ item.label }}
								</a-radio-button>
							</a-radio-group>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="12">
						<a-form-item label="实收金额（元）" name="paidAmount">
							<a-input-number v-model:value="form.paidAmount" :min="0.01" :precision="2" style="width: 100%" />
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
	</a-modal>
</template>

<script setup name="quickEnrollModal">
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import orgApi from '@/api/sys/orgApi'

	const props = defineProps({
		open: { type: Boolean, default: false }
	})
	const emit = defineEmits(['update:open', 'success'])

	const formRef = ref()
	const submitting = ref(false)
	const campusOptions = ref([])
	const packageOptions = ref([])
	const packageRaw = ref([])
	const channelOptions = [
		{ value: 'CASH', label: '现金' },
		{ value: 'WECHAT', label: '微信' },
		{ value: 'ALIPAY', label: '支付宝' },
		{ value: 'OFFLINE', label: '线下转账' }
	]

	const emptyForm = () => ({
		name: '',
		gender: 'MALE',
		birthday: null,
		parentName: '',
		parentPhone: '',
		campusId: undefined,
		remark: '',
		coursePackageId: undefined,
		payNow: false,
		paidAmount: null,
		payChannel: 'CASH'
	})
	const form = ref(emptyForm())
	let optionsLoaded = false

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

	watch(
		() => props.open,
		(open) => {
			if (open) {
				form.value = emptyForm()
				loadOptions()
			}
		}
	)

	const payTip = computed(() => {
		if (!form.value.coursePackageId) return '选择课包后可登记收款'
		return form.value.payNow ? '保存时登记收款并生成课时' : '仅生成待收款订单'
	})
	const onPackageChange = (packageId) => {
		const item = packageRaw.value.find((p) => p.id === packageId)
		form.value.paidAmount = item ? Number(item.price) : null
		form.value.payNow = !!packageId
	}

	const onClose = () => emit('update:open', false)

	const onSubmit = () => {
		formRef.value.validate().then(() => {
			submitting.value = true
			const payload = {
				...form.value,
				birthday: form.value.birthday ? `${form.value.birthday} 00:00:00` : undefined
			}
			if (!payload.coursePackageId) {
				payload.payNow = false
			}
			studentApi
				.quickEnroll(payload)
				.then(() => {
					message.success(`已录入学员：${form.value.name}`)
					emit('success')
					onClose()
				})
				.catch(() => {})
				.finally(() => {
					submitting.value = false
				})
		})
	}
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
</style>
