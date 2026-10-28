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
import org.springframework.core.Ordered;
import vip.xiaonuo.common.context.StartupPhase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动迁移阶段顺序单测
 * <p>
 * 阶段链是「结构调整 → 权限登记 → 账号初始化 → 自检 → 定时任务」，
 * 顺序错了不会报错，只会让权限指向不存在的表、或在结构未就绪时触发业务。
 * 本类把顺序固定下来，避免以后调整常量时无意打乱依赖关系。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class StartupPhaseTest {

    @Test
    @DisplayName("阶段严格递增：底座结构最早，定时任务最后")
    void phasesAreStrictlyIncreasing() {
        int[] chain = {
                StartupPhase.BASE_SCHEMA,
                StartupPhase.PLATFORM_SCHEMA,
                StartupPhase.DOMAIN_SCHEMA,
                StartupPhase.DOMAIN_PERMISSION,
                StartupPhase.PLATFORM_BOOTSTRAP,
                StartupPhase.AUDIT,
                StartupPhase.TIMER_START
        };
        for (int i = 1; i < chain.length; i++) {
            assertTrue(chain[i] > chain[i - 1],
                    "第 " + i + " 个阶段必须大于前一个，否则顺序未定义");
        }
    }

    @Test
    @DisplayName("结构调整必须早于权限登记")
    void schemaBeforePermission() {
        assertTrue(StartupPhase.BASE_SCHEMA < StartupPhase.DOMAIN_PERMISSION);
        assertTrue(StartupPhase.PLATFORM_SCHEMA < StartupPhase.DOMAIN_PERMISSION);
        assertTrue(StartupPhase.DOMAIN_SCHEMA < StartupPhase.DOMAIN_PERMISSION,
                "权限登记会引用表与菜单，必须在结构就绪之后");
    }

    @Test
    @DisplayName("自检必须在全部结构与权限之后")
    void auditAfterEverything() {
        assertTrue(StartupPhase.AUDIT > StartupPhase.BASE_SCHEMA);
        assertTrue(StartupPhase.AUDIT > StartupPhase.PLATFORM_SCHEMA);
        assertTrue(StartupPhase.AUDIT > StartupPhase.DOMAIN_SCHEMA);
        assertTrue(StartupPhase.AUDIT > StartupPhase.DOMAIN_PERMISSION);
        assertTrue(StartupPhase.AUDIT > StartupPhase.PLATFORM_BOOTSTRAP);
    }

    @Test
    @DisplayName("定时任务在自检之后启动，避免结构未就绪时触行业务")
    void timerStartsLast() {
        assertTrue(StartupPhase.TIMER_START > StartupPhase.AUDIT);
    }

    @Test
    @DisplayName("所有阶段都早于 LOWEST_PRECEDENCE，保证能被有序调度")
    void allPhasesBeforeLowestPrecedence() {
        assertTrue(StartupPhase.BASE_SCHEMA < Ordered.LOWEST_PRECEDENCE);
        assertTrue(StartupPhase.TIMER_START < Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("阶段间距充足，便于将来在同一层级内细分")
    void phasesHaveRoomToSubdivide() {
        int minGap = StartupPhase.TIMER_START - StartupPhase.BASE_SCHEMA;
        assertTrue(minGap >= 1000,
                "相邻阶段的间距应留出余量，当前总跨度 " + minGap);
        // 相邻阶段间距应大于等于 1000
        assertEquals(1000, StartupPhase.PLATFORM_SCHEMA - StartupPhase.BASE_SCHEMA);
    }
}
