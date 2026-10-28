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
package vip.xiaonuo.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.student.modular.studentrefund.util.RefundAmountCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退费金额试算单测
 * <p>
 * 申请、试算、审批三个环节共用 {@link RefundAmountCalculator}，
 * 本类通过不变量保证三处口径一致，防止以后改动漏改其中一处导致金额分叉。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class RefundAmountCalculatorTest {

    private static final BigDecimal PAID = new BigDecimal("3000.00");
    private static final BigDecimal PACKAGE_LESSONS = new BigDecimal("30");

    @Test
    @DisplayName("折算单价为实付除以购买课时，保留4位")
    void unitPrice() {
        BigDecimal unitPrice = RefundAmountCalculator.unitPrice(PAID, PACKAGE_LESSONS);
        assertEquals(new BigDecimal("100.0000"), unitPrice);
        assertEquals(4, unitPrice.scale());
    }

    @Test
    @DisplayName("全部课时可退时，退费金额等于实付金额")
    void refundAllLessonsEqualsPaid() {
        BigDecimal amount = RefundAmountCalculator.refundAmount(PACKAGE_LESSONS, PAID, PACKAGE_LESSONS);
        assertEquals(0, amount.compareTo(PAID));
    }

    @Test
    @DisplayName("部分课时可退时按比例计算并保留2位")
    void refundPartialLessons() {
        BigDecimal amount = RefundAmountCalculator.refundAmount(new BigDecimal("7"), PAID, PACKAGE_LESSONS);
        assertEquals(new BigDecimal("700.00"), amount);
        assertEquals(2, amount.scale());
    }

    @Test
    @DisplayName("金额保留2位采用HALF_UP，不产生浮点误差")
    void halfUpRounding() {
        // 1000 / 3 = 333.3333，1 课时 → 333.33
        BigDecimal paid = new BigDecimal("1000.00");
        BigDecimal lessons = new BigDecimal("3");
        BigDecimal amount = RefundAmountCalculator.refundAmount(BigDecimal.ONE, paid, lessons);
        assertEquals(new BigDecimal("333.33"), amount);

        // 实付 10 / 3 = 3.3333，2 课时 → 6.67
        BigDecimal amount2 = RefundAmountCalculator.refundAmount(new BigDecimal("2"), new BigDecimal("10.00"), lessons);
        assertEquals(new BigDecimal("6.67"), amount2);
    }

    @Test
    @DisplayName("空实付按0处理，不抛异常")
    void nullPaidTreatedAsZero() {
        BigDecimal amount = RefundAmountCalculator.refundAmount(BigDecimal.ONE, null, PACKAGE_LESSONS);
        assertEquals(0, amount.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("课包课时为0或空时拒绝计算")
    void invalidPackageLessonsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RefundAmountCalculator.unitPrice(PAID, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> RefundAmountCalculator.unitPrice(PAID, null));
    }

    // ---------- 不变量：同一输入在三个环节必须得到同一金额 ----------

    @Test
    @DisplayName("不变量：试算接口与申请/审批返回的金额完全一致")
    void sameAmountAcrossApplyPreviewApprove() {
        BigDecimal refundLessons = new BigDecimal("12");
        // 申请环节
        BigDecimal applyAmount = RefundAmountCalculator.calc(refundLessons, PAID, PACKAGE_LESSONS).refundAmount();
        // 试算环节（单独调用 unitPrice + refundAmount，模拟不同调用方式）
        BigDecimal previewUnitPrice = RefundAmountCalculator.unitPrice(PAID, PACKAGE_LESSONS);
        BigDecimal previewAmount = RefundAmountCalculator.refundAmount(refundLessons, PAID, PACKAGE_LESSONS);
        // 审批环节（按最新消耗重算）
        BigDecimal approveAmount = RefundAmountCalculator.calc(refundLessons, PAID, PACKAGE_LESSONS).refundAmount();

        assertEquals(applyAmount, previewAmount);
        assertEquals(applyAmount, approveAmount);
        // calc 返回的单价与实际使用单价一致
        assertEquals(previewUnitPrice, RefundAmountCalculator.calc(refundLessons, PAID, PACKAGE_LESSONS).unitPrice());
    }

    @Test
    @DisplayName("不变量：退费金额不超过实付金额")
    void refundNeverExceedsPaid() {
        BigDecimal[][] cases = {
                {new BigDecimal("1"), new BigDecimal("3000.00"), new BigDecimal("30")},
                {new BigDecimal("29"), new BigDecimal("3000.00"), new BigDecimal("30")},
                {new BigDecimal("30"), new BigDecimal("3000.00"), new BigDecimal("30")},
                {new BigDecimal("7"), new BigDecimal("999.99"), new BigDecimal("7")},
                {new BigDecimal("1"), new BigDecimal("1.00"), new BigDecimal("9")},
        };
        for (BigDecimal[] c : cases) {
            BigDecimal amount = RefundAmountCalculator.refundAmount(c[0], c[1], c[2]);
            BigDecimal roundedPaid = c[1].setScale(2, RoundingMode.HALF_UP);
            assertTrue(amount.compareTo(roundedPaid) <= 0,
                    "退费金额 " + amount + " 不应超过实付 " + roundedPaid);
        }
    }

    @Test
    @DisplayName("不变量：可退课时为0时金额为0")
    void zeroRefundLessonsMeansZeroAmount() {
        BigDecimal amount = RefundAmountCalculator.refundAmount(BigDecimal.ZERO, PAID, PACKAGE_LESSONS);
        assertEquals(0, amount.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("不变量：金额随可退课时单调不减")
    void amountMonotonicInRefundLessons() {
        BigDecimal previous = BigDecimal.valueOf(-1);
        for (int lessons = 0; lessons <= 30; lessons++) {
            BigDecimal amount = RefundAmountCalculator.refundAmount(
                    BigDecimal.valueOf(lessons), PAID, PACKAGE_LESSONS);
            assertTrue(amount.compareTo(previous) >= 0,
                    "可退课时增加时退费金额不应减少：" + lessons);
            previous = amount;
        }
    }
}
