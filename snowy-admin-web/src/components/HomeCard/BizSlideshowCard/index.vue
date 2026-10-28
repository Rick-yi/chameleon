<template>
	<a-card :title="title" :bordered="false" :class="{ 'has-banner': bannerList.length }">
		<!-- 有真实配置的轮播图 -->
		<a-carousel v-if="bannerList.length" class="snowy-right-card-one" autoplay arrows>
			<template #prevArrow>
				<div class="custom-slick-arrow" style="left: 10px; z-index: 1">
					<LeftOutlined />
				</div>
			</template>
			<template #nextArrow>
				<div class="custom-slick-arrow" style="right: 10px"><RightOutlined /></div>
			</template>
			<img
				v-for="item in bannerList"
				:key="item.id"
				:src="item.image"
				class="carousel-images"
				@click="leaveForOpen(item.pathDetails)"
			/>
		</a-carousel>
		<!-- 未配置轮播图：展示机构快捷入口，避免出现占位大图 -->
		<div v-else class="entry-panel">
			<div class="entry-hello">{{ helloText }}</div>
			<div class="entry-date">{{ todayText }}</div>
			<div class="entry-grid">
				<div v-for="item in entryList" :key="item.path" class="entry-item" @click="router.push(item.path)">
					<div class="entry-icon" :style="{ background: item.color }">
						<component :is="item.icon" />
					</div>
					<div class="entry-label">{{ item.text }}</div>
				</div>
			</div>
		</div>
	</a-card>
</template>

<script setup name="carousel">
	import bizIndexApi from '@/api/biz/bizIndexApi'
	import { cloneDeep } from 'lodash-es'
	import router from '@/router'
	import tool from '@/utils/tool'
	import { DashboardOutlined, WalletOutlined, TeamOutlined, CalendarOutlined } from '@ant-design/icons-vue'
	const slideshowList = ref([])
	// 后端在未配置轮播图时会返回一张固定占位图，这里过滤掉，只展示真实配置的图
	const PLACEHOLDER_IMAGE_PREFIX = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAfQAAADcCAIAAAAJPMQyAAAAGXRFWHRTb2Z0'
	const bannerList = computed(() =>
		(slideshowList.value || []).filter((item) => item.image && !item.image.startsWith(PLACEHOLDER_IMAGE_PREFIX))
	)
	// 有轮播图时沿用原标题（空标题），没有时给出「快捷入口」标题，避免出现空卡片头
	const title = computed(() => (bannerList.value.length ? '' : '快捷入口'))
	// 外部传来的参数
	const props = defineProps({
		config: {
			type: Object,
			default: () => {}
		}
	})
	// 快捷入口
	const entryList = [
		{ text: '工作台', path: '/workbench', icon: DashboardOutlined, color: '#eaf2fd' },
		{ text: '收款台', path: '/student/order', icon: WalletOutlined, color: '#e8f7f0' },
		{ text: '学员管理', path: '/student', icon: TeamOutlined, color: '#f3edfd' },
		{ text: '排课课表', path: '/course/schedule', icon: CalendarOutlined, color: '#fdf3e6' }
	]
	const orgName = computed(() => tool.data.get('USER_INFO')?.orgName || '本机构')
	const helloText = computed(() => `${orgName.value}，欢迎回来`)
	const todayText = computed(() => {
		const d = new Date()
		const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][d.getDay()]
		return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${week}`
	})
	// 界面加载完后异步查询
	onMounted(() => {
		// 获得业务首页轮播图
		const param = {
			// 这是字典内维护的该位置
			place: props.config.options.place ? props.config.options.place : 'BACK_SYS_INDEX'
		}
		bizIndexApi
			.bizIndexSlideshowList(param)
			.then((data) => {
				slideshowList.value = data
			})
			.catch(() => {})
	})
	// URL跟路由的跳转
	const leaveForOpen = (value) => {
		if (isEmpty(value)) {
			return
		}
		const detail = cloneDeep(value)
		let result = {}
		if (typeof detail !== 'object') {
			result = JSON.parse(detail)
		}
		// json内包含且是开启了点击，否则不处理
		if (result.whetherToClick && result.whetherToClick === 'ENABLE') {
			if (result.skipMode && result.skipMode === 'URL') {
				window.open(result.url)
			}
			if (result.skipMode && result.skipMode === 'ROUTER') {
				router.replace({
					path: result.url
				})
			}
		}
	}
</script>

<style scoped>
	.carousel-images {
		height: 180px;
		width: 100%;
		cursor: pointer;
	}
	.snowy-right-card-one {
		height: 180px;
	}
	.ant-carousel :deep(.slick-slide) {
		text-align: center;
		height: 180px;
		line-height: 150px;
		background: var(--primary-1);
		overflow: hidden;
	}
	.ant-carousel :deep(.slick-arrow.custom-slick-arrow) {
		width: 25px;
		height: 25px;
		font-size: 25px;
		color: #fff;
		background-color: rgba(31, 45, 61, 0.11);
		opacity: 0.3;
		z-index: 1;
	}
	.ant-carousel :deep(.custom-slick-arrow:before) {
		display: none;
	}
	.ant-carousel :deep(.custom-slick-arrow:hover) {
		opacity: 0.5;
	}
	.ant-carousel :deep(.slick-slide h3) {
		color: #fff;
	}
	/* 有轮播图时卡片内不留白 */
	.has-banner :deep(.ant-card-body) {
		padding: 0 !important;
	}
	/* ---------- 快捷入口（未配置轮播图时的替代内容） ---------- */
	.entry-panel {
		padding: 4px 0 8px;
	}
	.entry-hello {
		font-size: 15px;
		font-weight: 600;
		color: var(--text-color);
		line-height: 22px;
	}
	.entry-date {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.entry-grid {
		margin-top: 14px;
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 8px;
	}
	.entry-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 6px;
		padding: 10px 4px;
		border-radius: 6px;
		cursor: pointer;
		transition: background 0.2s;
	}
	.entry-item:hover {
		background: var(--background-color-light);
	}
	.entry-icon {
		width: 34px;
		height: 34px;
		border-radius: 8px;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 16px;
		color: var(--text-color);
	}
	.entry-label {
		font-size: 12px;
		color: var(--text-color-secondary);
	}
</style>
