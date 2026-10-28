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
import vip.xiaonuo.student.core.enums.StudentStatusMachineEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学员状态机规单测
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class StudentStatusMachineTest {

    @Test
    @DisplayName("在读可流转到停课/休学/退学/结课")
    void studyingCanMoveToAllActiveTargets() {
        for (StudentStatusMachineEnum target : new StudentStatusMachineEnum[]{
                StudentStatusMachineEnum.STOP, StudentStatusMachineEnum.SUSPEND,
                StudentStatusMachineEnum.WITHDRAWN, StudentStatusMachineEnum.GRADUATED}) {
            assertEquals(target, StudentStatusMachineEnum.resolveTarget(target.getValue(), "STUDYING"));
        }
    }

    @Test
    @DisplayName("停课/休学可以复课回到在读")
    void stoppedAndSuspendedCanResume() {
        assertEquals(StudentStatusMachineEnum.STUDYING,
                StudentStatusMachineEnum.resolveTarget("STUDYING", "STOP"));
        assertEquals(StudentStatusMachineEnum.STUDYING,
                StudentStatusMachineEnum.resolveTarget("STUDYING", "SUSPEND"));
    }

    @Test
    @DisplayName("已退费是历史状态，不能作为流转目标")
    void refundedIsNotATarget() {
        assertFalse(StudentStatusMachineEnum.REFUNDED.canBeTarget());
        assertThrows(IllegalArgumentException.class,
                () -> StudentStatusMachineEnum.resolveTarget("REFUNDED", "STUDYING"));
    }

    @Test
    @DisplayName("未知状态值被拒绝")
    void unknownStatusRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> StudentStatusMachineEnum.resolveTarget("NOT_EXIST", "STUDYING"));
        assertThrows(IllegalArgumentException.class,
                () -> StudentStatusMachineEnum.resolveTarget(null, "STUDYING"));
    }

    @Test
    @DisplayName("流转到相同状态被拒绝")
    void sameStatusRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> StudentStatusMachineEnum.resolveTarget("STUDYING", "STUDYING"));
        assertThrows(IllegalArgumentException.class,
                () -> StudentStatusMachineEnum.resolveTarget("STOP", "STOP"));
    }

    @Test
    @DisplayName("复课不强制填写原因，其余流转必须填写")
    void reasonRequiredOnlyForNonResume() {
        assertFalse(StudentStatusMachineEnum.reasonRequired(StudentStatusMachineEnum.STUDYING));
        assertTrue(StudentStatusMachineEnum.reasonRequired(StudentStatusMachineEnum.STOP));
        assertTrue(StudentStatusMachineEnum.reasonRequired(StudentStatusMachineEnum.SUSPEND));
        assertTrue(StudentStatusMachineEnum.reasonRequired(StudentStatusMachineEnum.WITHDRAWN));
        assertTrue(StudentStatusMachineEnum.reasonRequired(StudentStatusMachineEnum.GRADUATED));
    }

    @Test
    @DisplayName("复课未填原因时规范化为「复课」")
    void resumeWithoutReasonDefaultsText() {
        assertEquals("复课", StudentStatusMachineEnum.normalizeReason(StudentStatusMachineEnum.STUDYING, null));
        assertEquals("复课", StudentStatusMachineEnum.normalizeReason(StudentStatusMachineEnum.STUDYING, "   "));
    }

    @Test
    @DisplayName("非复课未填原因保持为空，由调用方决定是否报错")
    void nonResumeWithoutReasonStaysNull() {
        assertNull(StudentStatusMachineEnum.normalizeReason(StudentStatusMachineEnum.STOP, null));
        assertNull(StudentStatusMachineEnum.normalizeReason(StudentStatusMachineEnum.WITHDRAWN, "  "));
    }

    @Test
    @DisplayName("填写的原因去除首尾空格后保留")
    void reasonTrimmedAndKept() {
        assertEquals("家长要求停课",
                StudentStatusMachineEnum.normalizeReason(StudentStatusMachineEnum.STOP, "  家长要求停课  "));
        assertEquals("搬家",
                StudentStatusMachineEnum.normalizeReason(StudentStatusMachineEnum.STUDYING, "搬家"));
    }

    @Test
    @DisplayName("状态值转中文名，未知值原样返回")
    void messageOf() {
        assertEquals("在读", StudentStatusMachineEnum.messageOf("STUDYING"));
        assertEquals("停课", StudentStatusMachineEnum.messageOf("STOP"));
        assertEquals("UNKNOWN", StudentStatusMachineEnum.messageOf("UNKNOWN"));
    }
}
