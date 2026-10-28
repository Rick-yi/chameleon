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
package vip.xiaonuo.student.modular.studentsundry.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 学员学杂费单据实体
 * <p>
 * 与课包订单（STUDENT_ORDER）分账：学杂费没有课时、没有消课、不参与课消确认收入，
 * 也不进入客单价与续费率的分母。独立成表是为了让「课包指标」在结构上不可能被污染。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@Getter
@Setter
@TableName("STUDENT_SUNDRY_ORDER")
public class StudentSundryOrder extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 校区id */
    @Schema(description = "校区id")
    private String campusId;

    /** 费用类型：UNIFORM 校服 / BOOK 教材 / ACTIVITY 活动费 / MEAL 餐费 / OTHER 其他 */
    @Schema(description = "费用类型")
    private String feeType;

    /** 物品/费用名称 */
    @Schema(description = "物品名称")
    private String itemName;

    /** 数量 */
    @Schema(description = "数量")
    private BigDecimal quantity;

    /** 单价 */
    @Schema(description = "单价")
    private BigDecimal unitPrice;

    /** 应收金额 */
    @Schema(description = "应收金额")
    private BigDecimal amount;

    /** 实收金额 */
    @Schema(description = "实收金额")
    private BigDecimal paidAmount;

    /** 已退金额 */
    @Schema(description = "已退金额")
    private BigDecimal refundedAmount;

    /** 收款渠道 */
    @Schema(description = "收款渠道")
    private String payChannel;

    /** 外部交易号 */
    @Schema(description = "外部交易号")
    private String tradeNo;

    /** 收款时间 */
    @Schema(description = "收款时间")
    private Date payTime;

    /** 退费时间 */
    @Schema(description = "退费时间")
    private Date refundTime;

    /** 状态 */
    @Schema(description = "状态")
    private String status;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 收据号 */
    @Schema(description = "收据号")
    private String receiptNo;

    /** 学员姓名（列表查询时填充，非数据库字段） */
    @Schema(description = "学员姓名")
    @TableField(exist = false)
    private String studentName;

    /** 校区名称（列表查询时填充，非数据库字段） */
    @Schema(description = "校区名称")
    @TableField(exist = false)
    private String campusName;
}
