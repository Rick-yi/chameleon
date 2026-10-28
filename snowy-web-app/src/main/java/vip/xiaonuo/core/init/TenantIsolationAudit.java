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
package vip.xiaonuo.core.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.context.StartupMigrationRegistry;
import vip.xiaonuo.core.config.GlobalConfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 租户隔离自检
 * <p>
 * 多租户行级隔离依赖 {@link GlobalConfigure#tenantTableNames()} 白名单：
 * 登记过的表由 MyBatis-Plus 拦截器自动拼接 TENANT_ID 条件；未登记的表完全依赖服务层手写过滤，
 * 一旦某一处漏写就是跨机构数据串号，既不会报错也不会有人察觉。
 * </p>
 * <p>
 * 本类在启动时比对数据库中「真正有 TENANT_ID 列」的表与白名单，
 * 对未登记的表输出告警清单。这是提醒而非拦截：存量表改造需要时间，直接阻断启动会阻塞发布。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@Slf4j
@Component
public class TenantIsolationAudit implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    /**
     * 平台级表：归属于平台运营/底座调度侧，不按租户隔离，无需登记
     * <p>
     * DEV_JOB 是定时任务定义表，TENANT_ID 用于标记任务归属（平台任务为 0），
     * 但调度器在无租户上下文时按「运行中」状态扫描全部租户的任务，
     * 若纳入拦截器会被拼上 TENANT_ID='0' 而漏掉各租户任务。
     * </p>
     */
    private static final Set<String> PLATFORM_TABLES = Set.of(
            "T_TENANT", "T_TENANT_LOGIN", "T_TENANT_PACKAGE", "T_TENANT_RENEW",
            "T_PLATFORM_BILL", "T_PLATFORM_METRIC", "DEV_JOB");

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            audit();
        } catch (Exception e) {
            // 自检失败不影响启动
            log.warn(">>> 租户隔离自检未执行：{}", e.getMessage());
        }
        reportMigrationFailures();
    }

    /**
     * 汇总启动期迁移失败
     * <p>
     * 各迁移监听器失败时只登记不抛出，应用会照常启动。这里集中输出，
     * 避免「行为静默降级、日志散落各处」导致运维漏看。
     * </p>
     */
    private void reportMigrationFailures() {
        if (!StartupMigrationRegistry.hasFailure()) {
            log.info(">>> 启动期结构/权限迁移全部成功");
            return;
        }
        Map<String, String> failures = StartupMigrationRegistry.failures();
        StringBuilder detail = new StringBuilder();
        failures.forEach((name, reason) -> detail.append("\n>>>   - ").append(name).append("：").append(reason));
        log.error("""
                >>> 启动期结构/权限迁移有 {} 项失败，应用已启动但这些变更未生效，相关功能可能异常。{}
                >>> 请按上方提示手工补跑对应 SQL 后重启。""",
                failures.size(), detail);
    }

    /**
     * 比对数据库实际表结构与租户白名单
     */
    private void audit() {
        // 查出所有含 TENANT_ID 列的表
        List<String> tablesWithTenantColumn = jdbcTemplate.queryForList("""
                SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'TENANT_ID'
                """, String.class);
        if (tablesWithTenantColumn.isEmpty()) {
            return;
        }
        Set<String> registered = GlobalConfigure.tenantTableNames();
        Set<String> unregistered = new TreeSet<>();
        for (String tableName : tablesWithTenantColumn) {
            String upperName = tableName == null ? "" : tableName.toUpperCase();
            if (upperName.isEmpty() || PLATFORM_TABLES.contains(upperName)) {
                continue;
            }
            if (!registered.contains(upperName)) {
                unregistered.add(upperName);
            }
        }
        if (unregistered.isEmpty()) {
            log.info(">>> 租户隔离自检通过：含 TENANT_ID 的 {} 张业务表均已登记", tablesWithTenantColumn.size());
            return;
        }
        // 未登记的表由服务层手写租户条件，需逐一确认每个查询都带了 TENANT_ID
        List<String> tableList = new ArrayList<>(unregistered);
        log.warn("""
                >>> 租户隔离自检：{} 张表含 TENANT_ID 但未登记到 TENANT_TABLES，隔离完全依赖服务层手写条件。
                >>> 请确认这些表的全部读写都显式过滤了租户，或将其登记进 GlobalConfigure.TENANT_TABLES：
                >>> {}""", tableList.size(), String.join("、", tableList));
    }

    @Override
    public int getOrder() {
        // 在结构迁移之后执行，保证新表已建好再自检
        return Ordered.LOWEST_PRECEDENCE;
    }
}
