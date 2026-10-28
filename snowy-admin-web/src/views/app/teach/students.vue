<template>
	<div>
		<!-- 搜索与新增：名册页直接录入，不用回首页 -->
		<div class="m-student-toolbar">
			<a-input-search v-model:value="keyword" placeholder="姓名 / 家长手机" allow-clear @search="load" />
			<a-button v-if="hasPerm('/student/add')" type="primary" @click="openCreate">新增学员</a-button>
		</div>

		<div v-if="loading" class="m-empty">加载中…</div>
		<div v-else-if="!list.length" class="m-empty">
			<div>没有匹配的学员</div>
			<a-button v-if="hasPerm('/student/add')" type="primary" style="margin-top: 10px" @click="openCreate">
				新增学员
			</a-button>
		</div>

		<div v-for="item in list" :key="item.id" class="m-card m-student">
			<div class="m-student-top">
				<a-avatar :size="40" style="background: var(--primary-1); color: var(--primary-6)">
					{{ (item.name || '?').slice(0, 1) }}
				</a-avatar>
				<div class="m-student-main">
					<div class="m-student-name">
						{{ item.name }}
						<a-tag :color="statusColor(item.status)" style="margin-left: 6px">{{ statusText(item.status) }}</a-tag>
					</div>
					<div class="m-student-sub">
						{{ genderText(item.gender) }} · 家长 {{ item.parentName || '-' }} {{ item.parentPhone || '' }}
					</div>
				</div>
				<div class="m-student-actions">
					<a-button v-if="hasPerm('/student/order/add')" size="small" @click.stop="openEnroll(item)">报名</a-button>
					<a-icon class="m-student-arrow" @click.stop="openDetail(item)"><right-outlined /></a-icon>
				</div>
			</div>
			<div class="m-student-extra">
				<div>
					<span>剩余</span>
					<b :class="lessonTone(item)">{{ lessonText(item) }}</b>
				</div>
				<div>
					<span>班级</span>
					<b>{{ item.classNames || '未分班' }}</b>
				</div>
				<div>
					<span>课包到期</span>
					<b :class="{ 'm-text-danger': isExpired(item.accountExpireTime) }">{{ shortDate(item.accountExpireTime) }}</b>
				</div>
			</div>
		</div>

		<enrollment-flow v-model:open="enrollOpen" :student="enrollStudent" @success="onEnrollSuccess" />

		<a-drawer
			v-model:open="detailOpen"
			:width="360"
			placement="right"
			title="学员详情"
			:body-style="{ padding: '12px' }"
		>
			<div v-if="currentStudent">
				<div class="m-detail-name">{{ currentStudent.name }}</div>
				<div class="m-detail-sub">
					{{ genderText(currentStudent.gender) }} · 家长 {{ currentStudent.parentName || '-' }}
					{{ currentStudent.parentPhone || '' }}
				</div>
				<div class="m-detail-grid">
					<div>
						<span>剩余课时</span><b>{{ remainText }}</b>
					</div>
					<div>
						<span>课包到期</span><b>{{ detailDate(currentStudent.accountExpireTime) }}</b>
					</div>
					<div>
						<span>所在班级</span><b>{{ currentStudent.classNames || '未分班' }}</b>
					</div>
					<div>
						<span>待收款</span><b>¥{{ Number(currentStudent.unpaidAmount || 0).toFixed(2) }}</b>
					</div>
				</div>
				<div class="m-detail-actions">
					<a-button
						v-if="hasPerm('/student/order/add')"
						type="primary"
						block
						class="m-detail-enroll"
						@click="enrollFromDetail"
					>
						报名 / 续费
					</a-button>
					<a-button v-if="hasPerm('/student/order/add')" block @click="openAdjustPackage">调课包</a-button>
				</div>
				<div class="m-section-title">课时账户</div>
				<div v-if="!accountList.length" class="m-empty" style="padding: 16px">暂无课时账户</div>
				<div v-for="acc in accountList" :key="acc.courseId" class="m-card" style="padding: 10px 12px">
					<div class="m-student-name">{{ acc.courseName || acc.courseId }}</div>
					<div class="m-student-sub">
						剩余 <b>{{ acc.remainLessons }}</b> 课时 / 共 {{ acc.totalLessons }} 课时
					</div>
				</div>
			</div>
		</a-drawer>

		<!-- 调课包：选预设课包或自定义课时/价格 -->
		<a-drawer v-model:open="pkgOpen" :width="360" placement="right" title="调课包" :body-style="{ padding: '12px' }">
			<div class="m-pkg-hint">调课包会生成一笔新订单，课时叠加到学员账户；历史订单不动，退费仍按各自订单核算。</div>
			<a-form :model="pkgForm" layout="vertical">
				<a-form-item label="课程" required>
					<a-select
						v-model:value="pkgForm.courseId"
						:options="courseOptions"
						placeholder="选择课程"
						show-search
						option-filter-prop="label"
						@change="onPkgCourseChange"
					/>
				</a-form-item>
				<a-form-item label="方式">
					<a-radio-group v-model:value="pkgForm.mode" button-style="solid" size="small">
						<a-radio-button value="PACKAGE">预设课包</a-radio-button>
						<a-radio-button value="CUSTOM">自定义</a-radio-button>
					</a-radio-group>
				</a-form-item>
				<a-form-item v-if="pkgForm.mode === 'PACKAGE'" label="课包" required>
					<a-select
						v-model:value="pkgForm.coursePackageId"
						:options="packageOptions"
						placeholder="选择课包"
						show-search
						option-filter-prop="label"
						@change="onPkgPackageChange"
					/>
				</a-form-item>
				<template v-else>
					<a-row :gutter="8">
						<a-col :span="12">
							<a-form-item label="课时" required>
								<a-input-number v-model:value="pkgForm.packageLessons" :min="0.5" :precision="2" style="width: 100%" />
							</a-form-item>
						</a-col>
						<a-col :span="12">
							<a-form-item label="赠课">
								<a-input-number
									v-model:value="pkgForm.packageGiftLessons"
									:min="0"
									:precision="2"
									style="width: 100%"
								/>
							</a-form-item>
						</a-col>
					</a-row>
					<a-row :gutter="8">
						<a-col :span="12">
							<a-form-item label="有效期(天)">
								<a-input-number v-model:value="pkgForm.packageValidityDays" :min="0" style="width: 100%" />
							</a-form-item>
						</a-col>
						<a-col :span="12">
							<a-form-item label="应收金额" required>
								<a-input-number v-model:value="pkgForm.amount" :min="0" :precision="2" style="width: 100%" />
							</a-form-item>
						</a-col>
					</a-row>
				</template>
				<a-form-item label="立即收款">
					<a-switch v-model:checked="pkgForm.payNow" />
				</a-form-item>
				<a-form-item v-if="pkgForm.payNow" label="收款渠道">
					<a-select
						v-model:value="pkgForm.payChannel"
						:options="[
							{ value: 'CASH', label: '现金' },
							{ value: 'WECHAT', label: '微信' },
							{ value: 'ALIPAY', label: '支付宝' },
							{ value: 'OFFLINE', label: '线下转账' }
						]"
					/>
				</a-form-item>
			</a-form>
			<div class="m-pkg-summary" v-if="pkgPreview">
				<span>本次应收</span><b>¥{{ pkgPreview }}</b>
			</div>
			<a-button type="primary" block :loading="pkgSubmitting" @click="submitAdjustPackage">确认调课包</a-button>
		</a-drawer>
	</div>
</template>

<script setup name="appTeachStudents">
	import { useRoute } from 'vue-router'
	import { message } from 'ant-design-vue'
	import studentApi from '@/api/student/studentApi'
	import courseApi from '@/api/course/courseApi'
	import EnrollmentFlow from '@/components/EnrollmentFlow/index.vue'

	const route = useRoute()
	const loading = ref(false)
	const keyword = ref('')
	const list = ref([])
	const detailOpen = ref(false)
	const currentStudent = ref(null)
	const accountList = ref([])
	const enrollOpen = ref(false)
	const enrollStudent = ref(null)

	const statusText = (status) => ({ STUDYING: '在读', STOP: '停课', REFUNDED: '已退费' })[status] || status
	const statusColor = (status) => (status === 'STUDYING' ? 'green' : status === 'STOP' ? 'orange' : 'red')
	const genderText = (gender) => ({ MALE: '男', FEMALE: '女' })[gender] || gender || '-'
	const shortDate = (value) => (value ? String(value).slice(0, 10) : '-')
	const detailDate = (value) => shortDate(value)
	const lessonText = (item) => {
		if (item.hasLessonAccount === false) return '未报名'
		return `${Number(item.remainLessons || 0)} 课时`
	}
	const lessonTone = (item) => {
		if (item.hasLessonAccount === false) return 'm-text-muted'
		const remain = Number(item.remainLessons || 0)
		if (remain <= 0) return 'm-text-danger'
		if (remain < 3) return 'm-text-warning'
		return ''
	}
	const isExpired = (value) => {
		if (!value) return false
		const date = new Date(String(value).replace(/-/g, '/'))
		return !Number.isNaN(date.getTime()) && date < new Date()
	}
	const remainText = computed(() => {
		if (!currentStudent.value) return '-'
		return lessonText(currentStudent.value)
	})

	const load = async () => {
		loading.value = true
		try {
			const res = await studentApi.studentPage({ current: 1, size: 50, searchKey: keyword.value }).catch(() => ({}))
			list.value = res?.records || []
		} finally {
			loading.value = false
		}
	}

	const loadAccounts = async (studentId) => {
		const res = await studentApi.lessonAccountPage({ current: 1, size: 20, studentId }).catch(() => ({}))
		accountList.value = res?.records || []
	}

	const openDetail = (item) => {
		currentStudent.value = item
		detailOpen.value = true
		loadAccounts(item.id)
	}

	/** 支持从报名成功页/其它页面带 ?id= 直接打开学员详情 */
	const openDetailById = async (id) => {
		const data = await studentApi.studentDetail({ id }).catch(() => null)
		if (!data) return
		openDetail(data)
	}

	const openCreate = () => {
		enrollStudent.value = null
		enrollOpen.value = true
	}

	const openEnroll = (item) => {
		enrollStudent.value = item
		enrollOpen.value = true
	}

	const enrollFromDetail = () => {
		detailOpen.value = false
		openEnroll(currentStudent.value)
	}

	// ---------- 调课包（预设课包 / 自定义课包） ----------
	const pkgOpen = ref(false)
	const pkgSubmitting = ref(false)
	const courseOptions = ref([])
	const packageRaw = ref([])
	const pkgForm = ref({
		courseId: undefined,
		mode: 'CUSTOM',
		coursePackageId: undefined,
		packageLessons: 10,
		packageGiftLessons: 0,
		packageValidityDays: 0,
		amount: 0,
		payNow: true,
		payChannel: 'CASH'
	})
	// 按所选课程过滤课包
	const packageOptions = computed(() =>
		packageRaw.value
			.filter((p) => !pkgForm.value.courseId || p.courseId === pkgForm.value.courseId)
			.map((p) => ({ value: p.id, label: `${p.name}（${p.lessons} 课时 / ¥${p.price}）` }))
	)
	// 应收预览：预设取课包价，自定义取手填金额
	const pkgPreview = computed(() => {
		if (pkgForm.value.mode === 'PACKAGE') {
			const item = packageRaw.value.find((p) => p.id === pkgForm.value.coursePackageId)
			return item ? Number(item.price || 0).toFixed(2) : '0.00'
		}
		return Number(pkgForm.value.amount || 0).toFixed(2)
	})

	const loadPkgOptions = async () => {
		if (courseOptions.value.length) return
		const [courseRes, packageRes] = await Promise.all([
			courseApi.coursePage({ current: 1, size: 200 }).catch(() => ({ records: [] })),
			courseApi.packagePage({ current: 1, size: 200 }).catch(() => ({ records: [] }))
		])
		courseOptions.value = (courseRes.records || []).map((c) => ({ value: c.id, label: c.name }))
		packageRaw.value = packageRes.records || []
	}

	const openAdjustPackage = async () => {
		detailOpen.value = false
		await loadPkgOptions()
		pkgForm.value = {
			courseId: undefined,
			mode: 'CUSTOM',
			coursePackageId: undefined,
			packageLessons: 10,
			packageGiftLessons: 0,
			packageValidityDays: 0,
			amount: 0,
			payNow: true,
			payChannel: 'CASH'
		}
		pkgOpen.value = true
	}

	const onPkgCourseChange = () => {
		// 换课程后清掉不再匹配的课包选择
		pkgForm.value.coursePackageId = undefined
	}

	const onPkgPackageChange = (id) => {
		const item = packageRaw.value.find((p) => p.id === id)
		if (item) pkgForm.value.amount = Number(item.price || 0)
	}

	const submitAdjustPackage = async () => {
		const form = pkgForm.value
		if (!form.courseId) {
			message.warning('请选择课程')
			return
		}
		if (form.mode === 'PACKAGE' && !form.coursePackageId) {
			message.warning('请选择课包')
			return
		}
		if (form.mode === 'CUSTOM' && !(Number(form.packageLessons) > 0)) {
			message.warning('课时数必须大于 0')
			return
		}
		pkgSubmitting.value = true
		try {
			const payload = {
				studentId: currentStudent.value.id,
				payChannel: form.payChannel
			}
			if (form.mode === 'PACKAGE') {
				payload.coursePackageId = form.coursePackageId
			} else {
				// 自定义课包：后端按课时/价格直接快照，不依赖课包表
				payload.courseId = form.courseId
				payload.packageLessons = form.packageLessons
				payload.packageGiftLessons = form.packageGiftLessons || 0
				payload.packageValidityDays = form.packageValidityDays || 0
				payload.amount = form.amount
			}
			const orderId = await studentApi.orderAdd(payload)
			if (!orderId) {
				message.error('下单失败，请重试')
				return
			}
			if (form.payNow) {
				try {
					await studentApi.orderOfflinePay({
						orderId,
						paidAmount: pkgPreview.value,
						payChannel: form.payChannel
					})
					message.success('调课包成功，课时已入账')
				} catch (e) {
					// 订单已生成但收款失败：保留订单，提示去收款台补收
					message.warning('订单已生成，请到收款台完成收款')
				}
			} else {
				message.success('已生成订单，待收款')
			}
			pkgOpen.value = false
			load()
			if (currentStudent.value) loadAccounts(currentStudent.value.id)
		} catch (e) {
			message.error(e?.msg || '调课包失败')
		} finally {
			pkgSubmitting.value = false
		}
	}

	const onEnrollSuccess = () => {
		enrollOpen.value = false
		load()
	}

	onMounted(async () => {
		await load()
		if (route.query.id) openDetailById(String(route.query.id))
	})
</script>

<style lang="less" scoped>
	.m-student-toolbar {
		display: flex;
		align-items: center;
		gap: 8px;
		margin-bottom: 12px;
		:deep(.ant-input-search) {
			flex: 1;
			min-width: 0;
		}
		:deep(.ant-input) {
			font-size: 16px;
		}
		:deep(.ant-btn) {
			flex: none;
			height: 40px;
			padding: 0 14px;
		}
	}
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
	}
	.m-student {
		display: block;
	}
	.m-student-top {
		display: flex;
		align-items: center;
		gap: 12px;
	}
	.m-student-main {
		flex: 1;
		min-width: 0;
	}
	.m-student-actions {
		display: flex;
		align-items: center;
		gap: 8px;
	}
	.m-student-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--text-color);
	}
	.m-student-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-student-extra {
		display: grid;
		grid-template-columns: 0.9fr 1.2fr 1fr;
		gap: 8px;
		margin-top: 10px;
		padding-top: 10px;
		border-top: 1px dashed var(--border-color-split);
		div {
			display: flex;
			flex-direction: column;
			gap: 2px;
			min-width: 0;
		}
		span {
			font-size: 11px;
			color: var(--text-color-secondary);
		}
		b {
			font-size: 13px;
			font-weight: 600;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
	}
	.m-text-muted {
		color: var(--text-color-secondary);
		font-weight: 400 !important;
	}
	.m-text-warning {
		color: var(--warning-text);
	}
	.m-text-danger {
		color: var(--danger-text);
	}
	.m-student-arrow {
		color: var(--text-color-tertiary);
	}
	.m-detail-name {
		font-size: 18px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-detail-sub {
		margin-top: 4px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-detail-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 8px;
		margin-top: 12px;
		padding: 10px 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
		div {
			display: flex;
			flex-direction: column;
			gap: 2px;
			min-width: 0;
		}
		span {
			font-size: 11px;
			color: var(--text-color-secondary);
		}
		b {
			font-size: 13px;
			font-weight: 600;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
	}
	.m-detail-enroll {
		height: 42px;
		margin-top: 12px;
	}
	/* 详情页操作区：报名/续费 + 调课包并排 */
	.m-detail-actions {
		display: flex;
		flex-direction: column;
		gap: 8px;
		margin-top: 12px;
		:deep(.ant-btn) {
			height: 42px;
		}
	}
	.m-pkg-hint {
		margin-bottom: 12px;
		padding: 8px 10px;
		border-radius: 6px;
		background: rgba(24, 144, 255, 0.06);
		font-size: 12px;
		line-height: 1.5;
		color: var(--text-color-secondary);
	}
	.m-pkg-summary {
		display: flex;
		align-items: baseline;
		justify-content: space-between;
		margin: 8px 2px 12px;
		font-size: 13px;
		color: var(--text-color-secondary);
		b {
			font-size: 20px;
			font-weight: 700;
			color: #fa541c;
		}
	}
	.m-section-title {
		margin: 16px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-empty {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 30px 12px;
		text-align: center;
		color: var(--text-color-secondary);
	}
</style>
