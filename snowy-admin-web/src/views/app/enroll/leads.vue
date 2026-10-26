<template>
	<div>
		<!-- 状态筛选 -->
		<div class="m-filter">
			<div
				v-for="item in statusTabs"
				:key="item.value"
				class="m-filter-item"
				:class="{ active: status === item.value }"
				@click="onTab(item.value)"
			>
				{{ item.text }}
				<span v-if="item.count !== undefined" class="m-filter-count">{{ item.count }}</span>
			</div>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!list.length" class="m-empty">没有符合条件的线索</div>

		<div v-for="item in list" :key="item.id" class="m-card m-lead" @click="goDetail(item)">
			<div class="m-lead-head">
				<div class="m-lead-name">{{ item.name }}</div>
				<a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
			</div>
			<div class="m-lead-sub">
				<a-icon><phone-outlined /></a-icon> {{ item.parentPhone || '无电话' }}
				<template v-if="item.intentCourseName"> · 意向 {{ item.intentCourseName }}</template>
			</div>
			<div class="m-lead-foot">
				<span class="m-lead-source">来源：{{ crmSourceText(item.source) }}</span>
				<span v-if="item.nextFollowTime" class="m-lead-next">
					下次跟进 {{ String(item.nextFollowTime).slice(5, 16) }}
				</span>
			</div>
		</div>
	</div>
</template>

<script setup name="appEnrollLeads">
	import { useRouter } from 'vue-router'
	import crmApi from '@/api/crm/crmApi'
	import { crmSourceText } from '@/utils/crmSource'

	const router = useRouter()
	const loading = ref(false)
	const list = ref([])
	const status = ref('ALL')

	const statusTabs = ref([
		{ value: 'ALL', text: '全部' },
		{ value: 'NEW', text: '新线索' },
		{ value: 'FOLLOWED', text: '跟进中' },
		{ value: 'TRIALED', text: '已试听' },
		{ value: 'ENROLLED', text: '已报名' }
	])

	const statusText = (s) =>
		({ NEW: '新线索', FOLLOWED: '跟进中', TRIALED: '已试听', ENROLLED: '已报名', LOST: '已流失' }[s] || s)
	const statusColor = (s) =>
		({ NEW: 'blue', FOLLOWED: 'orange', TRIALED: 'purple', ENROLLED: 'green', LOST: 'default' }[s] || 'default')

	const load = async () => {
		loading.value = true
		try {
			const params = { current: 1, size: 50 }
			if (status.value !== 'ALL') params.status = status.value
			const res = await crmApi.leadPage(params).catch(() => ({}))
			list.value = res?.records || []
			// 各状态数量
			const all = await crmApi.leadPage({ current: 1, size: 1 }).catch(() => ({}))
			statusTabs.value[0].count = all?.total || 0
		} finally {
			loading.value = false
		}
	}

	const onTab = (value) => {
		status.value = value
		load()
	}

	const goDetail = (item) => router.push(`/app/enroll/lead?id=${item.id}`)

	onMounted(load)
</script>

<style lang="less" scoped>
	.m-filter {
		display: flex;
		gap: 8px;
		overflow-x: auto;
		padding-bottom: 10px;
		margin-bottom: 2px;
		white-space: nowrap;
	}
	.m-filter-item {
		flex-shrink: 0;
		padding: 6px 12px;
		font-size: 13px;
		color: var(--text-color-secondary);
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: 16px;
		&.active {
			color: #fff;
			background: var(--primary-6);
			border-color: var(--primary-6);
		}
	}
	.m-filter-count {
		margin-left: 4px;
		opacity: 0.75;
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-lead-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
	}
	.m-lead-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-lead-sub {
		margin-top: 4px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-lead-foot {
		margin-top: 8px;
		display: flex;
		justify-content: space-between;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.m-lead-next {
		color: var(--primary-6);
	}
	.m-empty {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 30px 12px;
		text-align: center;
		color: var(--text-color-secondary);
	}
</style>
