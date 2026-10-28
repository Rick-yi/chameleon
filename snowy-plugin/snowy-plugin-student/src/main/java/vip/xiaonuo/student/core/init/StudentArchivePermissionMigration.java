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

import java.util.List;
import vip.xiaonuo.common.context.StartupPhase;

/**
 * 学员档案增强接口权限
 * <p>
 * 标签、状态变更、导入导出、时间线都是新增接口，必须补资源与角色授权。
 * 读取类复用「学员分页」权限，写入类复用「编辑学员」权限。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class StudentArchivePermissionMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private record Permission(String id, String title, String url, String baseUrl, boolean action) {
    }

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission("btn_stu_tag_list", "学员标签列表", "/student/tag/list", "/student/page", false),
            new Permission("btn_stu_tag_of", "学员已选标签", "/student/tag/studentTags", "/student/page", false),
            new Permission("btn_stu_tag_add", "新增学员标签", "/student/tag/add", "/student/edit", true),
            new Permission("btn_stu_tag_edit", "编辑学员标签", "/student/tag/edit", "/student/edit", true),
            new Permission("btn_stu_tag_del", "删除学员标签", "/student/tag/delete", "/student/edit", true),
            new Permission("btn_stu_tag_bind", "绑定学员标签", "/student/tag/bind", "/student/edit", true),
            new Permission("btn_stu_status_chg", "变更学员状态", "/student/status/change", "/student/edit", true),
            new Permission("btn_stu_status_log", "学员状态记录", "/student/status/logs", "/student/page", false),
            new Permission("btn_stu_import_tpl", "学员导入模板", "/student/importTemplate", "/student/page", false),
            new Permission("btn_stu_import", "批量导入学员", "/student/import", "/student/edit", true),
            new Permission("btn_stu_export", "导出学员", "/student/export", "/student/page", false),
            new Permission("btn_stu_timeline", "学员动态时间线", "/student/timeline", "/student/page", false)
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
                        VALUES (?, 'menu_student_001', ?, ?, 'BUTTON', 'BUTTON', 'YES', 'YES', 'YES', 0,
                                ?, 'NOT_DELETE', NOW(), '1')
                        """, permission.id(), permission.title(), permission.url(),
                        permission.action() ? "{\"action\":true}" : null);

                menuGranted += jdbcTemplate.update("""
                        UPDATE `SYS_RELATION`
                        SET `EXT_JSON` = JSON_ARRAY_APPEND(`EXT_JSON`, '$.buttonInfo', ?)
                        WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
                          AND `TARGET_ID` = 'menu_student_001'
                          AND NOT JSON_CONTAINS(JSON_EXTRACT(`EXT_JSON`, '$.buttonInfo'), JSON_QUOTE(?))
                        """, permission.id(), permission.id());

                roleGranted += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RELATION`
                            (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                        SELECT CONCAT('sap_', LEFT(MD5(CONCAT(r.`OBJECT_ID`, r.`TENANT_ID`, ?)), 15)),
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
                        SELECT CONCAT('sas_', LEFT(MD5(CONCAT(r.`ID`, r.`TENANT_ID`, ?)), 15)),
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
                log.info(">>> 学员档案权限升级：资源 {}，菜单授权 {}，角色接口授权 {}",
                        resourceAdded, menuGranted, roleGranted);
            }
        } catch (Exception e) {
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("学员档案权限升级",
                    "请手工执行 docs/_streams/student.md 中的权限 SQL：" + e.getMessage());
            log.error(">>> 学员档案权限升级失败，请手工执行 docs/_streams/student.md 中的权限 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        return StartupPhase.DOMAIN_PERMISSION;
    }
}
