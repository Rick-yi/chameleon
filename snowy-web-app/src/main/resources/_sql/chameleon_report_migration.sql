-- ============================================================
-- chameleon R1 报表升级脚本（一次性执行，执行前请备份）
-- 内容：
--   1. 订单校区快照
--   2. 退费完成时间
--   3. 报表查询索引
-- 执行方式：mysql -uroot -p chameleon < chameleon_report_migration.sql
-- 说明：应用启动时 P0SchemaMigration 也会做同样的幂等检查。
-- ============================================================

ALTER TABLE `STUDENT_ORDER` ADD COLUMN `CAMPUS_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '校区id(下单快照)' AFTER `COURSE_ID`;
UPDATE `STUDENT_ORDER` o JOIN `STUDENT` s ON o.`STUDENT_ID` = s.`ID` SET o.`CAMPUS_ID` = s.`CAMPUS_ID` WHERE o.`CAMPUS_ID` IS NULL;
ALTER TABLE `STUDENT_ORDER` ADD INDEX `IDX_ORDER_TENANT_PAYTIME` (`TENANT_ID`, `PAY_TIME`, `STATUS`);
ALTER TABLE `STUDENT_ORDER` ADD INDEX `IDX_ORDER_TENANT_CAMPUS` (`TENANT_ID`, `CAMPUS_ID`, `PAY_TIME`);

ALTER TABLE `STUDENT_REFUND` ADD COLUMN `REFUND_TIME` datetime NULL DEFAULT NULL COMMENT '退款完成时间' AFTER `APPROVE_TIME`;
UPDATE `STUDENT_REFUND` SET `REFUND_TIME` = COALESCE(`UPDATE_TIME`, `APPROVE_TIME`, `CREATE_TIME`) WHERE `STATUS` = 'REFUNDED' AND `REFUND_TIME` IS NULL;
ALTER TABLE `STUDENT_REFUND` ADD INDEX `IDX_REFUND_TENANT_REFUNDTIME` (`TENANT_ID`, `STATUS`, `REFUND_TIME`);
