-- ============================================================
-- chameleon 菜单 DB 化：业务模块与菜单（平台级，不参与租户隔离）
-- 执行方式：mysql -uroot -p chameleon < chameleon_menu.sql
-- 说明：
--   - 可重复执行（INSERT IGNORE）；
--   - 菜单挂到「教务运营」「平台运营」两个模块下，并授权给超级管理员角色；
--   - 执行后前端 src/config/route.js 只保留个人中心静态菜单，业务菜单全部来自数据库；
--   - 学员详情不再是独立菜单页，改为「学员管理」列表内的抽屉（前端保留旧链接重定向）。
-- ============================================================

-- 模块
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('mod_edu_001', NULL, '教务运营', NULL, 'edu', 'MODULE', NULL, NULL, NULL, NULL, 'read-outlined', '#1677ff', NULL, NULL, NULL, 10, 'NOT_DELETE', NOW(), '1'),
('mod_platform_001', NULL, '平台运营', NULL, 'platform', 'MODULE', NULL, NULL, NULL, NULL, 'bank-outlined', '#722ed1', NULL, NULL, NULL, 20, 'NOT_DELETE', NOW(), '1'),
('mod_report_001', NULL, '报表中心', NULL, 'report', 'MODULE', NULL, NULL, NULL, NULL, 'bar-chart-outlined', '#13c2c2', NULL, NULL, NULL, 30, 'NOT_DELETE', NOW(), '1');

-- 菜单（老师端/机构端业务页面；学员详情为隐藏路由 VISIBLE=NO）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_crm_001',      '0', '招生线索', 'crmLead',        'crm',           'MENU', 'mod_edu_001', 'MENU', '/crm',             'crm/index',        'funnel-plot-outlined', NULL, 'YES', 'YES', 'YES', 1, 'NOT_DELETE', NOW(), '1'),
('menu_student_001',  '0', '学员管理', 'studentIndex',   'student',       'MENU', 'mod_edu_001', 'MENU', '/student',         'student/index',    'smile-outlined',       NULL, 'YES', 'YES', 'YES', 2, 'NOT_DELETE', NOW(), '1'),
('menu_detail_001',   '0', '学员详情', 'studentDetail',  'studentDetail', 'MENU', 'mod_edu_001', 'MENU', '/student/detail',  'student/detail',   'profile-outlined',     NULL, 'NO',  NULL,  NULL,  3, 'NOT_DELETE', NOW(), '1'),
('menu_course_001',   '0', '课程管理', 'courseIndex',    'course',        'MENU', 'mod_edu_001', 'MENU', '/course',          'course/index',     'book-outlined',        NULL, 'YES', 'YES', 'YES', 4, 'NOT_DELETE', NOW(), '1'),
('menu_pkg_001',      '0', '课包管理', 'coursePackage',  'coursePkg',     'MENU', 'mod_edu_001', 'MENU', '/course/package',  'course/package',   'gift-outlined',        NULL, 'YES', 'YES', 'YES', 5, 'NOT_DELETE', NOW(), '1'),
('menu_class_001',    '0', '班级管理', 'courseClass',    'courseClass',   'MENU', 'mod_edu_001', 'MENU', '/course/class',    'course/class',     'team-outlined',        NULL, 'YES', 'YES', 'YES', 6, 'NOT_DELETE', NOW(), '1'),
('menu_schedule_001', '0', '排课课表', 'courseSchedule', 'courseSchedule','MENU', 'mod_edu_001', 'MENU', '/course/schedule', 'course/schedule',  'calendar-outlined',    NULL, 'YES', 'YES', 'YES', 7, 'NOT_DELETE', NOW(), '1'),
('menu_attend_001',   '0', '点名消课', 'courseAttend',   'courseAttend',  'MENU', 'mod_edu_001', 'MENU', '/course/attend',   'course/attend',    'check-square-outlined',NULL, 'YES', 'YES', 'YES', 8, 'NOT_DELETE', NOW(), '1'),
('menu_renew_001',    '0', '续费预警', 'renewWarn',      'renewWarn',     'MENU', 'mod_edu_001', 'MENU', '/student/renewWarn','student/renewWarn','bell-outlined',       NULL, 'YES', 'YES', 'YES', 9, 'NOT_DELETE', NOW(), '1'),
('menu_finance_001',  '0', '财务收款', 'financeIndex',   'finance',       'MENU', 'mod_edu_001', 'MENU', '/finance',         'finance/index',    'account-book-outlined',NULL, 'YES', 'YES', 'YES', 10,'NOT_DELETE', NOW(), '1'),
('menu_report_dash_001', '0', '经营看板', 'reportDashboard', 'reportDashboard', 'MENU', 'mod_report_001', 'MENU', '/report/dashboard', 'report/dashboard', 'dashboard-outlined', NULL, 'YES', 'YES', 'YES', 1, 'NOT_DELETE', NOW(), '1'),
('menu_report_fin_001',  '0', '财务报表', 'reportFinance',   'reportFinance',   'MENU', 'mod_report_001', 'MENU', '/report/finance',   'report/finance',   'fund-outlined',      NULL, 'YES', 'YES', 'YES', 2, 'NOT_DELETE', NOW(), '1'),
('menu_report_acad_001', '0', '教务报表', 'reportAcademic', 'reportAcademic', 'MENU', 'mod_report_001', 'MENU', '/report/academic', 'report/academic', 'solution-outlined', NULL, 'YES', 'YES', 'YES', 3, 'NOT_DELETE', NOW(), '1'),
('menu_report_crm_001',  '0', '招生报表', 'reportCrm',      'reportCrm',      'MENU', 'mod_report_001', 'MENU', '/report/crm',      'report/crm',      'funnel-plot-outlined', NULL, 'YES', 'YES', 'YES', 4, 'NOT_DELETE', NOW(), '1'),
('menu_plat_dash_001', '0', '平台看板', 'platformDashboard', 'platformDashboard', 'MENU', 'mod_platform_001','MENU','/platform/dashboard','platform/dashboard','pie-chart-outlined', NULL, 'YES', 'YES', 'YES', 0, 'NOT_DELETE', NOW(), '1'),
('menu_tenant_001',   '0', '租户管理', 'platformTenant', 'platformTenant','MENU', 'mod_platform_001','MENU','/platform/tenant','platform/tenant', 'bank-outlined',        NULL, 'YES', 'YES', 'YES', 1, 'NOT_DELETE', NOW(), '1');

-- 授权给超级管理员角色（SYS_ROLE_HAS_RESOURCE；父子菜单会自动带出）
-- 注意：SYS_RELATION 已租户化（M1），关系行必须带 TENANT_ID，否则会被租户拦截器过滤
SET @DEMO_TENANT_ID = '1554740179362967450';

INSERT IGNORE INTO `SYS_RELATION` (`ID`, `TENANT_ID`, `OBJECT_ID`, `TARGET_ID`, `CATEGORY`, `EXT_JSON`) VALUES
('rel_m_crm',    @DEMO_TENANT_ID, '1570687866138206208', 'menu_crm_001',      'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_crm_001","buttonInfo":[]}'),
('rel_m_stu',    @DEMO_TENANT_ID, '1570687866138206208', 'menu_student_001',  'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_student_001","buttonInfo":[]}'),
('rel_m_detail', @DEMO_TENANT_ID, '1570687866138206208', 'menu_detail_001',   'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_detail_001","buttonInfo":[]}'),
('rel_m_course', @DEMO_TENANT_ID, '1570687866138206208', 'menu_course_001',   'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_course_001","buttonInfo":[]}'),
('rel_m_pkg',    @DEMO_TENANT_ID, '1570687866138206208', 'menu_pkg_001',      'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_pkg_001","buttonInfo":[]}'),
('rel_m_class',  @DEMO_TENANT_ID, '1570687866138206208', 'menu_class_001',    'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_class_001","buttonInfo":[]}'),
('rel_m_sched',  @DEMO_TENANT_ID, '1570687866138206208', 'menu_schedule_001', 'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_schedule_001","buttonInfo":[]}'),
('rel_m_attend', @DEMO_TENANT_ID, '1570687866138206208', 'menu_attend_001',   'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_attend_001","buttonInfo":[]}'),
('rel_m_renew',  @DEMO_TENANT_ID, '1570687866138206208', 'menu_renew_001',    'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_renew_001","buttonInfo":[]}'),
('rel_m_report_dash', @DEMO_TENANT_ID, '1570687866138206208', 'menu_report_dash_001', 'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_report_dash_001","buttonInfo":[]}'),
('rel_m_report_fin',  @DEMO_TENANT_ID, '1570687866138206208', 'menu_report_fin_001',  'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_report_fin_001","buttonInfo":[]}'),
('rel_m_report_acad', @DEMO_TENANT_ID, '1570687866138206208', 'menu_report_acad_001', 'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_report_acad_001","buttonInfo":[]}'),
('rel_m_report_crm',  @DEMO_TENANT_ID, '1570687866138206208', 'menu_report_crm_001',  'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_report_crm_001","buttonInfo":[]}'),
('rel_m_fin',    @DEMO_TENANT_ID, '1570687866138206208', 'menu_finance_001',  'SYS_ROLE_HAS_RESOURCE', '{"menuId":"menu_finance_001","buttonInfo":[]}');

-- 注意：平台运营菜单（租户管理/套餐管理）不授权给业务租户，
-- 由后端 PlatformBootstrap 在启动时授权给租户 0 的 platformAdmin 角色。

-- 兼容首次执行时租户字段为空的场景
UPDATE `SYS_RELATION` SET `TENANT_ID` = @DEMO_TENANT_ID WHERE `ID` LIKE 'rel_m_%' AND `TENANT_ID` IS NULL;

-- 套餐管理（平台运营模块）
INSERT IGNORE INTO `SYS_RESOURCE` (`ID`, `PARENT_ID`, `TITLE`, `NAME`, `CODE`, `CATEGORY`, `MODULE`, `MENU_TYPE`, `PATH`, `COMPONENT`, `ICON`, `COLOR`, `VISIBLE`, `DISPLAY_LAYOUT`, `KEEP_LIVE`, `SORT_CODE`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`) VALUES
('menu_ppkg_001', '0', '套餐管理', 'platformPackage', 'platformPackage', 'MENU', 'mod_platform_001', 'MENU', '/platform/package', 'platform/package', 'database-outlined', NULL, 'YES', 'YES', 'YES', 2, 'NOT_DELETE', NOW(), '1');
