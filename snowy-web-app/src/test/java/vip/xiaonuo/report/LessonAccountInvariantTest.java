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
import vip.xiaonuo.student.modular.studentlessonaccount.util.LessonAccountInvariant;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课时账户账务不变量单测
 * <p>
 * 不变量：剩余 = 总课时 - 已用。该式由点名扣课、回补、报名、退费四条 SQL 分别维护，
 * 任一处写错都不会立即报错，只会让退费试算或报表静默算错。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class LessonAccountInvariantTest {

    @Test
    @DisplayName("三项自洽时校验通过")
    void consistentAccount() {
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(
                new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("20"));
        assertTrue(r.consistent());
        assertEquals(0, r.drift().compareTo(BigDecimal.ZERO));
        assertFalse(r.overCredited());
        assertFalse(r.underCredited());
    }

    @Test
    @DisplayName("精度不同但数值相等时仍判定自洽")
    void differentScaleStillConsistent() {
        // 30.00 - 10.00 = 20.0000，用 equals 会误判为不一致
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(
                new BigDecimal("30.00"), new BigDecimal("10.00"), new BigDecimal("20.0000"));
        assertTrue(r.consistent(), "BigDecimal 精度差异不应影响一致性判定");
    }

    @Test
    @DisplayName("少扣课时导致剩余虚高，偏差为正")
    void overCreditedDetected() {
        // 应剩 20，实际剩 22 → 少扣了 2 课时
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(
                new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("22"));
        assertFalse(r.consistent());
        assertTrue(r.overCredited());
        assertEquals(0, r.drift().compareTo(new BigDecimal("2")));
    }

    @Test
    @DisplayName("多扣课时导致剩余偏低，偏差为负")
    void underCreditedDetected() {
        // 应剩 20，实际剩 18 → 多扣了 2 课时
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(
                new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("18"));
        assertFalse(r.consistent());
        assertTrue(r.underCredited());
        assertEquals(0, r.drift().compareTo(new BigDecimal("-2")));
    }

    @Test
    @DisplayName("空值按 0 处理，不抛空指针")
    void nullTreatedAsZero() {
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(null, null, null);
        assertTrue(r.consistent());
        assertEquals(0, r.expected().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("剩余为负被识别为异常")
    void negativeRemainDetected() {
        assertTrue(LessonAccountInvariant.isNegativeRemain(new BigDecimal("-1")));
        assertFalse(LessonAccountInvariant.isNegativeRemain(BigDecimal.ZERO));
        assertFalse(LessonAccountInvariant.isNegativeRemain(null));
    }

    @Test
    @DisplayName("已用超过总量被识别为异常")
    void overusedDetected() {
        assertTrue(LessonAccountInvariant.isOverused(new BigDecimal("10"), new BigDecimal("11")));
        // 刚好用满不算异常
        assertFalse(LessonAccountInvariant.isOverused(new BigDecimal("10"), new BigDecimal("10")));
        assertFalse(LessonAccountInvariant.isOverused(null, null));
    }

    @Test
    @DisplayName("不变量在各种正常业务序列下保持")
    void invariantHoldsAcrossBusinessSequence() {
        // 报名 30 课时
        BigDecimal total = new BigDecimal("30");
        BigDecimal used = BigDecimal.ZERO;
        BigDecimal remain = new BigDecimal("30");
        assertTrue(LessonAccountInvariant.check(total, used, remain).consistent());

        // 点名扣 3 次各 1 课时
        for (int i = 0; i < 3; i++) {
            used = used.add(BigDecimal.ONE);
            remain = remain.subtract(BigDecimal.ONE);
            assertTrue(LessonAccountInvariant.check(total, used, remain).consistent(),
                    "第 " + (i + 1) + " 次扣课后应保持自洽");
        }

        // 回补 1 课时
        used = used.subtract(BigDecimal.ONE);
        remain = remain.add(BigDecimal.ONE);
        assertTrue(LessonAccountInvariant.check(total, used, remain).consistent());

        // 退费扣掉 10 课时总量
        total = total.subtract(new BigDecimal("10"));
        remain = remain.subtract(new BigDecimal("10"));
        assertTrue(LessonAccountInvariant.check(total, used, remain).consistent());
        assertEquals(0, remain.compareTo(new BigDecimal("18")));
        assertEquals(0, used.compareTo(new BigDecimal("2")));
    }

    @Test
    @DisplayName("退费只扣总量不扣已用，剩余相应减少后仍自洽")
    void refundKeepsInvariant() {
        // 初始 20 总量、用 5、剩 15；退费扣 4 总量
        BigDecimal total = new BigDecimal("20").subtract(new BigDecimal("4"));
        BigDecimal used = new BigDecimal("5");
        BigDecimal remain = new BigDecimal("15").subtract(new BigDecimal("4"));
        assertTrue(LessonAccountInvariant.check(total, used, remain).consistent());
        assertEquals(0, remain.compareTo(new BigDecimal("11")));
    }
}
