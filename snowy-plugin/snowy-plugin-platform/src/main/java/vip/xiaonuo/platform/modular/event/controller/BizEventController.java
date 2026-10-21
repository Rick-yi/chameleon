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
package vip.xiaonuo.platform.modular.event.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.platform.modular.event.dto.BizEventSummaryDTO;
import vip.xiaonuo.platform.modular.event.entity.BizEvent;
import vip.xiaonuo.platform.modular.event.param.BizEventQueryParam;
import vip.xiaonuo.platform.modular.event.service.BizEventService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务埋点控制器
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Tag(name = "业务埋点控制器")
@RestController
public class BizEventController {

	@Resource
	private BizEventService bizEventService;

	/**
	 * 前端行为埋点（页面访问 / 功能点击）
	 *
	 * @author 会飞的猪
	 * @date 2026/10/21 10:00
	 */
	@Operation(summary = "上报行为埋点")
	@PostMapping("/biz/event/track")
	public CommonResult<String> track(@RequestBody Map<String, Object> body) {
		String eventCode = body.get("eventCode") == null ? BizEventCodeEnum.FEATURE_CLICK.getValue() : String.valueOf(body.get("eventCode"));
		String targetType = body.get("targetType") == null ? "page" : String.valueOf(body.get("targetType"));
		String targetId = body.get("targetId") == null ? null : String.valueOf(body.get("targetId"));
		String targetName = body.get("targetName") == null ? null : String.valueOf(body.get("targetName"));
		Map<String, Object> props = new HashMap<>();
		Object path = body.get("path");
		if (path != null) {
			props.put("path", String.valueOf(path));
		}
		bizEventService.record(eventCode, targetType, targetId, targetName, props);
		return CommonResult.ok();
	}

	/**
	 * 埋点流水分页
	 *
	 * @author 会飞的猪
	 * @date 2026/10/21 10:00
	 */
	@Operation(summary = "获取埋点流水分页")
	@GetMapping("/biz/event/page")
	public CommonResult<Page<BizEvent>> page(@Valid BizEventQueryParam bizEventQueryParam) {
		return CommonResult.data(bizEventService.page(bizEventQueryParam));
	}

	/**
	 * 埋点统计（事件计数 / 漏斗 / 趋势）
	 *
	 * @author 会飞的猪
	 * @date 2026/10/21 10:00
	 */
	@Operation(summary = "获取埋点统计")
	@GetMapping("/biz/event/summary")
	public CommonResult<BizEventSummaryDTO> summary(@RequestParam(required = false) Integer days) {
		BizEventQueryParam param = new BizEventQueryParam();
		param.setDays(days);
		return CommonResult.data(bizEventService.summary(param));
	}

	/**
	 * 事件码字典
	 *
	 * @author 会飞的猪
	 * @date 2026/10/21 10:00
	 */
	@Operation(summary = "获取事件码字典")
	@GetMapping("/biz/event/codes")
	public CommonResult<List<Map<String, String>>> codes() {
		List<Map<String, String>> list = new java.util.ArrayList<>();
		for (BizEventCodeEnum item : BizEventCodeEnum.values()) {
			Map<String, String> map = new HashMap<>();
			map.put("value", item.getValue());
			map.put("label", item.getMessage());
			list.add(map);
		}
		return CommonResult.data(list);
	}
}
