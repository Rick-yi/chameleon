<template>
	<div>
		<div class="m-card m-profile">
			<a-avatar :size="48" style="background: var(--primary-1); color: var(--primary-6)">
				{{ (userInfo?.name || '?').slice(0, 1) }}
			</a-avatar>
			<div class="m-profile-main">
				<div class="m-profile-name">{{ userInfo?.name || '未登录' }}</div>
				<div class="m-profile-sub">
					{{ userInfo?.account || '' }} · {{ appStore.orgName }}
				</div>
			</div>
		</div>

		<div class="m-section-title">当前视角</div>
		<div class="m-card">
			<div
				v-for="item in appStore.allowedViews"
				:key="item.key"
				class="m-view-row"
				:class="{ active: appStore.view === item.key }"
				@click="onSwitch(item.key)"
			>
				<div>
					<div class="m-view-name">{{ item.name }}</div>
					<div class="m-view-desc">{{ item.desc }}</div>
				</div>
				<a-icon v-if="appStore.view === item.key" style="color: var(--primary-6)"><check-outlined /></a-icon>
			</div>
		</div>

		<div class="m-section-title">消息</div>
		<div class="m-card">
			<div class="m-about-row m-clickable" @click="router.push('/app/message')">
				<span>消息中心</span>
				<span :class="{ 'm-unread': unreadCount > 0 }">
					{{ unreadCount > 0 ? unreadCount + ' 条未读' : '暂无未读' }}
				</span>
			</div>
		</div>

		<div class="m-section-title">关于</div>
		<div class="m-card">
			<div class="m-about-row">
				<span>版本</span>
				<span>{{ version }}</span>
			</div>
			<div class="m-about-row">
				<span>机构</span>
				<span>{{ appStore.orgName }}</span>
			</div>
		</div>

		<a-button block danger style="margin-top: 20px" @click="onLogout">退出登录</a-button>
	</div>
</template>

<script setup name="appMine">
	import { useRouter } from 'vue-router'
	import { message } from 'ant-design-vue'
	import { useAppStore, APP_VIEWS } from '@/store/app'
	import { globalStore } from '@/store'
	import indexApi from '@/api/sys/indexApi'
	import tool from '@/utils/tool'

	const router = useRouter()
	const appStore = useAppStore()
	const views = APP_VIEWS
	const userInfo = computed(() => appStore.userInfo)
	const version = computed(() => globalStore().sysBaseConfig?.SNOWY_SYS_VERSION || '3.0.0')
	const unreadCount = ref(0)

	// 进入「我的」时刷新未读数，避免角标长期不更新
	onMounted(() => {
		indexApi.indexMessageUnreadCount().then((data) => {
			unreadCount.value = Number(data || 0)
		})
	})

	const onSwitch = (key) => {
		appStore.setView(key)
		message.success(`已切换到「${APP_VIEWS.find((item) => item.key === key).name}」视角`)
	}

	const onLogout = () => {
		tool.data.remove('TOKEN')
		tool.data.remove('USER_INFO')
		tool.data.remove('MENU')
		appStore.setView('all')
		router.push('/app/login')
	}
</script>

<style lang="less" scoped>
	.m-card {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 14px;
	}
	.m-profile {
		display: flex;
		align-items: center;
		gap: 12px;
	}
	.m-profile-name {
		font-size: 17px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-profile-sub {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-section-title {
		margin: 16px 2px 8px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-view-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 10px 0;
		border-bottom: 1px solid var(--border-color-split);
		&:last-child {
			border-bottom: none;
		}
	}
	.m-view-name {
		font-size: 15px;
		color: var(--text-color);
	}
	.m-view-desc {
		margin-top: 2px;
		font-size: 12px;
		color: var(--text-color-secondary);
	}
	.m-about-row {
		display: flex;
		justify-content: space-between;
		padding: 6px 0;
		font-size: 14px;
		color: var(--text-color-secondary);
	}

	.m-clickable {
		cursor: pointer;
		&:active {
			opacity: 0.7;
		}
	}

	.m-unread {
		color: var(--primary-6);
		font-weight: 500;
	}
</style>
