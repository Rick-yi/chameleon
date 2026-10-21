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
import { baseRequest } from '@/utils/request'

const financeRequest = (url, ...arg) => baseRequest(`/finance/report/` + url, ...arg)
const academicRequest = (url, ...arg) => baseRequest(`/course/report/academic/` + url, ...arg)
const crmRequest = (url, ...arg) => baseRequest(`/crm/report/` + url, ...arg)

/**
 * 报表接口（财务/教务/招生）
 *
 * @author 会飞的猪
 * @date 2026-10-20
 */
export default {
	// 财务
	dashboard(data) {
		return financeRequest('dashboard', data, 'get')
	},
	summary(data) {
		return financeRequest('summary', data, 'get')
	},
	trend(data) {
		return financeRequest('trend', data, 'get')
	},
	channel(data) {
		return financeRequest('channel', data, 'get')
	},
	courseRank(data) {
		return financeRequest('courseRank', data, 'get')
	},
	campusRank(data) {
		return financeRequest('campusRank', data, 'get')
	},
	attendTrend(data) {
		return financeRequest('attendTrend', data, 'get')
	},
	campusOptions() {
		return financeRequest('campusOptions', null, 'get')
	},
	exportReport(data) {
		return financeRequest('export', data, 'get', {
			responseType: 'blob'
		})
	},
	exportAsync(data) {
		return financeRequest('export/async', data, 'get')
	},
	exportAsyncStatus(data) {
		return financeRequest('export/async/status', data, 'get')
	},
	exportAsyncDownload(data) {
		return financeRequest('export/async/download', data, 'get', {
			responseType: 'blob'
		})
	},
	consume(data) {
		return financeRequest('consume', data, 'get')
	},
	dailyStat(data) {
		return financeRequest('dailyStat', data, 'get')
	},
	rebuildDailyStat(data) {
		return financeRequest('dailyStat/rebuild', data, 'post')
	},
	reconcile(data) {
		return financeRequest('reconcile', data, 'get')
	},
	clearCache() {
		return financeRequest('cache/clear', null, 'post')
	},
	// 教务
	academicAttendance(data) {
		return academicRequest('attendance', data, 'get')
	},
	academicClassFill(data) {
		return academicRequest('classFill', data, 'get')
	},
	academicTeacherWorkload(data) {
		return academicRequest('teacherWorkload', data, 'get')
	},
	academicLessonProgress(data) {
		return academicRequest('lessonProgress', data, 'get')
	},
	academicRiskStudents(data) {
		return academicRequest('riskStudents', data, 'get')
	},
	academicExport(data) {
		return academicRequest('export', data, 'get', {
			responseType: 'blob'
		})
	},
	// 招生
	crmFunnel(data) {
		return crmRequest('funnel', data, 'get')
	},
	crmSource(data) {
		return crmRequest('source', data, 'get')
	},
	crmFollowRank(data) {
		return crmRequest('followRank', data, 'get')
	},
	crmTrial(data) {
		return crmRequest('trial', data, 'get')
	},
	crmExport(data) {
		return crmRequest('export', data, 'get', {
			responseType: 'blob'
		})
	}
}
