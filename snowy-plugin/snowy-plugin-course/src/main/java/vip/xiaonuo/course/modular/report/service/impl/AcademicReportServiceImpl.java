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
package vip.xiaonuo.course.modular.report.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.course.api.dto.AcademicAttendanceDTO;
import vip.xiaonuo.course.api.dto.AcademicClassFillDTO;
import vip.xiaonuo.course.api.dto.AcademicTeacherWorkloadDTO;
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.mapper.CourseMapper;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.enums.CourseAttendStatusEnum;
import vip.xiaonuo.course.modular.courseclassattend.mapper.CourseClassAttendMapper;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.mapper.CourseClassStudentMapper;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.enums.CourseScheduleStatusEnum;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;
import vip.xiaonuo.course.modular.report.service.AcademicReportService;
import vip.xiaonuo.student.api.StudentReportApi;
import vip.xiaonuo.student.api.dto.AcademicLessonProgressDTO;
import vip.xiaonuo.student.api.dto.AcademicRiskStudentDTO;
import vip.xiaonuo.student.api.param.AcademicReportQueryParam;
import vip.xiaonuo.sys.api.SysOrgApi;
import vip.xiaonuo.sys.api.SysUserApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教务报表Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class AcademicReportServiceImpl implements AcademicReportService {

    /** 默认返回条数 */
    private static final int DEFAULT_LIMIT = 100;

    @Resource
    private CourseClassMapper courseClassMapper;

    @Resource
    private CourseClassStudentMapper courseClassStudentMapper;

    @Resource
    private CourseScheduleMapper courseScheduleMapper;

    @Resource
    private CourseClassAttendMapper courseClassAttendMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private StudentReportApi studentReportApi;

    @Resource
    private SysUserApi sysUserApi;

    @Resource
    private SysOrgApi sysOrgApi;

    @Override
    public AcademicAttendanceDTO attendance(AcademicReportQueryParam param) {
        Date start = resolveStart(param);
        Date end = resolveEnd(param);
        List<CourseClass> classList = queryClassList(param);
        AcademicAttendanceDTO result = new AcademicAttendanceDTO();
        if (ObjectUtil.isEmpty(classList)) {
            return result;
        }
        List<String> classIdList = classList.stream().map(CourseClass::getId).collect(Collectors.toList());
        List<CourseSchedule> scheduleList = queryScheduleList(classIdList, param.getTeacherId(), start, end);
        Map<String, Long> classStudentCountMap = studyingCountMap(classIdList);
        long expectedCount = 0L;
        for (CourseSchedule schedule : scheduleList) {
            expectedCount += classStudentCountMap.getOrDefault(schedule.getClassId(), 0L);
        }
        result.setExpectedCount(expectedCount);
        if (ObjectUtil.isEmpty(scheduleList)) {
            return result;
        }
        List<String> scheduleIdList = scheduleList.stream().map(CourseSchedule::getId).collect(Collectors.toList());
        List<CourseClassAttend> attendList = courseClassAttendMapper.selectList(
                new LambdaQueryWrapper<CourseClassAttend>().in(CourseClassAttend::getScheduleId, scheduleIdList));
        long attendCount = 0L;
        long leaveCount = 0L;
        long absentCount = 0L;
        for (CourseClassAttend attend : attendList) {
            if (CourseAttendStatusEnum.ATTEND.getValue().equals(attend.getAttendStatus())) {
                attendCount++;
            } else if (CourseAttendStatusEnum.LEAVE.getValue().equals(attend.getAttendStatus())) {
                leaveCount++;
            } else if (CourseAttendStatusEnum.ABSENT.getValue().equals(attend.getAttendStatus())) {
                absentCount++;
            }
        }
        result.setAttendCount(attendCount);
        result.setLeaveCount(leaveCount);
        result.setAbsentCount(absentCount);
        long markedCount = attendCount + leaveCount + absentCount;
        result.setAttendRate(percent(attendCount, markedCount));
        result.setAttendanceRate(percent(attendCount, expectedCount));
        return result;
    }

    @Override
    public List<AcademicClassFillDTO> classFill(AcademicReportQueryParam param) {
        List<CourseClass> classList = queryClassList(param);
        if (ObjectUtil.isEmpty(classList)) {
            return new ArrayList<>();
        }
        List<String> classIdList = classList.stream().map(CourseClass::getId).collect(Collectors.toList());
        Map<String, Long> studyingMap = studyingCountMap(classIdList);
        Map<String, String> courseNameMap = loadCourseNameMap(classList.stream()
                .map(CourseClass::getCourseId).collect(Collectors.toList()));
        Map<String, String> teacherNameMap = loadUserNameMap(classList.stream()
                .map(CourseClass::getTeacherId).collect(Collectors.toList()));
        Map<String, String> campusNameMap = loadOrgNameMap(classList.stream()
                .map(CourseClass::getCampusId).collect(Collectors.toList()));
        List<AcademicClassFillDTO> resultList = new ArrayList<>();
        for (CourseClass courseClass : classList) {
            AcademicClassFillDTO dto = new AcademicClassFillDTO();
            dto.setClassId(courseClass.getId());
            dto.setClassName(courseClass.getName());
            dto.setCourseId(courseClass.getCourseId());
            dto.setCourseName(courseNameMap.get(courseClass.getCourseId()));
            dto.setCampusId(courseClass.getCampusId());
            dto.setCampusName(campusNameMap.get(courseClass.getCampusId()));
            dto.setTeacherId(courseClass.getTeacherId());
            dto.setTeacherName(teacherNameMap.get(courseClass.getTeacherId()));
            dto.setCapacity(ObjectUtil.defaultIfNull(courseClass.getCapacity(), 0));
            long studyingCount = studyingMap.getOrDefault(courseClass.getId(), 0L);
            dto.setStudyingCount(studyingCount);
            dto.setFillRate(percent(studyingCount, dto.getCapacity()));
            resultList.add(dto);
        }
        resultList.sort((a, b) -> b.getFillRate().compareTo(a.getFillRate()));
        return limit(resultList, param.getLimit());
    }

    @Override
    public List<AcademicTeacherWorkloadDTO> teacherWorkload(AcademicReportQueryParam param) {
        Date start = resolveStart(param);
        Date end = resolveEnd(param);
        List<CourseClass> classList = queryClassList(param);
        if (ObjectUtil.isEmpty(classList)) {
            return new ArrayList<>();
        }
        List<String> classIdList = classList.stream().map(CourseClass::getId).collect(Collectors.toList());
        List<CourseSchedule> scheduleList = queryScheduleList(classIdList, param.getTeacherId(), start, end);
        if (ObjectUtil.isEmpty(scheduleList)) {
            return new ArrayList<>();
        }
        Map<String, String> classTeacherMap = classList.stream()
                .filter(item -> StrUtil.isNotBlank(item.getTeacherId()))
                .collect(Collectors.toMap(CourseClass::getId, CourseClass::getTeacherId, (a, b) -> a));
        Map<String, String> scheduleTeacherMap = new HashMap<>();
        Map<String, AcademicTeacherWorkloadDTO> workloadMap = new LinkedHashMap<>();
        for (CourseSchedule schedule : scheduleList) {
            String teacherId = StrUtil.blankToDefault(schedule.getTeacherId(),
                    classTeacherMap.get(schedule.getClassId()));
            if (StrUtil.isBlank(teacherId)) {
                continue;
            }
            scheduleTeacherMap.put(schedule.getId(), teacherId);
            AcademicTeacherWorkloadDTO dto = workloadMap.computeIfAbsent(teacherId, key -> {
                AcademicTeacherWorkloadDTO item = new AcademicTeacherWorkloadDTO();
                item.setTeacherId(key);
                return item;
            });
            dto.setScheduleCount(dto.getScheduleCount() + 1);
            dto.setPlannedLessons(dto.getPlannedLessons().add(BigDecimal.ONE));
        }
        if (workloadMap.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> scheduleIdList = new ArrayList<>(scheduleTeacherMap.keySet());
        List<CourseClassAttend> attendList = courseClassAttendMapper.selectList(
                new LambdaQueryWrapper<CourseClassAttend>().in(CourseClassAttend::getScheduleId, scheduleIdList));
        for (CourseClassAttend attend : attendList) {
            if (!CourseAttendStatusEnum.ATTEND.getValue().equals(attend.getAttendStatus())) {
                continue;
            }
            String teacherId = scheduleTeacherMap.get(attend.getScheduleId());
            AcademicTeacherWorkloadDTO dto = workloadMap.get(teacherId);
            if (dto == null) {
                continue;
            }
            dto.setAttendCount(dto.getAttendCount() + 1);
            dto.setActualLessons(dto.getActualLessons().add(
                    ObjectUtil.defaultIfNull(attend.getLessonsDeducted(), BigDecimal.ONE)));
        }
        Map<String, String> teacherNameMap = loadUserNameMap(new ArrayList<>(workloadMap.keySet()));
        List<AcademicTeacherWorkloadDTO> resultList = new ArrayList<>(workloadMap.values());
        resultList.forEach(dto -> dto.setTeacherName(teacherNameMap.get(dto.getTeacherId())));
        resultList.sort((a, b) -> b.getActualLessons().compareTo(a.getActualLessons()));
        return limit(resultList, param.getLimit());
    }

    @Override
    public List<AcademicLessonProgressDTO> lessonProgress(AcademicReportQueryParam param) {
        return studentReportApi.lessonProgress(param);
    }

    @Override
    public List<AcademicRiskStudentDTO> riskStudents(AcademicReportQueryParam param) {
        return studentReportApi.riskStudents(param);
    }

    @Override
    public void export(AcademicReportQueryParam param, String type, HttpServletResponse response) throws IOException {
        String exportType = StrUtil.blankToDefault(type, "ATTENDANCE").toUpperCase();
        String sheetName;
        List<List<String>> headList = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        switch (exportType) {
            case "ATTENDANCE" -> {
                sheetName = "出勤汇总";
                headList.add(List.of("到课人次"));
                headList.add(List.of("请假人次"));
                headList.add(List.of("缺勤人次"));
                headList.add(List.of("应到人次"));
                headList.add(List.of("到课率(%)"));
                headList.add(List.of("出勤率(%)"));
                AcademicAttendanceDTO dto = attendance(param);
                rowList.add(List.of(String.valueOf(dto.getAttendCount()), String.valueOf(dto.getLeaveCount()),
                        String.valueOf(dto.getAbsentCount()), String.valueOf(dto.getExpectedCount()),
                        num(dto.getAttendRate()), num(dto.getAttendanceRate())));
            }
            case "CLASS_FILL" -> {
                sheetName = "班级满班率";
                headList.add(List.of("班级"));
                headList.add(List.of("课程"));
                headList.add(List.of("校区"));
                headList.add(List.of("教师"));
                headList.add(List.of("容量"));
                headList.add(List.of("在学人数"));
                headList.add(List.of("满班率(%)"));
                for (AcademicClassFillDTO dto : classFill(param)) {
                    rowList.add(List.of(text(dto.getClassName()), text(dto.getCourseName()), text(dto.getCampusName()),
                            text(dto.getTeacherName()), String.valueOf(dto.getCapacity()), String.valueOf(dto.getStudyingCount()),
                            num(dto.getFillRate())));
                }
            }
            case "TEACHER" -> {
                sheetName = "教师课时";
                headList.add(List.of("教师"));
                headList.add(List.of("排课次数"));
                headList.add(List.of("计划课时"));
                headList.add(List.of("实际课消课时"));
                headList.add(List.of("到课人次"));
                for (AcademicTeacherWorkloadDTO dto : teacherWorkload(param)) {
                    rowList.add(List.of(text(dto.getTeacherName()), String.valueOf(dto.getScheduleCount()),
                            num(dto.getPlannedLessons()), num(dto.getActualLessons()), String.valueOf(dto.getAttendCount())));
                }
            }
            case "LESSON_PROGRESS" -> {
                sheetName = "学员课消进度";
                headList.add(List.of("学员"));
                headList.add(List.of("课程"));
                headList.add(List.of("总课时"));
                headList.add(List.of("已消课时"));
                headList.add(List.of("剩余课时"));
                headList.add(List.of("课消进度(%)"));
                headList.add(List.of("到期时间"));
                headList.add(List.of("状态"));
                for (AcademicLessonProgressDTO dto : lessonProgress(param)) {
                    rowList.add(List.of(text(dto.getStudentName()), text(dto.getCourseName()), num(dto.getTotalLessons()),
                            num(dto.getUsedLessons()), num(dto.getRemainLessons()), num(dto.getProgressRate()),
                            time(dto.getExpireTime()), text(dto.getStatus())));
                }
            }
            case "RISK_STUDENT" -> {
                sheetName = "风险学员";
                headList.add(List.of("学员"));
                headList.add(List.of("风险类型"));
                headList.add(List.of("课程"));
                headList.add(List.of("剩余课时"));
                headList.add(List.of("到期时间"));
                headList.add(List.of("最近消课"));
                headList.add(List.of("未消课天数"));
                headList.add(List.of("说明"));
                for (AcademicRiskStudentDTO dto : riskStudents(param)) {
                    rowList.add(List.of(text(dto.getStudentName()), text(dto.getRiskType()), text(dto.getCourseName()),
                            num(dto.getRemainLessons()), time(dto.getExpireTime()), time(dto.getLastAttendTime()),
                            dto.getDaysWithoutAttend() == null ? "" : String.valueOf(dto.getDaysWithoutAttend()),
                            text(dto.getRiskDesc())));
                }
            }
            default -> throw new CommonException("不支持的导出类型：{}", exportType);
        }
        if (rowList.isEmpty()) {
            throw new CommonException("无数据可导出");
        }
        File file = FileUtil.file(FileUtil.getTmpDir(), "教务报表_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
        try {
            CommonExcelUtil.write(file, sheetName, headList, rowList);
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String num(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String time(Date value) {
        return value == null ? "" : DateUtil.formatDateTime(value);
    }

    /**
     * 班级查询条件
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private List<CourseClass> queryClassList(AcademicReportQueryParam param) {
        return courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                .eq(StrUtil.isNotBlank(param.getCampusId()), CourseClass::getCampusId, param.getCampusId())
                .eq(StrUtil.isNotBlank(param.getCourseId()), CourseClass::getCourseId, param.getCourseId())
                .eq(StrUtil.isNotBlank(param.getClassId()), CourseClass::getId, param.getClassId())
                .eq(StrUtil.isNotBlank(param.getTeacherId()), CourseClass::getTeacherId, param.getTeacherId())
                .orderByAsc(CourseClass::getName));
    }

    /**
     * 排课查询条件
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private List<CourseSchedule> queryScheduleList(List<String> classIdList, String teacherId, Date start, Date end) {
        return courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                .in(CourseSchedule::getClassId, classIdList)
                .eq(StrUtil.isNotBlank(teacherId), CourseSchedule::getTeacherId, teacherId)
                .ne(CourseSchedule::getStatus, CourseScheduleStatusEnum.CANCELED.getValue())
                .ge(CourseSchedule::getScheduleDate, DateUtil.beginOfDay(start))
                .le(CourseSchedule::getScheduleDate, DateUtil.endOfDay(end))
                .orderByAsc(CourseSchedule::getScheduleDate));
    }

    /**
     * 班级在学人数
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, Long> studyingCountMap(List<String> classIdList) {
        List<CourseClassStudent> classStudentList = courseClassStudentMapper.selectList(
                new LambdaQueryWrapper<CourseClassStudent>()
                        .in(CourseClassStudent::getClassId, classIdList)
                        .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()));
        return classStudentList.stream().collect(Collectors.groupingBy(CourseClassStudent::getClassId,
                Collectors.counting()));
    }

    /**
     * 课程名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, String> loadCourseNameMap(List<String> courseIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = courseIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (Course course : courseMapper.selectBatchIds(distinctIdList)) {
            nameMap.put(course.getId(), course.getName());
        }
        return nameMap;
    }

    /**
     * 用户名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, String> loadUserNameMap(List<String> userIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = userIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (JSONObject user : sysUserApi.getUserListByIdListWithoutException(distinctIdList)) {
            nameMap.put(user.getStr("id"), user.getStr("name"));
        }
        return nameMap;
    }

    /**
     * 组织名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, String> loadOrgNameMap(List<String> orgIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = orgIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (JSONObject org : sysOrgApi.getOrgListByIdListWithoutException(distinctIdList)) {
            nameMap.put(org.getStr("id"), org.getStr("name"));
        }
        return nameMap;
    }

    private Date resolveStart(AcademicReportQueryParam param) {
        if (StrUtil.isNotBlank(param.getStartTime())) {
            return DateUtil.parse(param.getStartTime());
        }
        return DateUtil.beginOfMonth(new Date());
    }

    private Date resolveEnd(AcademicReportQueryParam param) {
        if (StrUtil.isNotBlank(param.getEndTime())) {
            return DateUtil.parse(param.getEndTime());
        }
        return new Date();
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private <T> List<T> limit(List<T> list, Integer limit) {
        int size = ObjectUtil.isNotEmpty(limit) ? limit : DEFAULT_LIMIT;
        return list.stream().limit(Math.max(size, 1)).collect(Collectors.toList());
    }
}
