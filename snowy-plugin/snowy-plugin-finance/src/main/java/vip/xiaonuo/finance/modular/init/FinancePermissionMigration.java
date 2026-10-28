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
package vip.xiaonuo.finance.modular.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 财务新增接口权限
 * <p>
 * 新增 URL 必须进入 SYS_RESOURCE / SYS_ROLE_HAS_PERMISSION，否则业务接口拦截会 403。
 * 对比/元信息复制财务汇总权限，导出记录复制异步导出状态权限。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class FinancePermissionMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private record Permission(String id, String parentId, String title, String url, String baseUrl) {
    }

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission("btn_fin_compare", "menu_report_dash_001", "财务对比",
                    "/finance/report/compare", "/finance/report/summary"),
            new Permission("btn_fin_meta", "menu_report_dash_001", "报表元信息",
                    "/finance/report/meta", "/finance/report/summary"),
            new Permission("btn_fin_exp_page", "menu_report_fin_001", "导出记录",
                    "/finance/report/export/async/page", "/finance/report/export/async/status")
    );

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            int resourceAdded = 0;
            int menuGranted = 0;
            int roleGranted = 0;
            for (Permission permission : PERMISSIONS) {
                resourceAdded += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RESOURCE`
                            (`ID`, `PARENT_ID`, `TITLE`, `CODE`, `CATEGORY`, `MENU_TYPE`, `VISIBLE`,
                             `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `EXT_JSON`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`)
                        VALUES (?, ?, ?, ?, 'BUTTON', 'BUTTON', 'YES', 'YES', 'YES', 0,
                                '{"action":true}', 'NOT_DELETE', NOW(), '1')
                        """, permission.id(), permission.parentId(), permission.title(), permission.url());

                menuGranted += jdbcTemplate.update("""
                        UPDATE `SYS_RELATION`
                        SET `EXT_JSON` = JSON_ARRAY_APPEND(`EXT_JSON`, '$.buttonInfo', ?)
                        WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
                          AND `TARGET_ID` = ?
                          AND NOT JSON_CONTAINS(JSON_EXTRACT(`EXT_JSON`, '$.buttonInfo'), JSON_QUOTE(?))
                        """, permission.id(), permission.parentId(), permission.id());

                roleGranted += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RELATION`
                            (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                        SELECT CONCAT('fip_', LEFT(MD5(CONCAT(r.`OBJECT_ID`, r.`TENANT_ID`, ?)), 15)),
                               r.`TENANT_ID`, r.`OBJECT_ID`, ?, 'SYS_ROLE_HAS_PERMISSION',
                               JSON_SET(r.`EXT_JSON`, '$.apiUrl', ?)
                        FROM `SYS_RELATION` r
                        WHERE r.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          AND r.`TARGET_ID` = ?
                          AND NOT EXISTS (
                              SELECT 1 FROM `SYS_RELATION` s
                              WHERE s.`OBJECT_ID` = r.`OBJECT_ID` AND s.`TARGET_ID` = ?
                                AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          )
                        """, permission.url(), permission.url(), permission.url(),
                        permission.baseUrl(), permission.url());

                jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RELATION`
                            (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                        SELECT CONCAT('fis_', LEFT(MD5(CONCAT(r.`ID`, r.`TENANT_ID`, ?)), 15)),
                               r.`TENANT_ID`, r.`ID`, ?, 'SYS_ROLE_HAS_PERMISSION',
                               JSON_OBJECT('scopeCategory', 'SCOPE_ALL', 'scopeDefineOrgIdList', JSON_ARRAY(),
                                           'apiUrl', ?)
                        FROM `SYS_ROLE` r
                        WHERE r.`CODE` = 'superAdmin'
                          AND NOT EXISTS (
                              SELECT 1 FROM `SYS_RELATION` s
                              WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = ?
                                AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          )
                        """, permission.url(), permission.url(), permission.url(), permission.url());
            }
            if (resourceAdded > 0 || menuGranted > 0 || roleGranted > 0) {
                log.info(">>> 财务权限升级：资源 {}，菜单授权 {}，角色接口授权 {}",
                        resourceAdded, menuGranted, roleGranted);
            }
        } catch (Exception e) {
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("财务权限升级",
                    "请手工执行 docs/_streams/finance.md 中的权限 SQL：" + e.getMessage());
            log.error(">>> 财务权限升级失败，请手工执行 docs/_streams/finance.md 中的权限 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 14;
    }
}
