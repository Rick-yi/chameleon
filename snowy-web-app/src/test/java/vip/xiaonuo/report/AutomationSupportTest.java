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
import vip.xiaonuo.biz.api.automation.AutomationSupport;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自动化公共逻辑单测
 *
 * @author 会飞的猪
 * @date 2026/10/26 18:30
 **/
class AutomationSupportTest {

    @Test
    @DisplayName("holdout 分组稳定且在 0-99")
    void holdoutBucketStable() {
        int first = AutomationSupport.holdoutBucket("t1", "AUTO_NOTIFY_UNPAID_ORDER", "o1");
        int second = AutomationSupport.holdoutBucket("t1", "AUTO_NOTIFY_UNPAID_ORDER", "o1");
        assertEquals(first, second);
        assertTrue(first >= 0 && first < 100);
    }

    @Test
    @DisplayName("holdout 0/100 边界")
    void holdoutBoundary() {
        assertFalse(AutomationSupport.isHoldout("t1", "rule", "target", 0));
        assertTrue(AutomationSupport.isHoldout("t1", "rule", "target", 100));
    }

    @Test
    @DisplayName("静默时段支持普通区间和跨午夜区间")
    void quietHours() {
        assertTrue(AutomationSupport.inQuietHours(LocalTime.of(23, 0), "22:00", "08:00"));
        assertTrue(AutomationSupport.inQuietHours(LocalTime.of(7, 59), "22:00", "08:00"));
        assertFalse(AutomationSupport.inQuietHours(LocalTime.of(12, 0), "22:00", "08:00"));
        assertTrue(AutomationSupport.inQuietHours(LocalTime.of(12, 0), "12:00", "13:00"));
        assertFalse(AutomationSupport.inQuietHours(LocalTime.of(13, 0), "12:00", "13:00"));
    }

    @Test
    @DisplayName("幂等键包含窗口，跨窗口不同")
    void idempotencyKey() {
        String first = AutomationSupport.idempotencyKey("rule", "NOTIFY", "target", "R24:1");
        String same = AutomationSupport.idempotencyKey("rule", "NOTIFY", "target", "R24:1");
        String next = AutomationSupport.idempotencyKey("rule", "NOTIFY", "target", "R24:2");
        assertEquals(first, same);
        assertNotEquals(first, next);
    }

    @Test
    @DisplayName("HH:mm 格式校验")
    void hhMmValidation() {
        assertTrue(AutomationSupport.isValidHhMm(null));
        assertTrue(AutomationSupport.isValidHhMm(""));
        assertTrue(AutomationSupport.isValidHhMm("08:00"));
        assertFalse(AutomationSupport.isValidHhMm("8:00"));
        assertFalse(AutomationSupport.isValidHhMm("24:00"));
    }
}
