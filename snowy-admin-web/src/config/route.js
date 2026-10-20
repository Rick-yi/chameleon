/**
 *  Copyright [2022] [https://www.xiaonuo.vip]
 *	Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *	1.请不要删除和修改根目录下的LICENSE文件。
 *	2.请不要删除和修改Snowy源码头部的版权声明。
 *	3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 *	4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 *	5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 *	6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
// 静态路由配置
const routes = {
	// 默认模块，仅限于后端未添加任何单页配置，用此路由
	module: [
		{
			id: '01',
			name: 'homeModule',
			path: '/homeModule',
			component: '',
			meta: {
				title: '默认',
				type: 'module',
				icon: 'bank-outlined'
			},
			children: []
		}
	],
	// 默认谁都有个人中心
	menu: [
		{
			id: '001',
			name: 'usercenter',
			path: '/usercenter',
			component: 'sys/user/userCenter',
			meta: {
				title: '个人中心',
				type: 'menu',
				hidden: true
			}
		},
		{
			id: '002',
			name: 'crmLead',
			path: '/crm',
			component: 'crm/index',
			meta: {
				title: '招生线索',
				type: 'menu',
				icon: 'funnel-plot-outlined'
			}
		},
		{
			id: '003',
			name: 'courseIndex',
			path: '/course',
			component: 'course/index',
			meta: {
				title: '课程管理',
				type: 'menu',
				icon: 'book-outlined'
			}
		},
		{
			id: '004',
			name: 'coursePackage',
			path: '/course/package',
			component: 'course/package',
			meta: {
				title: '课包管理',
				type: 'menu',
				icon: 'gift-outlined'
			}
		},
		{
			id: '005',
			name: 'courseClass',
			path: '/course/class',
			component: 'course/class',
			meta: {
				title: '班级管理',
				type: 'menu',
				icon: 'team-outlined'
			}
		},
		{
			id: '006',
			name: 'courseSchedule',
			path: '/course/schedule',
			component: 'course/schedule',
			meta: {
				title: '排课课表',
				type: 'menu',
				icon: 'calendar-outlined'
			}
		},
		{
			id: '007',
			name: 'courseAttend',
			path: '/course/attend',
			component: 'course/attend',
			meta: {
				title: '点名消课',
				type: 'menu',
				icon: 'check-square-outlined'
			}
		},
		{
			id: '008',
			name: 'studentIndex',
			path: '/student',
			component: 'student/index',
			meta: {
				title: '学员管理',
				type: 'menu',
				icon: 'smile-outlined'
			}
		},
		{
			id: '009',
			name: 'studentDetail',
			path: '/student/detail',
			component: 'student/detail',
			meta: {
				title: '学员详情',
				type: 'menu',
				icon: 'profile-outlined',
				hidden: true
			}
		},
		{
			id: '010',
			name: 'renewWarn',
			path: '/student/renewWarn',
			component: 'student/renewWarn',
			meta: {
				title: '续费预警',
				type: 'menu',
				icon: 'bell-outlined'
			}
		},
		{
			id: '011',
			name: 'platformTenant',
			path: '/platform/tenant',
			component: 'platform/tenant',
			meta: {
				title: '租户管理',
				type: 'menu',
				icon: 'bank-outlined'
			}
		}
	]
}

export default routes
