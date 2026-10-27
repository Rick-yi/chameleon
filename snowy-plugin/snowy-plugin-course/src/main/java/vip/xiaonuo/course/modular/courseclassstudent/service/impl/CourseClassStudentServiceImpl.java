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
package vip.xiaonuo.course.modular.courseclassstudent.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import vip.xiaonuo.auth.core.util.StpRoleUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.enums.CourseClassFormEnum;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.mapper.CourseClassStudentMapper;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentAddParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentIdParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentPageParam;
import vip.xiaonuo.course.modular.courseclassstudent.service.CourseClassStudentService;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.student.api.StudentApi;
import vip.xiaonuo.student.api.dto.StudentBriefDTO;
import vip.xiaonuo.student.api.enums.StudentStatusEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 班级学员Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseClassStudentServiceImpl extends ServiceImpl<CourseClassStudentMapper, CourseClassStudent> implements CourseClassStudentService {

    private final CourseClassService courseClassService;

    private final StudentApi studentApi;

    private final BizEventApi bizEventApi;

    public CourseClassStudentServiceImpl(CourseClassService courseClassService, StudentApi studentApi, BizEventApi bizEventApi) {
        this.courseClassService = courseClassService;
        this.studentApi = studentApi;
        this.bizEventApi = bizEventApi;
    }

    @Override
    public Page<CourseClassStudent> page(CourseClassStudentPageParam courseClassStudentPageParam) {
        QueryWrapper<CourseClassStudent> queryWrapper = new QueryWrapper<CourseClassStudent>().checkSqlInjection();
        queryWrapper.lambda().eq(CourseClassStudent::getClassId, courseClassStudentPageParam.getClassId())
                // 岗位数据范围：授课老师只能看自己带的班级
                .apply(StrUtil.isNotBlank(selfTeacherId()), "class_id in (select id from COURSE_CLASS where teacher_id = {0})", selfTeacherId())
                .orderByDesc(CourseClassStudent::getCreateTime);
        Page<CourseClassStudent> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillStudentName(page.getRecords());
        return page;
    }

    /**
     * 回填学员姓名（移动端点名、学员名册直接展示，避免前端再查一遍学员列表）
     *
     * @author 会飞的猪
     * @date 2026/10/22 10:00
     **/
    private void fillStudentName(List<CourseClassStudent> recordList) {
        if (ObjectUtil.isEmpty(recordList)) {
            return;
        }
        List<String> studentIdList = recordList.stream().map(CourseClassStudent::getStudentId)
                .filter(ObjectUtil::isNotEmpty).distinct().collect(Collectors.toList());
        if (studentIdList.isEmpty()) {
            return;
        }
        Map<String, String> nameMap = new HashMap<>();
        for (StudentBriefDTO brief : studentApi.listStudentBrief(studentIdList)) {
            nameMap.put(brief.getId(), brief.getName());
        }
        recordList.forEach(item -> item.setStudentName(nameMap.get(item.getStudentId())));
    }

    /**
     * 当前登录人是授课老师时返回其用户id（用于「只看自己班」过滤）
     */
    private String selfTeacherId() {
        return StpRoleUtil.hasRole(StpRoleUtil.ROLE_TEACHER) ? StpRoleUtil.currentUserId() : null;
    }

    @Override
    public List<CourseClassStudent> listByClassId(String classId) {
        return this.list(new LambdaQueryWrapper<CourseClassStudent>()
                .eq(CourseClassStudent::getClassId, classId)
                .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue())
                .orderByAsc(CourseClassStudent::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseClassStudentAddParam courseClassStudentAddParam) {
        // 校验班级存在
        CourseClass courseClass = courseClassService.queryEntity(courseClassStudentAddParam.getClassId());
        if (ObjectUtil.isEmpty(courseClass)) {
            throw new CommonException("班级不存在");
        }
        if (ObjectUtil.isEmpty(courseClassStudentAddParam.getStudentIdList())) {
            throw new CommonException("请选择要加入的学员");
        }
        // 一对一班只允许绑定学员本人入班，避免把其他学员加进容量为 1 的班
        if (CourseClassFormEnum.isOne2One(courseClass.getClassType())) {
            boolean onlyBoundStudent = courseClassStudentAddParam.getStudentIdList().size() == 1
                    && ObjectUtil.equal(courseClass.getStudentId(), courseClassStudentAddParam.getStudentIdList().get(0));
            if (!onlyBoundStudent) {
                throw new CommonException("班级【{}】是一对一班级，只能加入绑定的学员", courseClass.getName());
            }
        }
        // 学员校验：必须存在且在读（停课/已退费学员不能入班）
        List<StudentBriefDTO> briefList = studentApi.listStudentBrief(courseClassStudentAddParam.getStudentIdList());
        Map<String, StudentBriefDTO> briefMap = new HashMap<>();
        for (StudentBriefDTO brief : briefList) {
            briefMap.put(brief.getId(), brief);
        }
        for (String studentId : courseClassStudentAddParam.getStudentIdList()) {
            StudentBriefDTO brief = briefMap.get(studentId);
            if (ObjectUtil.isEmpty(brief)) {
                throw new CommonException("学员不存在，id值为：{}", studentId);
            }
            if (!StudentStatusEnum.STUDYING.getValue().equals(brief.getStatus())) {
                throw new CommonException("学员【{}】当前状态【{}】不能入班", brief.getName(), brief.getStatus());
            }
        }
        List<CourseClassStudent> toSaveList = new ArrayList<>();
        for (String studentId : courseClassStudentAddParam.getStudentIdList()) {
            // 已在该班（未退班）则跳过
            CourseClassStudent exist = this.getOne(new LambdaQueryWrapper<CourseClassStudent>()
                    .eq(CourseClassStudent::getClassId, courseClassStudentAddParam.getClassId())
                    .eq(CourseClassStudent::getStudentId, studentId)
                    .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()));
            if (ObjectUtil.isNotEmpty(exist)) {
                continue;
            }
            CourseClassStudent courseClassStudent = new CourseClassStudent();
            courseClassStudent.setClassId(courseClassStudentAddParam.getClassId());
            courseClassStudent.setStudentId(studentId);
            courseClassStudent.setStatus(CourseClassStudentStatusEnum.STUDYING.getValue());
            toSaveList.add(courseClassStudent);
        }
        if (ObjectUtil.isEmpty(toSaveList)) {
            return;
        }
        bizEventApi.log(BizEventCodeEnum.CLASS_ENROLL.getValue(), "class", courseClass.getId(), courseClass.getName(),
                Map.of("人数", String.valueOf(toSaveList.size())));
        // 容量校验：在读学员数 + 新增人数不得超过班级容量
        int currentCount = this.listByClassId(courseClassStudentAddParam.getClassId()).size();
        int capacity = courseClass.getCapacity() == null ? 0 : courseClass.getCapacity();
        if (capacity > 0 && currentCount + toSaveList.size() > capacity) {
            throw new CommonException("班级【{}】容量为{}人，当前{}人，本次新增{}人，超出容量",
                    courseClass.getName(), capacity, currentCount, toSaveList.size());
        }
        this.saveBatch(toSaveList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CourseClassStudentIdParam> courseClassStudentIdParamList) {
        List<String> courseClassStudentIdList = CollStreamUtil.toList(courseClassStudentIdParamList, CourseClassStudentIdParam::getId);
        if (ObjectUtil.isNotEmpty(courseClassStudentIdList)) {
            // 退班：逻辑删除并置状态退班
            List<CourseClassStudent> quitList = this.listByIds(courseClassStudentIdList);
            quitList.forEach(item -> item.setStatus(CourseClassStudentStatusEnum.QUIT.getValue()));
            this.updateBatchById(quitList);
            this.removeByIds(courseClassStudentIdList);
            bizEventApi.log(BizEventCodeEnum.CLASS_QUIT.getValue(), "class", null, null,
                    Map.of("人数", String.valueOf(quitList.size())));
        }
    }

    @Override
    public CourseClassStudent queryEntity(String id) {
        CourseClassStudent courseClassStudent = this.getById(id);
        if (ObjectUtil.isEmpty(courseClassStudent)) {
            throw new CommonException("班级学员关系不存在，id值为：{}", id);
        }
        return courseClassStudent;
    }
}
