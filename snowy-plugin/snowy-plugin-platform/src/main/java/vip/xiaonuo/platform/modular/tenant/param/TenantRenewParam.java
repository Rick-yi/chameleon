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
package vip.xiaonuo.platform.modular.tenant.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 租户续费参数
 *
 * @author 会飞的猪
 * @date 2026/10/26 09:00
 **/
@Getter
@Setter
public class TenantRenewParam {

    /** 租户id */
    @Schema(description = "租户id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "租户id不能为空")
    private String id;

    /** 续费天数 */
    @Schema(description = "续费天数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "续费天数不能为空")
    @Min(value = 1, message = "续费天数至少为1天")
    @Max(value = 3650, message = "续费天数不能超过3650天")
    private Integer days;

    /** 续费套餐id，为空则沿用当前套餐 */
    @Schema(description = "续费套餐id")
    private String packageId;

    /** 续费金额 */
    @Schema(description = "续费金额")
    private BigDecimal amount;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
