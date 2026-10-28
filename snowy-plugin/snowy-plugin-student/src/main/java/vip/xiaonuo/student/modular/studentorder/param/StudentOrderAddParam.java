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
package vip.xiaonuo.student.modular.studentorder.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 报名下单参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class StudentOrderAddParam {

    /** 学员id */
    @Schema(description = "学员id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "学员id不能为空")
    private String studentId;

    /** 课包id（预设课包下单时必填；自定义课包时留空） */
    @Schema(description = "课包id（自定义课包时留空）")
    private String coursePackageId;

    /** 支付渠道 */
    @Schema(description = "支付渠道(微信/支付宝/线下)")
    private String payChannel;

    // ---------- 自定义课包（coursePackageId 为空时生效） ----------

    /** 课程id（自定义课包必填） */
    @Schema(description = "课程id（自定义课包必填）")
    private String courseId;

    /** 课包名称（自定义课包，留空默认「自定义课包」） */
    @Schema(description = "课包名称")
    private String packageName;

    /** 购买课时（自定义课包必填） */
    @Schema(description = "购买课时（自定义课包必填）")
    private java.math.BigDecimal packageLessons;

    /** 赠送课时（自定义课包，默认 0） */
    @Schema(description = "赠送课时")
    private java.math.BigDecimal packageGiftLessons;

    /** 有效期天数（自定义课包，默认 0 表示不限） */
    @Schema(description = "有效期天数")
    private Integer packageValidityDays;

    /** 应收金额（自定义课包必填） */
    @Schema(description = "应收金额（自定义课包必填）")
    private java.math.BigDecimal amount;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
