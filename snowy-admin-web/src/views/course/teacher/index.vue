<template>
	<div>
		<page-header title="老师管理" desc="老师账号、校区与带班情况">
			<template #extra>
				<a-button v-if="canManage" type="primary" @click="onOpenAdd">
					<template #icon><plus-outlined /></template>
					新增老师
				</a-button>
			</template>
		</page-header>

		<div class="ent-toolbar">
			<a-form layout="inline" :model="searchForm">
				<a-form-item label="姓名/账号">
					<a-input v-model:value="searchForm.searchKey" placeholder="姓名或账号" allow-clear style="width: 200px" @pressEnter="reload" />
				</a-form-item>
				<a-form-item label="状态">
					<a-select v-model:value="searchForm.userStatus" placeholder="全部状态" :options="statusOptions" allow-clear style="width: 130px" />
				</a-form-item>
				<a-form-item>
					<a-space>
						<a-button type="primary" @click="reload">查询</a-button>
						<a-button @click="reset">重置</a-button>
					</a-space>
				</a-form-item>
			</a-form>
		</div>

		<div class="ent-body">
			<a-table
				:columns="columns"
				:data-source="list"
				:row-key="(r) => r.id"
				:loading="loading"
				bordered
				size="middle"
				:scroll="{ x: 'max-content' }"
				:pagination="{
					current: page.current,
					pageSize: page.size,
					total: page.total,
					showSizeChanger: false,
					onChange: onPageChange
				}"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'name'">
						<div class="teacher-name">{{ record.name }}</div>
						<div class="teacher-account">{{ record.account }}</div>
					</template>
					<template v-if="column.dataIndex === 'phone'">{{ record.phone || '-' }}</template>
					<template v-if="column.dataIndex === 'campus'">{{ campusName(record) }}</template>
					<template v-if="column.dataIndex === 'classCount'">{{ record.classCount || 0 }} 个班</template>
					<template v-if="column.dataIndex === 'studentCount'">{{ record.studentCount || 0 }} 人</template>
					<template v-if="column.dataIndex === 'status'">
						<a-tag :color="record.userStatus === 'ENABLE' ? 'green' : 'default'">
							{{ record.userStatus === 'ENABLE' ? '启用' : '停用' }}
						</a-tag>
					</template>
					<template v-if="column.dataIndex === 'action'">
						<a-space>
							<a v-if="canManage" @click="onOpenEdit(record)">编辑</a>
							<a-divider v-if="canManage" type="vertical" />
							<a v-if="canManage" @click="onToggle(record)">
								{{ record.userStatus === 'ENABLE' ? '停用' : '启用' }}
							</a>
							<a-divider v-if="canManage" type="vertical" />
							<a v-if="canManage" @click="onResetPassword(record)">重置密码</a>
						</a-space>
					</template>
				</template>
			</a-table>
		</div>

		<a-modal v-model:open="open" :title="formData.id ? '编辑老师' : '新增老师'" :width="560" :destroy-on-close="true" @ok="onSubmit">
			<a-form ref="formRef" :model="formData" layout="vertical">
				<a-row :gutter="16">
					<a-col :span="12">
						<a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入老师姓名' }]">
							<a-input v-model:value="formData.name" placeholder="如：王老师" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="账号" name="account" :rules="[{ required: true, message: '请输入登录账号' }]">
							<a-input v-model:value="formData.account" placeholder="用于登录 APP/后台" :disabled="!!formData.id" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="手机号" name="phone">
							<a-input v-model:value="formData.phone" placeholder="选填" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="所属校区" name="orgId" :rules="[{ required: true, message: '请选择校区' }]">
							<a-select v-model:value="formData.orgId" :options="campusOptions" placeholder="请选择校区" show-search option-filter-prop="label" />
						</a-form-item>
					</a-col>
					<a-col v-if="!formData.id" :span="24">
						<a-form-item label="初始密码" name="password">
							<a-input-password v-model:value="formData.password" placeholder="留空则使用系统默认密码" />
						</a-form-item>
					</a-col>
				</a-row>
				<a-alert type="info" show-icon message="老师账号会自动授予「授课老师」角色；班级、排课里即可选择该老师。" />
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="courseTeacher">
	import { message } from 'ant-design-vue'
	import userApi from '@/api/sys/userApi'
	import roleApi from '@/api/sys/roleApi'
	import courseApi from '@/api/course/courseApi'
	import { globalStore } from '@/store'

	const store = globalStore()
	const canManage = computed(
		() =>
			hasPerm('/sys/user/edit') ||
			(store.userInfo?.roleCodeList || []).includes('superAdmin') ||
			store.userInfo?.account === 'superAdmin'
	)

	const loading = ref(false)
	const list = ref([])
	const page = ref({ current: 1, size: 10, total: 0 })
	const searchForm = ref({ searchKey: undefined, userStatus: undefined })
	const campusOptions = ref([])
	const classCountMap = ref({})
	const teacherRoleId = ref('')
	const open = ref(false)
	const formRef = ref()
	const formData = ref({})
	const statusOptions = [
		{ value: 'ENABLE', label: '启用' },
		{ value: 'DISABLED', label: '停用' }
	]
	const columns = [
		{ title: '老师', dataIndex: 'name' },
		{ title: '手机号', dataIndex: 'phone', width: 130 },
		{ title: '校区', dataIndex: 'campus', width: 150 },
		{ title: '带班', dataIndex: 'classCount', width: 90 },
		{ title: '学员', dataIndex: 'studentCount', width: 90 },
		{ title: '状态', dataIndex: 'status', width: 90 },
		{ title: '操作', dataIndex: 'action', width: 210 }
	]

	const campusName = (record) => {
		if (record.orgName) return record.orgName
		return campusOptions.value.find((item) => item.value === record.orgId)?.label || '-'
	}

	const loadOptions = () => {
		courseApi.campusOptions().then((records) => {
			campusOptions.value = (records || []).map((item) => ({
				value: item.value ?? item.id,
				label: item.label ?? item.name
			}))
		})
	}

	const loadTeacherRole = async () => {
		if (teacherRoleId.value) return teacherRoleId.value
		const data = await roleApi.rolePage({ current: 1, size: 200 })
		const role = (data?.records || []).find((item) => item.code === 'teacher')
		if (!role) {
			message.error('未找到「授课老师」角色，请先在角色管理中创建')
			return ''
		}
		teacherRoleId.value = role.id
		return teacherRoleId.value
	}

	const load = async () => {
		loading.value = true
		try {
			const [userData, classData] = await Promise.all([
				userApi.userPage({
					current: page.value.current,
					size: page.value.size,
					searchKey: searchForm.value.searchKey,
					userStatus: searchForm.value.userStatus,
					roleCode: 'teacher'
				}),
				courseApi.classPage({ current: 1, size: 500 }).catch(() => ({}))
			])
			const countMap = {}
			for (const item of classData?.records || []) {
				if (!item.teacherId) continue
				const stat = countMap[item.teacherId] || { classCount: 0, studentCount: 0 }
				stat.classCount += 1
				stat.studentCount += Number(item.studentCount || 0)
				countMap[item.teacherId] = stat
			}
			classCountMap.value = countMap
			list.value = (userData?.records || []).map((item) => ({
				...item,
				classCount: countMap[item.id]?.classCount || 0,
				studentCount: countMap[item.id]?.studentCount || 0
			}))
			page.value.total = userData?.total || 0
		} finally {
			loading.value = false
		}
	}

	const reload = () => {
		page.value.current = 1
		load()
	}
	const reset = () => {
		searchForm.value = { searchKey: undefined, userStatus: undefined }
		reload()
	}
	const onPageChange = (current, size) => {
		page.value.current = current
		page.value.size = size
		load()
	}

	const onOpenAdd = () => {
		formData.value = {
			name: '',
			account: '',
			phone: '',
			orgId: campusOptions.value.length === 1 ? campusOptions.value[0].value : undefined,
			password: ''
		}
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = {
			id: record.id,
			name: record.name,
			account: record.account,
			phone: record.phone,
			orgId: record.orgId
		}
		open.value = true
	}

	const onSubmit = () => {
		formRef.value.validate().then(async () => {
			const edit = !!formData.value.id
			try {
				if (edit) {
					await userApi.submitForm(
						{
							id: formData.value.id,
							account: formData.value.account,
							name: formData.value.name,
							phone: formData.value.phone,
							orgId: formData.value.orgId
						},
						true
					)
					message.success('老师信息已更新')
				} else {
					await createTeacher()
					message.success('老师已新增，可到班级/排课里选择')
				}
				open.value = false
				load()
			} catch (e) {
				// 错误提示由请求拦截器统一处理
			}
		})
	}

	const createTeacher = async () => {
		const roleId = await loadTeacherRole()
		if (!roleId) throw new Error('缺少授课老师角色')
		let createdUser = null
		try {
			await userApi.submitForm({
				name: formData.value.name,
				account: formData.value.account,
				phone: formData.value.phone || undefined,
				orgId: formData.value.orgId,
				positionId: null,
				password: formData.value.password || undefined
			})
			// 授权前不能用 roleCode 过滤，否则查不到刚建的账号
			const userData = await userApi.userPage({
				current: 1,
				size: 20,
				searchKey: formData.value.account
			})
			createdUser = (userData?.records || []).find((item) => item.account === formData.value.account)
			if (!createdUser) {
				throw new Error('账号已创建但未查询到，请到用户管理手动授权')
			}
			await userApi.grantRole({ id: createdUser.id, roleIdList: [roleId] })
		} catch (e) {
			// 授权失败时删除刚建的账号，避免留下无角色账号
			if (createdUser?.id) {
				await userApi.userDelete([{ id: createdUser.id }]).catch(() => {})
			}
			throw e
		}
	}

	const onToggle = (record) => {
		const enable = record.userStatus !== 'ENABLE'
		const api = enable ? userApi.userEnableUser : userApi.userDisableUser
		api({ id: record.id }).then(() => {
			message.success(enable ? '已启用该老师' : '已停用该老师')
			load()
		})
	}

	const onResetPassword = (record) => {
		userApi.userResetPassword({ id: record.id }).then(() => {
			message.success('已重置为系统默认密码')
		})
	}

	loadOptions()
	loadTeacherRole()
	load()
</script>

<style lang="less" scoped>
	.teacher-name {
		font-size: 14px;
		font-weight: 500;
		color: var(--text-color);
	}
	.teacher-account {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
</style>
