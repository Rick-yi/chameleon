<template>
	<a-modal title="预览" :width="700" :open="open" :destroy-on-close="true" :footer="null" @cancel="onClose">
		<!--
			此处不做净化：内容是当前管理员正在编辑的模板本身，输入者与查看者是同一人，
			不存在「他人写入、我执行」的存储型 XSS 链路；净化反而会让预览与真实发信结果不一致。
			若将来改为预览他人共享的模板，必须改为 sanitizeHtml。
		-->
		<div class="preview-container" v-html="previewHtml"></div>
	</a-modal>
</template>

<script setup name="emailTemplatePreview">
	// 默认是关闭状态
	const open = ref(false)
	const previewHtml = ref('')
	const formRef = ref()
	// 打开抽屉
	const onOpen = (html) => {
		open.value = true
		previewHtml.value = html
	}
	// 关闭抽屉
	const onClose = () => {
		previewHtml.value = ''
		open.value = false
	}
	// 调用这个函数将子组件的一些数据和方法暴露出去
	defineExpose({
		onOpen
	})
</script>
<style scoped>
	.preview-container {
		overflow: auto;
		max-height: 600px;
	}
	.preview-container::-webkit-scrollbar {
		display: none;
	}
	.preview-container {
		-ms-overflow-style: none;
		scrollbar-width: none;
	}
</style>
