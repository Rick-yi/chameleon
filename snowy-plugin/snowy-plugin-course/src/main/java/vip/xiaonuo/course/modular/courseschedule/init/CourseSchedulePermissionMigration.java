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
package vip.xiaonuo.course.modular.courseschedule.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 排课新增接口权限
 * <p>
 * 批量冲突预检/复制上周复用「手动排课」的授权角色。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class CourseSchedulePermissionMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    private record Permission(String id, String title, String url, boolean action) {
    }

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission("btn_sched_conflict", "排课冲突预检", "/course/schedule/conflictCheckBatch", false),
            new Permission("btn_sched_copy_week", "复制上周课表", "/course/schedule/copyLastWeek", true)
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
                        VALUES (?, 'menu_schedule_001', ?, ?, 'BUTTON', 'BUTTON', 'YES', 'YES', 'YES', 0,
                                ?, 'NOT_DELETE', NOW(), '1')
                        """, permission.id(), permission.title(), permission.url(),
                        permission.action() ? "{\"action\":true}" : null);

                menuGranted += jdbcTemplate.update("""
                        UPDATE `SYS_RELATION`
                        SET `EXT_JSON` = JSON_ARRAY_APPEND(`EXT_JSON`, '$.buttonInfo', ?)
                        WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
                          AND `TARGET_ID` = 'menu_schedule_001'
                          AND NOT JSON_CONTAINS(JSON_EXTRACT(`EXT_JSON`, '$.buttonInfo'), JSON_QUOTE(?))
                        """, permission.id(), permission.id());

                roleGranted += jdbcTemplate.update("""
                        INSERT IGNORE INTO `SYS_RELATION`
                            (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
                        SELECT CONCAT('rsp_', LEFT(MD5(CONCAT(r.`OBJECT_ID`, r.`TENANT_ID`, ?)), 15)),
                               r.`TENANT_ID`, r.`OBJECT_ID`, ?, 'SYS_ROLE_HAS_PERMISSION',
                               JSON_SET(r.`EXT_JSON`, '$.apiUrl', ?)
                        FROM `SYS_RELATION` r
                        WHERE r.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          AND r.`TARGET_ID` = '/course/schedule/add'
                          AND NOT EXISTS (
                              SELECT 1 FROM `SYS_RELATION` s
                              WHERE s.`OBJECT_ID` = r.`OBJECT_ID` AND s.`TARGET_ID` = ?
                                AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
                          )
                        """, permission.url(), permission.url(), permission.url(), permission.url());
            }
            if (resourceAdded > 0 || menuGranted > 0 || roleGranted > 0) {
                log.info(">>> 排课权限升级：资源 {}，菜单授权 {}，角色接口授权 {}",
                        resourceAdded, menuGranted, roleGranted);
            }
        } catch (Exception e) {
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("排课权限升级",
                    "请手工执行 docs/_streams/sched.md 中的权限 SQL：" + e.getMessage());
            log.error(">>> 排课权限升级失败，请手工执行 docs/_streams/sched.md 中的权限 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 14;
    }
}
