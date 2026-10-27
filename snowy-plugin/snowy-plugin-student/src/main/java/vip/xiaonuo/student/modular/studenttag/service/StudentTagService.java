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
package vip.xiaonuo.student.modular.studenttag.service;

import vip.xiaonuo.student.modular.studenttag.entity.StudentTag;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagAddParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagBindParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagEditParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagIdParam;

import java.util.List;
import java.util.Map;

/**
 * 学员标签Service
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
public interface StudentTagService {

    /** 标签字典（当前租户） */
    List<StudentTag> listTags();

    /** 新增标签 */
    String add(StudentTagAddParam param);

    /** 编辑标签 */
    void edit(StudentTagEditParam param);

    /** 删除标签（同时清理学员关联） */
    void delete(StudentTagIdParam param);

    /** 某学员的标签 */
    List<StudentTag> listByStudent(String studentId);

    /** 批量回填学员标签：studentId → 标签列表 */
    Map<String, List<StudentTag>> mapByStudentIds(List<String> studentIdList);

    /** 全量覆盖某学员的标签 */
    void bind(StudentTagBindParam param);
}
