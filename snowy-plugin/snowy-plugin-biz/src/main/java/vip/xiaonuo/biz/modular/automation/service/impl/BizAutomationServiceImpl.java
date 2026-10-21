/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.biz.modular.automation.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.biz.api.automation.BizAutomationRuleDTO;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationConfig;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationLog;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationConfigMapper;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationLogMapper;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationLogPageParam;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationToggleParam;
import vip.xiaonuo.biz.modular.automation.service.BizAutomationService;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化规则Service接口实现类
 * <p>
 * 规则由各业务插件实现 {@link BizAutomationHandler} 并注册为 Spring Bean，
 * 这里统一负责：开关与参数存储、立即执行、定时执行与执行记录。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Service
public class BizAutomationServiceImpl extends ServiceImpl<BizAutomationConfigMapper, BizAutomationConfig> implements BizAutomationService {

    /** 执行方式：定时 */
    public static final String TRIGGER_TYPE_TIMER = "TIMER";
    /** 执行方式：手动（自动化中心点「立即执行」） */
    public static final String TRIGGER_TYPE_MANUAL = "MANUAL";
    /** 执行方式：业务实时触发 */
    public static final String TRIGGER_TYPE_TRIGGER = "TRIGGER";
    /** 执行模式：定时执行 */
    public static final String MODE_TIMER = "TIMER";
    /** 执行模式：业务触发时实时执行 */
    public static final String MODE_TRIGGER = "TRIGGER";
    /** 系统操作人 */
    public static final String SYSTEM_USER = "-1";

    @Resource
    private BizAutomationLogMapper bizAutomationLogMapper;

    /** 各业务插件注册的自动化规则 */
    private final List<BizAutomationHandler> handlerList;

    public BizAutomationServiceImpl(List<BizAutomationHandler> handlerList) {
        this.handlerList = handlerList == null ? List.of() : handlerList;
    }

    @Override
    public List<BizAutomationRuleDTO> ruleList() {
        Map<String, BizAutomationConfig> configMap = new HashMap<>();
        for (BizAutomationConfig config : this.list(new LambdaQueryWrapper<BizAutomationConfig>())) {
            configMap.put(config.getRuleCode(), config);
        }
        List<BizAutomationRuleDTO> resultList = new ArrayList<>();
        for (BizAutomationHandler handler : handlerList) {
            BizAutomationRuleDTO dto = new BizAutomationRuleDTO();
            dto.setRuleCode(handler.getRuleCode());
            dto.setRuleName(handler.getRuleName());
            dto.setRuleDesc(handler.getRuleDesc());
            dto.setMode(handler.getMode());
            dto.setParamDef(handler.getParamDef());
            BizAutomationConfig config = configMap.get(handler.getRuleCode());
            boolean enabled = config != null && StrUtil.isNotBlank(config.getEnabled())
                    ? "YES".equalsIgnoreCase(config.getEnabled()) : handler.defaultEnabled();
            dto.setEnabled(enabled);
            dto.setParams(mergeParams(handler, config));
            // 最近一次执行情况
            BizAutomationLog lastLog = bizAutomationLogMapper.selectOne(new LambdaQueryWrapper<BizAutomationLog>()
                    .eq(BizAutomationLog::getRuleCode, handler.getRuleCode())
                    .orderByDesc(BizAutomationLog::getCreateTime)
                    .last("limit 1"));
            if (lastLog != null) {
                dto.setLastRunTime(lastLog.getCreateTime() == null ? null
                        : cn.hutool.core.date.DateUtil.formatDateTime(lastLog.getCreateTime()));
                dto.setLastRunMessage(lastLog.getMessage());
                dto.setLastRunSuccess("YES".equalsIgnoreCase(lastLog.getSuccess()));
            }
            Long totalCount = bizAutomationLogMapper.selectCount(new LambdaQueryWrapper<BizAutomationLog>()
                    .eq(BizAutomationLog::getRuleCode, handler.getRuleCode()));
            dto.setTotalRunCount(totalCount);
            resultList.add(dto);
        }
        return resultList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggle(BizAutomationToggleParam bizAutomationToggleParam) {
        BizAutomationHandler handler = getHandler(bizAutomationToggleParam.getRuleCode());
        if (handler == null) {
            throw new CommonException("自动化规则不存在：{}", bizAutomationToggleParam.getRuleCode());
        }
        BizAutomationConfig config = this.getOne(new LambdaQueryWrapper<BizAutomationConfig>()
                .eq(BizAutomationConfig::getRuleCode, bizAutomationToggleParam.getRuleCode()), false);
        if (config == null) {
            config = new BizAutomationConfig();
            config.setRuleCode(handler.getRuleCode());
            config.setTenantId(TenantContext.getTenantId());
        }
        config.setEnabled("YES".equalsIgnoreCase(bizAutomationToggleParam.getEnabled()) ? "YES" : "NO");
        config.setParams(bizAutomationToggleParam.getParams() == null ? null
                : JSONUtil.toJsonStr(bizAutomationToggleParam.getParams()));
        this.saveOrUpdate(config);
    }

    @Override
    public BizAutomationHandler.AutomationResult run(String ruleCode, String triggerType) {
        BizAutomationHandler handler = getHandler(ruleCode);
        if (handler == null) {
            throw new CommonException("自动化规则不存在：{}", ruleCode);
        }
        long start = System.currentTimeMillis();
        BizAutomationHandler.AutomationResult result;
        try {
            BizAutomationHandler.AutomationContext context = new BizAutomationHandler.AutomationContext(
                    getParams(ruleCode), triggerType, currentOperatorId());
            result = handler.execute(context);
        } catch (Exception e) {
            log.error(">>> 自动化规则[{}]执行失败", ruleCode, e);
            result = BizAutomationHandler.AutomationResult.fail("执行失败：" + e.getMessage());
        }
        // 执行记录
        BizAutomationLog automationLog = new BizAutomationLog();
        automationLog.setTenantId(TenantContext.getTenantId());
        automationLog.setRuleCode(ruleCode);
        automationLog.setRuleName(handler.getRuleName());
        automationLog.setTriggerType(triggerType);
        automationLog.setSuccess(result.success() ? "YES" : "NO");
        automationLog.setMessage(StrUtil.maxLength(result.message(), 480));
        automationLog.setAffectedCount(result.affectedCount());
        automationLog.setDetail(result.detail());
        automationLog.setCostMs(System.currentTimeMillis() - start);
        bizAutomationLogMapper.insert(automationLog);
        return result;
    }

    @Override
    public Page<BizAutomationLog> logPage(BizAutomationLogPageParam bizAutomationLogPageParam) {
        LambdaQueryWrapper<BizAutomationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(bizAutomationLogPageParam.getRuleCode())) {
            queryWrapper.eq(BizAutomationLog::getRuleCode, bizAutomationLogPageParam.getRuleCode());
        }
        if (StrUtil.isNotBlank(bizAutomationLogPageParam.getTriggerType())) {
            queryWrapper.eq(BizAutomationLog::getTriggerType, bizAutomationLogPageParam.getTriggerType());
        }
        if (StrUtil.isNotBlank(bizAutomationLogPageParam.getSuccess())) {
            queryWrapper.eq(BizAutomationLog::getSuccess, bizAutomationLogPageParam.getSuccess());
        }
        queryWrapper.orderByDesc(BizAutomationLog::getCreateTime);
        return bizAutomationLogMapper.selectPage(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Override
    public boolean isEnabled(String ruleCode) {
        BizAutomationConfig config = this.getOne(new LambdaQueryWrapper<BizAutomationConfig>()
                .eq(BizAutomationConfig::getRuleCode, ruleCode), false);
        if (config == null) {
            BizAutomationHandler handler = getHandler(ruleCode);
            return handler != null && handler.defaultEnabled();
        }
        return "YES".equalsIgnoreCase(config.getEnabled());
    }

    @Override
    public Map<String, String> getParams(String ruleCode) {
        BizAutomationHandler handler = getHandler(ruleCode);
        BizAutomationConfig config = this.getOne(new LambdaQueryWrapper<BizAutomationConfig>()
                .eq(BizAutomationConfig::getRuleCode, ruleCode), false);
        return mergeParams(handler, config);
    }

    @Override
    public int runTimerRules() {
        int count = 0;
        for (BizAutomationHandler handler : handlerList) {
            if (!MODE_TIMER.equals(handler.getMode())) {
                continue;
            }
            if (!isEnabled(handler.getRuleCode())) {
                continue;
            }
            run(handler.getRuleCode(), TRIGGER_TYPE_TIMER);
            count++;
        }
        return count;
    }

    /** 合并规则默认参数与租户自定义参数 */
    private Map<String, String> mergeParams(BizAutomationHandler handler, BizAutomationConfig config) {
        Map<String, String> params = new HashMap<>();
        if (handler != null) {
            for (BizAutomationHandler.AutomationParam param : handler.getParamDef()) {
                params.put(param.key(), param.defaultValue());
            }
        }
        if (config != null && StrUtil.isNotBlank(config.getParams())) {
            try {
                cn.hutool.json.JSONObject saved = JSONUtil.parseObj(config.getParams());
                for (String key : saved.keySet()) {
                    Object value = saved.get(key);
                    params.put(key, value == null ? null : String.valueOf(value));
                }
            } catch (Exception e) {
                log.warn(">>> 自动化规则参数解析失败：{}", config.getRuleCode());
            }
        }
        return params;
    }

    /** 查找规则执行器 */
    private BizAutomationHandler getHandler(String ruleCode) {
        for (BizAutomationHandler handler : handlerList) {
            if (handler.getRuleCode().equals(ruleCode)) {
                return handler;
            }
        }
        return null;
    }

    /** 当前操作人（定时任务为系统） */
    private String currentOperatorId() {
        try {
            return cn.dev33.satoken.stp.StpUtil.isLogin() ? cn.dev33.satoken.stp.StpUtil.getLoginIdAsString() : SYSTEM_USER;
        } catch (Exception e) {
            return SYSTEM_USER;
        }
    }
}
