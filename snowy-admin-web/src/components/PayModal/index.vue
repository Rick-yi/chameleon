<template>
	<a-modal v-model:open="visible" title="线下收款登记" :width="480" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-alert type="info" show-icon message="请核对实收金额，确认后课时立即入账" style="margin-bottom: 16px" />
			<a-form-item label="订单">
				<a-input :value="orderLabel" disabled />
			</a-form-item>
			<a-form-item label="应收金额">
				<a-input :value="`¥${formData.amount || 0}`" disabled />
			</a-form-item>
			<a-form-item label="收款渠道" name="payChannel">
				<a-select v-model:value="formData.payChannel" :options="payChannelOptions" />
			</a-form-item>
			<a-form-item label="实收金额" name="paidAmount" :rules="[{ required: true, message: '请输入实收金额' }]">
				<a-input-number v-model:value="formData.paidAmount" :min="0" :precision="2" style="width: 100%" />
			</a-form-item>
			<a-form-item label="备注" name="remark">
				<a-input v-model:value="formData.remark" placeholder="如：收款人、转账流水号" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>
</template>

<script setup name="PayModal">
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import { PAY_CHANNEL_OPTIONS } from '@/utils/payChannel'

	// 线下收款登记弹窗（报名 / 转学员 / 续费 共用）
	const props = defineProps({
		order: { type: Object, default: null }
	})
	const emit = defineEmits(['success'])

	const visible = ref(false)
	const formRef = ref()
	const formData = ref({})
	const payChannelOptions = PAY_CHANNEL_OPTIONS
	const orderLabel = computed(() => {
		const order = props.order || {}
		return `${order.studentName || ''} · ${order.packageName || ''}`
	})

	// 打开弹窗并带入订单信息
	const open = (order) => {
		formData.value = {
			orderId: order.id || order.orderId,
			studentName: order.studentName,
			packageName: order.packageName,
			amount: order.amount,
			paidAmount: order.amount,
			payChannel: 'OFFLINE',
			remark: ''
		}
		visible.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			studentApi
				.orderOfflinePay({
					orderId: formData.value.orderId,
					paidAmount: formData.value.paidAmount,
					payChannel: formData.value.payChannel,
					remark: formData.value.remark
				})
				.then(() => {
					visible.value = false
					message.success('收款已登记，课时已入账')
					emit('success')
				})
		})
	}

	defineExpose({ open })
</script>
