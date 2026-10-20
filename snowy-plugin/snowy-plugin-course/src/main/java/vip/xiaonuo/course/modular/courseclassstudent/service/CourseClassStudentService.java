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
package vip.xiaonuo.course.modular.courseclassstudent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentAddParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentIdParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentPageParam;

import java.util.List;

/**
 * 班级学员Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface CourseClassStudentService {

    /** 获取班级学员分页 */
    Page<CourseClassStudent> page(CourseClassStudentPageParam courseClassStudentPageParam);

    /** 获取班级在学学员列表 */
    List<CourseClassStudent> listByClassId(String classId);

    /** 批量入班 */
    void add(CourseClassStudentAddParam courseClassStudentAddParam);

    /** 退班（逻辑删除） */
    void delete(List<CourseClassStudentIdParam> courseClassStudentIdParamList);

    /** 获取班级学员详情（内部用） */
    CourseClassStudent queryEntity(String id);
}
