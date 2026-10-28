<template>
	<div>
		<div class="m-card m-msg-toolbar">
			<span>共 {{ total }} 条 · 未读 {{ unreadCount }} 条</span>
			<a v-if="unreadCount > 0" @click="markAllRead">全部已读</a>
		</div>

		<a-spin :spinning="loading">
			<div v-if="!list.length && !loading" class="m-empty">暂无消息</div>
			<div v-for="item in list" :key="item.id" class="m-card m-msg" @click="openDetail(item)">
				<div class="m-msg-head">
					<span class="m-msg-dot" :class="{ unread: !item.read }" />
					<span class="m-msg-title">{{ item.subject }}</span>
					<span class="m-msg-time">{{ shortTime(item.createTime) }}</span>
				</div>
				<div class="m-msg-desc">{{ item.content }}</div>
			</div>
			<a-button v-if="hasMore" block :loading="loading" style="margin-top: 12px" @click="loadMore"> 加载更多 </a-button>
		</a-spin>

		<a-modal v-model:open="detailOpen" title="消息详情" :footer="null" :width="'92%'" style="max-width: 480px">
			<div class="m-detail-title">{{ detail.subject }}</div>
			<div class="m-detail-time">{{ detail.createTime }}</div>
			<div class="m-detail-content">{{ detail.content }}</div>
		</a-modal>
	</div>
</template>

<script setup name="appMessage">
	import { message } from 'ant-design-vue'
	import indexApi from '@/api/sys/indexApi'

	const list = ref([])
	const loading = ref(false)
	const current = ref(1)
	const size = 20
	const total = ref(0)
	const unreadCount = ref(0)
	const hasMore = computed(() => list.value.length < total.value)
	const detailOpen = ref(false)
	const detail = ref({})

	const load = (reset) => {
		if (reset) {
			current.value = 1
			list.value = []
		}
		loading.value = true
		indexApi
			.indexMessagePage({ current: current.value, size })
			.then((data) => {
				const records = (data?.records || []).map((item) => ({ ...item }))
				list.value = reset ? records : [...list.value, ...records]
				total.value = Number(data?.total || 0)
				unreadCount.value = list.value.filter((item) => !item.read).length
			})
			.finally(() => {
				loading.value = false
			})
	}

	const loadMore = () => {
		current.value += 1
		load(false)
	}

	const openDetail = (item) => {
		detail.value = item
		detailOpen.value = true
		indexApi.indexMessageDetail({ id: item.id }).then((data) => {
			detail.value = { ...item, ...data }
			item.read = true
			unreadCount.value = list.value.filter((row) => !row.read).length
		})
	}

	const markAllRead = () => {
		indexApi.indexMessageAllMarkRead().then(() => {
			list.value.forEach((item) => (item.read = true))
			unreadCount.value = 0
			message.success('已全部标记为已读')
		})
	}

	const shortTime = (text) => {
		if (!text) return ''
		// 今天只显示时分，其他显示日期
		const today = new Date()
		const pad = (n) => String(n).padStart(2, '0')
		const todayText = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
		return String(text).startsWith(todayText) ? String(text).slice(11, 16) : String(text).slice(5, 10)
	}

	onMounted(() => load(true))
</script>

<style lang="less" scoped>
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 14px;
	}

	.m-msg-toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 13px;
		color: var(--text-color-secondary);
		margin-bottom: 12px;
		a {
			color: var(--primary-6);
		}
	}

	.m-empty {
		text-align: center;
		color: var(--text-color-secondary);
		padding: 40px 0;
		font-size: 13px;
	}

	.m-msg {
		margin-bottom: 10px;
		cursor: pointer;
		&:active {
			background: var(--fill-quaternary);
		}
	}

	.m-msg-head {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.m-msg-dot {
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background: #d9d9d9;
		flex: none;
		&.unread {
			background: var(--primary-6);
		}
	}

	.m-msg-title {
		flex: 1;
		min-width: 0;
		font-size: 14px;
		font-weight: 500;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.m-msg-time {
		font-size: 12px;
		color: var(--text-color-secondary);
		flex: none;
	}

	.m-msg-desc {
		margin-top: 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.m-detail-title {
		font-size: 16px;
		font-weight: 600;
	}

	.m-detail-time {
		font-size: 12px;
		color: var(--text-color-secondary);
		margin: 6px 0 12px;
	}

	.m-detail-content {
		font-size: 14px;
		line-height: 1.7;
		white-space: pre-wrap;
	}
</style>
