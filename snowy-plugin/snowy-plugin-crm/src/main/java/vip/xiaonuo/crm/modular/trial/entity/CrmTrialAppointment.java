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
package vip.xiaonuo.crm.modular.trial.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.util.Date;

/**
 * 试听预约实体
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("CRM_TRIAL_APPOINTMENT")
public class CrmTrialAppointment extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 线索id */
    @Schema(description = "线索id")
    private String leadId;

    /** 试听课程id */
    @Schema(description = "试听课程id")
    private String courseId;

    /** 试听时间 */
    @Schema(description = "试听时间")
    private Date trialTime;

    /** 状态 */
    @Schema(description = "状态(待试听/已完成/已取消)")
    private String status;

    /** 试听结果 */
    @Schema(description = "试听结果(待定/有意向/无意向)")
    private String result;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 学员姓名（列表查询时由线索回填，非数据库字段） */
    @Schema(description = "学员姓名")
    @TableField(exist = false)
    private String leadName;

    /** 家长手机（列表查询时由线索回填，非数据库字段） */
    @Schema(description = "家长手机")
    @TableField(exist = false)
    private String parentPhone;

    /** 顾问/跟进人姓名（列表查询时由线索回填，非数据库字段） */
    @Schema(description = "顾问姓名")
    @TableField(exist = false)
    private String followUserName;

    /** 线索状态（列表查询时由线索回填，非数据库字段） */
    @Schema(description = "线索状态")
    @TableField(exist = false)
    private String leadStatus;
}
