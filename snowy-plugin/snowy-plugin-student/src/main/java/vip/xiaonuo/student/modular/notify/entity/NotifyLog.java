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
package vip.xiaonuo.student.modular.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.util.Date;

/**
 * 通知流水实体
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("NOTIFY_LOG")
public class NotifyLog extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 业务类型（点名消课/调课/续费预警） */
    @Schema(description = "业务类型")
    private String bizType;

    /** 接收人类型（家长/教职工） */
    @Schema(description = "接收人类型")
    private String receiverType;

    /** 接收人id */
    @Schema(description = "接收人id")
    private String receiverId;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

    /** 内容 */
    @Schema(description = "内容")
    private String content;

    /** 渠道（站内/短信/小程序） */
    @Schema(description = "渠道")
    private String channel;

    /** 发送状态（待发送/已发送/失败） */
    @Schema(description = "发送状态")
    private String status;

    /** 发送时间 */
    @Schema(description = "发送时间")
    private Date sendTime;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}