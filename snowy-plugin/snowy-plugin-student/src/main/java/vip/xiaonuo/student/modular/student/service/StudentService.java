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
package vip.xiaonuo.student.modular.student.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentEditParam;
import vip.xiaonuo.student.modular.student.param.StudentIdParam;
import vip.xiaonuo.student.modular.student.param.StudentPageParam;

import java.util.List;

/**
 * 学员Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentService {

    /** 获取学员分页 */
    Page<Student> page(StudentPageParam studentPageParam);

    /** 添加学员 */
    void add(StudentAddParam studentAddParam);

    /** 编辑学员 */
    void edit(StudentEditParam studentEditParam);

    /** 删除学员 */
    void delete(List<StudentIdParam> studentIdParamList);

    /** 获取学员详情 */
    Student detail(StudentIdParam studentIdParam);

    /** 获取学员详情（内部用） */
    Student queryEntity(String id);
}
