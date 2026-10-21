<template>
	<div class="page-header">
		<div>
			<div class="page-header-title">{{ title }}</div>
			<div v-if="desc" class="page-header-desc">{{ desc }}</div>
		</div>
		<div v-if="$slots.extra" class="page-header-extra">
			<slot name="extra" />
		</div>
	</div>
</template>

<script setup name="PageHeader">
	// 业务页统一页头：标题 + 说明 + 右侧操作区
	defineProps({
		title: { type: String, default: '' },
		desc: { type: String, default: '' }
	})
</script>
