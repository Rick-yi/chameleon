<template>
	<div class="m-teacher-page">
		<div class="m-teacher-search">
			<a-input-search
				v-model:value="searchForm.searchKey"
				placeholder="姓名或账号"
				allow-clear
				enter-button="查询"
				@search="reload"
			/>
			<a-select
				v-model:value="searchForm.userStatus"
				placeholder="全部状态"
				:options="statusOptions"
				allow-clear
				style="width: 100%"
				@change="reload"
			/>
		</div>

		<div v-if="canManage" class="m-teacher-add">
			<a-button type="primary" block @click="onOpenAdd">新增老师</a-button>
		</div>

		<a-spin :spinning="loading">
			<div v-if="!list.length" class="m-empty">
				<div>没有符合条件的老师</div>
				<a-button v-if="canManage" type="primary" style="margin-top: 10px" @click="onOpenAdd">新增老师</a-button>
			</div>
			<div v-for="item in list" :key="item.id" class="m-card m-teacher-card">
				<div class="m-teacher-head">
					<div>
						<div class="m-teacher-name">{{ item.name }}</div>
						<div class="m-teacher-account">{{ item.account }}</div>
					</div>
					<a-tag :color="item.userStatus === 'ENABLE' ? 'green' : 'default'">
						{{ item.userStatus === 'ENABLE' ? '启用' : '停用' }}
					</a-tag>
				</div>
				<div class="m-teacher-line">
					<span><phone-outlined /> {{ item.phone || '未填手机' }}</span>
					<span>{{ campusName(item) }}</span>
				</div>
				<div class="m-teacher-grid">
					<div><span>带班</span><b>{{ item.classCount || 0 }} 个</b></div>
					<div><span>学员</span><b>{{ item.studentCount || 0 }} 人</b></div>
				</div>
				<div v-if="canManage" class="m-teacher-actions">
					<a-button size="small" @click="onOpenEdit(item)">编辑</a-button>
					<a-button size="small" @click="onToggle(item)">{{ item.userStatus === 'ENABLE' ? '停用' : '启用' }}</a-button>
					<a-button size="small" @click="onResetPassword(item)">重置密码</a-button>
				</div>
			</div>
			<div v-if="page.total > page.size" class="m-teacher-pager">
				<a-pagination simple :current="page.current" :page-size="page.size" :total="page.total" @change="onPageChange" />
			</div>
		</a-spin>

		<a-modal
			v-model:open="open"
			:title="formData.id ? '编辑老师' : '新增老师'"
			:width="'calc(100vw - 24px)'"
			:destroy-on-close="true"
			@ok="onSubmit"
		>
			<a-form ref="formRef" :model="formData" layout="vertical">
				<a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入老师姓名' }]">
					<a-input v-model:value="formData.name" placeholder="如：王老师" allow-clear />
				</a-form-item>
				<a-form-item label="账号" name="account" :rules="[{ required: true, message: '请输入登录账号' }]">
					<a-input v-model:value="formData.account" placeholder="用于登录 APP/后台" :disabled="!!formData.id" allow-clear />
				</a-form-item>
				<a-form-item label="手机号" name="phone">
					<a-input v-model:value="formData.phone" placeholder="选填" allow-clear />
				</a-form-item>
				<a-form-item label="所属校区" name="orgId" :rules="[{ required: true, message: '请选择校区' }]">
					<a-select v-model:value="formData.orgId" :options="campusOptions" placeholder="请选择校区" show-search option-filter-prop="label" />
				</a-form-item>
				<a-form-item v-if="!formData.id" label="初始密码" name="password">
					<a-input-password v-model:value="formData.password" placeholder="留空则使用系统默认密码" />
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="appTeacher">
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
	const teacherRoleId = ref('')
	const open = ref(false)
	const formRef = ref()
	const formData = ref({})
	const statusOptions = [
		{ value: 'ENABLE', label: '启用' },
		{ value: 'DISABLED', label: '停用' }
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
			message.error('未找到「授课老师」角色')
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
	const onPageChange = (current) => {
		page.value.current = current
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
			try {
				if (formData.value.id) {
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
					message.success('老师已新增')
				}
				open.value = false
				load()
			} catch (e) {
				// 请求拦截器已提示具体错误
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
			const userData = await userApi.userPage({
				current: 1,
				size: 20,
				searchKey: formData.value.account
			})
			createdUser = (userData?.records || []).find((item) => item.account === formData.value.account)
			if (!createdUser) {
				throw new Error('账号已创建但未查询到')
			}
			await userApi.grantRole({ id: createdUser.id, roleIdList: [roleId] })
		} catch (e) {
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
	.m-teacher-page {
		padding-bottom: 8px;
	}
	.m-teacher-search {
		display: flex;
		flex-direction: column;
		gap: 8px;
		padding: 12px;
		margin-bottom: 10px;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		:deep(.ant-input) {
			font-size: 16px;
		}
	}
	.m-teacher-add {
		margin-bottom: 10px;
		:deep(.ant-btn) {
			height: 42px;
			font-weight: 500;
		}
	}
	.m-empty {
		padding: 30px 12px;
		text-align: center;
		font-size: 13px;
		color: var(--text-color-secondary);
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
	}
	.m-card {
		padding: 12px 14px;
		margin-bottom: 10px;
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
	}
	.m-teacher-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 8px;
	}
	.m-teacher-name {
		font-size: 16px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-teacher-account {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.m-teacher-line {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 10px;
		margin-top: 8px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-teacher-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 8px;
		margin-top: 10px;
		padding: 10px 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
		div {
			display: flex;
			align-items: center;
			justify-content: space-between;
			gap: 6px;
		}
		span {
			font-size: 12px;
			color: var(--text-color-secondary);
		}
		b {
			font-size: 14px;
			color: var(--text-color);
		}
	}
	.m-teacher-actions {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
		margin-top: 10px;
		:deep(.ant-btn) {
			min-height: 38px;
			flex: 1;
		}
	}
	.m-teacher-pager {
		display: flex;
		justify-content: center;
		padding: 8px 0 4px;
	}
</style>
