<template>
	<div class="index-container">
		<a-row :gutter="10">
			<!-- 轮播图（小屏情况显示） -->
			<a-col :xs="24" :sm="24" :md="24" :lg="0" :xl="0">
				<!-- 轮播图 -->
				<biz-slideshow-card :config="slideshowConfig" class="mb-2" />
			</a-col>
			<a-col :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
				<!-- 人员信息 -->
				<sys-user-info-card class="mb-2" />
				<!-- 业务数据（超管专属） -->
				<sys-biz-data-card v-if="isSuperAdmin" class="mb-2" />
				<!-- 周访问量（平台运营专属） -->
				<sys-vis-chart-data-card v-if="isPlatformAdmin" class="mb-2" />
				<a-row :gutter="10">
					<!-- 访问记录 -->
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<sys-vis-log-card class="mb-2" />
					</a-col>
					<!-- 操作记录 -->
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<sys-op-log-card class="mb-2" />
					</a-col>
				</a-row>
			</a-col>
			<a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
				<a-row :gutter="10">
					<a-col :xs="0" :sm="0" :md="0" :lg="24" :xl="24">
						<!-- 轮播图 -->
						<biz-slideshow-card :config="slideshowConfig" class="mb-2" />
					</a-col>
					<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
						<!-- 基础工具（超管专属） -->
						<sys-tool-data-card v-if="isSuperAdmin" class="mb-2" />
					</a-col>
					<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
						<!-- 运维一览（超管专属） -->
						<sys-op-data-card v-if="isSuperAdmin" class="mb-2" />
					</a-col>
					<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
						<!-- 系统信息 -->
						<sys-info-card class="mb-2" />
					</a-col>
				</a-row>
			</a-col>
		</a-row>
	</div>
</template>

<script setup name="indexHome">
	import { computed } from 'vue'
	import tool from '@/utils/tool'

	// 轮播图配置
	const slideshowConfig = {
		options: {
			place: 'BACK_SYS_INDEX'
		}
	}

	// 首页部分卡片对应超管/平台运营专属接口，越权会 403，这里按角色隐藏，避免出现空白卡片
	const roleCodeList = computed(() => tool.data.get('USER_INFO')?.roleCodeList || [])
	const isSuperAdmin = computed(() => roleCodeList.value.includes('superAdmin'))
	const isPlatformAdmin = computed(() => roleCodeList.value.includes('platformAdmin'))
</script>

<style lang="less" scoped>
	.index-container {
		:deep(.ant-card) {
			transition: all 0.3s;
		}
	}
</style>
