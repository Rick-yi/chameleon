<template>
	<div class="m-app">
		<header class="m-header">
			<a-icon v-if="showBack" class="m-header-back" @click="onBack"><arrow-left-outlined /></a-icon>
			<div class="m-header-main">
				<div class="m-header-title">{{ headerTitle }}</div>
				<div class="m-header-sub">{{ appStore.orgName }} · {{ appStore.viewName }}</div>
			</div>
			<div class="m-header-right">
				<a-badge :count="todoCount" :offset="[2, 2]">
					<a class="m-header-btn" @click="go('/app/home')"><bell-outlined /></a>
				</a-badge>
				<a class="m-header-btn" @click="viewOpen = true"><appstore-outlined /></a>
			</div>
		</header>

		<main class="m-body">
			<router-view v-slot="{ Component }">
				<keep-alive>
					<component :is="Component" :key="route.path" />
				</keep-alive>
			</router-view>
		</main>

		<nav class="m-tabbar">
			<div
				v-for="tab in tabs"
				:key="tab.path"
				class="m-tab"
				:class="{ active: isActive(tab) }"
				@click="go(tab.path)"
			>
				<component :is="tab.icon" class="m-tab-icon" />
				<span>{{ tab.text }}</span>
			</div>
		</nav>

		<!-- 视角切换 -->
		<a-modal v-model:open="viewOpen" title="切换视角" :footer="null" :width="320">
			<div class="m-view-list">
				<div
					v-for="item in appStore.allowedViews"
					:key="item.key"
					class="m-view-item"
					:class="{ active: appStore.view === item.key }"
					@click="onSwitchView(item.key)"
				>
					<div class="m-view-name">{{ item.name }}</div>
					<div class="m-view-desc">{{ item.desc }}</div>
				</div>
			</div>
		</a-modal>
	</div>
</template>

<script setup name="appLayout">
	import { useRoute, useRouter } from 'vue-router'
	import { useAppStore } from '@/store/app'
	import {
		HomeOutlined,
		CalendarOutlined,
		TeamOutlined,
		UserOutlined,
		PhoneOutlined,
		WalletOutlined,
		BarChartOutlined
	} from '@ant-design/icons-vue'

	const route = useRoute()
	const router = useRouter()
	const appStore = useAppStore()
	const viewOpen = ref(false)
	const todoCount = ref(0)

	/** 不同视角的底部导航 */
	const TABS = {
		teach: [
			{ path: '/app/home', text: '工作台', icon: HomeOutlined },
			{ path: '/app/teach/schedule', text: '课表', icon: CalendarOutlined },
			{ path: '/app/teach/students', text: '学员', icon: TeamOutlined },
			{ path: '/app/mine', text: '我的', icon: UserOutlined }
		],
		enroll: [
			{ path: '/app/home', text: '工作台', icon: HomeOutlined },
			{ path: '/app/enroll/leads', text: '线索', icon: PhoneOutlined },
			{ path: '/app/teach/students', text: '学员', icon: TeamOutlined },
			{ path: '/app/mine', text: '我的', icon: UserOutlined }
		],
		cashier: [
			{ path: '/app/home', text: '工作台', icon: HomeOutlined },
			{ path: '/app/cashier/orders', text: '收款', icon: WalletOutlined },
			{ path: '/app/report', text: '报表', icon: BarChartOutlined },
			{ path: '/app/mine', text: '我的', icon: UserOutlined }
		],
		all: [
			{ path: '/app/home', text: '工作台', icon: HomeOutlined },
			{ path: '/app/report', text: '报表', icon: BarChartOutlined },
			{ path: '/app/enroll/leads', text: '线索', icon: PhoneOutlined },
			{ path: '/app/mine', text: '我的', icon: UserOutlined }
		]
	}

	const tabs = computed(() => TABS[appStore.view] || TABS.all)

	const headerTitle = computed(() => {
		const map = {
			'/app/home': '工作台',
			'/app/teach/schedule': '我的课表',
			'/app/teach/attend': '点名消课',
			'/app/teach/students': '学员名册',
			'/app/student/renew': '续费预警',
			'/app/enroll/leads': '我的线索',
			'/app/enroll/lead': '线索详情',
			'/app/cashier/orders': '收款台',
			'/app/report': '经营报表',
			'/app/mine': '我的'
		}
		return map[route.path] || '教培助手'
	})

	const showBack = computed(() => !['/app/home', '/app/mine'].includes(route.path))

	const isActive = (tab) => {
		if (tab.path === '/app/home') return route.path === '/app/home'
		return route.path.startsWith(tab.path)
	}

	const go = (path) => {
		if (route.path !== path) router.push(path)
	}

	const onBack = () => {
		if (window.history.length > 1) router.back()
		else router.push('/app/home')
	}

	// App 壳可能只注入了 token（没有菜单），这里补一次用户信息
	onMounted(async () => {
		if (!appStore.userInfo?.id) {
			try {
				const { useUserStore } = await import('@/store/user')
				await useUserStore().initUserInfo()
				appStore.refreshUser()
				// 当前视角不在该角色允许范围内时，回落到角色默认视角
				const allowedKeyList = appStore.allowedViews.map((item) => item.key)
				if (!allowedKeyList.includes(appStore.view)) {
					appStore.setView(appStore.defaultView())
				}
				// 用户信息就绪后再校验一次当前页面是否在权限范围内
				guardViewPath()
			} catch (e) {
				// 用户信息获取失败不影响已登录状态下的使用
			}
		}
	})

	/** 越权页面直接回首页，避免满屏 403 */
	const guardViewPath = () => {
		if (route.path === '/app/login') return
		if (!appStore.canVisit(route.path)) {
			router.replace('/app/home')
		}
	}

	watch(() => [appStore.view, route.path], guardViewPath, { immediate: true })

	const onSwitchView = (key) => {
		appStore.setView(key)
		viewOpen.value = false
		// 切视角后回到首页，避免停留在别的视角的页面
		if (!['/app/home', '/app/mine'].includes(route.path)) router.push('/app/home')
	}
</script>

<style lang="less" scoped>
	.m-app {
		display: flex;
		flex-direction: column;
		min-height: 100vh;
		background: #f2f3f5;
		/* 移动端安全区 */
		padding-bottom: env(safe-area-inset-bottom);
	}
	.m-header {
		position: sticky;
		top: 0;
		z-index: 20;
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 10px 14px;
		background: #fff;
		border-bottom: 1px solid var(--border-color-split);
	}
	.m-header-back {
		font-size: 18px;
		color: var(--text-color);
	}
	.m-header-main {
		flex: 1;
		min-width: 0;
	}
	.m-header-title {
		font-size: 16px;
		font-weight: 600;
		color: var(--text-color);
		line-height: 22px;
	}
	.m-header-sub {
		font-size: 12px;
		color: var(--text-color-secondary);
		line-height: 16px;
	}
	.m-header-right {
		display: flex;
		align-items: center;
		gap: 14px;
	}
	.m-header-btn {
		font-size: 18px;
		color: var(--text-color-secondary);
	}
	.m-body {
		flex: 1;
		padding: 12px 12px 76px;
	}
	.m-tabbar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 20;
		display: flex;
		background: #fff;
		border-top: 1px solid var(--border-color-split);
		padding-bottom: env(safe-area-inset-bottom);
	}
	.m-tab {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
		padding: 8px 0 6px;
		font-size: 11px;
		color: var(--text-color-secondary);
		.anticon {
			font-size: 18px;
		}
		&.active {
			color: var(--primary-6);
			font-weight: 500;
		}
	}
	.m-view-list {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}
	.m-view-item {
		padding: 12px;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-control);
		&.active {
			border-color: var(--primary-6);
			background: var(--primary-1);
		}
	}
	.m-view-name {
		font-size: 15px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-view-desc {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
</style>
