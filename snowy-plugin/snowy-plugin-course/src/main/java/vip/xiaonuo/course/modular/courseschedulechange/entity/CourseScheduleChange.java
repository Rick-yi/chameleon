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
package vip.xiaonuo.course.modular.courseschedulechange.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 调课记录实体
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("COURSE_SCHEDULE_CHANGE")
public class CourseScheduleChange extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 原排课id */
    @Schema(description = "原排课id")
    private String fromScheduleId;

    /** 新排课id */
    @Schema(description = "新排课id")
    private String toScheduleId;

    /** 调课原因 */
    @Schema(description = "调课原因")
    private String changeReason;

    /** 原课节日期（列表填充，非数据库字段） */
    @Schema(description = "原课节日期")
    @TableField(exist = false)
    private String fromDate;

    /** 原课节时间段（列表填充，非数据库字段） */
    @Schema(description = "原课节时间段")
    @TableField(exist = false)
    private String fromTime;

    /** 原课节班级（列表填充，非数据库字段） */
    @Schema(description = "原课节班级")
    @TableField(exist = false)
    private String fromClassName;

    /** 新课节日期（列表填充，非数据库字段） */
    @Schema(description = "新课节日期")
    @TableField(exist = false)
    private String toDate;

    /** 新课节时间段（列表填充，非数据库字段） */
    @Schema(description = "新课节时间段")
    @TableField(exist = false)
    private String toTime;

    /** 新课节班级（列表填充，非数据库字段） */
    @Schema(description = "新课节班级")
    @TableField(exist = false)
    private String toClassName;

    /** 调课人姓名（列表填充，非数据库字段） */
    @Schema(description = "调课人姓名")
    @TableField(exist = false)
    private String changeUserName;
}
