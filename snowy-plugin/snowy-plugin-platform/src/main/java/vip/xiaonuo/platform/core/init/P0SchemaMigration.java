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
package vip.xiaonuo.platform.core.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * P0 结构升级
 * <p>
 * 仅做可重复执行的补丁检查：缺列补列、缺表建表、补租户编码、清理越权平台菜单。
 * 正式多环境部署建议改用 Flyway/Liquibase，本类用于兼容当前已有数据库。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Component
public class P0SchemaMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private static final String NOTIFY_LOG_DDL = """
            CREATE TABLE IF NOT EXISTS `NOTIFY_LOG` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `BIZ_TYPE` varchar(50) NULL DEFAULT NULL COMMENT '业务类型',
              `RECEIVER_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '接收人类型',
              `RECEIVER_ID` varchar(20) NULL DEFAULT NULL COMMENT '接收人id',
              `STUDENT_ID` varchar(20) NULL DEFAULT NULL COMMENT '学员id',
              `TITLE` varchar(255) NULL DEFAULT NULL COMMENT '标题',
              `CONTENT` varchar(1000) NULL DEFAULT NULL COMMENT '内容',
              `CHANNEL` varchar(20) NULL DEFAULT NULL COMMENT '渠道',
              `STATUS` varchar(20) NULL DEFAULT NULL COMMENT '发送状态',
              `SEND_TIME` datetime NULL DEFAULT NULL COMMENT '发送时间',
              `REMARK` varchar(500) NULL DEFAULT NULL COMMENT '备注',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_NOTIFY_TENANT_STUDENT` (`TENANT_ID`, `STUDENT_ID`),
              INDEX `IDX_NOTIFY_STATUS` (`STATUS`, `CREATE_TIME`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知流水' ROW_FORMAT = Dynamic
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            if (!columnExists("T_TENANT", "CODE")) {
                jdbcTemplate.execute("ALTER TABLE `T_TENANT` ADD COLUMN `CODE` varchar(32) NULL DEFAULT NULL COMMENT '租户编码(登录识别机构)' AFTER `ID`");
                log.info(">>> P0 结构升级：T_TENANT.CODE 已添加");
            }
            jdbcTemplate.update("UPDATE `T_TENANT` SET `CODE` = 'demo' WHERE `ID` = '1554740179362967450' AND (`CODE` IS NULL OR `CODE` = '')");
            jdbcTemplate.update("UPDATE `T_TENANT` SET `CODE` = 'test' WHERE `NAME` LIKE '%联调%' AND (`CODE` IS NULL OR `CODE` = '')");
            jdbcTemplate.update("UPDATE `T_TENANT` SET `CODE` = CONCAT('t', `ID`) WHERE (`CODE` IS NULL OR `CODE` = '') AND `ID` <> '0'");
            if (!indexExists("T_TENANT", "UK_TENANT_CODE")) {
                jdbcTemplate.execute("ALTER TABLE `T_TENANT` ADD UNIQUE INDEX `UK_TENANT_CODE` (`CODE`)");
                log.info(">>> P0 结构升级：UK_TENANT_CODE 已添加");
            }

            if (!columnExists("STUDENT_ORDER", "CONSUMED_LESSONS")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD COLUMN `CONSUMED_LESSONS` decimal(10,2) NULL DEFAULT 0.00 COMMENT '已消耗课时(含赠课)' AFTER `PAY_TIME`");
                log.info(">>> P0 结构升级：STUDENT_ORDER.CONSUMED_LESSONS 已添加");
            }
            if (!columnExists("STUDENT_ORDER", "REFUNDED_LESSONS")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD COLUMN `REFUNDED_LESSONS` decimal(10,2) NULL DEFAULT 0.00 COMMENT '已退课时(不含赠课)' AFTER `CONSUMED_LESSONS`");
                log.info(">>> P0 结构升级：STUDENT_ORDER.REFUNDED_LESSONS 已添加");
            }
            jdbcTemplate.update("UPDATE `STUDENT_ORDER` SET `CONSUMED_LESSONS` = 0.00 WHERE `CONSUMED_LESSONS` IS NULL");
            jdbcTemplate.update("UPDATE `STUDENT_ORDER` SET `REFUNDED_LESSONS` = 0.00 WHERE `REFUNDED_LESSONS` IS NULL");
            if (!columnExists("STUDENT_ORDER", "CAMPUS_ID")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD COLUMN `CAMPUS_ID` varchar(20) NULL DEFAULT NULL COMMENT '校区id(下单快照)' AFTER `COURSE_ID`");
                log.info(">>> 报表结构升级：STUDENT_ORDER.CAMPUS_ID 已添加");
            }
            // 历史订单按学员当前校区回填，避免按校区筛选时缺失
            jdbcTemplate.update("UPDATE `STUDENT_ORDER` o JOIN `STUDENT` s ON o.`STUDENT_ID` = s.`ID` SET o.`CAMPUS_ID` = s.`CAMPUS_ID` WHERE o.`CAMPUS_ID` IS NULL");
            if (!indexExists("STUDENT_ORDER", "IDX_ORDER_TENANT_PAYTIME")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD INDEX `IDX_ORDER_TENANT_PAYTIME` (`TENANT_ID`, `PAY_TIME`, `STATUS`)");
            }
            if (!indexExists("STUDENT_ORDER", "IDX_ORDER_TENANT_CAMPUS")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD INDEX `IDX_ORDER_TENANT_CAMPUS` (`TENANT_ID`, `CAMPUS_ID`, `PAY_TIME`)");
            }

            if (!columnExists("STUDENT_REFUND", "GIFT_LESSONS")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_REFUND` ADD COLUMN `GIFT_LESSONS` decimal(10,2) NULL DEFAULT 0.00 COMMENT '随订单作废的赠课课时' AFTER `REFUND_LESSONS`");
                log.info(">>> P0 结构升级：STUDENT_REFUND.GIFT_LESSONS 已添加");
            }
            jdbcTemplate.update("UPDATE `STUDENT_REFUND` SET `GIFT_LESSONS` = 0.00 WHERE `GIFT_LESSONS` IS NULL");
            if (!columnExists("STUDENT_REFUND", "REFUND_TIME")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_REFUND` ADD COLUMN `REFUND_TIME` datetime NULL DEFAULT NULL COMMENT '退款完成时间' AFTER `APPROVE_TIME`");
                log.info(">>> 报表结构升级：STUDENT_REFUND.REFUND_TIME 已添加");
            }
            // 历史已退款单回填完成时间
            jdbcTemplate.update("UPDATE `STUDENT_REFUND` SET `REFUND_TIME` = COALESCE(`UPDATE_TIME`, `APPROVE_TIME`, `CREATE_TIME`) WHERE `STATUS` = 'REFUNDED' AND `REFUND_TIME` IS NULL");
            if (!indexExists("STUDENT_REFUND", "IDX_REFUND_TENANT_REFUNDTIME")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_REFUND` ADD INDEX `IDX_REFUND_TENANT_REFUNDTIME` (`TENANT_ID`, `STATUS`, `REFUND_TIME`)");
            }

            if (!tableExists("NOTIFY_LOG")) {
                jdbcTemplate.execute(NOTIFY_LOG_DDL);
                log.info(">>> P0 结构升级：NOTIFY_LOG 已创建");
            }

            int removed = jdbcTemplate.update("DELETE FROM `SYS_RELATION` WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE' AND `TARGET_ID` IN ('menu_tenant_001','menu_ppkg_001') AND `TENANT_ID` <> '0'");
            if (removed > 0) {
                log.info(">>> P0 结构升级：已清理 {} 条业务租户平台菜单越权授权", removed);
            }
        } catch (Exception e) {
            log.error(">>> P0 结构升级失败，请手工执行 chameleon_p0_migration.sql", e);
        }
    }

    @Override
    public int getOrder() {
        // 早于 PlatformBootstrap（LOWEST_PRECEDENCE - 10）
        return Ordered.LOWEST_PRECEDENCE - 20;
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