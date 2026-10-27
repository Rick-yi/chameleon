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
package vip.xiaonuo.platform.modular.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台经营指标DTO
 * <p>
 * 全部指标基于现有租户/套餐/续费/登录数据实时计算，不额外落统计表。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class PlatformMetricsDTO {

    /** 统计月份 yyyy-MM */
    @Schema(description = "统计月份")
    private String month;

    /** 套餐筛选id */
    @Schema(description = "套餐id")
    private String packageId;

    /** 套餐名称 */
    @Schema(description = "套餐名称")
    private String packageName;

    /** 统计月活跃租户（未停用且未到期，含试用） */
    @Schema(description = "活跃租户")
    private Long activeTenantCount = 0L;

    /** 付费租户（正常状态且配置了套餐） */
    @Schema(description = "付费租户")
    private Long payingTenantCount = 0L;

    /** MRR（按套餐价折算到月，正常状态租户） */
    @Schema(description = "MRR")
    private BigDecimal mrr = BigDecimal.ZERO;

    /** ARR（MRR × 12） */
    @Schema(description = "ARR")
    private BigDecimal arr = BigDecimal.ZERO;

    /** 当月续费金额 */
    @Schema(description = "当月续费金额")
    private BigDecimal renewalAmount = BigDecimal.ZERO;

    /** 当月续费租户数 */
    @Schema(description = "当月续费租户数")
    private Long renewalTenantCount = 0L;

    /** 续费率（续费租户 / 到期租户，%） */
    @Schema(description = "续费率(%)")
    private BigDecimal renewalRate = BigDecimal.ZERO;

    /** 当月到期未续租户数 */
    @Schema(description = "当月到期未续租户数")
    private Long expiredUnrenewedCount = 0L;

    /** 当月流失租户（到期未续且已停用） */
    @Schema(description = "当月流失租户")
    private Long churnTenantCount = 0L;

    /** 流失率（流失租户 / 月初活跃租户，%） */
    @Schema(description = "流失率(%)")
    private BigDecimal churnRate = BigDecimal.ZERO;

    /** 当月新增（试用）租户 */
    @Schema(description = "当月新增租户")
    private Long newTenantCount = 0L;

    /** 当前仍为试用状态的租户 */
    @Schema(description = "当前试用租户")
    private Long trialTenantCount = 0L;

    /** 当月新增租户中已产生续费的租户 */
    @Schema(description = "试用转化租户")
    private Long trialConvertedCount = 0L;

    /** 试用转化率（%） */
    @Schema(description = "试用转化率(%)")
    private BigDecimal trialConversionRate = BigDecimal.ZERO;

    /** 30 天内到期租户 */
    @Schema(description = "30天内到期租户")
    private Long expiring30Count = 0L;

    /** 已过期租户 */
    @Schema(description = "已过期租户")
    private Long expiredCount = 0L;

    /** 近 12 个月趋势 */
    @Schema(description = "月度趋势")
    private List<PlatformMetricTrendDTO> trend = new ArrayList<>();

    /** 当前到期分布 */
    @Schema(description = "到期分布")
    private List<PlatformExpiryBucketDTO> expiryDistribution = new ArrayList<>();
}
