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
package vip.xiaonuo.platform.modular.event.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.auth.core.pojo.SaBaseLoginUser;
import vip.xiaonuo.auth.core.util.StpLoginUserUtil;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.platform.modular.event.dto.BizEventSummaryDTO;
import vip.xiaonuo.platform.modular.event.entity.BizEvent;
import vip.xiaonuo.platform.modular.event.mapper.BizEventMapper;
import vip.xiaonuo.platform.modular.event.param.BizEventQueryParam;
import vip.xiaonuo.platform.modular.event.service.BizEventService;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务埋点Service实现
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Slf4j
@Service
public class BizEventServiceImpl extends ServiceImpl<BizEventMapper, BizEvent> implements BizEventService {

	/** 埋点只用于观测，任何异常都不允许影响主流程 */
	@Override
	public void record(String eventCode, String targetType, String targetId, String targetName, Map<String, Object> props) {
		try {
			BizEvent event = new BizEvent();
			event.setTenantId(TenantContext.getTenantId());
			event.setEventCode(eventCode);
			event.setEventName(BizEventCodeEnum.messageOf(eventCode));
			event.setTargetType(StrUtil.blankToDefault(targetType, "unknown"));
			event.setTargetId(targetId);
			event.setTargetName(targetName);
			if (ObjectUtil.isNotEmpty(props)) {
				String json = JSONUtil.toJsonStr(props);
				event.setProps(json.length() > 1000 ? json.substring(0, 1000) : json);
			}
			// 操作人（未登录场景忽略）
			try {
				SaBaseLoginUser loginUser = StpLoginUserUtil.getLoginUser();
				if (loginUser != null) {
					event.setUserId(loginUser.getId());
					event.setUserName(loginUser.getName());
				}
			} catch (Exception ignore) {
				// 未登录或会话失效时不记录操作人
			}
			event.setEventTime(new Date());
			this.save(event);
		} catch (Exception e) {
			log.warn(">>> 业务埋点记录失败：{}，原因：{}", eventCode, e.getMessage());
		}
	}

	@Override
	public Page<BizEvent> page(BizEventQueryParam bizEventQueryParam) {
		QueryWrapper<BizEvent> queryWrapper = new QueryWrapper<BizEvent>().checkSqlInjection();
		queryWrapper.lambda()
				.eq(StrUtil.isNotBlank(bizEventQueryParam.getEventCode()), BizEvent::getEventCode, bizEventQueryParam.getEventCode())
				.eq(StrUtil.isNotBlank(bizEventQueryParam.getTargetType()), BizEvent::getTargetType, bizEventQueryParam.getTargetType())
				.eq(StrUtil.isNotBlank(bizEventQueryParam.getTargetId()), BizEvent::getTargetId, bizEventQueryParam.getTargetId())
				.and(StrUtil.isNotBlank(bizEventQueryParam.getSearchKey()),
						q -> q.like(BizEvent::getTargetName, bizEventQueryParam.getSearchKey())
								.or().like(BizEvent::getUserName, bizEventQueryParam.getSearchKey()))
				.orderByDesc(BizEvent::getEventTime);
		return this.page(CommonPageRequest.defaultPage(), queryWrapper);
	}

	@Override
	public BizEventSummaryDTO summary(BizEventQueryParam bizEventQueryParam) {
		int days = ObjectUtil.defaultIfNull(bizEventQueryParam.getDays(), 30);
		Date startTime = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -(days - 1)));
		Date endTime = DateUtil.endOfDay(new Date());
		List<BizEvent> eventList = this.list(new LambdaQueryWrapper<BizEvent>()
				.between(BizEvent::getEventTime, startTime, endTime)
				.orderByAsc(BizEvent::getEventTime));

		BizEventSummaryDTO summary = new BizEventSummaryDTO();

		// 1. 事件计数
		Map<String, Long> countMap = new LinkedHashMap<>();
		Map<String, Long> dateCountMap = new LinkedHashMap<>();
		for (BizEvent event : eventList) {
			countMap.merge(event.getEventCode(), 1L, Long::sum);
			String date = event.getEventTime() == null ? "" : DateUtil.formatDate(event.getEventTime());
			dateCountMap.merge(date + "|" + event.getEventCode(), 1L, Long::sum);
		}
		for (BizEventCodeEnum codeEnum : BizEventCodeEnum.values()) {
			Long count = countMap.getOrDefault(codeEnum.getValue(), 0L);
			summary.getEventCountList().add(new BizEventSummaryDTO.Item(codeEnum.getValue(), codeEnum.getMessage(), count));
		}

		// 2. 业务漏斗：招生 → 报名收款
		List<String[]> funnelNodes = List.of(
				new String[]{"线索创建", BizEventCodeEnum.LEAD_CREATE.getValue()},
				new String[]{"线索跟进", BizEventCodeEnum.LEAD_FOLLOW.getValue()},
				new String[]{"试听预约", BizEventCodeEnum.TRIAL_BOOK.getValue()},
				new String[]{"转学员", BizEventCodeEnum.LEAD_CONVERT.getValue()},
				new String[]{"报名开单", BizEventCodeEnum.ORDER_CREATE.getValue()},
				new String[]{"收款成功", BizEventCodeEnum.ORDER_PAY.getValue()});
		Long previous = null;
		for (String[] node : funnelNodes) {
			Long count = countMap.getOrDefault(node[1], 0L);
			Double rate = null;
			// 上一节点为 0 时转化率无意义（留空展示），避免出现 0% 的误导
			if (previous != null && previous > 0) {
				rate = Math.round(count * 10000.0 / previous) / 100.0;
			}
			summary.getFunnelList().add(new BizEventSummaryDTO.Funnel(node[0], node[1], count, rate));
			previous = count;
		}

		// 3. 按天趋势（只保留有数据的日期）
		List<BizEventSummaryDTO.Trend> trendList = new ArrayList<>();
		for (Map.Entry<String, Long> entry : dateCountMap.entrySet()) {
			String[] parts = entry.getKey().split("\\|");
			trendList.add(new BizEventSummaryDTO.Trend(parts[0], parts[1], entry.getValue()));
		}
		summary.setTrendList(trendList);
		return summary;
	}
}
