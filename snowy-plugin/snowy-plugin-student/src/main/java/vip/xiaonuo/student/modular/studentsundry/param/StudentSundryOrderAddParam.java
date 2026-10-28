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
package vip.xiaonuo.student.modular.studentsundry.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 学杂费单据新增参数
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@Getter
@Setter
public class StudentSundryOrderAddParam {

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 校区id */
    @Schema(description = "校区id")
    private String campusId;

    /** 费用类型 */
    @Schema(description = "费用类型：UNIFORM/BOOK/ACTIVITY/MEAL/OTHER")
    private String feeType;

    /** 物品名称 */
    @Schema(description = "物品/费用名称")
    private String itemName;

    /** 数量 */
    @Schema(description = "数量")
    private BigDecimal quantity;

    /** 单价 */
    @Schema(description = "单价")
    private BigDecimal unitPrice;

    /** 应收金额（不填时按 数量 × 单价 计算） */
    @Schema(description = "应收金额")
    private BigDecimal amount;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 是否立即收款 */
    @Schema(description = "是否立即收款")
    private Boolean payNow;

    /** 收款渠道（payNow 为 true 时使用） */
    @Schema(description = "收款渠道")
    private String payChannel;

    /** 实收金额（payNow 为 true 且与应收不同时使用） */
    @Schema(description = "实收金额")
    private BigDecimal paidAmount;
}
