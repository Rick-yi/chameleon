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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.common.context.StartupMigrationRegistry;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动期迁移失败登记单测
 * <p>
 * 迁移失败原先只打日志、应用照常启动，故障会推迟到业务调用时才暴露。
 * 本类保证失败能被集中登记，避免「静默降级」。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
class StartupMigrationRegistryTest {

    @AfterEach
    void tearDown() {
        // 静态状态，每个用例后清空，避免互相影响
        StartupMigrationRegistry.clear();
    }

    @Test
    @DisplayName("初始状态无失败")
    void initiallyNoFailure() {
        assertFalse(StartupMigrationRegistry.hasFailure());
        assertTrue(StartupMigrationRegistry.failures().isEmpty());
        assertTrue(StartupMigrationRegistry.failedNames().isEmpty());
    }

    @Test
    @DisplayName("登记失败后可查询到名称与原因")
    void recordFailure() {
        StartupMigrationRegistry.record("P0 结构升级", "请手工执行 chameleon_p0_migration.sql：表不存在");
        assertTrue(StartupMigrationRegistry.hasFailure());

        Map<String, String> failures = StartupMigrationRegistry.failures();
        assertEquals(1, failures.size());
        assertTrue(failures.containsKey("P0 结构升级"));
        assertTrue(failures.get("P0 结构升级").contains("chameleon_p0_migration.sql"));
    }

    @Test
    @DisplayName("多个迁移失败全部登记且保持登记顺序")
    void multipleFailuresKeepOrder() {
        StartupMigrationRegistry.record("财务权限升级", "原因A");
        StartupMigrationRegistry.record("学员档案结构升级", "原因B");

        assertEquals(java.util.List.of("财务权限升级", "学员档案结构升级"),
                StartupMigrationRegistry.failedNames());
    }

    @Test
    @DisplayName("同名迁移重复失败只保留一条，后登记的原因覆盖前者")
    void sameMigrationOverwrites() {
        StartupMigrationRegistry.record("财务权限升级", "第一次原因");
        StartupMigrationRegistry.record("财务权限升级", "第二次原因");

        assertEquals(1, StartupMigrationRegistry.failures().size());
        assertEquals("第二次原因", StartupMigrationRegistry.failures().get("财务权限升级"));
    }

    @Test
    @DisplayName("原因为空时记为未知原因，避免出现空描述")
    void nullReasonRecordedAsUnknown() {
        StartupMigrationRegistry.record("某迁移", null);
        assertEquals("未知原因", StartupMigrationRegistry.failures().get("某迁移"));
    }

    @Test
    @DisplayName("返回的失败副本不影响内部状态")
    void returnedMapIsCopy() {
        StartupMigrationRegistry.record("某迁移", "原因");
        Map<String, String> copy = StartupMigrationRegistry.failures();
        copy.clear();
        assertTrue(StartupMigrationRegistry.hasFailure(), "外部清空副本不应影响登记器");
    }

    @Test
    @DisplayName("清空后回到无失败状态")
    void clearResets() {
        StartupMigrationRegistry.record("某迁移", "原因");
        StartupMigrationRegistry.clear();
        assertFalse(StartupMigrationRegistry.hasFailure());
        assertTrue(StartupMigrationRegistry.failures().isEmpty());
    }
}
