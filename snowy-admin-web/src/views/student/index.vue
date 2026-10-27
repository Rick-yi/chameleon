<template>
	<div class="student-page">
		<page-header title="学员管理" desc="学员档案、课时账户与报名入口">
			<template #extra>
				<a-space>
					<a-button v-if="hasPerm('/student/import')" @click="openImport">
						<template #icon><import-outlined /></template>
						批量导入
					</a-button>
					<a-button v-if="hasPerm('/student/export')" @click="onExport">
						<template #icon><export-outlined /></template>
						导出
					</a-button>
					<a-button v-if="hasPerm('/student/add')" type="primary" @click="onOpenAdd">
						<template #icon><plus-outlined /></template>
						新增学员
					</a-button>
				</a-space>
			</template>
		</page-header>
		<div class="ent-toolbar">
			<a-form layout="inline" :model="searchFormState">
				<a-form-item label="姓名/手机">
					<a-input v-model:value="searchFormState.searchKey" placeholder="请输入姓名或家长手机" allow-clear style="width: 220px" />
				</a-form-item>
				<a-form-item label="校区">
					<a-select
						v-model:value="searchFormState.campusId"
						placeholder="全部校区"
						:options="campusOptions"
						allow-clear
						show-search
						option-filter-prop="label"
						style="width: 150px"
					/>
				</a-form-item>
				<a-form-item label="状态">
					<a-select v-model:value="searchFormState.status" placeholder="全部状态" :options="statusOptions" allow-clear style="width: 140px" />
				</a-form-item>
				<a-form-item label="标签">
					<a-select
						v-model:value="searchFormState.tagId"
						placeholder="全部标签"
						:options="tagOptions"
						allow-clear
						show-search
						option-filter-prop="label"
						style="width: 150px"
					/>
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="onSearch">
							<template #icon><SearchOutlined /></template>
							查询
						</a-button>
						<a-button @click="reset">
							<template #icon><redo-outlined /></template>
							重置
						</a-button>
					</a-space>
				</a-form-item>
			</a-form>
		</div>
		<div class="ent-body">
		<s-table
			v-if="!isMobile"
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:scroll="{ x: 'max-content' }"
		>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'remainLessons'">
					<span :class="lessonTone(record)">{{ lessonText(record) }}</span>
				</template>
				<template v-if="column.dataIndex === 'accountExpireTime'">
					<span :class="{ 'text-danger': isExpired(record.accountExpireTime) }">{{ formatDate(record.accountExpireTime) }}</span>
				</template>
				<template v-if="column.dataIndex === 'classNames'">
					{{ record.classNames || '-' }}
				</template>
				<template v-if="column.dataIndex === 'unpaidAmount'">
					<span :class="{ 'text-warning': Number(record.unpaidAmount) > 0 }">{{ formatMoney(record.unpaidAmount) }}</span>
				</template>
				<template v-if="column.dataIndex === 'status'">
						<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
						<span v-if="record.statusReason" class="status-reason" :title="record.statusReason">{{ record.statusReason }}</span>
					</template>
					<template v-if="column.dataIndex === 'tags'">
						<a-space :size="4" wrap>
							<a-tag v-for="tag in record.tagList || []" :key="tag.id" :color="tag.color || 'blue'">{{ tag.name }}</a-tag>
							<span v-if="!(record.tagList || []).length" class="text-muted">-</span>
						</a-space>
					</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="openDetail(record)">详情</a>
						<a-divider type="vertical" />
						<a v-if="hasPerm('/student/order/add')" @click="openEnrollment(record)">报名</a>
						<a-divider type="vertical" />
						<table-more :actions="studentMoreActions" @select="(action) => onStudentMore(action, record)" />
					</a-space>
				</template>
			</template>
		</s-table>
			<div v-else class="student-mobile">
				<a-spin :spinning="mobileLoading">
					<div v-if="!mobileStudents.length" class="ent-empty">
						<div>没有符合条件的学员</div>
						<a-button v-if="hasPerm('/student/add')" type="primary" style="margin-top: 10px" @click="onOpenAdd">新增学员</a-button>
					</div>
					<div v-for="item in mobileStudents" :key="item.id" class="student-card">
						<div class="student-card-head">
							<div class="student-card-name">
								{{ item.name }}
								<a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
							</div>
							<div class="student-card-remain" :class="lessonTone(item)">
								{{ lessonText(item) }}<span v-if="item.hasLessonAccount !== false">课时</span>
							</div>
						</div>
						<div class="student-card-line">
							<span>
								{{ item.parentName || '家长' }} · {{ item.parentPhone || '-' }}
								<span v-if="item.campusId" class="student-card-campus"> · {{ campusNameMap[item.campusId] || '校区' }}</span>
							</span>
							<a v-if="item.parentPhone" :href="'tel:' + item.parentPhone">拨号</a>
						</div>
						<div v-if="(item.tagList || []).length || item.statusReason" class="student-card-tags">
							<a-tag v-for="tag in item.tagList || []" :key="tag.id" :color="tag.color || 'blue'">{{ tag.name }}</a-tag>
							<span v-if="item.statusReason" class="student-card-reason">{{ item.statusReason }}</span>
						</div>
						<div class="student-card-grid">
							<div>
								<span>课包到期</span>
								<b :class="{ 'text-danger': isExpired(item.accountExpireTime) }">{{ formatDate(item.accountExpireTime) }}</b>
							</div>
							<div>
								<span>所在班级</span>
								<b>{{ item.classNames || '-' }}</b>
							</div>
							<div>
								<span>待收款</span>
								<b :class="{ 'text-warning': Number(item.unpaidAmount) > 0 }">{{ formatMoney(item.unpaidAmount) }}</b>
							</div>
						</div>
						<div class="student-card-actions">
							<a-button size="small" @click="openDetail(item)">详情</a-button>
							<a-button v-if="hasPerm('/student/order/add')" size="small" @click="openEnrollment(item)">报名</a-button>
							<table-more :actions="studentMoreActions" @select="(action) => onStudentMore(action, item)" />
						</div>
					</div>
					<div v-if="mobileTotal > mobilePage.size" class="student-mobile-pager">
						<a-pagination
							simple
							:current="mobilePage.current"
							:page-size="mobilePage.size"
							:total="mobileTotal"
							@change="onMobilePageChange"
						/>
					</div>
				</a-spin>
			</div>
		</div>
	</div>

	<enrollment-flow v-model:open="enrollOpen" :student="enrollStudent" @success="refreshList" />

	<!-- 学员详情：抽屉内展示，保留列表上下文，避免独立页面来回切换 -->
	<a-drawer v-model:open="detailOpen" :width="880" :title="detailStudentName || '学员详情'" destroy-on-close>
		<student-detail v-if="detailOpen" :key="detailKey" :student-id="detailStudentId" />
	</a-drawer>

	<a-modal v-model:open="open" :title="formData.id ? '编辑学员' : '新增学员'" :width="600" :destroy-on-close="true" @ok="onSubmit">
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="学员姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
						<a-input v-model:value="formData.name" placeholder="请输入学员姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="性别" name="gender">
						<a-radio-group v-model:value="formData.gender" :options="[{ value: '男', label: '男' }, { value: '女', label: '女' }]" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="生日" name="birthday">
						<a-date-picker v-model:value="formData.birthday" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="家长姓名" name="parentName">
						<a-input v-model:value="formData.parentName" placeholder="请输入家长姓名" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="家长手机" name="parentPhone">
						<a-input v-model:value="formData.parentPhone" placeholder="选填，用于家长端绑定与通知" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="状态" name="status">
						<a-radio-group v-model:value="formData.status" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="校区" name="campusId" :rules="[{ required: true, message: '请选择校区' }]">
						<a-select
							v-model:value="formData.campusId"
							:options="campusOptions"
							placeholder="请选择校区"
							show-search
							option-filter-prop="label"
						/>
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="备注" name="remark">
						<a-textarea v-model:value="formData.remark" :auto-size="{ minRows: 2, maxRows: 4 }" />
					</a-form-item>
				</a-col>
			</a-row>
			</a-form>
		</a-modal>

		<!-- 批量导入：模板下载 + 错误行回显 + 可重试 -->
		<a-modal v-model:open="importOpen" title="批量导入学员" :width="720" :footer="null" :destroy-on-close="true">
			<div class="import-tip">
				请先下载模板按格式填写。导入按行校验，失败行会显示行号与原因；修正后可只保留失败行再次上传重试。
			</div>
			<a-space style="margin-bottom: 12px">
				<a-button @click="downloadTemplate">
					<template #icon><download-outlined /></template>
					下载模板
				</a-button>
				<a-button v-if="importResult" @click="resetImport">重新上传</a-button>
			</a-space>
			<a-spin :spinning="importLoading">
				<a-upload-dragger v-if="!importResult" :show-upload-list="false" :custom-request="customImport" accept=".xls,.xlsx">
					<p class="ant-upload-drag-icon"><inbox-outlined /></p>
					<p class="ant-upload-text">单击或拖动 Excel 文件到此区域</p>
					<p class="ant-upload-hint">仅支持 xls / xlsx，单次最多 2000 行</p>
				</a-upload-dragger>
			</a-spin>
			<a-alert v-if="importResult" type="info" show-icon style="margin-top: 12px">
				<template #message>
					<div>导入总数：{{ importResult.totalCount }} 条 · 成功 {{ importResult.successCount }} 条 · 失败 {{ importResult.errorCount }} 条</div>
					<div v-if="importResult.errorCount > 0" style="margin-top: 8px">
						<a-table
							:data-source="importResult.errorDetail || []"
							:columns="importErrorColumns"
							:row-key="(r) => r.rowNum"
							size="small"
							:pagination="false"
							bordered
						/>
					</div>
				</template>
			</a-alert>
		</a-modal>

		<!-- 状态变更：停课/休学/退学/结课/复课 -->
		<a-modal v-model:open="statusOpen" title="学员状态变更" :width="480" :destroy-on-close="true" @ok="onSubmitStatus">
			<a-form :model="statusForm" layout="vertical">
				<a-alert
					type="info"
					show-icon
					style="margin-bottom: 12px"
					message="状态变更会写入变更记录；停课/休学后如需恢复上课，请选择「在读」完成复课。"
				/>
				<a-form-item label="目标状态" required>
					<a-select v-model:value="statusForm.status" :options="statusChangeOptions" />
				</a-form-item>
				<a-form-item label="变更原因" :required="statusForm.status !== 'STUDYING'">
					<a-textarea
						v-model:value="statusForm.reason"
						:auto-size="{ minRows: 3, maxRows: 5 }"
						:placeholder="statusForm.status === 'STUDYING' ? '复课备注（选填）' : '请填写原因，如：家长申请、搬迁、课程结束'"
					/>
				</a-form-item>
			</a-form>
		</a-modal>

		<!-- 学员标签绑定 -->
		<a-modal v-model:open="tagBindOpen" title="设置学员标签" :width="480" :destroy-on-close="true" @ok="onSubmitTags">
			<a-form layout="vertical">
				<a-form-item label="标签（可多选）">
					<a-select
						v-model:value="tagBindForm.tagIdList"
						mode="multiple"
						:options="tagOptions"
						placeholder="请选择标签，可多选"
						allow-clear
						option-filter-prop="label"
					/>
				</a-form-item>
				<a class="tag-manage-link" @click="openTagManage">管理标签字典</a>
			</a-form>
		</a-modal>

		<!-- 标签字典管理 -->
		<a-modal v-model:open="tagManageOpen" title="标签字典" :width="560" :footer="null" :destroy-on-close="true">
			<a-space style="margin-bottom: 12px" wrap>
				<a-input v-model:value="newTag.name" placeholder="新标签名称" style="width: 180px" allow-clear />
				<a-select v-model:value="newTag.color" style="width: 120px" :options="tagColorOptions" />
				<a-input-number v-model:value="newTag.sortCode" :min="0" placeholder="排序" style="width: 100px" />
				<a-button type="primary" @click="onAddTag">新增</a-button>
			</a-space>
			<a-table :data-source="tagList" :columns="tagColumns" :row-key="(r) => r.id" size="small" :pagination="false" bordered>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'name'">
						<a-tag :color="record.color || 'blue'">{{ record.name }}</a-tag>
						<a-input v-if="record.editing" v-model:value="record.editName" size="small" style="width: 140px; margin-left: 8px" />
					</template>
					<template v-if="column.dataIndex === 'action'">
						<a-space>
							<a v-if="!record.editing" @click="record.editing = true; record.editName = record.name">重命名</a>
							<a v-else @click="onEditTag(record)">保存</a>
							<a-divider type="vertical" />
							<a-popconfirm title="删除后学员身上的该标签也会移除，确定吗？" @confirm="onDeleteTag(record)">
								<a class="text-danger">删除</a>
							</a-popconfirm>
						</a-space>
					</template>
				</template>
			</a-table>
		</a-modal>
	</template>

<script setup name="studentIndex">
	import { useRoute, useRouter } from 'vue-router'
	import { message } from 'ant-design-vue'
	import StudentDetail from './detail.vue'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import downloadUtil from '@/utils/downloadUtil'
	import { globalStore } from '@/store'
	const router = useRouter()
	const route = useRoute()
	const store = globalStore()
	const isMobile = computed(() => store.isMobile)
	const searchFormState = ref({})
	const tableRef = ref()
	const mobileStudents = ref([])
	const mobileTotal = ref(0)
	const mobilePage = ref({ current: 1, size: 10 })
	const mobileLoading = ref(false)
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)
	const enrollOpen = ref(false)
	const enrollStudent = ref(null)
	const statusOptions = [
		{ value: 'STUDYING', label: '在读' },
		{ value: 'STOP', label: '停课' },
		{ value: 'SUSPEND', label: '休学' },
		{ value: 'WITHDRAWN', label: '退学' },
		{ value: 'GRADUATED', label: '结课' },
		{ value: 'REFUNDED', label: '已退费' }
	]
	const statusChangeOptions = statusOptions.filter((item) => item.value !== 'REFUNDED')
	const columns = [
		{ title: '学员姓名', dataIndex: 'name' },
		{ title: '性别', dataIndex: 'gender' },
		{ title: '家长手机', dataIndex: 'parentPhone' },
		{ title: '家长姓名', dataIndex: 'parentName' },
		{ title: '剩余课时', dataIndex: 'remainLessons', width: 110, align: 'right' },
		{ title: '课包到期', dataIndex: 'accountExpireTime', width: 120 },
		{ title: '所在班级', dataIndex: 'classNames', width: 160 },
		{ title: '待收款', dataIndex: 'unpaidAmount', width: 110, align: 'right' },
		{ title: '标签', dataIndex: 'tags', width: 160 },
		{ title: '状态', dataIndex: 'status' },
		{ title: '创建时间', dataIndex: 'createTime' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const formatLessons = (value) => {
		const number = Number(value || 0)
		return Number.isInteger(number) ? String(number) : number.toFixed(2)
	}
	const lessonText = (record) => (record.hasLessonAccount === false ? '未报名' : formatLessons(record.remainLessons))
	const lessonTone = (record) => {
		if (record.hasLessonAccount === false) return 'text-muted'
		const number = Number(record.remainLessons || 0)
		if (number <= 0) return 'text-danger'
		if (number < 3) return 'text-warning'
		return ''
	}
	const formatDate = (value) => (value ? String(value).slice(0, 10) : '-')
	const isExpired = (value) => {
		if (!value) return false
		const date = new Date(String(value).replace(/-/g, '/'))
		return !Number.isNaN(date.getTime()) && date < new Date()
	}
	const formatMoney = (value) => `¥${Number(value || 0).toFixed(2)}`
	const loadData = (parameter) => {
		return studentApi.studentPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	// 手机端卡片列表：与桌面表格共用筛选条件，独立分页
	const loadMobile = (page = 1) => {
		mobileLoading.value = true
		const parameter = { current: page, size: mobilePage.value.size, ...searchFormState.value }
		return studentApi
			.studentPage(parameter)
			.then((data) => {
				mobileStudents.value = data.records || []
				mobileTotal.value = data.total || 0
				mobilePage.value.current = page
			})
			.catch(() => {
				mobileStudents.value = []
				mobileTotal.value = 0
			})
			.finally(() => {
				mobileLoading.value = false
			})
	}
	const onSearch = () => {
		if (isMobile.value) {
			loadMobile(1)
		} else {
			tableRef.value?.refresh(true)
		}
	}
	const refreshList = () => {
		if (isMobile.value) {
			return loadMobile(mobilePage.value.current)
		}
		tableRef.value?.refresh()
	}
	const onMobilePageChange = (page) => loadMobile(page)
	watch(
		isMobile,
		(value) => {
			if (value) loadMobile(1)
		},
		{ immediate: true }
	)
	// 支持从报表带参下钻（状态）
	const applyDrillParams = () => {
		if (route.query.status) {
			searchFormState.value.status = route.query.status
		}
	}
	applyDrillParams()
	const reset = () => {
		searchFormState.value = {}
		onSearch()
	}
	const statusText = (status) => {
		const item = statusOptions.find((i) => i.value === status)
		return item ? item.label : status
	}
	const statusColor = (status) => {
		if (status === 'STUDYING') return 'green'
		if (status === 'STOP') return 'orange'
		if (status === 'SUSPEND') return 'gold'
		if (status === 'GRADUATED') return 'blue'
		if (status === 'WITHDRAWN') return 'red'
		return 'red'
	}
	const onExport = () => {
		studentApi
			.studentExport({ ...searchFormState.value })
			.then((res) => downloadUtil.resultDownload(res))
			.catch(() => message.error('导出失败，请稍后重试'))
	}
	const openEnrollment = (record) => {
		enrollStudent.value = record
		enrollOpen.value = true
	}
	const onOpenAdd = () => {
		// 新增学员直接走三步报名：建档 + 课包/班级 + 收款，避免建档后再补报名
		enrollStudent.value = null
		enrollOpen.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		if (!formData.value.campusId && campusOptions.value.length === 1) {
			formData.value.campusId = campusOptions.value[0].value
		}
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? studentApi.studentEdit : studentApi.studentAdd
			api(formData.value).then(() => {
				open.value = false
				// 新增学员回到第一页，确保能看到刚建档的记录
				if (!edit) mobilePage.value.current = 1
				refreshList()
			})
		})
	}
	const onDelete = (record) => {
		studentApi.studentDelete([{ id: record.id }]).then(() => refreshList())
	}
	// 操作列：次要操作收进「更多」下拉
	const studentMoreActions = [
		{ key: 'edit', label: '编辑' },
		{ key: 'status', label: '状态变更' },
		{ key: 'tags', label: '设置标签' },
		{ key: 'delete', label: '删除', danger: true, confirmTitle: '确定删除该学员吗？', confirmContent: '删除后该学员不再出现在列表中。' }
	]
	const onStudentMore = (action, record) => {
		if (action.key === 'edit') {
			onOpenEdit(record)
		} else if (action.key === 'status') {
			openStatus(record)
		} else if (action.key === 'tags') {
			openTagBind(record)
		} else if (action.key === 'delete') {
			onDelete(record)
		}
	}
	// 详情抽屉：状态/标签变更后通过 key 强制刷新
	const detailKey = ref(0)
	const bumpDetail = () => {
		detailKey.value += 1
	}
	// 批量导入
	const importOpen = ref(false)
	const importLoading = ref(false)
	const importResult = ref(null)
	const importErrorColumns = [
		{ title: '行号', dataIndex: 'rowNum', width: 70 },
		{ title: '学员', dataIndex: 'name', width: 110 },
		{ title: '家长手机', dataIndex: 'parentPhone', width: 130 },
		{ title: '失败原因', dataIndex: 'msg' }
	]
	const openImport = () => {
		importResult.value = null
		importOpen.value = true
	}
	const resetImport = () => {
		importResult.value = null
	}
	const downloadTemplate = () => {
		studentApi
			.studentImportTemplate()
			.then((res) => downloadUtil.resultDownload(res))
			.catch(() => message.error('模板下载失败，请稍后重试'))
	}
	const customImport = (data) => {
		const extension = '.'.concat(data.file.name.split('.').slice(-1).toString().toLowerCase())
		if (!['.xls', '.xlsx'].includes(extension)) {
			message.warning('仅支持 xls / xlsx 格式文件')
			return false
		}
		importLoading.value = true
		const fileData = new FormData()
		fileData.append('file', data.file)
		return studentApi
			.studentImport(fileData)
			.then((res) => {
				importResult.value = res
				if (Number(res?.successCount) > 0) {
					refreshList()
					if (Number(res?.errorCount) === 0) {
						message.success(`导入完成，成功 ${res.successCount} 条`)
					}
				}
			})
			.catch(() => message.error('导入失败，请检查文件后重试'))
			.finally(() => {
				importLoading.value = false
			})
	}
	// 状态变更
	const statusOpen = ref(false)
	const statusForm = ref({ studentId: '', status: 'STOP', reason: '' })
	const openStatus = (record) => {
		statusForm.value = {
			studentId: record.id,
			status: record.status === 'STUDYING' ? 'STOP' : 'STUDYING',
			reason: ''
		}
		statusOpen.value = true
	}
	const onSubmitStatus = () => {
		const form = statusForm.value
		if (form.status !== 'STUDYING' && !String(form.reason || '').trim()) {
			message.warning('请填写状态变更原因')
			return
		}
		studentApi.studentStatusChange(form).then(() => {
			statusOpen.value = false
			refreshList()
			bumpDetail()
		})
	}
	// 标签绑定与字典
	const tagList = ref([])
	const tagOptions = computed(() => tagList.value.map((item) => ({ value: item.id, label: item.name })))
	const tagBindOpen = ref(false)
	const tagBindForm = ref({ studentId: '', tagIdList: [] })
	const openTagBind = (record) => {
		tagBindForm.value = { studentId: record.id, tagIdList: (record.tagList || []).map((tag) => tag.id) }
		tagBindOpen.value = true
	}
	const onSubmitTags = () => {
		studentApi.tagBind(tagBindForm.value).then(() => {
			tagBindOpen.value = false
			refreshList()
			bumpDetail()
		})
	}
	const loadTags = () => {
		return studentApi.tagList().then((list) => {
			tagList.value = (list || []).map((item) => ({ ...item, editing: false, editName: item.name }))
		})
	}
	const tagManageOpen = ref(false)
	watch(tagManageOpen, (value) => {
		if (value) loadTags()
	})
	const openTagManage = () => {
		tagManageOpen.value = true
	}
	const newTag = ref({ name: '', color: 'blue', sortCode: 0 })
	const tagColorOptions = [
		{ value: 'blue', label: '蓝色' },
		{ value: 'green', label: '绿色' },
		{ value: 'orange', label: '橙色' },
		{ value: 'red', label: '红色' },
		{ value: 'purple', label: '紫色' },
		{ value: 'cyan', label: '青色' }
	]
	const tagColumns = [
		{ title: '标签', dataIndex: 'name' },
		{ title: '排序', dataIndex: 'sortCode', width: 80 },
		{ title: '操作', dataIndex: 'action', width: 150, align: 'center' }
	]
	const onAddTag = () => {
		const name = String(newTag.value.name || '').trim()
		if (!name) {
			message.warning('请输入标签名称')
			return
		}
		studentApi.tagAdd({ ...newTag.value, name }).then(() => {
			newTag.value = { name: '', color: 'blue', sortCode: 0 }
			loadTags()
		})
	}
	const onEditTag = (record) => {
		const name = String(record.editName || '').trim()
		if (!name) {
			message.warning('标签名称不能为空')
			return
		}
		studentApi.tagEdit({ id: record.id, name, color: record.color, sortCode: record.sortCode }).then(() => {
			record.editing = false
			loadTags()
		})
	}
	const onDeleteTag = (record) => {
		studentApi.tagDelete({ id: record.id }).then(() => {
			loadTags()
			refreshList()
		})
	}
	loadTags()
	// 详情抽屉
	const detailOpen = ref(false)
	const detailStudentId = ref('')
	const detailStudentName = ref('')
	const campusOptions = ref([])
	const campusNameMap = computed(() => Object.fromEntries(campusOptions.value.map((item) => [item.value, item.label])))
	courseApi.campusOptions().then((records) => {
		campusOptions.value = (records || []).map((item) => ({ value: item.value ?? item.id, label: item.label ?? item.name }))
		if (open.value && !formData.value.campusId && campusOptions.value.length === 1) {
			formData.value.campusId = campusOptions.value[0].value
		}
	})
	const openDetail = (record) => {
		detailStudentId.value = record.id
		detailStudentName.value = record.name
		detailOpen.value = true
	}
	// 支持从其它页面带 ?id= 直接打开详情抽屉
	if (route.query.id) {
		detailStudentId.value = String(route.query.id)
		detailOpen.value = true
	}
</script>

<style scoped lang="less">
	.text-danger {
		color: #cf1322;
		font-weight: 600;
	}
	.text-warning {
		color: var(--accent-text);
		font-weight: 600;
	}
	.text-muted {
		color: var(--text-color-secondary);
	}
	.status-reason {
		margin-left: 6px;
		font-size: 12px;
		color: var(--text-color-secondary);
		display: inline-block;
		max-width: 130px;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		vertical-align: middle;
	}
	.import-tip {
		margin-bottom: 12px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.tag-manage-link {
		font-size: 12px;
	}
	.student-card-tags {
		display: flex;
		align-items: center;
		gap: 4px;
		flex-wrap: wrap;
		margin-top: 6px;
	}
	.student-card-reason {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.student-mobile {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	.student-card {
		padding: 12px;
		background: #fff;
		border: 1px solid var(--border-color-split);
		border-radius: var(--radius-card);
	}
	.student-card-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 8px;
	}
	.student-card-name {
		display: flex;
		align-items: center;
		gap: 6px;
		min-width: 0;
		font-size: 15px;
		font-weight: 600;
	}
	.student-card-remain {
		font-size: 22px;
		font-weight: 700;
		line-height: 1;
		white-space: nowrap;
	}
	.student-card-remain span {
		margin-left: 2px;
		font-size: 11px;
		font-weight: 400;
		color: var(--text-color-secondary);
	}
	.student-card-line {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		margin-top: 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.student-card-campus {
		color: var(--text-color-tertiary);
	}
	.student-card-line a {
		font-weight: 500;
	}
	.student-card-grid {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 8px;
		margin-top: 10px;
		padding: 10px 0;
		border-top: 1px dashed var(--border-color-split);
		border-bottom: 1px dashed var(--border-color-split);
	}
	.student-card-grid > div {
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
	}
	.student-card-grid span {
		font-size: 11px;
		color: var(--text-color-secondary);
	}
	.student-card-grid b {
		font-size: 13px;
		font-weight: 600;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
	.student-card-actions {
		display: flex;
		align-items: center;
		gap: 8px;
		margin-top: 10px;
	}
	.student-card-actions :deep(.ant-btn) {
		flex: 1;
		height: 40px;
	}
	.student-card-actions :deep(.table-more) {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		height: 40px;
		padding: 0 12px;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
	}
	.student-mobile-pager {
		display: flex;
		justify-content: center;
		padding: 12px 0 4px;
	}
	@media (max-width: 576px) {
		.student-page :deep(.ent-toolbar .ant-input),
		.student-page :deep(.ent-toolbar .ant-select-selection-search-input),
		.student-page :deep(.ent-toolbar .ant-select-selection-item) {
			font-size: 16px;
		}
		.student-page :deep(.ent-toolbar .ant-btn),
		.student-page :deep(.page-header .ant-btn) {
			height: 44px;
		}
		.student-card-actions :deep(.ant-btn),
		.student-card-actions :deep(.table-more) {
			height: 44px;
		}
	}
</style>
