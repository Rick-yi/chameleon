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

import java.util.List;

/**
 * 续费预警新增接口权限
 * <p>
 * 新接口必须在 SYS_RESOURCE / SYS_RELATION 中有授权，否则业务接口权限拦截会返回 403。
 * 这里复制「续费预警分页」已有角色，并补充所有租户超管。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class StudentRenewWarnPermissionMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private record Permission(String id, String title, String url, boolean action) {
    }

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission("btn_rn_cfg_list", "预警配置列表", "/student/renewWarn/config/list", false),
            new Permission("btn_rn_cfg_save", "保存预警配置", "/student/renewWarn/config/save", true),
            new Permission("btn_rn_cfg_del", "删除预警配置", "/student/renewWarn/config/delete", true),
            new Permission("btn_rn_notify", "发送续费提醒", "/student/renewWarn/notify", true),
            new Permission("btn_rn_assign", "转顾问", "/student/renewWarn/assign", true),
            new Permission("btn_rn_process", "标记已处理", "/student/renewWarn/process", true),
            new Permission("btn_rn_assignee", "顾问选项", "/student/renewWarn/assigneeOptions", false),
            new Permission("btn_rn_risk_page", "风险学员列表", "/student/renewWarn/risk/page", false),
            new Permission("btn_rn_risk_todo", "风险学员待办", "/student/renewWarn/risk/todo", true),
            new Permission("btn_rn_follow_add", "登记跟进", "/student/renewWarn/follow/add", true),
            new Permission("btn_rn_follow_page", "跟进记录", "/student/renewWarn/follow/page", false),
            new Permission("btn_rn_stats", "续费流失统计", "/student/renewWarn/stats", false)
    );

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            int resourceAdded = 0;
            int menuGranted = 0;
            int roleGranted = 0;
            int adminGranted = 0;
            for (Permission permission : PERMISSIONS) {
                resourceAdded += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RESOURCE`
                            (`ID`, `PARENT_ID`, `TITLE`, `CODE`, `CATEGORY`, `MENU_TYPE`, `VISIBLE`,
                             `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `EXT_JSON`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`)
                        VALUES (?, 'menu_renew_001', ?, ?, 'BUTTON', 'BUTTON', 'YES', 'YES', 'YES', 0,
                                ?, 'NOT_DELETE', NOW(), '1')
                        """, permission.id(), permission.title(), permission.url(),
                        permission.action() ? "{\"action\":true}" : null);

                menuGranted += jdbcTemplate.update("""
                        UPDATE `SYS_RELATION`
                        SET `EXT_JSON` = JSON_ARRAY_APPEND(`EXT_JSON`, '$.buttonInfo', ?)
                        WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
                          AND `TARGET_ID` = 'menu_renew_001'
                          AND NOT JSON_CONTAINS(JSON_EXTRACT(`EXT_JSON`, '$.buttonInfo'), JSON_QUOTE(?))
                        """, permission.id(), permission.id());

                roleGranted += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RELATION`
                            (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                        SELECT CONCAT('rnp_', LEFT(MD5(CONCAT(r.`OBJECT_ID`, r.`TENANT_ID`, ?)), 15)),
                               r.`TENANT_ID`, r.`OBJECT_ID`, ?, 'SYS_ROLE_HAS_PERMISSION',
                               JSON_SET(r.`EXT_JSON`, '$.apiUrl', ?)
                        FROM `SYS_RELATION` r
                        WHERE r.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          AND r.`TARGET_ID` = '/student/renewWarn/page'
                          AND NOT EXISTS (
                              SELECT 1 FROM `SYS_RELATION` s
                              WHERE s.`OBJECT_ID` = r.`OBJECT_ID` AND s.`TARGET_ID` = ?
                                AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          )
                        """, permission.url(), permission.url(), permission.url(), permission.url());

                adminGranted += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RELATION`
                            (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                        SELECT CONCAT('rns_', LEFT(MD5(CONCAT(r.`ID`, r.`TENANT_ID`, ?)), 15)),
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
            if (resourceAdded > 0 || menuGranted > 0 || roleGranted > 0 || adminGranted > 0) {
                log.info(">>> 续费闭环权限升级：资源 {}，菜单授权 {}，角色接口授权 {}，超管授权 {}",
                        resourceAdded, menuGranted, roleGranted, adminGranted);
            }
        } catch (Exception e) {
            log.error(">>> 续费闭环权限升级失败，请手工执行 docs/_streams/renew.md 中的权限 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 14;
    }
}
