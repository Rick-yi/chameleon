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

    /**
     * 校验并返回合法的流转目标
     * <p>
     * 规则集中在此处，避免服务层与前端各自判断导致口径不一致：
     * 目标状态必须存在、不能是历史状态 REFUNDED、不能与当前状态相同。
     * </p>
     *
     * @param targetStatus 目标状态值
     * @param fromStatus   当前状态值
     * @return 目标状态枚举
     * @throws IllegalArgumentException 目标非法时抛出
     **/
    public static StudentStatusMachineEnum resolveTarget(String targetStatus, String fromStatus) {
        StudentStatusMachineEnum target = getByValue(targetStatus);
        if (target == null || !target.canBeTarget()) {
            throw new IllegalArgumentException("不支持的目标状态：" + targetStatus);
        }
        if (target.getValue().equals(fromStatus)) {
            throw new IllegalArgumentException("学员当前已是【" + target.getMessage() + "】，无需变更");
        }
        return target;
    }

    /**
     * 是否需要填写变更原因
     * <p>
     * 复课（流转回在读）可省略原因并默认记为「复课」；其余流转必须填原因。
     * </p>
     *
     * @param target 目标状态
     * @return true 表示必须填写原因
     **/
    public static boolean reasonRequired(StudentStatusMachineEnum target) {
        return target != null && target != STUDYING;
    }

    /**
     * 规范化变更原因：复课且未填原因时返回默认值「复课」
     *
     * @param target 目标状态
     * @param reason 用户填写的原因（可能为空）
     * @return 规范化后的原因
     **/
    public static String normalizeReason(StudentStatusMachineEnum target, String reason) {
        String trimmed = reason == null ? null : reason.trim();
        if (trimmed != null && !trimmed.isEmpty()) {
            return trimmed;
        }
        return target == STUDYING ? "复课" : null;
    }
}
