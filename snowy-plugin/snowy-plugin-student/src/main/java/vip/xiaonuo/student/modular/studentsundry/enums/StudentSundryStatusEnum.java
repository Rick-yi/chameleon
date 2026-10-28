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
package vip.xiaonuo.student.modular.studentsundry.enums;

/**
 * 学杂费单据状态
 * <p>
 * 与课包订单状态刻意区分：学杂费没有「消课」概念，付款即收入确认；
 * 退费走独立的退款中/已退费，不复用课包订单的 REFUNDED（那里还牵着课时回补）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class StudentSundryStatusEnum {

    /** 待收款 */
    public static final String UNPAID = "UNPAID";

    /** 待收款（已提交收款，等待确认） */
    public static final String PENDING = "PENDING";

    /** 已收款 */
    public static final String PAID = "PAID";

    /** 退款中 */
    public static final String REFUNDING = "REFUNDING";

    /** 已退款 */
    public static final String REFUNDED = "REFUNDED";

    /** 已取消 */
    public static final String CANCELED = "CANCELED";

    private StudentSundryStatusEnum() {
    }

    /**
     * 状态值转中文名
     *
     * @param value 状态值
     * @return 中文名，未知值原样返回
     */
    public static String messageOf(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case UNPAID -> "待收款";
            case PENDING -> "待确认";
            case PAID -> "已收款";
            case REFUNDING -> "退款中";
            case REFUNDED -> "已退款";
            case CANCELED -> "已取消";
            default -> value;
        };
    }

    /**
     * 是否为已收款状态（计入学杂费收入）
     *
     * @param status 状态值
     * @return true 表示已收款
     */
    public static boolean isPaid(String status) {
        return PAID.equals(status);
    }

    /**
     * 是否为可退费状态
     *
     * @param status 状态值
     * @return true 表示可以发起退费
     */
    public static boolean canRefund(String status) {
        return PAID.equals(status);
    }

    /**
     * 是否为可取消状态（未收款才允许取消）
     *
     * @param status 状态值
     * @return true 表示可以取消
     */
    public static boolean canCancel(String status) {
        return UNPAID.equals(status) || PENDING.equals(status);
    }
}
