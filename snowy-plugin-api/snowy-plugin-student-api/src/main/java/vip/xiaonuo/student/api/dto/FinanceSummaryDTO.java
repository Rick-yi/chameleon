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

/**
 * 财务汇总DTO
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class FinanceSummaryDTO {

    /** 收款金额 */
    @Schema(description = "收款金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 退费金额 */
    @Schema(description = "退费金额")
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** 净营收 */
    @Schema(description = "净营收")
    private BigDecimal netAmount = BigDecimal.ZERO;

    /** 已支付订单数 */
    @Schema(description = "已支付订单数")
    private Long orderCount = 0L;

    /** 付费学员数 */
    @Schema(description = "付费学员数")
    private Long payingStudentCount = 0L;

    /** 客单价 */
    @Schema(description = "客单价")
    private BigDecimal avgOrderAmount = BigDecimal.ZERO;

    /** 退费率（百分比） */
    @Schema(description = "退费率（百分比）")
    private BigDecimal refundRate = BigDecimal.ZERO;

    /** 待收款金额 */
    @Schema(description = "待收款金额")
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    /** 待收款订单数 */
    @Schema(description = "待收款订单数")
    private Long unpaidOrderCount = 0L;

    /** 本月新增学员 */
    @Schema(description = "本月新增学员")
    private Long newStudentCount = 0L;

    /** 在读学员数 */
    @Schema(description = "在读学员数")
    private Long studyingStudentCount = 0L;

    /** 课消课时（净消耗） */
    @Schema(description = "课消课时")
    private BigDecimal attendLessons = BigDecimal.ZERO;

    /** 待处理续费预警数 */
    @Schema(description = "待处理续费预警数")
    private Long renewWarnCount = 0L;

    /** 课消金额（权责口径） */
    @Schema(description = "课消金额（权责口径）")
    private BigDecimal recognizedAmount = BigDecimal.ZERO;

    /** 生均收入 */
    @Schema(description = "生均收入")
    private BigDecimal perStudentRevenue = BigDecimal.ZERO;

    /** 实收率（百分比） */
    @Schema(description = "实收率（百分比）")
    private BigDecimal collectionRate = BigDecimal.ZERO;
}