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
package vip.xiaonuo.student.modular.studentrenewarn.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 续费与流失闭环结构升级
 * <p>
 * 独立于 P0SchemaMigration，仅做幂等补丁：补预警处理/跟进字段、建阈值配配置表与回访记录表。
 * 正式多环境部署建议改用 Flyway/Liquibase。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Slf4j
@Component
public class StudentRenewWarnSchemaMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    /** 续费预警阈值配置表 */
    private static final String WARN_CONFIG_DDL = """
            CREATE TABLE IF NOT EXISTS `STUDENT_RENEW_WARN_CONFIG` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `SCOPE_TYPE` varchar(20) NOT NULL COMMENT '配置范围(DEFAULT/COURSE/CLASS)',
              `SCOPE_ID` varchar(20) NOT NULL DEFAULT '0' COMMENT '范围id(默认0)',
              `LESSON_THRESHOLD` decimal(10,2) NOT NULL DEFAULT 3.00 COMMENT '剩余课时阈值',
              `EXPIRE_DAYS` int NOT NULL DEFAULT 7 COMMENT '到期提前天数',
              `REPEAT_DAYS` int NOT NULL DEFAULT 7 COMMENT '重复提醒间隔(天)',
              `ENABLED` varchar(10) NOT NULL DEFAULT 'YES' COMMENT '是否启用',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              INDEX `IDX_WARN_CONFIG_SCOPE` (`TENANT_ID`, `SCOPE_TYPE`, `SCOPE_ID`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '续费预警阈值配置' ROW_FORMAT = Dynamic
            """;

    /** 学员回访/跟进记录表 */
    private static final String FOLLOW_RECORD_DDL = """
            CREATE TABLE IF NOT EXISTS `STUDENT_FOLLOW_RECORD` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `STUDENT_ID` varchar(20) NOT NULL COMMENT '学员id',
              `WARN_ID` varchar(20) NULL DEFAULT NULL COMMENT '续费预警id',
              `BIZ_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '业务类型(RENEW_WARN/RISK/LOSS)',
              `FOLLOW_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '跟进方式',
              `CONTENT` varchar(1000) NULL DEFAULT NULL COMMENT '跟进内容',
              `NEXT_FOLLOW_TIME` datetime NULL DEFAULT NULL COMMENT '下次跟进时间',
              `FOLLOW_USER_ID` varchar(20) NULL DEFAULT NULL COMMENT '跟进人id',
              `LOSS_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '流失类型',
              `LOSS_REASON` varchar(500) NULL DEFAULT NULL COMMENT '流失原因',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              INDEX `IDX_FOLLOW_TENANT_STUDENT` (`TENANT_ID`, `STUDENT_ID`, `CREATE_TIME`),
              INDEX `IDX_FOLLOW_TENANT_WARN` (`TENANT_ID`, `WARN_ID`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学员回访跟进记录' ROW_FORMAT = Dynamic
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            if (!tableExists("STUDENT_RENEW_WARN_CONFIG")) {
                jdbcTemplate.execute(WARN_CONFIG_DDL);
                log.info(">>> 续费闭环结构升级：已创建 STUDENT_RENEW_WARN_CONFIG");
            }
            if (!tableExists("STUDENT_FOLLOW_RECORD")) {
                jdbcTemplate.execute(FOLLOW_RECORD_DDL);
                log.info(">>> 续费闭环结构升级：已创建 STUDENT_FOLLOW_RECORD");
            }
            addColumnIfMissing("STUDENT_RENEW_WARN", "SNAPSHOT_TIME",
                    "ALTER TABLE `STUDENT_RENEW_WARN` ADD COLUMN `SNAPSHOT_TIME` datetime NULL DEFAULT NULL COMMENT '预警快照时间' AFTER `LAST_NOTIFY_TIME`");
            addColumnIfMissing("STUDENT_RENEW_WARN", "FOLLOW_USER_ID",
                    "ALTER TABLE `STUDENT_RENEW_WARN` ADD COLUMN `FOLLOW_USER_ID` varchar(20) NULL DEFAULT NULL COMMENT '跟进人id' AFTER `SNAPSHOT_TIME`");
            addColumnIfMissing("STUDENT_RENEW_WARN", "FOLLOW_TIME",
                    "ALTER TABLE `STUDENT_RENEW_WARN` ADD COLUMN `FOLLOW_TIME` datetime NULL DEFAULT NULL COMMENT '转顾问时间' AFTER `FOLLOW_USER_ID`");
            addColumnIfMissing("STUDENT_RENEW_WARN", "PROCESS_REMARK",
                    "ALTER TABLE `STUDENT_RENEW_WARN` ADD COLUMN `PROCESS_REMARK` varchar(500) NULL DEFAULT NULL COMMENT '处理说明' AFTER `FOLLOW_TIME`");
            addColumnIfMissing("STUDENT_RENEW_WARN", "PROCESS_TIME",
                    "ALTER TABLE `STUDENT_RENEW_WARN` ADD COLUMN `PROCESS_TIME` datetime NULL DEFAULT NULL COMMENT '处理时间' AFTER `PROCESS_REMARK`");
            if (!indexExists("STUDENT_RENEW_WARN", "IDX_WARN_TENANT_STATUS_CREATE")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_RENEW_WARN` "
                        + "ADD INDEX `IDX_WARN_TENANT_STATUS_CREATE` (`TENANT_ID`, `STATUS`, `CREATE_TIME`)");
                log.info(">>> 续费闭环结构升级：已补续费预警状态索引");
            }
        } catch (Exception e) {
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("续费闭环结构升级",
                    "请手工执行 docs/_streams/renew.md 中的迁移 SQL：" + e.getMessage());
            log.error(">>> 续费闭环结构升级失败，请手工执行 docs/_streams/renew.md 中的迁移 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        // 晚于 P0SchemaMigration（LOWEST_PRECEDENCE - 20），保证基础表已存在
        return Ordered.LOWEST_PRECEDENCE - 15;
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
            log.info(">>> 续费闭环结构升级：{}.{} 已补列", tableName, columnName);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }
}
