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

    private static final String FIN_DAILY_STAT_DDL = """
            CREATE TABLE IF NOT EXISTS `FIN_DAILY_STAT` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `STAT_DATE` datetime NULL DEFAULT NULL COMMENT '统计日期',
              `CAMPUS_ID` varchar(20) NULL DEFAULT NULL COMMENT '校区id',
              `COURSE_ID` varchar(20) NULL DEFAULT NULL COMMENT '课程id',
              `PAID_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '收款金额',
              `REFUND_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '退费金额',
              `NET_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '净营收',
              `ORDER_COUNT` bigint NULL DEFAULT 0 COMMENT '订单数',
              `PAYING_STUDENT_COUNT` bigint NULL DEFAULT 0 COMMENT '付费学员数',
              `ATTEND_LESSONS` decimal(10,2) NULL DEFAULT 0.00 COMMENT '点名消课课时',
              `RESTORE_LESSONS` decimal(10,2) NULL DEFAULT 0.00 COMMENT '回补课时',
              `NET_LESSONS` decimal(10,2) NULL DEFAULT 0.00 COMMENT '净课消课时',
              `RECOGNIZED_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '课消金额(权责口径)',
              `NEW_STUDENT_COUNT` bigint NULL DEFAULT 0 COMMENT '新增学员数',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_FIN_STAT_TENANT_DATE` (`TENANT_ID`, `STAT_DATE`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '财务日汇总' ROW_FORMAT = Dynamic
            """;

    /** 排课学员表 DDL（不挂班级时直接指定这节课的学员） */
    private static final String SCHEDULE_STUDENT_DDL = """
            CREATE TABLE IF NOT EXISTS `COURSE_SCHEDULE_STUDENT` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `SCHEDULE_ID` varchar(20) NOT NULL COMMENT '排课id',
              `STUDENT_ID` varchar(20) NOT NULL COMMENT '学员id',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              UNIQUE INDEX `UK_SCHEDULE_STUDENT` (`SCHEDULE_ID`, `STUDENT_ID`),
              INDEX `IDX_SCHEDULE_STUDENT_STUDENT` (`TENANT_ID`, `STUDENT_ID`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '排课学员(不挂班级时直接指定)';
            """;

    /** 停课日 DDL（节假日/停课，生成排课时跳过） */
    private static final String SCHEDULE_CLOSURE_DDL = """
            CREATE TABLE IF NOT EXISTS `COURSE_SCHEDULE_CLOSURE` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `CAMPUS_ID` varchar(20) NULL DEFAULT NULL COMMENT '校区id(为空表示全机构)',
              `CLOSURE_DATE` date NOT NULL COMMENT '停课日期',
              `NAME` varchar(50) NULL DEFAULT NULL COMMENT '停课名称',
              `REMARK` varchar(200) NULL DEFAULT NULL COMMENT '备注',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              UNIQUE INDEX `UK_CLOSURE_TENANT_CAMPUS_DATE` (`TENANT_ID`, `CAMPUS_ID`, `CLOSURE_DATE`),
              INDEX `IDX_CLOSURE_TENANT_DATE` (`TENANT_ID`, `CLOSURE_DATE`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '停课日(节假日)';
            """;

    /** 教师不可用时段 DDL */
    private static final String TEACHER_UNAVAILABLE_DDL = """
            CREATE TABLE IF NOT EXISTS `COURSE_TEACHER_UNAVAILABLE` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `TEACHER_ID` varchar(20) NOT NULL COMMENT '教师id',
              `START_TIME` datetime NOT NULL COMMENT '不可用开始',
              `END_TIME` datetime NOT NULL COMMENT '不可用结束',
              `REASON` varchar(200) NULL DEFAULT NULL COMMENT '原因',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              INDEX `IDX_UNAVAILABLE_TENANT_TEACHER` (`TENANT_ID`, `TEACHER_ID`, `START_TIME`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师不可用时段';
            """;

    /** 业务埋点表 DDL */
    private static final String BIZ_EVENT_DDL = """
            CREATE TABLE IF NOT EXISTS `BIZ_EVENT` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `EVENT_CODE` varchar(50) NOT NULL COMMENT '事件码',
              `EVENT_NAME` varchar(100) NULL DEFAULT NULL COMMENT '事件名称',
              `USER_ID` varchar(20) NULL DEFAULT NULL COMMENT '操作人id',
              `USER_NAME` varchar(50) NULL DEFAULT NULL COMMENT '操作人姓名',
              `TARGET_TYPE` varchar(30) NULL DEFAULT NULL COMMENT '对象类型',
              `TARGET_ID` varchar(100) NULL DEFAULT NULL COMMENT '对象id',
              `TARGET_NAME` varchar(100) NULL DEFAULT NULL COMMENT '对象名称',
              `PROPS` varchar(1000) NULL DEFAULT NULL COMMENT '业务属性(JSON)',
              `EVENT_TIME` datetime NULL DEFAULT NULL COMMENT '事件发生时间',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              INDEX `IDX_EVENT_TENANT_CODE_TIME` (`TENANT_ID`, `EVENT_CODE`, `EVENT_TIME`),
              INDEX `IDX_EVENT_TENANT_TIME` (`TENANT_ID`, `EVENT_TIME`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务埋点流水';
            """;

    private static final String BIZ_EXPORT_TASK_DDL = """
            CREATE TABLE IF NOT EXISTS `BIZ_EXPORT_TASK` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `EXPORT_TYPE` varchar(50) NOT NULL COMMENT '导出类型编码',
              `PARAM_JSON` text NULL COMMENT '查询参数(JSON)',
              `STATUS` varchar(20) NOT NULL COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED',
              `FILE_NAME` varchar(200) NULL DEFAULT NULL COMMENT '下载文件名',
              `FILE_PATH` varchar(500) NULL DEFAULT NULL COMMENT '服务器文件路径',
              `ROW_COUNT` bigint NULL DEFAULT NULL COMMENT '数据行数',
              `ERROR_MSG` varchar(500) NULL DEFAULT NULL COMMENT '失败原因',
              `FINISH_TIME` datetime NULL DEFAULT NULL COMMENT '完成时间',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
              PRIMARY KEY (`ID`),
              INDEX `IDX_EXPORT_TENANT_TIME` (`TENANT_ID`, `CREATE_TIME`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '异步导出任务';
            """;

    private static final String TENANT_LOGIN_DDL = """
            CREATE TABLE IF NOT EXISTS `T_TENANT_LOGIN` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `USER_ID` varchar(20) NULL DEFAULT NULL COMMENT '用户id',
              `ACCOUNT` varchar(100) NULL DEFAULT NULL COMMENT '账号',
              `LOGIN_TIME` datetime NULL DEFAULT NULL COMMENT '登录时间',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_TENANT_LOGIN_TIME` (`TENANT_ID`, `LOGIN_TIME`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户登录记录' ROW_FORMAT = Dynamic
            """;

    private static final String FIN_EXPORT_TASK_DDL = """
            CREATE TABLE IF NOT EXISTS `FIN_EXPORT_TASK` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `EXPORT_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '导出类型',
              `PARAM_JSON` varchar(1000) NULL DEFAULT NULL COMMENT '查询参数',
              `STATUS` varchar(20) NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/RUNNING/SUCCESS/FAILED)',
              `FILE_PATH` varchar(500) NULL DEFAULT NULL COMMENT '生成文件路径',
              `FILE_NAME` varchar(255) NULL DEFAULT NULL COMMENT '下载文件名',
              `ROW_COUNT` bigint NULL DEFAULT 0 COMMENT '数据行数',
              `ERROR_MSG` varchar(500) NULL DEFAULT NULL COMMENT '失败原因',
              `FINISH_TIME` datetime NULL DEFAULT NULL COMMENT '完成时间',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_FIN_EXPORT_TENANT` (`TENANT_ID`, `STATUS`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报表异步导出任务' ROW_FORMAT = Dynamic
            """;

    private static final String TENANT_RENEW_DDL = """
            CREATE TABLE IF NOT EXISTS `T_TENANT_RENEW` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
              `PACKAGE_ID` varchar(20) NULL DEFAULT NULL COMMENT '续费套餐id',
              `DAYS` int NULL DEFAULT 0 COMMENT '续费天数',
              `AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '续费金额',
              `OLD_EXPIRE_TIME` datetime NULL DEFAULT NULL COMMENT '原到期时间',
              `NEW_EXPIRE_TIME` datetime NULL DEFAULT NULL COMMENT '新到期时间',
              `REMARK` varchar(500) NULL DEFAULT NULL COMMENT '备注',
              `OPERATOR_ID` varchar(20) NULL DEFAULT NULL COMMENT '操作人id',
              `OPERATOR_NAME` varchar(100) NULL DEFAULT NULL COMMENT '操作人',
              `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_TENANT_RENEW_TENANT` (`TENANT_ID`, `CREATE_TIME`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户续费记录' ROW_FORMAT = Dynamic
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            log.info(">>> P0 结构升级检查开始");
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
            if (!columnExists("STUDENT_ORDER", "REMARK")) {
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD COLUMN `REMARK` varchar(500) NULL DEFAULT NULL COMMENT '收款备注' AFTER `PAY_CHANNEL`");
                log.info(">>> 账务结构升级：STUDENT_ORDER.REMARK 已添加");
            }
            // 历史学员校区回填：优先取在读班级校区，其次取机构第一个部门/校区
            int studentCampusFilled = jdbcTemplate.update("""
                    UPDATE `STUDENT` s
                    SET s.`CAMPUS_ID` = (
                        SELECT MIN(c.`CAMPUS_ID`)
                        FROM `COURSE_CLASS_STUDENT` cs
                        JOIN `COURSE_CLASS` c ON c.`ID` = cs.`CLASS_ID`
                        WHERE cs.`STUDENT_ID` = s.`ID`
                          AND cs.`DELETE_FLAG` = 'NOT_DELETE'
                          AND c.`DELETE_FLAG` = 'NOT_DELETE'
                          AND c.`CAMPUS_ID` IS NOT NULL
                    )
                    WHERE s.`CAMPUS_ID` IS NULL
                      AND s.`DELETE_FLAG` = 'NOT_DELETE'
                      AND EXISTS (
                        SELECT 1
                        FROM `COURSE_CLASS_STUDENT` cs
                        JOIN `COURSE_CLASS` c ON c.`ID` = cs.`CLASS_ID`
                        WHERE cs.`STUDENT_ID` = s.`ID`
                          AND cs.`DELETE_FLAG` = 'NOT_DELETE'
                          AND c.`DELETE_FLAG` = 'NOT_DELETE'
                          AND c.`CAMPUS_ID` IS NOT NULL
                      )
                    """);
            int studentCampusDefaulted = jdbcTemplate.update("""
                    UPDATE `STUDENT` s
                    SET s.`CAMPUS_ID` = (
                        SELECT o.`ID`
                        FROM `SYS_ORG` o
                        WHERE o.`TENANT_ID` = s.`TENANT_ID`
                          AND o.`CATEGORY` = 'DEPT'
                          AND o.`DELETE_FLAG` = 'NOT_DELETE'
                        ORDER BY o.`SORT_CODE`, o.`ID`
                        LIMIT 1
                    )
                    WHERE s.`CAMPUS_ID` IS NULL
                      AND s.`DELETE_FLAG` = 'NOT_DELETE'
                      AND (
                        SELECT COUNT(*) FROM `SYS_ORG` o
                        WHERE o.`TENANT_ID` = s.`TENANT_ID`
                          AND o.`CATEGORY` = 'DEPT'
                          AND o.`DELETE_FLAG` = 'NOT_DELETE'
                      ) = 1
                    """);
            if (studentCampusFilled + studentCampusDefaulted > 0) {
                log.info(">>> 报表结构升级：回填学员校区 {} 条（班级 {} 条，默认校区 {} 条）",
                        studentCampusFilled + studentCampusDefaulted, studentCampusFilled, studentCampusDefaulted);
            }
            // 历史订单按学员当前校区回填，避免按校区筛选时缺失
            jdbcTemplate.update("UPDATE `STUDENT_ORDER` o JOIN `STUDENT` s ON o.`STUDENT_ID` = s.`ID` SET o.`CAMPUS_ID` = s.`CAMPUS_ID` WHERE o.`CAMPUS_ID` IS NULL");
            // 单校区机构的历史日汇总可直接落校区；多校区需按课程/班级人工重算，避免串校区
            int dailyStatCampusFilled = jdbcTemplate.update("""
                    UPDATE `FIN_DAILY_STAT` f
                    JOIN (
                        SELECT `TENANT_ID`, MIN(`CAMPUS_ID`) AS `CAMPUS_ID`
                        FROM `STUDENT`
                        WHERE `CAMPUS_ID` IS NOT NULL AND `DELETE_FLAG` = 'NOT_DELETE'
                        GROUP BY `TENANT_ID`
                        HAVING COUNT(DISTINCT `CAMPUS_ID`) = 1
                    ) t ON t.`TENANT_ID` = f.`TENANT_ID`
                    SET f.`CAMPUS_ID` = t.`CAMPUS_ID`
                    WHERE f.`CAMPUS_ID` IS NULL AND f.`DELETE_FLAG` = 'NOT_DELETE'
                    """);
            if (dailyStatCampusFilled > 0) {
                log.info(">>> 报表结构升级：回填单校区日汇总 {} 条", dailyStatCampusFilled);
            }
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

            if (!tableExists("FIN_DAILY_STAT")) {
                jdbcTemplate.execute(FIN_DAILY_STAT_DDL);
                log.info(">>> 报表结构升级：FIN_DAILY_STAT 已创建");
            }
            if (!columnExists("FIN_DAILY_STAT", "RECOGNIZED_AMOUNT")) {
                jdbcTemplate.execute("ALTER TABLE `FIN_DAILY_STAT` ADD COLUMN `RECOGNIZED_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '课消金额(权责口径)' AFTER `NET_LESSONS`");
                log.info(">>> 报表结构升级：FIN_DAILY_STAT.RECOGNIZED_AMOUNT 已添加");
            }

            // 课包维度扣课规则
            if (!columnExists("COURSE_PACKAGE", "LESSONS_PER_CLASS")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_PACKAGE` ADD COLUMN `LESSONS_PER_CLASS` decimal(10,2) NULL DEFAULT NULL COMMENT '每课扣课时数(为空沿用课程配置)' AFTER `PRICE`");
                jdbcTemplate.execute("ALTER TABLE `COURSE_PACKAGE` ADD COLUMN `LEAVE_DEDUCT` tinyint NULL DEFAULT NULL COMMENT '请假是否扣课(为空沿用课程配置)' AFTER `LESSONS_PER_CLASS`");
                jdbcTemplate.execute("ALTER TABLE `COURSE_PACKAGE` ADD COLUMN `ABSENT_DEDUCT` tinyint NULL DEFAULT NULL COMMENT '缺勤是否扣课(为空沿用课程配置)' AFTER `LEAVE_DEDUCT`");
                log.info(">>> 课务结构升级：COURSE_PACKAGE 扣课规则列已添加");
            }
            // 点名记录备注（未消课时记录原因）
            if (!columnExists("COURSE_CLASS_ATTEND", "REMARK")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_ATTEND` ADD COLUMN `REMARK` varchar(200) NULL DEFAULT NULL COMMENT '备注(未消课原因等)' AFTER `LESSONS_DEDUCTED`");
                log.info(">>> 课务结构升级：COURSE_CLASS_ATTEND.REMARK 已添加");
            }

            // 逻辑删除 + 唯一索引冲突修复（点名记录 / 班级学员）
            if (!columnExists("COURSE_CLASS_ATTEND", "ACTIVE_FLAG")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_ATTEND` ADD COLUMN `ACTIVE_FLAG` tinyint GENERATED ALWAYS AS (IF(`DELETE_FLAG` = 'NOT_DELETE', 1, NULL)) STORED COMMENT '有效记录标记(仅未删除参与唯一约束)'");
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_ATTEND` DROP INDEX `UK_ATTEND`");
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_ATTEND` ADD UNIQUE INDEX `UK_ATTEND_ACTIVE` (`SCHEDULE_ID`, `STUDENT_ID`, `ACTIVE_FLAG`)");
                log.info(">>> 课务结构升级：COURSE_CLASS_ATTEND 唯一索引已改为 ACTIVE_FLAG 方案");
            }
            if (!columnExists("COURSE_CLASS_STUDENT", "ACTIVE_FLAG")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_STUDENT` ADD COLUMN `ACTIVE_FLAG` tinyint GENERATED ALWAYS AS (IF(`DELETE_FLAG` = 'NOT_DELETE', 1, NULL)) STORED COMMENT '有效记录标记(仅未删除参与唯一约束)'");
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_STUDENT` DROP INDEX `UK_CLASS_STUDENT`");
                jdbcTemplate.execute("ALTER TABLE `COURSE_CLASS_STUDENT` ADD UNIQUE INDEX `UK_CLASS_STUDENT_ACTIVE` (`CLASS_ID`, `STUDENT_ID`, `ACTIVE_FLAG`)");
                log.info(">>> 课务结构升级：COURSE_CLASS_STUDENT 唯一索引已改为 ACTIVE_FLAG 方案");
            }

            // 排课解绑班级：新增课程/校区列 + 排课学员表（老师带一群学生、无固定班级时使用）
            if (!columnExists("COURSE_SCHEDULE", "COURSE_ID")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD COLUMN `COURSE_ID` varchar(20) NULL DEFAULT NULL COMMENT '课程id(消课按此课程扣减课时账户)' AFTER `CLASS_ID`");
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD COLUMN `CAMPUS_ID` varchar(20) NULL DEFAULT NULL COMMENT '校区id(直接排课时按老师所属校区)' AFTER `COURSE_ID`");
                log.info(">>> 课务结构升级：COURSE_SCHEDULE 课程/校区列已添加");
            }
            if (!tableExists("COURSE_SCHEDULE_STUDENT")) {
                jdbcTemplate.execute(SCHEDULE_STUDENT_DDL);
                log.info(">>> 课务结构升级：COURSE_SCHEDULE_STUDENT 排课学员表已创建");
            }
            if (!tableExists("COURSE_SCHEDULE_CLOSURE")) {
                jdbcTemplate.execute(SCHEDULE_CLOSURE_DDL);
                log.info(">>> 课务结构升级：COURSE_SCHEDULE_CLOSURE 停课日表已创建");
            }
            if (!tableExists("COURSE_TEACHER_UNAVAILABLE")) {
                jdbcTemplate.execute(TEACHER_UNAVAILABLE_DDL);
                log.info(">>> 课务结构升级：COURSE_TEACHER_UNAVAILABLE 教师不可用时段表已创建");
            }

            // 排课并发唯一约束：同一租户、同一班级、同一天同一开始时间只允许一节正常课
            // 用生成列 + 唯一索引，已取消/已调的课节不参与（生成列为 NULL 时唯一索引不生效）
            if (!columnExists("COURSE_SCHEDULE", "ACTIVE_SLOT_KEY")) {
                int canceled = jdbcTemplate.update("""
                        UPDATE `COURSE_SCHEDULE` s
                        JOIN (
                            SELECT `CLASS_ID`, `SCHEDULE_DATE`, `START_TIME`, MIN(`ID`) AS `KEEP_ID`
                            FROM `COURSE_SCHEDULE`
                            WHERE `STATUS` = 'NORMAL' AND `CLASS_ID` IS NOT NULL
                            GROUP BY `CLASS_ID`, `SCHEDULE_DATE`, `START_TIME`
                            HAVING COUNT(*) > 1
                        ) d ON s.`CLASS_ID` = d.`CLASS_ID`
                            AND s.`SCHEDULE_DATE` = d.`SCHEDULE_DATE`
                            AND s.`START_TIME` = d.`START_TIME`
                        SET s.`STATUS` = 'CANCELED'
                        WHERE s.`STATUS` = 'NORMAL' AND s.`ID` <> d.`KEEP_ID`
                        """);
                if (canceled > 0) {
                    log.warn(">>> 课务结构升级：清理历史重复排课 {} 条（保留每组最早一条，其余置为已取消）", canceled);
                }
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD COLUMN `ACTIVE_SLOT_KEY` varchar(64) "
                        + "GENERATED ALWAYS AS (CASE WHEN `STATUS` = 'NORMAL' AND `CLASS_ID` IS NOT NULL "
                        + "THEN CONCAT(`CLASS_ID`, '|', DATE_FORMAT(`SCHEDULE_DATE`, '%Y-%m-%d'), '|', "
                        + "TIME_FORMAT(`START_TIME`, '%H:%i')) ELSE NULL END) STORED "
                        + "COMMENT '有效排课槽位(仅正常课节参与唯一约束)'");
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD UNIQUE INDEX `UK_SCHEDULE_ACTIVE_SLOT` "
                        + "(`TENANT_ID`, `ACTIVE_SLOT_KEY`)");
                log.info(">>> 课务结构升级：COURSE_SCHEDULE 班级时段唯一索引已添加");
            }

            // 教师/教室并发唯一约束：同一租户下同一时段老师和教室也只能有一节课
            if (!columnExists("COURSE_SCHEDULE", "ACTIVE_TEACHER_KEY")) {
                int canceled = jdbcTemplate.update("""
                        UPDATE `COURSE_SCHEDULE` s
                        JOIN (
                            SELECT `TEACHER_ID`, `SCHEDULE_DATE`, `START_TIME`, MIN(`ID`) AS `KEEP_ID`
                            FROM `COURSE_SCHEDULE`
                            WHERE `STATUS` = 'NORMAL' AND `TEACHER_ID` IS NOT NULL
                            GROUP BY `TEACHER_ID`, `SCHEDULE_DATE`, `START_TIME`
                            HAVING COUNT(*) > 1
                        ) d ON s.`TEACHER_ID` = d.`TEACHER_ID`
                            AND s.`SCHEDULE_DATE` = d.`SCHEDULE_DATE`
                            AND s.`START_TIME` = d.`START_TIME`
                        SET s.`STATUS` = 'CANCELED'
                        WHERE s.`STATUS` = 'NORMAL' AND s.`ID` <> d.`KEEP_ID`
                        """);
                if (canceled > 0) {
                    log.warn(">>> 课务结构升级：清理同教师同时段重复排课 {} 条", canceled);
                }
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD COLUMN `ACTIVE_TEACHER_KEY` varchar(64) "
                        + "GENERATED ALWAYS AS (CASE WHEN `STATUS` = 'NORMAL' AND `TEACHER_ID` IS NOT NULL "
                        + "THEN CONCAT(`TEACHER_ID`, '|', DATE_FORMAT(`SCHEDULE_DATE`, '%Y-%m-%d'), '|', "
                        + "TIME_FORMAT(`START_TIME`, '%H:%i')) ELSE NULL END) STORED "
                        + "COMMENT '教师时段唯一键(仅正常课节参与)'");
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD UNIQUE INDEX `UK_SCHEDULE_ACTIVE_TEACHER` "
                        + "(`TENANT_ID`, `ACTIVE_TEACHER_KEY`)");
                log.info(">>> 课务结构升级：COURSE_SCHEDULE 教师时段唯一索引已添加");
            }
            if (!columnExists("COURSE_SCHEDULE", "ACTIVE_ROOM_KEY")) {
                int canceled = jdbcTemplate.update("""
                        UPDATE `COURSE_SCHEDULE` s
                        JOIN (
                            SELECT UPPER(TRIM(`CLASSROOM`)) AS `ROOM`, `SCHEDULE_DATE`, `START_TIME`, MIN(`ID`) AS `KEEP_ID`
                            FROM `COURSE_SCHEDULE`
                            WHERE `STATUS` = 'NORMAL' AND `CLASSROOM` IS NOT NULL AND TRIM(`CLASSROOM`) <> ''
                            GROUP BY UPPER(TRIM(`CLASSROOM`)), `SCHEDULE_DATE`, `START_TIME`
                            HAVING COUNT(*) > 1
                        ) d ON UPPER(TRIM(s.`CLASSROOM`)) = d.`ROOM`
                            AND s.`SCHEDULE_DATE` = d.`SCHEDULE_DATE`
                            AND s.`START_TIME` = d.`START_TIME`
                        SET s.`STATUS` = 'CANCELED'
                        WHERE s.`STATUS` = 'NORMAL' AND s.`ID` <> d.`KEEP_ID`
                        """);
                if (canceled > 0) {
                    log.warn(">>> 课务结构升级：清理同教室同时段重复排课 {} 条", canceled);
                }
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD COLUMN `ACTIVE_ROOM_KEY` varchar(80) "
                        + "GENERATED ALWAYS AS (CASE WHEN `STATUS` = 'NORMAL' AND `CLASSROOM` IS NOT NULL "
                        + "AND TRIM(`CLASSROOM`) <> '' THEN CONCAT(UPPER(TRIM(`CLASSROOM`)), '|', "
                        + "DATE_FORMAT(`SCHEDULE_DATE`, '%Y-%m-%d'), '|', TIME_FORMAT(`START_TIME`, '%H:%i')) "
                        + "ELSE NULL END) STORED COMMENT '教室时段唯一键(大小写不敏感，仅正常课节参与)'");
                jdbcTemplate.execute("ALTER TABLE `COURSE_SCHEDULE` ADD UNIQUE INDEX `UK_SCHEDULE_ACTIVE_ROOM` "
                        + "(`TENANT_ID`, `ACTIVE_ROOM_KEY`)");
                log.info(">>> 课务结构升级：COURSE_SCHEDULE 教室时段唯一索引已添加");
            }

            // 业务埋点表
            if (!tableExists("BIZ_EVENT")) {
                jdbcTemplate.execute(BIZ_EVENT_DDL);
                log.info(">>> 平台结构升级：BIZ_EVENT 业务埋点表已创建");
            }
            if (!tableExists("BIZ_EXPORT_TASK")) {
                jdbcTemplate.execute(BIZ_EXPORT_TASK_DDL);
                log.info(">>> 平台结构升级：BIZ_EXPORT_TASK 异步导出任务表已创建");
            }

            // 定时任务归属租户
            if (!columnExists("DEV_JOB", "TENANT_ID")) {
                jdbcTemplate.execute("ALTER TABLE `DEV_JOB` ADD COLUMN `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '归属租户id(平台任务为0)' AFTER `SORT_CODE`");
                jdbcTemplate.update("UPDATE `DEV_JOB` SET `TENANT_ID` = '0' WHERE `TENANT_ID` IS NULL");
                log.info(">>> 平台结构升级：DEV_JOB.TENANT_ID 已添加");
            }

            if (!tableExists("T_TENANT_LOGIN")) {
                jdbcTemplate.execute(TENANT_LOGIN_DDL);
                log.info(">>> 平台结构升级：T_TENANT_LOGIN 已创建");
            }

            if (!tableExists("T_TENANT_RENEW")) {
                jdbcTemplate.execute(TENANT_RENEW_DDL);
                log.info(">>> 平台结构升级：T_TENANT_RENEW 已创建");
            }

            if (!tableExists("FIN_EXPORT_TASK")) {
                jdbcTemplate.execute(FIN_EXPORT_TASK_DDL);
                log.info(">>> 报表结构升级：FIN_EXPORT_TASK 已创建");
            }

            // 课程扣课规则可配置（请假/缺勤是否扣课）
            if (!columnExists("COURSE", "LEAVE_DEDUCT")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE` ADD COLUMN `LEAVE_DEDUCT` tinyint NULL DEFAULT 0 COMMENT '请假是否扣课时' AFTER `LESSONS_PER_CLASS`");
                log.info(">>> 课务结构升级：COURSE.LEAVE_DEDUCT 已添加");
            }
            if (!columnExists("COURSE", "ABSENT_DEDUCT")) {
                jdbcTemplate.execute("ALTER TABLE `COURSE` ADD COLUMN `ABSENT_DEDUCT` tinyint NULL DEFAULT 1 COMMENT '缺勤是否扣课时' AFTER `LEAVE_DEDUCT`");
                log.info(">>> 课务结构升级：COURSE.ABSENT_DEDUCT 已添加");
            }

            // 同一学员同课包只允许一张待支付订单（生成列 + 唯一索引兜底）
            if (!columnExists("STUDENT_ORDER", "UNPAID_FLAG")) {
                int duplicated = jdbcTemplate.update(
                        "UPDATE `STUDENT_ORDER` o1 JOIN `STUDENT_ORDER` o2 "
                                + "ON o1.`TENANT_ID` = o2.`TENANT_ID` AND o1.`STUDENT_ID` = o2.`STUDENT_ID` "
                                + "AND o1.`COURSE_PACKAGE_ID` = o2.`COURSE_PACKAGE_ID` "
                                + "AND o1.`STATUS` = 'UNPAID' AND o2.`STATUS` = 'UNPAID' "
                                + "AND (o1.`CREATE_TIME` > o2.`CREATE_TIME` "
                                + "OR (o1.`CREATE_TIME` = o2.`CREATE_TIME` AND o1.`ID` > o2.`ID`)) "
                                + "SET o1.`STATUS` = 'CANCELED'");
                if (duplicated > 0) {
                    log.info(">>> 账务结构升级：已取消 {} 条重复的待支付订单", duplicated);
                }
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD COLUMN `UNPAID_FLAG` tinyint "
                        + "GENERATED ALWAYS AS (CASE WHEN `STATUS` = 'UNPAID' THEN 1 ELSE NULL END) STORED");
                jdbcTemplate.execute("ALTER TABLE `STUDENT_ORDER` ADD UNIQUE INDEX `UK_ORDER_UNPAID` "
                        + "(`TENANT_ID`, `STUDENT_ID`, `COURSE_PACKAGE_ID`, `UNPAID_FLAG`)");
                log.info(">>> 账务结构升级：STUDENT_ORDER.UK_ORDER_UNPAID 已添加");
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