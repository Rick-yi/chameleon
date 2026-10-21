/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时需要注意以下几点：
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@163.com商议合作。
 * 6.如需更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为：https://www.xiaonuo.vip
 */
package vip.xiaonuo.platform.api;

import java.util.Map;

/**
 * 业务埋点API（跨插件调用）
 * <p>
 * 业务域在关键动作上打点，平台域统一落库与统计。埋点失败不影响主流程。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public interface BizEventApi {

	/**
	 * 记录一条业务埋点
	 *
	 * @param eventCode   事件码（见 BizEventCodeEnum）
	 * @param targetType  对象类型（student/lead/order/course/class/schedule/page）
	 * @param targetId    对象id
	 * @param targetName  对象名称
	 * @param props       业务属性（可空）
	 */
	void log(String eventCode, String targetType, String targetId, String targetName, Map<String, Object> props);

	/**
	 * 记录一条业务埋点（无属性）
	 *
	 * @param eventCode  事件码
	 * @param targetType 对象类型
	 * @param targetId   对象id
	 * @param targetName 对象名称
	 */
	default void log(String eventCode, String targetType, String targetId, String targetName) {
		log(eventCode, targetType, targetId, targetName, null);
	}
}
