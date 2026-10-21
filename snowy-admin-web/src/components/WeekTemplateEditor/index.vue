<template>
	<div>
		<div
			v-for="(item, index) in localList"
			:key="index"
			style="display: flex; gap: 8px; align-items: center; margin-bottom: 8px; flex-wrap: wrap"
		>
			<a-select v-model:value="item.dayOfWeek" style="width: 110px" :options="weekDayOptions" />
			<a-time-picker v-model:value="item.startTime" format="HH:mm" value-format="HH:mm" style="width: 110px" />
			<span>至</span>
			<a-time-picker v-model:value="item.endTime" format="HH:mm" value-format="HH:mm" style="width: 110px" />
			<a-input v-model:value="item.classroom" placeholder="教室" style="width: 120px" allow-clear />
			<a-input v-model:value="item.teacherId" placeholder="教师（可选）" style="width: 150px" allow-clear />
			<a-button type="link" danger size="small" @click="removeAt(index)">删除</a-button>
		</div>
		<a-button type="dashed" block @click="addAt">
			<template #icon><plus-outlined /></template>
			添加上课时段
		</a-button>
		<div style="color: rgba(0, 0, 0, 0.45); font-size: 12px; margin-top: 6px">
			固定课表用于「批量生成」排课：按周几 + 时段自动展开未来 N 周的排课；未填教室/教师的时段沿用班级默认设置
		</div>
	</div>
</template>

<script setup name="WeekTemplateEditor">
	// 每周固定课表编辑器（班级表单与排课页共用）
	// 编辑期间只在本地维护，保存时由父组件通过 getValue() 取值，避免逐字符输入被外部回写打断
	const props = defineProps({
		modelValue: { type: Array, default: () => [] }
	})
	const localList = ref([])
	watch(
		() => props.modelValue,
		(val) => {
			localList.value = (val || []).map((item) => ({ ...item }))
		},
		{ immediate: true }
	)

	const weekDayOptions = [
		{ value: 1, label: '周一' },
		{ value: 2, label: '周二' },
		{ value: 3, label: '周三' },
		{ value: 4, label: '周四' },
		{ value: 5, label: '周五' },
		{ value: 6, label: '周六' },
		{ value: 7, label: '周日' }
	]

	const addAt = () => {
		localList.value.push({ dayOfWeek: 1, startTime: '09:00', endTime: '10:30', classroom: '', teacherId: '' })
	}
	const removeAt = (index) => {
		localList.value.splice(index, 1)
	}
	// 暴露给父组件：保存时取值
	defineExpose({
		getValue: () => localList.value.map((item) => ({ ...item }))
	})
</script>
