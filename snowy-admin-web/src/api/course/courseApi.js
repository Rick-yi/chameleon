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

const request = (url, ...arg) => baseRequest(`/course/` + url, ...arg)
/**
 * 课程域接口（课程大类/课程/课包/班级/排课/点名）
 *
 * @author 会飞的猪
 * @date 2026-10-20
 */
export default {
	// 课程大类
	categoryList() {
		return request('category/list', null, 'get')
	},
	categoryAdd(data) {
		return request('category/add', data)
	},
	// 课程
	coursePage(data) {
		return request('page', data, 'get')
	},
	courseAdd(data) {
		return request('add', data)
	},
	courseEdit(data) {
		return request('edit', data)
	},
	courseDelete(data) {
		return request('delete', data)
	},
	// 课包
	packagePage(data) {
		return request('package/page', data, 'get')
	},
	packageAdd(data) {
		return request('package/add', data)
	},
	packageEdit(data) {
		return request('package/edit', data)
	},
	packageDelete(data) {
		return request('package/delete', data)
	},
	// 班级
	classPage(data) {
		return request('class/page', data, 'get')
	},
	classAdd(data) {
		return request('class/add', data)
	},
	classEdit(data) {
		return request('class/edit', data)
	},
	classDelete(data) {
		return request('class/delete', data)
	},
	// 校区可选项
	campusOptions() {
		return request('class/campusOptions', null, 'get')
	},
	// 班级学员
	classStudentPage(data) {
		return request('classStudent/page', data, 'get')
	},
	classStudentAdd(data) {
		return request('classStudent/add', data)
	},
	classStudentDelete(data) {
		return request('classStudent/delete', data)
	},
	// 排课
	schedulePage(data) {
		return request('schedule/page', data, 'get')
	},
	scheduleAdd(data) {
		return request('schedule/add', data)
	},
	scheduleGenerate(data) {
		return request('schedule/generate', data)
	},
	scheduleChange(data) {
		return request('schedule/change', data)
	},
	scheduleCancel(data) {
		return request('schedule/cancel', data)
	},
	// 批量取消排课（已点名的自动跳过）
	scheduleCancelBatch(data) {
		return request('schedule/cancelBatch', data)
	},
	// 课表区间查询（不分页）
	// 推荐可排课时段（排课助手）
	scheduleSuggest(data) {
		return request('schedule/suggest', data, 'get')
	},
	scheduleWeek(data) {
		return request('schedule/week', data, 'get')
	},
	// 教师可选项
	teacherOptions(data) {
		return request('schedule/teacherOptions', data, 'get')
	},
	// 导出课表（Excel：课表清单 + 周视图）
	scheduleExport(data) {
		return request('schedule/export', data, 'get', { responseType: 'blob' })
	},
	// 直接排课（不挂班级：老师带一群学生上课）
	scheduleGenerateAdhoc(data) {
		return request('schedule/generateAdhoc', data)
	},
	// 调课记录
	scheduleChangePage(data) {
		return request('scheduleChange/page', data, 'get')
	},
	// 点名
	attendPage(data) {
		return request('attend/page', data, 'get')
	},
	// 学员出勤记录（请假 / 缺勤 / 到课）
	studentAttendPage(data) {
		return request('attend/page', data, 'get')
	},
	attendConfirm(data) {
		return request('attend/confirm', data)
	},
	attendCancel(data) {
		return request('attend/cancel', data)
	}
}
