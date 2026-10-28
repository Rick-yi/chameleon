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
package vip.xiaonuo.course.modular.courseclass.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.auth.core.util.StpDataScopeUtil;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.common.util.CommonSqlUtil;
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.enums.CourseClassTypeEnum;
import vip.xiaonuo.course.modular.course.enums.CourseStatusEnum;
import vip.xiaonuo.course.modular.course.service.CourseService;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.enums.CourseClassFormEnum;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassAddParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassEditParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassIdParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassPageParam;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.mapper.CourseClassStudentMapper;
import vip.xiaonuo.student.api.StudentApi;
import vip.xiaonuo.student.api.dto.StudentBriefDTO;
import vip.xiaonuo.student.api.enums.StudentStatusEnum;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班级Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseClassServiceImpl extends ServiceImpl<CourseClassMapper, CourseClass> implements CourseClassService {

    @Resource
    private CourseClassStudentMapper courseClassStudentMapper;

    @Resource
    private CourseService courseService;

    @Resource
    private StudentApi studentApi;

    @Override
    public Page<CourseClass> page(CourseClassPageParam courseClassPageParam) {
        QueryWrapper<CourseClass> queryWrapper = new QueryWrapper<CourseClass>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getSearchKey())) {
            queryWrapper.lambda().like(CourseClass::getName, courseClassPageParam.getSearchKey());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getCourseId())) {
            queryWrapper.lambda().eq(CourseClass::getCourseId, courseClassPageParam.getCourseId());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getClassType())) {
            queryWrapper.lambda().eq(CourseClass::getClassType, courseClassPageParam.getClassType());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getStudentId())) {
            queryWrapper.lambda().eq(CourseClass::getStudentId, courseClassPageParam.getStudentId());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getTeacherId())) {
            queryWrapper.lambda().eq(CourseClass::getTeacherId, courseClassPageParam.getTeacherId());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getClassroom())) {
            queryWrapper.lambda().like(CourseClass::getClassroom, courseClassPageParam.getClassroom());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getStatus())) {
            queryWrapper.lambda().eq(CourseClass::getStatus, courseClassPageParam.getStatus());
        }
        if (courseClassPageParam.getHasSchedule() != null) {
            String existsSql = "EXISTS (SELECT 1 FROM `COURSE_SCHEDULE` s WHERE s.`CLASS_ID` = `COURSE_CLASS`.`ID` "
                    + "AND s.`TENANT_ID` = `COURSE_CLASS`.`TENANT_ID` AND s.`DELETE_FLAG` = 'NOT_DELETE')";
            queryWrapper.apply(courseClassPageParam.getHasSchedule() ? existsSql : "NOT " + existsSql);
        }
        if (courseClassPageParam.getFull() != null) {
            String countSql = "(SELECT COUNT(*) FROM `COURSE_CLASS_STUDENT` cs WHERE cs.`CLASS_ID` = `COURSE_CLASS`.`ID` "
                    + "AND cs.`TENANT_ID` = `COURSE_CLASS`.`TENANT_ID` AND cs.`DELETE_FLAG` = 'NOT_DELETE' "
                    + "AND cs.`STATUS` = 'STUDYING')";
            String capacitySql = "COALESCE(`COURSE_CLASS`.`CAPACITY`, 0)";
            if (courseClassPageParam.getFull()) {
                queryWrapper.apply(capacitySql + " > 0 AND " + countSql + " >= " + capacitySql);
            } else {
                queryWrapper.apply("(" + capacitySql + " = 0 OR " + countSql + " < " + capacitySql + ")");
            }
        }
        // 数据范围：仅可见校区的班级
        List<String> visibleCampusIds = StpDataScopeUtil.visibleOrgIds();
        if (visibleCampusIds != null) {
            if (visibleCampusIds.isEmpty()) {
                queryWrapper.lambda().eq(CourseClass::getId, "__NO_DATA__");
            } else {
                CommonSqlUtil.safeIn(queryWrapper.lambda(), CourseClass::getCampusId, visibleCampusIds);
            }
        }
        if (ObjectUtil.isAllNotEmpty(courseClassPageParam.getSortField(), courseClassPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(courseClassPageParam.getSortOrder());
            queryWrapper.orderBy(true, courseClassPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(courseClassPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(CourseClass::getCreateTime);
        }
        Page<CourseClass> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillStudentCount(page.getRecords());
        fillStudentName(page.getRecords());
        return page;
    }

    /**
     * 回填在读学员数（批量统计，避免逐个查询）
     */
    private void fillStudentCount(List<CourseClass> classList) {
        if (ObjectUtil.isEmpty(classList)) {
            return;
        }
        List<String> classIdList = classList.stream().map(CourseClass::getId).collect(Collectors.toList());
        Map<String, Long> countMap = courseClassStudentMapper.selectList(new LambdaQueryWrapper<CourseClassStudent>()
                        .in(CourseClassStudent::getClassId, classIdList)
                        .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()))
                .stream().collect(Collectors.groupingBy(CourseClassStudent::getClassId, Collectors.counting()));
        for (CourseClass courseClass : classList) {
            courseClass.setStudentCount(countMap.getOrDefault(courseClass.getId(), 0L).intValue());
        }
    }

    /**
     * 回填一对一班绑定的学员姓名
     */
    private void fillStudentName(List<CourseClass> classList) {
        if (ObjectUtil.isEmpty(classList)) {
            return;
        }
        List<String> studentIdList = classList.stream().map(CourseClass::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (studentIdList.isEmpty()) {
            return;
        }
        Map<String, String> nameMap = studentApi.listStudentBrief(studentIdList).stream()
                .collect(Collectors.toMap(StudentBriefDTO::getId, StudentBriefDTO::getName, (left, right) -> left));
        classList.forEach(item -> item.setStudentName(nameMap.get(item.getStudentId())));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseClassAddParam courseClassAddParam) {
        CourseClass courseClass = BeanUtil.toBean(courseClassAddParam, CourseClass.class);
        prepareClassType(courseClass, null);
        boolean repeatName = this.count(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getName, courseClass.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的班级：{}", courseClass.getName());
        }
        if (ObjectUtil.isEmpty(courseClass.getStatus())) {
            courseClass.setStatus(CourseStatusEnum.ENABLE.getValue());
        }
        this.save(courseClass);
        syncOneToOneRoster(courseClass, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CourseClassEditParam courseClassEditParam) {
        CourseClass courseClass = this.queryEntity(courseClassEditParam.getId());
        CourseClass oldClass = BeanUtil.copyProperties(courseClass, CourseClass.class);
        BeanUtil.copyProperties(courseClassEditParam, courseClass);
        prepareClassType(courseClass, oldClass);
        boolean repeatName = this.count(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getName, courseClass.getName())
                .ne(CourseClass::getId, courseClass.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的班级：{}", courseClass.getName());
        }
        this.updateById(courseClass);
        syncOneToOneRoster(courseClass, oldClass);
    }

    /**
     * 归一化班型并校验一对一班绑定学员
     */
    private void prepareClassType(CourseClass courseClass, CourseClass oldClass) {
        Course course = courseService.queryEntity(courseClass.getCourseId());
        String fallbackType = oldClass == null
                ? CourseClassFormEnum.GROUP.getValue()
                : StrUtil.blankToDefault(oldClass.getClassType(), CourseClassFormEnum.GROUP.getValue());
        String classType = StrUtil.blankToDefault(courseClass.getClassType(), fallbackType);
        // 课程本身是一对一时，班级不允许建成一对多
        if (CourseClassTypeEnum.ONE2ONE.getValue().equals(course.getClassType())) {
            classType = CourseClassFormEnum.ONE2ONE.getValue();
        }
        if (!CourseClassFormEnum.isOne2One(classType)) {
            courseClass.setClassType(CourseClassFormEnum.GROUP.getValue());
            courseClass.setStudentId(null);
            if (courseClass.getCapacity() == null || courseClass.getCapacity() < 1) {
                courseClass.setCapacity(15);
            }
            return;
        }

        if (StrUtil.isBlank(courseClass.getStudentId())) {
            throw new CommonException("一对一班级必须绑定一名学员");
        }
        List<StudentBriefDTO> briefList = studentApi.listStudentBrief(List.of(courseClass.getStudentId()));
        StudentBriefDTO brief = briefList.stream().findFirst().orElse(null);
        if (ObjectUtil.isEmpty(brief)) {
            throw new CommonException("绑定的学员不存在");
        }
        if (!StudentStatusEnum.STUDYING.getValue().equals(brief.getStatus())) {
            throw new CommonException("学员【{}】当前状态【{}】不能绑定一对一班级", brief.getName(), brief.getStatus());
        }
        // 原班级已有名册时不允许直接改成一对一，避免把多名学员塞进容量为 1 的班
        boolean wasOne2One = oldClass != null && CourseClassFormEnum.isOne2One(oldClass.getClassType());
        if (oldClass != null && !wasOne2One) {
            long rosterCount = courseClassStudentMapper.selectCount(new LambdaQueryWrapper<CourseClassStudent>()
                    .eq(CourseClassStudent::getClassId, oldClass.getId())
                    .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()));
            if (rosterCount > 0) {
                throw new CommonException("班级已有 {} 名在读学员，请新建一对一班级并绑定学员", rosterCount);
            }
        }
        LambdaQueryWrapper<CourseClass> duplicateWrapper = new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getCourseId, courseClass.getCourseId())
                .eq(CourseClass::getStudentId, courseClass.getStudentId())
                .eq(CourseClass::getClassType, CourseClassFormEnum.ONE2ONE.getValue())
                .eq(CourseClass::getStatus, CourseStatusEnum.ENABLE.getValue());
        if (StrUtil.isNotBlank(courseClass.getId())) {
            duplicateWrapper.ne(CourseClass::getId, courseClass.getId());
        }
        if (this.count(duplicateWrapper) > 0) {
            throw new CommonException("学员【{}】在该课程下已有一对一班级", brief.getName());
        }
        courseClass.setClassType(CourseClassFormEnum.ONE2ONE.getValue());
        courseClass.setCapacity(1);
        if (StrUtil.isBlank(courseClass.getCampusId())) {
            courseClass.setCampusId(brief.getCampusId());
        }
    }

    /**
     * 一对一班自动维护名册：换学员时退出旧学员，新学员自动入班
     */
    private void syncOneToOneRoster(CourseClass courseClass, CourseClass oldClass) {
        if (!CourseClassFormEnum.isOne2One(courseClass.getClassType())) {
            return;
        }
        String previousStudentId = oldClass == null ? null : oldClass.getStudentId();
        if (StrUtil.isNotBlank(previousStudentId) && !previousStudentId.equals(courseClass.getStudentId())) {
            quitRoster(courseClass.getId(), previousStudentId);
        }
        if (StrUtil.isBlank(courseClass.getStudentId())) {
            return;
        }
        long existed = courseClassStudentMapper.selectCount(new LambdaQueryWrapper<CourseClassStudent>()
                .eq(CourseClassStudent::getClassId, courseClass.getId())
                .eq(CourseClassStudent::getStudentId, courseClass.getStudentId())
                .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()));
        if (existed > 0) {
            return;
        }
        CourseClassStudent relation = new CourseClassStudent();
        relation.setClassId(courseClass.getId());
        relation.setStudentId(courseClass.getStudentId());
        relation.setStatus(CourseClassStudentStatusEnum.STUDYING.getValue());
        courseClassStudentMapper.insert(relation);
    }

    /**
     * 一对一换绑时，旧学员退出班级名册（保留历史点名记录）
     */
    private void quitRoster(String classId, String studentId) {
        CourseClassStudent relation = courseClassStudentMapper.selectOne(new LambdaQueryWrapper<CourseClassStudent>()
                .eq(CourseClassStudent::getClassId, classId)
                .eq(CourseClassStudent::getStudentId, studentId)
                .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue())
                .last("limit 1"));
        if (ObjectUtil.isEmpty(relation)) {
            return;
        }
        relation.setStatus(CourseClassStudentStatusEnum.QUIT.getValue());
        courseClassStudentMapper.updateById(relation);
        courseClassStudentMapper.deleteById(relation.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CourseClassIdParam> courseClassIdParamList) {
        List<String> courseClassIdList = CollStreamUtil.toList(courseClassIdParamList, CourseClassIdParam::getId);
        if (ObjectUtil.isNotEmpty(courseClassIdList)) {
            this.removeByIds(courseClassIdList);
        }
    }

    @Override
    public CourseClass detail(CourseClassIdParam courseClassIdParam) {
        CourseClass courseClass = this.queryEntity(courseClassIdParam.getId());
        fillStudentName(List.of(courseClass));
        return courseClass;
    }

    @Override
    public CourseClass queryEntity(String id) {
        CourseClass courseClass = this.getById(id);
        if (ObjectUtil.isEmpty(courseClass)) {
            throw new CommonException("班级不存在，id值为：{}", id);
        }
        return courseClass;
    }
}
