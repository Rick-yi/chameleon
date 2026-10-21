-- ============================================================================
-- chameleon C3 迁移：自动化中心需要的字段补充
--   内容：STUDENT_ORDER 增加 LAST_NOTIFY_TIME（未支付订单提醒的最近提醒时间）
--   说明：执行一次即可；重复执行会报「Duplicate column」，可忽略
-- ============================================================================

ALTER TABLE `STUDENT_ORDER` ADD COLUMN `LAST_NOTIFY_TIME` datetime NULL DEFAULT NULL COMMENT '最近提醒时间(自动化：未支付订单提醒)';
