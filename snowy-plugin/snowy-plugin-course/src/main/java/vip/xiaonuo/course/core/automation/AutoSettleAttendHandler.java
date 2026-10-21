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
package vip.xiaonuo.course.core.automation;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.mapper.CourseClassAttendMapper;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendConfirmParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendItemParam;
import vip.xiaonuo.course.modular.courseclassattend.service.CourseClassAttendService;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.enums.CourseScheduleStatusEnum;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;
import vip.xiaonuo.dev.api.DevMessageApi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动化规则：课节自动结算
 * <p>
 * 老师上完课常常要晚上才想起点名。这里在「课节结束 + 宽限期」之后，
 * 若该课节仍没有任何点名记录，则自动按缺勤点名并按课程规则扣课，
 * 同时给任课老师发一条站内信说明已自动结算（仍可手工撤销并回补课时）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Component
public class AutoSettleAttendHandler implements BizAutomationHandler {

    /** 规则编码 */
    public static final String RULE_CODE = "AUTO_SETTLE_ATTEND";
    /** 缺勤状态 */
    private static final String ATTEND_STATUS_ABSENT = "ABSENT";

    @Resource
    private CourseScheduleMapper courseScheduleMapper;

    @Resource
    private CourseClassAttendMapper courseClassAttendMapper;

    @Resource
    private CourseScheduleService courseScheduleService;

    @Resource
    private CourseClassAttendService courseClassAttendService;

    @Resource
    private DevMessageApi devMessageApi;

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return "课节自动结算";
    }

    @Override
    public String getRuleDesc() {
        return "课节结束超过宽限时间仍未点名的，自动按缺勤点名并扣减课时，老师不用晚上再回来点名（可随时手工撤销回补）";
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
    public List<AutomationParam> getParamDef() {
        return List.of(
                new AutomationParam("graceMinutes", "宽限时间（分钟）", "NUMBER", "120",
                        "课节结束后等待多久仍未点名才自动结算，建议不小于 60 分钟"),
                new AutomationParam("notifyTeacher", "通知任课老师", "SWITCH", "YES",
                        "自动结算后给任课老师发站内信")
        );
    }

    @Override
    public AutomationResult execute(AutomationContext context) {
        int graceMinutes = context.getIntParam("graceMinutes", 120);
        boolean notifyTeacher = !"NO".equalsIgnoreCase(context.getParam("notifyTeacher", "YES"));
        // 截止时间：现在往前推宽限期
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(graceMinutes);

        // 待结算课节：截止日期之前的正常课节（精确到「日期+下课时间」的判断在下面做）
        List<CourseSchedule> scheduleList = courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue())
                .le(CourseSchedule::getScheduleDate, deadline.toLocalDate().atStartOfDay())
                .orderByAsc(CourseSchedule::getScheduleDate));

        int settledCount = 0;
        List<String> detailList = new ArrayList<>();
        for (CourseSchedule schedule : scheduleList) {
            if (!isEnded(schedule, deadline)) {
                continue;
            }
            // 已有点名记录则跳过（老师已经点过了）
            Long attendCount = courseClassAttendMapper.selectCount(new LambdaQueryWrapper<CourseClassAttend>()
                    .eq(CourseClassAttend::getScheduleId, schedule.getId()));
            if (attendCount != null && attendCount > 0) {
                continue;
            }
            List<String> rosterStudentIdList = courseScheduleService.listRosterStudentIds(schedule.getId());
            if (rosterStudentIdList.isEmpty()) {
                continue;
            }
            try {
                CourseClassAttendConfirmParam confirmParam = new CourseClassAttendConfirmParam();
                confirmParam.setScheduleId(schedule.getId());
                List<CourseClassAttendItemParam> attendList = new ArrayList<>();
                for (String studentId : rosterStudentIdList) {
                    CourseClassAttendItemParam item = new CourseClassAttendItemParam();
                    item.setStudentId(studentId);
                    item.setAttendStatus(ATTEND_STATUS_ABSENT);
                    attendList.add(item);
                }
                confirmParam.setAttendList(attendList);
                courseClassAttendService.confirm(confirmParam);
                settledCount++;
                detailList.add(schedule.getId());
                // 通知任课老师
                if (notifyTeacher && StrUtil.isNotBlank(schedule.getTeacherId())) {
                    devMessageApi.sendMessageWithContent(List.of(schedule.getTeacherId()),
                            "课节已自动结算",
                            "【" + (StrUtil.isBlank(schedule.getClassroom()) ? "未设教室" : schedule.getClassroom())
                                    + "】这节课已超过 " + graceMinutes + " 分钟未点名，系统已按缺勤自动结算并扣除课时，如有异议可在「点名消课」中撤销。");
                }
            } catch (Exception e) {
                log.warn(">>> 课节[{}]自动结算失败：{}", schedule.getId(), e.getMessage());
            }
        }
        String message = settledCount > 0
                ? "已自动结算 " + settledCount + " 节课"
                : "没有需要自动结算的课节";
        return AutomationResult.ok(message, settledCount,
                detailList.isEmpty() ? null : String.join(",", detailList));
    }

    /** 课节是否已结束（课节日期 + 下课时间 <= 截止时间） */
    private boolean isEnded(CourseSchedule schedule, LocalDateTime deadline) {
        if (schedule.getScheduleDate() == null || schedule.getEndTime() == null) {
            return false;
        }
        LocalDate date = schedule.getScheduleDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalTime endTime = schedule.getEndTime();
        LocalDateTime endDateTime = date.atTime(endTime);
        return !endDateTime.isAfter(deadline);
    }
}
