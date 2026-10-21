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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.service.CourseService;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.enums.CourseAttendStatusEnum;
import vip.xiaonuo.course.modular.courseclassattend.mapper.CourseClassAttendMapper;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendConfirmParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendIdParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendItemParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendPageParam;
import vip.xiaonuo.course.modular.courseclassattend.service.CourseClassAttendService;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.enums.CourseScheduleStatusEnum;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;
import vip.xiaonuo.student.api.StudentLessonApi;
import vip.xiaonuo.student.api.StudentNotifyApi;
import vip.xiaonuo.student.api.pojo.LessonDeductParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private CourseClassService courseClassService;

    @Resource
    private CourseService courseService;

    @Resource
    private StudentLessonApi studentLessonApi;

    @Resource
    private StudentNotifyApi studentNotifyApi;

    @Override
    public Page<CourseClassAttend> page(CourseClassAttendPageParam courseClassAttendPageParam) {
        QueryWrapper<CourseClassAttend> queryWrapper = new QueryWrapper<CourseClassAttend>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(CourseClassAttend::getScheduleId, courseClassAttendPageParam.getScheduleId())
                .orderByAsc(CourseClassAttend::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirm(CourseClassAttendConfirmParam courseClassAttendConfirmParam) {
        CourseSchedule schedule = courseScheduleService.queryEntity(courseClassAttendConfirmParam.getScheduleId());
        if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(schedule.getStatus())) {
            throw new CommonException("排课状态【{}】不允许点名", schedule.getStatus());
        }
        // 每课扣课时数（课程配置，默认1）
        CourseClass courseClass = courseClassService.queryEntity(schedule.getClassId());
        Course course = courseService.queryEntity(courseClass.getCourseId());
        BigDecimal lessonsPerClass = ObjectUtil.isNotEmpty(course.getLessonsPerClass())
                ? course.getLessonsPerClass() : BigDecimal.ONE;
        // 扣课规则（课程配置）：到课必扣；缺勤、请假是否扣课可配置
        boolean absentDeduct = course.getAbsentDeduct() == null || course.getAbsentDeduct();
        boolean leaveDeduct = Boolean.TRUE.equals(course.getLeaveDeduct());
        String loginUserId = StpUtil.getLoginIdAsString();
        // 已点名学员（唯一索引幂等，跳过重复）
        List<String> itemStudentIdList = courseClassAttendConfirmParam.getAttendList().stream()
                .map(CourseClassAttendItemParam::getStudentId).collect(Collectors.toList());
        List<String> existStudentIdList = this.list(new LambdaQueryWrapper<CourseClassAttend>()
                        .eq(CourseClassAttend::getScheduleId, schedule.getId())
                        .in(CourseClassAttend::getStudentId, itemStudentIdList))
                .stream().map(CourseClassAttend::getStudentId).collect(Collectors.toList());
        List<CourseClassAttend> attendList = new ArrayList<>();
        List<LessonDeductParam> deductParamList = new ArrayList<>();
        for (CourseClassAttendItemParam item : courseClassAttendConfirmParam.getAttendList()) {
            if (existStudentIdList.contains(item.getStudentId())) {
                continue;
            }
            CourseClassAttend attend = new CourseClassAttend();
            attend.setScheduleId(schedule.getId());
            attend.setStudentId(item.getStudentId());
            attend.setAttendStatus(item.getAttendStatus());
            attend.setAttendUser(loginUserId);
            attend.setAttendTime(new Date());
            // 按课程配置决定是否扣课（到课必扣，缺勤/请假可配置）
            boolean shouldDeduct = CourseAttendStatusEnum.ATTEND.getValue().equals(item.getAttendStatus())
                    || (CourseAttendStatusEnum.ABSENT.getValue().equals(item.getAttendStatus()) && absentDeduct)
                    || (CourseAttendStatusEnum.LEAVE.getValue().equals(item.getAttendStatus()) && leaveDeduct);
            if (shouldDeduct) {
                attend.setLessonsDeducted(lessonsPerClass);
                LessonDeductParam deductParam = new LessonDeductParam();
                deductParam.setStudentId(item.getStudentId());
                deductParam.setCourseId(course.getId());
                deductParam.setScheduleId(schedule.getId());
                deductParam.setLessons(lessonsPerClass);
                deductParam.setAttendStatus(item.getAttendStatus());
                deductParamList.add(deductParam);
            } else {
                attend.setLessonsDeducted(BigDecimal.ZERO);
            }
            attendList.add(attend);
        }
        if (ObjectUtil.isNotEmpty(attendList)) {
            this.saveBatch(attendList);
        }
        // 扣课联动（同一事务：任一学员余额不足 → 抛异常 → 点名记录一并回滚）
        if (ObjectUtil.isNotEmpty(deductParamList)) {
            studentLessonApi.deductLessons(deductParamList);
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
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(CourseClassAttendIdParam courseClassAttendIdParam) {
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
