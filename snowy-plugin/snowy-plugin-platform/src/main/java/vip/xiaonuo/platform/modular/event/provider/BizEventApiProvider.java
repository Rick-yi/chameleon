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
package vip.xiaonuo.platform.modular.event.provider;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.modular.event.service.BizEventService;

import java.util.Map;

/**
 * 业务埋点API实现
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class BizEventApiProvider implements BizEventApi {

	@Resource
	private BizEventService bizEventService;

	@Override
	public void log(String eventCode, String targetType, String targetId, String targetName, Map<String, Object> props) {
		bizEventService.record(eventCode, targetType, targetId, targetName, props);
	}
}
