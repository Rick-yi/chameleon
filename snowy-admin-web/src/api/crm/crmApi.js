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

const request = (url, ...arg) => baseRequest(`/crm/` + url, ...arg)
/**
 * 招生域接口（线索/跟进/试听预约）
 *
 * @author 会飞的猪
 * @date 2026-10-20
 */
export default {
	// 线索
	leadPage(data) {
		return request('lead/page', data, 'get')
	},
	leadAdd(data) {
		return request('lead/add', data)
	},
	leadEdit(data) {
		return request('lead/edit', data)
	},
	leadDelete(data) {
		return request('lead/delete', data)
	},
	leadDetail(data) {
		return request('lead/detail', data, 'get')
	},
	// 跟进
	followPage(data) {
		return request('follow/page', data, 'get')
	},
	followAdd(data) {
		return request('follow/add', data)
	},
	// 试听
	trialPage(data) {
		return request('trial/page', data, 'get')
	},
	trialAdd(data) {
		return request('trial/add', data)
	},
	trialCancel(data) {
		return request('trial/cancel', data)
	}
}
