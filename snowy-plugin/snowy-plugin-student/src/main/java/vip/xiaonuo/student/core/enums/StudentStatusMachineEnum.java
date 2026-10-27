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
package vip.xiaonuo.student.core.enums;

import lombok.Getter;

/**
 * 学员状态机
 * <p>
 * 在读 → 停课/休学/退学/结课；停课/休学 → 在读（复课）。
 * 历史数据中的 REFUNDED（已退费）保留展示，不再作为新流转目标。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Getter
public enum StudentStatusMachineEnum {

    /** 在读 */
    STUDYING("STUDYING", "在读"),

    /** 停课 */
    STOP("STOP", "停课"),

    /** 休学 */
    SUSPEND("SUSPEND", "休学"),

    /** 退学 */
    WITHDRAWN("WITHDRAWN", "退学"),

    /** 结课 */
    GRADUATED("GRADUATED", "结课"),

    /** 已退费（历史状态） */
    REFUNDED("REFUNDED", "已退费");

    private final String value;

    private final String message;

    StudentStatusMachineEnum(String value, String message) {
        this.value = value;
        this.message = message;
    }

    /**
     * 按状态值查找
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    public static StudentStatusMachineEnum getByValue(String value) {
        for (StudentStatusMachineEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 状态值转中文名
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    public static String messageOf(String value) {
        StudentStatusMachineEnum item = getByValue(value);
        return item == null ? value : item.getMessage();
    }

    /**
     * 是否允许作为流转目标
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    public boolean canBeTarget() {
        return this != REFUNDED;
    }
}
