<template>
	<div>
		<page-header title="排课课表" desc="周视图 / 日视图 / 教师视角，可批量生成、导出课表">
			<template #extra>
				<a-button @click="onExport">
					<template #icon><file-text-outlined /></template>
					导出课表
				</a-button>
				<a-button @click="onOpenTemplate">
					<template #icon><profile-outlined /></template>
					课表模板
				</a-button>
				<a-button @click="openChangeDrawer">
					<template #icon><history-outlined /></template>
					调课记录
				</a-button>
				<a-button @click="onOpenGenerate">
					<template #icon><thunderbolt-outlined /></template>
					批量生成
				</a-button>
				<a-button @click="onOpenAdhoc">
					<template #icon><team-outlined /></template>
					直接排课
				</a-button>
				<a-button type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					手动排课
				</a-button>
			</template>
		</page-header>
		<!-- 工具栏 -->
		<div class="ent-toolbar">
			<a-radio-group v-model:value="viewMode" button-style="solid">
				<a-radio-button value="week">周视图</a-radio-button>
				<a-radio-button value="day">日视图</a-radio-button>
				<a-radio-button value="teacher">教师视图</a-radio-button>
			</a-radio-group>
			<template v-if="viewMode === 'week'">
				<a-button @click="shiftWeek(-1)">‹ 上一周</a-button>
				<span style="font-weight: 500">{{ weekLabel }}</span>
				<a-button @click="shiftWeek(1)">下一周 ›</a-button>
				<a-button @click="shiftWeek(0)">本周</a-button>
			</template>
			<template v-else-if="viewMode === 'day'">
				<a-date-picker v-model:value="dayDate" value-format="YYYY-MM-DD" style="width: 160px" />
				<span style="font-weight: 500">{{ dayLabel }}</span>
			</template>
			<a-select v-model:value="searchFormState.classId" placeholder="全部班级" :options="classOptions" allow-clear style="width: 200px" @change="loadWeek" />
			<a-select v-model:value="searchFormState.teacherId" placeholder="全部教师" :options="teacherOptions" allow-clear style="width: 160px" show-search option-filter-prop="label" @change="loadWeek" />
			<a-input v-model:value="searchFormState.classroom" placeholder="教室" allow-clear style="width: 120px" @pressEnter="loadWeek" />
		</div>

		<div class="ent-body" style="padding: 0">
		<!-- 周视图 -->
		<div v-if="viewMode === 'week'" style="padding: 16px; overflow-x: auto">
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
							class="ent-schedule-card"
							:class="{ 'is-conflict': conflictIdSet.has(s.id) }"
							@click="onOpenChange(s)"
						>
							<div style="display: flex; align-items: center; gap: 4px">
								<span style="font-size: 13px; font-weight: 500; color: #1677ff">{{ s.className || className(s.classId) }}</span>
								<a-tag v-if="conflictIdSet.has(s.id)" color="red" style="margin-left: auto; font-size: 11px; line-height: 16px; padding: 0 4px">冲突</a-tag>
								<a-tag v-else :color="attendColor(s.attendStatus)" style="margin-left: auto; font-size: 11px; line-height: 16px; padding: 0 4px">
									{{ attendText(s.attendStatus) }}
								</a-tag>
							</div>
							<div style="font-size: 11px; color: rgba(0,0,0,.65)">{{ s.classroom || '未设教室' }}</div>
							<div v-if="s.teacherName" style="font-size: 11px; color: rgba(0,0,0,.45)">{{ s.teacherName }}</div>
							<a-button type="link" size="small" style="padding: 0; font-size: 12px" @click.stop="goAttend(s)">点名 ›</a-button>
						</div>
					</div>
				</template>
			</div>
			<div class="ent-note" :class="{ 'is-error': conflictIdSet.size }">
				{{ conflictTip || `本周共 ${scheduleList.length} 节课${conflictIdSet.size ? '，存在 ' + conflictIdSet.size + ' 节冲突（教室或教师撞课）' : ''}` }}
			</div>
		</div>

		<!-- 日视图 -->
		<div v-else-if="viewMode === 'day'" style="padding: 16px">
			<div v-if="classroomUsage.length" style="margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap">
				<a-tag v-for="item in classroomUsage" :key="item.name" color="blue">{{ item.name }} {{ item.count }} 节</a-tag>
			</div>
			<div v-if="!dayScheduleList.length" class="ent-empty">当天没有排课</div>
			<a-table
				v-else
				:columns="dayColumns"
				:data-source="dayScheduleList"
				:row-key="(r) => r.id"
				bordered
				size="middle"
				:pagination="false"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'time'">
						{{ (record.startTime || '').slice(0, 5) }} - {{ (record.endTime || '').slice(0, 5) }}
					</template>
					<template v-if="column.dataIndex === 'className'">
						{{ record.className || className(record.classId) }}
					</template>
					<template v-if="column.dataIndex === 'attendStatus'">
						<a-tag :color="attendColor(record.attendStatus)">{{ attendText(record.attendStatus) }}</a-tag>
					</template>
					<template v-if="column.dataIndex === 'conflict'">
						<a-tag v-if="conflictIdSet.has(record.id)" color="red">冲突</a-tag>
						<span v-else>-</span>
					</template>
					<template v-if="column.dataIndex === 'action'">
						<a-space>
							<a @click="goAttend(record)">点名</a>
							<a-divider type="vertical" />
							<a @click="onOpenChange(record)">调课</a>
						</a-space>
					</template>
				</template>
			</a-table>
		</div>

		<!-- 教师视图 -->
		<div v-else style="padding: 16px">
			<div v-if="!teacherGroups.length" class="ent-empty">本周没有排课</div>
			<a-row v-else :gutter="12">
				<a-col v-for="group in teacherGroups" :key="group.key" :xs="24" :sm="12" :xl="8">
					<a-card size="small" style="margin-bottom: 12px">
						<template #title>
							<span>{{ group.name }} <a-tag>{{ group.list.length }} 节</a-tag></span>
						</template>
						<div v-for="day in group.days" :key="day.label" style="margin-bottom: 8px">
							<div style="font-size: 12px; color: rgba(0,0,0,.45); margin-bottom: 4px">{{ day.label }}</div>
							<div
								v-for="s in day.list"
								:key="s.id"
								class="ent-schedule-card"
								:class="{ 'is-conflict': conflictIdSet.has(s.id) }"
								style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px"
								@click="onOpenChange(s)"
							>
								<span style="font-size: 12px; color: #1677ff; width: 96px">
									{{ (s.startTime || '').slice(0, 5) }}-{{ (s.endTime || '').slice(0, 5) }}
								</span>
								<span style="font-size: 12px">{{ s.className || className(s.classId) }}</span>
								<span style="font-size: 11px; color: rgba(0,0,0,.45)">{{ s.classroom || '未设教室' }}</span>
								<a-tag v-if="conflictIdSet.has(s.id)" color="red" style="margin-left: auto; font-size: 11px; line-height: 16px; padding: 0 4px">冲突</a-tag>
							</div>
						</div>
					</a-card>
				</a-col>
			</a-row>
		</div>
		</div>
	</div>

	<!-- 手动排课 -->
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
					<a-form-item label="教师" name="teacherId">
						<a-select
							v-model:value="addFormData.teacherId"
							placeholder="请选择教师"
							:options="teacherOptions"
							show-search
							option-filter-prop="label"
							allow-clear
						/>
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>

	<!-- 批量生成 -->
	<a-modal v-model:open="generateOpen" title="按周模板批量生成" :width="520" :destroy-on-close="true">
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
			<a-form-item label="固定课表">
				<div style="width: 100%">
					<template v-if="currentTemplate.length">
						<div v-for="(item, index) in currentTemplate" :key="index" style="font-size: 12px; color: rgba(0,0,0,.65); margin-bottom: 2px">
							周{{ '一二三四五六日'[item.dayOfWeek - 1] }} {{ item.startTime }}-{{ item.endTime }}
							<span v-if="item.classroom"> · {{ item.classroom }}</span>
						</div>
						<div style="font-size: 12px; color: rgba(0,0,0,.45); margin-top: 4px">
							未填教室/教师的时段自动沿用班级默认设置
						</div>
					</template>
					<a-alert v-else type="warning" show-icon message="该班级还没有固定课表，请先到「班级管理」配置" />
				</div>
			</a-form-item>
			<a-form-item v-if="generateRangeLabel" label="生成区间">
				<span>{{ generateRangeLabel }}</span>
			</a-form-item>
			<div v-if="generateResult" style="background: #f5f7fa; border-radius: 6px; padding: 10px 12px; margin-bottom: 12px">
				<div style="font-size: 13px; margin-bottom: 4px">{{ generateSummary }}</div>
				<div v-if="generateResult.conflicts && generateResult.conflicts.length" style="color: #ff4d4f; font-size: 12px">
					<div v-for="(item, index) in generateResult.conflicts" :key="index">· {{ item }}</div>
				</div>
			</div>
			<div style="display: flex; gap: 8px; justify-content: flex-end">
				<a-button @click="onPreviewGenerate">预览</a-button>
				<a-button type="primary" @click="onSubmitGenerate">生成</a-button>
			</div>
		</a-form>
	</a-modal>

	<!-- 直接排课：不建班级，直接给课程和一群学员排课 -->
	<a-modal v-model:open="adhocOpen" title="直接排课（无需建班级）" :width="720" :destroy-on-close="true" @ok="onSubmitAdhoc">
		<a-alert
			type="info"
			show-icon
			style="margin-bottom: 16px"
			message="适用于老师带固定学员、临时集训、一对一或试听：直接指定课程、老师与这节课的学员名单，按周时段展开生成排课"
		/>
		<a-form layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="课程" required>
						<a-select v-model:value="adhocForm.courseId" placeholder="请选择课程" :options="courseOptions" show-search option-filter-prop="label" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="老师">
						<a-select v-model:value="adhocForm.teacherId" placeholder="请选择老师" :options="teacherOptions" allow-clear show-search option-filter-prop="label" />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="教室">
						<a-input v-model:value="adhocForm.classroom" placeholder="如：A101" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="开始日期" required>
						<a-date-picker v-model:value="adhocForm.startDate" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="8">
					<a-form-item label="生成周数" required>
						<a-input-number v-model:value="adhocForm.weekCount" :min="1" :max="26" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="上课学员" required>
						<a-select
							v-model:value="adhocForm.studentIdList"
							mode="multiple"
							placeholder="搜索并选择这节课的学员"
							:options="studentOptions"
							show-search
							option-filter-prop="label"
							:max-tag-count="responsive"
						/>
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="每周时段" required>
						<div style="width: 100%">
							<div v-for="(item, index) in adhocForm.weekTemplate" :key="index" style="display: flex; gap: 8px; align-items: center; margin-bottom: 8px; flex-wrap: wrap">
								<a-select v-model:value="item.dayOfWeek" style="width: 110px" :options="weekDayOptions" />
								<a-time-picker v-model:value="item.startTime" format="HH:mm" value-format="HH:mm" style="width: 110px" />
								<span>至</span>
								<a-time-picker v-model:value="item.endTime" format="HH:mm" value-format="HH:mm" style="width: 110px" />
								<a-button type="link" danger size="small" @click="adhocForm.weekTemplate.splice(index, 1)">删除</a-button>
							</div>
							<a-button type="dashed" block @click="addAdhocSlot">
								<template #icon><plus-outlined /></template>
								添加上课时段
							</a-button>
						</div>
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>

	<!-- 课表模板 -->
	<a-modal v-model:open="templateOpen" title="编辑班级课表模板" :width="720" :destroy-on-close="true" @ok="onSubmitTemplate">
		<a-form layout="vertical">
			<a-form-item label="班级" name="templateClassId" :rules="[{ required: true, message: '请选择班级' }]">
				<a-select v-model:value="templateFormData.classId" placeholder="请选择班级" :options="classOptions" @change="loadTemplate" />
			</a-form-item>
			<a-form-item label="每周固定课表">
				<week-template-editor ref="templateEditorRef" :model-value="templateFormData.weekTemplate" />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 调课记录 -->
	<a-drawer v-model:open="changeDrawerOpen" title="调课记录" :width="760" destroy-on-close>
		<a-table
			:columns="changeColumns"
			:data-source="changeList"
			:row-key="(r) => r.id"
			:loading="changeLoading"
			size="small"
			:pagination="false"
			scroll="{ y: 460 }"
		>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'change'">
					<div class="change-from">
						{{ record.fromDate || '-' }} {{ record.fromTime || '' }}
						<span v-if="record.fromClassName" style="color: rgba(0,0,0,.45)">（{{ record.fromClassName }}）</span>
					</div>
					<div class="change-to">
						→ {{ record.toDate || '-' }} {{ record.toTime || '' }}
						<span v-if="record.toClassName" style="color: rgba(0,0,0,.45)">（{{ record.toClassName }}）</span>
					</div>
				</template>
				<template v-if="column.dataIndex === 'changeReason'">
					{{ record.changeReason || '-' }}
				</template>
				<template v-if="column.dataIndex === 'changeUserName'">
					{{ record.changeUserName || '-' }}
				</template>
				<template v-if="column.dataIndex === 'createTime'">
					{{ (record.createTime || '').slice(0, 16).replace('T', ' ') }}
				</template>
			</template>
		</a-table>
		<a-empty v-if="!changeLoading && !changeList.length" description="暂无调课记录" />
	</a-drawer>

	<!-- 调课 -->
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
					<a-form-item label="新教师" name="teacherId">
						<a-select
							v-model:value="changeFormData.teacherId"
							placeholder="请选择教师"
							:options="teacherOptions"
							show-search
							option-filter-prop="label"
							allow-clear
						/>
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="调课原因" name="changeReason" :rules="[{ required: true, message: '请输入调课原因' }]">
						<a-input v-model:value="changeFormData.changeReason" placeholder="如：教室调整" allow-clear />
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
	import { useRoute, useRouter } from 'vue-router'
	import { message } from 'ant-design-vue'
	import { FileTextOutlined, HistoryOutlined, PlusOutlined, ProfileOutlined, TeamOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'
	import downloadUtil from '@/utils/downloadUtil'
	const router = useRouter()
	const route = useRoute()
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')
	const goAttend = (record) => {
		router.push({ path: '/course/attend', query: { classId: record.classId, scheduleId: record.id } })
	}

	const searchFormState = ref({})
	const classOptions = ref([])
	const classRecords = ref([])
	const teacherOptions = ref([])
	const scheduleList = ref([])
	const scheduleMap = ref({})
	const weekDays = ref([])
	const weekLabel = ref('')
	const monday = ref(null)
	const conflictTip = ref('')
	const loading = ref(false)
	const timeSlots = ref([])
	const viewMode = ref('week')
	const dayDate = ref(fmtToday())
	const addOpen = ref(false)
	const addFormRef = ref()
	const addFormData = ref({})
	const generateOpen = ref(false)
	const generateFormRef = ref()
	const generateFormData = ref({})
	const generateResult = ref(null)
	const changeOpen = ref(false)
	const changeFormRef = ref()
	const changeFormData = ref({})
	const changeDrawerOpen = ref(false)
	const changeLoading = ref(false)
	const changeList = ref([])
	const changeColumns = [
		{ title: '调课', dataIndex: 'change', width: 260 },
		{ title: '原因', dataIndex: 'changeReason' },
		{ title: '操作人', dataIndex: 'changeUserName', width: 100 },
		{ title: '操作时间', dataIndex: 'createTime', width: 160 }
	]
	const openChangeDrawer = () => {
		changeDrawerOpen.value = true
		changeLoading.value = true
		courseApi
			.scheduleChangePage({ current: 1, size: 50 })
			.then((data) => {
				changeList.value = data?.records || []
			})
			.finally(() => (changeLoading.value = false))
	}
	// ---------- 直接排课（不挂班级） ----------
	const adhocOpen = ref(false)
	const adhocForm = ref({})
	const courseOptions = ref([])
	const studentOptions = ref([])
	const weekDayOptions = [
		{ value: 1, label: '周一' },
		{ value: 2, label: '周二' },
		{ value: 3, label: '周三' },
		{ value: 4, label: '周四' },
		{ value: 5, label: '周五' },
		{ value: 6, label: '周六' },
		{ value: 7, label: '周日' }
	]
	const loadAdhocOptions = () => {
		courseApi.coursePage({ current: 1, size: 200 }).then((data) => {
			courseOptions.value = (data.records || []).map((item) => ({ value: item.id, label: item.name }))
		})
		studentApi.studentPage({ current: 1, size: 500 }).then((data) => {
			studentOptions.value = (data.records || []).map((item) => ({
				value: item.id,
				label: `${item.name}(${item.parentPhone || '-'})`
			}))
		})
	}
	const onOpenAdhoc = () => {
		const start = new Date()
		adhocForm.value = {
			courseId: undefined,
			teacherId: undefined,
			classroom: undefined,
			studentIdList: [],
			startDate: fmtDate(start),
			weekCount: 4,
			weekTemplate: [{ dayOfWeek: 1, startTime: '09:00', endTime: '10:30' }]
		}
		adhocOpen.value = true
	}
	const addAdhocSlot = () => {
		adhocForm.value.weekTemplate.push({ dayOfWeek: 1, startTime: '09:00', endTime: '10:30' })
	}
	const onSubmitAdhoc = () => {
		const form = adhocForm.value
		if (!form.courseId) {
			message.warning('请选择课程')
			return
		}
		if (!form.studentIdList || !form.studentIdList.length) {
			message.warning('请选择上课学员')
			return
		}
		if (!form.weekTemplate || !form.weekTemplate.length) {
			message.warning('请配置每周上课时段')
			return
		}
		courseApi
			.scheduleGenerateAdhoc({
				courseId: form.courseId,
				teacherId: form.teacherId,
				classroom: form.classroom,
				studentIdList: form.studentIdList,
				startDate: `${form.startDate} 00:00:00`,
				weekCount: form.weekCount,
				weekTemplateJson: JSON.stringify(form.weekTemplate)
			})
			.then((result) => {
				adhocOpen.value = false
				message.success(`已生成 ${result?.createdCount || 0} 节课`)
				loadWeek()
			})
	}
	const templateOpen = ref(false)
	const templateEditorRef = ref()
	const templateFormData = ref({ classId: undefined, weekTemplate: [] })

	const DEFAULT_SLOTS = [
		{ start: '09:00', end: '10:30' },
		{ start: '10:00', end: '11:30' },
		{ start: '14:00', end: '15:30' },
		{ start: '16:00', end: '17:30' },
		{ start: '19:00', end: '20:30' }
	]
	const dayColumns = [
		{ title: '时间', dataIndex: 'time', width: 130 },
		{ title: '班级', dataIndex: 'className' },
		{ title: '教师', dataIndex: 'teacherName' },
		{ title: '教室', dataIndex: 'classroom' },
		{ title: '点名', dataIndex: 'attendStatus', width: 100 },
		{ title: '冲突', dataIndex: 'conflict', width: 80 },
		{ title: '操作', dataIndex: 'action', align: 'center', width: 120 }
	]

	function pad(n) {
		return String(n).padStart(2, '0')
	}
	function fmtDate(d) {
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}
	function fmtToday() {
		return fmtDate(new Date())
	}
	const toMinutes = (time) => {
		if (!time) return 0
		const [h, m] = String(time).split(':')
		return Number(h) * 60 + Number(m)
	}
	const toTimeText = (minutes) => `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`
	const dateOf = (s) => (s.scheduleDate || '').slice(0, 10)
	const startOf = (s) => (s.startTime || '').slice(0, 5)
	const endOf = (s) => (s.endTime || '').slice(0, 5)

	const loadClasses = () => {
		courseApi.classPage({ current: 1, size: 200 }).then((data) => {
			classRecords.value = data.records || []
			classOptions.value = classRecords.value.map((item) => ({ value: item.id, label: item.name }))
		})
	}
	const loadTeachers = () => {
		courseApi.teacherOptions({}).then((data) => {
			teacherOptions.value = data || []
		})
	}
	const className = (id) => {
		const item = classOptions.value.find((i) => i.value === id)
		return item ? item.label : id
	}
	const classRecord = (id) => classRecords.value.find((item) => item.id === id)

	// 当前班级的固定课表（生成弹窗展示）
	const currentTemplate = computed(() => {
		const cls = classRecord(generateFormData.value.classId)
		if (!cls || !cls.weekTemplateJson) return []
		try {
			const list = typeof cls.weekTemplateJson === 'string' ? JSON.parse(cls.weekTemplateJson) : cls.weekTemplateJson
			return Array.isArray(list) ? list : []
		} catch (e) {
			return []
		}
	})
	// 生成区间（周一 ~ 结束日）
	const generateRangeLabel = computed(() => {
		if (!generateFormData.value.startDate || !generateFormData.value.weekCount) return ''
		const start = new Date(generateFormData.value.startDate.replace(/-/g, '/'))
		if (isNaN(start.getTime())) return ''
		const day = (start.getDay() + 6) % 7
		const mondayDate = new Date(start.getFullYear(), start.getMonth(), start.getDate() - day)
		const end = new Date(mondayDate)
		end.setDate(mondayDate.getDate() + generateFormData.value.weekCount * 7 - 1)
		return `${fmtDate(mondayDate)} ~ ${fmtDate(end)}（共 ${generateFormData.value.weekCount} 周）`
	})

	// 冲突检测：同一日期、时间重叠且教室或教师相同 → 冲突
	const conflictIdSet = computed(() => {
		const ids = new Set()
		const list = scheduleList.value
		for (let i = 0; i < list.length; i++) {
			for (let j = i + 1; j < list.length; j++) {
				const a = list[i]
				const b = list[j]
				if (dateOf(a) !== dateOf(b)) continue
				const aStart = toMinutes(startOf(a))
				const aEnd = toMinutes(endOf(a))
				const bStart = toMinutes(startOf(b))
				const bEnd = toMinutes(endOf(b))
				if (!(aStart < bEnd && aEnd > bStart)) continue
				const sameRoom = a.classroom && b.classroom && a.classroom === b.classroom
				const sameTeacher = a.teacherId && b.teacherId && a.teacherId === b.teacherId
				if (sameRoom || sameTeacher) {
					ids.add(a.id)
					ids.add(b.id)
				}
			}
		}
		return ids
	})

	// 日视图数据
	const dayScheduleList = computed(() =>
		scheduleList.value
			.filter((s) => dateOf(s) === dayDate.value)
			.sort((a, b) => startOf(a).localeCompare(startOf(b)))
	)
	const dayLabel = computed(() => {
		const names = '一二三四五六日'
		const d = new Date(String(dayDate.value).replace(/-/g, '/'))
		return isNaN(d.getTime()) ? dayDate.value : `周${names[(d.getDay() + 6) % 7]} ${dayDate.value}`
	})
	// 教室占用汇总
	const classroomUsage = computed(() => {
		const map = {}
		for (const s of dayScheduleList.value) {
			if (!s.classroom) continue
			map[s.classroom] = (map[s.classroom] || 0) + 1
		}
		return Object.keys(map).map((name) => ({ name, count: map[name] }))
	})

	// 教师视图：按教师分组，组内按天分组
	const teacherGroups = computed(() => {
		const groups = []
		const map = new Map()
		for (const s of scheduleList.value) {
			const key = s.teacherId || 'none'
			if (!map.has(key)) {
				const group = { key, name: s.teacherName || '未指定教师', list: [], days: [] }
				map.set(key, group)
				groups.push(group)
			}
			map.get(key).list.push(s)
		}
		for (const group of groups) {
			const dayMap = new Map()
			for (const s of group.list) {
				const date = dateOf(s)
				if (!dayMap.has(date)) dayMap.set(date, [])
				dayMap.get(date).push(s)
			}
			group.days = [...dayMap.entries()]
				.sort((a, b) => a[0].localeCompare(b[0]))
				.map(([date, list]) => ({
					label: `${date}（周${'一二三四五六日'[(new Date(date.replace(/-/g, '/')).getDay() + 6) % 7]}）`,
					list: list.sort((a, b) => startOf(a).localeCompare(startOf(b)))
				}))
		}
		return groups.sort((a, b) => b.list.length - a.list.length)
	})

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

	// 课表加载：整周数据一次取回（不限条数），支持班级/教师/教室筛选
	const loadWeek = () => {
		const end = new Date(monday.value)
		end.setDate(monday.value.getDate() + 6)
		loading.value = true
		courseApi
			.scheduleWeek({
				classId: searchFormState.value.classId,
				teacherId: searchFormState.value.teacherId,
				classroom: searchFormState.value.classroom,
				startDate: fmtDate(monday.value),
				endDate: fmtDate(end)
			})
			.then((data) => {
				scheduleList.value = data || []
				renderSchedule()
			})
			.finally(() => {
				loading.value = false
			})
	}

	// 渲染：时间轴按实际排课动态生成，课节按开始时间归位（非整点课程也能显示）
	const renderSchedule = () => {
		const startSet = new Map()
		for (const s of scheduleList.value) {
			const start = startOf(s)
			if (!start) continue
			if (!startSet.has(start)) {
				startSet.set(start, toMinutes(endOf(s) || start))
			} else {
				startSet.set(start, Math.max(startSet.get(start), toMinutes(endOf(s) || start)))
			}
		}
		const slots = startSet.size
			? [...startSet.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([start, end]) => ({ start, end: toTimeText(end) }))
			: DEFAULT_SLOTS
		timeSlots.value = slots
		const map = {}
		for (const s of scheduleList.value) {
			const date = dateOf(s)
			const start = startOf(s)
			if (!date || !start) continue
			// 归位到「开始时间不晚于本节课」的最后一行，保证非整点课程不丢
			let row = slots[0]
			for (const slot of slots) {
				if (slot.start <= start) {
					row = slot
				}
			}
			const key = date + '|' + row.start
			map[key] = map[key] || []
			map[key].push(s)
		}
		scheduleMap.value = map
	}

	const shiftWeek = (n) => {
		const m = new Date(monday.value)
		m.setDate(m.getDate() + 7 * n)
		monday.value = m
		buildWeek()
	}

	const weekHeadStyle = (d) => {
		const today = fmtToday()
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

	const onOpenAdd = () => {
		addFormData.value = { classId: searchFormState.value.classId, startTime: '09:00', endTime: '10:30' }
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

	const generateSummary = computed(() => {
		if (!generateResult.value) return ''
		const r = generateResult.value
		return `计划 ${r.plannedCount} 节 · 新增 ${r.createdCount} 节 · 跳过重复 ${r.skippedCount} 节${r.preview ? '（仅预览）' : ''}`
	})
	const doGenerate = (preview) => {
		generateFormRef.value.validate().then(() => {
			const body = {
				...generateFormData.value,
				startDate: `${generateFormData.value.startDate} 00:00:00`,
				preview
			}
			return courseApi.scheduleGenerate(body).then((result) => {
				generateResult.value = result || null
				if (!preview) {
					generateOpen.value = false
					conflictTip.value = ''
					loadWeek()
				}
			})
		})
	}
	const onOpenGenerate = () => {
		generateFormData.value = {
			classId: searchFormState.value.classId || generateFormData.value.classId,
			startDate: generateFormData.value.startDate || fmtDate(monday.value),
			weekCount: generateFormData.value.weekCount || 4
		}
		generateOpen.value = true
	}
	const onPreviewGenerate = () => doGenerate(true)
	const onSubmitGenerate = () => doGenerate(false)

	// 课表模板编辑
	const loadTemplate = (classId) => {
		const cls = classRecord(classId)
		let list = []
		if (cls && cls.weekTemplateJson) {
			try {
				const parsed = typeof cls.weekTemplateJson === 'string' ? JSON.parse(cls.weekTemplateJson) : cls.weekTemplateJson
				list = Array.isArray(parsed) ? parsed : []
			} catch (e) {
				list = []
			}
		}
		templateFormData.value.weekTemplate = list
	}
	const onOpenTemplate = () => {
		const classId = searchFormState.value.classId || templateFormData.value.classId
		templateFormData.value = { classId, weekTemplate: [] }
		loadTemplate(classId)
		templateOpen.value = true
	}
	const onSubmitTemplate = () => {
		const classId = templateFormData.value.classId
		if (!classId) {
			message.warning('请先选择班级')
			return
		}
		const cls = classRecord(classId)
		if (!cls) {
			message.warning('班级不存在，请刷新后重试')
			return
		}
		const weekTemplate = templateEditorRef.value ? templateEditorRef.value.getValue() : []
		courseApi
			.classEdit({ ...cls, weekTemplateJson: JSON.stringify(weekTemplate) })
			.then(() => {
				templateOpen.value = false
				message.success('课表模板已保存')
				loadClasses()
			})
	}

	// 导出课表
	const onExport = () => {
		const end = new Date(monday.value)
		end.setDate(monday.value.getDate() + 6)
		courseApi
			.scheduleExport({
				classId: searchFormState.value.classId,
				teacherId: searchFormState.value.teacherId,
				classroom: searchFormState.value.classroom,
				startDate: fmtDate(monday.value),
				endDate: fmtDate(end)
			})
			.then((res) => downloadUtil.resultDownload(res))
			.catch(() => {})
	}

	const onOpenChange = (record) => {
		changeFormData.value = {
			scheduleId: record.id,
			scheduleDate: dateOf(record),
			startTime: startOf(record),
			endTime: endOf(record),
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
	// 支持从「班级管理」带班级 id 过来（点「去生成排课」）
	if (route.query.classId) {
		searchFormState.value.classId = route.query.classId
	}
	loadClasses()
	loadTeachers()
	loadAdhocOptions()
	buildWeek()
</script>
