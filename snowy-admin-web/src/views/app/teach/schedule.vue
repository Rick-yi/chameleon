<template>
	<div>
		<!-- 视图切换 -->
		<div class="m-view-tabs">
			<div class="m-view-tab" :class="{ active: scope === 'day' }" @click="onScope('day')">今天</div>
			<div class="m-view-tab" :class="{ active: scope === 'week' }" @click="onScope('week')">本周</div>
		</div>

		<!-- 日期条 -->
		<div class="m-date-bar">
			<a-icon class="m-date-arrow" @click="shiftDay(-1)"><left-outlined /></a-icon>
			<div class="m-date-text" @click="backToday">{{ dateLabel }}</div>
			<a-icon class="m-date-arrow" @click="shiftDay(1)"><right-outlined /></a-icon>
			<a-tag v-if="isToday" color="blue" style="margin-left: 8px">今天</a-tag>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!scheduleList.length" class="m-empty">
			{{ dateLabel }}没有排课
			<div class="m-empty-sub">可以在 PC 端排课，或切换日期查看</div>
		</div>

		<template v-for="group in groupedList" :key="group.date">
			<div class="m-section-title">{{ group.label }}</div>
			<div
				v-for="item in group.list"
				:key="item.id"
				class="m-card m-class-card"
				:class="{ done: item.attendStatus === 'DONE' }"
				@click="goAttend(item)"
			>
				<div class="m-class-time">
					<div class="m-class-start">{{ String(item.startTime || '').slice(0, 5) }}</div>
					<div class="m-class-end">{{ String(item.endTime || '').slice(0, 5) }}</div>
				</div>
				<div class="m-class-main">
					<div class="m-class-title">{{ item.className || '未命名班级' }}</div>
					<div class="m-class-sub">
						{{ item.classroom || '未定教室' }}
						<template v-if="item.teacherName">· {{ item.teacherName }}</template>
					</div>
					<div class="m-class-tags">
						<a-tag :color="attendColor(item.attendStatus)">{{ attendText(item.attendStatus) }}</a-tag>
						<a-tag v-if="item.studentCount">应到 {{ item.studentCount }} 人</a-tag>
					</div>
				</div>
				<a-icon class="m-class-arrow"><right-outlined /></a-icon>
			</div>
		</template>
	</div>
</template>

<script setup name="appTeachSchedule">
	import { useRouter } from 'vue-router'
	import courseApi from '@/api/course/courseApi'
	import { useAppStore } from '@/store/app'

	const router = useRouter()
	const appStore = useAppStore()
	const loading = ref(false)
	const scheduleList = ref([])
	const date = ref(new Date())
	const scope = ref('day')

	const pad = (n) => String(n).padStart(2, '0')
	const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	const todayStr = fmt(new Date())
	const isToday = computed(() => fmt(date.value) === todayStr)

	const weekName = ['日', '一', '二', '三', '四', '五', '六']
	const dateLabel = computed(() => {
		const d = date.value
		return `${d.getMonth() + 1}月${d.getDate()}日 周${weekName[d.getDay()]}`
	})

	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' })[status] || '待点名'
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' })[status] || 'default'

	/** 按天分组的课节 */
	const groupedList = computed(() => {
		const map = new Map()
		for (const item of scheduleList.value) {
			const key = String(item.scheduleDate || '').slice(0, 10)
			if (!map.has(key)) map.set(key, [])
			map.get(key).push(item)
		}
		return [...map.entries()].map(([date, list]) => ({
			date,
			label: dayLabel(date),
			list
		}))
	})

	/** 日期 → 「10月22日 周四」 */
	const dayLabel = (dateStr) => {
		if (!dateStr) return '未知日期'
		const d = new Date(`${dateStr}T00:00:00`)
		if (Number.isNaN(d.getTime())) return dateStr
		return `${d.getMonth() + 1}月${d.getDate()}日 周${weekName[d.getDay()]}`
	}

	const load = async () => {
		loading.value = true
		try {
			// 本周视图取周一至周日
			let dayStart = `${fmt(date.value)} 00:00:00`
			let dayEnd = `${fmt(date.value)} 23:59:59`
			if (scope.value === 'week') {
				const monday = new Date(date.value)
				const offset = (monday.getDay() + 6) % 7
				monday.setDate(monday.getDate() - offset)
				const sunday = new Date(monday)
				sunday.setDate(sunday.getDate() + 6)
				dayStart = `${fmt(monday)} 00:00:00`
				dayEnd = `${fmt(sunday)} 23:59:59`
			}
			const data = await courseApi.scheduleWeek({ startDate: dayStart, endDate: dayEnd })
			let list = data || []
			// 教学视角：只看自己的课（教师 = 登录用户）
			const userId = appStore.userInfo?.id
			if (appStore.view === 'teach' && userId) {
				const mine = list.filter((item) => item.teacherId === userId)
				if (mine.length) list = mine
			}
			// 待点名的排前面
			scheduleList.value = [...list].sort((a, b) => {
				const rank = (item) => (item.attendStatus === 'NONE' ? 0 : item.attendStatus === 'PARTIAL' ? 1 : 2)
				return rank(a) - rank(b) || String(a.startTime).localeCompare(String(b.startTime))
			})
		} finally {
			loading.value = false
		}
	}

	const onScope = (value) => {
		scope.value = value
		load()
	}

	const shiftDay = (n) => {
		const d = new Date(date.value)
		d.setDate(d.getDate() + n)
		date.value = d
		load()
	}
	const backToday = () => {
		date.value = new Date()
		load()
	}
	const goAttend = (item) => {
		router.push(`/app/teach/attend?scheduleId=${item.id}&classId=${item.classId || ''}`)
	}

	onMounted(load)
</script>

<style lang="less" scoped>
	.m-view-tabs {
		display: flex;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 4px;
		gap: 4px;
		margin-bottom: 10px;
	}
	.m-view-tab {
		flex: 1;
		text-align: center;
		padding: 8px 0;
		font-size: 14px;
		color: var(--text-color-secondary);
		border-radius: var(--radius-control);
		&.active {
			background: var(--primary-1);
			color: var(--primary-6);
			font-weight: 600;
		}
	}
	.m-section-title {
		margin: 12px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-date-bar {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 18px;
		padding: 10px 12px;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		margin-bottom: 12px;
	}
	.m-date-text {
		font-size: 16px;
		font-weight: 600;
		color: var(--text-color);
		font-variant-numeric: tabular-nums;
	}
	.m-date-arrow {
		font-size: 16px;
		color: var(--text-color-secondary);
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-class-card {
		display: flex;
		align-items: center;
		gap: 12px;
	}
	.m-class-time {
		width: 56px;
		flex-shrink: 0;
		text-align: center;
	}
	.m-class-start {
		font-size: 16px;
		font-weight: 600;
		color: var(--primary-6);
		font-variant-numeric: tabular-nums;
	}
	.m-class-end {
		font-size: 12px;
		color: var(--text-color-tertiary);
		font-variant-numeric: tabular-nums;
	}
	.m-class-main {
		flex: 1;
		min-width: 0;
	}
	.m-class-title {
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-class-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-class-tags {
		margin-top: 6px;
		display: flex;
		gap: 6px;
		flex-wrap: wrap;
	}
	.m-class-arrow {
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
	.m-empty-sub {
		margin-top: 6px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
</style>
