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

		<div class="auto-setting ent-body">
			<div class="auto-setting-row">
				<span class="auto-setting-label">自动化总开关</span>
				<a-switch
					:checked="setting.paused !== 'YES'"
					checked-children="运行中"
					un-checked-children="已暂停"
					:loading="settingSaving"
					@change="onPauseChange"
				/>
				<span class="auto-setting-label">静默时段</span>
				<a-input v-model:value="setting.quietStart" placeholder="22:00" style="width: 78px" allow-clear />
				<span class="auto-setting-sep">至</span>
				<a-input v-model:value="setting.quietEnd" placeholder="08:00" style="width: 78px" allow-clear />
				<a-button size="small" :loading="settingSaving" @click="saveSetting">保存</a-button>
				<a-tag v-if="setting.inQuietHours" color="orange">当前静默中</a-tag>
			</div>
			<div class="auto-setting-hint">
				静默时段只跳过提醒类规则；自动结算、线索分配不受影响。时区：{{ setting.timezone || 'Asia/Shanghai' }}
			</div>
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
										:min="param.min ?? 0"
										:max="param.max"
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
										:placeholder="param.remark || '请输入'"
										style="width: 180px"
										@blur="onParamChange(rule)"
									/>
								</div>
							</div>
						</div>

						<div class="auto-guard">
							<span class="auto-guard-label">每日上限</span>
							<a-input-number
								v-model:value="rule.dailyCap"
								:min="0"
								:max="100000"
								size="small"
								style="width: 100px"
								@change="onRuleGuardChange(rule)"
							/>
							<span class="auto-guard-hint">0 表示不限</span>
							<template v-if="rule.supportsHoldout">
								<span class="auto-guard-label">对照组</span>
								<a-input-number
									v-model:value="rule.holdoutPercent"
									:min="0"
									:max="100"
									size="small"
									style="width: 84px"
									@change="onRuleGuardChange(rule)"
								/>
								<span class="auto-guard-hint">%</span>
							</template>
							<span class="auto-version">v{{ rule.version || 1 }}</span>
						</div>

						<div class="auto-card-foot">
							<div class="auto-last">
								<template v-if="rule.lastRunTime">
									最近执行 {{ rule.lastRunTime }}
									<a-tag :color="rule.lastRunSuccess ? 'green' : 'red'" style="margin-left: 6px">
										{{ rule.lastRunSuccess ? '成功' : '失败' }}
									</a-tag>
									<div class="auto-last-msg">{{ rule.lastRunMessage }}</div>
									<div v-if="rule.lastRunTime" class="auto-last-counts">
										成功 {{ rule.lastSuccessCount ?? 0 }} · 失败 {{ rule.lastFailCount ?? 0 }}
									</div>
								</template>
								<template v-else>
									<span class="auto-last-muted">尚未执行过（累计 0 次）</span>
								</template>
							</div>
							<div class="auto-card-actions">
								<a-button v-if="rule.supportsHoldout" size="small" @click="onExperiment(rule)">效果</a-button>
								<a-button v-if="rule.mode === 'TIMER' && hasPerm('/biz/automation/run')" size="small" @click="onRun(rule, true)">
									预览
								</a-button>
								<a-button v-if="rule.mode === 'TIMER' && hasPerm('/biz/automation/run')" type="primary" ghost :loading="running === rule.ruleCode" @click="onRun(rule)">
									<template #icon><play-circle-outlined /></template>
									立即执行
								</a-button>
							</div>
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
						<template v-if="column.dataIndex === 'counts'">
							<span class="auto-log-counts">
								成功 {{ record.successCount ?? 0 }} · 失败 {{ record.failCount ?? 0 }} · 跳过 {{ record.skipCount ?? 0 }}
							</span>
						</template>
						<template v-if="column.dataIndex === 'delivery'">
							<a-tag :color="deliveryColor(record.deliveryStatus)">
								{{ deliveryText(record.deliveryStatus) }}
							</a-tag>
							<span v-if="record.deliveryCount" class="auto-log-counts"> {{ record.deliveryCount }} 条</span>
						</template>
						<template v-if="column.dataIndex === 'experimentGroup'">
							<a-tag v-if="record.experimentGroup && record.experimentGroup !== 'NONE'">
								{{ record.experimentGroup }}
							</a-tag>
							<span v-else>-</span>
						</template>
						<template v-if="column.dataIndex === 'message'">
							<span class="auto-log-msg">{{ record.message }}</span>
						</template>
					</template>
				</a-table>
			</div>
		</a-spin>

		<a-modal v-model:open="experimentOpen" title="效果归因" :width="680" :footer="null">
			<a-alert type="info" show-icon :message="experimentData.note || '按 holdout 分组的观察性统计'" style="margin-bottom: 12px" />
			<a-table
				:columns="experimentColumns"
				:data-source="experimentData.groups || []"
				:row-key="(r) => r.group"
				size="small"
				:pagination="false"
			/>
		</a-modal>
	</div>
</template>

<script setup name="bizAutomation">
	import { message, Modal } from 'ant-design-vue'
	import automationApi from '@/api/biz/automationApi'

	const loading = ref(false)
	const switching = ref('')
	const running = ref('')
	const ruleList = ref([])
	const logList = ref([])
	const logLoading = ref(false)
	const settingSaving = ref(false)
	const setting = ref({ paused: 'NO', quietStart: '', quietEnd: '', timezone: 'Asia/Shanghai' })
	const experimentOpen = ref(false)
	const experimentData = ref({ groups: [], note: '' })

	const logColumns = [
		{ title: '规则', dataIndex: 'ruleName', width: 150 },
		{ title: '触发方式', dataIndex: 'triggerType', width: 90 },
		{ title: '结果', dataIndex: 'success', width: 76 },
		{ title: '成功/失败/跳过', dataIndex: 'counts', width: 160 },
		{ title: '触达', dataIndex: 'delivery', width: 110 },
		{ title: '实验组', dataIndex: 'experimentGroup', width: 100 },
		{ title: '说明', dataIndex: 'message' },
		{ title: '耗时', dataIndex: 'costMs', width: 76 },
		{ title: '执行时间', dataIndex: 'createTime', width: 160 }
	]
	const experimentColumns = [
		{ title: '分组', dataIndex: 'group', width: 100 },
		{ title: '目标数', dataIndex: 'total', width: 90 },
		{ title: '成功数', dataIndex: 'success', width: 90 },
		{ title: '转化率', dataIndex: 'rate', width: 90, customRender: ({ text }) => `${text ?? 0}%` },
		{ title: '归因金额', dataIndex: 'value', width: 110 }
	]

	const triggerText = (type) => ({ TIMER: '定时', MANUAL: '手动', TRIGGER: '实时' }[type] || type)
	const triggerColor = (type) => ({ TIMER: 'blue', MANUAL: 'orange', TRIGGER: 'purple' }[type] || 'default')
	const deliveryText = (status) => ({ CREATED: '已创建', FAILED: '失败', NONE: '未触达' }[status] || '-')
	const deliveryColor = (status) => ({ CREATED: 'green', FAILED: 'red' }[status] || 'default')

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
			loadSetting()
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

	const loadSetting = async () => {
		try {
			const data = await automationApi.getSetting()
			setting.value = {
				paused: data?.paused || 'NO',
				quietStart: data?.quietStart || '',
				quietEnd: data?.quietEnd || '',
				timezone: data?.timezone || 'Asia/Shanghai',
				inQuietHours: !!data?.inQuietHours
			}
		} catch (e) {
			// 设置接口失败不影响规则列表
		}
	}

	const saveSetting = () => {
		settingSaving.value = true
		automationApi
			.saveSetting({
				paused: setting.value.paused,
				quietStart: setting.value.quietStart,
				quietEnd: setting.value.quietEnd,
				timezone: setting.value.timezone
			})
			.then(() => {
				message.success('自动化设置已保存')
				loadSetting()
			})
			.finally(() => (settingSaving.value = false))
	}

	const onPauseChange = (checked) => {
		setting.value.paused = checked ? 'NO' : 'YES'
		saveSetting()
	}

	const onRuleGuardChange = (rule) => {
		save(rule, rule.enabled ? 'YES' : 'NO', collectParams(rule), false)
	}

	const onParamChange = (rule) => {
		save(rule, rule.enabled ? 'YES' : 'NO', collectParams(rule))
	}

	const onToggle = (rule, checked) => {
		switching.value = rule.ruleCode
		save(rule, checked ? 'YES' : 'NO', collectParams(rule)).finally(() => (switching.value = ''))
	}

	const save = (rule, enabled, params, showMessage = true) => {
		return automationApi
			.toggle({
				ruleCode: rule.ruleCode,
				enabled,
				params,
				dailyCap: rule.dailyCap || 0,
				holdoutPercent: rule.holdoutPercent || 0
			})
			.then(() => {
				rule.enabled = enabled === 'YES'
				if (showMessage) {
					message.success(enabled === 'YES' ? `已开启「${rule.ruleName}」` : `已关闭「${rule.ruleName}」`)
				}
			})
			.catch(() => {
				loadRules()
			})
	}

	const runRule = (rule) => {
		running.value = rule.ruleCode
		return automationApi
			.run({ ruleCode: rule.ruleCode, preview: false })
			.then((data) => {
				message.success(data?.message || '执行完成')
				loadRules()
			})
			.finally(() => (running.value = ''))
	}

	const onRun = (rule, previewOnly = false) => {
		running.value = rule.ruleCode
		automationApi
			.run({ ruleCode: rule.ruleCode, preview: true })
			.then((data) => {
				running.value = ''
				if (previewOnly) {
					Modal.info({ title: '影响面预览', content: data?.message || '没有需要处理的目标' })
					return
				}
				if (!data?.affectedCount) {
					message.info(data?.message || '没有需要处理的目标')
					return
				}
				Modal.confirm({
					title: `确认执行「${rule.ruleName}」`,
					content: `${data.message}，确认立即执行？`,
					okText: '确认执行',
					cancelText: '取消',
					onOk: () => runRule(rule)
				})
			})
			.catch(() => {
				running.value = ''
			})
	}

	const onExperiment = (rule) => {
		automationApi.experiment(rule.ruleCode, 90).then((data) => {
			experimentData.value = data || { groups: [], note: '' }
			experimentOpen.value = true
		})
	}

	loadRules()
</script>

<style lang="less" scoped>
	.auto-tip {
		margin-bottom: 12px;
	}
	.auto-setting {
		margin-bottom: 12px;
	}
	.auto-setting-row {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-wrap: wrap;
	}
	.auto-setting-label {
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.auto-setting-sep {
		color: var(--text-color-tertiary);
	}
	.auto-setting-hint {
		margin-top: 6px;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.auto-guard {
		display: flex;
		align-items: center;
		gap: 6px;
		flex-wrap: wrap;
		margin-top: 10px;
		padding: 8px 12px;
		border: 1px dashed var(--border-color-split);
		border-radius: var(--radius-control);
	}
	.auto-guard-label {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.auto-guard-hint {
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.auto-version {
		margin-left: auto;
		font-size: 12px;
		color: var(--text-color-tertiary);
	}
	.auto-card-actions {
		display: flex;
		align-items: center;
		gap: 6px;
		flex-wrap: wrap;
		justify-content: flex-end;
	}
	.auto-last-counts {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-tertiary);
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
