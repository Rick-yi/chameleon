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
package vip.xiaonuo.student.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 看板待办DTO
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class DashboardTodoDTO {

    /** 类型：RENEW_WARN/UNPAID_ORDER */
    @Schema(description = "待办类型")
    private String type;

    /** 业务id（预警id/订单id） */
    @Schema(description = "业务id")
    private String id;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 学员姓名 */
    @Schema(description = "学员姓名")
    private String studentName;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

    /** 内容 */
    @Schema(description = "内容")
    private String content;

    /** 金额（待收款订单） */
    @Schema(description = "金额")
    private BigDecimal amount;

    /** 剩余课时（续费预警） */
    @Schema(description = "剩余课时")
    private BigDecimal remainLessons;

    /** 时间 */
    @Schema(description = "时间")
    private Date time;
}