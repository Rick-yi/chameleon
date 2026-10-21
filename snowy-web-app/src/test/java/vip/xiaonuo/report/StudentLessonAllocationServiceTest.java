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
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.service.StudentLessonAllocationService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单课时分摊（退费计算）单测
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
class StudentLessonAllocationServiceTest {

    private final StudentLessonAllocationService allocationService = new StudentLessonAllocationService();

    private StudentOrder order(BigDecimal packageLessons, BigDecimal giftLessons, BigDecimal consumedLessons) {
        StudentOrder order = new StudentOrder();
        order.setPackageLessons(packageLessons);
        order.setPackageGiftLessons(giftLessons);
        order.setConsumedLessons(consumedLessons);
        return order;
    }

    @Test
    @DisplayName("只消耗了部分购买课时：可退购买课时扣减已消耗，赠课不动")
    void partiallyConsumed() {
        StudentLessonAllocationService.RefundCalc calc = allocationService.calcRefund(
                order(new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("3")));
        assertEquals(new BigDecimal("7"), calc.refundLessons());
        assertEquals(new BigDecimal("2"), calc.giftLessons());
    }

    @Test
    @DisplayName("购买课时全部消耗：可退购买课时为 0")
    void allPurchasedConsumed() {
        StudentLessonAllocationService.RefundCalc calc = allocationService.calcRefund(
                order(new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("10")));
        assertEquals(BigDecimal.ZERO, calc.refundLessons());
        assertEquals(new BigDecimal("2"), calc.giftLessons());
    }

    @Test
    @DisplayName("赠课也被消耗：赠课作废量随之扣减")
    void giftConsumed() {
        StudentLessonAllocationService.RefundCalc calc = allocationService.calcRefund(
                order(new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("12")));
        assertEquals(BigDecimal.ZERO, calc.refundLessons());
        assertEquals(BigDecimal.ZERO, calc.giftLessons());
    }

    @Test
    @DisplayName("尚未消耗：可退全部购买课时")
    void nothingConsumed() {
        StudentLessonAllocationService.RefundCalc calc = allocationService.calcRefund(
                order(new BigDecimal("10"), new BigDecimal("2"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("10"), calc.refundLessons());
        assertEquals(new BigDecimal("2"), calc.giftLessons());
    }
}
