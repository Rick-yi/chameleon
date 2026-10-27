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
package vip.xiaonuo.student.modular.studentrenewarn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.util.Date;

/**
 * 学员回访/跟进记录实体
 * <p>
 * 续费预警跟进、风险学员联系家长、退学/结课流失回访共用，避免为每个场景重复建表。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
@TableName("STUDENT_FOLLOW_RECORD")
public class StudentFollowRecord extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 续费预警id（风险学员跟进可为空） */
    @Schema(description = "续费预警id")
    private String warnId;

    /** 业务类型：RENEW_WARN/RISK/LOSS */
    @Schema(description = "业务类型(RENEW_WARN/RISK/LOSS)")
    private String bizType;

    /** 跟进方式：PHONE/WECHAT/VISIT/OTHER */
    @Schema(description = "跟进方式(PHONE/WECHAT/VISIT/OTHER)")
    private String followType;

    /** 跟进内容 */
    @Schema(description = "跟进内容")
    private String content;

    /** 下次跟进时间 */
    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    /** 跟进人id */
    @Schema(description = "跟进人id")
    private String followUserId;

    /** 流失类型：REFUND/GRADUATE/TRANSFER/OTHER（仅流失登记） */
    @Schema(description = "流失类型(REFUND/GRADUATE/TRANSFER/OTHER)")
    private String lossType;

    /** 流失原因（仅流失登记） */
    @Schema(description = "流失原因")
    private String lossReason;

    /** 学员姓名（列表展示用，非数据库字段） */
    @Schema(description = "学员姓名")
    @TableField(exist = false)
    private String studentName;

    /** 跟进人姓名（列表展示用，非数据库字段） */
    @Schema(description = "跟进人姓名")
    @TableField(exist = false)
    private String followUserName;
}
