<template>
	<div style="position: relative">
		<a-space class="go-back-button">
			<a-button :href="props.src" size="small" target="_blank">
				<template #icon><download-outlined /></template>
			</a-button>
			<a-button type="primary" size="small" @click="emit('goBack')">
				<template #icon><rollback-outlined /></template>
				返回
			</a-button>
		</a-space>
		<a-card :bordered="false" :body-style="{ padding: 0 }">
			<a-spin :spinning="loading">
				<vue-office-docx
					v-if="currentFileType === 'doc' || currentFileType === 'docx'"
					:src="props.src + '&token=' + tool.data.get('TOKEN')"
					class="xn-ht82"
					@rendered="renderedHandler"
				/>
				<vue-office-excel
					v-else-if="currentFileType === 'xls' || currentFileType === 'xlsx'"
					:src="props.src + '&token=' + tool.data.get('TOKEN')"
					class="xn-ht82"
					@rendered="renderedHandler"
					@error="errorHandler"
				/>
				<vue-pdf-embed
					v-else-if="currentFileType === 'pdf'"
					annotation-layer
					text-layer
					:source="props.src + '&token=' + tool.data.get('TOKEN')"
					@rendered="renderedHandler"
					@renderingFailed="errorHandler"
				/>
				<img
					v-else-if="
						currentFileType === 'png' ||
						currentFileType === 'jpg' ||
						currentFileType === 'gif' ||
						currentFileType === 'bmp' ||
						currentFileType === 'jpeg' ||
						currentFileType === 'ico' ||
						currentFileType === 'svg'
					"
					:src="props.src + '&token=' + tool.data.get('TOKEN')"
					class="xn-mwh"
				/>
				<a-result v-else status="warning" title="不支持预览的文件类型" />
			</a-spin>
		</a-card>
	</div>
</template>

<script setup>
	import tool from '@/utils/tool'
	import { message } from 'ant-design-vue'
	//引入VueOfficeDocx组件
	import VueOfficeDocx from '@vue-office/docx'
	//引入相关样式
	import '@vue-office/docx/lib/index.css'
	//引入VueOfficeExcel组件
	import VueOfficeExcel from '@vue-office/excel'
	//引入相关样式
	import '@vue-office/excel/lib/index.css'
	//引Pdf组件
	// 调用组件：https://github.com/hrynko/vue-pdf-embed
	import VuePdfEmbed from 'vue-pdf-embed'
	import 'vue-pdf-embed/dist/styles/annotationLayer.css'
	import 'vue-pdf-embed/dist/styles/textLayer.css'

	const loading = ref(false)
	const emit = defineEmits({ goBack: null })
	const props = defineProps({
		src: {
			type: String,
			default: '',
			required: true
		},
		// 文件类型
		fileType: {
			type: String,
			default: 'defaults',
			required: false
		}
	})
	const currentFileType = ref()
	watch(
		() => props.src,
		(newVal) => {
			if (newVal) {
				currentFileType.value = props.fileType.toLowerCase()
			}
		},
		{ immediate: true, deep: true }
	)
	watch(
		() => props.src,
		(newVal) => {
			if (newVal) {
				if (
					currentFileType.value === 'doc' ||
					currentFileType.value === 'docx' ||
					currentFileType.value === 'xls' ||
					currentFileType.value === 'xlsx' ||
					currentFileType.value === 'pdf'
				) {
					loading.value = true
				}
			}
		},
		{ immediate: true, deep: true }
	)
	// 渲染完成
	const renderedHandler = () => {
		loading.value = false
	}
	// 渲染失败
	const errorHandler = () => {
		message.warning('渲染失败，请尝试重新打开！')
	}
</script>

<style lang="less" scoped>
	.xn-mwh {
		max-width: 100%;
	}
	.xn-ht82 {
		height: 82vh;
	}
	.go-back-button {
		position: absolute;
		float: right;
		right: 0;
		z-index: 999;
	}
</style>
