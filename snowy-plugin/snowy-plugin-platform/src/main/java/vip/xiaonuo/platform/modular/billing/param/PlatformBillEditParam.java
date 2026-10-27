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
package vip.xiaonuo.platform.modular.billing.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 账单编辑参数
 *
 * @author 会飞的猪
 * @date 2026/10/26 12:00
 **/
@Getter
@Setter
public class PlatformBillEditParam {

    /** 主键 */
    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 租户id */
    @Schema(description = "租户id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "租户id不能为空")
    private String tenantId;

    /** 套餐id */
    @Schema(description = "套餐id")
    private String packageId;

    /** 账单周期开始 */
    @Schema(description = "账单周期开始")
    private Date periodStart;

    /** 账单周期结束 */
    @Schema(description = "账单周期结束")
    private Date periodEnd;

    /** 金额(元) */
    @Schema(description = "金额(元)")
    private BigDecimal amount;

    /** 状态(UNPAID/PAID/VOID) */
    @Schema(description = "状态")
    private String status;

    /** 收款方式 */
    @Schema(description = "收款方式")
    private String payMethod;

    /** 收款时间 */
    @Schema(description = "收款时间")
    private Date payTime;

    /** 发票状态 */
    @Schema(description = "发票状态")
    private String invoiceStatus;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
