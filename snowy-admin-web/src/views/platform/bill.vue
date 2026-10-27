<template>
	<xn-panel>
		<a-row :gutter="12" style="margin-bottom: 12px">
			<a-col :xs="12" :sm="8" :xl="4">
				<a-card :bordered="false"><a-statistic title="账单数" :value="summary.billCount || 0" /></a-card>
			</a-col>
			<a-col :xs="12" :sm="8" :xl="4">
				<a-card :bordered="false"><a-statistic title="已收款" :value="summary.paidAmount || 0" prefix="¥" :precision="2" :value-style="{ color: '#2ba471' }" /></a-card>
			</a-col>
			<a-col :xs="12" :sm="8" :xl="4">
				<a-card :bordered="false"><a-statistic title="待收款" :value="summary.unpaidAmount || 0" prefix="¥" :precision="2" :value-style="{ color: '#d89614' }" /></a-card>
			</a-col>
			<a-col :xs="12" :sm="8" :xl="4">
				<a-card :bordered="false"><a-statistic title="已作废" :value="summary.voidAmount || 0" prefix="¥" :precision="2" /></a-card>
			</a-col>
			<a-col :xs="12" :sm="8" :xl="4">
				<a-card :bordered="false"><a-statistic title="待收款单数" :value="summary.unpaidCount || 0" /></a-card>
			</a-col>
			<a-col :xs="12" :sm="8" :xl="4">
				<a-card :bordered="false"><a-statistic title="已收款单数" :value="summary.paidCount || 0" /></a-card>
			</a-col>
		</a-row>

		<a-form ref="searchFormRef" :model="searchForm">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="12" :lg="6">
					<a-form-item label="关键词">
						<a-input v-model:value="searchForm.searchKey" placeholder="账单号/租户名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12" :lg="5">
					<a-form-item label="状态">
						<a-select v-model:value="searchForm.status" placeholder="全部状态" :options="statusOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12" :lg="5">
					<a-form-item label="发票">
						<a-select v-model:value="searchForm.invoiceStatus" placeholder="全部" :options="invoiceOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="12" :lg="8">
					<a-form-item>
						<a-space>
							<a-button type="primary" @click="reload">查询</a-button>
							<a-button @click="reset">重置</a-button>
							<a-button @click="onExport">导出对账</a-button>
						</a-space>
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>

		<s-table ref="tableRef" :columns="columns" :data="loadData" bordered :row-key="(r) => r.id" :scroll="{ x: 'max-content' }">
			<template #operator>
				<a-button type="primary" @click="onOpenAdd">新增账单</a-button>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'billNo'">
					<span class="bill-no">{{ record.billNo || '-' }}</span>
				</template>
				<template v-if="column.dataIndex === 'tenant'">
					<div>{{ record.tenantName || '-' }}</div>
					<div class="bill-sub">{{ record.tenantCode || '' }}</div>
				</template>
				<template v-if="column.dataIndex === 'amount'">¥{{ Number(record.amount || 0).toFixed(2) }}</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="billColor(record.status)">{{ billText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'invoiceStatus'">{{ invoiceText(record.invoiceStatus) }}</template>
				<template v-if="column.dataIndex === 'period'">{{ date(record.periodStart) }} ~ {{ date(record.periodEnd) }}</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space :size="4">
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a v-if="record.status === 'UNPAID'" @click="onOpenPay(record)">收款</a>
						<a v-if="record.status === 'UNPAID'" @click="onVoid(record)">作废</a>
						<a-popconfirm v-if="record.status !== 'PAID'" title="确定删除该账单吗？" @confirm="onDelete(record)">
							<a style="color: var(--danger-text)">删除</a>
						</a-popconfirm>
						<a v-if="record.status === 'PAID'" @click="onOpenPay(record)">查看</a>
					</a-space>
				</template>
			</template>
		</s-table>

		<a-modal v-model:open="open" :title="form.id ? '编辑账单' : '新增账单'" :width="620" :destroy-on-close="true" @ok="onSubmit">
			<a-form ref="formRef" :model="form" layout="vertical">
				<a-row :gutter="16">
					<a-col :span="12">
						<a-form-item label="租户" name="tenantId" :rules="[{ required: true, message: '请选择租户' }]">
							<a-select v-model:value="form.tenantId" :options="tenantOptions" show-search option-filter-prop="label" placeholder="请选择租户" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="套餐" name="packageId">
							<a-select v-model:value="form.packageId" :options="packageOptions" placeholder="选填" show-search option-filter-prop="label" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="周期开始">
							<a-date-picker v-model:value="form.periodStart" value-format="YYYY-MM-DD" style="width: 100%" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="周期结束">
							<a-date-picker v-model:value="form.periodEnd" value-format="YYYY-MM-DD" style="width: 100%" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="金额(元)" name="amount" :rules="[{ required: true, message: '请输入金额' }]">
							<a-input-number v-model:value="form.amount" :min="0" :precision="2" style="width: 100%" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="状态">
							<a-select v-model:value="form.status" :options="statusOptions" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="发票状态">
							<a-select v-model:value="form.invoiceStatus" :options="invoiceOptions" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="收款方式">
							<a-select v-model:value="form.payMethod" :options="payMethodOptions" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="24">
						<a-form-item label="备注">
							<a-input v-model:value="form.remark" placeholder="人工对账备注" allow-clear />
						</a-form-item>
					</a-col>
				</a-row>
			</a-form>
		</a-modal>

		<a-modal v-model:open="payOpen" title="登记收款" :width="460" :destroy-on-close="true" @ok="onPay">
			<a-form layout="vertical">
				<a-form-item label="账单">
					<a-input :value="`${payBill.tenantName || ''} · ${payBill.billNo || ''} · ¥${Number(payBill.amount || 0).toFixed(2)}`" disabled />
				</a-form-item>
				<a-form-item label="收款方式">
					<a-select v-model:value="payForm.payMethod" :options="payMethodOptions" />
				</a-form-item>
				<a-form-item label="收款时间">
					<a-date-picker v-model:value="payForm.payTime" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
				</a-form-item>
				<a-form-item label="备注">
					<a-input v-model:value="payForm.remark" placeholder="流水号/收款人" allow-clear />
				</a-form-item>
			</a-form>
		</a-modal>
	</xn-panel>
</template>

<script setup name="platformBill">
	import { message, Modal } from 'ant-design-vue'
	import tenantApi from '@/api/platform/tenantApi'
	import downloadUtil from '@/utils/downloadUtil'

	const loading = ref(false)
	const summary = ref({})
	const searchForm = ref({})
	const tableRef = ref()
	const formRef = ref()
	const form = ref({})
	const open = ref(false)
	const payOpen = ref(false)
	const payBill = ref({})
	const payForm = ref({})
	const tenantOptions = ref([])
	const packageOptions = ref([])
	const statusOptions = [
		{ value: 'UNPAID', label: '待收款' },
		{ value: 'PAID', label: '已收款' },
		{ value: 'VOID', label: '已作废' }
	]
	const invoiceOptions = [
		{ value: 'NONE', label: '未开票' },
		{ value: 'REQUESTED', label: '已申请' },
		{ value: 'ISSUED', label: '已开票' }
	]
	const payMethodOptions = [
		{ value: 'OFFLINE', label: '线下' },
		{ value: 'BANK', label: '银行转账' },
		{ value: 'OTHER', label: '其他' }
	]
	const columns = [
		{ title: '账单号', dataIndex: 'billNo', width: 150 },
		{ title: '租户', dataIndex: 'tenant', width: 180 },
		{ title: '套餐', dataIndex: 'packageName', width: 130 },
		{ title: '周期', dataIndex: 'period', width: 200 },
		{ title: '金额', dataIndex: 'amount', width: 110 },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '发票', dataIndex: 'invoiceStatus', width: 90 },
		{ title: '收款方式', dataIndex: 'payMethod', width: 90 },
		{ title: '收款时间', dataIndex: 'payTime', width: 160 },
		{ title: '备注', dataIndex: 'remark', ellipsis: true },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 180, fixed: 'right' }
	]
	const date = (v) => (v ? String(v).slice(0, 10) : '-')
	const billText = (s) => ({ UNPAID: '待收款', PAID: '已收款', VOID: '已作废' }[s] || s)
	const billColor = (s) => (s === 'PAID' ? 'green' : s === 'UNPAID' ? 'orange' : 'default')
	const invoiceText = (s) => ({ NONE: '未开票', REQUESTED: '已申请', ISSUED: '已开票' }[s] || s)
	const loadData = (parameter) => tenantApi.billPage(Object.assign({}, parameter, searchForm.value))
	const loadSummary = () => tenantApi.billSummary(searchForm.value).then((data) => (summary.value = data || {}))
	const loadOptions = () => {
		tenantApi.tenantPage({ current: 1, size: 500 }).then((data) => {
			tenantOptions.value = (data?.records || []).map((item) => ({ value: item.id, label: `${item.name}（${item.code || '-'}）` }))
		})
		tenantApi.packagePage({ current: 1, size: 200 }).then((data) => {
			packageOptions.value = (data?.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const reload = () => {
		tableRef.value?.refresh(true)
		loadSummary()
	}
	const reset = () => {
		searchForm.value = {}
		reload()
	}
	const onOpenAdd = () => {
		form.value = { status: 'UNPAID', invoiceStatus: 'NONE', amount: 0, payMethod: 'OFFLINE' }
		open.value = true
	}
	const onOpenEdit = (record) => {
		form.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const api = form.value.id ? tenantApi.billEdit : tenantApi.billAdd
			api(form.value).then(() => {
				message.success('账单已保存')
				open.value = false
				reload()
			})
		})
	}
	const onDelete = (record) => {
		tenantApi.billDelete([{ id: record.id }]).then(() => {
			message.success('已删除')
			reload()
		})
	}
	const onOpenPay = (record) => {
		payBill.value = record
		payForm.value = { id: record.id, payMethod: record.payMethod || 'OFFLINE', payTime: undefined, remark: '' }
		payOpen.value = true
	}
	const onPay = () => {
		tenantApi.billPay(payForm.value).then(() => {
			message.success('已登记收款')
			payOpen.value = false
			reload()
		})
	}
	const onVoid = (record) => {
		Modal.confirm({
			title: '作废账单',
			content: `确定作废 ${record.billNo || ''} 吗？`,
			okText: '作废',
			okButtonProps: { danger: true },
			cancelText: '取消',
			onOk: () => tenantApi.billVoid({ id: record.id, reason: '平台手工作废' }).then(() => reload())
		})
	}
	const onExport = () => {
		tenantApi.billExport(searchForm.value).then((res) => downloadUtil.resultDownload(res))
	}
	loadOptions()
	loadSummary()
</script>

<style scoped lang="less">
	.bill-no {
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
		font-size: 12px;
	}
	.bill-sub {
		font-size: 11px;
		color: var(--text-color-tertiary);
	}
</style>
