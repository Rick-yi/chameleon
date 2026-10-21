-- ============================================================
-- chameleon 报表接口权限与按钮权限（可重复执行）
-- 内容：
--   1. 报表按钮资源（导出 / 查看明细），用于前端按按钮显示
--   2. 报表菜单授权给各租户超级管理员
--   3. 报表接口权限（@SaCheckPermission）授权给各租户超级管理员
-- 执行方式：mysql -uroot -p chameleon < chameleon_report_permission.sql
-- 前置：先执行 chameleon_menu.sql（报表菜单）与 chameleon_report_r2r4_migration.sql
-- ============================================================

-- 1. 报表按钮资源（CATEGORY=BUTTON，挂在报表菜单下）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('btn_rpt_exp_fin',   'menu_report_fin_001',    '导出',     'reportExport',        'reportExport',        'BUTTON', 'mod_report_001',   1, 'NOT_DELETE', NOW(), '1'),
('btn_rpt_det_fin',   'menu_report_fin_001',    '查看明细', 'reportDetail',        'reportDetail',        'BUTTON', 'mod_report_001',   2, 'NOT_DELETE', NOW(), '1'),
('btn_rpt_exp_acad',  'menu_report_acad_001',   '导出',     'reportAcademicExport','reportAcademicExport','BUTTON', 'mod_report_001',   3, 'NOT_DELETE', NOW(), '1'),
('btn_rpt_exp_crm',   'menu_report_crm_001',    '导出',     'reportCrmExport',     'reportCrmExport',     'BUTTON', 'mod_report_001',   4, 'NOT_DELETE', NOW(), '1'),
('btn_rpt_exp_plat',  'menu_plat_dash_001',     '导出',     'reportPlatformExport','reportPlatformExport','BUTTON', 'mod_platform_001', 5, 'NOT_DELETE', NOW(), '1');

-- 2. 报表菜单授权给各租户超级管理员（平台看板菜单仅平台运营可见，不在此授权）
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rm', LEFT(MD5(CONCAT(r.`ID`, m.`ID`)), 17)),
       r.`TENANT_ID`, r.`ID`, m.`ID`, 'SYS_ROLE_HAS_RESOURCE',
       JSON_OBJECT('menuId', m.`ID`, 'buttonInfo', JSON_ARRAY())
FROM `SYS_ROLE` r
JOIN `SYS_RESOURCE` m ON m.`ID` IN ('menu_report_dash_001', 'menu_report_fin_001', 'menu_report_acad_001', 'menu_report_crm_001')
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = m.`ID` AND s.`CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  );

-- 3. 报表接口权限授权给各租户超级管理员
INSERT INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rp', LEFT(MD5(CONCAT(r.`ID`, p.`URL`)), 17)),
       r.`TENANT_ID`, r.`ID`, p.`URL`, 'SYS_ROLE_HAS_PERMISSION',
       JSON_OBJECT('apiUrl', p.`URL`, 'scopeCategory', 'SCOPE_ALL', 'scopeDefineOrgIdList', JSON_ARRAY())
FROM `SYS_ROLE` r
JOIN (
    SELECT '/finance/payment/page' AS `URL` UNION ALL
    SELECT '/finance/refund/page' UNION ALL
    SELECT '/finance/stat' UNION ALL
    SELECT '/finance/report/summary' UNION ALL
    SELECT '/finance/report/trend' UNION ALL
    SELECT '/finance/report/channel' UNION ALL
    SELECT '/finance/report/courseRank' UNION ALL
    SELECT '/finance/report/campusRank' UNION ALL
    SELECT '/finance/report/attendTrend' UNION ALL
    SELECT '/finance/report/campusOptions' UNION ALL
    SELECT '/finance/report/export' UNION ALL
    SELECT '/finance/report/dashboard' UNION ALL
    SELECT '/finance/report/dailyStat' UNION ALL
    SELECT '/finance/report/dailyStat/rebuild' UNION ALL
    SELECT '/finance/report/reconcile' UNION ALL
    SELECT '/finance/report/consume' UNION ALL
    SELECT '/finance/report/export/async' UNION ALL
    SELECT '/finance/report/export/async/status' UNION ALL
    SELECT '/finance/report/export/async/download' UNION ALL
    SELECT '/finance/report/cache/clear' UNION ALL
    SELECT '/course/report/academic/attendance' UNION ALL
    SELECT '/course/report/academic/classFill' UNION ALL
    SELECT '/course/report/academic/teacherWorkload' UNION ALL
    SELECT '/course/report/academic/lessonProgress' UNION ALL
    SELECT '/course/report/academic/riskStudents' UNION ALL
    SELECT '/course/report/academic/export' UNION ALL
    SELECT '/crm/report/funnel' UNION ALL
    SELECT '/crm/report/source' UNION ALL
    SELECT '/crm/report/followRank' UNION ALL
    SELECT '/crm/report/trial' UNION ALL
    SELECT '/crm/report/export'
) p
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = p.`URL` AND s.`CATEGORY` = 'SYS_ROLE_HAS_PERMISSION'
  );

-- 4. 报表菜单的按钮授权（导出 / 查看明细）写入角色菜单关系的 buttonInfo
UPDATE `SYS_RELATION`
SET `EXT_JSON` = JSON_SET(`EXT_JSON`, '$.buttonInfo', JSON_ARRAY('btn_rpt_exp_fin', 'btn_rpt_det_fin'))
WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  AND `TARGET_ID` = 'menu_report_fin_001'
  AND `OBJECT_ID` IN (SELECT `ID` FROM `SYS_ROLE` WHERE `CODE` = 'superAdmin');

UPDATE `SYS_RELATION`
SET `EXT_JSON` = JSON_SET(`EXT_JSON`, '$.buttonInfo', JSON_ARRAY('btn_rpt_exp_acad'))
WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  AND `TARGET_ID` = 'menu_report_acad_001'
  AND `OBJECT_ID` IN (SELECT `ID` FROM `SYS_ROLE` WHERE `CODE` = 'superAdmin');

UPDATE `SYS_RELATION`
SET `EXT_JSON` = JSON_SET(`EXT_JSON`, '$.buttonInfo', JSON_ARRAY('btn_rpt_exp_crm'))
WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  AND `TARGET_ID` = 'menu_report_crm_001'
  AND `OBJECT_ID` IN (SELECT `ID` FROM `SYS_ROLE` WHERE `CODE` = 'superAdmin');

-- 平台看板的导出按钮授权给平台运营角色
UPDATE `SYS_RELATION`
SET `EXT_JSON` = JSON_SET(`EXT_JSON`, '$.buttonInfo', JSON_ARRAY('btn_rpt_exp_plat'))
WHERE `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  AND `TARGET_ID` = 'menu_plat_dash_001'
  AND `OBJECT_ID` IN (SELECT `ID` FROM `SYS_ROLE` WHERE `CODE` = 'platformAdmin');
