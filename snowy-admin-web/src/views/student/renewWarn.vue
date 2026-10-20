<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="学员id" name="studentId">
						<a-input v-model:value="searchFormState.studentId" placeholder="请输入学员id" allow-clear />
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
					<a-popconfirm v-if="record.status === 'PENDING'" title="确定忽略该预警吗？" @confirm="onIgnore(record)">
						<a-button type="link" size="small">忽略</a-button>
					</a-popconfirm>
				</template>
			</template>
		</s-table>
	</xn-panel>
</template>

<script setup name="renewWarn">
	import studentApi from '@/api/student/studentApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const statusOptions = [
		{ value: 'PENDING', label: '待处理' },
		{ value: 'RENEWED', label: '已续费' },
		{ value: 'IGNORED', label: '已忽略' }
	]
	const columns = [
		{ title: '学员id', dataIndex: 'studentId' },
		{ title: '课程id', dataIndex: 'courseId' },
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
</script>
