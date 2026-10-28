<template>
	<xn-panel>
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="机构名称" name="searchKey">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入机构名称/编码/联系人" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item label="状态" name="status">
						<a-select
							v-model:value="searchFormState.status"
							placeholder="请选择状态"
							:options="statusOptions"
							allow-clear
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
					<a-form-item>
						<a-space>
							<a-button type="primary" @click="tableRef.refresh(true)">
								<template #icon><SearchOutlined /></template>
								查询
							</a-button>
							<a-button @click="reset">
								<template #icon><redo-outlined /></template>
								重置
							</a-button>
						</a-space>
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:scroll="{ x: 'max-content' }"
		>
			<template #operator>
				<a-space>
					<a-button type="primary" @click="onOpenAdd">
						<template #icon><plus-outlined /></template>
						开通租户
					</a-button>
					<a-button @click="onCheckExpired">到期扫描</a-button>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'status'">
					<a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space :size="4">
						<a @click="onOpenEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a @click="onOpenRenew(record)">续费</a>
						<a-divider type="vertical" />
						<a v-if="record.status !== 'STOP'" @click="onOpenStop(record)">停用</a>
						<a-popconfirm v-else title="确定启用该租户吗？" @confirm="onEnable(record)">
							<a>启用</a>
						</a-popconfirm>
						<a-divider type="vertical" />
						<a-dropdown>
							<a @click.prevent>更多 <down-outlined /></a>
							<template #overlay>
								<a-menu>
									<a-menu-item @click="onResetPassword(record)">重置管理员密码</a-menu-item>
									<a-menu-item @click="onOpenLoginRecords(record)">登录记录</a-menu-item>
									<a-menu-item @click="onOpenRenewRecords(record)">续费记录</a-menu-item>
									<a-menu-divider />
									<a-menu-item danger @click="onDelete(record)">删除租户</a-menu-item>
								</a-menu>
							</template>
						</a-dropdown>
					</a-space>
				</template>
			</template>
		</s-table>
	</xn-panel>

	<a-modal
		v-model:open="open"
		:title="formData.id ? '编辑租户' : '开通租户'"
		:width="640"
		:destroy-on-close="true"
		@ok="onSubmit"
	>
		<a-form ref="formRef" :model="formData" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="机构编码" name="code" :rules="[{ required: true, message: '请输入机构编码' }]">
						<a-input v-model:value="formData.code" placeholder="登录时使用，如 demo" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="机构名称" name="name" :rules="[{ required: true, message: '请输入机构名称' }]">
						<a-input v-model:value="formData.name" placeholder="请输入机构名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="联系人" name="contactName">
						<a-input v-model:value="formData.contactName" placeholder="请输入联系人" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="联系电话" name="contactPhone">
						<a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="套餐" name="packageId">
						<a-select
							v-model:value="formData.packageId"
							placeholder="请选择套餐"
							:options="packageOptions"
							allow-clear
						/>
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="到期时间" name="expireTime">
						<a-date-picker v-model:value="formData.expireTime" value-format="YYYY-MM-DD" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="12" v-if="formData.id">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="formData.status" placeholder="请选择状态" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<template v-if="!formData.id">
					<a-col :span="24">
						<a-alert
							type="info"
							show-icon
							message="开通后系统会自动创建机构管理员、默认校区、管理员角色与业务菜单授权"
							style="margin-bottom: 16px"
						/>
					</a-col>
					<a-col :span="12">
						<a-form-item label="管理员账号" name="adminAccount">
							<a-input v-model:value="formData.adminAccount" placeholder="默认 admin" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="管理员姓名" name="adminName">
							<a-input v-model:value="formData.adminName" placeholder="默认 机构管理员" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="管理员初始密码" name="adminPassword">
							<a-input-password v-model:value="formData.adminPassword" placeholder="留空使用系统默认密码" />
						</a-form-item>
					</a-col>
					<a-col :span="12">
						<a-form-item label="默认校区名称" name="campusName">
							<a-input v-model:value="formData.campusName" placeholder="默认 默认校区" allow-clear />
						</a-form-item>
					</a-col>
				</template>
				<a-col :span="24">
					<a-form-item label="备注" name="remark">
						<a-input v-model:value="formData.remark" placeholder="请输入备注" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>

	<!-- 续费 -->
	<a-modal
		v-model:open="renewOpen"
		title="租户续费"
		:width="520"
		:confirm-loading="renewSubmitting"
		:destroy-on-close="true"
		@ok="onSubmitRenew"
	>
		<a-alert
			type="info"
			show-icon
			style="margin-bottom: 16px"
			:message="`当前到期：${renewData.expireTime || '未设置'}；续费后预计至 ${renewPreview}`"
		/>
		<a-form ref="renewFormRef" :model="renewData" layout="vertical">
			<a-form-item label="续费时长" name="days" :rules="[{ required: true, message: '请填写续费天数' }]">
				<a-input-number
					v-model:value="renewData.days"
					:min="1"
					:max="3650"
					:precision="0"
					style="width: 100%"
					addon-after="天"
				/>
				<div class="renew-presets">
					<a v-for="item in renewPresets" :key="item.days" @click="renewData.days = item.days">{{ item.label }}</a>
				</div>
			</a-form-item>
			<a-form-item label="续费套餐" name="packageId">
				<a-select
					v-model:value="renewData.packageId"
					:options="packageOptions"
					placeholder="不选则沿用当前套餐"
					allow-clear
				/>
			</a-form-item>
			<a-form-item label="续费金额（元）" name="amount">
				<a-input-number v-model:value="renewData.amount" :min="0" :precision="2" style="width: 100%" />
			</a-form-item>
			<a-form-item label="备注" name="remark">
				<a-input v-model:value="renewData.remark" placeholder="选填，如合同号、收款方式" allow-clear />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 停用 -->
	<a-modal
		v-model:open="stopOpen"
		title="停用租户"
		:width="480"
		:confirm-loading="stopSubmitting"
		:destroy-on-close="true"
		@ok="onSubmitStop"
	>
		<a-alert
			type="warning"
			show-icon
			style="margin-bottom: 16px"
			message="停用后该机构所有账号立即下线，且无法再登录。"
		/>
		<a-form layout="vertical">
			<a-form-item label="停用原因" required>
				<a-textarea v-model:value="stopReason" :rows="3" placeholder="如：欠费、合同到期、客户申请停用" />
			</a-form-item>
		</a-form>
	</a-modal>

	<!-- 重置密码结果 -->
	<a-modal v-model:open="resetOpen" title="管理员密码已重置" :width="460" :footer="null">
		<a-alert
			type="warning"
			show-icon
			style="margin-bottom: 16px"
			message="此密码只显示一次，请立即复制并通知机构管理员；原密码和已登录会话均已失效。"
		/>
		<div class="reset-row">
			<span>机构编码</span>
			<b>{{ resetResult.tenantCode }}</b>
		</div>
		<div class="reset-row">
			<span>管理员账号</span>
			<b>{{ resetResult.account }}</b>
		</div>
		<div class="reset-row">
			<span>新密码</span>
			<b class="reset-password">{{ resetResult.password }}</b>
			<a-button size="small" type="link" @click="copyText(resetResult.password)">复制</a-button>
		</div>
	</a-modal>

	<!-- 登录记录 -->
	<a-drawer v-model:open="loginOpen" title="最近登录记录" :width="520">
		<a-table
			size="small"
			:columns="loginColumns"
			:data-source="loginList"
			:loading="loginLoading"
			:pagination="false"
			:row-key="(row) => row.account + '-' + row.loginTime"
		/>
	</a-drawer>

	<!-- 续费记录 -->
	<a-drawer v-model:open="renewRecordsOpen" title="续费记录" :width="720">
		<a-table
			size="small"
			:columns="renewColumns"
			:data-source="renewList"
			:loading="renewLoading"
			:pagination="false"
			:row-key="(row) => row.createTime + '-' + row.newExpireTime"
		/>
	</a-drawer>
</template>

<script setup name="platformTenant">
	import { message, Modal } from 'ant-design-vue'
	import tenantApi from '@/api/platform/tenantApi'
	const searchFormState = ref({})
	const tableRef = ref()
	const formRef = ref()
	const formData = ref({})
	const open = ref(false)

	// 续费
	const renewOpen = ref(false)
	const renewFormRef = ref()
	const renewSubmitting = ref(false)
	const renewData = ref({})
	const renewPresets = [
		{ label: '1 个月', days: 30 },
		{ label: '3 个月', days: 90 },
		{ label: '6 个月', days: 180 },
		{ label: '1 年', days: 365 }
	]
	const parseDate = (value) => {
		if (!value) return null
		const date = new Date(String(value).replace(/-/g, '/'))
		return Number.isNaN(date.getTime()) ? null : date
	}
	const formatDate = (date) => {
		if (!date) return '-'
		const pad = (n) => String(n).padStart(2, '0')
		return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
	}
	const renewPreview = computed(() => {
		const days = Number(renewData.value.days) || 0
		if (days <= 0) return '-'
		const now = new Date()
		const current = parseDate(renewData.value.expireTime)
		const base = current && current > now ? current : now
		base.setDate(base.getDate() + days)
		return formatDate(base)
	})

	// 停用
	const stopOpen = ref(false)
	const stopSubmitting = ref(false)
	const stopReason = ref('')
	const selectedTenant = ref({})

	// 重置密码
	const resetOpen = ref(false)
	const resetResult = ref({})
	const copyText = async (text) => {
		try {
			await navigator.clipboard.writeText(text)
			message.success('已复制')
		} catch (e) {
			message.warning('复制失败，请手动选择密码')
		}
	}

	// 记录抽屉
	const loginOpen = ref(false)
	const loginLoading = ref(false)
	const loginList = ref([])
	const loginColumns = [
		{ title: '账号', dataIndex: 'account' },
		{ title: '登录时间', dataIndex: 'loginTime' }
	]
	const renewRecordsOpen = ref(false)
	const renewLoading = ref(false)
	const renewList = ref([])
	const renewColumns = [
		{ title: '续费天数', dataIndex: 'days' },
		{ title: '金额', dataIndex: 'amount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '原到期', dataIndex: 'oldExpireTime' },
		{ title: '新到期', dataIndex: 'newExpireTime' },
		{ title: '操作人', dataIndex: 'operatorName' },
		{ title: '备注', dataIndex: 'remark' },
		{ title: '操作时间', dataIndex: 'createTime' }
	]
	const statusOptions = [
		{ value: 'TRIAL', label: '试用' },
		{ value: 'NORMAL', label: '正常' },
		{ value: 'STOP', label: '停用' }
	]
	const packageOptions = ref([])
	const loadPackages = () => {
		tenantApi.packagePage({ current: 1, size: 100 }).then((data) => {
			packageOptions.value = (data.records || []).map((p) => ({
				value: p.id,
				label: `${p.name}（${p.validityDays || 365}天）`
			}))
		})
	}
	const onCheckExpired = () => {
		tenantApi.tenantCheckExpired().then((count) => {
			tableRef.value.refresh()
			if (count > 0) {
				message.success(`已停用 ${count} 个到期租户`)
			} else {
				message.info('没有到期租户')
			}
		})
	}
	const columns = [
		{ title: '机构编码', dataIndex: 'code' },
		{ title: '机构名称', dataIndex: 'name' },
		{ title: '联系人', dataIndex: 'contactName' },
		{ title: '联系电话', dataIndex: 'contactPhone' },
		{ title: '套餐', dataIndex: 'packageId', customRender: ({ record }) => packageName(record.packageId) },
		{
			title: '套餐限额',
			dataIndex: 'studentLimit',
			customRender: ({ record }) =>
				record.studentLimit ? `学员 ${record.studentLimit} / 校区 ${record.campusLimit ?? '-'}` : '不限'
		},
		{ title: '到期时间', dataIndex: 'expireTime' },
		{
			title: '剩余天数',
			dataIndex: 'expireDays',
			customRender: ({ record }) => {
				if (record.expireDays === null || record.expireDays === undefined) return '-'
				return record.expireDays < 0 ? `已过期 ${Math.abs(record.expireDays)} 天` : `${record.expireDays} 天`
			}
		},
		{ title: '学员数', dataIndex: 'studentCount' },
		{ title: '订单数', dataIndex: 'orderCount' },
		{ title: '收款金额', dataIndex: 'paidAmount', customRender: ({ text }) => `¥${Number(text || 0).toFixed(2)}` },
		{ title: '最近登录', dataIndex: 'lastLoginTime' },
		{ title: '状态', dataIndex: 'status' },
		{ title: '操作', dataIndex: 'action', align: 'center', fixed: 'right' }
	]
	const packageName = (id) => {
		const item = packageOptions.value.find((p) => p.value === id)
		return item ? item.label : id
	}
	const loadData = (parameter) => {
		return tenantApi.tenantPage(Object.assign(parameter, searchFormState.value)).then((data) => data)
	}
	const reset = () => {
		searchFormState.value = {}
		tableRef.value.refresh(true)
	}
	const statusText = (s) => ({ TRIAL: '试用', NORMAL: '正常', STOP: '停用' })[s] || s
	const statusColor = (s) => (s === 'NORMAL' ? 'green' : s === 'TRIAL' ? 'orange' : 'red')
	const onOpenAdd = () => {
		formData.value = { status: 'TRIAL', adminAccount: 'admin', adminName: '机构管理员', campusName: '默认校区' }
		open.value = true
	}
	const onOpenEdit = (record) => {
		formData.value = { ...record }
		open.value = true
	}
	const onSubmit = () => {
		formRef.value.validate().then(() => {
			const edit = !!formData.value.id
			const api = edit ? tenantApi.tenantEdit : tenantApi.tenantAdd
			api(formData.value).then(() => {
				open.value = false
				tableRef.value.refresh()
			})
		})
	}
	const onDelete = (record) => {
		Modal.confirm({
			title: '删除租户',
			content: `确定删除「${record.name}」吗？删除后该机构的业务数据仍保留在库中，但无法再登录。`,
			okText: '删除',
			okType: 'danger',
			onOk: () => {
				return tenantApi.tenantDelete([{ id: record.id }]).then(() => tableRef.value.refresh())
			}
		})
	}

	const onOpenRenew = (record) => {
		selectedTenant.value = record
		renewData.value = {
			id: record.id,
			days: 90,
			packageId: record.packageId,
			amount: undefined,
			remark: '',
			expireTime: record.expireTime
		}
		renewOpen.value = true
	}
	const onSubmitRenew = () => {
		renewFormRef.value.validate().then(() => {
			renewSubmitting.value = true
			const { expireTime, ...payload } = renewData.value
			tenantApi
				.tenantRenew(payload)
				.then((data) => {
					message.success(`已续费至 ${formatDate(parseDate(data.expireTime))}`)
					renewOpen.value = false
					tableRef.value.refresh()
				})
				.finally(() => {
					renewSubmitting.value = false
				})
		})
	}

	const onOpenStop = (record) => {
		selectedTenant.value = record
		stopReason.value = ''
		stopOpen.value = true
	}
	const onSubmitStop = () => {
		if (!stopReason.value.trim()) {
			message.warning('请填写停用原因')
			return
		}
		stopSubmitting.value = true
		tenantApi
			.tenantStatus({ id: selectedTenant.value.id, status: 'STOP', reason: stopReason.value.trim() })
			.then(() => {
				message.success('已停用，该机构账号已全部下线')
				stopOpen.value = false
				tableRef.value.refresh()
			})
			.finally(() => {
				stopSubmitting.value = false
			})
	}
	const onEnable = (record) => {
		tenantApi.tenantStatus({ id: record.id, status: 'NORMAL' }).then(() => {
			message.success('已启用')
			tableRef.value.refresh()
		})
	}

	const onResetPassword = (record) => {
		Modal.confirm({
			title: '重置管理员密码',
			content: `确定重置「${record.name}」的管理员密码吗？原密码和该管理员已登录的会话会立即失效。`,
			okText: '重置',
			okType: 'danger',
			onOk: () => {
				return tenantApi.tenantResetPassword({ id: record.id }).then((data) => {
					resetResult.value = data
					resetOpen.value = true
				})
			}
		})
	}

	const onOpenLoginRecords = (record) => {
		loginOpen.value = true
		loginLoading.value = true
		loginList.value = []
		tenantApi
			.tenantLoginRecords({ id: record.id })
			.then((data) => {
				loginList.value = data || []
			})
			.finally(() => {
				loginLoading.value = false
			})
	}
	const onOpenRenewRecords = (record) => {
		renewRecordsOpen.value = true
		renewLoading.value = true
		renewList.value = []
		tenantApi
			.tenantRenewRecords({ id: record.id })
			.then((data) => {
				renewList.value = data || []
			})
			.finally(() => {
				renewLoading.value = false
			})
	}

	loadPackages()
</script>

<style scoped lang="less">
	.renew-presets {
		display: flex;
		gap: 14px;
		margin-top: 8px;
		font-size: 12px;
	}
	.reset-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: 10px 0;
		border-bottom: 1px dashed var(--border-color-split);
		span {
			color: var(--text-color-secondary);
			font-size: 13px;
		}
		b {
			font-weight: 600;
		}
	}
	.reset-password {
		font-family: Consolas, monospace;
		font-size: 16px;
		letter-spacing: 1px;
		color: var(--accent-text);
	}
</style>
