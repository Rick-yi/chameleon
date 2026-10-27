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
package vip.xiaonuo.student.modular.studentrenewarn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 续费/流失统计（轻量口径）
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class StudentRenewWarnStatsVO {

    /** 统计开始时间 */
    @Schema(description = "统计开始时间")
    private String startTime;

    /** 统计结束时间 */
    @Schema(description = "统计结束时间")
    private String endTime;

    /** 预警总数 */
    @Schema(description = "预警总数")
    private Long warnTotal = 0L;

    /** 待处理 */
    @Schema(description = "待处理")
    private Long pendingCount = 0L;

    /** 已处理 */
    @Schema(description = "已处理")
    private Long processedCount = 0L;

    /** 已续费 */
    @Schema(description = "已续费")
    private Long renewedCount = 0L;

    /** 已忽略 */
    @Schema(description = "已忽略")
    private Long ignoredCount = 0L;

    /** 预警涉及学员数 */
    @Schema(description = "预警涉及学员数")
    private Long warnStudentCount = 0L;

    /** 续费率(%) = 已续费预警数 / 预警总数 */
    @Schema(description = "续费率(%)")
    private BigDecimal renewedRate = BigDecimal.ZERO;

    /** 流失登记数 */
    @Schema(description = "流失登记数")
    private Long lossCount = 0L;

    /** 流失学员数 */
    @Schema(description = "流失学员数")
    private Long lossStudentCount = 0L;

    /** 流失率(%) = 流失学员数 / 预警涉及学员数 */
    @Schema(description = "流失率(%)")
    private BigDecimal churnRate = BigDecimal.ZERO;
}
