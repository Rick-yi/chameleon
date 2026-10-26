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
  `DAILY_CAP` int DEFAULT 0 COMMENT '每日执行上限(0不限)',
  `HOLDOUT_PERCENT` int DEFAULT 0 COMMENT 'holdout比例(0-100)',
  `VERSION` int DEFAULT 1 COMMENT '配置版本',
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
  `RULE_VERSION` int DEFAULT 1 COMMENT '规则版本',
  `INPUT_HASH` varchar(64) DEFAULT NULL COMMENT '输入哈希',
  `TARGET_COUNT` int DEFAULT 0 COMMENT '目标总数',
  `SUCCESS_COUNT` int DEFAULT 0 COMMENT '成功数',
  `FAIL_COUNT` int DEFAULT 0 COMMENT '失败数',
  `SKIP_COUNT` int DEFAULT 0 COMMENT '跳过数',
  `DELIVERY_STATUS` varchar(20) DEFAULT NULL COMMENT '触达状态',
  `DELIVERY_COUNT` int DEFAULT 0 COMMENT '触达条数',
  `MESSAGE_ID` varchar(20) DEFAULT NULL COMMENT '站内信id',
  `DRY_RUN` varchar(10) DEFAULT 'NO' COMMENT '是否预览',
  `EXPERIMENT_GROUP` varchar(20) DEFAULT 'NONE' COMMENT '实验分组',
  `ATTRIBUTION_STATUS` varchar(20) DEFAULT 'NA' COMMENT '归因状态',
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

-- 2.1) 租户级自动化设置
CREATE TABLE IF NOT EXISTS `BIZ_AUTOMATION_SETTING` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) DEFAULT NULL COMMENT '租户id',
  `PAUSED` varchar(10) DEFAULT 'NO' COMMENT '是否暂停(YES/NO)',
  `QUIET_START` varchar(5) DEFAULT NULL COMMENT '静默开始 HH:mm',
  `QUIET_END` varchar(5) DEFAULT NULL COMMENT '静默结束 HH:mm',
  `TIMEZONE` varchar(64) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `DELETE_FLAG` varchar(255) DEFAULT 'NOT_DELETE',
  `CREATE_TIME` datetime DEFAULT NULL,
  `CREATE_USER` varchar(20) DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `UPDATE_USER` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTO_SETTING_TENANT` (`TENANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化租户设置';

-- 2.2) 目标级执行记录（幂等、逐目标结果、实验归因）
CREATE TABLE IF NOT EXISTS `BIZ_AUTOMATION_TARGET` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) DEFAULT NULL COMMENT '租户id',
  `LOG_ID` varchar(20) DEFAULT NULL COMMENT '执行记录id',
  `RULE_CODE` varchar(64) NOT NULL COMMENT '规则编码',
  `TARGET_TYPE` varchar(30) DEFAULT NULL COMMENT '目标类型',
  `TARGET_ID` varchar(100) DEFAULT NULL COMMENT '目标id',
  `ACTION` varchar(30) DEFAULT NULL COMMENT '动作',
  `STATUS` varchar(20) DEFAULT NULL COMMENT '状态',
  `REASON` varchar(500) DEFAULT NULL COMMENT '失败或跳过原因',
  `IDEMPOTENCY_KEY` varchar(255) NOT NULL COMMENT '幂等键',
  `MESSAGE_ID` varchar(20) DEFAULT NULL COMMENT '站内信id',
  `EXPERIMENT_GROUP` varchar(20) DEFAULT 'NONE' COMMENT '实验分组',
  `OUTCOME_STATUS` varchar(20) DEFAULT 'NA' COMMENT '归因状态',
  `OUTCOME_TIME` datetime DEFAULT NULL COMMENT '归因时间',
  `OUTCOME_VALUE` decimal(12,2) DEFAULT 0.00 COMMENT '归因金额',
  `RETRY_COUNT` int DEFAULT 0 COMMENT '重试次数',
  `ACTION_TIME` datetime DEFAULT NULL COMMENT '动作时间',
  `DELETE_FLAG` varchar(255) DEFAULT 'NOT_DELETE',
  `CREATE_TIME` datetime DEFAULT NULL,
  `CREATE_USER` varchar(20) DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `UPDATE_USER` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_AUTO_TARGET_KEY` (`TENANT_ID`, `RULE_CODE`, `IDEMPOTENCY_KEY`),
  KEY `IDX_AUTO_TARGET_LOG` (`LOG_ID`),
  KEY `IDX_AUTO_TARGET_TENANT_RULE_TIME` (`TENANT_ID`, `RULE_CODE`, `CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化目标级执行记录';

-- 2.3) 配置变更历史
CREATE TABLE IF NOT EXISTS `BIZ_AUTOMATION_CONFIG_HISTORY` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) DEFAULT NULL COMMENT '租户id',
  `RULE_CODE` varchar(64) DEFAULT NULL COMMENT '规则编码',
  `VERSION` int DEFAULT 1 COMMENT '配置版本',
  `OLD_ENABLED` varchar(10) DEFAULT NULL,
  `NEW_ENABLED` varchar(10) DEFAULT NULL,
  `OLD_PARAMS` longtext,
  `NEW_PARAMS` longtext,
  `OPERATOR_ID` varchar(20) DEFAULT NULL,
  `DELETE_FLAG` varchar(255) DEFAULT 'NOT_DELETE',
  `CREATE_TIME` datetime DEFAULT NULL,
  `CREATE_USER` varchar(20) DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `UPDATE_USER` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTO_HISTORY_TENANT_RULE` (`TENANT_ID`, `RULE_CODE`, `CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化配置变更历史';

-- 3) 菜单：自动化中心（挂到「教务运营」模块下）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_automation_001', '0', '自动化中心', 'bizAutomation', 'automation', 'MENU', 'mod_edu_001', 'MENU', '/automation', 'automation/index', 'thunderbolt-outlined', NULL, 'YES', 'YES', 'YES', 12, 'NOT_DELETE', NOW(), '1');

-- 4) 接口权限授予各租户超级管理员
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('ra_', LEFT(MD5(CONCAT(r.`ID`, p.`api_url`)), 17)),
       r.`TENANT_ID`, r.`ID`, p.`api_url`, 'SYS_ROLE_HAS_PERMISSION',
       JSON_OBJECT('apiUrl', p.`api_url`, 'scopeCategory', 'SCOPE_ALL', 'scopeDefineOrgIdList', JSON_ARRAY())
FROM `SYS_ROLE` r
JOIN (
    SELECT '/biz/automation/rule'   AS `api_url`
    UNION ALL SELECT '/biz/automation/toggle'
    UNION ALL SELECT '/biz/automation/run'
    UNION ALL SELECT '/biz/automation/log'
    UNION ALL SELECT '/biz/automation/setting'
    UNION ALL SELECT '/biz/automation/experiment'
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
