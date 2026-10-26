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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.biz.api.automation.AutomationSupport;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.biz.api.automation.BizAutomationRuleDTO;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationConfig;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationConfigHistory;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationLog;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationSetting;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationTarget;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationConfigHistoryMapper;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationConfigMapper;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationLogMapper;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationSettingMapper;
import vip.xiaonuo.biz.modular.automation.mapper.BizAutomationTargetMapper;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationLogPageParam;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationSettingParam;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationToggleParam;
import vip.xiaonuo.biz.modular.automation.service.BizAutomationService;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 自动化规则Service接口实现类
 * <p>
 * 规则由各业务插件实现 {@link BizAutomationHandler} 并注册为 Spring Bean，
 * 这里统一负责：开关与参数存储、立即执行、预览、租约锁、静默时段、每日配额、
 * 目标级幂等占位、执行记录与实验归因。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Service
public class BizAutomationServiceImpl extends ServiceImpl<BizAutomationConfigMapper, BizAutomationConfig>
        implements BizAutomationService {

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

    private static final int MAX_RETRY = 3;

    @Resource
    private BizAutomationLogMapper bizAutomationLogMapper;

    @Resource
    private BizAutomationTargetMapper bizAutomationTargetMapper;

    @Resource
    private BizAutomationSettingMapper bizAutomationSettingMapper;

    @Resource
    private BizAutomationConfigHistoryMapper bizAutomationConfigHistoryMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private JdbcTemplate jdbcTemplate;

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
            dto.setNotificationRule(handler.isNotificationRule());
            dto.setSupportsHoldout(handler.supportsHoldout());
            BizAutomationConfig config = configMap.get(handler.getRuleCode());
            boolean enabled = config != null && StrUtil.isNotBlank(config.getEnabled())
                    ? "YES".equalsIgnoreCase(config.getEnabled()) : handler.defaultEnabled();
            dto.setEnabled(enabled);
            dto.setParams(mergeParams(handler, config));
            dto.setDailyCap(config == null || config.getDailyCap() == null ? 0 : config.getDailyCap());
            dto.setHoldoutPercent(config == null || config.getHoldoutPercent() == null
                    ? 0 : config.getHoldoutPercent());
            dto.setVersion(config == null || config.getVersion() == null ? 1 : config.getVersion());
            dto.setLastChangeTime(config == null || config.getUpdateTime() == null ? null
                    : DateUtil.formatDateTime(config.getUpdateTime()));
            // 最近一次执行情况
            BizAutomationLog lastLog = bizAutomationLogMapper.selectOne(new LambdaQueryWrapper<BizAutomationLog>()
                    .eq(BizAutomationLog::getRuleCode, handler.getRuleCode())
                    .orderByDesc(BizAutomationLog::getCreateTime)
                    .last("limit 1"));
            if (lastLog != null) {
                dto.setLastRunTime(lastLog.getCreateTime() == null ? null
                        : DateUtil.formatDateTime(lastLog.getCreateTime()));
                dto.setLastRunMessage(lastLog.getMessage());
                dto.setLastRunSuccess("YES".equalsIgnoreCase(lastLog.getSuccess()));
                dto.setLastSuccessCount(lastLog.getSuccessCount());
                dto.setLastFailCount(lastLog.getFailCount());
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
    public void toggle(BizAutomationToggleParam param) {
        BizAutomationHandler handler = getHandler(param.getRuleCode());
        if (handler == null) {
            throw new CommonException("自动化规则不存在：{}", param.getRuleCode());
        }
        if (!"YES".equalsIgnoreCase(param.getEnabled()) && !"NO".equalsIgnoreCase(param.getEnabled())) {
            throw new CommonException("开关值只能是 YES 或 NO");
        }
        int dailyCap = ObjectUtil.defaultIfNull(param.getDailyCap(), 0);
        if (dailyCap < 0 || dailyCap > 100000) {
            throw new CommonException("每日上限必须在 0-100000 之间");
        }
        int holdoutPercent = ObjectUtil.defaultIfNull(param.getHoldoutPercent(), 0);
        if (holdoutPercent < 0 || holdoutPercent > 100) {
            throw new CommonException("holdout 比例必须在 0-100 之间");
        }
        if (holdoutPercent > 0 && !handler.supportsHoldout()) {
            throw new CommonException("该规则不支持 holdout 实验分组");
        }
        Map<String, String> params = param.getParams() == null ? Map.of() : param.getParams();
        validateParams(handler, params);

        BizAutomationConfig config = this.getOne(new LambdaQueryWrapper<BizAutomationConfig>()
                .eq(BizAutomationConfig::getRuleCode, param.getRuleCode()), false);
        BizAutomationConfigHistory history = new BizAutomationConfigHistory();
        history.setId(IdUtil.getSnowflakeNextIdStr());
        history.setTenantId(TenantContext.getTenantId());
        history.setRuleCode(param.getRuleCode());
        history.setOperatorId(currentOperatorId());
        history.setOldEnabled(config == null ? null : config.getEnabled());
        history.setOldParams(config == null ? null : config.getParams());
        history.setNewEnabled("YES".equalsIgnoreCase(param.getEnabled()) ? "YES" : "NO");
        history.setNewParams(JSONUtil.toJsonStr(params));
        int newVersion = config == null || config.getVersion() == null ? 1 : config.getVersion() + 1;
        history.setVersion(newVersion);
        bizAutomationConfigHistoryMapper.insert(history);

        if (config == null) {
            config = new BizAutomationConfig();
            config.setRuleCode(handler.getRuleCode());
            config.setTenantId(TenantContext.getTenantId());
        }
        config.setEnabled(history.getNewEnabled());
        config.setParams(history.getNewParams());
        config.setDailyCap(dailyCap);
        config.setHoldoutPercent(holdoutPercent);
        config.setVersion(newVersion);
        this.saveOrUpdate(config);
    }

    @Override
    public BizAutomationHandler.AutomationResult run(String ruleCode, String triggerType) {
        return run(ruleCode, triggerType, false);
    }

    @Override
    public BizAutomationHandler.AutomationResult run(String ruleCode, String triggerType, boolean dryRun) {
        BizAutomationHandler handler = getHandler(ruleCode);
        if (handler == null) {
            throw new CommonException("自动化规则不存在：{}", ruleCode);
        }
        String tenantId = TenantContext.getTenantId();
        BizAutomationSetting setting = getSettingEntity();
        if (!dryRun && isPaused(setting)) {
            return BizAutomationHandler.AutomationResult.ok("自动化已暂停，未执行任何动作", 0);
        }
        if (!dryRun && handler.isNotificationRule() && !TRIGGER_TYPE_MANUAL.equals(triggerType)
                && inQuietHours(setting)) {
            return BizAutomationHandler.AutomationResult.ok("当前处于静默时段，提醒类规则已跳过", 0);
        }
        Map<String, String> params = getParams(ruleCode);
        validateParams(handler, params);
        int remainingQuota = dryRun ? Integer.MAX_VALUE : remainingQuota(ruleCode);
        if (!dryRun && remainingQuota <= 0) {
            return BizAutomationHandler.AutomationResult.ok("今日已达执行上限，未执行任何动作", 0);
        }

        RLock lock = null;
        boolean locked = false;
        if (!dryRun) {
            lock = redissonClient.getLock("biz:automation:lock:" + tenantId + ":" + ruleCode);
            try {
                locked = lock.tryLock(0, 10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return BizAutomationHandler.AutomationResult.fail("获取执行锁被中断");
            }
            if (!locked) {
                return BizAutomationHandler.AutomationResult.fail("上一轮仍在执行，本次已跳过");
            }
        }

        long start = System.currentTimeMillis();
        String logId = IdUtil.getSnowflakeNextIdStr();
        BizAutomationTargetRecorder recorder = new BizAutomationTargetRecorder(
                tenantId, ruleCode, logId, dryRun, handler.supportsHoldout(),
                getHoldoutPercent(ruleCode), remainingQuota);
        BizAutomationHandler.AutomationResult result;
        try {
            BizAutomationHandler.AutomationContext context = new BizAutomationHandler.AutomationContext(
                    params, triggerType, currentOperatorId(), dryRun, remainingQuota, tenantId, ruleCode, recorder);
            result = handler.execute(context);
        } catch (Exception e) {
            log.error(">>> 自动化规则[{}]执行失败", ruleCode, e);
            result = BizAutomationHandler.AutomationResult.fail("执行失败：" + e.getMessage());
        } finally {
            if (lock != null && locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        // 预览不落执行记录，避免每次点击都污染历史
        if (dryRun) {
            return result;
        }
        int successCount = recorder.successCount;
        int failCount = recorder.failCount;
        int skipCount = recorder.skipCount;
        int targetCount = successCount + failCount + skipCount;
        boolean overallSuccess = result.success() && failCount == 0;
        String message = result.message();
        if (failCount > 0) {
            message = message + "（失败 " + failCount + " 条）";
        }
        if (skipCount > 0) {
            message = message + "（跳过 " + skipCount + " 条）";
        }

        BizAutomationLog automationLog = new BizAutomationLog();
        automationLog.setId(logId);
        automationLog.setTenantId(tenantId);
        automationLog.setRuleCode(ruleCode);
        automationLog.setRuleName(handler.getRuleName());
        automationLog.setTriggerType(triggerType);
        automationLog.setSuccess(overallSuccess ? "YES" : "NO");
        automationLog.setMessage(StrUtil.maxLength(message, 480));
        automationLog.setAffectedCount(successCount);
        automationLog.setRuleVersion(handler.getRuleVersion());
        automationLog.setInputHash(DigestUtil.sha256Hex(String.join("|",
                StrUtil.blankToDefault(tenantId, ""), ruleCode,
                String.valueOf(handler.getRuleVersion()), JSONUtil.toJsonStr(params))));
        automationLog.setTargetCount(targetCount);
        automationLog.setSuccessCount(successCount);
        automationLog.setFailCount(failCount);
        automationLog.setSkipCount(skipCount);
        automationLog.setDeliveryStatus(recorder.deliveryStatus());
        automationLog.setDeliveryCount(recorder.deliveryCount);
        automationLog.setMessageId(recorder.firstMessageId());
        automationLog.setDryRun("NO");
        automationLog.setExperimentGroup(recorder.experimentGroupSummary());
        automationLog.setAttributionStatus(handler.supportsHoldout() ? "PENDING" : "NA");
        automationLog.setDetail(recorder.detailJson(result.detail()));
        automationLog.setCostMs(System.currentTimeMillis() - start);
        bizAutomationLogMapper.insert(automationLog);
        return new BizAutomationHandler.AutomationResult(overallSuccess, message, successCount,
                automationLog.getDetail());
    }

    @Override
    public Page<BizAutomationLog> logPage(BizAutomationLogPageParam param) {
        LambdaQueryWrapper<BizAutomationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(param.getRuleCode())) {
            queryWrapper.eq(BizAutomationLog::getRuleCode, param.getRuleCode());
        }
        if (StrUtil.isNotBlank(param.getTriggerType())) {
            queryWrapper.eq(BizAutomationLog::getTriggerType, param.getTriggerType());
        }
        if (StrUtil.isNotBlank(param.getSuccess())) {
            queryWrapper.eq(BizAutomationLog::getSuccess, param.getSuccess());
        }
        queryWrapper.orderByDesc(BizAutomationLog::getCreateTime);
        return bizAutomationLogMapper.selectPage(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Override
    public boolean isEnabled(String ruleCode) {
        if (isPaused(getSettingEntity())) {
            return false;
        }
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

    @Override
    public Map<String, Object> getSetting() {
        BizAutomationSetting setting = getSettingEntity();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paused", setting.getPaused());
        result.put("quietStart", setting.getQuietStart());
        result.put("quietEnd", setting.getQuietEnd());
        result.put("timezone", setting.getTimezone());
        result.put("inQuietHours", inQuietHours(setting));
        result.put("serverTime", DateUtil.formatDateTime(new Date()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSetting(BizAutomationSettingParam param) {
        String paused = "YES".equalsIgnoreCase(param.getPaused()) ? "YES" : "NO";
        if (StrUtil.isNotBlank(param.getQuietStart()) && !AutomationSupport.isValidHhMm(param.getQuietStart())) {
            throw new CommonException("静默开始时间格式应为 HH:mm");
        }
        if (StrUtil.isNotBlank(param.getQuietEnd()) && !AutomationSupport.isValidHhMm(param.getQuietEnd())) {
            throw new CommonException("静默结束时间格式应为 HH:mm");
        }
        if ((StrUtil.isBlank(param.getQuietStart()) && StrUtil.isNotBlank(param.getQuietEnd()))
                || (StrUtil.isNotBlank(param.getQuietStart()) && StrUtil.isBlank(param.getQuietEnd()))) {
            throw new CommonException("静默开始和结束时间需要同时填写");
        }
        String timezone = StrUtil.blankToDefault(param.getTimezone(), "Asia/Shanghai");
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new CommonException("时区不合法：{}", timezone);
        }
        BizAutomationSetting setting = getSettingEntity();
        boolean exists = setting.getId() != null;
        if (!exists) {
            setting.setId(IdUtil.getSnowflakeNextIdStr());
            setting.setTenantId(TenantContext.getTenantId());
            setting.setCreateTime(new Date());
            setting.setDeleteFlag("NOT_DELETE");
        }
        setting.setPaused(paused);
        String quietStart = StrUtil.emptyToNull(param.getQuietStart());
        String quietEnd = StrUtil.emptyToNull(param.getQuietEnd());
        setting.setQuietStart(quietStart);
        setting.setQuietEnd(quietEnd);
        setting.setTimezone(timezone);
        if (exists) {
            // updateById 默认忽略 null，清空静默时段必须显式 set
            bizAutomationSettingMapper.update(null, new LambdaUpdateWrapper<BizAutomationSetting>()
                    .eq(BizAutomationSetting::getId, setting.getId())
                    .set(BizAutomationSetting::getPaused, paused)
                    .set(BizAutomationSetting::getQuietStart, quietStart)
                    .set(BizAutomationSetting::getQuietEnd, quietEnd)
                    .set(BizAutomationSetting::getTimezone, timezone)
                    .set(BizAutomationSetting::getUpdateTime, new Date()));
        } else {
            bizAutomationSettingMapper.insert(setting);
        }
    }

    @Override
    public Map<String, Object> experimentStats(String ruleCode, Integer windowDays) {
        BizAutomationHandler handler = getHandler(ruleCode);
        if (handler == null) {
            throw new CommonException("自动化规则不存在：{}", ruleCode);
        }
        int days = windowDays == null || windowDays < 1 || windowDays > 365 ? 90 : windowDays;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleCode", ruleCode);
        result.put("windowDays", days);
        if (!handler.supportsHoldout()) {
            result.put("groups", List.of());
            result.put("note", "该规则不支持 holdout 实验，仅提供执行结果，不计算增量");
            return result;
        }
        refreshOutcomes(ruleCode, days);
        Date start = DateUtil.offsetDay(new Date(), -days);
        List<Map<String, Object>> groups = jdbcTemplate.queryForList("""
                SELECT `EXPERIMENT_GROUP` AS `group`,
                       COUNT(*) AS `total`,
                       SUM(CASE WHEN `OUTCOME_STATUS` = 'SUCCESS' THEN 1 ELSE 0 END) AS `success`,
                       COALESCE(SUM(`OUTCOME_VALUE`), 0) AS `value`
                FROM `BIZ_AUTOMATION_TARGET`
                WHERE `TENANT_ID` = ? AND `RULE_CODE` = ?
                  AND `EXPERIMENT_GROUP` IN ('TREATMENT', 'HOLDOUT')
                  AND `STATUS` IN ('SUCCESS', 'SKIPPED')
                  AND `CREATE_TIME` >= ?
                GROUP BY `EXPERIMENT_GROUP`
                """, TenantContext.getTenantId(), ruleCode, start);
        for (Map<String, Object> group : groups) {
            long total = ((Number) group.get("total")).longValue();
            long success = ((Number) group.get("success")).longValue();
            group.put("rate", total == 0 ? 0 : Math.round(success * 10000.0 / total) / 100.0);
        }
        result.put("groups", groups);
        result.put("note", "按 holdout 分组的观察性归因；付款/续费为业务表结果，不等同于严格因果证明");
        return result;
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
                JSONObject saved = JSONUtil.parseObj(config.getParams());
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

    /** 参数范围校验：数字上下限、开关值、文本长度 */
    private void validateParams(BizAutomationHandler handler, Map<String, String> params) {
        for (BizAutomationHandler.AutomationParam param : handler.getParamDef()) {
            String raw = params == null ? null : params.get(param.key());
            String value = StrUtil.isBlank(raw) ? param.defaultValue() : raw;
            if ("NUMBER".equals(param.type())) {
                int number;
                try {
                    number = Integer.parseInt(value);
                } catch (Exception e) {
                    throw new CommonException("参数【{}】必须是整数", param.label());
                }
                if (param.min() != null && number < param.min()) {
                    throw new CommonException("参数【{}】不能小于 {}", param.label(), param.min());
                }
                if (param.max() != null && number > param.max()) {
                    throw new CommonException("参数【{}】不能大于 {}", param.label(), param.max());
                }
            } else if ("SWITCH".equals(param.type())) {
                if (!"YES".equalsIgnoreCase(value) && !"NO".equalsIgnoreCase(value)) {
                    throw new CommonException("参数【{}】只能是 YES 或 NO", param.label());
                }
            } else if (value != null && value.length() > 500) {
                throw new CommonException("参数【{}】过长", param.label());
            }
        }
    }

    private BizAutomationSetting getSettingEntity() {
        BizAutomationSetting setting = bizAutomationSettingMapper.selectOne(
                new LambdaQueryWrapper<BizAutomationSetting>().last("limit 1"));
        if (setting == null) {
            setting = new BizAutomationSetting();
            setting.setPaused("NO");
            setting.setTimezone("Asia/Shanghai");
        }
        return setting;
    }

    private boolean isPaused(BizAutomationSetting setting) {
        return setting != null && "YES".equalsIgnoreCase(setting.getPaused());
    }

    private boolean inQuietHours(BizAutomationSetting setting) {
        if (setting == null) {
            return false;
        }
        String timezone = StrUtil.blankToDefault(setting.getTimezone(), "Asia/Shanghai");
        LocalTime now = ZonedDateTime.now(ZoneId.of(timezone)).toLocalTime();
        return AutomationSupport.inQuietHours(now, setting.getQuietStart(), setting.getQuietEnd());
    }

    private int getHoldoutPercent(String ruleCode) {
        BizAutomationConfig config = this.getOne(new LambdaQueryWrapper<BizAutomationConfig>()
                .eq(BizAutomationConfig::getRuleCode, ruleCode), false);
        return config == null || config.getHoldoutPercent() == null ? 0 : config.getHoldoutPercent();
    }

    /** 当天该规则已成功执行的目标数，用于每日配额 */
    private int remainingQuota(String ruleCode) {
        BizAutomationConfig config = this.getOne(new LambdaQueryWrapper<BizAutomationConfig>()
                .eq(BizAutomationConfig::getRuleCode, ruleCode), false);
        if (config == null || config.getDailyCap() == null || config.getDailyCap() <= 0) {
            return Integer.MAX_VALUE;
        }
        Long used = bizAutomationTargetMapper.selectCount(new LambdaQueryWrapper<BizAutomationTarget>()
                .eq(BizAutomationTarget::getRuleCode, ruleCode)
                .eq(BizAutomationTarget::getStatus, "SUCCESS")
                .ge(BizAutomationTarget::getCreateTime, DateUtil.beginOfDay(new Date())));
        return Math.max(0, config.getDailyCap() - (used == null ? 0 : used.intValue()));
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

    /**
     * 归因：把 48 小时前的触达目标与业务结果对齐。
     * 只做观察性统计，不声称严格因果。
     */
    private void refreshOutcomes(String ruleCode, int windowDays) {
        Date start = DateUtil.offsetDay(new Date(), -windowDays);
        List<Map<String, Object>> pendingList = jdbcTemplate.queryForList("""
                SELECT `ID`, `TARGET_TYPE`, `TARGET_ID`, COALESCE(`ACTION_TIME`, `CREATE_TIME`) AS `ACTION_TIME`
                FROM `BIZ_AUTOMATION_TARGET`
                WHERE `TENANT_ID` = ? AND `RULE_CODE` = ?
                  AND `OUTCOME_STATUS` = 'PENDING'
                  AND `STATUS` IN ('SUCCESS', 'SKIPPED')
                  AND `CREATE_TIME` < DATE_SUB(NOW(), INTERVAL 48 HOUR)
                  AND `CREATE_TIME` >= ?
                ORDER BY `CREATE_TIME` ASC
                LIMIT 500
                """, TenantContext.getTenantId(), ruleCode, start);
        for (Map<String, Object> row : pendingList) {
            String targetType = String.valueOf(row.get("TARGET_TYPE"));
            String targetId = String.valueOf(row.get("TARGET_ID"));
            Date actionTime = (Date) row.get("ACTION_TIME");
            String outcomeStatus = "FAILED";
            BigDecimal outcomeValue = BigDecimal.ZERO;
            if ("ORDER".equals(targetType)) {
                List<Map<String, Object>> orders = jdbcTemplate.queryForList("""
                        SELECT `STATUS`, `PAY_TIME`, `PAID_AMOUNT`
                        FROM `STUDENT_ORDER`
                        WHERE `ID` = ? AND `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'
                        """, targetId, TenantContext.getTenantId());
                if (!orders.isEmpty()) {
                    Map<String, Object> order = orders.get(0);
                    Date payTime = (Date) order.get("PAY_TIME");
                    String status = String.valueOf(order.get("STATUS"));
                    if (payTime != null && payTime.after(actionTime)
                            && ("PAID".equals(status) || "REFUNDED".equals(status))) {
                        outcomeStatus = "SUCCESS";
                        outcomeValue = order.get("PAID_AMOUNT") == null ? BigDecimal.ZERO
                                : new BigDecimal(String.valueOf(order.get("PAID_AMOUNT")));
                    }
                }
            } else if ("RENEW_WARN".equals(targetType)) {
                List<Map<String, Object>> warns = jdbcTemplate.queryForList("""
                        SELECT `STUDENT_ID` FROM `STUDENT_RENEW_WARN`
                        WHERE `ID` = ? AND `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'
                        """, targetId, TenantContext.getTenantId());
                if (!warns.isEmpty()) {
                    String studentId = String.valueOf(warns.get(0).get("STUDENT_ID"));
                    List<Map<String, Object>> orders = jdbcTemplate.queryForList("""
                            SELECT COALESCE(SUM(`PAID_AMOUNT`), 0) AS `PAID`
                            FROM `STUDENT_ORDER`
                            WHERE `TENANT_ID` = ? AND `STUDENT_ID` = ?
                              AND `DELETE_FLAG` = 'NOT_DELETE'
                              AND `STATUS` IN ('PAID', 'REFUNDED')
                              AND `PAY_TIME` > ?
                            """, TenantContext.getTenantId(), studentId, actionTime);
                    BigDecimal paid = orders.isEmpty() || orders.get(0).get("PAID") == null ? BigDecimal.ZERO
                            : new BigDecimal(String.valueOf(orders.get(0).get("PAID")));
                    if (paid.compareTo(BigDecimal.ZERO) > 0) {
                        outcomeStatus = "SUCCESS";
                        outcomeValue = paid;
                    }
                }
            }
            jdbcTemplate.update("""
                    UPDATE `BIZ_AUTOMATION_TARGET`
                    SET `OUTCOME_STATUS` = ?, `OUTCOME_TIME` = NOW(), `OUTCOME_VALUE` = ?, `UPDATE_TIME` = NOW()
                    WHERE `ID` = ? AND `TENANT_ID` = ?
                    """, outcomeStatus, outcomeValue, String.valueOf(row.get("ID")), TenantContext.getTenantId());
        }
    }

    /**
     * 目标级记录器：数据库唯一键做幂等占位，失败记录允许有限重试。
     */
    private class BizAutomationTargetRecorder implements BizAutomationHandler.TargetRecorder {

        private final String tenantId;
        private final String ruleCode;
        private final String logId;
        private final boolean dryRun;
        private final boolean supportsHoldout;
        private final int holdoutPercent;
        private final int quota;
        private int claimedCount;
        private int successCount;
        private int failCount;
        private int skipCount;
        private int deliveryCount;
        private final Set<String> groups = new LinkedHashSet<>();
        private final Set<String> messageIds = new LinkedHashSet<>();

        BizAutomationTargetRecorder(String tenantId, String ruleCode, String logId, boolean dryRun,
                                    boolean supportsHoldout, int holdoutPercent, int quota) {
            this.tenantId = tenantId;
            this.ruleCode = ruleCode;
            this.logId = logId;
            this.dryRun = dryRun;
            this.supportsHoldout = supportsHoldout;
            this.holdoutPercent = holdoutPercent;
            this.quota = quota;
        }

        @Override
        public boolean claim(String targetType, String targetId, String action, String windowKey) {
            if (claimedCount >= quota) {
                return false;
            }
            String key = AutomationSupport.idempotencyKey(ruleCode, action, targetId, windowKey);
            if (dryRun) {
                claimedCount++;
                return true;
            }
            BizAutomationTarget existing = find(key);
            if (existing == null) {
                BizAutomationTarget target = newTarget(targetType, targetId, action, key);
                target.setStatus("CLAIMED");
                target.setCreateTime(new Date());
                target.setActionTime(new Date());
                try {
                    bizAutomationTargetMapper.insert(target);
                    claimedCount++;
                    return true;
                } catch (DuplicateKeyException e) {
                    return false;
                }
            }
            int retry = ObjectUtil.defaultIfNull(existing.getRetryCount(), 0);
            if ("FAILED".equals(existing.getStatus()) && retry < MAX_RETRY) {
                bizAutomationTargetMapper.update(null, new LambdaUpdateWrapper<BizAutomationTarget>()
                        .eq(BizAutomationTarget::getId, existing.getId())
                        .set(BizAutomationTarget::getStatus, "CLAIMED")
                        .set(BizAutomationTarget::getReason, null)
                        .set(BizAutomationTarget::getRetryCount, retry + 1)
                        .set(BizAutomationTarget::getActionTime, new Date())
                        .set(BizAutomationTarget::getUpdateTime, new Date()));
                claimedCount++;
                return true;
            }
            return false;
        }

        @Override
        public void success(String targetType, String targetId, String action, String messageId, String detail) {
            successCount++;
            groups.add(experimentGroup(targetId));
            if (dryRun) {
                return;
            }
            BizAutomationTarget target = findLatest(targetId, action);
            if (target == null) {
                target = newTarget(targetType, targetId, action,
                        AutomationSupport.idempotencyKey(ruleCode, action, targetId, ""));
                target.setStatus("SUCCESS");
                target.setMessageId(messageId);
                target.setExperimentGroup(experimentGroup(targetId));
                target.setCreateTime(new Date());
                target.setActionTime(new Date());
                bizAutomationTargetMapper.insert(target);
            } else {
                bizAutomationTargetMapper.update(null, new LambdaUpdateWrapper<BizAutomationTarget>()
                        .eq(BizAutomationTarget::getId, target.getId())
                        .set(BizAutomationTarget::getStatus, "SUCCESS")
                        .set(BizAutomationTarget::getReason, null)
                        .set(BizAutomationTarget::getMessageId, messageId)
                        .set(BizAutomationTarget::getExperimentGroup, experimentGroup(targetId))
                        .set(BizAutomationTarget::getActionTime, new Date())
                        .set(BizAutomationTarget::getUpdateTime, new Date()));
            }
            if (StrUtil.isNotBlank(messageId)) {
                deliveryCount++;
                messageIds.add(messageId);
            }
        }

        @Override
        public void fail(String targetType, String targetId, String action, String reason) {
            failCount++;
            groups.add(experimentGroup(targetId));
            if (dryRun) {
                return;
            }
            BizAutomationTarget target = findLatest(targetId, action);
            if (target == null) {
                target = newTarget(targetType, targetId, action,
                        AutomationSupport.idempotencyKey(ruleCode, action, targetId, ""));
                target.setStatus("FAILED");
                target.setReason(StrUtil.maxLength(reason, 480));
                target.setExperimentGroup(experimentGroup(targetId));
                target.setCreateTime(new Date());
                target.setActionTime(new Date());
                bizAutomationTargetMapper.insert(target);
            } else {
                bizAutomationTargetMapper.update(null, new LambdaUpdateWrapper<BizAutomationTarget>()
                        .eq(BizAutomationTarget::getId, target.getId())
                        .set(BizAutomationTarget::getStatus, "FAILED")
                        .set(BizAutomationTarget::getReason, StrUtil.maxLength(reason, 480))
                        .set(BizAutomationTarget::getExperimentGroup, experimentGroup(targetId))
                        .set(BizAutomationTarget::getUpdateTime, new Date()));
            }
        }

        @Override
        public void skip(String targetType, String targetId, String action, String reason, String experimentGroup) {
            skipCount++;
            groups.add(experimentGroup);
            if (dryRun) {
                return;
            }
            String key = AutomationSupport.idempotencyKey(ruleCode, action, targetId, "");
            BizAutomationTarget target = findLatest(targetId, action);
            if (target == null) {
                target = newTarget(targetType, targetId, action, key);
                target.setStatus("SKIPPED");
                target.setReason(StrUtil.maxLength(reason, 480));
                target.setExperimentGroup(experimentGroup);
                target.setCreateTime(new Date());
                target.setActionTime(new Date());
                bizAutomationTargetMapper.insert(target);
            } else {
                bizAutomationTargetMapper.update(null, new LambdaUpdateWrapper<BizAutomationTarget>()
                        .eq(BizAutomationTarget::getId, target.getId())
                        .set(BizAutomationTarget::getStatus, "SKIPPED")
                        .set(BizAutomationTarget::getReason, StrUtil.maxLength(reason, 480))
                        .set(BizAutomationTarget::getExperimentGroup, experimentGroup)
                        .set(BizAutomationTarget::getUpdateTime, new Date()));
            }
        }

        @Override
        public boolean isHoldout(String targetId) {
            if (!supportsHoldout || holdoutPercent <= 0) {
                return false;
            }
            return AutomationSupport.isHoldout(tenantId, ruleCode, targetId, holdoutPercent);
        }

        @Override
        public String experimentGroup(String targetId) {
            if (!supportsHoldout) {
                return "NONE";
            }
            return isHoldout(targetId) ? "HOLDOUT" : "TREATMENT";
        }

        String deliveryStatus() {
            if (deliveryCount > 0) {
                return "CREATED";
            }
            return failCount > 0 ? "FAILED" : "NONE";
        }

        String firstMessageId() {
            return messageIds.isEmpty() ? null : messageIds.iterator().next();
        }

        String experimentGroupSummary() {
            if (groups.contains("TREATMENT") && groups.contains("HOLDOUT")) {
                return "MIXED";
            }
            if (groups.contains("HOLDOUT")) {
                return "HOLDOUT";
            }
            if (groups.contains("TREATMENT")) {
                return "TREATMENT";
            }
            return "NONE";
        }

        String detailJson(String handlerDetail) {
            JSONObject detail = JSONUtil.createObj()
                    .set("handlerDetail", handlerDetail)
                    .set("success", successCount)
                    .set("failed", failCount)
                    .set("skipped", skipCount)
                    .set("delivery", deliveryCount);
            return JSONUtil.toJsonStr(detail);
        }

        private BizAutomationTarget newTarget(String targetType, String targetId, String action, String key) {
            BizAutomationTarget target = new BizAutomationTarget();
            target.setId(IdUtil.getSnowflakeNextIdStr());
            target.setTenantId(tenantId);
            target.setLogId(logId);
            target.setRuleCode(ruleCode);
            target.setTargetType(targetType);
            target.setTargetId(targetId);
            target.setAction(action);
            target.setIdempotencyKey(key);
            target.setExperimentGroup(experimentGroup(targetId));
            target.setOutcomeStatus(supportsHoldout ? "PENDING" : "NA");
            target.setRetryCount(0);
            target.setDeleteFlag("NOT_DELETE");
            return target;
        }

        private BizAutomationTarget find(String key) {
            return bizAutomationTargetMapper.selectOne(new LambdaQueryWrapper<BizAutomationTarget>()
                    .eq(BizAutomationTarget::getRuleCode, ruleCode)
                    .eq(BizAutomationTarget::getIdempotencyKey, key)
                    .last("limit 1"));
        }

        private BizAutomationTarget findLatest(String targetId, String action) {
            return bizAutomationTargetMapper.selectOne(new LambdaQueryWrapper<BizAutomationTarget>()
                    .eq(BizAutomationTarget::getRuleCode, ruleCode)
                    .eq(BizAutomationTarget::getTargetId, targetId)
                    .eq(BizAutomationTarget::getAction, action)
                    .orderByDesc(BizAutomationTarget::getCreateTime)
                    .last("limit 1"));
        }
    }
}
