<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="学员" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="姓名或家长手机" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="searchFormState.status" placeholder="请选择状态" :options="statusOptions" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
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
				</a-col>
			</a-row>
		</a-form>
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:scroll="{ x: 'max-content' }"
		>
			<template #operator>
				<a-button type="primary" @click="onScan">
					<template #icon><thunderbolt-outlined /></template>
					立即扫描
				</a-button>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'warnReason'">
					<a-tag :color="record.warnReason === 'LESSON_LOW' ? 'red' : 'orange'">
						{{ record.warnReason === 'LESSON_LOW' ? '课时不足' : '即将到期' }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="record.status === 'PENDING' ? 'orange' : record.status === 'RENEWED' ? 'green' : 'default'">
						{{ record.status === 'PENDING' ? '待处理' : record.status === 'RENEWED' ? '已续费' : '已忽略' }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space v-if="record.status === 'PENDING'">
						<a-button type="link" size="small" @click="onOpenRenew(record)">续费</a-button>
						<a-popconfirm title="确定忽略该预警吗？" @confirm="onIgnore(record)">
							<a-button type="link" size="small">忽略</a-button>
						</a-popconfirm>
					</a-space>
					<span v-else>-</span>
				</template>
			</template>
		</s-table>
	</xn-panel>

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
</template>

<script setup name="renewWarn">
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const statusOptions = [
		{ value: 'PENDING', label: '待处理' },
		{ value: 'RENEWED', label: '已续费' },
		{ value: 'IGNORED', label: '已忽略' }
	]
	const columns = [
		{ title: '学员', dataIndex: 'studentName' },
		{ title: '课程', dataIndex: 'courseName' },
		{ title: '剩余课时', dataIndex: 'remainLessons' },
		{ title: '预警原因', dataIndex: 'warnReason' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '最近通知', dataIndex: 'lastNotifyTime' },
		{ title: '生成时间', dataIndex: 'createTime' },
		{ title: '操作', dataIndex: 'action', align: 'center' }
	]
	const loadData = (parameter) => {
		return studentApi.renewWarnPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const onScan = () => {
		studentApi.renewWarnScan().then(() => tableRef.value.refresh(true))
	}
	const onIgnore = (record) => {
		studentApi.renewWarnIgnore({ id: record.id }).then(() => tableRef.value.refresh())
	}

	// 一键续费：下单 → 收款 → 课时入账 → 预警自动置为已续费
	const renewOpen = ref(false)
	const renewFormRef = ref()
	const renewFormData = ref({})
	const renewPackageOptions = ref([])
	const payChannelOptions = [
		{ value: 'OFFLINE', label: '线下转账' },
		{ value: 'CASH', label: '现金' }
	]
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
					tableRef.value.refresh()
				})
		})
	}
</script>
