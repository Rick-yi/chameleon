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
package vip.xiaonuo.student.modular.studentrefund.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 退费金额试算
 * <p>
 * 退费金额口径只在这里实现一次：申请、试算、审批三个环节共用同一算式，
 * 避免三处各自计算后因改动漏改导致同一订单出现三个不同金额。
 * </p>
 * <p>
 * 算式：折算单价 = 实付金额 / 课包购买课时（保留4位，HALF_UP）；
 * 退费金额 = 可退购买课时 × 折算单价（保留2位，HALF_UP）。
 * 赠课不可退，不参与金额计算。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class RefundAmountCalculator {

    /** 折算单价保留位数（先保留足够精度，再乘课时，避免多次取整放大误差） */
    private static final int UNIT_PRICE_SCALE = 4;

    /** 金额保留位数（到分） */
    private static final int AMOUNT_SCALE = 2;

    private RefundAmountCalculator() {
    }

    /**
     * 计算折算单价
     *
     * @param paidAmount     订单实付金额
     * @param packageLessons 课包购买课时（不含赠课）
     * @return 折算单价
     * @throws IllegalArgumentException 课包课时为空或不大于0时抛出
     */
    public static BigDecimal unitPrice(BigDecimal paidAmount, BigDecimal packageLessons) {
        if (packageLessons == null || packageLessons.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("课包购买课时必须大于0，无法计算折算单价");
        }
        return nvl(paidAmount).divide(packageLessons, UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算退费金额
     *
     * @param refundLessons  可退购买课时（不含赠课）
     * @param paidAmount     订单实付金额
     * @param packageLessons 课包购买课时（不含赠课）
     * @return 退费金额（保留2位）
     */
    public static BigDecimal refundAmount(BigDecimal refundLessons, BigDecimal paidAmount,
                                          BigDecimal packageLessons) {
        return nvl(refundLessons).multiply(unitPrice(paidAmount, packageLessons))
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 一次性算出退费金额与折算单价
     *
     * @param refundLessons  可退购买课时（不含赠课）
     * @param paidAmount     订单实付金额
     * @param packageLessons 课包购买课时（不含赠课）
     * @return 试算结果
     */
    public static RefundAmount calc(BigDecimal refundLessons, BigDecimal paidAmount, BigDecimal packageLessons) {
        BigDecimal unitPrice = unitPrice(paidAmount, packageLessons);
        BigDecimal amount = nvl(refundLessons).multiply(unitPrice).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        return new RefundAmount(unitPrice, amount);
    }

    /**
     * 退费试算结果
     *
     * @param unitPrice    折算单价（保留4位）
     * @param refundAmount 退费金额（保留2位）
     */
    public record RefundAmount(BigDecimal unitPrice, BigDecimal refundAmount) {
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
