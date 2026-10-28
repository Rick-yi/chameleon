<template>
	<a-drawer :open="open" title="导出记录" :width="560" :body-style="{ padding: '12px 16px' }" @close="onClose">
		<a-spin :spinning="loading">
			<a-empty v-if="!loading && !list.length" description="暂无导出记录" />
			<div v-for="item in list" :key="item.id" class="rec-row">
				<div class="rec-main">
					<div class="rec-title">{{ item.fileName || typeLabel(item.exportType) }}</div>
					<div class="rec-sub">
						<a-tag :color="statusColor(item.status)" style="margin-right: 6px">{{ statusText(item.status) }}</a-tag>
						{{ item.createTime }}
						<span v-if="item.rowCount"> · {{ item.rowCount }} 行</span>
					</div>
					<div v-if="item.status === 'FAILED' && item.errorMsg" class="rec-err">{{ item.errorMsg }}</div>
				</div>
				<a-button v-if="item.status === 'SUCCESS'" type="link" size="small" @click="download(item)"> 下载 </a-button>
			</div>
		</a-spin>
	</a-drawer>
</template>

<script setup name="exportRecordsDrawer">
	import { message } from 'ant-design-vue'
	import exportApi from '@/api/biz/exportApi'
	import downloadUtil from '@/utils/downloadUtil'

	const props = defineProps({
		open: {
			type: Boolean,
			default: false
		},
		exportType: {
			type: String,
			default: ''
		}
	})
	const emit = defineEmits(['update:open'])

	const loading = ref(false)
	const list = ref([])

	const TYPE_LABEL = {
		COURSE_ACADEMIC: '教务报表',
		CRM_REPORT: '招生报表'
	}

	const typeLabel = (type) => TYPE_LABEL[type] || type || '导出文件'
	const statusText = (status) =>
		({ PENDING: '排队中', RUNNING: '生成中', SUCCESS: '已完成', FAILED: '失败' })[status] || status
	const statusColor = (status) =>
		({ PENDING: 'default', RUNNING: 'processing', SUCCESS: 'success', FAILED: 'error' })[status]

	const load = () => {
		loading.value = true
		exportApi
			.page({ current: 1, size: 30, exportType: props.exportType || undefined })
			.then((data) => {
				list.value = data?.records || []
			})
			.finally(() => {
				loading.value = false
			})
	}

	const download = (item) => {
		exportApi.download({ id: item.id }).then((res) => {
			downloadUtil.resultDownload(res)
		})
	}

	const onClose = () => emit('update:open', false)

	watch(
		() => props.open,
		(value) => {
			if (value) load()
		}
	)
</script>

<style lang="less" scoped>
	.rec-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 0;
		border-bottom: 1px solid var(--border-color-split);
	}

	.rec-main {
		flex: 1;
		min-width: 0;
	}

	.rec-title {
		font-size: 14px;
		font-weight: 500;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.rec-sub {
		font-size: 12px;
		color: var(--text-color-secondary);
		margin-top: 4px;
	}

	.rec-err {
		font-size: 12px;
		color: #ff4d4f;
		margin-top: 4px;
	}
</style>
