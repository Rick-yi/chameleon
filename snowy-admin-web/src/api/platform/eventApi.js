/**
 *  Copyright [2022] [https://www.xiaonuo.vip]
 *	Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *	1.请不要删除和修改根目录下的LICENSE文件。
 *	2.请不要删除和修改Snowy源码头部的版权声明。
 *	3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 *	4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 *	5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@163.com商议合作。
 *	6.如需更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为：https://www.xiaonuo.vip
 */
import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/biz/event/` + url, ...arg)

/**
 * 业务埋点接口（页面访问 / 事件流水 / 统计）
 *
 * @author 会飞的猪
 * @date 2026-10-21
 */
export default {
	// 上报行为埋点
	track(data) {
		return request('track', data)
	},
	// 埋点流水分页
	eventPage(data) {
		return request('page', data, 'get')
	},
	// 埋点统计（事件计数 / 漏斗 / 趋势）
	summary(data) {
		return request('summary', data, 'get')
	},
	// 事件码字典
	codes() {
		return request('codes', null, 'get')
	}
}
