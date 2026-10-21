-- ============================================================
-- chameleon C2 升级脚本（执行前请备份）
-- 内容：修复「逻辑删除 + 唯一索引」冲突
--   1. COURSE_CLASS_ATTEND：撤销点名后再次点名/再次撤销会报唯一键冲突
--   2. COURSE_CLASS_STUDENT：退班后再次入班/再次退班会报唯一键冲突
--
-- 原因：唯一索引里带了 DELETE_FLAG，而逻辑删除把 DELETE_FLAG 置为 'DELETED'，
--       同一业务键第二次删除就会与历史 'DELETED' 行冲突（Duplicate entry）。
-- 方案：与 STUDENT_ORDER 的 UNPAID_FLAG 同一套路——用生成列只在「未删除」时参与唯一约束，
--       已删除行生成列为 NULL，MySQL 唯一索引忽略 NULL，因此不再冲突。
-- 执行方式：mysql -uroot -p chameleon < chameleon_c2_migration.sql
-- 说明：应用启动时 P0SchemaMigration 也会做同样的幂等检查。
-- ============================================================

-- 1. 点名记录
ALTER TABLE `COURSE_CLASS_ATTEND`
  ADD COLUMN `ACTIVE_FLAG` tinyint GENERATED ALWAYS AS (IF(`DELETE_FLAG` = 'NOT_DELETE', 1, NULL)) STORED COMMENT '有效记录标记(仅未删除参与唯一约束)';

ALTER TABLE `COURSE_CLASS_ATTEND` DROP INDEX `UK_ATTEND`;

ALTER TABLE `COURSE_CLASS_ATTEND`
  ADD UNIQUE INDEX `UK_ATTEND_ACTIVE` (`SCHEDULE_ID`, `STUDENT_ID`, `ACTIVE_FLAG`);

-- 2. 班级学员
ALTER TABLE `COURSE_CLASS_STUDENT`
  ADD COLUMN `ACTIVE_FLAG` tinyint GENERATED ALWAYS AS (IF(`DELETE_FLAG` = 'NOT_DELETE', 1, NULL)) STORED COMMENT '有效记录标记(仅未删除参与唯一约束)';

ALTER TABLE `COURSE_CLASS_STUDENT` DROP INDEX `UK_CLASS_STUDENT`;

ALTER TABLE `COURSE_CLASS_STUDENT`
  ADD UNIQUE INDEX `UK_CLASS_STUDENT_ACTIVE` (`CLASS_ID`, `STUDENT_ID`, `ACTIVE_FLAG`);
