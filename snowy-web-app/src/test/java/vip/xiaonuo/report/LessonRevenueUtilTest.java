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
import vip.xiaonuo.student.core.util.LessonRevenueUtil;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 课时收入确认口径单测
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
class LessonRevenueUtilTest {

    @Test
    @DisplayName("单价 = 实收 ÷ 购买课时")
    void unitPrice() {
        assertEquals(0, new BigDecimal("100")
                .compareTo(LessonRevenueUtil.unitPrice(new BigDecimal("1000"), new BigDecimal("10"))));
    }

    @Test
    @DisplayName("购买课时为空时无法定价")
    void unitPriceWithoutPackageLessons() {
        assertNull(LessonRevenueUtil.unitPrice(new BigDecimal("1000"), null));
        assertNull(LessonRevenueUtil.unitPrice(new BigDecimal("1000"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("点名消课确认收入")
    void attendRecognizesRevenue() {
        assertEquals(0, new BigDecimal("300")
                .compareTo(LessonRevenueUtil.recognizedAmount(new BigDecimal("3"), new BigDecimal("100"), "ATTEND")));
    }

    @Test
    @DisplayName("撤销点名回补冲减收入")
    void restoreReversesRevenue() {
        assertEquals(0, new BigDecimal("-300")
                .compareTo(LessonRevenueUtil.recognizedAmount(new BigDecimal("3"), new BigDecimal("100"), "RESTORE")));
    }

    @Test
    @DisplayName("退费销课（退回未消耗课时）不冲减收入")
    void refundDoesNotReverseRevenue() {
        assertEquals(BigDecimal.ZERO,
                LessonRevenueUtil.recognizedAmount(new BigDecimal("20"), new BigDecimal("100"), "REFUND"));
    }

    @Test
    @DisplayName("无法定价时不计入金额")
    void noPriceNoAmount() {
        assertEquals(BigDecimal.ZERO,
                LessonRevenueUtil.recognizedAmount(new BigDecimal("3"), null, "ATTEND"));
    }
}
