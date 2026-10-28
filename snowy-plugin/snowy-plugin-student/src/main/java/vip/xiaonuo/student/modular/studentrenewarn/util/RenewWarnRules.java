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
package vip.xiaonuo.student.modular.studentrenewarn.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;

import java.util.Objects;

/**
 * 续费预警判定规则
 * <p>
 * 把 {@code scan()} 里的三段判定逻辑抽成纯函数，便于单测覆盖。
 * 这些规则改错了不会抛异常，只会让整机构的续费预警静默失效或误报，
 * 所以必须有不变量测试守着。
 * </p>
 * <p>
 * 规则：
 * <ul>
 *   <li>剩余课时低于阈值 → 课时不足预警（优先判断）</li>
 *   <li>否则若到期时间在「今天 + 提前天数」之内且尚未过期 → 即将到期预警</li>
 *   <li>已过期的账户不再产生「即将到期」预警（应走流失流程）</li>
 * </ul>
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class RenewWarnRules {

    private RenewWarnRules() {
    }

    /**
     * 判定预警原因
     *
     * @param remainLessons   剩余课时，可为 null（视为无课时数据）
     * @param expireTime      到期时间，可为 null（视为无到期约束）
     * @param lessonThreshold 剩余课时阈值
     * @param expireDays      到期提前提醒天数
     * @param now             判定基准时间
     * @return 预警原因码，不需要预警时返回 null
     */
    public static String resolveWarnReason(java.math.BigDecimal remainLessons, LocalDateTime expireTime,
                                           java.math.BigDecimal lessonThreshold, int expireDays,
                                           LocalDateTime now) {
        // 1) 课时不足优先：这是最需要立即跟进的信号
        if (remainLessons != null && lessonThreshold != null
                && remainLessons.compareTo(lessonThreshold) <= 0) {
            return StudentRenewWarnStatusEnum.REASON_LESSON_LOW;
        }
        // 2) 即将到期：必须在「现在 ~ 今天+提前天数」这个区间内
        //    已过期的账户不在此列，避免把流失学员继续标记为待续费
        if (expireTime != null) {
            LocalDateTime deadline = now.plusDays(expireDays);
            if (expireTime.isAfter(now) && expireTime.isBefore(deadline)) {
                return StudentRenewWarnStatusEnum.REASON_EXPIRE_SOON;
            }
        }
        return null;
    }

    /**
     * 是否需要生成新预警（重复间隔去重）
     * <p>
     * 规则：待处理预警不重复生成；已处理/已忽略/已续费在间隔天数内不重复提醒。
     * </p>
     *
     * @param latestStatus      最近一条预警状态，没有历史预警时传 null
     * @param latestCreateTime  最近一条预警创建时间，没有历史预警时传 null
     * @param repeatDays        重复提醒间隔天数
     * @param now               判定基准时间
     * @return true 表示应当生成新预警
     */
    public static boolean shouldCreateWarn(String latestStatus, LocalDateTime latestCreateTime,
                                           int repeatDays, LocalDateTime now) {
        if (latestStatus == null) {
            // 从未预警过
            return true;
        }
        // 仍有待处理的预警，不重复生成
        if (Objects.equals(latestStatus, StudentRenewWarnStatusEnum.PENDING)) {
            return false;
        }
        if (latestCreateTime == null) {
            return true;
        }
        // 已处理过的预警在间隔内不重复提醒
        LocalDateTime repeatThreshold = now.minusDays(repeatDays);
        return !latestCreateTime.isAfter(repeatThreshold);
    }

    /**
     * 计算预警是否已过重复间隔（按天粒度）
     *
     * @param latestCreateTime 上次预警时间
     * @param repeatDays       间隔天数
     * @param now              当前时间
     * @return true 表示已超过间隔
     */
    public static boolean isBeyondRepeatInterval(LocalDateTime latestCreateTime, int repeatDays,
                                                 LocalDateTime now) {
        if (latestCreateTime == null) {
            return true;
        }
        long elapsedDays = ChronoUnit.DAYS.between(latestCreateTime.toLocalDate(), now.toLocalDate());
        return elapsedDays >= repeatDays;
    }

    /**
     * 到期日是否已过（按日期粒度）
     *
     * @param expireTime 到期时间
     * @param today      今天
     * @return true 表示已过期
     */
    public static boolean isExpired(LocalDateTime expireTime, LocalDate today) {
        return expireTime != null && expireTime.toLocalDate().isBefore(today);
    }
}
