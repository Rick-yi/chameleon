-- ============================================================
-- chameleon 易用性改造：新增「工作台」「收款台」菜单（可重复执行）
-- 执行方式：mysql -uroot -p chameleon < chameleon_ux_menu.sql
-- 内容：
--   1. 新增两个业务菜单（工作台 / 收款台），挂到「教务运营」模块下
--   2. 教务运营菜单排序微调（工作台置顶、收款台紧随学员管理）
--   3. 授权给各租户超级管理员角色
-- 前置：chameleon_menu.sql 已执行
-- ============================================================

-- 1. 新增菜单
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_workbench_001', '0', '工作台', 'workbenchIndex', 'workbench', 'MENU', 'mod_edu_001', 'MENU', '/workbench',      'workbench/index', 'home-outlined',      NULL, 'YES', 'YES', 'YES', 0,  'NOT_DELETE', NOW(), '1'),
('menu_order_001',     '0', '收款台', 'studentOrder',   'order',     'MENU', 'mod_edu_001', 'MENU', '/student/order', 'student/order',  'wallet-outlined',    NULL, 'YES', 'YES', 'YES', 3,  'NOT_DELETE', NOW(), '1');

-- 2. 教务运营模块排序：工作台(0) → 招生线索(1) → 学员管理(2) → 收款台(3) → 其后顺延
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 5  WHERE `ID` = 'menu_course_001';
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 6  WHERE `ID` = 'menu_pkg_001';
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 7  WHERE `ID` = 'menu_class_001';
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 8  WHERE `ID` = 'menu_schedule_001';
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 9  WHERE `ID` = 'menu_attend_001';
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 10 WHERE `ID` = 'menu_renew_001';
UPDATE `SYS_RESOURCE` SET `SORT_CODE` = 11 WHERE `ID` = 'menu_finance_001';

-- 3. 授权给各租户超级管理员（菜单 + 按钮信息）
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rel_ux_', RIGHT(m.`ID`, 3), '_', LEFT(MD5(CONCAT(r.`ID`, m.`ID`)), 6)),
       r.`TENANT_ID`, r.`ID`, m.`ID`, 'SYS_ROLE_HAS_RESOURCE',
       JSON_OBJECT('menuId', m.`ID`, 'buttonInfo', JSON_ARRAY())
FROM `SYS_ROLE` r
JOIN `SYS_RESOURCE` m ON m.`ID` IN ('menu_workbench_001', 'menu_order_001')
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = m.`ID` AND s.`CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  );

-- 4. 菜单去重：「财务收款」与「收款台」功能重复，统一收敛到「收款台」，经营报表在「报表中心 · 财务报表」
DELETE FROM `SYS_RELATION` WHERE `TARGET_ID` = 'menu_finance_001' AND `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE';
DELETE FROM `SYS_RESOURCE` WHERE `ID` = 'menu_finance_001';

-- 5. 运营埋点菜单（报表中心）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_event_001', '0', '运营埋点', 'reportEventAnalysis', 'eventAnalysis', 'MENU', 'mod_report_001', 'MENU', '/report/eventAnalysis', 'report/eventAnalysis', 'deployment-unit-outlined', NULL, 'YES', 'YES', 'YES', 5, 'NOT_DELETE', NOW(), '1');

INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`)
SELECT CONCAT('rel_ux_event', LEFT(MD5(CONCAT(r.`ID`, 'menu_event_001')), 12)),
       r.`TENANT_ID`, r.`ID`, 'menu_event_001', 'SYS_ROLE_HAS_RESOURCE',
       JSON_OBJECT('menuId', 'menu_event_001', 'buttonInfo', JSON_ARRAY())
FROM `SYS_ROLE` r
WHERE r.`CODE` = 'superAdmin'
  AND NOT EXISTS (
      SELECT 1 FROM `SYS_RELATION` s
      WHERE s.`OBJECT_ID` = r.`ID` AND s.`TARGET_ID` = 'menu_event_001' AND s.`CATEGORY` = 'SYS_ROLE_HAS_RESOURCE'
  );

-- 6. 学员详情改为「学员管理」内的抽屉：移除独立菜单（前端 config/route.js 保留旧链接重定向）
DELETE FROM `SYS_RELATION` WHERE `TARGET_ID` = 'menu_detail_001';
DELETE FROM `SYS_RESOURCE` WHERE `ID` = 'menu_detail_001';

-- 6. 兼容历史数据：关系行补租户（未授权角色仍需手动在「角色授权」中勾选）
UPDATE `SYS_RELATION` SET `TENANT_ID` = (SELECT `ID` FROM `T_TENANT` ORDER BY `CREATE_TIME` LIMIT 1)
WHERE `ID` LIKE 'rel_ux_%' AND (`TENANT_ID` IS NULL OR `TENANT_ID` = '');
