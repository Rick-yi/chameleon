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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 平台运营能力结构升级
 * <p>
 * 独立于 P0SchemaMigration 的幂等补丁：仅补套餐字段、账单表和账单菜单，重复执行无副作用。
 * 执行顺序早于 PlatformBootstrap，保证平台运营角色能自动获得新增菜单授权。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Slf4j
@Component
public class PlatformOpsSchemaMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private static final String PLATFORM_BILL_DDL = """
            CREATE TABLE IF NOT EXISTS `T_PLATFORM_BILL` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `BILL_NO` varchar(40) NULL DEFAULT NULL COMMENT '账单号',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `PACKAGE_ID` varchar(20) NULL DEFAULT NULL COMMENT '套餐id',
              `PERIOD_START` datetime NULL DEFAULT NULL COMMENT '账单周期开始',
              `PERIOD_END` datetime NULL DEFAULT NULL COMMENT '账单周期结束',
              `AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '金额(元)',
              `STATUS` varchar(20) NULL DEFAULT 'UNPAID' COMMENT '状态(UNPAID/PAID/VOID)',
              `PAY_METHOD` varchar(20) NULL DEFAULT NULL COMMENT '收款方式(OFFLINE/BANK/OTHER)',
              `PAY_TIME` datetime NULL DEFAULT NULL COMMENT '收款时间',
              `INVOICE_STATUS` varchar(20) NULL DEFAULT 'NONE' COMMENT '发票状态(NONE/REQUESTED/ISSUED)',
              `SOURCE` varchar(20) NULL DEFAULT 'MANUAL' COMMENT '来源(MANUAL/RENEW)',
              `REMARK` varchar(500) NULL DEFAULT NULL COMMENT '备注',
              `OPERATOR_ID` varchar(20) NULL DEFAULT NULL COMMENT '操作人id',
              `OPERATOR_NAME` varchar(100) NULL DEFAULT NULL COMMENT '操作人',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_PLATFORM_BILL_TENANT` (`TENANT_ID`, `CREATE_TIME`),
              INDEX `IDX_PLATFORM_BILL_STATUS` (`STATUS`, `CREATE_TIME`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
              COMMENT = '平台内部账单/收款记录(对接真实支付前的人工对账)' ROW_FORMAT = Dynamic
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            log.info(">>> 平台运营结构升级检查开始");
            if (!columnExists("T_TENANT_PACKAGE", "ACCOUNT_LIMIT")) {
                jdbcTemplate.execute("ALTER TABLE `T_TENANT_PACKAGE` ADD COLUMN `ACCOUNT_LIMIT` int NULL DEFAULT NULL "
                        + "COMMENT '账号数上限' AFTER `CAMPUS_LIMIT`");
                log.info(">>> 平台结构升级：T_TENANT_PACKAGE.ACCOUNT_LIMIT 已添加");
            }
            if (!columnExists("T_TENANT_PACKAGE", "STATUS")) {
                jdbcTemplate.execute("ALTER TABLE `T_TENANT_PACKAGE` ADD COLUMN `STATUS` varchar(20) NULL DEFAULT 'ON' "
                        + "COMMENT '上下架状态(ON/OFF)' AFTER `FUNC_CONFIG`");
                jdbcTemplate.update("UPDATE `T_TENANT_PACKAGE` SET `STATUS` = 'ON' WHERE `STATUS` IS NULL OR `STATUS` = ''");
                log.info(">>> 平台结构升级：T_TENANT_PACKAGE.STATUS 已添加");
            }
            if (!tableExists("T_PLATFORM_BILL")) {
                jdbcTemplate.execute(PLATFORM_BILL_DDL);
                log.info(">>> 平台结构升级：T_PLATFORM_BILL 已创建");
            }
            // 平台账单菜单：挂到平台运营模块，由 PlatformBootstrap 统一授权给 platformAdmin
            int menuAdded = jdbcTemplate.update("""
                    INSERT IGNORE INTO `SYS_RESOURCE`
                        (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`,
                         `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`,
                         `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`)
                    VALUES ('menu_plat_bill_001', '0', '账单管理', 'platformBill', 'platformBill', 'MENU',
                            'mod_platform_001', 'MENU', '/platform/bill', 'platform/bill', 'file-text-outlined',
                            NULL, 'YES', 'YES', 'YES', 3, 'NOT_DELETE', NOW(), '1')
                    """);
            if (menuAdded > 0) {
                log.info(">>> 平台结构升级：账单管理菜单已创建");
            }
            // 平台租户缺少 B 端密码有效期配置时，登录会抛“请联系管理员配置密码有效期天数”
            int configAdded = jdbcTemplate.update("""
                    INSERT INTO `DEV_CONFIG`
                        (`ID`, `TENANT_ID`, `CONFIG_KEY`, `CONFIG_VALUE`, `CATEGORY`, `REMARK`, `SORT_CODE`,
                         `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`)
                    SELECT 'dev_pwd_expire_0', '0', 'SNOWY_SYS_DEFAULT_PASSWORD_EXPIRED_DAYS_FOR_B', '30',
                           'PASSWORD_STRATEGY_FOR_B', 'B端密码有效期天数', 160, 'NOT_DELETE', NOW(), '1'
                    WHERE NOT EXISTS (
                        SELECT 1 FROM `DEV_CONFIG`
                        WHERE `TENANT_ID` = '0' AND `CONFIG_KEY` = 'SNOWY_SYS_DEFAULT_PASSWORD_EXPIRED_DAYS_FOR_B'
                    )
                    """);
            if (configAdded > 0) {
                log.info(">>> 平台结构升级：平台租户密码有效期默认配置已补充");
            }
        } catch (Exception e) {
            log.error(">>> 平台运营结构升级失败，请手工执行 docs/_streams/platform.md 中的迁移 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        // 早于 PlatformBootstrap（LOWEST_PRECEDENCE - 10），保证菜单授权覆盖新增菜单
        return Ordered.LOWEST_PRECEDENCE - 15;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
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
