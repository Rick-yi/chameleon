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

const request = (url, ...arg) => baseRequest(`/student/` + url, ...arg)
/**
 * 学员与课时账务接口
 *
 * @author 会飞的猪
 * @date 2026-10-20
 */
export default {
	// 学员
	studentPage(data) {
		return request('page', data, 'get')
	},
	studentAdd(data) {
		return request('add', data)
	},
	studentEdit(data) {
		return request('edit', data)
	},
	studentDelete(data) {
		return request('delete', data)
	},
	studentDetail(data) {
		return request('detail', data, 'get')
	},
	// 订单
	orderPage(data) {
		return request('order/page', data, 'get')
	},
	orderAdd(data) {
		return request('order/add', data)
	},
	orderPayCallback(data) {
		return request('order/payCallback', data)
	},
	orderOfflinePay(data) {
		return request('order/offlinePay', data)
	},
	orderCancel(data) {
		return request('order/cancel', data)
	},
	// 续费（下单 + 收款一次完成）
	orderRenew(data) {
		return request('order/renew', data)
	},
	// 课时账户
	lessonAccountPage(data) {
		return request('lessonAccount/page', data, 'get')
	},
	// 人工调整课时
	lessonAccountAdjust(data) {
		return request('lessonAccount/adjust', data)
	},
	// 课时流水
	lessonFlowPage(data) {
		return request('lessonFlow/page', data, 'get')
	},
	// 课时余额（批量：studentIds 逗号分隔 + courseId）
	lessonBalance(data) {
		return request('lessonAccount/balance', data, 'get')
	},
	// 退费
	refundPage(data) {
		return request('refund/page', data, 'get')
	},
	refundApply(data) {
		return request('refund/apply', data)
	},
	refundApprove(data) {
		return request('refund/approve', data)
	},
	refundConfirm(data) {
		return request('refund/confirm', data)
	},
	refundCancel(data) {
		return request('refund/cancel', data)
	},
	// 续费预警
	renewWarnPage(data) {
		return request('renewWarn/page', data, 'get')
	},
	renewWarnScan() {
		return request('renewWarn/scan', null)
	},
	renewWarnIgnore(data) {
		return request('renewWarn/ignore', data)
	}
}
