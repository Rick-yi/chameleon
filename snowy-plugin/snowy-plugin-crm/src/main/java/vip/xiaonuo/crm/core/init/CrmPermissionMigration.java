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
package vip.xiaonuo.crm.core.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * CRM 新增接口权限补齐
 * <p>
 * 试听结果更新是新增接口，复用「新增试听」的授权角色；不修改 P0SchemaMigration。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class CrmPermissionMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private static final String TRIAL_UPDATE_RESOURCE_ID = "btn_crm_trial_update";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO `SYS_RESOURCE`
                        (`ID`, `PARENT_ID`, `TITLE`, `CODE`, `CATEGORY`, `MENU_TYPE`, `VISIBLE`,
                         `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `EXT_JSON`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`)
                    VALUES (?, 'menu_crm_001', '更新试听结果', '/crm/trial/update',
                            'BUTTON', 'BUTTON', 'YES', 'YES', 'YES', 0, '{"action":true}', 'NOT_DELETE', NOW(), '1')
                    """, TRIAL_UPDATE_RESOURCE_ID);

            int buttonGranted = jdbcTemplate.update("""
                    UPDATE `SYS_RELATION`
                    SET `EXT_JSON` = JSON_ARRAY_APPEND(`EXT_JSON`, '$.buttonInfo', ?)
                    WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
                      AND `TARGET_ID` = 'menu_crm_001'
                      AND JSON_CONTAINS(JSON_EXTRACT(`EXT_JSON`, '$.buttonInfo'),
                          JSON_QUOTE((SELECT `ID` FROM `SYS_RESOURCE` WHERE `CODE` = '/crm/trial/add' LIMIT 1)))
                      AND NOT JSON_CONTAINS(JSON_EXTRACT(`EXT_JSON`, '$.buttonInfo'), JSON_QUOTE(?))
                    """, TRIAL_UPDATE_RESOURCE_ID, TRIAL_UPDATE_RESOURCE_ID);

            int permissionGranted = jdbcTemplate.update("""
                    INSERT IGNORE INTO `SYS_RELATION`
                        (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                    SELECT CONCAT('crpp_', LEFT(MD5(CONCAT(r.`OBJECT_ID`, r.`TENANT_ID`)), 15)),
                           r.`TENANT_ID`, r.`OBJECT_ID`, '/crm/trial/update', 'SYS_ROLE_HAS_PERMISSION',
                           JSON_SET(r.`EXT_JSON`, '$.apiUrl', '/crm/trial/update')
                    FROM `SYS_RELATION` r
                    WHERE r.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                      AND r.`TARGET_ID` = '/crm/trial/add'
                      AND NOT EXISTS (
                          SELECT 1 FROM `SYS_RELATION` s
                          WHERE s.`OBJECT_ID` = r.`OBJECT_ID` AND s.`TARGET_ID` = '/crm/trial/update'
                            AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                      )
                    """);

            int superAdminGranted = jdbcTemplate.update("""
                    INSERT IGNORE INTO `SYS_RELATION`
                        (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                    SELECT CONCAT('crps_', LEFT(MD5(CONCAT(r.`ID`, r.`TENANT_ID`)), 15)),
                           r.`TENANT_ID`, r.`ID`, '/crm/trial/update', 'SYS_ROLE_HAS_PERMISSION',
                           JSON_OBJECT('scopeCategory', 'SCOPE_ALL', 'scopeDefineOrgIdList', JSON_ARRAY(),
                                       'apiUrl', '/crm/trial/update')
                    FROM `SYS_ROLE` r
                    WHERE r.`CODE` = 'superAdmin'
                      AND NOT EXISTS (
                          SELECT 1 FROM `SYS_RELATION` s
                          WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = '/crm/trial/update'
                            AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                      )
                    """);

            if (buttonGranted > 0 || permissionGranted > 0 || superAdminGranted > 0) {
                log.info(">>> CRM 权限升级：试听结果更新已授权 {} 个菜单角色 / {} 个试听角色 / {} 个超管",
                        buttonGranted, permissionGranted, superAdminGranted);
            }
        } catch (Exception e) {
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("CRM 权限升级",
                    "请手工执行 docs/_streams/crm.md 中的权限 SQL：" + e.getMessage());
            log.error(">>> CRM 权限升级失败，请手工执行 docs/_streams/crm.md 中的权限 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 15;
    }
}
