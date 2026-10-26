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
package vip.xiaonuo.crm.core.automation;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadStatusEnum;
import vip.xiaonuo.crm.modular.lead.mapper.CrmLeadMapper;
import vip.xiaonuo.crm.modular.trial.entity.CrmTrialAppointment;
import vip.xiaonuo.crm.modular.trial.enums.CrmTrialStatusEnum;
import vip.xiaonuo.crm.modular.trial.mapper.CrmTrialAppointmentMapper;
import vip.xiaonuo.dev.api.DevMessageApi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 自动化规则：试听结束自动推进
 * <p>
 * 顾问约完试听常常忘了回来改状态，线索就卡在「待试听」。
 * 这里在试听时间过后自动把预约置为已完成、把线索推进到「已试听」，
 * 并给跟进人发站内信提醒录入结果、推进转化。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/23 10:00
 **/
@Slf4j
@Component
public class AutoTrialFollowUpHandler implements BizAutomationHandler {

    /** 规则编码 */
    public static final String RULE_CODE = "AUTO_TRIAL_FOLLOWUP";
    /** 提示里保留的条数 */
    private static final int MESSAGE_LIMIT = 5;

    @Resource
    private CrmTrialAppointmentMapper crmTrialAppointmentMapper;

    @Resource
    private CrmLeadMapper crmLeadMapper;

    @Resource
    private DevMessageApi devMessageApi;

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return "试听结束自动推进";
    }

    @Override
    public String getRuleDesc() {
        return "试听时间过后自动把预约置为已完成、线索推进到「已试听」，并提醒跟进人录入结果、推进转化";
    }

    @Override
    public String getMode() {
        return "TIMER";
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public int getRuleVersion() {
        return 2;
    }

    @Override
    public List<AutomationParam> getParamDef() {
        return List.of(
                new AutomationParam("graceMinutes", "宽限时间（分钟）", "NUMBER", "30",
                        "试听结束后等待多久仍未处理才自动推进，留出顾问当场录入结果的时间", 1, 1440),
                new AutomationParam("notify", "提醒跟进人", "SWITCH", "YES",
                        "推进后给线索跟进人发站内信，提醒录入试听结果并跟进转化")
        );
    }

    @Override
    public AutomationResult execute(AutomationContext context) {
        int graceMinutes = context.getIntParam("graceMinutes", 30);
        boolean notify = !"NO".equalsIgnoreCase(context.getParam("notify", "YES"));
        int limit = Math.min(context.remainingQuota(), 200);
        if (limit <= 0) {
            return AutomationResult.ok("今日已达执行上限", 0);
        }
        Date deadline = DateUtil.offsetMinute(new Date(), -Math.max(graceMinutes, 0));

        // 已过试听时间、仍停在「待试听」的预约
        List<CrmTrialAppointment> trialList = crmTrialAppointmentMapper.selectList(
                new LambdaQueryWrapper<CrmTrialAppointment>()
                        .eq(CrmTrialAppointment::getStatus, CrmTrialStatusEnum.PENDING)
                        .isNotNull(CrmTrialAppointment::getTrialTime)
                        .lt(CrmTrialAppointment::getTrialTime, deadline)
                        .orderByAsc(CrmTrialAppointment::getTrialTime)
                        .last("LIMIT " + limit));
        if (trialList.isEmpty()) {
            return AutomationResult.ok("没有需要推进的试听预约", 0);
        }

        List<CrmTrialAppointment> advancedTrials = new ArrayList<>();
        List<String> leadIdList = new ArrayList<>();
        List<String> detailList = new ArrayList<>();
        for (CrmTrialAppointment trial : trialList) {
            if (context.dryRun()) {
                advancedTrials.add(trial);
                if (StrUtil.isNotBlank(trial.getLeadId())) {
                    leadIdList.add(trial.getLeadId());
                }
                detailList.add(trial.getId());
                continue;
            }
            if (!context.claim("TRIAL", trial.getId(), "ADVANCE", "ADVANCE")) {
                continue;
            }
            // 条件更新：只有仍为「待试听」才推进，避免和顾问手工操作打架
            int updated = crmTrialAppointmentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmTrialAppointment>()
                            .eq(CrmTrialAppointment::getId, trial.getId())
                            .eq(CrmTrialAppointment::getStatus, CrmTrialStatusEnum.PENDING)
                            .set(CrmTrialAppointment::getStatus, CrmTrialStatusEnum.DONE));
            if (updated == 0) {
                context.skip("TRIAL", trial.getId(), "ADVANCE", "预约状态已被手工修改");
                continue;
            }
            advancedTrials.add(trial);
            if (StrUtil.isNotBlank(trial.getLeadId())) {
                leadIdList.add(trial.getLeadId());
            }
            detailList.add(trial.getId());
            context.success("TRIAL", trial.getId(), "ADVANCE", null, null);
        }
        if (context.dryRun()) {
            return AutomationResult.ok("预览：将推进 " + advancedTrials.size() + " 场试听",
                    advancedTrials.size(), detailList.isEmpty() ? null : String.join(",", detailList));
        }
        if (advancedTrials.isEmpty()) {
            return AutomationResult.ok("没有需要推进的试听预约", 0);
        }

        int advancedLeadCount = 0;
        List<String> notifyUserIdList = new ArrayList<>();
        List<String> messageLineList = new ArrayList<>();
        if (!leadIdList.isEmpty()) {
            List<CrmLead> leadList = crmLeadMapper.selectList(new LambdaQueryWrapper<CrmLead>()
                    .in(CrmLead::getId, leadIdList));
            for (CrmLead lead : leadList) {
                // 线索推进：新线索/跟进中 → 已试听（已报名/已流失不动）
                int leadUpdated = crmLeadMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmLead>()
                                .eq(CrmLead::getId, lead.getId())
                                .in(CrmLead::getStatus, CrmLeadStatusEnum.NEW.getValue(),
                                        CrmLeadStatusEnum.FOLLOWED.getValue())
                                .set(CrmLead::getStatus, CrmLeadStatusEnum.TRIALED.getValue()));
                if (leadUpdated > 0) {
                    advancedLeadCount++;
                }
                // 约试听时线索可能已被置为「已试听」，结果仍需录入，提醒照发
                if (StrUtil.isNotBlank(lead.getFollowUserId())) {
                    notifyUserIdList.add(lead.getFollowUserId());
                }
                if (messageLineList.size() < MESSAGE_LIMIT) {
                    messageLineList.add("· " + lead.getName() + "（" + StrUtil.blankToDefault(lead.getParentPhone(), "无电话") + "）");
                }
            }
        }
        if (notify && !notifyUserIdList.isEmpty()) {
            try {
                String messageId = devMessageApi.sendMessageWithContentReceipt(
                        notifyUserIdList.stream().distinct().toList(),
                        "试听已完成，请跟进转化",
                        "以下线索的试听时间已过，预约已置为「已完成」，请录入试听结果并推进转化：\n"
                                + String.join("\n", messageLineList));
                for (CrmTrialAppointment trial : advancedTrials) {
                    context.success("TRIAL", trial.getId(), "NOTIFY", messageId, null);
                }
            } catch (Exception e) {
                for (CrmTrialAppointment trial : advancedTrials) {
                    context.fail("TRIAL", trial.getId(), "NOTIFY", "推进成功但通知失败：" + e.getMessage());
                }
                return AutomationResult.fail("已推进 " + advancedTrials.size() + " 场试听，但通知失败："
                        + e.getMessage());
            }
        }
        String message = "已推进 " + advancedTrials.size() + " 场试听、更新 " + advancedLeadCount + " 条线索";
        return AutomationResult.ok(message, advancedTrials.size(),
                detailList.isEmpty() ? null : String.join(",", detailList));
    }
}
