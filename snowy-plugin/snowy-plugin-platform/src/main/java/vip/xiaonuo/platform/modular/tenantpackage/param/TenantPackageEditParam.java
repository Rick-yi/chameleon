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
package vip.xiaonuo.platform.modular.tenantpackage.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 套餐编辑参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class TenantPackageEditParam {

    /** id */
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 套餐名称 */
    @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "套餐名称不能为空")
    private String name;

    /** 价格(元) */
    @Schema(description = "价格(元)")
    private BigDecimal price;

    /** 有效期(天) */
    @Schema(description = "有效期(天)")
    private Integer validityDays;

    /** 功能开关配置(JSON) */
    @Schema(description = "功能开关配置(JSON)")
    private String funcConfig;

    /** 上下架状态(ON/OFF) */
    @Schema(description = "上下架状态(ON/OFF)")
    private String status;

    /** 学员数上限 */
    @Schema(description = "学员数上限")
    private Integer studentLimit;

    /** 校区数上限 */
    @Schema(description = "校区数上限")
    private Integer campusLimit;

    /** 账号数上限 */
    @Schema(description = "账号数上限")
    private Integer accountLimit;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 排序码 */
    @Schema(description = "排序码")
    private Integer sortCode;
}
