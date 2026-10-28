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
package vip.xiaonuo.student.core.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 学员档案增强结构升级
 * <p>
 * 独立于 P0SchemaMigration：只做缺表建表、缺列补列，可重复执行。
 * 包含：学员标签体系、状态变更记录、学员状态原因/时间字段。
 * 说明：STUDENT_TAG / STUDENT_TAG_REL / STUDENT_STATUS_LOG 未登记到
 * GlobalConfigure.TENANT_TABLES（本次改动不修改该文件），服务层显式使用 TENANT_ID。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class StudentArchiveSchemaMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private static final String STUDENT_TAG_DDL = """
            CREATE TABLE IF NOT EXISTS `STUDENT_TAG` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NOT NULL DEFAULT '0' COMMENT '租户id',
              `NAME` varchar(50) NOT NULL COMMENT '标签名称',
              `COLOR` varchar(20) NULL DEFAULT 'blue' COMMENT '标签颜色',
              `SORT_CODE` int NULL DEFAULT 0 COMMENT '排序号',
              `REMARK` varchar(500) NULL DEFAULT NULL COMMENT '备注',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_STUDENT_TAG_TENANT` (`TENANT_ID`, `SORT_CODE`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
              COMMENT = '学员标签字典' ROW_FORMAT = Dynamic
            """;

    private static final String STUDENT_TAG_REL_DDL = """
            CREATE TABLE IF NOT EXISTS `STUDENT_TAG_REL` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NOT NULL DEFAULT '0' COMMENT '租户id',
              `STUDENT_ID` varchar(20) NOT NULL COMMENT '学员id',
              `TAG_ID` varchar(20) NOT NULL COMMENT '标签id',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              UNIQUE INDEX `UK_STUDENT_TAG_REL` (`TENANT_ID`, `STUDENT_ID`, `TAG_ID`),
              INDEX `IDX_STUDENT_TAG_REL_TAG` (`TENANT_ID`, `TAG_ID`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
              COMMENT = '学员-标签关联' ROW_FORMAT = Dynamic
            """;

    private static final String STUDENT_STATUS_LOG_DDL = """
            CREATE TABLE IF NOT EXISTS `STUDENT_STATUS_LOG` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NOT NULL DEFAULT '0' COMMENT '租户id',
              `STUDENT_ID` varchar(20) NOT NULL COMMENT '学员id',
              `FROM_STATUS` varchar(20) NULL DEFAULT NULL COMMENT '变更前状态',
              `TO_STATUS` varchar(20) NULL DEFAULT NULL COMMENT '变更后状态',
              `REASON` varchar(500) NULL DEFAULT NULL COMMENT '变更原因',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_STUDENT_STATUS_LOG_STUDENT` (`TENANT_ID`, `STUDENT_ID`, `CREATE_TIME`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
              COMMENT = '学员状态变更记录' ROW_FORMAT = Dynamic
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            log.info(">>> 学员档案增强结构升级检查开始");
            if (!columnExists("STUDENT", "STATUS_REASON")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT` ADD COLUMN `STATUS_REASON` varchar(500) NULL DEFAULT NULL "
                        + "COMMENT '状态变更原因' AFTER `STATUS`");
                log.info(">>> 学员结构升级：STUDENT.STATUS_REASON 已添加");
            }
            if (!columnExists("STUDENT", "STATUS_CHANGE_TIME")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT` ADD COLUMN `STATUS_CHANGE_TIME` datetime NULL DEFAULT NULL "
                        + "COMMENT '状态变更时间' AFTER `STATUS_REASON`");
                log.info(">>> 学员结构升级：STUDENT.STATUS_CHANGE_TIME 已添加");
            }
            // 历史状态回填：非在读学员补一个可追溯的变更时间，避免详情页空白
            int backfilled = jdbcTemplate.update("""
                    UPDATE `STUDENT`
                    SET `STATUS_CHANGE_TIME` = COALESCE(`UPDATE_TIME`, `CREATE_TIME`)
                    WHERE `STATUS` IS NOT NULL AND `STATUS` <> 'STUDYING'
                      AND `STATUS_CHANGE_TIME` IS NULL AND `DELETE_FLAG` = 'NOT_DELETE'
                    """);
            if (backfilled > 0) {
                log.info(">>> 学员结构升级：回填历史状态变更时间 {} 条", backfilled);
            }
            if (!indexExists("STUDENT", "IDX_STUDENT_TENANT_STATUS")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT` ADD INDEX `IDX_STUDENT_TENANT_STATUS` "
                        + "(`TENANT_ID`, `STATUS`)");
                log.info(">>> 学员结构升级：IDX_STUDENT_TENANT_STATUS 已添加");
            }
            if (!tableExists("STUDENT_TAG")) {
                jdbcTemplate.execute(STUDENT_TAG_DDL);
                log.info(">>> 学员结构升级：STUDENT_TAG 已创建");
            }
            if (!tableExists("STUDENT_TAG_REL")) {
                jdbcTemplate.execute(STUDENT_TAG_REL_DDL);
                log.info(">>> 学员结构升级：STUDENT_TAG_REL 已创建");
            }
            if (!tableExists("STUDENT_STATUS_LOG")) {
                jdbcTemplate.execute(STUDENT_STATUS_LOG_DDL);
                log.info(">>> 学员结构升级：STUDENT_STATUS_LOG 已创建");
            }
            log.info(">>> 学员档案增强结构升级检查完成");
        } catch (Exception e) {
            // 结构升级失败不阻塞应用启动，但必须留下明确日志由运维处理
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("学员档案增强结构升级",
                    "请手工执行 docs/_streams/student.md 中的迁移 SQL：" + e.getMessage());
            log.error(">>> 学员档案增强结构升级失败，请手工执行 docs/_streams/student.md 中的迁移 SQL：", e);
        }
    }

    @Override
    public int getOrder() {
        // 晚于 P0SchemaMigration（LOWEST_PRECEDENCE - 20）
        return Ordered.LOWEST_PRECEDENCE - 15;
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
