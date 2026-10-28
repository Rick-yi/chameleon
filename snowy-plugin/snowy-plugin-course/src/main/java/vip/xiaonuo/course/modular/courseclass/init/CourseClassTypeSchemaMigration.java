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
package vip.xiaonuo.course.modular.courseclass.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 班级班型结构升级
 * <p>
 * 一对多班级沿用原班级名册；一对一班直接绑定一名学员（STUDENT_ID），
 * 名册仍由服务层自动维护，排课/点名/课消链路保持不变。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Slf4j
@Component
public class CourseClassTypeSchemaMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            boolean changed = false;
            if (!columnExists("COURSE_CLASS", "CLASS_TYPE")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS` ADD COLUMN `CLASS_TYPE` varchar(20) NOT NULL "
                        + "DEFAULT 'GROUP' COMMENT '班型：GROUP一对多/ONE2ONE一对一' AFTER `COURSE_ID`");
                log.info(">>> 班级结构升级：COURSE_CLASS.CLASS_TYPE 已添加");
                changed = true;
            }
            if (!columnExists("COURSE_CLASS", "STUDENT_ID")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS` ADD COLUMN `STUDENT_ID` varchar(20) NULL DEFAULT NULL "
                        + "COMMENT '一对一班绑定的学员id' AFTER `CLASS_TYPE`");
                log.info(">>> 班级结构升级：COURSE_CLASS.STUDENT_ID 已添加");
                changed = true;
            }
            int backfilled = jdbcTemplate.update("""
                    UPDATE `COURSE_CLASS`
                    SET `CLASS_TYPE` = 'GROUP'
                    WHERE `CLASS_TYPE` IS NULL OR `CLASS_TYPE` = ''
                    """);
            if (backfilled > 0) {
                log.info(">>> 班级结构升级：回填历史班级班型 {} 条", backfilled);
                changed = true;
            }
            if (!indexExists("COURSE_CLASS", "IDX_COURSE_CLASS_TENANT_STUDENT")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS` ADD INDEX `IDX_COURSE_CLASS_TENANT_STUDENT` "
                        + "(`TENANT_ID`, `STUDENT_ID`)");
                log.info(">>> 班级结构升级：IDX_COURSE_CLASS_TENANT_STUDENT 已添加");
                changed = true;
            }
            if (changed) {
                log.info(">>> 班级班型结构升级完成");
            }
        } catch (Exception e) {
            vip.xiaonuo.common.context.StartupMigrationRegistry.record("班级班型结构升级",
                    "请手工执行 docs/_streams/sched.md 中的迁移 SQL：" + e.getMessage());
            log.error(">>> 班级班型结构升级失败，请手工执行 docs/_streams/sched.md 中的迁移 SQL", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 13;
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
}
