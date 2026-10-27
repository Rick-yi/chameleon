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
package vip.xiaonuo.platform.modular.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 平台账单汇总
 *
 * @author 会飞的猪
 * @date 2026/10/26 12:00
 **/
@Getter
@Setter
public class PlatformBillSummaryDTO {

    /** 账单数 */
    @Schema(description = "账单数")
    private Long billCount = 0L;

    /** 已收款账单数 */
    @Schema(description = "已收款账单数")
    private Long paidCount = 0L;

    /** 待收款账单数 */
    @Schema(description = "待收款账单数")
    private Long unpaidCount = 0L;

    /** 已作废账单数 */
    @Schema(description = "已作废账单数")
    private Long voidCount = 0L;

    /** 账单总额 */
    @Schema(description = "账单总额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 已收款金额 */
    @Schema(description = "已收款金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 待收款金额 */
    @Schema(description = "待收款金额")
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    /** 已作废金额 */
    @Schema(description = "已作废金额")
    private BigDecimal voidAmount = BigDecimal.ZERO;
}
