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
package vip.xiaonuo.platform.modular.billing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 平台内部账单/收款记录
 * <p>
 * 仅用于平台运营内部对账，不接真实支付渠道与发票平台。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 12:00
 **/
@Getter
@Setter
@TableName("T_PLATFORM_BILL")
public class PlatformBill extends CommonEntity {

    /** 主键 */
    @Schema(description = "主键")
    private String id;

    /** 账单号 */
    @Schema(description = "账单号")
    private String billNo;

    /** 租户id */
    @Schema(description = "租户id")
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
    @Schema(description = "状态(UNPAID/PAID/VOID)")
    private String status;

    /** 收款方式(OFFLINE/BANK/OTHER) */
    @Schema(description = "收款方式")
    private String payMethod;

    /** 收款时间 */
    @Schema(description = "收款时间")
    private Date payTime;

    /** 发票状态(NONE/REQUESTED/ISSUED)，仅人工登记 */
    @Schema(description = "发票状态")
    private String invoiceStatus;

    /** 来源(MANUAL/RENEW) */
    @Schema(description = "来源")
    private String source;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 操作人id */
    @Schema(description = "操作人id")
    private String operatorId;

    /** 操作人 */
    @Schema(description = "操作人")
    private String operatorName;

    /** 租户名称（展示用） */
    @Schema(description = "租户名称")
    @TableField(exist = false)
    private String tenantName;

    /** 租户编码（展示用） */
    @Schema(description = "租户编码")
    @TableField(exist = false)
    private String tenantCode;

    /** 套餐名称（展示用） */
    @Schema(description = "套餐名称")
    @TableField(exist = false)
    private String packageName;
}
