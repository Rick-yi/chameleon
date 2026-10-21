-- ============================================================================
-- chameleon 自动化中心：规则配置表、执行记录表、菜单与定时任务
--   内容：
--     1. BIZ_AUTOMATION_CONFIG  租户级自动化规则开关与参数
--     2. BIZ_AUTOMATION_LOG     自动化执行记录
--     3. 「自动化中心」菜单（挂到「教务运营」模块下）
--     4. 自动化接口权限授予各租户超级管理员
--     5. 自动化规则定时任务（每 10 分钟）
--   执行方式：mysql -uroot -p chameleon < chameleon_automation.sql
--   幂等：可重复执行
-- ============================================================================

-- 1) 规则配置表
CREATE TABLE IF NOT EXISTS `BIZ_AUTOMATION_CONFIG` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) DEFAULT NULL COMMENT '租户id',
  `RULE_CODE` varchar(64) NOT NULL COMMENT '规则编码',
  `ENABLED` varchar(10) DEFAULT 'NO' COMMENT '是否开启(YES/NO)',
  `PARAMS` longtext COMMENT '规则参数(JSON)',
  `REMARK` varchar(255) DEFAULT NULL COMMENT '备注',
  `DELETE_FLAG` varchar(255) DEFAULT 'NOT_DELETE' COMMENT '删除标志',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) DEFAULT NULL COMMENT '创建人',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `UPDATE_USER` varchar(20) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTO_TENANT_RULE` (`TENANT_ID`, `RULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化规则配置';

-- 2) 执行记录表
CREATE TABLE IF NOT EXISTS `BIZ_AUTOMATION_LOG` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) DEFAULT NULL COMMENT '租户id',
  `RULE_CODE` varchar(64) DEFAULT NULL COMMENT '规则编码',
  `RULE_NAME` varchar(255) DEFAULT NULL COMMENT '规则名称',
  `TRIGGER_TYPE` varchar(20) DEFAULT NULL COMMENT '触发方式(TIMER定时/MANUAL手动/TRIGGER实时)',
  `SUCCESS` varchar(10) DEFAULT 'YES' COMMENT '是否成功(YES/NO)',
  `MESSAGE` varchar(500) DEFAULT NULL COMMENT '结果描述',
  `AFFECTED_COUNT` int DEFAULT 0 COMMENT '影响条数',
  `DETAIL` longtext COMMENT '明细(JSON)',
  `COST_MS` bigint DEFAULT 0 COMMENT '耗时(毫秒)',
  `DELETE_FLAG` varchar(255) DEFAULT 'NOT_DELETE' COMMENT '删除标志',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) DEFAULT NULL COMMENT '创建人',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `UPDATE_USER` varchar(20) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTO_LOG_TENANT_RULE` (`TENANT_ID`,`RULE_CODE`),
  KEY `IDX_AUTO_LOG_TIME` (`CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化执行记录';

-- 3) 菜单：自动化中心（挂到「教务运营」模块下）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_automation_001', '0', '自动化中心', 'bizAutomation', 'automation', 'MENU', 'mod_edu_001', 'MENU', '/automation', 'automation/index', 'thunderbolt-outlined', NULL, 'YES', 'YES', 'YES', 12, 'NOT_DELETE', NOW(), '1');

-- 4) 接口权限授予各租户超级管理员
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rel_auto_', LEFT(MD5(CONCAT(r.`ID`, p.`api_url`)), 16)),
       r.`TENANT_ID`, r.`ID`, p.`api_url`, 'SYS_ROLE_HAS_PERMISSION',
       JSON_OBJECT('apiUrl', p.`api_url`, 'scopeCategory', 'SCOPE_ALL', 'scopeDefineOrgIdList', JSON_ARRAY())
FROM `SYS_ROLE` r
JOIN (
    SELECT '/biz/automation/rule'   AS `api_url`
    UNION ALL SELECT '/biz/automation/toggle'
    UNION ALL SELECT '/biz/automation/run'
    UNION ALL SELECT '/biz/automation/log'
) p
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = p.`api_url` AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
  );

-- 4.1) 菜单授权给各租户超级管理员
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rel_auto_menu_', LEFT(MD5(CONCAT(r.`ID`, 'menu_automation_001')), 16)),
       r.`TENANT_ID`, r.`ID`, 'menu_automation_001', 'SYS_ROLE_HAS_RESOURCE',
       JSON_OBJECT('menuId', 'menu_automation_001', 'buttonInfo', JSON_ARRAY())
FROM `SYS_ROLE` r
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = 'menu_automation_001' AND s.`CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  );

-- 5) 定时任务：每 10 分钟执行一次自动化规则
INSERT IGNORE INTO `DEV_JOB` (`ID`, `NAME`, `CODE`, `CATEGORY`, `ACTION_CLASS`, `CRON_EXPRESSION`, `JOB_STATUS`, `SORT_CODE`, `TENANT_ID`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('job_auto_001', '自动化规则执行', 'autoRuleJob', 'BIZ', 'vip.xiaonuo.biz.modular.automation.timer.BizAutomationTimerTaskRunner', '0 0/10 * * * ?', 'RUNNING', 2, '0', 'NOT_DELETE', NOW(), '-1');
