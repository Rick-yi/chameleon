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
package vip.xiaonuo.student.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 课时收入确认工具
 * <p>
 * 权责口径：课消金额 = 消课课时 × 订单购买课时单价。
 * 点名消课为收入确认（+），撤销点名回补为冲减（-），
 * 退费销课退回的是「未消耗」课时（本就未确认收入），不参与冲减。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public class LessonRevenueUtil {

    /** 点名消课 */
    public static final String FLOW_ATTEND = "ATTEND";

    /** 回补（撤销点名） */
    public static final String FLOW_RESTORE = "RESTORE";

    /** 退费销课 */
    public static final String FLOW_REFUND = "REFUND";

    /**
     * 订单购买课时单价（实收 ÷ 购买课时）
     *
     * @param paidAmount    实收金额
     * @param packageLessons 购买课时（不含赠课）
     * @return 单价；购买课时为空或不大于 0 时返回 null
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public static BigDecimal unitPrice(BigDecimal paidAmount, BigDecimal packageLessons) {
        if (packageLessons == null || packageLessons.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        return paid.divide(packageLessons, 6, RoundingMode.HALF_UP);
    }

    /**
     * 按流水类型计算收入确认金额
     *
     * @param lessons  课时绝对值
     * @param unitPrice 单价（为空表示无法定价，不计入金额）
     * @param flowType 流水类型
     * @return 收入确认金额（可为负，表示冲减）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public static BigDecimal recognizedAmount(BigDecimal lessons, BigDecimal unitPrice, String flowType) {
        if (lessons == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal absLessons = lessons.abs();
        if (FLOW_ATTEND.equals(flowType)) {
            return absLessons.multiply(unitPrice);
        }
        if (FLOW_RESTORE.equals(flowType)) {
            return absLessons.multiply(unitPrice).negate();
        }
        // 退费销课等其他类型不参与收入确认
        return BigDecimal.ZERO;
    }
}
