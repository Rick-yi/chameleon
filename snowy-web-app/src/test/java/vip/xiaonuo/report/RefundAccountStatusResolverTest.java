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
import vip.xiaonuo.student.modular.studentrefund.util.RefundAccountStatusResolver;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退费后课时账户状态判定单测
 * <p>
 * 判定错了会让账户永久停留在冻结态（学员无法上课）或错误清零（课时丢失），
 * 都是账务级故障，因此把边界固定下来。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class RefundAccountStatusResolverTest {

    @Test
    @DisplayName("还有剩余课时时回到正常状态")
    void positiveRemainIsNormal() {
        assertEquals(RefundAccountStatusResolver.NORMAL,
                RefundAccountStatusResolver.resolve(new BigDecimal("1")));
        assertEquals(RefundAccountStatusResolver.NORMAL,
                RefundAccountStatusResolver.resolve(new BigDecimal("0.5")));
    }

    @Test
    @DisplayName("剩余课时为 0 时清零")
    void zeroRemainIsCleared() {
        assertEquals(RefundAccountStatusResolver.CLEARED,
                RefundAccountStatusResolver.resolve(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("不同精度的 0 都判定为清零")
    void zeroWithScaleIsCleared() {
        // BigDecimal("0.00").equals(BigDecimal.ZERO) 为 false，必须用 compareTo
        assertEquals(RefundAccountStatusResolver.CLEARED,
                RefundAccountStatusResolver.resolve(new BigDecimal("0.00")));
        assertEquals(RefundAccountStatusResolver.CLEARED,
                RefundAccountStatusResolver.resolve(new BigDecimal("0.0000")));
    }

    @Test
    @DisplayName("剩余课时为 null 视为清零，避免空指针")
    void nullRemainIsCleared() {
        assertEquals(RefundAccountStatusResolver.CLEARED,
                RefundAccountStatusResolver.resolve(null));
    }

    @Test
    @DisplayName("负数剩余也判为清零")
    void negativeRemainIsCleared() {
        assertEquals(RefundAccountStatusResolver.CLEARED,
                RefundAccountStatusResolver.resolve(new BigDecimal("-1")));
    }

    // ---------- 扣减前置校验 ----------

    @Test
    @DisplayName("余额充足时允许扣减")
    void sufficientBalanceCanDeduct() {
        assertTrue(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("5"), new BigDecimal("2"),
                new BigDecimal("10"), new BigDecimal("3")));
    }

    @Test
    @DisplayName("余额刚好相等时允许扣减（含等号）")
    void exactBalanceCanDeduct() {
        assertTrue(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("5"), new BigDecimal("2"),
                new BigDecimal("5"), new BigDecimal("2")));
    }

    @Test
    @DisplayName("剩余课时不足时拒绝扣减")
    void insufficientRemainRejected() {
        assertFalse(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("5"), BigDecimal.ZERO,
                new BigDecimal("4"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("赠课余额不足时拒绝扣减")
    void insufficientGiftRejected() {
        assertFalse(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("5"), new BigDecimal("3"),
                new BigDecimal("10"), new BigDecimal("2")));
    }

    @Test
    @DisplayName("扣减课时为 0 或负数时拒绝")
    void nonPositiveRemoveRejected() {
        assertFalse(RefundAccountStatusResolver.canDeduct(
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("10")));
        assertFalse(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("-1"), BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("10")));
    }

    @Test
    @DisplayName("赠课扣减为负数时拒绝")
    void negativeGiftRemoveRejected() {
        assertFalse(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("5"), new BigDecimal("-1"),
                new BigDecimal("10"), new BigDecimal("10")));
    }

    @Test
    @DisplayName("余额为 null 视为 0，拒绝非零扣减")
    void nullBalanceTreatedAsZero() {
        assertFalse(RefundAccountStatusResolver.canDeduct(
                new BigDecimal("1"), BigDecimal.ZERO, null, null));
        // 扣减 0 本身已在外层被拒，这里确认不会因 null 抛异常
        assertFalse(RefundAccountStatusResolver.canDeduct(
                BigDecimal.ZERO, BigDecimal.ZERO, null, null));
    }
}
