-- ============================================================
-- chameleon 报表 R2-R4 升级脚本（执行前请备份）
-- 适用场景：
--   1. 已执行过旧版 chameleon_menu.sql（教务报表/平台看板菜单 ID 被 varchar(20) 截断）；
--   2. 需要补齐教务报表、招生报表、平台看板菜单。
-- 执行顺序：
--   mysql -uroot -p chameleon < chameleon_menu.sql
--   mysql -uroot -p chameleon < chameleon_report_r2r4_migration.sql
-- 说明：
--   - chameleon_menu.sql 使用 INSERT IGNORE，可重复执行；
--   - 本脚本负责清理旧版截断菜单并修复授权关系，可重复执行。
-- ============================================================

-- 旧版菜单 ID 被截断为 20 位：menu_report_academic / menu_platform_dash_0
-- 若新版正确 ID 已存在，删除旧行；否则把旧行改名为正确 ID。
DELETE r FROM `SYS_RESOURCE` r
    JOIN `SYS_RESOURCE` n ON n.`ID` = 'menu_report_acad_001'
WHERE r.`ID` = 'menu_report_academic';
UPDATE `SYS_RESOURCE` SET `ID` = 'menu_report_acad_001' WHERE `ID` = 'menu_report_academic';

DELETE r FROM `SYS_RESOURCE` r
    JOIN `SYS_RESOURCE` n ON n.`ID` = 'menu_plat_dash_001'
WHERE r.`ID` = 'menu_platform_dash_0';
UPDATE `SYS_RESOURCE` SET `ID` = 'menu_plat_dash_001' WHERE `ID` = 'menu_platform_dash_0';

-- 修复旧授权关系
DELETE FROM `SYS_RELATION` WHERE `ID` = 'rel_m_report_academi';
DELETE FROM `SYS_RELATION` WHERE `TARGET_ID` IN ('menu_report_academic', 'menu_report_academic_001')
    AND `CATEGORY` = 'SYS_ROLE_HAS_RESOURCE';
UPDATE `SYS_RELATION`
SET `TARGET_ID` = 'menu_plat_dash_001',
    `EXT_JSON` = REPLACE(`EXT_JSON`, 'menu_platform_dash_0', 'menu_plat_dash_001')
WHERE `TARGET_ID` = 'menu_platform_dash_0';

-- 演示机构超管授权教务报表
SET @DEMO_TENANT_ID = '1554740179362967450';
INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`) VALUES
('rel_m_report_acad', @DEMO_TENANT_ID, '1570687866138206208', 'menu_report_acad_001', 'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_report_acad_001","buttonInfo":[]}');

-- 平台看板授权由 PlatformBootstrap 在启动时按 mod_platform_001 下的菜单自动补齐，
-- 这里仅修复可能存在的旧关系目标。
