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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.mapper.CourseClassStudentMapper;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentAddParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentIdParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentPageParam;
import vip.xiaonuo.course.modular.courseclassstudent.service.CourseClassStudentService;

import java.util.ArrayList;
import java.util.List;

/**
 * 班级学员Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseClassStudentServiceImpl extends ServiceImpl<CourseClassStudentMapper, CourseClassStudent> implements CourseClassStudentService {

    private final CourseClassService courseClassService;

    public CourseClassStudentServiceImpl(CourseClassService courseClassService) {
        this.courseClassService = courseClassService;
    }

    @Override
    public Page<CourseClassStudent> page(CourseClassStudentPageParam courseClassStudentPageParam) {
        QueryWrapper<CourseClassStudent> queryWrapper = new QueryWrapper<CourseClassStudent>().checkSqlInjection();
        queryWrapper.lambda().eq(CourseClassStudent::getClassId, courseClassStudentPageParam.getClassId())
                .orderByDesc(CourseClassStudent::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseClassStudentAddParam courseClassStudentAddParam) {
        // 校验班级存在
        CourseClass courseClass = courseClassService.queryEntity(courseClassStudentAddParam.getClassId());
        if (ObjectUtil.isEmpty(courseClass)) {
            throw new CommonException("班级不存在");
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
        if (ObjectUtil.isNotEmpty(toSaveList)) {
            this.saveBatch(toSaveList);
        }
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
