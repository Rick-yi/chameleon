-- ============================================================
-- chameleon Finance：财务域支撑字段（订单补充课程/支付时间）
-- 执行方式：mysql -uroot -p chameleon < chameleon_finance.sql
-- 说明：本脚本为一次性升级脚本（ALTER 不可重复执行），执行前请备份。
-- ============================================================

-- 订单补充：课程id（下单时快照，用于按课程统计）
ALTER TABLE `STUDENT_ORDER` ADD COLUMN `COURSE_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '课程id(快照)' AFTER `COURSE_PACKAGE_ID`;

-- 订单补充：支付时间（用于财务报表按支付时间统计）
ALTER TABLE `STUDENT_ORDER` ADD COLUMN `PAY_TIME` datetime NULL DEFAULT NULL COMMENT '支付时间' AFTER `TRADE_NO`;

-- 回填：已支付/已退费订单的支付时间（更新时间可能为空，回落到创建时间）
UPDATE `STUDENT_ORDER` SET `PAY_TIME` = COALESCE(`UPDATE_TIME`, `CREATE_TIME`) WHERE `STATUS` IN ('PAID', 'REFUNDED') AND `PAY_TIME` IS NULL;

-- 回填：通过课包关联回填课程id
UPDATE `STUDENT_ORDER` o
  JOIN `COURSE_PACKAGE` p ON o.`COURSE_PACKAGE_ID` = p.`ID`
SET o.`COURSE_ID` = p.`COURSE_ID`
WHERE o.`COURSE_ID` IS NULL;
