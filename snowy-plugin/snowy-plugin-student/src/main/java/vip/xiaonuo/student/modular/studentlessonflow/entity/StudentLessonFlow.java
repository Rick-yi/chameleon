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
package vip.xiaonuo.student.modular.studentlessonflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;

/**
 * 课时流水实体（入账/消课/回补/退费/调整 统一流水）
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("STUDENT_LESSON_FLOW")
public class StudentLessonFlow extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 班级名称（列表查询时填充，非数据库字段） */
    @Schema(description = "班级名称")
    @TableField(exist = false)
    private String className;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 课程id */
    @Schema(description = "课程id")
    private String courseId;

    /** 关联排课id */
    @Schema(description = "关联排课id")
    private String scheduleId;

    /** 类型 */
    @Schema(description = "类型(购课入账/点名消课/回补/退费销课/人工调整)")
    private String flowType;

    /** 课时(正=入账,负=扣减) */
    @Schema(description = "课时(正=入账,负=扣减)")
    private BigDecimal lessons;

    /** 关联订单id */
    @Schema(description = "关联订单id")
    private String refOrderId;

    /** 关联流水id */
    @Schema(description = "关联流水id(回补用)")
    private String refFlowId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
