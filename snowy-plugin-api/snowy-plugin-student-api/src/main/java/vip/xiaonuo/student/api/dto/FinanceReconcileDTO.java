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
import java.util.ArrayList;
import java.util.List;

/**
 * 报表对账DTO
 * <p>
 * 用于校验「报表数字」与「业务明细」是否一致，差异会给出明确说明。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class FinanceReconcileDTO {

    /** 收款金额（订单明细直算） */
    @Schema(description = "收款金额（订单明细）")
    private BigDecimal orderPaidAmount = BigDecimal.ZERO;

    /** 收款金额（报表口径） */
    @Schema(description = "收款金额（报表口径）")
    private BigDecimal summaryPaidAmount = BigDecimal.ZERO;

    /** 收款差异 */
    @Schema(description = "收款差异")
    private BigDecimal paidDiff = BigDecimal.ZERO;

    /** 退费金额（退费明细直算） */
    @Schema(description = "退费金额（退费明细）")
    private BigDecimal refundDetailAmount = BigDecimal.ZERO;

    /** 退费金额（报表口径） */
    @Schema(description = "退费金额（报表口径）")
    private BigDecimal summaryRefundAmount = BigDecimal.ZERO;

    /** 退费差异 */
    @Schema(description = "退费差异")
    private BigDecimal refundDiff = BigDecimal.ZERO;

    /** 课消课时（全量流水净消耗） */
    @Schema(description = "课消课时（全量流水净消耗）")
    private BigDecimal flowAttendLessons = BigDecimal.ZERO;

    /** 已用课时（课时账户合计） */
    @Schema(description = "已用课时（账户合计）")
    private BigDecimal accountUsedLessons = BigDecimal.ZERO;

    /** 课消差异 */
    @Schema(description = "课消差异")
    private BigDecimal lessonDiff = BigDecimal.ZERO;

    /** 有流水支撑账户的已用课时合计 */
    @Schema(description = "有流水支撑账户的已用课时合计")
    private BigDecimal trackedUsedLessons = BigDecimal.ZERO;

    /** 有流水支撑账户的流水净消耗合计 */
    @Schema(description = "有流水支撑账户的流水净消耗合计")
    private BigDecimal trackedFlowLessons = BigDecimal.ZERO;

    /** 无流水支撑的历史已用课时（不计入差异） */
    @Schema(description = "无流水支撑的历史已用课时")
    private BigDecimal untrackedUsedLessons = BigDecimal.ZERO;

    /** 是否一致 */
    @Schema(description = "是否一致")
    private Boolean consistent = Boolean.TRUE;

    /** 差异说明 */
    @Schema(description = "差异说明")
    private List<String> messages = new ArrayList<>();
}
