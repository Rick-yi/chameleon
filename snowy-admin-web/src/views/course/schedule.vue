<template>
	<xn-panel>
		<div style="padding: 16px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap; border-bottom: 1px solid #f0f0f0">
			<a-button @click="shiftWeek(-1)">‹ 上一周</a-button>
			<span style="font-weight: 500">{{ weekLabel }}</span>
			<a-button @click="shiftWeek(1)">下一周 ›</a-button>
			<a-select v-model:value="searchFormState.classId" placeholder="全部班级" :options="classOptions" allow-clear style="width: 220px" />
			<div style="margin-left: auto; display: flex; gap: 8px">
				<a-button @click="generateOpen = true">⚙ 批量生成</a-button>
				<a-button type="primary" @click="onOpenAdd">＋ 手动排课</a-button>
			</div>
		</div>
		<div style="padding: 16px; overflow-x: auto">
			<div style="display: grid; grid-template-columns: 80px repeat(7, 1fr); min-width: 980px; border: 1px solid #f0f0f0; border-radius: 6px; overflow: hidden">
				<div style="background: #fafafa; text-align: center; padding: 10px 4px; font-weight: 500; border-right: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0">时间</div>
				<div v-for="d in weekDays" :key="d.date" :style="weekHeadStyle(d)">
					{{ d.label }}
				</div>
				<template v-for="slot in timeSlots" :key="slot.start">
					<div style="background: #fafafa; color: rgba(0,0,0,.45); font-size: 12px; text-align: center; padding: 10px 2px; border-right: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0">
						{{ slot.start }}<br />{{ slot.end }}
					</div>
					<div v-for="d in weekDays" :key="d.date + slot.start" style="min-height: 80px; border-right: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0; padding: 6px">
						<div
							v-for="s in scheduleMap[d.date + '|' + slot.start] || []"
							:key="s.id"
							style="background: #e6f4ff; border: 1px solid #91caff; border-radius: 6px; padding: 6px 8px; margin-bottom: 6px; cursor: pointer"
							@click="onOpenChange(s)"
						>
							<div style="font-size: 13px; font-weight: 500; color: #1677ff">{{ className(s.classId) }}</div>
							<div style="font-size: 11px; color: rgba(0,0,0,.65)">{{ s.classroom }}</div>
						</div>
					</div>
				</template>
			</div>
			<a-alert v-if="conflictTip" type="error" show-icon :message="conflictTip" style="margin-top: 12px" />
		</div>
	</xn-panel>

	<a-modal v-model:open="addOpen" title="手动排课" :width="560" :destroy-on-close="true" @ok="onSubmitAdd">
		<a-form ref="addFormRef" :model="addFormData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="班级" name="classId" :rules="[{ required: true, message: '请选择班级' }]">
						<a-select v-model:value="addFormData.classId" placeholder="请选择班级" :options="classOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="上课日期" name="scheduleDate" :rules="[{ required: true, message: '请选择日期' }]">
						<a-date-picker v-model:value="addFormData.scheduleDate" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="开始时间" name="startTime" :rules="[{ required: true, message: '请选择开始时间' }]">
						<a-time-picker v-model:value="addFormData.startTime" format="HH:mm" value-format="HH:mm" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="结束时间" name="endTime" :rules="[{ required: true, message: '请选择结束时间' }]">
						<a-time-picker v-model:value="addFormData.endTime" format="HH:mm" value-format="HH:mm" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="教室" name="classroom">
						<a-input v-model:value="addFormData.classroom" placeholder="如：A101" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="教师id" name="teacherId">
						<a-input v-model:value="addFormData.teacherId" placeholder="可选" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>

	<a-modal v-model:open="generateOpen" title="按周模板批量生成" :width="480" :destroy-on-close="true" @ok="onSubmitGenerate">
		<a-form ref="generateFormRef" :model="generateFormData" layout="vertical">
			<a-form-item label="班级" name="classId" :rules="[{ required: true, message: '请选择班级' }]">
				<a-select v-model:value="generateFormData.classId" placeholder="请选择班级" :options="classOptions" />
			</a-form-item>
			<a-form-item label="开始日期" name="startDate" :rules="[{ required: true, message: '请选择开始日期' }]">
				<a-date-picker v-model:value="generateFormData.startDate" value-format="YYYY-MM-DD" style="width: 100%" />
			</a-form-item>
			<a-form-item label="生成周数" name="weekCount" :rules="[{ required: true, message: '请输入生成周数' }]">
				<a-input-number v-model:value="generateFormData.weekCount" :min="1" :max="26" style="width: 100%" />
			</a-form-item>
		</a-form>
	</a-modal>

	<a-modal v-model:open="changeOpen" title="调课" :width="560" :destroy-on-close="true" @ok="onSubmitChange">
		<a-form ref="changeFormRef" :model="changeFormData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="新日期" name="scheduleDate" :rules="[{ required: true, message: '请选择日期' }]">
						<a-date-picker v-model:value="changeFormData.scheduleDate" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="新时间段" name="startTime" :rules="[{ required: true, message: '请选择时间' }]">
						<a-space>
							<a-time-picker v-model:value="changeFormData.startTime" format="HH:mm" value-format="HH:mm" />
							<span>至</span>
							<a-time-picker v-model:value="changeFormData.endTime" format="HH:mm" value-format="HH:mm" />
						</a-space>
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="新教室" name="classroom">
						<a-input v-model:value="changeFormData.classroom" placeholder="如：B201" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="新教师id" name="teacherId">
						<a-input v-model:value="changeFormData.teacherId" placeholder="可选" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="调课原因" name="changeReason" :rules="[{ required: true, message: '请输入调课原因' }]">
						<a-input v-model:value="changeFormData.changeReason" placeholder="请输入调课原因" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>
			<div style="display: flex; justify-content: flex-end">
				<a-popconfirm title="确定取消这节课吗？" @confirm="onCancelSchedule">
					<a-button danger>取消这节课</a-button>
				</a-popconfirm>
			</div>
		</a-form>
	</a-modal>
</template>

<script setup name="courseSchedule">
	import courseApi from '@/api/course/courseApi'
	const searchFormState = ref({})
	const classOptions = ref([])
	const scheduleMap = ref({})
	const weekDays = ref([])
	const weekLabel = ref('')
	const monday = ref(null)
	const conflictTip = ref('')
	const timeSlots = [
		{ start: '09:00', end: '10:30' },
		{ start: '10:00', end: '11:30' },
		{ start: '14:00', end: '15:30' },
		{ start: '16:00', end: '17:30' },
		{ start: '19:00', end: '20:30' }
	]
	const addOpen = ref(false)
	const addFormRef = ref()
	const addFormData = ref({})
	const generateOpen = ref(false)
	const generateFormRef = ref()
	const generateFormData = ref({})
	const changeOpen = ref(false)
	const changeFormRef = ref()
	const changeFormData = ref({})

	const pad = (n) => String(n).padStart(2, '0')
	const fmtDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	const fmtDateTime = (d) => `${fmtDate(d)} 00:00:00`

	const buildWeek = () => {
		const m = new Date(monday.value)
		const days = []
		for (let i = 0; i < 7; i++) {
			const d = new Date(m)
			d.setDate(m.getDate() + i)
			days.push({ date: fmtDate(d), label: `周${'一二三四五六日'[i]} ${fmtDate(d).slice(5)}` })
		}
		weekDays.value = days
		const end = new Date(m)
		end.setDate(m.getDate() + 6)
		weekLabel.value = `${fmtDate(m)} ~ ${fmtDate(end)}`
		loadWeek()
	}

	const loadWeek = () => {
		const end = new Date(monday.value)
		end.setDate(end.getDate() + 6)
		courseApi
			.schedulePage({
				current: 1,
				size: 200,
				classId: searchFormState.value.classId,
				startDate: fmtDateTime(monday.value),
				endDate: fmtDateTime(end)
			})
			.then((data) => {
				const records = (data.records || []).filter((s) => s.status === 'NORMAL')
				const map = {}
				for (const s of records) {
					const date = s.scheduleDate.slice(0, 10)
					const start = s.startTime.slice(0, 5)
					map[date + '|' + start] = map[date + '|' + start] || []
					map[date + '|' + start].push(s)
				}
				scheduleMap.value = map
			})
	}

	const shiftWeek = (n) => {
		const m = new Date(monday.value)
		m.setDate(m.getDate() + 7 * n)
		monday.value = m
		buildWeek()
	}

	const weekHeadStyle = (d) => {
		const today = fmtDate(new Date())
		return {
			background: '#fafafa',
			textAlign: 'center',
			padding: '10px 4px',
			fontWeight: 500,
			borderRight: '1px solid #f0f0f0',
			borderBottom: '1px solid #f0f0f0',
			color: d.date === today ? '#1677ff' : undefined
		}
	}

	const loadClasses = () => {
		courseApi.classPage({ current: 1, size: 100 }).then((data) => {
			classOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const className = (id) => {
		const item = classOptions.value.find((i) => i.value === id)
		return item ? item.label : id
	}

	const onOpenAdd = () => {
		addFormData.value = { classId: searchFormState.value.classId }
		addOpen.value = true
	}
	const onSubmitAdd = () => {
		addFormRef.value.validate().then(() => {
			const body = { ...addFormData.value, scheduleDate: `${addFormData.value.scheduleDate} 00:00:00` }
			courseApi.scheduleAdd(body).then(() => {
				addOpen.value = false
				conflictTip.value = ''
				loadWeek()
			})
		})
	}
	const onSubmitGenerate = () => {
		generateFormRef.value.validate().then(() => {
			const body = { ...generateFormData.value, startDate: `${generateFormData.value.startDate} 00:00:00` }
			courseApi.scheduleGenerate(body).then(() => {
				generateOpen.value = false
				conflictTip.value = ''
				loadWeek()
			})
		})
	}
	const onOpenChange = (record) => {
		changeFormData.value = {
			scheduleId: record.id,
			scheduleDate: record.scheduleDate.slice(0, 10),
			startTime: record.startTime.slice(0, 5),
			endTime: record.endTime.slice(0, 5),
			classroom: record.classroom,
			teacherId: record.teacherId,
			changeReason: ''
		}
		changeOpen.value = true
	}
	const onSubmitChange = () => {
		changeFormRef.value.validate().then(() => {
			const body = { ...changeFormData.value, scheduleDate: `${changeFormData.value.scheduleDate} 00:00:00` }
			courseApi.scheduleChange(body).then(() => {
				changeOpen.value = false
				loadWeek()
			})
		})
	}
	const onCancelSchedule = () => {
		courseApi.scheduleCancel({ id: changeFormData.value.scheduleId }).then(() => {
			changeOpen.value = false
			loadWeek()
		})
	}

	// 本周周一
	const now = new Date()
	const day = (now.getDay() + 6) % 7
	monday.value = new Date(now.getFullYear(), now.getMonth(), now.getDate() - day)
	loadClasses()
	buildWeek()
</script>
