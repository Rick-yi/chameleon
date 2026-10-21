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
package vip.xiaonuo.course.modular.courseclassattend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.course.core.util.AttendDeductRule;
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.service.CourseService;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.enums.CourseAttendStatusEnum;
import vip.xiaonuo.course.modular.courseclassattend.mapper.CourseClassAttendMapper;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendConfirmParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendIdParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendItemParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendPageParam;
import vip.xiaonuo.course.modular.courseclassattend.service.CourseClassAttendService;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.enums.CourseScheduleStatusEnum;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;
import vip.xiaonuo.student.api.StudentLessonApi;
import vip.xiaonuo.student.api.StudentApi;
import vip.xiaonuo.student.api.StudentNotifyApi;
import vip.xiaonuo.student.api.dto.LessonBalanceDTO;
import vip.xiaonuo.student.api.dto.StudentBriefDTO;
import vip.xiaonuo.student.api.enums.LessonAccountStatusEnum;
import vip.xiaonuo.student.api.enums.StudentStatusEnum;
import vip.xiaonuo.course.api.dto.AttendConfirmResultDTO;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.service.CourseClassStudentService;
import vip.xiaonuo.student.api.StudentOrderApi;
import vip.xiaonuo.student.api.dto.StudentOrderRefDTO;
import vip.xiaonuo.student.api.pojo.LessonDeductParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 点名记录Service接口实现类
 * <p>
 * 点名确认 = 保存点名记录 + 调用课时账务API扣课（同一事务）；
 * 到课/缺勤按课程每课课时数扣减，请假不扣；任一学员余额不足整体回滚。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseClassAttendServiceImpl extends ServiceImpl<CourseClassAttendMapper, CourseClassAttend> implements CourseClassAttendService {

    @Resource
    private CourseScheduleService courseScheduleService;

    @Resource
    private StudentOrderApi studentOrderApi;

    @Resource
    private CourseApi courseApi;

    @Resource
    private CourseClassService courseClassService;

    @Resource
    private CourseService courseService;

    @Resource
    private StudentLessonApi studentLessonApi;

    @Resource
    private CourseScheduleMapper courseScheduleMapper;

    @Resource
    private CourseClassMapper courseClassMapper;

    @Resource
    private BizEventApi bizEventApi;

    @Resource
    private StudentApi studentApi;

    @Resource
    private CourseClassStudentService courseClassStudentService;

    @Resource
    private StudentNotifyApi studentNotifyApi;

    @Override
    public Page<CourseClassAttend> page(CourseClassAttendPageParam courseClassAttendPageParam) {
        QueryWrapper<CourseClassAttend> queryWrapper = new QueryWrapper<CourseClassAttend>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(courseClassAttendPageParam.getScheduleId()), CourseClassAttend::getScheduleId, courseClassAttendPageParam.getScheduleId())
                .eq(ObjectUtil.isNotEmpty(courseClassAttendPageParam.getStudentId()), CourseClassAttend::getStudentId, courseClassAttendPageParam.getStudentId())
                .orderByDesc(CourseClassAttend::getCreateTime);
        Page<CourseClassAttend> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillScheduleInfo(page.getRecords());
        return page;
    }

    /**
     * 填充出勤记录对应的上课日期、时间段与班级
     */
    private void fillScheduleInfo(List<CourseClassAttend> attendList) {
        if (ObjectUtil.isEmpty(attendList)) {
            return;
        }
        List<String> scheduleIdList = attendList.stream().map(CourseClassAttend::getScheduleId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(scheduleIdList)) {
            return;
        }
        Map<String, CourseSchedule> scheduleMap = new HashMap<>();
        courseScheduleMapper.selectBatchIds(scheduleIdList).forEach(s -> scheduleMap.put(s.getId(), s));
        Map<String, String> classNameMap = new HashMap<>();
        List<String> classIdList = scheduleMap.values().stream().map(CourseSchedule::getClassId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(classIdList)) {
            courseClassMapper.selectBatchIds(classIdList).forEach(c -> classNameMap.put(c.getId(), c.getName()));
        }
        for (CourseClassAttend attend : attendList) {
            CourseSchedule schedule = scheduleMap.get(attend.getScheduleId());
            if (ObjectUtil.isEmpty(schedule)) {
                continue;
            }
            attend.setScheduleDate(schedule.getScheduleDate() == null ? null : DateUtil.formatDate(schedule.getScheduleDate()));
            attend.setTimeText(timeText(schedule));
            attend.setClassName(classNameMap.get(schedule.getClassId()));
        }
    }

    /**
     * 课节时间段文案
     */
    private String timeText(CourseSchedule schedule) {
        if (schedule.getStartTime() == null) {
            return null;
        }
        String start = String.valueOf(schedule.getStartTime());
        String end = schedule.getEndTime() == null ? "" : String.valueOf(schedule.getEndTime());
        if (start.length() > 5) start = start.substring(0, 5);
        if (end.length() > 5) end = end.substring(0, 5);
        return StrUtil.isBlank(end) ? start : start + "-" + end;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AttendConfirmResultDTO confirm(CourseClassAttendConfirmParam courseClassAttendConfirmParam) {
        AttendConfirmResultDTO result = new AttendConfirmResultDTO();
        CourseSchedule schedule = courseScheduleService.queryEntity(courseClassAttendConfirmParam.getScheduleId());
        if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(schedule.getStatus())) {
            throw new CommonException("排课状态【{}】不允许点名", schedule.getStatus());
        }
        // 每课扣课时数（课程配置，默认1）
        // 排课可以挂班级，也可以是「直接排课」（不挂班级，课程直接取自排课）
        CourseClass courseClass = null;
        Course course;
        if (StrUtil.isNotBlank(schedule.getClassId())) {
            courseClass = courseClassService.queryEntity(schedule.getClassId());
            course = courseService.queryEntity(courseClass.getCourseId());
        } else {
            course = courseService.queryEntity(schedule.getCourseId());
        }
        // 扣课规则（课程配置）：到课必扣；缺勤、请假是否扣课可配置
        boolean absentDeduct = course.getAbsentDeduct() == null || course.getAbsentDeduct();
        boolean leaveDeduct = Boolean.TRUE.equals(course.getLeaveDeduct());
        BigDecimal lessonsPerClass = ObjectUtil.isNotEmpty(course.getLessonsPerClass())
                ? course.getLessonsPerClass() : BigDecimal.ONE;
        String loginUserId = StpUtil.getLoginIdAsString();
        // 已点名学员（唯一索引幂等，跳过重复）
        List<String> itemStudentIdList = courseClassAttendConfirmParam.getAttendList().stream()
                .map(CourseClassAttendItemParam::getStudentId).collect(Collectors.toList());
        // 学员所购课包（课包维度的扣课规则优先于课程配置）
        Map<String, String> studentPackageMap = new HashMap<>();
        for (StudentOrderRefDTO orderRef : studentOrderApi.listPaidOrderRefs(itemStudentIdList)) {
            studentPackageMap.putIfAbsent(orderRef.getStudentId() + "|" + orderRef.getCourseId(),
                    orderRef.getCoursePackageId());
        }
        List<String> existStudentIdList = this.list(new LambdaQueryWrapper<CourseClassAttend>()
                        .eq(CourseClassAttend::getScheduleId, schedule.getId())
                        .in(CourseClassAttend::getStudentId, itemStudentIdList))
                .stream().map(CourseClassAttend::getStudentId).collect(Collectors.toList());
        // 校验：只能给「本节课名单」内的学员点名（防止越权点名他人并扣其课时）
        // 名单 = 班级在读学员 ∪ 排课直接指定学员（老师带一群学生、无固定班级时）
        List<String> rosterStudentIdList = courseScheduleService.listRosterStudentIds(schedule.getId());
        Map<String, String> classStudentNameMap = new HashMap<>();
        for (String rosterStudentId : rosterStudentIdList) {
            classStudentNameMap.put(rosterStudentId, rosterStudentId);
        }
        List<StudentBriefDTO> studentBriefList = studentApi.listStudentBrief(itemStudentIdList);
        Map<String, StudentBriefDTO> studentBriefMap = new HashMap<>();
        for (StudentBriefDTO brief : studentBriefList) {
            studentBriefMap.put(brief.getId(), brief);
        }
        for (String studentId : itemStudentIdList) {
            if (!classStudentNameMap.containsKey(studentId)) {
                StudentBriefDTO brief = studentBriefMap.get(studentId);
                throw new CommonException("学员【{}】不在该班级，无法点名", brief == null ? studentId : brief.getName());
            }
            StudentBriefDTO brief = studentBriefMap.get(studentId);
            if (brief != null && !StudentStatusEnum.STUDYING.getValue().equals(brief.getStatus())) {
                throw new CommonException("学员【{}】当前状态【{}】，不能点名消课", brief.getName(), brief.getStatus());
            }
        }
        // 课时余额（用于判断哪些学员本次无法消课，不影响其他学员）
        Map<String, LessonBalanceDTO> balanceMap = new HashMap<>();
        for (LessonBalanceDTO balance : studentLessonApi.listLessonBalance(itemStudentIdList, course.getId())) {
            balanceMap.put(balance.getStudentId(), balance);
        }
        List<CourseClassAttend> attendList = new ArrayList<>();
        List<LessonDeductParam> deductParamList = new ArrayList<>();
        for (CourseClassAttendItemParam item : courseClassAttendConfirmParam.getAttendList()) {
            if (existStudentIdList.contains(item.getStudentId())) {
                continue;
            }
            // 生效的扣课规则：课包优先，课程兜底
            BigDecimal itemLessonsPerClass = lessonsPerClass;
            boolean itemLeaveDeduct = leaveDeduct;
            boolean itemAbsentDeduct = absentDeduct;
            String packageId = studentPackageMap.get(item.getStudentId() + "|" + course.getId());
            if (StrUtil.isNotBlank(packageId)) {
                JSONObject packageInfo = courseApi.getCoursePackageInfo(packageId);
                if (packageInfo != null) {
                    BigDecimal packageLessons = packageInfo.getBigDecimal("lessonsPerClass");
                    if (packageLessons != null) {
                        itemLessonsPerClass = packageLessons;
                    }
                    Object leaveValue = packageInfo.get("leaveDeduct");
                    if (leaveValue != null) {
                        itemLeaveDeduct = Boolean.valueOf(leaveValue.toString());
                    }
                    Object absentValue = packageInfo.get("absentDeduct");
                    if (absentValue != null) {
                        itemAbsentDeduct = Boolean.valueOf(absentValue.toString());
                    }
                }
            }
            CourseClassAttend attend = new CourseClassAttend();
            attend.setScheduleId(schedule.getId());
            attend.setStudentId(item.getStudentId());
            attend.setAttendStatus(item.getAttendStatus());
            attend.setAttendUser(loginUserId);
            attend.setAttendTime(new Date());
            // 按生效规则计算应扣课时（课包优先、课程兜底）
            BigDecimal deductLessons = AttendDeductRule.deductLessons(itemLessonsPerClass, itemLeaveDeduct,
                    itemAbsentDeduct, item.getAttendStatus());
            // 余额判断：课时不足/无账户/账户冻结 → 本次不消课，但保留出勤记录，不影响其他学员
            String skipReason = null;
            if (deductLessons.compareTo(BigDecimal.ZERO) > 0) {
                LessonBalanceDTO balance = balanceMap.get(item.getStudentId());
                if (balance == null) {
                    skipReason = "未购买该课程课包，无课时可消";
                } else if (!LessonAccountStatusEnum.NORMAL.getValue().equals(balance.getStatus())) {
                    skipReason = "课时账户状态异常（" + balance.getStatus() + "）";
                } else if (balance.getRemainLessons() == null
                        || balance.getRemainLessons().compareTo(deductLessons) < 0) {
                    skipReason = "剩余课时不足（剩余 " + balance.getRemainLessons() + "，本次需 " + deductLessons + "）";
                }
            }
            if (skipReason != null) {
                attend.setLessonsDeducted(BigDecimal.ZERO);
                attend.setRemark("未消课：" + skipReason);
                StudentBriefDTO brief = studentBriefMap.get(item.getStudentId());
                result.getSkippedList().add(new AttendConfirmResultDTO.Skipped(item.getStudentId(),
                        brief == null ? item.getStudentId() : brief.getName(), skipReason));
            } else {
                attend.setLessonsDeducted(deductLessons);
                if (deductLessons.compareTo(BigDecimal.ZERO) > 0) {
                    LessonDeductParam deductParam = new LessonDeductParam();
                    deductParam.setStudentId(item.getStudentId());
                    deductParam.setCourseId(course.getId());
                    deductParam.setScheduleId(schedule.getId());
                    deductParam.setLessons(deductLessons);
                    deductParam.setAttendStatus(item.getAttendStatus());
                    deductParamList.add(deductParam);
                    result.setDeductCount(result.getDeductCount() + 1);
                }
            }
            attendList.add(attend);
        }
        result.setTotalCount(attendList.size());
        if (ObjectUtil.isNotEmpty(attendList)) {
            this.saveBatch(attendList);
        }
        // 扣课联动（同一事务；余额不足的学员已在上面跳过，不会影响整班）
        if (ObjectUtil.isNotEmpty(deductParamList)) {
            studentLessonApi.deductLessons(deductParamList);
        }
        // 埋点：点名消课
        if (ObjectUtil.isNotEmpty(attendList)) {
            bizEventApi.log(BizEventCodeEnum.ATTEND_CONFIRM.getValue(), "schedule", schedule.getId(),
                    courseClass == null ? "直接排课" : courseClass.getName(),
                    Map.of("人数", String.valueOf(attendList.size()), "扣课人次", String.valueOf(deductParamList.size())));
        }
        // 消课通知落库（v1 站内记录，家长端/订阅消息在 M4 消费）
        for (CourseClassAttend attend : attendList) {
            if (ObjectUtil.isNotEmpty(attend.getLessonsDeducted())
                    && attend.getLessonsDeducted().compareTo(BigDecimal.ZERO) > 0) {
                studentNotifyApi.log("ATTEND", "PARENT", attend.getStudentId(), attend.getStudentId(),
                        "消课通知", "学员于" + DateUtil.formatDate(schedule.getScheduleDate())
                                + "完成课程【" + course.getName() + "】，扣减"
                                + attend.getLessonsDeducted().stripTrailingZeros().toPlainString() + "课时",
                        "IN_APP");
            }
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(CourseClassAttendIdParam courseClassAttendIdParam) {
        CourseClassAttend cancelAttend = this.queryEntity(courseClassAttendIdParam.getId());
        bizEventApi.log(BizEventCodeEnum.ATTEND_CANCEL.getValue(), "schedule", cancelAttend.getScheduleId(), null, null);
        CourseClassAttend attend = this.queryEntity(courseClassAttendIdParam.getId());
        // 扣过课时则回补
        if (ObjectUtil.isNotEmpty(attend.getLessonsDeducted())
                && attend.getLessonsDeducted().compareTo(BigDecimal.ZERO) > 0) {
            studentLessonApi.restoreLessons(attend.getScheduleId(), attend.getStudentId());
        }
        this.removeById(attend.getId());
    }

    @Override
    public CourseClassAttend queryEntity(String id) {
        CourseClassAttend attend = this.getById(id);
        if (ObjectUtil.isEmpty(attend)) {
            throw new CommonException("点名记录不存在，id值为：{}", id);
        }
        return attend;
    }
}
