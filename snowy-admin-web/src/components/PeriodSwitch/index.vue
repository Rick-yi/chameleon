<template>
	<a-radio-group :value="modelValue" size="small" button-style="solid" @change="onChange">
		<a-radio-button v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</a-radio-button>
	</a-radio-group>
</template>

<script setup name="periodSwitch">
	import { PERIOD_OPTIONS } from '@/utils/reportPeriod'

	const props = defineProps({
		modelValue: { type: String, default: 'MONTH' },
		allowCustom: { type: Boolean, default: false }
	})
	const emit = defineEmits(['update:modelValue', 'change'])
	const options = computed(() =>
		props.allowCustom && props.modelValue === 'CUSTOM'
			? [...PERIOD_OPTIONS, { value: 'CUSTOM', label: '自定义' }]
			: PERIOD_OPTIONS
	)
	const onChange = (event) => {
		const value = event?.target?.value
		if (!value) return
		emit('update:modelValue', value)
		emit('change', value)
	}
</script>
