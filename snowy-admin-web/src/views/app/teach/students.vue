<template>
	<div>
		<!-- 搜索 -->
		<div class="m-search">
			<a-input-search
				v-model:value="keyword"
				placeholder="姓名 / 家长手机"
				allow-clear
				@search="load"
			/>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!list.length" class="m-empty">没有匹配的学员</div>

		<div v-for="item in list" :key="item.id" class="m-card m-student">
			<a-avatar :size="40" style="background: var(--primary-1); color: var(--primary-6)">
				{{ (item.name || '?').slice(0, 1) }}
			</a-avatar>
			<div class="m-student-main">
				<div class="m-student-name">
					{{ item.name }}
					<a-tag :color="statusColor(item.status)" style="margin-left: 6px">{{ statusText(item.status) }}</a-tag>
				</div>
				<div class="m-student-sub">
					{{ item.gender }} · 家长 {{ item.parentName || '-' }} {{ item.parentPhone || '' }}
				</div>
			</div>
			<a-icon class="m-student-arrow" @click.stop="openDetail(item)"><right-outlined /></a-icon>
		</div>

		<a-drawer v-model:open="detailOpen" :width="360" placement="right" title="学员详情" :body-style="{ padding: '12px' }">
			<div v-if="currentStudent">
				<div class="m-detail-name">{{ currentStudent.name }}</div>
				<div class="m-detail-sub">
					{{ currentStudent.gender }} · {{ currentStudent.parentName }} {{ currentStudent.parentPhone }}
				</div>
				<div class="m-section-title">课时账户</div>
				<div v-if="!accountList.length" class="m-empty" style="padding: 16px">暂无课时账户</div>
				<div v-for="acc in accountList" :key="acc.courseId" class="m-card" style="padding: 10px 12px">
					<div class="m-student-name">{{ acc.courseName || acc.courseId }}</div>
					<div class="m-student-sub">
						剩余 <b>{{ acc.remainLessons }}</b> 课时 / 共 {{ acc.totalLessons }} 课时
					</div>
				</div>
			</div>
		</a-drawer>
	</div>
</template>

<script setup name="appTeachStudents">
	import studentApi from '@/api/student/studentApi'

	const loading = ref(false)
	const keyword = ref('')
	const list = ref([])
	const detailOpen = ref(false)
	const currentStudent = ref(null)
	const accountList = ref([])

	const statusText = (status) => ({ STUDYING: '在读', STOP: '停课', REFUNDED: '已退费' }[status] || status)
	const statusColor = (status) => (status === 'STUDYING' ? 'green' : status === 'STOP' ? 'orange' : 'red')

	const load = async () => {
		loading.value = true
		try {
			const res = await studentApi.studentPage({ current: 1, size: 50, searchKey: keyword.value }).catch(() => ({}))
			list.value = res?.records || []
		} finally {
			loading.value = false
		}
	}

	const openDetail = async (item) => {
		currentStudent.value = item
		detailOpen.value = true
		const res = await studentApi
			.lessonAccountPage({ current: 1, size: 20, studentId: item.id })
			.catch(() => ({}))
		accountList.value = res?.records || []
	}

	onMounted(load)
</script>

<style lang="less" scoped>
	.m-search {
		margin-bottom: 12px;
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-student {
		display: flex;
		align-items: center;
		gap: 12px;
	}
	.m-student-main {
		flex: 1;
		min-width: 0;
	}
	.m-student-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-student-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-student-arrow {
		color: var(--text-color-tertiary);
	}
	.m-detail-name {
		font-size: 18px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-detail-sub {
		margin-top: 4px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-section-title {
		margin: 16px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
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
