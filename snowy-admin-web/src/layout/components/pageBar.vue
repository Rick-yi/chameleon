<template>
	<div v-if="show" class="ent-page-bar">
		<div class="ent-page-bar-main">
			<span class="ent-page-bar-title">{{ title }}</span>
			<span v-if="desc" class="ent-page-bar-desc">{{ desc }}</span>
		</div>
		<div class="ent-page-bar-extra">
			<slot name="extra" />
		</div>
	</div>
</template>

<script setup name="pageBar">
	import { useRoute } from 'vue-router'
	const route = useRoute()

	// 已有自定义页头的业务页不再重复渲染全局页头
	const CUSTOM_HEADER_PATHS = [
		'/workbench',
		'/student',
		'/student/order',
		'/student/renewWarn',
		'/student/detail',
		'/crm',
		'/course',
		'/course/package',
		'/course/class',
		'/course/schedule',
		'/course/attend',
		'/report/dashboard',
		'/report/finance',
		'/report/academic',
		'/report/crm',
		'/report/eventAnalysis'
	]
	// 注意：新增使用 <page-header> 的业务页，需把路径补进上面的列表，否则会出现双标题

	// 页面说明（上游页面的补充文案）
	const DESC_MAP = {
		'/sys/user': '系统用户账号、状态与角色授权',
		'/sys/role': '角色、菜单、接口权限与数据范围',
		'/sys/org': '组织架构：总部与校区',
		'/sys/position': '职位与人员归属',
		'/sys/resource/menu': '菜单、按钮资源与授权',
		'/sys/userCenter': '个人资料、账号安全与登录记录',
		'/biz/user': '业务端用户账号',
		'/biz/org': '业务组织架构',
		'/biz/dict': '业务字典维护',
		'/biz/notice': '站内通知与公告',
		'/biz/position': '业务职位维护',
		'/biz/group': '业务用户分组',
		'/platform/tenant': '租户开通、套餐与到期管理',
		'/platform/package': '套餐、限额与功能开关',
		'/platform/dashboard': '平台侧租户经营概览'
	}

	const title = computed(() => route.meta?.title || '')
	const desc = computed(() => DESC_MAP[route.path] || '')
	const show = computed(() => {
		if (!title.value) return false
		if (route.path === '/' || route.path === '/index') return false
		return !CUSTOM_HEADER_PATHS.some((path) => route.path === path || route.path.startsWith(path + '/'))
	})
</script>
