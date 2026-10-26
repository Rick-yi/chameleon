<template>
	<div class="m-attend">
		<!-- 课节信息 -->
		<div class="m-attend-head">
			<div class="m-attend-title">{{ scheduleInfo?.className || '未命名班级' }}</div>
			<div class="m-attend-sub">
				{{ (scheduleInfo?.scheduleDate || '').slice(0, 10) }}
				{{ String(scheduleInfo?.startTime || '').slice(0, 5) }}-{{ String(scheduleInfo?.endTime || '').slice(0, 5) }}
				{{ scheduleInfo?.classroom ? '· ' + scheduleInfo.classroom : '' }}
			</div>
			<div class="m-attend-count">
				应到 <b>{{ studentList.length }}</b> 人 · 到课 <b class="ok">{{ countBy('ATTEND') }}</b> ·
				请假 <b>{{ countBy('LEAVE') }}</b> · 缺勤 <b class="warn">{{ countBy('ABSENT') }}</b>
			</div>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!studentList.length" class="m-empty">该课节没有学员名单</div>

		<!-- 学员点名 -->
		<div v-for="item in studentList" :key="item.studentId" class="m-attend-row">
			<div class="m-attend-info">
				<div class="m-attend-name">{{ item.studentName || item.studentId }}</div>
				<div class="m-attend-remain">
					<template v-if="item.balance">
						剩 {{ item.balance.remainLessons }} 课时
					</template>
					<template v-else>未购课</template>
				</div>
			</div>
			<div class="m-attend-acts">
				<a-button
					v-for="opt in statusOptions"
					:key="opt.value"
					class="m-attend-btn"
					:class="{ active: item.attendStatus === opt.value, [opt.value]: true }"
					:disabled="!!item.attendId"
					@click="item.attendStatus = opt.value"
				>
					{{ opt.text }}
				</a-button>
			</div>
		</div>

		<!-- 底部操作条 -->
		<div class="m-attend-foot">
			<a-button @click="markAll('ATTEND')">全部到课</a-button>
			<a-button @click="markAll('LEAVE')">整班请假</a-button>
			<a-button v-if="hasPerm('/course/attend/confirm')" type="primary" :loading="submitting" @click="onSubmit">提交点名</a-button>
		</div>
	</div>
</template>

<script setup name="appTeachAttend">
	import { useRoute, useRouter } from 'vue-router'
	import { message } from 'ant-design-vue'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'

	const route = useRoute()
	const router = useRouter()
	const loading = ref(false)
	const submitting = ref(false)
	const scheduleId = ref(route.query.scheduleId || '')
	const classId = ref(route.query.classId || '')
	const scheduleInfo = ref(null)
	const studentList = ref([])
	const classOptions = ref([])
	const classRecords = ref([])

	const statusOptions = [
		{ value: 'ATTEND', text: '到课' },
		{ value: 'LEAVE', text: '请假' },
		{ value: 'ABSENT', text: '缺勤' }
	]

	const countBy = (status) => studentList.value.filter((item) => item.attendStatus === status).length

	const load = async () => {
		loading.value = true
		try {
			// 最近 7 天的课节（用于自动定位最近一节）
			const pad = (n) => String(n).padStart(2, '0')
			const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
			const end = new Date()
			const start = new Date(end.getTime() - 7 * 24 * 3600 * 1000)
			const week = await courseApi
				.scheduleWeek({ startDate: `${fmt(start)} 00:00:00`, endDate: `${fmt(end)} 23:59:59` })
				.catch(() => [])
			// 未指定课节时：自动选中最近一节未点名的课
			if (!scheduleId.value && (week || []).length) {
				const pending = (week || []).find((item) => item.attendStatus !== 'DONE')
				scheduleId.value = (pending || week[0]).id
			}
			if (!scheduleId.value) {
				message.warning('没有可点名的课节，请先在课表排课')
				return
			}
			scheduleInfo.value = (week || []).find((item) => item.id === scheduleId.value) || null
			if (!classId.value && scheduleInfo.value?.classId) classId.value = scheduleInfo.value.classId

			// 名单
			const [classStudentRes, attendRes] = await Promise.all([
				courseApi.classStudentPage({ current: 1, size: 200, classId: classId.value }).catch(() => ({})),
				courseApi.attendPage({ current: 1, size: 200, scheduleId: scheduleId.value }).catch(() => ({}))
			])
			const attendMap = {}
			for (const item of attendRes?.records || []) attendMap[item.studentId] = item
			const list = (classStudentRes?.records || [])
				.filter((item) => item.status === 'STUDYING')
				.map((item) => ({
					studentId: item.studentId,
					studentName: item.studentName,
					attendStatus: attendMap[item.studentId]?.attendStatus || 'ATTEND',
					attendId: attendMap[item.studentId]?.id
				}))
			studentList.value = list
			await loadBalance(list)
		} finally {
			loading.value = false
		}
	}

	// 剩余课时
	const loadBalance = async (list) => {
		if (!list.length) return
		const classRecord = classRecords.value.find((item) => item.id === classId.value)
		if (!classRecord?.courseId) return
		const balanceList = await studentApi
			.lessonBalance({ studentIds: list.map((item) => item.studentId).join(','), courseId: classRecord.courseId })
			.catch(() => [])
		const map = {}
		for (const item of balanceList || []) map[item.studentId] = item
		list.forEach((item) => (item.balance = map[item.studentId]))
	}

	// 班级（取课程 id 用）
	const loadClasses = async () => {
		const res = await courseApi.classPage({ current: 1, size: 200 }).catch(() => ({}))
		classRecords.value = res?.records || []
		classOptions.value = classRecords.value.map((item) => ({ value: item.id, label: item.name }))
	}

	const markAll = (status) => {
		studentList.value.forEach((item) => {
			if (!item.attendId) item.attendStatus = status
		})
	}

	const onSubmit = async () => {
		if (!studentList.value.length) {
			message.warning('没有可点名的学员')
			return
		}
		submitting.value = true
		try {
			const attendList = studentList.value
				.filter((item) => !item.attendId)
				.map((item) => ({ studentId: item.studentId, attendStatus: item.attendStatus }))
			if (!attendList.length) {
				message.info('该课节已完成点名')
				return
			}
			const res = await courseApi.attendConfirm({ scheduleId: scheduleId.value, attendList })
			const skipped = res?.skippedList || []
			if (skipped.length) {
				message.warning(`点名完成，${res?.deductCount ?? attendList.length} 人已消课；${skipped.length} 人未消课：${skipped.map((item) => `${item.studentName}（${item.reason}）`).join('；')}`)
			} else {
				message.success(`点名完成，${res?.deductCount ?? attendList.length} 人已消课`)
			}
			router.back()
		} finally {
			submitting.value = false
		}
	}

	onMounted(async () => {
		await loadClasses()
		await load()
	})
</script>

<style lang="less" scoped>
	.m-attend-head {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 12px;
	}
	.m-attend-title {
		font-size: 16px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-attend-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-attend-count {
		margin-top: 8px;
		font-size: 12px;
		color: var(--text-color-secondary);
		b {
			font-size: 14px;
			color: var(--text-color);
		}
		.ok {
			color: var(--success-text);
		}
		.warn {
			color: var(--danger-text);
		}
	}
	.m-attend-row {
		display: flex;
		align-items: center;
		gap: 10px;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 10px 12px;
		margin-bottom: 8px;
	}
	.m-attend-info {
		flex: 1;
		min-width: 0;
	}
	.m-attend-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-attend-remain {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.m-attend-acts {
		display: flex;
		gap: 6px;
	}
	.m-attend-btn {
		padding: 0 10px;
		font-size: 13px;
		&.active.ATTEND {
			color: #fff;
			background: #2ba471;
			border-color: #2ba471;
		}
		&.active.LEAVE {
			color: #fff;
			background: #d89614;
			border-color: #d89614;
		}
		&.active.ABSENT {
			color: #fff;
			background: #e5484d;
			border-color: #e5484d;
		}
	}
	.m-attend-foot {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 56px;
		z-index: 15;
		display: flex;
		gap: 8px;
		padding: 10px 12px;
		background: #fff;
		border-top: 1px solid var(--border-color-split);
		.ant-btn {
			flex: 1;
			height: 40px;
		}
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
