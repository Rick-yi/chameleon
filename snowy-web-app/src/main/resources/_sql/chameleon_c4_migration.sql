-- ============================================================================
-- chameleon C4 迁移：数据治理与权限审计
--   1. SYS_AUTH_AUDIT     权限变更审计表
--   2. 埋点归档定时任务   每天清理保留期（90 天）之前的埋点流水
--   3. 「权限审计」菜单   系统管理下，仅超管可见
-- 幂等：可重复执行
-- ============================================================================

-- 1) 权限变更审计表
CREATE TABLE IF NOT EXISTS `SYS_AUTH_AUDIT` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) DEFAULT NULL COMMENT '租户id',
  `OPERATOR_ID` varchar(20) DEFAULT NULL COMMENT '操作人id',
  `OPERATOR_NAME` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `ACTION` varchar(30) DEFAULT NULL COMMENT '动作(ROLE_RESOURCE/ROLE_PERMISSION/USER_ROLE)',
  `TARGET_TYPE` varchar(20) DEFAULT NULL COMMENT '对象类型(ROLE/USER)',
  `TARGET_ID` varchar(20) DEFAULT NULL COMMENT '对象id',
  `TARGET_NAME` varchar(120) DEFAULT NULL COMMENT '对象名称',
  `SUMMARY` varchar(500) DEFAULT NULL COMMENT '变更摘要',
  `DETAIL` longtext COMMENT '变更明细(JSON)',
  `DELETE_FLAG` varchar(255) DEFAULT 'NOT_DELETE' COMMENT '删除标志',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) DEFAULT NULL COMMENT '创建人',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `UPDATE_USER` varchar(20) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTH_AUDIT_TENANT_TIME` (`TENANT_ID`, `CREATE_TIME`),
  KEY `IDX_AUTH_AUDIT_TARGET` (`TARGET_TYPE`, `TARGET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限变更审计';

-- 2) 埋点归档定时任务（每天 03:30）
INSERT IGNORE INTO `DEV_JOB` (`ID`, `NAME`, `CODE`, `CATEGORY`, `ACTION_CLASS`, `CRON_EXPRESSION`, `JOB_STATUS`, `SORT_CODE`, `TENANT_ID`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('job_event_arch_01', '埋点流水归档', 'eventArchiveJob', 'BIZ',
 'vip.xiaonuo.platform.modular.event.timer.BizEventArchiveTimerTaskRunner', '0 30 3 * * ?', 'RUNNING', 3, '0', 'NOT_DELETE', NOW(), '-1');

-- 3) 「权限审计」菜单（挂在系统管理模块下）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_auth_audit_001', '0', '权限审计', 'sysAuthAudit', 'authAudit', 'MENU', '1548901111999773976', 'MENU', '/sys/authAudit', 'sys/authAudit/index', 'safety-certificate-outlined', NULL, 'YES', 'YES', 'YES', 90, 'NOT_DELETE', NOW(), '1');

-- 4) 授权给各租户超级管理员（菜单）
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rel_auth_audit_', LEFT(MD5(CONCAT(r.`ID`, 'menu_auth_audit_001')), 16)),
       r.`TENANT_ID`, r.`ID`, 'menu_auth_audit_001', 'SYS_ROLE_HAS_RESOURCE',
       JSON_OBJECT('menuId', 'menu_auth_audit_001', 'buttonInfo', JSON_ARRAY())
FROM `SYS_ROLE` r
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = 'menu_auth_audit_001' AND s.`CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  );
