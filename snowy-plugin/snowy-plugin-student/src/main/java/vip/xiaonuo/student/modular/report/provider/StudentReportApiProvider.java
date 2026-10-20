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
package vip.xiaonuo.student.modular.report.provider;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.api.StudentReportApi;
import vip.xiaonuo.student.api.dto.AcademicLessonProgressDTO;
import vip.xiaonuo.student.api.dto.AcademicRiskStudentDTO;
import vip.xiaonuo.student.api.param.AcademicReportQueryParam;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.enums.StudentStatusEnum;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.enums.LessonAccountStatusEnum;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.mapper.StudentLessonFlowMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员域报表API接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentReportApiProvider implements StudentReportApi {

    /** 低剩余课时阈值 */
    private static final int LOW_LESSON_THRESHOLD = 3;

    /** 长期未消课天数阈值 */
    private static final int NO_ATTEND_DAYS = 30;

    /** 默认返回条数 */
    private static final int DEFAULT_LIMIT = 100;

    @Resource
    private StudentLessonAccountMapper studentLessonAccountMapper;

    @Resource
    private StudentLessonFlowMapper studentLessonFlowMapper;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private CourseApi courseApi;

    @Override
    public List<AcademicLessonProgressDTO> lessonProgress(AcademicReportQueryParam param) {
        Date start = parseTime(param.getStartTime());
        Date end = parseTime(param.getEndTime());
        List<String> studentIdList = resolveStudentIdList(param);
        if (studentIdList != null && studentIdList.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<StudentLessonAccount> wrapper = new LambdaQueryWrapper<StudentLessonAccount>()
                .ge(start != null, StudentLessonAccount::getCreateTime, start)
                .le(end != null, StudentLessonAccount::getCreateTime, end)
                .eq(StrUtil.isNotBlank(param.getCourseId()), StudentLessonAccount::getCourseId, param.getCourseId())
                .in(ObjectUtil.isNotEmpty(studentIdList), StudentLessonAccount::getStudentId, studentIdList)
                .orderByAsc(StudentLessonAccount::getStudentId);
        List<StudentLessonAccount> accountList = studentLessonAccountMapper.selectList(wrapper);
        if (ObjectUtil.isEmpty(accountList)) {
            return new ArrayList<>();
        }
        Map<String, String> studentNameMap = loadStudentNameMap(accountList.stream()
                .map(StudentLessonAccount::getStudentId).collect(Collectors.toList()));
        List<AcademicLessonProgressDTO> resultList = new ArrayList<>();
        for (StudentLessonAccount account : accountList) {
            AcademicLessonProgressDTO dto = new AcademicLessonProgressDTO();
            dto.setStudentId(account.getStudentId());
            dto.setStudentName(studentNameMap.get(account.getStudentId()));
            dto.setCourseId(account.getCourseId());
            dto.setCourseName(resolveCourseName(account.getCourseId()));
            dto.setTotalLessons(nvl(account.getTotalLessons()));
            dto.setUsedLessons(nvl(account.getUsedLessons()));
            dto.setRemainLessons(nvl(account.getRemainLessons()));
            dto.setExpireTime(account.getExpireTime());
            dto.setStatus(account.getStatus());
            BigDecimal total = nvl(account.getTotalLessons());
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                dto.setProgressRate(nvl(account.getUsedLessons()).multiply(BigDecimal.valueOf(100))
                        .divide(total, 2, RoundingMode.HALF_UP));
            }
            resultList.add(dto);
        }
        return limitProgress(resultList, param.getLimit());
    }

    @Override
    public List<AcademicRiskStudentDTO> riskStudents(AcademicReportQueryParam param) {
        List<String> studentIdList = resolveStudentIdList(param);
        if (studentIdList != null && studentIdList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Student> studentMap = loadStudentMap(studentIdList);
        Map<String, StudentLessonAccount> accountMap = loadFirstAccountMap(studentIdList, param.getCourseId());
        String riskType = StrUtil.blankToDefault(param.getType(), "LOW_LESSON");
        List<AcademicRiskStudentDTO> resultList = new ArrayList<>();
        if ("LOW_LESSON".equalsIgnoreCase(riskType)) {
            LambdaQueryWrapper<StudentLessonAccount> wrapper = new LambdaQueryWrapper<StudentLessonAccount>()
                    .eq(StudentLessonAccount::getStatus, LessonAccountStatusEnum.NORMAL.getValue())
                    .le(StudentLessonAccount::getRemainLessons, LOW_LESSON_THRESHOLD)
                    .eq(StrUtil.isNotBlank(param.getCourseId()), StudentLessonAccount::getCourseId, param.getCourseId())
                    .in(ObjectUtil.isNotEmpty(studentIdList), StudentLessonAccount::getStudentId, studentIdList);
            for (StudentLessonAccount account : studentLessonAccountMapper.selectList(wrapper)) {
                Student student = studentMap.get(account.getStudentId());
                if (student != null && !StudentStatusEnum.STUDYING.getValue().equals(student.getStatus())) {
                    continue;
                }
                AcademicRiskStudentDTO dto = buildRiskDto(student, account);
                dto.setRiskType("LOW_LESSON");
                dto.setRiskDesc("剩余课时不足 " + LOW_LESSON_THRESHOLD + " 课时");
                resultList.add(dto);
            }
        } else if ("NO_ATTEND".equalsIgnoreCase(riskType)) {
            Map<String, Date> lastAttendMap = loadLastAttendMap(new ArrayList<>(studentMap.keySet()));
            for (Student student : studentMap.values()) {
                if (!StudentStatusEnum.STUDYING.getValue().equals(student.getStatus())) {
                    continue;
                }
                Date lastAttendTime = lastAttendMap.get(student.getId());
                long days = lastAttendTime == null ? Long.MAX_VALUE
                        : DateUtil.betweenDay(lastAttendTime, new Date(), true);
                if (days < NO_ATTEND_DAYS) {
                    continue;
                }
                AcademicRiskStudentDTO dto = buildRiskDto(student, accountMap.get(student.getId()));
                dto.setLastAttendTime(lastAttendTime);
                dto.setDaysWithoutAttend(lastAttendTime == null ? null : days);
                dto.setRiskType("NO_ATTEND");
                dto.setRiskDesc(lastAttendTime == null ? "从未消课" : "已 " + days + " 天未消课");
                resultList.add(dto);
            }
        } else if ("STOPPED".equalsIgnoreCase(riskType)) {
            for (Student student : studentMap.values()) {
                if (!StudentStatusEnum.STOP.getValue().equals(student.getStatus())) {
                    continue;
                }
                AcademicRiskStudentDTO dto = buildRiskDto(student, accountMap.get(student.getId()));
                dto.setRiskType("STOPPED");
                dto.setRiskDesc("学员已停课");
                resultList.add(dto);
            }
        }
        resultList.sort(Comparator.comparing(dto -> StrUtil.blankToDefault(dto.getStudentId(), "")));
        return limitRisk(resultList, param.getLimit());
    }

    /**
     * 解析筛选后的学员id；返回 null 表示不限制学员
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private List<String> resolveStudentIdList(AcademicReportQueryParam param) {
        if (StrUtil.isNotBlank(param.getClassId())) {
            List<String> classStudentIdList = courseApi.getStudyingStudentIdList(param.getClassId());
            if (ObjectUtil.isEmpty(classStudentIdList)) {
                return new ArrayList<>();
            }
            if (StrUtil.isBlank(param.getCampusId())) {
                return classStudentIdList;
            }
            List<String> campusStudentIdList = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .eq(Student::getCampusId, param.getCampusId()))
                    .stream().map(Student::getId).collect(Collectors.toList());
            return classStudentIdList.stream().filter(campusStudentIdList::contains).collect(Collectors.toList());
        }
        if (StrUtil.isNotBlank(param.getCampusId())) {
            return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .eq(Student::getCampusId, param.getCampusId()))
                    .stream().map(Student::getId).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 学员名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, String> loadStudentNameMap(List<String> studentIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = studentIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (Student student : studentMapper.selectBatchIds(distinctIdList)) {
            nameMap.put(student.getId(), student.getName());
        }
        return nameMap;
    }

    /**
     * 学员实体
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, Student> loadStudentMap(List<String> studentIdList) {
        Map<String, Student> studentMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            studentMapper.selectBatchIds(studentIdList).forEach(student -> studentMap.put(student.getId(), student));
        } else {
            studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .in(Student::getStatus, StudentStatusEnum.STUDYING.getValue(), StudentStatusEnum.STOP.getValue()))
                    .forEach(student -> studentMap.put(student.getId(), student));
        }
        return studentMap;
    }

    /**
     * 每个学员取一个课时账户（用于风险学员展示课程/剩余课时）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, StudentLessonAccount> loadFirstAccountMap(List<String> studentIdList, String courseId) {
        Map<String, StudentLessonAccount> accountMap = new HashMap<>();
        LambdaQueryWrapper<StudentLessonAccount> wrapper = new LambdaQueryWrapper<StudentLessonAccount>()
                .eq(StrUtil.isNotBlank(courseId), StudentLessonAccount::getCourseId, courseId)
                .in(ObjectUtil.isNotEmpty(studentIdList), StudentLessonAccount::getStudentId, studentIdList)
                .orderByDesc(StudentLessonAccount::getRemainLessons);
        for (StudentLessonAccount account : studentLessonAccountMapper.selectList(wrapper)) {
            accountMap.putIfAbsent(account.getStudentId(), account);
        }
        return accountMap;
    }

    /**
     * 最近一次消课时间
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, Date> loadLastAttendMap(List<String> studentIdList) {
        Map<String, Date> lastAttendMap = new HashMap<>();
        if (ObjectUtil.isEmpty(studentIdList)) {
            return lastAttendMap;
        }
        List<StudentLessonFlow> flowList = studentLessonFlowMapper.selectList(new LambdaQueryWrapper<StudentLessonFlow>()
                .in(StudentLessonFlow::getStudentId, studentIdList)
                .eq(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue())
                .orderByDesc(StudentLessonFlow::getCreateTime));
        for (StudentLessonFlow flow : flowList) {
            lastAttendMap.putIfAbsent(flow.getStudentId(), flow.getCreateTime());
        }
        return lastAttendMap;
    }

    /**
     * 构建风险学员DTO
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private AcademicRiskStudentDTO buildRiskDto(Student student, StudentLessonAccount account) {
        AcademicRiskStudentDTO dto = new AcademicRiskStudentDTO();
        if (student != null) {
            dto.setStudentId(student.getId());
            dto.setStudentName(student.getName());
            dto.setCampusId(student.getCampusId());
        }
        if (account != null) {
            dto.setCourseId(account.getCourseId());
            dto.setCourseName(resolveCourseName(account.getCourseId()));
            dto.setRemainLessons(nvl(account.getRemainLessons()));
            dto.setExpireTime(account.getExpireTime());
        }
        return dto;
    }

    /**
     * 课程名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String resolveCourseName(String courseId) {
        if (StrUtil.isBlank(courseId)) {
            return null;
        }
        String courseName = courseApi.getCourseName(courseId);
        return StrUtil.blankToDefault(courseName, courseId);
    }

    private List<AcademicLessonProgressDTO> limitProgress(List<AcademicLessonProgressDTO> list, Integer limit) {
        int size = ObjectUtil.isNotEmpty(limit) ? limit : DEFAULT_LIMIT;
        return list.stream().limit(Math.max(size, 1)).collect(Collectors.toList());
    }

    private List<AcademicRiskStudentDTO> limitRisk(List<AcademicRiskStudentDTO> list, Integer limit) {
        int size = ObjectUtil.isNotEmpty(limit) ? limit : DEFAULT_LIMIT;
        return list.stream().limit(Math.max(size, 1)).collect(Collectors.toList());
    }

    private Date parseTime(String value) {
        return StrUtil.isBlank(value) ? null : DateUtil.parse(value);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
