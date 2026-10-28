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
import request from '@/utils/request'

/**
 * 学杂费接口
 * 与课包订单分账：不产生课时、不参与课消
 */
export default {
	// 分页查询
	page(data) {
		return request('sundry/page', data, 'get')
	},
	// 某学员的全部学杂费
	listByStudent(studentId) {
		return request('sundry/listByStudent', { studentId }, 'get')
	},
	// 学员学杂费汇总（应收/已收/已退）
	summary(studentId) {
		return request('sundry/summary', { studentId }, 'get')
	},
	// 新增
	add(data) {
		return request('sundry/add', data)
	},
	// 收款
	pay(data) {
		return request('sundry/pay', data)
	},
	// 退费
	refund(data) {
		return request('sundry/refund', data)
	},
	// 取消
	cancel(id) {
		return request('sundry/cancel', { id })
	},
	// 删除
	del(id) {
		return request('sundry/delete', { id })
	}
}
