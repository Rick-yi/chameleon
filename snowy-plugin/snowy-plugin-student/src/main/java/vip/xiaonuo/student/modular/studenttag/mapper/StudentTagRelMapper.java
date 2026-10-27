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
package vip.xiaonuo.student.modular.studenttag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import vip.xiaonuo.student.modular.studenttag.entity.StudentTagRel;

/**
 * 学员-标签关联Mapper
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
public interface StudentTagRelMapper extends BaseMapper<StudentTagRel> {

    /**
     * 物理删除某学员的全部标签关联（重新绑定前清理，避免逻辑删除与唯一索引冲突）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Delete("DELETE FROM `STUDENT_TAG_REL` WHERE `TENANT_ID` = #{tenantId} AND `STUDENT_ID` = #{studentId}")
    int deleteByStudent(@Param("tenantId") String tenantId, @Param("studentId") String studentId);

    /**
     * 物理删除某标签的全部关联（删除标签时清理）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Delete("DELETE FROM `STUDENT_TAG_REL` WHERE `TENANT_ID` = #{tenantId} AND `TAG_ID` = #{tagId}")
    int deleteByTag(@Param("tenantId") String tenantId, @Param("tagId") String tagId);
}
