<template>
	<div class="attend-page">
		<page-header title="点名消课" desc="选择课节即可点名，按课包规则自动扣减课时" />
		<!-- 今日/所选日期的课节：点一下直接进入点名 -->
		<a-card :bordered="false" title="课节列表" style="margin-bottom: 12px">
			<div class="attend-filter">
				<a-date-picker v-model:value="date" value-format="YYYY-MM-DD" class="attend-date" @change="onFilterChange" />
				<a-select
					v-model:value="classId"
					placeholder="全部班级"
					:options="classOptions"
					class="attend-class"
					allow-clear
					@change="onFilterChange"
				/>
				<a-button type="primary" class="attend-refresh" @click="loadSchedules">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
				<span class="attend-filter-tip">点击课节卡片即可进入点名</span>
			</div>
			<a-alert v-if="scheduleError" type="error" show-icon style="margin-bottom: 12px" :message="scheduleError">
				<template #action>
					<a-button size="small" @click="loadSchedules">重试</a-button>
				</template>
			</a-alert>
			<div v-else-if="!scheduleList.length" class="ent-empty">
				该日期没有排课
				<a v-if="recentHint" style="margin-left: 8px" @click="jumpToRecent">最近有课：{{ recentHint.date }}（查看）</a>
			</div>
			<a-row v-else :gutter="12" class="attend-slots">
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
							<span v-if="item.studentCount" style="font-size: 12px; color: rgba(0, 0, 0, 0.45)"
								>应到 {{ item.studentCount }} 人</span
							>
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
						<span class="attend-course-tag">课程：{{ currentCourseName }}</span>
					</span>
					<a-button type="link" size="small" class="attend-switch" @click="scheduleId = undefined">换一节</a-button>
				</template>
				<span v-else style="color: rgba(0, 0, 0, 0.45); font-size: 13px">尚未选择课节</span>
				<div style="margin-left: auto">
					<span v-if="scheduleId" style="color: rgba(0, 0, 0, 0.45); font-size: 12px">点击方块即可切换出勤状态</span>
				</div>
			</div>
			<div style="padding: 16px 0">
				<div v-if="!scheduleId" class="ent-empty">请在上方选择要点的课节，选中后这里会列出该课节的学员名单</div>
				<template v-else>
					<a-alert v-if="attendError" type="error" show-icon style="margin-bottom: 12px" :message="attendError">
						<template #action>
							<a-button size="small" @click="loadAttendPage">重试</a-button>
						</template>
					</a-alert>
					<template v-else>
						<div class="attend-toolbar">
							<span class="attend-toolbar-title">
								应到 {{ students.length }} 人
								<a-tag v-if="savedCount" color="green" style="margin-left: 6px">已点名 {{ savedCount }}</a-tag>
								<a-tag v-if="pendingCount" color="orange" style="margin-left: 4px">本次待提交 {{ pendingCount }}</a-tag>
							</span>
							<div class="attend-actions">
								<a-button @click="markAll('ATTEND')">全部到课</a-button>
								<a-button @click="markAll('LEAVE')">整班请假</a-button>
								<a-button @click="markAll('ABSENT')">整班缺勤</a-button>
								<a-button
									v-if="hasPerm('/course/attend/confirm')"
									type="primary"
									class="attend-confirm"
									:disabled="pendingCount === 0"
									@click="onConfirm"
								>
									确认点名并扣课{{ pendingCount ? `（${pendingCount}）` : '' }}
								</a-button>
							</div>
						</div>
						<a-alert
							v-if="savedCount"
							type="info"
							show-icon
							style="margin-bottom: 12px"
							:message="`本节已有 ${savedCount} 人完成点名；已点名学员为只读，如需修改请先点击方块上的「撤销」。`"
						/>
						<a-alert v-if="scheduleId" type="info" show-icon style="margin-bottom: 12px" :message="accountHint" />
						<a-alert
							v-if="lastResult && lastResult.skipped.length"
							type="warning"
							show-icon
							closable
							style="margin-bottom: 12px"
							@close="lastResult = null"
						>
							<template #message>
								<div>点名完成：{{ lastResult.deductCount }} 人已消课；{{ lastResult.skipped.length }} 人未消课</div>
								<div v-for="item in lastResult.skipped" :key="item.studentId" class="attend-skip-line">
									{{ item.studentName }}：{{ item.reason }}
								</div>
							</template>
						</a-alert>
						<div class="attend-legend">
							<span class="attend-chip st-attend">到课 {{ countByStatus('ATTEND') }}</span>
							<span class="attend-chip st-leave">请假 {{ countByStatus('LEAVE') }}</span>
							<span class="attend-chip st-absent">缺勤 {{ countByStatus('ABSENT') }}</span>
							<span v-if="pendingCount" class="attend-tip">
								本次待提交：到课 {{ pendingCountByStatus('ATTEND') }} · 请假 {{ pendingCountByStatus('LEAVE') }} · 缺勤
								{{ pendingCountByStatus('ABSENT') }}
							</span>
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
									<span v-if="!balanceMap[record.studentId]" class="attend-balance is-danger"
										>未购{{ currentCourseName }}课包</span
									>
									<span
										v-else-if="Number(balanceMap[record.studentId].remainLessons) < 1.5"
										class="attend-balance is-warn"
									>
										剩 {{ balanceMap[record.studentId].remainLessons }}
									</span>
									<span v-else class="attend-balance">剩 {{ balanceMap[record.studentId].remainLessons }}</span>
								</div>
								<div v-if="balanceMap[record.studentId]" class="attend-account-line">
									扣 {{ currentCourseName }} 课时账户{{ sharedCourseBalance ? '（与同课程其他班级共用）' : '' }}
								</div>
								<div class="attend-tile-status">
									<span class="dot" />
									{{ statusText(record.attendStatus) }}
									<a v-if="record.attendId" class="attend-undo" @click.stop="onCancelAttend(record)">撤销</a>
								</div>
							</div>
						</div>
					</template>
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
	const targetScheduleId = ref(route.query.scheduleId ? String(route.query.scheduleId) : null)
	const targetResolved = ref(false)
	const classOptions = ref([])
	const classRecords = ref([])
	const scheduleOptions = ref([])
	const scheduleList = ref([])
	const students = ref([])
	const studentMap = ref({})
	const lastResult = ref(null)
	const scheduleError = ref('')
	const attendError = ref('')
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' })[status] || '待点名'
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' })[status] || 'default'
	const selectSchedule = (item) => {
		scheduleId.value = item.id
		classId.value = item.classId
		loadAttendPage()
	}
	const loadClasses = () => {
		courseApi.classPage({ current: 1, size: 200 }).then((data) => {
			classRecords.value = data.records || []
			classOptions.value = classRecords.value.map((item) => ({
				value: item.id,
				label: item.classType === 'ONE2ONE' && item.studentName ? `${item.name} · ${item.studentName}` : item.name
			}))
		})
	}
	// 默认日期始终是今天；今天没课时只提示「最近有课」，不自动改日期
	const recentHint = ref(null)
	const fmtDay = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

	// 从课表/工作台直达时，先按课节详情定位日期和班级，避免拿“今天”的课节顶替
	const resolveTargetSchedule = () => {
		if (!targetScheduleId.value || targetResolved.value) return Promise.resolve()
		targetResolved.value = true
		if (route.query.date) date.value = String(route.query.date).slice(0, 10)
		if (route.query.classId) classId.value = String(route.query.classId)
		return courseApi
			.scheduleDetail({ id: targetScheduleId.value })
			.then((detail) => {
				if (!detail) return
				date.value = String(detail.scheduleDate || date.value).slice(0, 10)
				classId.value = detail.classId || classId.value
			})
			.catch(() => {})
	}

	const onFilterChange = () => {
		// 用户主动换日期/班级后，不再强制定位原来的课节
		targetScheduleId.value = null
		loadSchedules()
	}

	const loadSchedules = () => {
		// 切换日期/班级后先清空当前点名对象，避免显示上一节课的名单
		scheduleId.value = undefined
		students.value = []
		recentHint.value = null
		scheduleError.value = ''
		scheduleList.value = []
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
				// 指定了目标课节时只认这一节；找不到也不能顺手选中别的课
				if (targetScheduleId.value) {
					const target = scheduleList.value.find((item) => item.id === targetScheduleId.value)
					if (target) {
						selectSchedule(target)
					} else {
						scheduleError.value = '未找到指定课节，可能已被调课或取消，请从最新课表重新进入'
					}
					return
				}
				// 当天没有排课：只查一下最近有课的日期做提示，不改变当前选择
				if (!scheduleList.value.length) {
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
							const dateList = [
								...new Set((recent || []).map((item) => String(item.scheduleDate || '').slice(0, 10)).filter(Boolean))
							].sort()
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
			.catch(() => {
				scheduleList.value = []
				scheduleError.value = '课节加载失败，请检查网络后重试'
			})
	}
	const jumpToRecent = () => {
		if (!recentHint.value) return
		targetScheduleId.value = null
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
	// 消课账户：优先排课上的课程（支持不挂班级的直接排课），其次班级默认课程
	const currentCourseId = computed(
		() => currentSchedule.value?.courseId || classRecords.value.find((item) => item.id === classId.value)?.courseId
	)
	const currentCourseName = computed(() => currentSchedule.value?.courseName || '当前课程')
	// 同一课程下多个班级共用「学员 + 课程」维度的课时账户
	const sharedCourseBalance = computed(() => {
		if (!currentCourseId.value) return false
		return classRecords.value.filter((item) => item.courseId === currentCourseId.value).length > 1
	})
	const accountHint = computed(() => {
		const sharedText = sharedCourseBalance.value ? '同一课程下的多个班级共用同一课时余额，本节扣减的是该课程账户。' : ''
		return `本节课程：${currentCourseName.value}。点名成功将从「学员 + ${currentCourseName.value}」课时账户扣减；${sharedText}余额不足或未购课包时，按现有规则保留出勤但不扣课，不影响其他学员。`
	})
	const loadAttendPage = () => {
		if (!scheduleId.value) return
		lastResult.value = null
		attendError.value = ''
		Promise.all([
			courseApi.classStudentPage({ current: 1, size: 200, classId: classId.value }),
			courseApi.attendPage({ current: 1, size: 200, scheduleId: scheduleId.value })
		])
			.then(([classStudentData, attendData]) => {
				const attendMap = {}
				for (const a of attendData.records || []) attendMap[a.studentId] = a
				const list = (classStudentData.records || [])
					.filter((cs) => cs.status === 'STUDYING')
					.map((cs) => ({
						studentId: cs.studentId,
						attendStatus: attendMap[cs.studentId] ? attendMap[cs.studentId].attendStatus : 'ATTEND',
						attendId: attendMap[cs.studentId] ? attendMap[cs.studentId].id : undefined
					}))
				students.value = list
				// 同步课节卡片的点名状态，确认/撤销后不用刷新整页
				const current = scheduleList.value.find((item) => item.id === scheduleId.value)
				if (current) {
					const saved = list.filter((item) => item.attendId).length
					current.studentCount = list.length
					current.attendCount = saved
					current.attendStatus = saved === 0 ? 'NONE' : saved >= list.length ? 'DONE' : 'PARTIAL'
				}
				const cls = classRecords.value.find((item) => item.id === classId.value)
				return loadBalance(
					list.map((item) => item.studentId),
					currentCourseId.value || cls?.courseId
				)
			})
			.catch(() => {
				students.value = []
				attendError.value = '学员名单加载失败，请检查网络后重试'
			})
	}
	// 统计各状态人数：包含已点名和本次待提交，保证与学员方块总数一致
	const countByStatus = (status) => students.value.filter((s) => s.attendStatus === status).length
	const pendingCountByStatus = (status) => students.value.filter((s) => s.attendStatus === status && !s.attendId).length
	const savedCount = computed(() => students.value.filter((s) => s.attendId).length)
	const pendingCount = computed(() => students.value.filter((s) => !s.attendId).length)
	const statusText = (status) => ({ ATTEND: '到课', LEAVE: '请假', ABSENT: '缺勤' })[status] || '待标记'
	const statusClass = (status) => (status === 'ATTEND' ? 'st-attend' : status === 'LEAVE' ? 'st-leave' : 'st-absent')
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
		courseApi.attendConfirm({ scheduleId: scheduleId.value, attendList }).then((res) => {
			// 请求拦截器已解包 CommonResult.data，这里 res 就是结果对象
			const result = res || {}
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
			// 放在 loadAttendPage 之后，避免被其内部的清空覆盖
			lastResult.value = {
				deductCount: result?.deductCount ?? attendList.length,
				skipped
			}
		})
	}
	const onCancelAttend = (record) => {
		courseApi.attendCancel({ id: record.attendId }).then(() => loadAttendPage())
	}
	loadClasses()
	loadStudents()
	resolveTargetSchedule().finally(loadSchedules)
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
	.attend-course-tag {
		margin-left: 8px;
		padding: 1px 8px;
		font-size: 12px;
		color: var(--primary-7);
		background: var(--primary-1);
		border-radius: 10px;
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
			color: var(--success-text);
			background: #f6ffed;
			border-color: #b7eb8f;
		}
		&.st-leave {
			color: #b07d00;
			background: #fffbe6;
			border-color: #ffe58f;
		}
		&.st-absent {
			color: var(--danger-text);
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
		transition:
			transform 0.15s,
			box-shadow 0.15s,
			background 0.15s;
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
			color: var(--danger-text);
		}
	}
	.attend-account-line {
		margin-top: 4px;
		font-size: 11px;
		color: var(--text-color-tertiary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
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
		color: var(--danger-text);
	}
	.attend-skip-line {
		margin-top: 4px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.attend-filter {
		display: flex;
		align-items: center;
		gap: 12px;
		flex-wrap: wrap;
		margin-bottom: 12px;
	}
	.attend-date {
		width: 160px;
	}
	.attend-class {
		width: 220px;
	}
	.attend-filter-tip {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.attend-toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		flex-wrap: wrap;
		margin-bottom: 12px;
	}
	.attend-toolbar-title {
		font-weight: 500;
	}
	.attend-actions {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-wrap: wrap;
	}
	.attend-actions .attend-confirm {
		min-width: 170px;
	}
	@media (max-width: 576px) {
		.attend-filter {
			display: grid;
			grid-template-columns: 1fr 1fr;
			gap: 8px;
		}
		.attend-date,
		.attend-class,
		.attend-filter .attend-refresh {
			width: 100% !important;
		}
		.attend-filter .attend-refresh {
			grid-column: 1 / -1;
		}
		.attend-filter-tip {
			grid-column: 1 / -1;
		}
		.attend-page :deep(.ant-picker-input > input),
		.attend-page :deep(.ant-select-selection-search-input),
		.attend-page :deep(.ant-select-selection-item) {
			font-size: 16px;
		}
		.attend-page :deep(.ant-btn) {
			height: 44px !important;
			min-height: 44px;
		}
		.attend-toolbar {
			display: block;
		}
		.attend-toolbar-title {
			display: flex;
			align-items: center;
			flex-wrap: wrap;
			margin-bottom: 8px;
		}
		.attend-actions {
			display: grid;
			grid-template-columns: repeat(3, 1fr);
			width: 100%;
		}
		.attend-actions .attend-confirm {
			grid-column: 1 / -1;
			width: 100%;
			height: 46px;
			font-size: 15px;
		}
		.attend-switch {
			min-height: 44px;
			padding: 0 8px;
		}
		.attend-undo {
			padding: 8px 4px;
			margin-right: -4px;
		}
		.attend-slots {
			display: flex !important;
			overflow-x: auto;
			flex-wrap: nowrap !important;
			gap: 8px;
			padding-bottom: 4px;
			margin-left: 0 !important;
			margin-right: 0 !important;
			scroll-snap-type: x mandatory;
		}
		.attend-slots > .ant-col {
			flex: 0 0 250px;
			max-width: 250px !important;
			padding: 0 !important;
			scroll-snap-align: start;
		}
		.attend-slot {
			margin-bottom: 0;
		}
	}
</style>
