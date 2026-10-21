<template>
	<div class="m-lead-detail">
		<!-- 学员信息 -->
		<div class="m-card">
			<div class="m-detail-name">{{ lead?.name || '加载中' }}</div>
			<div class="m-detail-sub">
				<a :href="`tel:${lead?.parentPhone || ''}`">
					<a-icon><phone-outlined /></a-icon> {{ lead?.parentPhone || '无电话' }}
				</a>
				<template v-if="lead?.intentCourseName"> · 意向 {{ lead.intentCourseName }}</template>
			</div>
			<div class="m-detail-tags">
				<a-tag :color="statusColor">{{ statusText }}</a-tag>
				<a-tag>来源：{{ lead?.source || '-' }}</a-tag>
			</div>
		</div>

		<!-- 跟进时间线 -->
		<div class="m-section-title">跟进记录</div>
		<div v-if="!followList.length" class="m-card m-empty">还没有跟进记录</div>
		<div v-for="item in followList" :key="item.id" class="m-card m-follow">
			<div class="m-follow-head">
				<span class="m-follow-type">{{ followTypeText(item.followType) }}</span>
				<span class="m-follow-time">{{ String(item.createTime || '').slice(5, 16) }}</span>
			</div>
			<div class="m-follow-content">{{ item.content }}</div>
			<div v-if="item.nextFollowTime" class="m-follow-next">
				下次跟进 {{ String(item.nextFollowTime).slice(0, 16) }}
			</div>
		</div>

		<!-- 底部操作 -->
		<div class="m-foot-bar">
			<a-button @click="followOpen = true">写跟进</a-button>
			<a-button @click="trialOpen = true">安排试听</a-button>
			<a-button type="primary" @click="convertOpen = true">转学员</a-button>
		</div>

		<!-- 写跟进 -->
		<a-modal v-model:open="followOpen" title="写跟进" :width="320" @ok="onFollow">
			<a-form layout="vertical">
				<a-form-item label="跟进方式">
					<a-radio-group v-model:value="followForm.followType" :options="followTypeOptions" button-style="solid" />
				</a-form-item>
				<a-form-item label="跟进内容">
					<a-textarea v-model:value="followForm.content" :rows="3" placeholder="沟通要点，如：家长关注价格与时间" />
				</a-form-item>
				<a-form-item label="下次跟进时间">
					<a-date-picker v-model:value="followForm.nextFollowTime" show-time style="width: 100%" />
				</a-form-item>
			</a-form>
		</a-modal>

		<!-- 安排试听 -->
		<a-modal v-model:open="trialOpen" title="安排试听" :width="320" @ok="onTrial">
			<a-form layout="vertical">
				<a-form-item label="试听课程">
					<a-select v-model:value="trialForm.courseId" :options="courseOptions" placeholder="选择课程" />
				</a-form-item>
				<a-form-item label="试听时间">
					<a-date-picker v-model:value="trialForm.trialTime" show-time style="width: 100%" />
				</a-form-item>
			</a-form>
		</a-modal>

		<!-- 转学员 -->
		<a-modal v-model:open="convertOpen" title="转为学员" :width="320" @ok="onConvert">
			<a-form layout="vertical">
				<a-form-item label="学员姓名">
					<a-input v-model:value="convertForm.name" :placeholder="lead?.name" />
				</a-form-item>
				<a-form-item label="家长手机">
					<a-input v-model:value="convertForm.parentPhone" :placeholder="lead?.parentPhone" />
				</a-form-item>
				<a-form-item label="报名课包">
					<a-select v-model:value="convertForm.coursePackageId" :options="packageOptions" placeholder="选择课包" />
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="appEnrollLead">
	import { useRoute } from 'vue-router'
	import { message } from 'ant-design-vue'
	import crmApi from '@/api/crm/crmApi'
	import courseApi from '@/api/course/courseApi'
	import studentApi from '@/api/student/studentApi'

	const route = useRoute()
	const leadId = ref(route.query.id || '')
	const lead = ref(null)
	const followList = ref([])
	const courseOptions = ref([])
	const packageOptions = ref([])

	const followOpen = ref(false)
	const trialOpen = ref(false)
	const convertOpen = ref(false)
	const followForm = ref({ followType: 'PHONE', content: '', nextFollowTime: undefined })
	const trialForm = ref({ courseId: undefined, trialTime: undefined })
	const convertForm = ref({ name: '', parentPhone: '', coursePackageId: undefined })

	const followTypeOptions = [
		{ value: 'PHONE', label: '电话' },
		{ value: 'VISIT', label: '到访' },
		{ value: 'WECHAT', label: '微信' }
	]
	const followTypeText = (t) => ({ PHONE: '电话沟通', VISIT: '到访', WECHAT: '微信', TRIAL: '试听' }[t] || t)
	const statusText = computed(
		() => ({ NEW: '新线索', FOLLOWED: '跟进中', TRIALED: '已试听', ENROLLED: '已报名', LOST: '已流失' }[lead.value?.status] || lead.value?.status)
	)
	const statusColor = computed(
		() => ({ NEW: 'blue', FOLLOWED: 'orange', TRIALED: 'purple', ENROLLED: 'green', LOST: 'default' }[lead.value?.status] || 'default')
	)

	const load = async () => {
		if (!leadId.value) return
		const [detailRes, followRes] = await Promise.all([
			crmApi.leadDetail({ id: leadId.value }).catch(() => null),
			crmApi.followPage({ current: 1, size: 50, leadId: leadId.value }).catch(() => ({}))
		])
		lead.value = detailRes?.data || null
		followList.value = followRes?.records || []
	}

	const loadOptions = async () => {
		const [courseRes, packageRes] = await Promise.all([
			courseApi.coursePage({ current: 1, size: 100 }).catch(() => ({})),
			courseApi.packagePage({ current: 1, size: 100 }).catch(() => ({}))
		])
		courseOptions.value = (courseRes?.records || []).map((item) => ({ value: item.id, label: item.name }))
		packageOptions.value = (packageRes?.records || []).map((item) => ({ value: item.id, label: item.name }))
	}

	const onFollow = async () => {
		if (!followForm.value.content) {
			message.warning('请填写跟进内容')
			return
		}
		await crmApi.followAdd({
			leadId: leadId.value,
			followType: followForm.value.followType,
			content: followForm.value.content,
			nextFollowTime: followForm.value.nextFollowTime ? String(followForm.value.nextFollowTime) : undefined
		})
		message.success('跟进已记录')
		followOpen.value = false
		followForm.value = { followType: 'PHONE', content: '', nextFollowTime: undefined }
		load()
	}

	const onTrial = async () => {
		if (!trialForm.value.courseId || !trialForm.value.trialTime) {
			message.warning('请选择课程与时间')
			return
		}
		await crmApi.trialAdd({
			leadId: leadId.value,
			courseId: trialForm.value.courseId,
			trialTime: String(trialForm.value.trialTime)
		})
		message.success('试听已安排')
		trialOpen.value = false
		load()
	}

	// 转学员：建学员档案 →（选课包时）开报名单 → 线索置为已报名
	const onConvert = async () => {
		if (!convertForm.value.name || !convertForm.value.parentPhone) {
			message.warning('请填写学员姓名与家长手机')
			return
		}
		const studentRes = await studentApi.studentAdd({
			name: convertForm.value.name,
			parentPhone: convertForm.value.parentPhone,
			gender: '男',
			status: 'STUDYING'
		})
		const studentId = studentRes?.data
		if (convertForm.value.coursePackageId) {
			await studentApi.orderAdd({
				studentId,
				coursePackageId: convertForm.value.coursePackageId,
				payChannel: 'OFFLINE'
			})
		}
		await crmApi.leadEdit({ ...lead.value, status: 'ENROLLED' })
		message.success(convertForm.value.coursePackageId ? '已转为学员并生成报名单，请到收款台完成收款' : '已转为学员')
		convertOpen.value = false
	}

	onMounted(async () => {
		await loadOptions()
		await load()
		if (lead.value) {
			convertForm.value.name = lead.value.name
			convertForm.value.parentPhone = lead.value.parentPhone
		}
	})
</script>

<style lang="less" scoped>
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 12px 14px;
		margin-bottom: 10px;
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
		a {
			color: var(--primary-6);
		}
	}
	.m-detail-tags {
		margin-top: 8px;
		display: flex;
		gap: 6px;
	}
	.m-section-title {
		margin: 16px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-follow-head {
		display: flex;
		justify-content: space-between;
		font-size: 12px;
		color: #86909c;
	}
	.m-follow-type {
		color: var(--primary-6);
	}
	.m-follow-content {
		margin-top: 4px;
		font-size: 14px;
		color: var(--text-color);
		line-height: 20px;
	}
	.m-follow-next {
		margin-top: 4px;
		font-size: 12px;
		color: #d89614;
	}
	.m-foot-bar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 56px;
		z-index: 15;
		display: flex;
		gap: 8px;
		padding: 10px 12px;
		background: #fff;
		border-top: 1px solid var(--border-color-split);
		.ant-btn {
			flex: 1;
			height: 40px;
		}
	}
	.m-empty {
		color: var(--text-color-secondary);
		text-align: center;
		padding: 20px 12px;
	}
</style>
