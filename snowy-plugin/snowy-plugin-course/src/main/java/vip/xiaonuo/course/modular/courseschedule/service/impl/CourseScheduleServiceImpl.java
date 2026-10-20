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
package vip.xiaonuo.course.modular.courseschedule.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.mapper.CourseClassAttendMapper;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.service.CourseClassStudentService;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.enums.CourseScheduleStatusEnum;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAddParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleChangeParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleGenerateParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleIdParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseSchedulePageParam;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;
import vip.xiaonuo.course.modular.courseschedulechange.entity.CourseScheduleChange;
import vip.xiaonuo.course.modular.courseschedulechange.service.CourseScheduleChangeService;
import vip.xiaonuo.student.api.StudentNotifyApi;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 排课Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements CourseScheduleService {

    @Resource
    private CourseClassService courseClassService;

    @Resource
    private CourseScheduleChangeService courseScheduleChangeService;

    @Resource
    private CourseClassStudentService courseClassStudentService;

    @Resource
    private StudentNotifyApi studentNotifyApi;

    @Resource
    private CourseClassAttendMapper courseClassAttendMapper;

    @Override
    public Page<CourseSchedule> page(CourseSchedulePageParam courseSchedulePageParam) {
        QueryWrapper<CourseSchedule> queryWrapper = new QueryWrapper<CourseSchedule>().checkSqlInjection();
        // GET 查询参数为字符串日期，解析后再比较
        Date startDate = ObjectUtil.isNotEmpty(courseSchedulePageParam.getStartDate())
                ? cn.hutool.core.date.DateUtil.parse(courseSchedulePageParam.getStartDate()) : null;
        Date endDate = ObjectUtil.isNotEmpty(courseSchedulePageParam.getEndDate())
                ? cn.hutool.core.date.DateUtil.parse(courseSchedulePageParam.getEndDate()) : null;
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(courseSchedulePageParam.getClassId()), CourseSchedule::getClassId, courseSchedulePageParam.getClassId())
                .ge(startDate != null, CourseSchedule::getScheduleDate, startDate)
                .le(endDate != null, CourseSchedule::getScheduleDate, endDate)
                .orderByAsc(CourseSchedule::getScheduleDate)
                .orderByAsc(CourseSchedule::getStartTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseScheduleAddParam courseScheduleAddParam) {
        List<String> conflicts = this.conflictCheck(courseScheduleAddParam.getClassId(), courseScheduleAddParam.getScheduleDate(),
                courseScheduleAddParam.getStartTime(), courseScheduleAddParam.getEndTime(),
                courseScheduleAddParam.getClassroom(), courseScheduleAddParam.getTeacherId(), null);
        if (ObjectUtil.isNotEmpty(conflicts)) {
            throw new CommonException("排课存在冲突：{}", String.join("；", conflicts));
        }
        CourseSchedule courseSchedule = BeanUtil.toBean(courseScheduleAddParam, CourseSchedule.class);
        courseSchedule.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
        this.save(courseSchedule);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void generate(CourseScheduleGenerateParam courseScheduleGenerateParam) {
        // 校验班级及其周课表模板
        CourseClass courseClass = courseClassService.queryEntity(courseScheduleGenerateParam.getClassId());
        if (ObjectUtil.isEmpty(courseClass.getWeekTemplateJson())) {
            throw new CommonException("班级【{}】未配置周课表模板", courseClass.getName());
        }
        JSONArray templateList = JSONUtil.parseArray(courseClass.getWeekTemplateJson());
        if (ObjectUtil.isEmpty(templateList)) {
            throw new CommonException("班级【{}】周课表模板为空", courseClass.getName());
        }
        Integer weekCount = courseScheduleGenerateParam.getWeekCount();
        if (ObjectUtil.isEmpty(weekCount) || weekCount <= 0 || weekCount > 26) {
            throw new CommonException("生成周数需在1-26之间");
        }
        // 以开始日期所在周的周一为基准
        LocalDate startLocalDate = courseScheduleGenerateParam.getStartDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate monday = startLocalDate.with(DayOfWeek.MONDAY);
        List<CourseSchedule> toSaveList = new ArrayList<>();
        List<String> allConflicts = new ArrayList<>();
        for (int week = 0; week < weekCount; week++) {
            for (int i = 0; i < templateList.size(); i++) {
                JSONObject template = templateList.getJSONObject(i);
                int dayOfWeek = template.getInt("dayOfWeek");
                if (dayOfWeek < 1 || dayOfWeek > 7) {
                    throw new CommonException("周课表模板星期配置非法：{}", dayOfWeek);
                }
                LocalDate date = monday.plusWeeks(week).plusDays(dayOfWeek - 1);
                LocalTime startTime = LocalTime.parse(template.getStr("startTime"));
                LocalTime endTime = LocalTime.parse(template.getStr("endTime"));
                String classroom = template.getStr("classroom");
                String teacherId = template.getStr("teacherId");
                Date scheduleDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                // 与已有排课冲突
                List<String> conflicts = this.conflictCheck(courseScheduleGenerateParam.getClassId(), scheduleDate,
                        startTime, endTime, classroom, teacherId, null);
                allConflicts.addAll(conflicts);
                // 与同批生成排课冲突（教室/教师）
                for (CourseSchedule planned : toSaveList) {
                    if (planned.getScheduleDate().equals(scheduleDate)
                            && planned.getStartTime().isBefore(endTime) && planned.getEndTime().isAfter(startTime)) {
                        if (ObjectUtil.isNotEmpty(classroom) && classroom.equals(planned.getClassroom())) {
                            allConflicts.add("教室【" + classroom + "】同时段已有排课（同批生成）");
                        }
                        if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(planned.getTeacherId())) {
                            allConflicts.add("教师同时段已有排课（同批生成）");
                        }
                    }
                }
                CourseSchedule courseSchedule = new CourseSchedule();
                courseSchedule.setClassId(courseScheduleGenerateParam.getClassId());
                courseSchedule.setScheduleDate(scheduleDate);
                courseSchedule.setStartTime(startTime);
                courseSchedule.setEndTime(endTime);
                courseSchedule.setClassroom(classroom);
                courseSchedule.setTeacherId(teacherId);
                courseSchedule.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
                toSaveList.add(courseSchedule);
            }
        }
        // 任一冲突整体失败（不落库）
        if (ObjectUtil.isNotEmpty(allConflicts)) {
            List<String> distinctConflicts = new ArrayList<>(new HashSet<>(allConflicts));
            throw new CommonException("存在排课冲突，未生成：{}", String.join("；", distinctConflicts));
        }
        this.saveBatch(toSaveList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void change(CourseScheduleChangeParam courseScheduleChangeParam) {
        CourseSchedule from = this.queryEntity(courseScheduleChangeParam.getScheduleId());
        if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(from.getStatus())) {
            throw new CommonException("排课当前状态【{}】不允许调课", from.getStatus());
        }
        List<String> conflicts = this.conflictCheck(from.getClassId(), courseScheduleChangeParam.getScheduleDate(),
                courseScheduleChangeParam.getStartTime(), courseScheduleChangeParam.getEndTime(),
                courseScheduleChangeParam.getClassroom(), courseScheduleChangeParam.getTeacherId(), from.getId());
        if (ObjectUtil.isNotEmpty(conflicts)) {
            throw new CommonException("调课存在冲突：{}", String.join("；", conflicts));
        }
        // 原排课置已调（保留历史，供流水/报表追溯）
        from.setStatus(CourseScheduleStatusEnum.CHANGED.getValue());
        this.updateById(from);
        // 生成新排课
        CourseSchedule to = new CourseSchedule();
        to.setClassId(from.getClassId());
        to.setScheduleDate(courseScheduleChangeParam.getScheduleDate());
        to.setStartTime(courseScheduleChangeParam.getStartTime());
        to.setEndTime(courseScheduleChangeParam.getEndTime());
        to.setClassroom(courseScheduleChangeParam.getClassroom());
        to.setTeacherId(courseScheduleChangeParam.getTeacherId());
        to.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
        this.save(to);
        // 调课记录
        CourseScheduleChange change = new CourseScheduleChange();
        change.setFromScheduleId(from.getId());
        change.setToScheduleId(to.getId());
        change.setChangeReason(courseScheduleChangeParam.getChangeReason());
        courseScheduleChangeService.add(change);
        // 调课通知落库
        List<CourseClassStudent> studentList = courseClassStudentService.listByClassId(from.getClassId());
        for (CourseClassStudent classStudent : studentList) {
            studentNotifyApi.log("SCHEDULE_CHANGE", "PARENT", classStudent.getStudentId(), classStudent.getStudentId(),
                    "调课通知", "课程调至" + DateUtil.formatDate(to.getScheduleDate()) + " "
                            + to.getStartTime() + "-" + to.getEndTime()
                            + (ObjectUtil.isNotEmpty(to.getClassroom()) ? "，教室：" + to.getClassroom() : "")
                            + (ObjectUtil.isNotEmpty(courseScheduleChangeParam.getChangeReason())
                            ? "，原因：" + courseScheduleChangeParam.getChangeReason() : ""),
                    "IN_APP");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(CourseScheduleIdParam courseScheduleIdParam) {
        CourseSchedule courseSchedule = this.queryEntity(courseScheduleIdParam.getId());
        if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(courseSchedule.getStatus())) {
            throw new CommonException("排课当前状态【{}】不允许取消", courseSchedule.getStatus());
        }
        // 已产生点名记录的排课不允许取消（先处理点名）
        Long attendCount = courseClassAttendMapper.selectCount(new LambdaQueryWrapper<CourseClassAttend>()
                .eq(CourseClassAttend::getScheduleId, courseSchedule.getId()));
        if (attendCount > 0) {
            throw new CommonException("该排课已点名，请先撤销点名记录再取消");
        }
        courseSchedule.setStatus(CourseScheduleStatusEnum.CANCELED.getValue());
        this.updateById(courseSchedule);
    }

    @Override
    public CourseSchedule detail(CourseScheduleIdParam courseScheduleIdParam) {
        return this.queryEntity(courseScheduleIdParam.getId());
    }

    @Override
    public CourseSchedule queryEntity(String id) {
        CourseSchedule courseSchedule = this.getById(id);
        if (ObjectUtil.isEmpty(courseSchedule)) {
            throw new CommonException("排课不存在，id值为：{}", id);
        }
        return courseSchedule;
    }

    @Override
    public List<String> conflictCheck(String classId, Date scheduleDate, LocalTime startTime, LocalTime endTime,
                                      String classroom, String teacherId, String excludeScheduleId) {
        // 同日同时段重叠的"正常"排课
        LambdaQueryWrapper<CourseSchedule> queryWrapper = new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getScheduleDate, scheduleDate)
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue())
                .lt(CourseSchedule::getStartTime, endTime)
                .gt(CourseSchedule::getEndTime, startTime);
        if (ObjectUtil.isNotEmpty(excludeScheduleId)) {
            queryWrapper.ne(CourseSchedule::getId, excludeScheduleId);
        }
        List<CourseSchedule> existList = this.list(queryWrapper);
        if (ObjectUtil.isEmpty(existList)) {
            return new ArrayList<>();
        }
        Set<String> conflictSet = new HashSet<>();
        for (CourseSchedule exist : existList) {
            if (ObjectUtil.isNotEmpty(classroom) && classroom.equals(exist.getClassroom())) {
                conflictSet.add("教室【" + classroom + "】同时段已有排课");
            }
            if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(exist.getTeacherId())) {
                conflictSet.add("教师同时段已有排课");
            }
            if (classId.equals(exist.getClassId())) {
                conflictSet.add("该班级同时段已有排课");
            }
        }
        return new ArrayList<>(conflictSet);
    }
}
