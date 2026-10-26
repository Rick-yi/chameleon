<template>
	<div>
		<!-- 搜索与新增：名册页直接录入，不用回首页 -->
		<div class="m-student-toolbar">
			<a-input-search
				v-model:value="keyword"
				placeholder="姓名 / 家长手机"
				allow-clear
				@search="load"
			/>
			<a-button v-if="hasPerm('/student/add')" type="primary" @click="openCreate">新增学员</a-button>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!list.length" class="m-empty">
			<div>没有匹配的学员</div>
			<a-button v-if="hasPerm('/student/add')" type="primary" style="margin-top: 10px" @click="openCreate">
				新增学员
			</a-button>
		</div>

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
			<div class="m-student-actions">
				<a-button v-if="hasPerm('/student/order/add')" size="small" @click.stop="openEnroll(item)">报名</a-button>
				<a-icon class="m-student-arrow" @click.stop="openDetail(item)"><right-outlined /></a-icon>
			</div>
		</div>

		<enrollment-flow v-model:open="enrollOpen" :student="enrollStudent" @success="onEnrollSuccess" />

		<a-drawer v-model:open="detailOpen" :width="360" placement="right" title="学员详情" :body-style="{ padding: '12px' }">
			<div v-if="currentStudent">
				<div class="m-detail-name">{{ currentStudent.name }}</div>
				<div class="m-detail-sub">
					{{ currentStudent.gender }} · 家长 {{ currentStudent.parentName || '-' }} {{ currentStudent.parentPhone || '' }}
				</div>
				<div class="m-detail-grid">
					<div><span>剩余课时</span><b>{{ remainText }}</b></div>
					<div><span>课包到期</span><b>{{ detailDate(currentStudent.accountExpireTime) }}</b></div>
					<div><span>所在班级</span><b>{{ currentStudent.classNames || '未分班' }}</b></div>
					<div><span>待收款</span><b>¥{{ Number(currentStudent.unpaidAmount || 0).toFixed(2) }}</b></div>
				</div>
				<a-button v-if="hasPerm('/student/order/add')" type="primary" block class="m-detail-enroll" @click="enrollFromDetail">
					报名 / 续费
				</a-button>
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
	import { useRoute } from 'vue-router'
	import studentApi from '@/api/student/studentApi'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'

	const route = useRoute()
	const loading = ref(false)
	const keyword = ref('')
	const list = ref([])
	const detailOpen = ref(false)
	const currentStudent = ref(null)
	const accountList = ref([])
	const enrollOpen = ref(false)
	const enrollStudent = ref(null)

	const statusText = (status) => ({ STUDYING: '在读', STOP: '停课', REFUNDED: '已退费' }[status] || status)
	const statusColor = (status) => (status === 'STUDYING' ? 'green' : status === 'STOP' ? 'orange' : 'red')
	const detailDate = (value) => (value ? String(value).slice(0, 10) : '-')
	const remainText = computed(() => {
		if (!currentStudent.value) return '-'
		if (currentStudent.value.hasLessonAccount === false) return '未报名'
		return `${Number(currentStudent.value.remainLessons || 0)} 课时`
	})

	const load = async () => {
		loading.value = true
		try {
			const res = await studentApi.studentPage({ current: 1, size: 50, searchKey: keyword.value }).catch(() => ({}))
			list.value = res?.records || []
		} finally {
			loading.value = false
		}
	}

	const loadAccounts = async (studentId) => {
		const res = await studentApi
			.lessonAccountPage({ current: 1, size: 20, studentId })
			.catch(() => ({}))
		accountList.value = res?.records || []
	}

	const openDetail = (item) => {
		currentStudent.value = item
		detailOpen.value = true
		loadAccounts(item.id)
	}

	/** 支持从报名成功页/其它页面带 ?id= 直接打开学员详情 */
	const openDetailById = async (id) => {
		const data = await studentApi.studentDetail({ id }).catch(() => null)
		if (!data) return
		openDetail(data)
	}

	const openCreate = () => {
		enrollStudent.value = null
		enrollOpen.value = true
	}

	const openEnroll = (item) => {
		enrollStudent.value = item
		enrollOpen.value = true
	}

	const enrollFromDetail = () => {
		detailOpen.value = false
		openEnroll(currentStudent.value)
	}

	const onEnrollSuccess = () => {
		enrollOpen.value = false
		load()
	}

	onMounted(async () => {
		await load()
		if (route.query.id) openDetailById(String(route.query.id))
	})
</script>

<style lang="less" scoped>
	.m-student-toolbar {
		display: flex;
		align-items: center;
		gap: 8px;
		margin-bottom: 12px;
		:deep(.ant-input-search) {
			flex: 1;
			min-width: 0;
		}
		:deep(.ant-input) {
			font-size: 16px;
		}
		:deep(.ant-btn) {
			flex: none;
			height: 40px;
			padding: 0 14px;
		}
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
	.m-student-actions {
		display: flex;
		align-items: center;
		gap: 8px;
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
	.m-detail-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 8px;
		margin-top: 12px;
		padding: 10px 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
		div {
			display: flex;
			flex-direction: column;
			gap: 2px;
			min-width: 0;
		}
		span {
			font-size: 11px;
			color: var(--text-color-secondary);
		}
		b {
			font-size: 13px;
			font-weight: 600;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
	}
	.m-detail-enroll {
		height: 42px;
		margin-top: 12px;
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
