<template>
	<div>
		<div class="m-filter">
			<div
				v-for="item in tabs"
				:key="item.value"
				class="m-filter-item"
				:class="{ active: status === item.value }"
				@click="onTab(item.value)"
			>
				{{ item.text }}
			</div>
		</div>

		<div v-if="hasPerm('/student/renewWarn/scan')" class="m-scan">
			<a-button size="small" :loading="scanning" @click="onScan">立即扫描</a-button>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!list.length" class="m-empty">没有{{ statusText(status) }}的续费预警</div>

		<div v-for="item in list" :key="item.id" class="m-card m-renew">
			<div class="m-renew-head">
				<div class="m-renew-name">{{ item.studentName || '学员' }}</div>
				<a-tag :color="item.warnReason === 'LESSON_LOW' ? 'red' : 'orange'">
					{{ item.warnReason === 'LESSON_LOW' ? '课时不足' : '即将到期' }}
				</a-tag>
			</div>
			<div class="m-renew-sub">
				{{ item.courseName || '课程' }} ·
				预警时剩余 {{ item.remainLessons == null ? '-' : item.remainLessons }} 课时
				<span v-if="item.currentRemainLessons != null"> / 当前 {{ item.currentRemainLessons }} 课时</span>
			</div>
			<div v-if="item.currentRenewed && item.status === 'PENDING'" class="m-renew-tag">
				<a-tag color="green">课时已补充，建议核对后处理</a-tag>
			</div>
			<div class="m-renew-meta">
				<span>生成 {{ formatTime(item.createTime) }}</span>
				<span v-if="item.lastNotifyTime">最近提醒 {{ formatTime(item.lastNotifyTime) }}</span>
				<span v-if="item.followUserName">跟进人 {{ item.followUserName }}</span>
			</div>
			<div v-if="item.status === 'PENDING'" class="m-renew-actions">
				<a-button v-if="hasPerm('/student/order/add')" type="primary" size="small" @click="onRenew(item)">续费</a-button>
				<a-button size="small" @click="onNotify(item)">提醒</a-button>
				<a-button size="small" @click="onProcess(item)">已处理</a-button>
				<a-button size="small" @click="onIgnore(item)">忽略</a-button>
			</div>
			<div v-else class="m-renew-status">{{ statusText(item.status) }}</div>
		</div>

		<enrollment-flow v-model:open="renewOpen" :student="renewStudent" @success="onRenewSuccess" />
	</div>
</template>

<script setup name="appStudentRenew">
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import renewApi from '@/api/student/renewApi'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'

	const loading = ref(false)
	const scanning = ref(false)
	const status = ref('PENDING')
	const list = ref([])
	const renewOpen = ref(false)
	const renewStudent = ref(null)
	const tabs = [
		{ value: 'PENDING', text: '待处理' },
		{ value: 'PROCESSED', text: '已处理' },
		{ value: 'RENEWED', text: '已续费' },
		{ value: 'IGNORED', text: '已忽略' }
	]

	const statusText = (value) =>
		({ PENDING: '待处理', PROCESSED: '已处理', RENEWED: '已续费', IGNORED: '已忽略' }[value] || value)
	const formatTime = (value) => (value ? String(value).slice(5, 16).replace('T', ' ') : '-')

	const load = async () => {
		loading.value = true
		try {
			const res = await renewApi
				.renewWarnPage({ current: 1, size: 50, status: status.value })
				.catch(() => ({}))
			list.value = res?.records || []
		} finally {
			loading.value = false
		}
	}

	const onTab = (value) => {
		status.value = value
		load()
	}

	const onScan = async () => {
		scanning.value = true
		try {
			await renewApi.renewWarnScan()
			message.success('扫描完成')
			load()
		} finally {
			scanning.value = false
		}
	}

	const onRenew = async (item) => {
		const res = await studentApi.studentDetail({ id: item.studentId }).catch(() => null)
		if (!res) {
			message.warning('学员信息加载失败，请稍后重试')
			return
		}
		renewStudent.value = res
		renewOpen.value = true
	}

	const onRenewSuccess = () => {
		renewOpen.value = false
		message.success('续费成功，课时已入账')
		load()
	}

	const onIgnore = async (item) => {
		await renewApi.renewWarnIgnore({ id: item.id })
		message.success('已忽略该预警')
		load()
	}

	const onNotify = async (item) => {
		await renewApi.renewWarnNotify({ id: item.id })
		message.success('提醒已记录')
		load()
	}

	const onProcess = async (item) => {
		await renewApi.renewWarnProcess({ id: item.id })
		message.success('已标记处理')
		load()
	}

	load()
</script>

<style lang="less" scoped>
	.m-filter {
		display: flex;
		gap: 8px;
		margin-bottom: 10px;
	}
	.m-filter-item {
		flex: 1;
		text-align: center;
		padding: 9px 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
		&.active {
			color: #fff;
			background: var(--primary-6);
			border-color: var(--primary-6);
		}
	}
	.m-scan {
		display: flex;
		justify-content: flex-end;
		margin-bottom: 10px;
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-renew-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
	}
	.m-renew-name {
		font-size: 15px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-renew-sub {
		margin-top: 4px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-renew-tag {
		margin-top: 4px;
	}
	.m-renew-meta {
		display: flex;
		gap: 12px;
		flex-wrap: wrap;
		margin-top: 4px;
		font-size: 11px;
		color: var(--text-color-tertiary);
	}
	.m-renew-actions {
		display: flex;
		justify-content: flex-end;
		gap: 8px;
		margin-top: 10px;
	}
	.m-renew-status {
		margin-top: 8px;
		text-align: right;
		font-size: 12px;
		color: var(--text-color-tertiary);
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
