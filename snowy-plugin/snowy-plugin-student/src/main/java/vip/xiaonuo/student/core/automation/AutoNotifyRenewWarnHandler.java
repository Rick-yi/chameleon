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
package vip.xiaonuo.student.core.automation;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.dev.api.DevMessageApi;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;
import vip.xiaonuo.student.modular.studentrenewarn.mapper.StudentRenewWarnMapper;
import vip.xiaonuo.sys.api.SysUserApi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化规则：续费预警提醒
 * <p>
 * 续费预警由每日扫描任务产生，但产生之后往往没人第一时间看到。
 * 这里把「待处理」的续费预警主动推送给机构管理员，避免课时耗尽后才发现。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Component
public class AutoNotifyRenewWarnHandler implements BizAutomationHandler {

    /** 规则编码 */
    public static final String RULE_CODE = "AUTO_NOTIFY_RENEW_WARN";
    /** 机构管理员角色码 */
    private static final String ADMIN_ROLE_CODE = "superAdmin";

    @Resource
    private StudentRenewWarnMapper studentRenewWarnMapper;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Resource
    private DevMessageApi devMessageApi;

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return "续费预警提醒";
    }

    @Override
    public String getRuleDesc() {
        return "有待处理的续费预警时，自动给机构管理员发站内信，别等课时耗尽才发现要续费";
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
    public boolean isNotificationRule() {
        return true;
    }

    @Override
    public int getRuleVersion() {
        return 2;
    }

    @Override
    public List<AutomationParam> getParamDef() {
        return List.of(
                new AutomationParam("repeatHours", "重复提醒间隔（小时）", "NUMBER", "24",
                        "同一条预警提醒过一次后，至少间隔这么久才再次提醒", 1, 720),
                new AutomationParam("notifyRoleCode", "接收人角色码", "TEXT", "superAdmin",
                        "接收提醒的角色码，机构管理员默认为 superAdmin")
        );
    }

    @Override
    public AutomationResult execute(AutomationContext context) {
        int repeatHours = context.getIntParam("repeatHours", 24);
        String roleCode = context.getParam("notifyRoleCode", ADMIN_ROLE_CODE);
        int limit = Math.min(context.remainingQuota(), 200);
        if (limit <= 0) {
            return AutomationResult.ok("今日已达执行上限", 0);
        }
        // 上次提醒时间超过间隔（或从未提醒）且仍待处理
        LocalDateTime notifyBefore = LocalDateTime.now().minusHours(repeatHours);
        List<StudentRenewWarn> warnList = studentRenewWarnMapper.selectList(new LambdaQueryWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING)
                .and(wrapper -> wrapper.isNull(StudentRenewWarn::getLastNotifyTime)
                        .or().lt(StudentRenewWarn::getLastNotifyTime, Date.from(notifyBefore.atZone(java.time.ZoneId.systemDefault()).toInstant())))
                .orderByAsc(StudentRenewWarn::getCreateTime)
                .last("LIMIT " + limit));
        if (warnList.isEmpty()) {
            return AutomationResult.ok("没有需要提醒的续费预警", 0);
        }
        List<String> receiverIdList = adminUserIdList(roleCode);
        if (receiverIdList.isEmpty()) {
            for (StudentRenewWarn warn : warnList) {
                context.skip("RENEW_WARN", warn.getId(), "NOTIFY", "没有找到接收提醒的管理员");
            }
            return AutomationResult.ok("没有找到接收提醒的管理员", 0);
        }
        // 学员名称（预警表里不冗余存名称，这里统一补齐）
        Map<String, String> studentNameMap = new HashMap<>();
        List<String> studentIdList = warnList.stream().map(StudentRenewWarn::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        for (Student student : studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .in(Student::getId, studentIdList))) {
            studentNameMap.put(student.getId(), student.getName());
        }
        String windowKey = "R" + repeatHours + ":" + (System.currentTimeMillis() / (repeatHours * 3600_000L));
        List<StudentRenewWarn> notifyList = new ArrayList<>();
        List<String> contentList = new ArrayList<>();
        for (StudentRenewWarn warn : warnList) {
            boolean holdout = context.isHoldout(warn.getId());
            if (!context.claim("RENEW_WARN", warn.getId(), "NOTIFY", windowKey)) {
                continue;
            }
            if (holdout) {
                context.skip("RENEW_WARN", warn.getId(), "NOTIFY", "命中实验对照组，不发送", "HOLDOUT");
                continue;
            }
            notifyList.add(warn);
            contentList.add("学员 " + StrUtil.blankToDefault(studentNameMap.get(warn.getStudentId()), warn.getStudentId())
                    + "：" + (StudentRenewWarnStatusEnum.REASON_LESSON_LOW.equals(warn.getWarnReason())
                    ? (warn.getRemainLessons() != null ? "剩余 " + warn.getRemainLessons() + " 课时" : "课时不足")
                    : "课包即将到期")
                    + "，请及时跟进续费");
        }
        if (notifyList.isEmpty()) {
            return AutomationResult.ok("本轮没有需要发送的续费预警", 0, "holdout 或窗口内已处理");
        }
        if (context.dryRun()) {
            return AutomationResult.ok("预览：将提醒 " + notifyList.size() + " 条续费预警",
                    notifyList.size(), String.join(",", notifyList.stream().map(StudentRenewWarn::getId).toList()));
        }
        String messageId;
        try {
            messageId = devMessageApi.sendMessageWithContentReceipt(receiverIdList, "续费预警提醒",
                    "共 " + contentList.size() + " 条续费预警待处理：\n" + String.join("\n", contentList));
        } catch (Exception e) {
            for (StudentRenewWarn warn : notifyList) {
                context.fail("RENEW_WARN", warn.getId(), "NOTIFY", "发送失败：" + e.getMessage());
            }
            return AutomationResult.fail("续费预警提醒发送失败：" + e.getMessage());
        }
        // 记录提醒时间，避免重复打扰；回写失败不重发，避免重复触达
        for (StudentRenewWarn warn : notifyList) {
            try {
                StudentRenewWarn update = new StudentRenewWarn();
                update.setId(warn.getId());
                update.setLastNotifyTime(new Date());
                studentRenewWarnMapper.updateById(update);
            } catch (Exception e) {
                log.warn(">>> 续费预警[{}]提醒状态回写失败：{}", warn.getId(), e.getMessage());
            }
            context.success("RENEW_WARN", warn.getId(), "NOTIFY", messageId, null);
        }
        return AutomationResult.ok("已提醒 " + notifyList.size() + " 条续费预警", notifyList.size(),
                String.join(",", notifyList.stream().map(StudentRenewWarn::getId).toList()));
    }

    /** 机构管理员用户id列表 */
    private List<String> adminUserIdList(String roleCode) {
        List<String> resultList = new ArrayList<>();
        List<JSONObject> userList = sysUserApi.listUserOptions(null);
        if (userList == null) {
            return resultList;
        }
        for (JSONObject user : userList) {
            String userId = user.getStr("id");
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            List<String> roleList = sysUserApi.ownRoleCode(userId);
            if (roleList != null && roleList.contains(roleCode)) {
                resultList.add(userId);
            }
        }
        return resultList;
    }
}
