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
package vip.xiaonuo.student.modular.studentlessonaccount.util;

import java.math.BigDecimal;

/**
 * 课时账户账务不变量
 * <p>
 * 账户表维护三个互相约束的字段：总课时、已用课时、剩余课时。
 * 不变量为 {@code 剩余 = 总课时 - 已用}。
 * </p>
 * <p>
 * 这个不变量由多条 SQL 分别维护：点名扣课累加已用、回补递减已用、
 * 报名增加总量、退费扣减总量。任何一处漏改或写错，都不会立即报错，
 * 只会在某次报表或退费试算时算错金额。因此提供独立校验入口，
 * 便于运营在异常时段自查。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class LessonAccountInvariant {

    private LessonAccountInvariant() {
    }

    /**
     * 校验账户三项课时是否自洽
     *
     * @param totalLessons  总课时（含赠课）
     * @param usedLessons   已用课时
     * @param remainLessons 剩余课时
     * @return 校验结果；自洽时 {@link Result#consistent()} 为 true
     */
    public static Result check(BigDecimal totalLessons, BigDecimal usedLessons, BigDecimal remainLessons) {
        BigDecimal total = nvl(totalLessons);
        BigDecimal used = nvl(usedLessons);
        BigDecimal remain = nvl(remainLessons);
        BigDecimal expected = total.subtract(used);
        // 用 compareTo 而非 equals：BigDecimal 的精度差异会让 equals 误判
        boolean ok = expected.compareTo(remain) == 0;
        return new Result(ok, expected, remain, remain.subtract(expected));
    }

    /**
     * 剩余课时是否被异常写成负数
     * <p>
     * 正常流程有多重条件更新保护，不应出现负数；出现即说明有路径绕过了保护。
     * </p>
     *
     * @param remainLessons 剩余课时
     * @return true 表示异常（负数）
     */
    public static boolean isNegativeRemain(BigDecimal remainLessons) {
        return nvl(remainLessons).compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 已用课时是否超过总课时
     * <p>
     * 超用说明扣课未做余额校验，或回补计算错误。
     * </p>
     *
     * @param totalLessons 总课时
     * @param usedLessons  已用课时
     * @return true 表示异常（超用）
     */
    public static boolean isOverused(BigDecimal totalLessons, BigDecimal usedLessons) {
        return nvl(usedLessons).compareTo(nvl(totalLessons)) > 0;
    }

    /**
     * 校验结果
     *
     * @param consistent 是否自洽
     * @param expected   按「总课时 - 已用」推算出的剩余课时
     * @param actual     账户上实际记录的剩余课时
     * @param drift      偏差 = 实际 - 推算
     */
    public record Result(boolean consistent, BigDecimal expected, BigDecimal actual, BigDecimal drift) {

        /** 偏差是否为正（实际剩余多于应得，通常是少扣了课） */
        public boolean overCredited() {
            return drift.compareTo(BigDecimal.ZERO) > 0;
        }

        /** 偏差是否为负（实际剩余少于应得，通常是多扣了课） */
        public boolean underCredited() {
            return drift.compareTo(BigDecimal.ZERO) < 0;
        }
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
