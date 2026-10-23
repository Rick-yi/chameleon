<template>
	<div>
		<page-header title="权限审计" desc="角色的菜单/接口授权、用户角色分配的变更留痕" />

		<div class="ent-toolbar">
			<a-form layout="inline" :model="searchFormState">
				<a-form-item label="动作">
					<a-select v-model:value="searchFormState.action" placeholder="全部动作" :options="actionOptions" allow-clear style="width: 160px" />
				</a-form-item>
				<a-form-item label="对象">
					<a-select v-model:value="searchFormState.targetType" placeholder="全部对象" :options="targetOptions" allow-clear style="width: 140px" />
				</a-form-item>
				<a-form-item label="关键词">
					<a-input v-model:value="searchFormState.searchKey" placeholder="角色/用户/操作人" allow-clear style="width: 180px" @pressEnter="tableRef.refresh(true)" />
				</a-form-item>
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
			</a-form>
		</div>

		<div class="ent-body">
			<s-table ref="tableRef" :columns="columns" :data="loadData" :scroll="{ x: 'max-content' }" :row-key="(record) => record.id">
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'action'">
						<a-tag :color="actionColor(record.action)">{{ actionText(record.action) }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'targetName'">
						<span>{{ record.targetName || record.targetId }}</span>
						<a-tag style="margin-left: 6px">{{ record.targetType === 'ROLE' ? '角色' : '用户' }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'detail'">
						<a @click="showDetail(record)">查看明细</a>
					</template>
				</template>
			</s-table>
		</div>

		<a-drawer v-model:open="detailOpen" title="变更明细" :width="480">
			<div class="audit-detail-label">摘要</div>
			<div class="audit-detail-text">{{ currentRow?.summary }}</div>
			<div class="audit-detail-label">变更内容（JSON）</div>
			<pre class="audit-detail-json">{{ prettyDetail }}</pre>
		</a-drawer>
	</div>
</template>

<script setup name="sysAuthAudit">
	import { SearchOutlined } from '@ant-design/icons-vue'
	import authAuditApi from '@/api/sys/authAuditApi'

	const tableRef = ref()
	const searchFormState = ref({})
	const detailOpen = ref(false)
	const currentRow = ref(null)

	const actionOptions = [
		{ value: 'ROLE_RESOURCE', label: '角色菜单授权' },
		{ value: 'ROLE_PERMISSION', label: '角色接口授权' },
		{ value: 'USER_ROLE', label: '用户分配角色' }
	]
	const targetOptions = [
		{ value: 'ROLE', label: '角色' },
		{ value: 'USER', label: '用户' }
	]

	const actionText = (v) =>
		({ ROLE_RESOURCE: '角色菜单授权', ROLE_PERMISSION: '角色接口授权', USER_ROLE: '用户分配角色' }[v] || v)
	const actionColor = (v) => ({ ROLE_RESOURCE: 'blue', ROLE_PERMISSION: 'purple', USER_ROLE: 'orange' }[v] || 'default')

	const columns = [
		{ title: '时间', dataIndex: 'createTime', width: 170 },
		{ title: '动作', dataIndex: 'action', width: 150 },
		{ title: '对象', dataIndex: 'targetName', width: 220 },
		{ title: '变更摘要', dataIndex: 'summary', ellipsis: true },
		{ title: '操作人', dataIndex: 'operatorName', width: 120 },
		{ title: '明细', dataIndex: 'detail', width: 90 }
	]

	const loadData = (parameter) => authAuditApi.page(Object.assign(parameter, searchFormState.value))

	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}

	const prettyDetail = computed(() => {
		if (!currentRow.value?.detail) return '（无）'
		try {
			return JSON.stringify(JSON.parse(currentRow.value.detail), null, 2)
		} catch (e) {
			return currentRow.value.detail
		}
	})

	const showDetail = (record) => {
		currentRow.value = record
		detailOpen.value = true
	}
</script>

<style lang="less" scoped>
	.audit-detail-label {
		margin: 12px 0 6px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.audit-detail-text {
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.audit-detail-json {
		background: var(--background-color-light);
		border-radius: var(--radius-control);
		padding: 10px 12px;
		font-size: 12px;
		line-height: 18px;
		max-height: 420px;
		overflow: auto;
		white-space: pre-wrap;
		word-break: break-all;
	}
</style>
