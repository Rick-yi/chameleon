/**
 * 移动端 App 状态（视角、用户信息）
 *
 * @author 会飞的猪
 * @date 2026/10/22
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import tool from '@/utils/tool'

/** 视角定义 */
export const APP_VIEWS = [
	{ key: 'all', name: '经营总览', desc: '校长 / 机构管理员' },
	{ key: 'teach', name: '教学', desc: '授课老师 / 校区教务' },
	{ key: 'enroll', name: '招生', desc: '招生顾问 / 市场' },
	{ key: 'cashier', name: '前台财务', desc: '前台 / 财务' }
]

/** 角色码 → 视角 */
const VIEW_BY_ROLE = {
	superAdmin: 'all',
	bizAdmin: 'all',
	principal: 'all',
	teacher: 'teach',
	teacherAdmin: 'teach',
	sales: 'enroll',
	salesAdmin: 'enroll',
	marketing: 'enroll',
	finance: 'cashier',
	cashier: 'cashier'
}

/** 视角 → 允许担任的角色码（用于按角色收敛「切换视角」选项） */
const VIEW_ROLES = {
	all: ['superAdmin', 'bizAdmin', 'principal'],
	teach: ['superAdmin', 'bizAdmin', 'principal', 'teacher', 'teacherAdmin', 'academic'],
	enroll: ['superAdmin', 'bizAdmin', 'principal', 'sales', 'salesAdmin', 'marketing'],
	cashier: ['superAdmin', 'bizAdmin', 'principal', 'cashier', 'finance']
}

export const useAppStore = defineStore('useAppStore', () => {
	/** 当前视角 */
	const view = ref(localStorage.getItem('APP_VIEW') || 'all')
	/** 用户信息 */
	const userInfo = ref(tool.data.get('USER_INFO') || {})
	/** 机构名 */
	const orgName = computed(() => userInfo.value?.orgName || '机构')

	const setView = (value) => {
		view.value = value
		localStorage.setItem('APP_VIEW', value)
	}

	/** 根据角色推导默认视角 */
	const defaultView = () => {
		const roleList = userInfo.value?.roleCodeList || []
		for (const role of roleList) {
			if (VIEW_BY_ROLE[role]) {
				return VIEW_BY_ROLE[role]
			}
		}
		return 'all'
	}

	/** 当前视角名称 */
	const viewName = computed(() => APP_VIEWS.find((item) => item.key === view.value)?.name || '经营总览')

	/** 各视角允许访问的页面前缀 */
	const VIEW_PATH_PREFIX = {
		teach: ['/app/home', '/app/teach', '/app/mine'],
		enroll: ['/app/home', '/app/enroll', '/app/teach/students', '/app/mine'],
		cashier: ['/app/home', '/app/cashier', '/app/report', '/app/mine'],
		all: ['/app']
	}

	/** 路径在当前视角下是否可访问 */
	const canVisit = (path) => {
		if (!path) return true
		const prefixList = VIEW_PATH_PREFIX[view.value] || VIEW_PATH_PREFIX.all
		return prefixList.some((prefix) => path === prefix || path.startsWith(prefix + '/'))
	}

	/** 当前账号可切换的视角（按角色收敛，避免切到没权限的视角后满屏 403） */
	const allowedViews = computed(() => {
		const roleList = userInfo.value?.roleCodeList || []
		const allowed = APP_VIEWS.filter((item) => (VIEW_ROLES[item.key] || []).some((role) => roleList.includes(role)))
		return allowed.length ? allowed : APP_VIEWS
	})

	/** 刷新用户信息 */
	const refreshUser = () => {
		userInfo.value = tool.data.get('USER_INFO') || {}
	}

	return { view, userInfo, orgName, viewName, allowedViews, canVisit, setView, defaultView, refreshUser }
})
