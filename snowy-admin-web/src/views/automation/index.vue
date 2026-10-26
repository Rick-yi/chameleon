<template>
	<div>
		<page-header title="自动化中心" desc="把重复的人工动作交给系统：点名结算、线索分配、收款与续费提醒">
			<template #extra>
				<a-button @click="loadRules">
					<template #icon><reload-outlined /></template>
					刷新
				</a-button>
			</template>
		</page-header>

		<div class="auto-tip ent-note">
			自动化规则默认关闭，开启后立即生效；定时型规则每 10 分钟自动跑一次，实时型规则在业务动作发生时生效。
			所有自动操作都会留下执行记录，出问题可以随时查、也可以在对应业务页手工撤销。
		</div>

		<a-spin :spinning="loading">
			<a-row :gutter="12">
				<a-col v-for="rule in ruleList" :key="rule.ruleCode" :xs="24" :sm="12" :xl="12">
					<div class="auto-card">
						<div class="auto-card-head">
							<div class="auto-card-title">
								<span>{{ rule.ruleName }}</span>
								<a-tag :color="rule.mode === 'TIMER' ? 'blue' : 'purple'">
									{{ rule.mode === 'TIMER' ? '定时执行' : '实时触发' }}
								</a-tag>
							</div>
							<a-switch
								:checked="rule.enabled"
								:loading="switching === rule.ruleCode"
								@change="(checked) => onToggle(rule, checked)"
							/>
						</div>
						<div class="auto-card-desc">{{ rule.ruleDesc }}</div>

						<div v-if="rule.paramDef && rule.paramDef.length" class="auto-params">
							<div v-for="param in rule.paramDef" :key="param.key" class="auto-param">
								<div class="auto-param-label">
									{{ param.label }}
									<a-tooltip v-if="param.remark" :title="param.remark">
										<question-circle-outlined class="auto-param-help" />
									</a-tooltip>
								</div>
								<div class="auto-param-control">
									<a-input-number
										v-if="param.type === 'NUMBER'"
										v-model:value="paramModel[rule.ruleCode][param.key]"
										:min="0"
										:step="1"
										style="width: 130px"
										@change="onParamChange(rule)"
									/>
									<a-switch
										v-else-if="param.type === 'SWITCH'"
										v-model:checked="paramModel[rule.ruleCode][param.key]"
										@change="onParamChange(rule)"
									/>
									<a-input
										v-else
										v-model:value="paramModel[rule.ruleCode][param.key]"
										placeholder="留空表示不排除任何人"
										style="width: 180px"
										@blur="onParamChange(rule)"
									/>
								</div>
							</div>
						</div>

						<div class="auto-card-foot">
							<div class="auto-last">
								<template v-if="rule.lastRunTime">
									最近执行 {{ rule.lastRunTime }}
									<a-tag :color="rule.lastRunSuccess ? 'green' : 'red'" style="margin-left: 6px">
										{{ rule.lastRunSuccess ? '成功' : '失败' }}
									</a-tag>
									<div class="auto-last-msg">{{ rule.lastRunMessage }}</div>
								</template>
								<template v-else>
									<span class="auto-last-muted">尚未执行过（累计 0 次）</span>
								</template>
							</div>
							<a-button v-if="rule.mode === 'TIMER' && hasPerm('/biz/automation/run')" type="primary" ghost :loading="running === rule.ruleCode" @click="onRun(rule)">
								<template #icon><play-circle-outlined /></template>
								立即执行
							</a-button>
						</div>
					</div>
				</a-col>
			</a-row>

			<!-- 执行记录 -->
			<div class="ent-body" style="margin-top: 12px">
				<a-table
					:columns="logColumns"
					:data-source="logList"
					:row-key="(r) => r.id"
					:loading="logLoading"
					size="middle"
					:pagination="false"
					:scroll="{ y: 320 }"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'ruleName'">
							{{ record.ruleName || record.ruleCode }}
						</template>
						<template v-if="column.dataIndex === 'triggerType'">
							<a-tag :color="triggerColor(record.triggerType)">{{ triggerText(record.triggerType) }}</a-tag>
						</template>
						<template v-if="column.dataIndex === 'success'">
							<a-tag :color="record.success === 'YES' ? 'green' : 'red'">
								{{ record.success === 'YES' ? '成功' : '失败' }}
							</a-tag>
						</template>
						<template v-if="column.dataIndex === 'message'">
							<span class="auto-log-msg">{{ record.message }}</span>
						</template>
					</template>
				</a-table>
			</div>
		</a-spin>
	</div>
</template>

<script setup name="bizAutomation">
	import { message } from 'ant-design-vue'
	import automationApi from '@/api/biz/automationApi'

	const loading = ref(false)
	const switching = ref('')
	const running = ref('')
	const ruleList = ref([])
	const logList = ref([])
	const logLoading = ref(false)

	const logColumns = [
		{ title: '规则', dataIndex: 'ruleName', width: 160 },
		{ title: '触发方式', dataIndex: 'triggerType', width: 100 },
		{ title: '结果', dataIndex: 'success', width: 80 },
		{ title: '说明', dataIndex: 'message' },
		{ title: '影响条数', dataIndex: 'affectedCount', width: 90 },
		{ title: '耗时', dataIndex: 'costMs', width: 80 },
		{ title: '执行时间', dataIndex: 'createTime', width: 160 }
	]

	const triggerText = (type) => ({ TIMER: '定时', MANUAL: '手动', TRIGGER: '实时' }[type] || type)
	const triggerColor = (type) => ({ TIMER: 'blue', MANUAL: 'orange', TRIGGER: 'purple' }[type] || 'default')

	// 参数编辑模型：ruleCode -> {paramKey: 值}（开关存布尔，数字存数值）
	const paramModel = ref({})

	/** 把后端返回的规则参数转成可编辑模型 */
	const buildParamModel = (rules) => {
		const model = {}
		for (const rule of rules) {
			const ruleModel = {}
			for (const param of rule.paramDef || []) {
				const raw = rule.params ? rule.params[param.key] : undefined
				if (param.type === 'SWITCH') {
					ruleModel[param.key] = (raw ?? param.defaultValue) !== 'NO'
				} else if (param.type === 'NUMBER') {
					const num = Number(raw ?? param.defaultValue)
					ruleModel[param.key] = Number.isNaN(num) ? undefined : num
				} else {
					ruleModel[param.key] = raw ?? param.defaultValue ?? ''
				}
			}
			model[rule.ruleCode] = ruleModel
		}
		paramModel.value = model
	}

	/** 收集某条规则当前参数 */
	const collectParams = (rule) => {
		const params = {}
		const model = paramModel.value[rule.ruleCode] || {}
		for (const param of rule.paramDef || []) {
			const value = model[param.key]
			params[param.key] = param.type === 'SWITCH' ? (value ? 'YES' : 'NO') : String(value ?? '')
		}
		return params
	}

	const loadRules = async () => {
		loading.value = true
		try {
			const data = await automationApi.ruleList()
			ruleList.value = data || []
			buildParamModel(ruleList.value)
			loadLog()
		} finally {
			loading.value = false
		}
	}

	const loadLog = async () => {
		logLoading.value = true
		try {
			const data = await automationApi.logPage({ current: 1, size: 20 })
			logList.value = data?.records || []
		} finally {
			logLoading.value = false
		}
	}

	const onParamChange = (rule) => {
		save(rule, rule.enabled ? 'YES' : 'NO', collectParams(rule))
	}

	const onToggle = (rule, checked) => {
		switching.value = rule.ruleCode
		save(rule, checked ? 'YES' : 'NO', collectParams(rule)).finally(() => (switching.value = ''))
	}

	const save = (rule, enabled, params) => {
		return automationApi
			.toggle({ ruleCode: rule.ruleCode, enabled, params })
			.then(() => {
				rule.enabled = enabled === 'YES'
				message.success(enabled === 'YES' ? `已开启「${rule.ruleName}」` : `已关闭「${rule.ruleName}」`)
			})
			.catch(() => {
				loadRules()
			})
	}

	const onRun = (rule) => {
		running.value = rule.ruleCode
		automationApi
			.run({ ruleCode: rule.ruleCode })
			.then((data) => {
				message.success(data?.message || '执行完成')
				loadRules()
			})
			.finally(() => (running.value = ''))
	}

	loadRules()
</script>

<style lang="less" scoped>
	.auto-tip {
		margin-bottom: 12px;
	}
	.auto-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 16px;
		margin-bottom: 12px;
		height: calc(100% - 12px);
	}
	.auto-card-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}
	.auto-card-title {
		display: flex;
		align-items: center;
		gap: 8px;
		font-size: 15px;
		font-weight: 600;
		color: var(--text-color);
	}
	.auto-card-desc {
		margin-top: 8px;
		font-size: 13px;
		line-height: 20px;
		color: var(--text-color-secondary);
	}
	.auto-params {
		margin-top: 12px;
		padding: 10px 12px;
		background: var(--background-color-light);
		border-radius: var(--radius-control);
	}
	.auto-param {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: 3px 0;
	}
	.auto-param-label {
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.auto-param-help {
		color: var(--text-color-tertiary);
		margin-left: 4px;
	}
	.auto-card-foot {
		margin-top: 12px;
		padding-top: 12px;
		border-top: 1px solid var(--border-color-split);
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}
	.auto-last {
		font-size: 12px;
		color: var(--text-color-secondary);
		min-width: 0;
	}
	.auto-last-msg {
		margin-top: 2px;
		color: var(--text-color-tertiary);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.auto-last-muted {
		color: var(--text-color-tertiary);
	}
	.auto-log-msg {
		color: var(--text-color-secondary);
	}
</style>
