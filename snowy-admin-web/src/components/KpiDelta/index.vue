<template>
	<span v-if="text" class="kpi-delta" :class="tone">{{ text }}</span>
</template>

<script setup name="kpiDelta">
	const props = defineProps({
		current: {
			type: [Number, String],
			default: 0
		},
		previous: {
			type: [Number, String],
			default: 0
		},
		// true 表示「上升是坏事」（如退费、缺勤、退费率）
		invert: {
			type: Boolean,
			default: false
		}
	})

	const pct = computed(() => {
		const current = Number(props.current) || 0
		const previous = Number(props.previous) || 0
		if (!previous) return null
		return ((current - previous) / previous) * 100
	})

	const text = computed(() => (pct.value === null ? '' : `较上期 ${pct.value > 0 ? '+' : ''}${pct.value.toFixed(1)}%`))

	const tone = computed(() => {
		if (pct.value === null || Math.abs(pct.value) < 0.05) return 'flat'
		const good = props.invert ? pct.value < 0 : pct.value > 0
		return good ? 'up' : 'down'
	})
</script>

<style lang="less" scoped>
	.kpi-delta {
		display: inline-block;
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);

		&.up {
			color: var(--success-text);
		}

		&.down {
			color: var(--danger-text);
		}
	}
</style>
