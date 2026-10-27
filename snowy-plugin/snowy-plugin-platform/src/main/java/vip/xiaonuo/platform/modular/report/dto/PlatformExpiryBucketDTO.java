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

/**
 * 租户到期分布
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class PlatformExpiryBucketDTO {

    /** 分档编码 */
    @Schema(description = "分档编码")
    private String bucket;

    /** 分档名称 */
    @Schema(description = "分档名称")
    private String label;

    /** 分档下界天数（含） */
    @Schema(description = "分档下界天数")
    private Integer minDays;

    /** 分档上界天数（含，null 表示无上限） */
    @Schema(description = "分档上界天数")
    private Integer maxDays;

    /** 租户数 */
    @Schema(description = "租户数")
    private Long tenantCount = 0L;

    /** 这些租户折算的 MRR */
    @Schema(description = "折算MRR")
    private BigDecimal mrr = BigDecimal.ZERO;
}
