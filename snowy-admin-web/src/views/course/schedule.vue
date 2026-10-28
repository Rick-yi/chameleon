<template>
	<div>
		<page-header title="排课课表" desc="周视图 / 日视图 / 教师视角，可批量生成、导出课表">
			<template #extra>
				<a-dropdown v-if="canSchedule">
					<a-button type="primary">
						<template #icon><plus-outlined /></template>
						排课
						<down-outlined />
					</a-button>
					<template #overlay>
						<a-menu @click="onScheduleMenuClick">
							<a-menu-item v-if="hasPerm('/course/schedule/generate')" key="generate">
								<thunderbolt-outlined /> 批量生成
							</a-menu-item>
							<a-menu-item v-if="hasPerm('/course/schedule/generateAdhoc')" key="adhoc">
								<team-outlined /> 直接排课
							</a-menu-item>
							<a-menu-item v-if="hasPerm('/course/schedule/add')" key="add">
								<plus-outlined /> 手动排课
							</a-menu-item>
						</a-menu>
					</template>
				</a-dropdown>
				<a-dropdown>
					<a-button>
						<template #icon><more-outlined /></template>
						更多
					</a-button>
					<template #overlay>
						<a-menu @click="onMoreMenuClick">
							<a-menu-item v-if="hasPerm('/course/schedule/export')" key="export">
								<file-text-outlined /> 导出课表
							</a-menu-item>
							<a-menu-item v-if="hasPerm('/course/schedule/add')" key="copy">
								<copy-outlined /> 复制上周
							</a-menu-item>
							<a-menu-item key="template">
								<profile-outlined /> 课表模板
							</a-menu-item>
							<a-menu-item key="history">
								<history-outlined /> 调课记录
							</a-menu-item>
							<a-menu-item key="config">
								<setting-outlined /> 排课配置
							</a-menu-item>
						</a-menu>
					</template>
				</a-dropdown>
			</template>
		</page-header>
		<!-- 工具栏 -->
		<div class="ent-toolbar sched-toolbar">
			<div class="sched-view-row">
				<a-radio-group v-model:value="viewMode" button-style="solid" @change="onViewModeChange">
					<a-radio-button value="week">周视图</a-radio-button>
					<a-radio-button value="day">日视图</a-radio-button>
					<a-radio-button value="teacher">教师视图</a-radio-button>
					<a-radio-button value="split">分屏查看</a-radio-button>
				</a-radio-group>
				<template v-if="viewMode === 'week' || viewMode === 'teacher' || viewMode === 'split'">
					<a-button @click="shiftWeek(-1)">‹ 上一周</a-button>
					<span style="font-weight: 500">{{ weekLabel }}</span>
					<a-button @click="shiftWeek(1)">下一周 ›</a-button>
					<a-button @click="shiftWeek(0)">本周</a-button>
				</template>
				<template v-else-if="viewMode === 'day'">
					<a-date-picker v-model:value="dayDate" value-format="YYYY-MM-DD" style="width: 160px" />
					<span style="font-weight: 500">{{ dayLabel }}</span>
				</template>
				<template v-if="viewMode === 'split'">
					<a-button :disabled="splitPanels.length >= 4" @click="addSplitPanel">
						<template #icon><plus-outlined /></template>
						添加窗口
					</a-button>
					<a-button :disabled="splitPanels.length === 1" @click="removeSplitPanel">移除窗口</a-button>
				</template>
			</div>
			<div v-if="viewMode !== 'split'" class="sched-filter-row">
				<a-select v-model:value="searchFormState.campusId" placeholder="全部校区" :options="campusOptions" allow-clear style="width: 140px" @change="loadWeek" />
				<a-select v-model:value="searchFormState.courseId" placeholder="全部课程" :options="courseOptions" allow-clear style="width: 160px" @change="loadWeek" />
				<a-select v-model:value="searchFormState.classId" placeholder="全部班级" :options="classOptions" allow-clear show-search option-filter-prop="label" style="width: 180px" @change="loadWeek" />
				<a-select v-model:value="searchFormState.teacherId" placeholder="全部教师" :options="teacherOptions" allow-clear style="width: 150px" show-search option-filter-prop="label" @change="loadWeek" />
				<a-input v-model:value="searchFormState.classroom" placeholder="教室" allow-clear style="width: 110px" @pressEnter="loadWeek" />
				<a-select v-model:value="searchFormState.attendStatus" placeholder="点名状态" :options="attendStatusOptions" allow-clear style="width: 120px" @change="loadWeek" />
				<a class="sched-filter-reset" @click="resetScheduleFilters">重置</a>
			</div>
		</div>

		<div class="ent-body" style="padding: 0">
		<!-- 周视图 -->
		<div v-if="viewMode === 'week'" style="padding: 16px; overflow-x: auto">
			<div class="sched-grid">
				<div class="sched-corner">时间</div>
				<div v-for="d in weekDays" :key="d.date" class="sched-head" :class="{ 'is-today': d.date === fmtToday() }">
					{{ d.label }}
					<a-tag v-if="closureMap[d.date]" color="orange" style="margin-left: 6px">停课</a-tag>
				</div>
				<template v-for="slot in timeSlots" :key="slot.start">
					<div class="sched-time">
						{{ slot.start }}<br />{{ slot.end }}
					</div>
					<div
						v-for="d in weekDays"
						:key="d.date + slot.start"
						class="sched-cell"
						:class="{ 'is-today': d.date === fmtToday(), 'is-drop-target': dragOverKey === d.date + '|' + slot.start }"
						@dragover.prevent="onDragOver(d, slot)"
						@dragleave="onDragLeave(d, slot)"
						@drop.prevent="onDrop($event, d, slot)"
					>
						<div
							v-for="s in scheduleMap[d.date + '|' + slot.start] || []"
							:key="s.id"
							class="ent-schedule-card"
							:class="{ 'is-conflict': conflictIdSet.has(s.id), 'is-dragging': draggingSchedule?.id === s.id }"
							:draggable="hasPerm('/course/schedule/change')"
							:title="hasPerm('/course/schedule/change') ? '点击进入点名，拖拽到其他日期/时段可调课' : '点击进入点名'"
							@dragstart="onDragStart($event, s)"
							@dragend="onDragEnd"
							@click="onCardClick(s)"
						>
							<div class="sched-card-head">
								<span class="sched-card-title">{{ s.className || className(s.classId) }}</span>
								<a-tag v-if="conflictIdSet.has(s.id)" color="red" class="sched-card-tag">冲突</a-tag>
								<a-tag v-else :color="attendColor(s.attendStatus)" class="sched-card-tag">
									{{ attendText(s.attendStatus) }}
								</a-tag>
							</div>
							<div class="sched-card-sub">{{ s.classroom || '未设教室' }}</div>
							<div v-if="s.teacherName" class="sched-card-sub2">{{ s.teacherName }}</div>
							<div class="sched-card-attend">
								应到 {{ s.studentCount || 0 }} · 已点 {{ s.attendCount || 0 }}
							</div>
							<div class="sched-card-actions">
								<a-button type="link" size="small" class="sched-card-link" @click.stop="goAttend(s)">点名 ›</a-button>
								<a class="sched-card-link" @click.stop="onOpenChange(s)">调课</a>
								<a class="sched-card-link sched-card-cancel" @click.stop="onCancelScheduleConfirm(s)">取消</a>
							</div>
						</div>
					</div>
				</template>
			</div>
			<div class="ent-note" :class="{ 'is-error': conflictIdSet.size }">
				{{ conflictTip || `本周共 ${scheduleList.length} 节课${conflictIdSet.size ? '，存在 ' + conflictIdSet.size + ' 节冲突（教室或教师撞课）' : ''}${hasPerm('/course/schedule/change') ? ' · 可拖拽课节调整日期/时段' : ''}` }}
			</div>
		</div>

		<!-- 日视图 -->
		<div v-else-if="viewMode === 'day'" style="padding: 16px">
			<div style="margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center">
				<a-tag v-for="item in classroomUsage" :key="item.name" color="blue">{{ item.name }} {{ item.count }} 节</a-tag>
				<a-button v-if="selectedScheduleIds.length" danger size="small" @click="onBatchCancel">
					批量取消（{{ selectedScheduleIds.length }}）
				</a-button>
			</div>
			<div v-if="!dayScheduleList.length" class="ent-empty">当天没有排课</div>
			<a-table
				v-else
				:columns="dayColumns"
				:data-source="dayScheduleList"
				:row-key="(r) => r.id"
				:row-selection="{ selectedRowKeys: selectedScheduleIds, onChange: onSelectSchedules }"
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
					<template v-if="column.dataIndex === 'attendance'">
						<span class="sched-attend-text">应到 {{ record.studentCount || 0 }} · 已点 {{ record.attendCount || 0 }}</span>
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
							<a-divider type="vertical" />
							<a style="color: var(--danger-text)" @click="onCancelScheduleConfirm(record)">取消</a>
						</a-space>
					</template>
				</template>
			</a-table>
		</div>

		<!-- 教师视图 -->
		<div v-else-if="viewMode === 'teacher'" style="padding: 16px">
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
								<span class="sched-teacher-time">
									{{ (s.startTime || '').slice(0, 5) }}-{{ (s.endTime || '').slice(0, 5) }}
								</span>
								<span style="font-size: 12px">{{ s.className || className(s.classId) }}</span>
								<span class="sched-card-sub2">{{ s.classroom || '未设教室' }}</span>
								<a class="sched-card-link sched-card-cancel" style="margin-left: auto" @click.stop="onCancelScheduleConfirm(s)">取消</a>
								<a-tag v-if="conflictIdSet.has(s.id)" color="red" style="font-size: 11px; line-height: 16px; padding: 0 4px">冲突</a-tag>
							</div>
						</div>
					</a-card>
				</a-col>
			</a-row>
		</div>

		<!-- 分屏查看：每个窗口独立筛选，同一周并排对比 -->
		<div v-else-if="viewMode === 'split'" class="sched-split-wrap">
			<div v-for="panel in splitPanels" :key="panel.key" class="sched-split-panel">
				<div class="sched-split-head">
					<span class="sched-split-title">窗口 {{ panel.key }}</span>
					<a-select
						v-model:value="panel.teacherId"
						placeholder="全部教师"
						:options="teacherOptions"
						allow-clear
						show-search
						option-filter-prop="label"
						size="small"
						style="width: 130px"
						@change="loadSplitPanel(panel)"
					/>
					<a-select
						v-model:value="panel.classId"
						placeholder="全部班级"
						:options="classOptions"
						allow-clear
						show-search
						option-filter-prop="label"
						size="small"
						style="width: 140px"
						@change="loadSplitPanel(panel)"
					/>
					<a-input
						v-model:value="panel.classroom"
						placeholder="教室"
						allow-clear
						size="small"
						style="width: 90px"
						@pressEnter="loadSplitPanel(panel)"
					/>
				</div>
				<a-spin :spinning="panel.loading">
					<div class="sched-split-body">
						<div v-if="!panel.list.length" class="ent-empty">本周没有排课</div>
						<div v-for="day in splitPanelDays(panel)" :key="day.date" class="sched-split-day">
							<div class="sched-split-day-label">{{ day.label }}</div>
							<div
								v-for="s in day.list"
								:key="s.id"
								class="ent-schedule-card sched-split-card"
								:class="{ 'is-conflict': conflictIdSet.has(s.id) }"
								@click="goAttend(s)"
							>
								<div class="sched-split-card-top">
									<span class="sched-split-time">{{ startOf(s) }}-{{ endOf(s) }}</span>
									<a-tag :color="attendColor(s.attendStatus)" class="sched-split-tag">{{ attendText(s.attendStatus) }}</a-tag>
								</div>
								<div class="sched-split-class">{{ s.className || className(s.classId) }}</div>
								<div class="sched-split-meta">
									{{ s.teacherName || '未指定教师' }} · {{ s.classroom || '未设教室' }}
								</div>
							</div>
						</div>
					</div>
				</a-spin>
			</div>
		</div>
		</div>
	</div>

	<!-- 排课配置：停课日 / 教师不可用 -->
	<a-modal v-model:open="configOpen" title="排课配置" :width="760" :footer="null" :destroy-on-close="true">
		<a-tabs v-model:activeKey="configTab">
			<a-tab-pane key="closure" tab="停课日">
				<a-space style="margin-bottom: 12px" wrap>
					<a-date-picker v-model:value="closureForm.closureDate" value-format="YYYY-MM-DD" placeholder="停课日期" style="width: 160px" />
					<a-input v-model:value="closureForm.name" placeholder="名称，如 国庆节" style="width: 180px" />
					<a-button type="primary" @click="addClosure">添加</a-button>
				</a-space>
				<a-table :data-source="closureList" :columns="closureColumns" size="small" :pagination="false" row-key="id">
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'closureDate'">{{ (record.closureDate || '').slice(0, 10) }}</template>
						<template v-if="column.dataIndex === 'action'">
							<a style="color: var(--danger-text)" @click="deleteClosure(record)">删除</a>
						</template>
					</template>
				</a-table>
			</a-tab-pane>
			<a-tab-pane key="unavailable" tab="教师不可用">
				<a-space style="margin-bottom: 12px" wrap>
					<a-select
						v-model:value="unavailableForm.teacherId"
						:options="teacherOptions"
						placeholder="选择教师"
						show-search
						option-filter-prop="label"
						style="width: 170px"
					/>
					<a-range-picker
						v-model:value="unavailableRange"
						show-time
						value-format="YYYY-MM-DD HH:mm:ss"
						:placeholder="['开始时间', '结束时间']"
						style="width: 340px"
					/>
					<a-input v-model:value="unavailableForm.reason" placeholder="原因（请假/教研）" style="width: 150px" />
					<a-button type="primary" @click="addUnavailable">添加</a-button>
				</a-space>
				<a-table :data-source="unavailableList" :columns="unavailableColumns" size="small" :pagination="false" row-key="id">
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'teacherId'">{{ teacherName(record.teacherId) }}</template>
						<template v-if="column.dataIndex === 'time'">
							{{ (record.startTime || '').slice(0, 16) }} ~ {{ (record.endTime || '').slice(11, 16) }}
						</template>
						<template v-if="column.dataIndex === 'reason'">{{ record.reason || '-' }}</template>
						<template v-if="column.dataIndex === 'action'">
							<a style="color: var(--danger-text)" @click="deleteUnavailable(record)">删除</a>
						</template>
					</template>
				</a-table>
			</a-tab-pane>
		</a-tabs>
	</a-modal>

	<!-- 复制上周课表 -->
	<a-modal v-model:open="copyOpen" title="复制上周课表" :width="520" :destroy-on-close="true" @ok="onSubmitCopy">
		<a-form layout="vertical">
			<a-form-item label="班级" required>
				<a-select v-model:value="copyForm.classId" placeholder="请选择要复制的班级" :options="classOptions" show-search option-filter-prop="label" />
			</a-form-item>
			<a-form-item label="复制到">
				<span>{{ weekLabel }}（上一周 → 本周）</span>
			</a-form-item>
			<a-alert type="info" show-icon message="停课日、已有相同课节、教室/教师冲突的课节会跳过，并在结果中说明原因。" style="margin-bottom: 12px" />
			<div v-if="copyResult" class="copy-result">
				<div>计划 {{ copyResult.plannedCount || 0 }} 节 · 新增 {{ copyResult.createdCount || 0 }} 节 · 跳过 {{ copyResult.skippedCount || 0 }} 节</div>
				<div v-if="copyResult.skipReasons?.length" class="copy-result-reasons">
					<div v-for="(item, index) in copyResult.skipReasons" :key="index">· {{ item }}</div>
				</div>
			</div>
		</a-form>
	</a-modal>

	<!-- 手动排课 -->
	<a-modal v-model:open="addOpen" title="手动排课" :width="560" :destroy-on-close="true" @ok="onSubmitAdd">
		<a-form ref="addFormRef" :model="addFormData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="班级" name="classId" :rules="[{ required: true, message: '请选择班级' }]">
						<a-select v-model:value="addFormData.classId" placeholder="请选择班级" :options="classOptions" @change="loadSuggest" />
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
						<a-input v-model:value="addFormData.classroom" placeholder="如：A101" allow-clear @blur="loadSuggest" />
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
							@change="loadSuggest"
						/>
					</a-form-item>
				</a-col>
			</a-row>
			<a-alert v-if="addConflicts.length" type="warning" show-icon style="margin-bottom: 12px">
				<template #message>
					<div v-for="(item, index) in addConflicts" :key="index">· {{ item }}</div>
				</template>
			</a-alert>
			<div v-if="addAlternatives.length" class="suggest-box">
				<div class="suggest-title">
					<span>可替代时段</span>
				</div>
				<div class="suggest-list">
					<a-tag
						v-for="item in addAlternatives"
						:key="item.scheduleDate + item.startTime"
						class="suggest-item"
						color="green"
						@click="applySuggest(item)"
					>
						{{ item.label }}
					</a-tag>
				</div>
			</div>
			<!-- 排课助手：按班级/教师/教室占用推荐可用时段 -->
			<div class="suggest-box">
				<div class="suggest-title">
					<span>推荐时段</span>
					<a v-if="suggestList.length" class="suggest-refresh" @click="loadSuggest">换一批</a>
				</div>
				<div v-if="suggestLoading" class="suggest-empty">正在计算可用时段…</div>
				<div v-else-if="!suggestList.length" class="suggest-empty">暂无可推荐时段（可调整日期或教师）</div>
				<div v-else class="suggest-list">
					<a-tag
						v-for="item in suggestList"
						:key="item.scheduleDate + item.startTime"
						class="suggest-item"
						color="blue"
						@click="applySuggest(item)"
					>
						{{ item.label }}
					</a-tag>
				</div>
			</div>
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
				<div v-if="generateResult.skipReasons && generateResult.skipReasons.length" style="color: var(--text-color-tertiary); font-size: 12px; margin-top: 4px">
					<div v-for="(item, index) in generateResult.skipReasons" :key="index">· {{ item }}</div>
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
	import { message, Modal } from 'ant-design-vue'
	import { CopyOutlined, DownOutlined, FileTextOutlined, HistoryOutlined, MoreOutlined, PlusOutlined, ProfileOutlined, SettingOutlined, TeamOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
	import courseApi from '@/api/course/courseApi'
	import { runAsyncExport } from '@/utils/asyncExport'
	import studentApi from '@/api/student/studentApi'
	const router = useRouter()
	const route = useRoute()
	const attendText = (status) => ({ NONE: '待点名', PARTIAL: '部分点名', DONE: '已点名' }[status] || '待点名')
	const attendColor = (status) => ({ NONE: 'orange', PARTIAL: 'gold', DONE: 'green' }[status] || 'default')
	const goAttend = (record) => {
		router.push({
			path: '/course/attend',
			query: {
				classId: record.classId,
				scheduleId: record.id,
				date: record.scheduleDate ? String(record.scheduleDate).slice(0, 10) : undefined
			}
		})
	}

	const canSchedule = computed(
		() =>
			hasPerm('/course/schedule/generate') ||
			hasPerm('/course/schedule/generateAdhoc') ||
			hasPerm('/course/schedule/add')
	)
	const onScheduleMenuClick = ({ key }) => {
		if (key === 'generate') onOpenGenerate()
		else if (key === 'adhoc') onOpenAdhoc()
		else if (key === 'add') onOpenAdd()
	}
	const onMoreMenuClick = ({ key }) => {
		if (key === 'export') onExport()
		else if (key === 'copy') onOpenCopy()
		else if (key === 'template') onOpenTemplate()
		else if (key === 'history') openChangeDrawer()
		else if (key === 'config') openConfig()
	}
	const onOpenCopy = () => {
		copyForm.value = { classId: searchFormState.value.classId || undefined }
		copyResult.value = null
		copyOpen.value = true
	}
	const onSubmitCopy = () => {
		if (!copyForm.value.classId) {
			message.warning('请选择要复制的班级')
			return
		}
		courseApi
			.scheduleCopyLastWeek({
				classId: copyForm.value.classId,
				weekStartDate: fmtDate(monday.value)
			})
			.then((result) => {
				copyResult.value = result || null
				if (Number(result?.createdCount || 0) > 0) loadWeek()
				message.success(`复制完成：新增 ${result?.createdCount || 0} 节，跳过 ${result?.skippedCount || 0} 节`)
			})
			.catch(() => {})
	}

	const searchFormState = ref({})
	const classOptions = ref([])
	const classRecords = ref([])
	const teacherOptions = ref([])
	const scheduleList = ref([])
	const scheduleMap = ref({})
	const draggingSchedule = ref(null)
	const dragOverKey = ref('')
	const suppressCardClick = ref(false)
	const weekDays = ref([])
	const weekLabel = ref('')
	const monday = ref(null)
	const conflictTip = ref('')
	const loading = ref(false)
	const timeSlots = ref([])
	const viewMode = ref('week')
	const dayDate = ref(fmtToday())
	const campusOptions = ref([])
	const attendStatusOptions = [
		{ value: 'NONE', label: '未点名' },
		{ value: 'PARTIAL', label: '部分点名' },
		{ value: 'DONE', label: '已点名' }
	]
	const onViewModeChange = (mode) => {
		if (mode === 'split') {
			loadAllSplitPanels()
			return
		}
		loadWeek()
	}
	const resetScheduleFilters = () => {
		searchFormState.value = {}
		loadWeek()
	}
	// 分屏窗口：每个窗口独立筛选，共用当前周
	const splitPanels = ref([])
	let splitSeq = 0
	const newSplitPanel = () => ({
		key: ++splitSeq,
		teacherId: undefined,
		classId: undefined,
		classroom: undefined,
		list: [],
		loading: false
	})
	const addSplitPanel = () => {
		if (splitPanels.value.length >= 4) return
		const panel = newSplitPanel()
		splitPanels.value.push(panel)
		loadSplitPanel(panel)
	}
	const removeSplitPanel = () => {
		if (splitPanels.value.length <= 1) return
		splitPanels.value.pop()
	}
	const splitPanelDays = (panel) => {
		return weekDays.value
			.map((day) => ({
				...day,
				list: panel.list
					.filter((s) => dateOf(s) === day.date)
					.sort((a, b) => String(a.startTime || '').localeCompare(String(b.startTime || '')))
			}))
			.filter((day) => day.list.length)
	}
	const loadSplitPanel = (panel) => {
		if (!panel) return
		panel.loading = true
		const end = new Date(monday.value)
		end.setDate(monday.value.getDate() + 6)
		courseApi
			.scheduleWeek({
				classId: panel.classId,
				teacherId: panel.teacherId,
				classroom: panel.classroom,
				startDate: fmtDate(monday.value),
				endDate: fmtDate(end)
			})
			.then((data) => {
				panel.list = data || []
			})
			.finally(() => {
				panel.loading = false
			})
	}
	const loadAllSplitPanels = () => {
		if (!splitPanels.value.length) {
			const panel = newSplitPanel()
			splitPanels.value = [panel]
			loadSplitPanel(panel)
			return
		}
		splitPanels.value.forEach(loadSplitPanel)
	}
	const addOpen = ref(false)
	const addFormRef = ref()
	const addFormData = ref({})
	const addConflicts = ref([])
	const addAlternatives = ref([])
	let addConflictTimer = null
	// 复制上周
	const copyOpen = ref(false)
	const copyForm = ref({})
	const copyResult = ref(null)
	// 排课助手：推荐时段
	const suggestList = ref([])
	const suggestLoading = ref(false)
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
		{ title: '应到/已点', dataIndex: 'attendance', width: 130 },
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
			classOptions.value = classRecords.value.map((item) => ({
				value: item.id,
				label: item.classType === 'ONE2ONE' && item.studentName ? `${item.name} · ${item.studentName}` : item.name
			}))
		})
	}
	const loadFilterOptions = () => {
		courseApi.campusOptions().then((data) => {
			campusOptions.value = (data || []).map((item) => ({ value: item.value ?? item.id, label: item.label ?? item.name }))
		}).catch(() => {})
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
	const selectedScheduleIds = ref([])
	const onSelectSchedules = (keys) => {
		selectedScheduleIds.value = keys
	}

	const cancelOne = (id) => courseApi.scheduleCancel({ id })

	// 单节取消：已点名的课节后端会拒绝，这里先给出提示
	const onCancelScheduleConfirm = (schedule) => {
		const timeText = `${dateOf(schedule) || ''} ${(schedule.startTime || '').slice(0, 5)}-${(schedule.endTime || '').slice(0, 5)}`
		Modal.confirm({
			title: '取消排课',
			content: `确定取消 ${timeText} 的这节课吗？已点名的课节需要先撤销点名记录。`,
			okText: '取消排课',
			okType: 'danger',
			cancelText: '返回',
			onOk: () =>
				cancelOne(schedule.id)
					.then(() => {
						message.success('已取消')
						loadWeek()
					})
					.catch(() => {})
		})
	}

	// 批量取消：跳过的课节（已点名/状态异常）会汇总提示
	const onBatchCancel = () => {
		if (!selectedScheduleIds.value.length) return
		const count = selectedScheduleIds.value.length
		Modal.confirm({
			title: '批量取消排课',
			content: `确定取消选中的 ${count} 节课吗？已点名的课节会自动跳过。`,
			okText: '取消排课',
			okType: 'danger',
			cancelText: '返回',
			onOk: () =>
				courseApi
					.scheduleCancelBatch(selectedScheduleIds.value.map((id) => ({ id })))
					.then((data) => {
						const result = data || {}
						message.success(
							`已取消 ${result.canceledCount || 0} 节` +
								(result.skippedCount ? `，跳过 ${result.skippedCount} 节` : '')
						)
						if (result.skippedList && result.skippedList.length) {
							Modal.info({
								title: '以下课节未取消',
								content: result.skippedList.join('；')
							})
						}
						selectedScheduleIds.value = []
						loadWeek()
					})
					.catch(() => {})
		})
	}

	const configOpen = ref(false)
	const configTab = ref('closure')
	const closureList = ref([])
	const closureMap = ref({})
	const closureForm = ref({})
	const closureColumns = [
		{ title: '日期', dataIndex: 'closureDate', width: 140 },
		{ title: '名称', dataIndex: 'name' },
		{ title: '操作', dataIndex: 'action', width: 80, align: 'center' }
	]
	const unavailableList = ref([])
	const unavailableForm = ref({})
	const unavailableRange = ref([])
	const unavailableColumns = [
		{ title: '教师', dataIndex: 'teacherId', width: 120 },
		{ title: '不可用时间', dataIndex: 'time' },
		{ title: '原因', dataIndex: 'reason' },
		{ title: '操作', dataIndex: 'action', width: 80, align: 'center' }
	]

	const teacherName = (id) => teacherOptions.value.find((item) => item.value === id)?.label || id

	const addDays = (days) => {
		const date = new Date()
		date.setDate(date.getDate() + days)
		return fmtDate(date)
	}

	const loadClosures = () =>
		courseApi
			.closureList({ startDate: `${addDays(-30)} 00:00:00`, endDate: `${addDays(180)} 23:59:59` })
			.then((data) => {
				closureList.value = data || []
			})
			.catch(() => {
				closureList.value = []
			})

	// 周视图表头标记停课
	const loadWeekClosures = () => {
		if (!monday.value) return Promise.resolve()
		const end = new Date(monday.value)
		end.setDate(end.getDate() + 6)
		return courseApi
			.closureList({ startDate: `${fmtDate(monday.value)} 00:00:00`, endDate: `${fmtDate(end)} 23:59:59` })
			.then((data) => {
				const map = {}
				;(data || []).forEach((item) => {
					map[String(item.closureDate).slice(0, 10)] = item.name || '停课'
				})
				closureMap.value = map
			})
			.catch(() => {
				closureMap.value = {}
			})
	}

	const openConfig = () => {
		configOpen.value = true
		loadClosures()
		loadUnavailable()
	}

	const addClosure = () => {
		if (!closureForm.value.closureDate) {
			message.warning('请选择停课日期')
			return
		}
		courseApi
			.closureAdd({ closureDate: `${closureForm.value.closureDate} 00:00:00`, name: closureForm.value.name })
			.then(() => {
				message.success('已添加停课日')
				closureForm.value = {}
				loadClosures()
				loadWeekClosures()
			})
			.catch(() => {})
	}

	const deleteClosure = (record) => {
		Modal.confirm({
			title: '删除停课日',
			content: `确定删除 ${String(record.closureDate).slice(0, 10)} 的停课设置吗？`,
			okType: 'danger',
			onOk: () =>
				courseApi.closureDelete({ id: record.id }).then(() => {
					message.success('已删除')
					loadClosures()
					loadWeekClosures()
				})
		})
	}

	const loadUnavailable = () =>
		courseApi
			.teacherUnavailableList({ startTime: `${addDays(-7)} 00:00:00`, endTime: `${addDays(60)} 23:59:59` })
			.then((data) => {
				unavailableList.value = data || []
			})
			.catch(() => {
				unavailableList.value = []
			})

	const addUnavailable = () => {
		if (!unavailableForm.value.teacherId || !unavailableRange.value || unavailableRange.value.length !== 2) {
			message.warning('请选择教师和不可用时间')
			return
		}
		courseApi
			.teacherUnavailableAdd({
				teacherId: unavailableForm.value.teacherId,
				startTime: unavailableRange.value[0],
				endTime: unavailableRange.value[1],
				reason: unavailableForm.value.reason
			})
			.then(() => {
				message.success('已添加不可用时段')
				unavailableForm.value = {}
				unavailableRange.value = []
				loadUnavailable()
			})
			.catch(() => {})
	}

	const deleteUnavailable = (record) => {
		Modal.confirm({
			title: '删除不可用时段',
			content: '确定删除这条不可用时段吗？',
			okType: 'danger',
			onOk: () =>
				courseApi.teacherUnavailableDelete({ id: record.id }).then(() => {
					message.success('已删除')
					loadUnavailable()
				})
		})
	}

	const loadWeek = () => {
		const end = new Date(monday.value)
		end.setDate(monday.value.getDate() + 6)
		loading.value = true
		courseApi
			.scheduleWeek({
				classId: searchFormState.value.classId,
				teacherId: searchFormState.value.teacherId,
				classroom: searchFormState.value.classroom,
				campusId: searchFormState.value.campusId,
				courseId: searchFormState.value.courseId,
				attendStatus: searchFormState.value.attendStatus,
				startDate: fmtDate(monday.value),
				endDate: fmtDate(end)
			})
			.then((data) => {
				scheduleList.value = data || []
				renderSchedule()
				loadWeekClosures()
				if (viewMode.value === 'split') loadAllSplitPanels()
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

	const onOpenAdd = () => {
		addFormData.value = { classId: searchFormState.value.classId, startTime: '09:00', endTime: '10:30' }
		addConflicts.value = []
		addAlternatives.value = []
		addOpen.value = true
		loadSuggest()
		checkAddConflicts()
	}

	/** 手动排课实时预检：冲突对象 + 可替代时段 */
	const checkAddConflicts = () => {
		if (addConflictTimer) clearTimeout(addConflictTimer)
		const form = addFormData.value
		if (!addOpen.value || !form.scheduleDate || !form.startTime || !form.endTime) {
			addConflicts.value = []
			addAlternatives.value = []
			return
		}
		addConflictTimer = setTimeout(() => {
			courseApi
				.scheduleConflictCheckBatch({
					classId: form.classId,
					teacherId: form.teacherId,
					classroom: form.classroom,
					slots: [
						{
							scheduleDate: form.scheduleDate,
							startTime: form.startTime,
							endTime: form.endTime
						}
					]
				})
				.then((list) => {
					const first = (list || [])[0]
					addConflicts.value = first?.conflicts || []
					addAlternatives.value = (first?.alternatives || []).slice(0, 6)
				})
				.catch(() => {
					addConflicts.value = []
					addAlternatives.value = []
				})
		}, 250)
	}
	watch(
		() => [
			addFormData.value.classId,
			addFormData.value.scheduleDate,
			addFormData.value.startTime,
			addFormData.value.endTime,
			addFormData.value.teacherId,
			addFormData.value.classroom
		],
		() => checkAddConflicts()
	)

	// 排课助手：按班级/教师/教室的占用推荐可用时段（默认看未来 7 天）
	const loadSuggest = () => {
		if (!addOpen.value) return
		suggestLoading.value = true
		const start = addFormData.value.scheduleDate || fmtDate(new Date())
		const endDate = new Date(`${start}T00:00:00`)
		endDate.setDate(endDate.getDate() + 6)
		courseApi
			.scheduleSuggest({
				classId: addFormData.value.classId,
				teacherId: addFormData.value.teacherId,
				classroom: addFormData.value.classroom,
				startDate: start,
				endDate: fmtDate(endDate),
				durationMinutes: 90,
				limit: 8
			})
			.then((data) => {
				suggestList.value = data || []
			})
			.catch(() => {
				suggestList.value = []
			})
			.finally(() => {
				suggestLoading.value = false
			})
	}

	// 点推荐时段直接回填表单
	const applySuggest = (item) => {
		addFormData.value.scheduleDate = item.scheduleDate
		addFormData.value.startTime = item.startTime.slice(0, 5)
		addFormData.value.endTime = item.endTime.slice(0, 5)
		checkAddConflicts()
		message.success(`已填入 ${item.label}`)
	}
	const onSubmitAdd = () => {
		addFormRef.value.validate().then(() => {
			if (addConflicts.value.length) {
				message.warning('当前时段存在冲突，请先调整时间或点选可替代时段')
				return
			}
			const body = { ...addFormData.value, scheduleDate: `${addFormData.value.scheduleDate} 00:00:00` }
			courseApi
				.scheduleAdd(body)
				.then(() => {
					addOpen.value = false
					addConflicts.value = []
					addAlternatives.value = []
					conflictTip.value = ''
					loadWeek()
				})
				.catch((err) => {
					// 冲突时不关闭弹窗，直接刷新推荐时段让用户点选替换
					const msg = String(err?.msg || '')
					if (msg.includes('冲突')) {
						message.warning('该时段冲突，已为你重新计算可用时段')
						loadSuggest()
						checkAddConflicts()
					}
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
		// 走通用异步导出：课表含清单 + 周视图两个 sheet，后台生成后自动下载
		runAsyncExport('COURSE_SCHEDULE', {
			classId: searchFormState.value.classId,
			teacherId: searchFormState.value.teacherId,
			classroom: searchFormState.value.classroom,
			startDate: fmtDate(monday.value),
			endDate: fmtDate(end)
		})
	}

	const onCardClick = (record) => {
		if (suppressCardClick.value) return
		// 点卡片进点名，调课走卡片上的「调课」入口
		goAttend(record)
	}
	const onDragStart = (event, record) => {
		draggingSchedule.value = record
		dragOverKey.value = ''
		suppressCardClick.value = true
		if (event.dataTransfer) {
			event.dataTransfer.effectAllowed = 'move'
			event.dataTransfer.setData('text/plain', record.id)
		}
	}
	const onDragEnd = () => {
		draggingSchedule.value = null
		dragOverKey.value = ''
		setTimeout(() => (suppressCardClick.value = false), 120)
	}
	const onDragOver = (day, slot) => {
		if (!draggingSchedule.value) return
		dragOverKey.value = `${day.date}|${slot.start}`
	}
	const onDragLeave = (day, slot) => {
		if (dragOverKey.value === `${day.date}|${slot.start}`) dragOverKey.value = ''
	}
	const moveConflicts = (record, date, start, end) => {
		const startMin = toMinutes(start)
		const endMin = toMinutes(end)
		return scheduleList.value.filter((item) => {
			if (item.id === record.id || dateOf(item) !== date) return false
			const overlap = toMinutes(startOf(item)) < endMin && toMinutes(endOf(item)) > startMin
			if (!overlap) return false
			const sameRoom = record.classroom && item.classroom && record.classroom === item.classroom
			const sameTeacher = record.teacherId && item.teacherId && record.teacherId === item.teacherId
			return sameRoom || sameTeacher
		})
	}
	const applyDragChange = (record, date, start, end) =>
		courseApi.scheduleChange({
			scheduleId: record.id,
			scheduleDate: `${date} 00:00:00`,
			startTime: start,
			endTime: end,
			classroom: record.classroom,
			teacherId: record.teacherId,
			changeReason: '拖拽调课'
		})
	const onDrop = (event, day, slot) => {
		event.preventDefault()
		const record = draggingSchedule.value
		draggingSchedule.value = null
		dragOverKey.value = ''
		if (!record) return
		setTimeout(() => (suppressCardClick.value = false), 120)
		const newStart = slot.start
		const duration = Math.max(30, toMinutes(endOf(record)) - toMinutes(startOf(record)))
		const newEnd = toTimeText(toMinutes(newStart) + duration)
		if (toMinutes(newStart) + duration >= 24 * 60) {
			message.warning('调课后结束时间超过当天 24:00，请选择更早的时段')
			return
		}
		if (dateOf(record) === day.date && startOf(record) === newStart) return
		if (closureMap.value[day.date]) {
			message.warning(`${day.date} 是停课日【${closureMap.value[day.date]}】，不能调课`)
			return
		}
		const conflicts = moveConflicts(record, day.date, newStart, newEnd)
		if (conflicts.length) {
			const names = [...new Set(conflicts.map((item) => item.className || className(item.classId)))].join('、')
			message.warning(`目标时段与 ${names} 冲突，不能调课`)
			return
		}
		Modal.confirm({
			title: '拖拽调课',
			content: `将 ${record.className || className(record.classId)} 从 ${dateOf(record)} ${startOf(record)}-${endOf(record)} 调整到 ${day.date} ${newStart}-${newEnd}，确认调课？`,
			okText: '确认调课',
			cancelText: '取消',
			onOk: () =>
				applyDragChange(record, day.date, newStart, newEnd)
					.then(() => {
						message.success('调课成功')
						loadWeek()
					})
					.catch(() => {})
		})
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
	loadFilterOptions()
	loadAdhocOptions()
	buildWeek()
</script>

<style lang="less" scoped>
	.ent-schedule-card[draggable='true'] {
		cursor: grab;
	}
	.ent-schedule-card.is-dragging {
		opacity: 0.45;
	}
	.sched-cell.is-drop-target {
		outline: 2px dashed var(--primary-6);
		outline-offset: -2px;
		background: var(--primary-1);
	}
	.sched-card-attend {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}

	.sched-attend-text {
		font-size: 12px;
		color: var(--text-color-secondary);
	}

	.sched-card-actions {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.sched-card-cancel {
		font-size: 12px;
		color: var(--danger-text);
		cursor: pointer;

		&:hover {
			opacity: 0.8;
		}
	}

	.suggest-box {
		margin-top: 4px;
		padding: 10px 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
	}
	.suggest-title {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.suggest-refresh {
		font-size: 12px;
	}
	.suggest-list {
		display: flex;
		flex-wrap: wrap;
		gap: 6px;
		margin-top: 8px;
	}
	.suggest-item {
		cursor: pointer;
		margin: 0;
	}
	.suggest-empty {
		margin-top: 8px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.copy-result {
		padding: 10px 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
		font-size: 13px;
	}
	.copy-result-reasons {
		margin-top: 6px;
		max-height: 160px;
		overflow-y: auto;
		font-size: 12px;
		color: var(--text-color-secondary);
	}

	.sched-toolbar {
		display: flex;
		flex-direction: column;
		align-items: stretch;
		gap: 10px;
	}
	.sched-view-row,
	.sched-filter-row {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-wrap: wrap;
	}
	.sched-filter-reset {
		color: var(--text-color-secondary);
		font-size: 13px;
	}
	.sched-split-wrap {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 12px;
		padding: 16px;
	}
	.sched-split-panel {
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		background: #fff;
		padding: 12px;
		min-width: 0;
	}
	.sched-split-head {
		display: flex;
		align-items: center;
		gap: 6px;
		flex-wrap: wrap;
		margin-bottom: 10px;
	}
	.sched-split-title {
		font-size: 13px;
		font-weight: 600;
	}
	.sched-split-body {
		max-height: 520px;
		overflow-y: auto;
	}
	.sched-split-day {
		margin-bottom: 10px;
	}
	.sched-split-day-label {
		font-size: 12px;
		color: var(--text-color-tertiary);
		margin-bottom: 4px;
	}
	.sched-split-card {
		margin-bottom: 6px;
		padding: 8px;
	}
	.sched-split-card-top {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
	}
	.sched-split-time {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.sched-split-class {
		margin-top: 2px;
		font-size: 13px;
		font-weight: 500;
	}
	.sched-split-meta {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	@media (max-width: 1100px) {
		.sched-split-wrap {
			grid-template-columns: minmax(0, 1fr);
		}
	}
</style>
