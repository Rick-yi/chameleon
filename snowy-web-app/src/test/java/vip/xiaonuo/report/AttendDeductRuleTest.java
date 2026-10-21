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
import vip.xiaonuo.course.core.util.AttendDeductRule;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 点名扣课规则单测
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
class AttendDeductRuleTest {

    @Test
    @DisplayName("到课必扣课时")
    void attendAlwaysDeduct() {
        assertEquals(new BigDecimal("1.5"), AttendDeductRule.deductLessons(new BigDecimal("1.5"), false, true, "ATTEND"));
    }

    @Test
    @DisplayName("缺勤默认扣课，可配置为不扣")
    void absentDeductConfigurable() {
        assertEquals(BigDecimal.ONE, AttendDeductRule.deductLessons(null, false, null, "ABSENT"));
        assertEquals(BigDecimal.ZERO, AttendDeductRule.deductLessons(null, false, false, "ABSENT"));
    }

    @Test
    @DisplayName("请假默认不扣课，可配置为扣")
    void leaveDeductConfigurable() {
        assertEquals(BigDecimal.ZERO, AttendDeductRule.deductLessons(null, false, true, "LEAVE"));
        assertEquals(BigDecimal.ONE, AttendDeductRule.deductLessons(null, true, true, "LEAVE"));
    }

    @Test
    @DisplayName("未配置每课扣课时数时按 1 课时计")
    void defaultLessonsPerClass() {
        assertEquals(BigDecimal.ONE, AttendDeductRule.deductLessons(null, false, true, "ATTEND"));
    }
}
