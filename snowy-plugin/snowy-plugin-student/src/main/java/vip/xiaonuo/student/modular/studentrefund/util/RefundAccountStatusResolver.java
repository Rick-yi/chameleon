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
package vip.xiaonuo.student.modular.studentrefund.util;

import java.math.BigDecimal;

/**
 * 退费后的课时账户状态判定
 * <p>
 * 退费确认完成后需要把冻结的账户解冻：还有剩余课时回到正常，用完则清零。
 * 这个判定藏在 confirm() 的流程里，改错了会让账户永久停留在冻结态（学员无法再上课）
 * 或错误清零（剩余课时丢失），都是账务级故障，所以抽出来单测。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class RefundAccountStatusResolver {

    /** 账户正常：还有可用课时 */
    public static final String NORMAL = "NORMAL";

    /** 账户已清零：课时用完 */
    public static final String CLEARED = "CLEARED";

    private RefundAccountStatusResolver() {
    }

    /**
     * 根据退费后的剩余课时判定账户状态
     *
     * @param remainLessonsAfterRefund 退费后的剩余课时，null 视为 0
     * @return NORMAL 或 CLEARED
     */
    public static String resolve(BigDecimal remainLessonsAfterRefund) {
        BigDecimal remain = remainLessonsAfterRefund == null ? BigDecimal.ZERO : remainLessonsAfterRefund;
        // 用 compareTo 而非 equals：BigDecimal("0.00").equals(BigDecimal.ZERO) 为 false
        return remain.compareTo(BigDecimal.ZERO) > 0 ? NORMAL : CLEARED;
    }

    /**
     * 是否可以释放冻结（退费确认前的前置校验）
     * <p>
     * 待扣减的课时必须为正，且账户剩余与赠课余额都必须足够，
     * 否则扣减会变成负数，账务直接错乱。
     * </p>
     *
     * @param totalRemove      本次要扣减的总课时（购买课时 + 赠课）
     * @param refundGiftLessons 本次作废的赠课
     * @param remainLessons    账户当前剩余课时
     * @param giftLessons      账户当前赠课余额
     * @return true 表示可以安全扣减
     */
    public static boolean canDeduct(BigDecimal totalRemove, BigDecimal refundGiftLessons,
                                    BigDecimal remainLessons, BigDecimal giftLessons) {
        BigDecimal remove = nvl(totalRemove);
        BigDecimal giftRemove = nvl(refundGiftLessons);
        if (remove.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (giftRemove.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        return nvl(remainLessons).compareTo(remove) >= 0
                && nvl(giftLessons).compareTo(giftRemove) >= 0;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
