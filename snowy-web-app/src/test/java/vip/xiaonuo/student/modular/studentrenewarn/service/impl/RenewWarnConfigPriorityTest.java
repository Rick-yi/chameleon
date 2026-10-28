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
package vip.xiaonuo.student.modular.studentrenewarn.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarnConfig;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 续费预警配置优先级单测
 * <p>
 * 规则：班级 > 课程 > 默认。这是续费预警的核心口径，
 * 顺序或启用判断被改错时不会报错，只会让整机构预警阈值静默失效。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class RenewWarnConfigPriorityTest {

    private StudentRenewWarnConfig config(String scopeType, String scopeId, String enabled, String threshold) {
        StudentRenewWarnConfig config = new StudentRenewWarnConfig();
        config.setScopeType(scopeType);
        config.setScopeId(scopeId);
        config.setEnabled(enabled);
        config.setLessonThreshold(new BigDecimal(threshold));
        return config;
    }

    @Test
    @DisplayName("班级配置优先于课程与默认")
    void classWinsOverCourseAndDefault() {
        StudentRenewWarnConfig classCfg = config("CLASS", "c1", "YES", "5");
        StudentRenewWarnConfig courseCfg = config("COURSE", "course1", "YES", "3");
        StudentRenewWarnConfig defaultCfg = config("DEFAULT", "0", "YES", "1");

        Map<String, StudentRenewWarnConfig> classMap = new HashMap<>();
        classMap.put("stu1|course1", classCfg);

        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(courseCfg, defaultCfg), classMap);
        assertSame(classCfg, picked);
    }

    @Test
    @DisplayName("无班级配置时课程配置优先于默认")
    void courseWinsOverDefault() {
        StudentRenewWarnConfig courseCfg = config("COURSE", "course1", "YES", "3");
        StudentRenewWarnConfig defaultCfg = config("DEFAULT", "0", "YES", "1");

        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(defaultCfg, courseCfg), new HashMap<>());
        assertSame(courseCfg, picked);
    }

    @Test
    @DisplayName("仅默认配置时回落到默认")
    void fallsBackToDefault() {
        StudentRenewWarnConfig defaultCfg = config("DEFAULT", "0", "YES", "1");
        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(defaultCfg), new HashMap<>());
        assertSame(defaultCfg, picked);
    }

    @Test
    @DisplayName("课程配置只对匹配的课程生效")
    void courseConfigOnlyMatchesItsCourse() {
        StudentRenewWarnConfig otherCourse = config("COURSE", "course2", "YES", "3");
        StudentRenewWarnConfig defaultCfg = config("DEFAULT", "0", "YES", "1");

        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(otherCourse, defaultCfg), new HashMap<>());
        assertSame(defaultCfg, picked, "课程不匹配时应回落到默认配置");
    }

    @Test
    @DisplayName("停用的课程配置被跳过，回落到默认")
    void disabledCourseConfigSkipped() {
        StudentRenewWarnConfig disabledCourse = config("COURSE", "course1", "NO", "3");
        StudentRenewWarnConfig defaultCfg = config("DEFAULT", "0", "YES", "1");

        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(disabledCourse, defaultCfg), new HashMap<>());
        assertSame(defaultCfg, picked);
    }

    @Test
    @DisplayName("停用的班级配置被跳过，继续找课程配置")
    void disabledClassConfigSkipped() {
        // 班级映射里的配置若被停用，不应生效
        StudentRenewWarnConfig disabledClass = config("CLASS", "c1", "NO", "5");
        StudentRenewWarnConfig courseCfg = config("COURSE", "course1", "YES", "3");

        Map<String, StudentRenewWarnConfig> classMap = new HashMap<>();
        classMap.put("stu1|course1", disabledClass);

        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(courseCfg), classMap);
        assertSame(courseCfg, picked, "班级配置被停用后应继续走课程级");
    }

    @Test
    @DisplayName("全部停用时返回 null，由调用方回落到系统参数")
    void allDisabledReturnsNull() {
        StudentRenewWarnConfig disabledCourse = config("COURSE", "course1", "NO", "3");
        StudentRenewWarnConfig disabledDefault = config("DEFAULT", "0", "NO", "1");

        assertNull(StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(disabledCourse, disabledDefault), new HashMap<>()));
    }

    @Test
    @DisplayName("配置列表为空时返回 null")
    void emptyConfigListReturnsNull() {
        assertNull(StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(), new HashMap<>()));
        assertNull(StudentRenewWarnConfigServiceImpl.pickByPriority("stu1", "course1", null, null));
    }

    @Test
    @DisplayName("班级配置只对映射中的学员+课程生效")
    void classConfigOnlyForMappedStudent() {
        StudentRenewWarnConfig classCfg = config("CLASS", "c1", "YES", "5");
        Map<String, StudentRenewWarnConfig> classMap = new HashMap<>();
        classMap.put("stu1|course1", classCfg);

        // 同班级不同学员，不在映射里
        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu2", "course1", List.of(), classMap);
        assertNull(picked, "未映射的学员不应命中该班级配置");
    }

    @Test
    @DisplayName("enabled 为空视为启用")
    void blankEnabledMeansEnabled() {
        StudentRenewWarnConfig noFlag = config("DEFAULT", "0", null, "1");
        StudentRenewWarnConfig picked = StudentRenewWarnConfigServiceImpl.pickByPriority(
                "stu1", "course1", List.of(noFlag), new HashMap<>());
        assertSame(noFlag, picked);
        assertEquals(new BigDecimal("1"), picked.getLessonThreshold());
    }
}
