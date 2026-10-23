<template>
	<div>
		<page-header title="点名消课" desc="选择课节即可点名，按课包规则自动扣减课时" />
		<!-- 今日/所选日期的课节：点一下直接进入点名 -->
		<a-card :bordered="false" title="课节列表" style="margin-bottom: 12px">
			<div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 12px">
				<a-date-picker v-model:value="date" value-format="YYYY-MM-DD" style="width: 160px" @change="loadSchedules" />
				<a-select v-model:value="classId" placeholder="全部班级" :options="classOptions" style="width: 220px" allow-clear @change="loadSchedules" />
				<a-button type="primary" @click="loadSchedules">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
				<span style="color: rgba(0,0,0,.45); font-size: 12px">点击课节卡片即可进入点名</span>
			</div>
			<div v-if="!scheduleList.length" class="ent-empty">
				该日期没有排课
				<a v-if="recentHint" style="margin-left: 8px" @click="jumpToRecent">最近有课：{{ recentHint.date }}（查看）</a>
			</div>
			<a-row v-else :gutter="12">
				<a-col v-for="item in scheduleList" :key="item.id" :xs="24" :sm="12" :xl="8">
					<div
						class="attend-slot"
						:class="{ done: item.attendStatus === 'DONE', active: item.id === scheduleId }"
						@click="selectSchedule(item)"
					>
						<div class="attend-slot-title">{{ item.className || '未命名班级' }}</div>
						<div class="attend-slot-sub">
							{{ item.startTime }} - {{ item.endTime }} · {{ item.classroom || '未定教室' }}
							{{ item.teacherName ? ' · ' + item.teacherName : '' }}
						</div>
						<div class="attend-slot-tag">
							<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
							<span v-if="item.studentCount" style="font-size: 12px; color: rgba(0,0,0,.45)">应到 {{ item.studentCount }} 人</span>
						</div>
					</div>
				</a-col>
			</a-row>
		</a-card>

		<a-card :bordered="false">
		<div class="attend-current">
			<template v-if="currentSchedule">
				<span class="attend-current-title">{{ currentSchedule.className || '未命名班级' }}</span>
				<span class="attend-current-meta">
					{{ (currentSchedule.scheduleDate || '').slice(0, 10) }}
					{{ currentSchedule.startTime }}-{{ currentSchedule.endTime }}
					{{ currentSchedule.classroom ? '· ' + currentSchedule.classroom : '' }}
				</span>
				<a-button type="link" size="small" @click="scheduleId = undefined">换一节</a-button>
			</template>
			<span v-else style="color: rgba(0,0,0,.45); font-size: 13px">尚未选择课节</span>
			<div style="margin-left: auto">
				<span v-if="scheduleId" style="color: rgba(0,0,0,.45); font-size: 12px">点击方块即可切换出勤状态</span>
			</div>
		</div>
		<div style="padding: 16px 0">
			<div v-if="!scheduleId" class="ent-empty">请在上方选择要点的课节，选中后这里会列出该课节的学员名单</div>
			<template v-else>
				<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 8px; flex-wrap: wrap">
					<span style="font-weight: 500">应到 {{ students.length }} 人</span>
					<a-space>
						<a-button @click="markAll('ATTEND')">全部到课</a-button>
						<a-button @click="markAll('LEAVE')">整班请假</a-button>
						<a-button @click="markAll('ABSENT')">整班缺勤</a-button>
						<a-button v-if="hasPerm('/course/attend/confirm')" type="primary" @click="onConfirm">确认点名并扣课</a-button>
					</a-space>
				</div>
				<div class="attend-legend">
					<span class="attend-chip st-attend">到课 {{ countByStatus('ATTEND') }}</span>
					<span class="attend-chip st-leave">请假 {{ countByStatus('LEAVE') }}</span>
					<span class="attend-chip st-absent">缺勤 {{ countByStatus('ABSENT') }}</span>
					<span class="attend-tip">点击学员方块切换：到课 → 缺勤 → 请假</span>
				</div>
				<div class="attend-grid">
					<div
						v-for="record in students"
						:key="record.studentId"
						class="attend-tile"
						:class="[statusClass(record.attendStatus), { locked: !!record.attendId }]"
						@click="toggleStudent(record)"
					>
						<div class="attend-tile-name">
							<span>{{ studentName(record.studentId) }}</span>
							<a-tag v-if="record.attendId" class="attend-tile-locked">已点名</a-tag>
						</div>
						<div class="attend-tile-sub">
							<span>{{ studentPhone(record.studentId) }}</span>
							<span v-if="!balanceMap[record.studentId]" class="attend-balance is-danger">未购课</span>
							<span v-else-if="Number(balanceMap[record.studentId].remainLessons) < 1.5" class="attend-balance is-warn">
								剩 {{ balanceMap[record.studentId].remainLessons }}
							</span>
							<span v-else class="attend-balance">剩 {{ balanceMap[record.studentId].remainLessons }}</span>
						</div>
						<div class="attend-tile-status">
							<span class="dot" />
							{{ statusText(record.attendStatus) }}
							<a v-if="record.attendId" class="attend-undo" @click.stop="onCancelAttend(record)">撤销</a>
						</div>
					</div>
				</div>
			</template>
		</div>
		</a-card>
	</div>
</template>

<script setup name="courseAttend">
	import { useRoute } from 'vue-router'
	import { message } from 'ant-design-vue'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	const route = useRoute()
	const classId = ref()
	// 默认今天（按本地时区计算，避免 toISOString 在东八区凌晨回退到昨天）
	const pad = (n) => String(n).padStart(2, '0')
	const todayText = () => {
		const d = new Date()
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}
	const date = ref(todayText())
	const scheduleId = ref()
	const classOptions = ref([])
	const classRecords = ref([])
	const scheduleOptions = ref([])
	const scheduleList = ref([])
	const students = ref([])
	const studentMap = ref({})
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')
	const selectSchedule = (item) => {
		scheduleId.value = item.id
		classId.value = item.classId
		loadAttendPage()
	}
	const loadClasses = () => {
		courseApi.classPage({ current: 1, size: 200 }).then((data) => {
			classRecords.value = data.records || []
			classOptions.value = classRecords.value.map((item) => ({ value: item.id, label: item.name }))
		})
	}
	// 默认日期始终是今天；今天没课时只提示「最近有课」，不自动改日期
	const recentHint = ref(null)
	const resolvingTarget = ref(false)
	const fmtDay = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	const loadSchedules = () => {
		// 切换日期/班级后先清空当前点名对象，避免显示上一节课的名单
		scheduleId.value = undefined
		students.value = []
		recentHint.value = null
		const dayStart = `${date.value} 00:00:00`
		const dayEnd = `${date.value} 23:59:59`
		courseApi
			.scheduleWeek({ classId: classId.value, startDate: dayStart, endDate: dayEnd })
			.then((data) => {
				scheduleList.value = data || []
				scheduleOptions.value = scheduleList.value.map((item) => ({
					value: item.id,
					label: `${item.className || '未命名班级'} ${item.startTime}-${item.endTime} ${item.classroom || ''}`
				}))
				// 从工作台/课表直达：目标课节可能不在当前日期
				const targetId = route.query.scheduleId
				if (targetId) {
					const target = scheduleList.value.find((item) => item.id === targetId)
					if (target) {
						selectSchedule(target)
						return
					}
					if (!resolvingTarget.value) {
						resolvingTarget.value = true
						const start = new Date()
						start.setDate(start.getDate() - 30)
						const end = new Date()
						end.setDate(end.getDate() + 7)
						courseApi
							.scheduleWeek({ startDate: `${fmtDay(start)} 00:00:00`, endDate: `${fmtDay(end)} 23:59:59` })
							.then((list) => {
								const found = (list || []).find((item) => item.id === targetId)
								if (found) {
									date.value = String(found.scheduleDate || '').slice(0, 10)
									loadSchedules()
								}
							})
							.catch(() => {})
					}
				}
				// 当天没有排课：只查一下最近有课的日期做提示，不改变当前选择
				if (!scheduleList.value.length && !resolvingTarget.value) {
					const end = new Date(`${date.value} 00:00:00`)
					const start = new Date(end)
					start.setDate(start.getDate() - 7)
					courseApi
						.scheduleWeek({
							classId: classId.value,
							startDate: `${fmtDay(start)} 00:00:00`,
							endDate: `${fmtDay(end)} 23:59:59`
						})
						.then((recent) => {
							const dateList = [...new Set((recent || []).map((item) => String(item.scheduleDate || '').slice(0, 10)).filter(Boolean))].sort()
							if (dateList.length) {
								recentHint.value = { date: dateList[dateList.length - 1] }
							}
						})
						.catch(() => {})
					return
				}
				// 默认选中第一个课节，少一次点击
				if (!scheduleId.value && scheduleList.value.length) {
					selectSchedule(scheduleList.value[0])
				}
			})
	}
	const jumpToRecent = () => {
		if (!recentHint.value) return
		date.value = recentHint.value.date
		loadSchedules()
	}
	const loadStudents = () => {
		studentApi.studentPage({ current: 1, size: 200 }).then((data) => {
			const map = {}
			for (const s of data.records || []) map[s.id] = s
			studentMap.value = map
		})
	}
	const studentName = (id) => {
		const s = studentMap.value[id]
		return s ? `${s.name}(${s.parentPhone})` : id
	}
	const balanceMap = ref({})
	// 加载学员在该课程下的剩余课时（没有余额 = 还没买这门课）
	const loadBalance = (studentIdList, courseId) => {
		if (!studentIdList.length || !courseId) {
			balanceMap.value = {}
			return Promise.resolve()
		}
		return studentApi
			.lessonBalance({ studentIds: studentIdList.join(','), courseId })
			.then((list) => {
				const map = {}
				for (const item of list || []) map[item.studentId] = item
				balanceMap.value = map
			})
			.catch(() => (balanceMap.value = {}))
	}
	// 当前选中的课节（用于表头展示，避免再放一个重复的下拉）
	const currentSchedule = computed(() => {
		const item = scheduleList.value.find((s) => s.id === scheduleId.value)
		return item || null
	})
	const loadAttendPage = () => {
		if (!scheduleId.value) return
		Promise.all([
			courseApi.classStudentPage({ current: 1, size: 200, classId: classId.value }),
			courseApi.attendPage({ current: 1, size: 200, scheduleId: scheduleId.value })
		]).then(([classStudentData, attendData]) => {
			const attendMap = {}
			for (const a of attendData.records || []) attendMap[a.studentId] = a
			const list = (classStudentData.records || []).filter((cs) => cs.status === 'STUDYING').map((cs) => ({
				studentId: cs.studentId,
				attendStatus: attendMap[cs.studentId] ? attendMap[cs.studentId].attendStatus : 'ATTEND',
				attendId: attendMap[cs.studentId] ? attendMap[cs.studentId].id : undefined
			}))
			students.value = list
			const cls = classRecords.value.find((item) => item.id === classId.value)
			return loadBalance(list.map((item) => item.studentId), cls?.courseId)
		})
	}
	// 统计各状态人数
	const countByStatus = (status) => students.value.filter((s) => s.attendStatus === status && !s.attendId).length
	const statusText = (status) => ({ ATTEND: '到课', LEAVE: '请假', ABSENT: '缺勤' }[status] || '待标记')
	const statusClass = (status) =>
		status === 'ATTEND' ? 'st-attend' : status === 'LEAVE' ? 'st-leave' : 'st-absent'
	const studentPhone = (id) => studentMap.value[id]?.parentPhone || ''
	// 方块点击：默认到课，点一下变缺勤，再点变请假，再点回到课
	const toggleStudent = (record) => {
		if (record.attendId) {
			message.info('已点名，如需修改请先撤销')
			return
		}
		record.attendStatus =
			record.attendStatus === 'ATTEND' ? 'ABSENT' : record.attendStatus === 'ABSENT' ? 'LEAVE' : 'ATTEND'
	}
	// 快捷标记：只影响还没点名的学员
	const markAll = (status) => {
		for (const item of students.value) {
			if (!item.attendId) {
				item.attendStatus = status
			}
		}
	}
	const onConfirm = () => {
		const attendList = students.value
			.filter((s) => !s.attendId)
			.map((s) => ({ studentId: s.studentId, attendStatus: s.attendStatus }))
		if (attendList.length === 0) {
			message.info('本节没有需要点名的学员')
			return
		}
		courseApi
			.attendConfirm({ scheduleId: scheduleId.value, attendList })
			.then((res) => {
				const result = res?.data
				const skipped = result?.skippedList || []
				if (skipped.length) {
					// 课时不足的学员不阻断整班点名，单独提示
					message.warning(
						`点名完成，${result.deductCount} 人已消课；${skipped.length} 人未消课：` +
							skipped.map((item) => `${item.studentName}（${item.reason}）`).join('；')
					)
				} else {
					message.success(`点名完成，${result?.deductCount ?? attendList.length} 人已消课`)
				}
				loadAttendPage()
			})
	}
	const onCancelAttend = (record) => {
		courseApi.attendCancel({ id: record.attendId }).then(() => loadAttendPage())
	}
	loadClasses()
	loadStudents()
	loadSchedules()
</script>

<style lang="less" scoped>
	.attend-current {
		display: flex;
		align-items: center;
		gap: 10px;
		flex-wrap: wrap;
		padding-bottom: 12px;
		border-bottom: 1px solid var(--border-color-split);
		margin-bottom: 4px;
	}
	.attend-current-title {
		font-size: 15px;
		font-weight: 600;
		color: var(--text-color);
	}
	.attend-current-meta {
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.attend-slot {
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 10px 12px;
		margin-bottom: 12px;
		cursor: pointer;
		transition: all 0.2s;
		&:hover {
			border-color: var(--primary-4);
			box-shadow: 0 2px 8px rgba(22, 104, 220, 0.12);
		}
		/* 选中的课节：明确高亮，用户一眼知道正在点哪一节 */
		&.active {
			background: var(--primary-1);
			border-color: var(--primary-6);
			box-shadow: 0 0 0 2px rgba(22, 104, 220, 0.12);
		}
		&.done {
			background: #f2fbf6;
			border-color: #b7eb8f;
		}
		&.done.active {
			background: var(--primary-1);
			border-color: var(--primary-6);
		}
	}
	.attend-slot-title {
		font-size: 14px;
		font-weight: 600;
	}
	.attend-slot-sub {
		font-size: 12px;
		color: var(--text-color-secondary);
		margin: 4px 0 6px;
	}
	.attend-slot-tag {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.attend-legend {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-wrap: wrap;
		margin-bottom: 12px;
		font-size: 12px;
	}
	.attend-chip {
		padding: 2px 10px;
		border-radius: 10px;
		border: 1px solid;
		&.st-attend {
			color: #2ba471;
			background: #f6ffed;
			border-color: #b7eb8f;
		}
		&.st-leave {
			color: #b07d00;
			background: #fffbe6;
			border-color: #ffe58f;
		}
		&.st-absent {
			color: #e5484d;
			background: #fff1f0;
			border-color: #ffa39e;
		}
	}
	.attend-tip {
		color: var(--text-color-secondary);
		margin-left: 4px;
	}
	.attend-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
		gap: 12px;
	}
	.attend-tile {
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px;
		background: #fff;
		cursor: pointer;
		user-select: none;
		transition: transform 0.15s, box-shadow 0.15s, background 0.15s;
		&:hover {
			transform: translateY(-1px);
			box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
		}
		&.st-attend {
			background: #f6ffed;
			border-color: #b7eb8f;
		}
		&.st-leave {
			background: #fffbe6;
			border-color: #ffe58f;
		}
		&.st-absent {
			background: #fff1f0;
			border-color: #ffa39e;
		}
		&.locked {
			cursor: default;
			opacity: 0.88;
			&:hover {
				transform: none;
				box-shadow: none;
			}
		}
	}
	.attend-tile-name {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 6px;
		font-size: 14px;
		font-weight: 600;
	}
	.attend-tile-locked {
		font-size: 11px;
		line-height: 16px;
		padding: 0 4px;
		margin: 0;
	}
	.attend-tile-sub {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		margin-top: 6px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.attend-balance {
		white-space: nowrap;
		&.is-warn {
			color: #d48806;
		}
		&.is-danger {
			color: #e5484d;
		}
	}
	.attend-tile-status {
		display: flex;
		align-items: center;
		gap: 6px;
		margin-top: 8px;
		font-size: 12px;
		color: var(--text-color);
	}
	.attend-tile-status .dot {
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background: currentColor;
	}
	.attend-undo {
		margin-left: auto;
		font-size: 12px;
		color: #e5484d;
	}
</style>
