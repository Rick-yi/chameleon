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
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;
import vip.xiaonuo.student.modular.studentrenewarn.util.RenewWarnRules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 续费预警判定规则单测
 * <p>
 * 这些规则改错了不会抛异常，只会让整机构的续费预警静默失效或误报，
 * 所以用测试把边界固定下来。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class RenewWarnRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 10, 0);
    private static final BigDecimal THRESHOLD = new BigDecimal("3");
    private static final int EXPIRE_DAYS = 7;
    private static final int REPEAT_DAYS = 7;

    // ---------- 预警原因判定 ----------

    @Test
    @DisplayName("剩余课时等于阈值时预警（含等号）")
    void lessonsEqualToThresholdWarns() {
        String reason = RenewWarnRules.resolveWarnReason(
                new BigDecimal("3"), null, THRESHOLD, EXPIRE_DAYS, NOW);
        assertEquals(StudentRenewWarnStatusEnum.REASON_LESSON_LOW, reason);
    }

    @Test
    @DisplayName("剩余课时低于阈值时预警")
    void lessonsBelowThresholdWarns() {
        String reason = RenewWarnRules.resolveWarnReason(
                new BigDecimal("1"), null, THRESHOLD, EXPIRE_DAYS, NOW);
        assertEquals(StudentRenewWarnStatusEnum.REASON_LESSON_LOW, reason);
    }

    @Test
    @DisplayName("课时充足且未临近到期时不预警")
    void noWarnWhenSufficientAndNotExpiring() {
        assertNull(RenewWarnRules.resolveWarnReason(
                new BigDecimal("10"), NOW.plusDays(30), THRESHOLD, EXPIRE_DAYS, NOW));
    }

    @Test
    @DisplayName("课时不足优先于临近到期")
    void lessonLowTakesPriorityOverExpire() {
        // 两个条件同时满足时，应报课时不足（更需要立即跟进）
        String reason = RenewWarnRules.resolveWarnReason(
                new BigDecimal("2"), NOW.plusDays(3), THRESHOLD, EXPIRE_DAYS, NOW);
        assertEquals(StudentRenewWarnStatusEnum.REASON_LESSON_LOW, reason);
    }

    @Test
    @DisplayName("到期时间在提前天数内且未过期时预警")
    void expiringSoonWarns() {
        String reason = RenewWarnRules.resolveWarnReason(
                new BigDecimal("10"), NOW.plusDays(3), THRESHOLD, EXPIRE_DAYS, NOW);
        assertEquals(StudentRenewWarnStatusEnum.REASON_EXPIRE_SOON, reason);
    }

    @Test
    @DisplayName("已过期的账户不再报「即将到期」")
    void alreadyExpiredDoesNotWarnAsExpiringSoon() {
        // 已过期属于流失场景，不应继续标记为待续费
        assertNull(RenewWarnRules.resolveWarnReason(
                new BigDecimal("10"), NOW.minusDays(1), THRESHOLD, EXPIRE_DAYS, NOW));
    }

    @Test
    @DisplayName("到期日超出提前天数不预警")
    void beyondExpireWindowDoesNotWarn() {
        assertNull(RenewWarnRules.resolveWarnReason(
                new BigDecimal("10"), NOW.plusDays(EXPIRE_DAYS + 1), THRESHOLD, EXPIRE_DAYS, NOW));
    }

    @Test
    @DisplayName("剩余课时为空时不因课时报预警")
    void nullLessonsDoesNotTriggerLessonWarn() {
        assertNull(RenewWarnRules.resolveWarnReason(
                null, NOW.plusDays(30), THRESHOLD, EXPIRE_DAYS, NOW));
    }

    @Test
    @DisplayName("到期时间为空时不会因到期报预警")
    void nullExpireDoesNotTriggerExpireWarn() {
        assertNull(RenewWarnRules.resolveWarnReason(
                new BigDecimal("10"), null, THRESHOLD, EXPIRE_DAYS, NOW));
    }

    @Test
    @DisplayName("阈值与到期都为空时不预警，避免误报")
    void bothNullNoWarn() {
        assertNull(RenewWarnRules.resolveWarnReason(null, null, THRESHOLD, EXPIRE_DAYS, NOW));
    }

    @Test
    @DisplayName("阈值调大后原本充足的课时会触发预警")
    void raisingThresholdTriggersWarn() {
        // 不变量：阈值与触发概率正相关
        assertNull(RenewWarnRules.resolveWarnReason(
                new BigDecimal("5"), null, THRESHOLD, EXPIRE_DAYS, NOW));
        assertEquals(StudentRenewWarnStatusEnum.REASON_LESSON_LOW,
                RenewWarnRules.resolveWarnReason(
                        new BigDecimal("5"), null, new BigDecimal("6"), EXPIRE_DAYS, NOW));
    }

    // ---------- 去重判定 ----------

    @Test
    @DisplayName("从未预警过时应当生成")
    void noHistoryCreatesWarn() {
        assertTrue(RenewWarnRules.shouldCreateWarn(null, null, REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("存在待处理预警时不重复生成")
    void pendingBlocksNewWarn() {
        assertFalse(RenewWarnRules.shouldCreateWarn(
                StudentRenewWarnStatusEnum.PENDING, NOW.minusDays(100), REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("已处理预警在间隔内不重复提醒")
    void processedWithinIntervalBlocked() {
        assertFalse(RenewWarnRules.shouldCreateWarn(
                StudentRenewWarnStatusEnum.IGNORED, NOW.minusDays(3), REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("已处理预警超过间隔后允许再次生成")
    void processedBeyondIntervalAllowed() {
        assertTrue(RenewWarnRules.shouldCreateWarn(
                StudentRenewWarnStatusEnum.PROCESSED, NOW.minusDays(REPEAT_DAYS + 1), REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("已续费在间隔内也不重复提醒")
    void renewedWithinIntervalBlocked() {
        assertFalse(RenewWarnRules.shouldCreateWarn(
                StudentRenewWarnStatusEnum.RENEWED, NOW.minusDays(1), REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("有状态但无时间时按可生成处理，避免永久卡住")
    void statusWithoutTimeAllowsCreate() {
        assertTrue(RenewWarnRules.shouldCreateWarn(
                StudentRenewWarnStatusEnum.IGNORED, null, REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("间隔为0时同一天允许再次生成")
    void zeroRepeatDaysAlwaysAllowed() {
        assertTrue(RenewWarnRules.shouldCreateWarn(
                StudentRenewWarnStatusEnum.IGNORED, NOW.minusHours(1), 0, NOW));
    }

    // ---------- 间隔与过期辅助判定 ----------

    @Test
    @DisplayName("超过间隔天数判定")
    void beyondRepeatInterval() {
        assertFalse(RenewWarnRules.isBeyondRepeatInterval(NOW.minusDays(3), REPEAT_DAYS, NOW));
        assertTrue(RenewWarnRules.isBeyondRepeatInterval(NOW.minusDays(7), REPEAT_DAYS, NOW));
        assertTrue(RenewWarnRules.isBeyondRepeatInterval(NOW.minusDays(30), REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("无历史时间视为已超间隔")
    void nullTimeIsBeyondInterval() {
        assertTrue(RenewWarnRules.isBeyondRepeatInterval(null, REPEAT_DAYS, NOW));
    }

    @Test
    @DisplayName("按日期粒度判断是否过期")
    void isExpiredByDate() {
        LocalDate today = NOW.toLocalDate();
        assertTrue(RenewWarnRules.isExpired(NOW.minusDays(1), today));
        // 当天不算过期
        assertFalse(RenewWarnRules.isExpired(NOW.withHour(23).withMinute(59), today));
        assertFalse(RenewWarnRules.isExpired(null, today));
    }

    @Test
    @DisplayName("不变量：阈值不变时，课时越少越可能触发预警")
    void monotonicInRemainLessons() {
        // 从阈值往下递减，必须始终触发
        for (int i = 0; i <= 3; i++) {
            assertEquals(StudentRenewWarnStatusEnum.REASON_LESSON_LOW,
                    RenewWarnRules.resolveWarnReason(
                            BigDecimal.valueOf(i), null, THRESHOLD, EXPIRE_DAYS, NOW),
                    "剩余 " + i + " 课时应触发预警");
        }
    }
}
