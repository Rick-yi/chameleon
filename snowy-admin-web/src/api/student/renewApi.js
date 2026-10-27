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

const request = (url, ...arg) => baseRequest(`/student/renewWarn/` + url, ...arg)

/**
 * 续费与流失闭环接口
 *
 * @author 会飞的猪
 * @date 2026-10-26
 */
export default {
	// 预警列表
	renewWarnPage(data) {
		return request('page', data, 'get')
	},
	renewWarnScan() {
		return request('scan', null)
	},
	renewWarnIgnore(data) {
		return request('ignore', data)
	},
	// 行内动作
	renewWarnNotify(data) {
		return request('notify', data)
	},
	renewWarnAssign(data) {
		return request('assign', data)
	},
	renewWarnProcess(data) {
		return request('process', data)
	},
	renewWarnAssigneeOptions(data) {
		return request('assigneeOptions', data, 'get')
	},
	// 阈值配置
	renewWarnConfigList() {
		return request('config/list', null, 'get')
	},
	renewWarnConfigSave(data) {
		return request('config/save', data)
	},
	renewWarnConfigDelete(data) {
		return request('config/delete', data)
	},
	// 风险学员
	renewWarnRiskPage(data) {
		return request('risk/page', data, 'get')
	},
	renewWarnRiskTodo(data) {
		return request('risk/todo', data)
	},
	// 回访/流失记录
	renewWarnFollowAdd(data) {
		return request('follow/add', data)
	},
	renewWarnFollowPage(data) {
		return request('follow/page', data, 'get')
	},
	// 续费率/流失率
	renewWarnStats(data) {
		return request('stats', data, 'get')
	}
}
