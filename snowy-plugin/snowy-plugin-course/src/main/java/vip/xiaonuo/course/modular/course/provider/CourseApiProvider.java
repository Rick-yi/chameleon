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
package vip.xiaonuo.course.modular.course.provider;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.mapper.CourseMapper;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.mapper.CourseClassStudentMapper;
import vip.xiaonuo.course.modular.coursepackage.entity.CoursePackage;
import vip.xiaonuo.course.modular.coursepackage.mapper.CoursePackageMapper;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程域API接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseApiProvider implements CourseApi {

    @Resource
    private CourseClassMapper courseClassMapper;

    @Resource
    private CourseScheduleMapper courseScheduleMapper;

    @Resource
    private CourseClassStudentMapper courseClassStudentMapper;

    @Resource
    private CoursePackageMapper coursePackageMapper;

    @Resource
    private CourseMapper courseMapper;

    @Override
    public JSONObject getCourseClassInfo(String classId) {
        CourseClass courseClass = courseClassMapper.selectById(classId);
        if (ObjectUtil.isEmpty(courseClass)) {
            return null;
        }
        return JSONUtil.createObj()
                .set("id", courseClass.getId())
                .set("name", courseClass.getName())
                .set("courseId", courseClass.getCourseId())
                .set("campusId", courseClass.getCampusId())
                .set("teacherId", courseClass.getTeacherId())
                .set("classroom", courseClass.getClassroom());
    }

    @Override
    public JSONObject getCourseScheduleInfo(String scheduleId) {
        CourseSchedule courseSchedule = courseScheduleMapper.selectById(scheduleId);
        if (ObjectUtil.isEmpty(courseSchedule)) {
            return null;
        }
        return JSONUtil.createObj()
                .set("id", courseSchedule.getId())
                .set("classId", courseSchedule.getClassId())
                .set("scheduleDate", courseSchedule.getScheduleDate())
                .set("startTime", courseSchedule.getStartTime())
                .set("endTime", courseSchedule.getEndTime())
                .set("classroom", courseSchedule.getClassroom())
                .set("teacherId", courseSchedule.getTeacherId())
                .set("status", courseSchedule.getStatus());
    }

    @Override
    public List<String> getStudyingStudentIdList(String classId) {
        return courseClassStudentMapper.selectList(new LambdaQueryWrapper<CourseClassStudent>()
                        .eq(CourseClassStudent::getClassId, classId)
                        .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()))
                .stream().map(CourseClassStudent::getStudentId).collect(Collectors.toList());
    }

    @Override
    public JSONObject getCoursePackageInfo(String packageId) {
        CoursePackage coursePackage = coursePackageMapper.selectById(packageId);
        if (ObjectUtil.isEmpty(coursePackage)) {
            return null;
        }
        int validityDays = ObjectUtil.defaultIfNull(coursePackage.getValidityDays(), 0);
        int giftValidityDays = ObjectUtil.defaultIfNull(coursePackage.getGiftValidityDays(), 0);
        return JSONUtil.createObj()
                .set("id", coursePackage.getId())
                .set("courseId", coursePackage.getCourseId())
                .set("name", coursePackage.getName())
                .set("lessons", coursePackage.getLessons())
                .set("giftLessons", coursePackage.getGiftLessons())
                .set("price", coursePackage.getPrice())
                .set("validityDays", validityDays)
                .set("giftValidityDays", giftValidityDays)
                .set("totalValidityDays", validityDays + giftValidityDays)
                .set("lessonsPerClass", coursePackage.getLessonsPerClass())
                .set("leaveDeduct", coursePackage.getLeaveDeduct())
                .set("absentDeduct", coursePackage.getAbsentDeduct());
    }

    @Override
    public String getCourseName(String courseId) {
        Course course = courseMapper.selectById(courseId);
        return ObjectUtil.isEmpty(course) ? null : course.getName();
    }

    @Override
    public java.util.Map<String, String> listCourseNameMap(java.util.List<String> courseIdList) {
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        if (ObjectUtil.isEmpty(courseIdList)) {
            return nameMap;
        }
        courseMapper.selectBatchIds(courseIdList).forEach(course -> nameMap.put(course.getId(), course.getName()));
        return nameMap;
    }

    @Override
    public java.util.Map<String, String> listScheduleClassNameMap(java.util.List<String> scheduleIdList) {
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        if (ObjectUtil.isEmpty(scheduleIdList)) {
            return nameMap;
        }
        List<CourseSchedule> scheduleList = courseScheduleMapper.selectBatchIds(scheduleIdList);
        List<String> classIdList = scheduleList.stream().map(CourseSchedule::getClassId)
                .filter(ObjectUtil::isNotEmpty).distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(classIdList)) {
            return nameMap;
        }
        java.util.Map<String, String> classNameMap = new java.util.HashMap<>();
        courseClassMapper.selectBatchIds(classIdList).forEach(item -> classNameMap.put(item.getId(), item.getName()));
        for (CourseSchedule schedule : scheduleList) {
            String className = classNameMap.get(schedule.getClassId());
            if (className != null) {
                nameMap.put(schedule.getId(), className);
            }
        }
        return nameMap;
    }

    @Override
    public List<String> listScheduleIdsByCampus(String campusId) {
        List<String> classIdList = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCampusId, campusId))
                .stream().map(CourseClass::getId).collect(Collectors.toList());
        if (ObjectUtil.isEmpty(classIdList)) {
            return List.of();
        }
        return courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                        .in(CourseSchedule::getClassId, classIdList))
                .stream().map(CourseSchedule::getId).distinct().collect(Collectors.toList());
    }

    @Override
    public String getScheduleCampusId(String scheduleId) {
        CourseSchedule courseSchedule = courseScheduleMapper.selectById(scheduleId);
        if (ObjectUtil.isEmpty(courseSchedule)) {
            return null;
        }
        CourseClass courseClass = courseClassMapper.selectById(courseSchedule.getClassId());
        return ObjectUtil.isEmpty(courseClass) ? null : courseClass.getCampusId();
    }

    @Override
    public java.util.Map<String, String> listScheduleCampusMap(java.util.List<String> scheduleIdList) {
        java.util.Map<String, String> campusMap = new java.util.HashMap<>();
        if (ObjectUtil.isEmpty(scheduleIdList)) {
            return campusMap;
        }
        // 1. 批量取排课（排课自带校区快照时直接用）
        List<CourseSchedule> scheduleList = courseScheduleMapper.selectBatchIds(scheduleIdList);
        // 2. 需要回退到班级校区的排课，批量取班级
        List<String> needClassIdList = scheduleList.stream()
                .filter(item -> StrUtil.isBlank(item.getCampusId()))
                .map(CourseSchedule::getClassId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        java.util.Map<String, String> classCampusMap = new java.util.HashMap<>();
        if (!needClassIdList.isEmpty()) {
            for (CourseClass courseClass : courseClassMapper.selectBatchIds(needClassIdList)) {
                classCampusMap.put(courseClass.getId(), courseClass.getCampusId());
            }
        }
        for (CourseSchedule schedule : scheduleList) {
            String campusId = StrUtil.isNotBlank(schedule.getCampusId())
                    ? schedule.getCampusId()
                    : classCampusMap.get(schedule.getClassId());
            if (StrUtil.isNotBlank(campusId)) {
                campusMap.put(schedule.getId(), campusId);
            }
        }
        return campusMap;
    }

    @Override
    public java.util.Map<String, String> listCoursePackageNameMap(java.util.List<String> packageIdList) {
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        if (ObjectUtil.isEmpty(packageIdList)) {
            return nameMap;
        }
        for (CoursePackage coursePackage : coursePackageMapper.selectBatchIds(packageIdList)) {
            nameMap.put(coursePackage.getId(), coursePackage.getName());
        }
        return nameMap;
    }

    @Override
    public java.util.Map<String, String> listStudentClassNameMap(java.util.List<String> studentIdList) {
        java.util.Map<String, String> resultMap = new java.util.HashMap<>();
        if (ObjectUtil.isEmpty(studentIdList)) {
            return resultMap;
        }
        List<CourseClassStudent> relationList = courseClassStudentMapper.selectList(
                new LambdaQueryWrapper<CourseClassStudent>()
                        .in(CourseClassStudent::getStudentId, studentIdList)
                        .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()));
        if (ObjectUtil.isEmpty(relationList)) {
            return resultMap;
        }
        List<String> classIdList = relationList.stream().map(CourseClassStudent::getClassId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        java.util.Map<String, String> classNameMap = new java.util.HashMap<>();
        if (ObjectUtil.isNotEmpty(classIdList)) {
            for (CourseClass courseClass : courseClassMapper.selectBatchIds(classIdList)) {
                classNameMap.put(courseClass.getId(), courseClass.getName());
            }
        }
        for (CourseClassStudent relation : relationList) {
            String className = classNameMap.get(relation.getClassId());
            if (StrUtil.isNotBlank(className)) {
                resultMap.merge(relation.getStudentId(), className,
                        (first, second) -> first.equals(second) ? first : first + "、" + second);
            }
        }
        return resultMap;
    }
}
