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
package vip.xiaonuo.finance.modular.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.student.api.dto.FinanceSummaryDTO;

/**
 * 财务对比期结果DTO
 * <p>
 * 同时返回环比（紧邻的等长上一周期）、同比（去年同期）与当前选择的对比期，
 * 固定周期口径保持不变，仅在原有汇总接口之外补充对比数据。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26
 **/
@Getter
@Setter
public class FinanceCompareDTO {

    /** 对比方式：MOM 环比 / YOY 同比 / CUSTOM 自定义 */
    @Schema(description = "对比方式：MOM/YOY/CUSTOM")
    private String mode = "MOM";

    /** 当前统计区间开始时间 */
    @Schema(description = "当前统计区间开始时间")
    private String startTime;

    /** 当前统计区间结束时间 */
    @Schema(description = "当前统计区间结束时间")
    private String endTime;

    /** 对比期开始时间 */
    @Schema(description = "对比期开始时间")
    private String compareStartTime;

    /** 对比期结束时间 */
    @Schema(description = "对比期结束时间")
    private String compareEndTime;

    /** 当前选择的对比期汇总 */
    @Schema(description = "当前选择的对比期汇总")
    private FinanceSummaryDTO compareSummary = new FinanceSummaryDTO();

    /** 环比汇总（紧邻的等长上一周期） */
    @Schema(description = "环比汇总")
    private FinanceSummaryDTO previousSummary = new FinanceSummaryDTO();

    /** 同比汇总（去年同期） */
    @Schema(description = "同比汇总")
    private FinanceSummaryDTO yoySummary = new FinanceSummaryDTO();

    /** 数据生成时间（服务端时间，用于页面展示数据更新时间） */
    @Schema(description = "数据生成时间")
    private String generatedAt;
}
