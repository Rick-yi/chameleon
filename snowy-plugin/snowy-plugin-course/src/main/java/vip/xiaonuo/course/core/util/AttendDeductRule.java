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
package vip.xiaonuo.course.core.util;

import java.math.BigDecimal;

/**
 * 点名扣课规则
 * <p>
 * 规则优先级：课包配置 &gt; 课程配置 &gt; 默认值。
 * 到课必扣；缺勤、请假是否扣课可配置（缺勤默认扣、请假默认不扣）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public class AttendDeductRule {

    /** 到课 */
    public static final String ATTEND = "ATTEND";

    /** 请假 */
    public static final String LEAVE = "LEAVE";

    /** 缺勤 */
    public static final String ABSENT = "ABSENT";

    /**
     * 计算本次点名应扣的课时数
     *
     * @param lessonsPerClass 每课扣课时数（为空按 1 计）
     * @param leaveDeduct      请假是否扣课（为空按不扣）
     * @param absentDeduct     缺勤是否扣课（为空按扣）
     * @param attendStatus     点名状态
     * @return 应扣课时，不扣时返回 0
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public static BigDecimal deductLessons(BigDecimal lessonsPerClass, Boolean leaveDeduct, Boolean absentDeduct,
                                           String attendStatus) {
        boolean leave = Boolean.TRUE.equals(leaveDeduct);
        boolean absent = absentDeduct == null || absentDeduct;
        boolean shouldDeduct = ATTEND.equals(attendStatus)
                || (ABSENT.equals(attendStatus) && absent)
                || (LEAVE.equals(attendStatus) && leave);
        if (!shouldDeduct) {
            return BigDecimal.ZERO;
        }
        return lessonsPerClass == null ? BigDecimal.ONE : lessonsPerClass;
    }
}
