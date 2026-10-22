-- ============================================================================
-- chameleon 教培岗位角色体系：存量租户补齐
--   背景：新租户开通时会自动创建「校长/招生顾问/授课老师/前台财务」四个岗位角色，
--        但存量租户（开通之前建的）没有这些角色，需要补一次。
--   用法：
--     1) 先执行 _sql/chameleon_role_api_button.sql（把业务接口登记为按钮资源）
--     2) 再调用平台运营接口为每个租户套用模板：
--        POST /platform/roleTemplate/apply  {"tenantId":"xxx"}
--   说明：套用是幂等的（已存在的授权不会重复添加），接口内部还会补齐机构管理员的全量业务权限。
-- ============================================================================

-- 1) 便于排查：查看各租户已有的岗位角色
-- SELECT t.CODE AS TENANT_CODE, r.CODE AS ROLE_CODE, r.NAME AS ROLE_NAME
-- FROM SYS_ROLE r JOIN T_TENANT t ON t.ID = r.TENANT_ID
-- WHERE r.CODE IN ('principal','sales','teacher','cashier')
-- ORDER BY t.CODE, r.CODE;

-- 2) 演示账号（demo 租户，可选）：teacher01 / sales01 / cashier01 / principal01
--    密码统一 Chameleon@2026!，实际使用时请让机构管理员在「用户管理」里改密
SET @tid = (SELECT ID FROM T_TENANT WHERE CODE = 'demo');
SET @campus = (SELECT ID FROM SYS_ORG WHERE TENANT_ID = @tid AND CATEGORY = 'DEPT' LIMIT 1);
SET @pwd = (SELECT PASSWORD FROM SYS_USER WHERE ACCOUNT = 'superAdmin' LIMIT 1);

INSERT IGNORE INTO SYS_USER (ID, TENANT_ID, ACCOUNT, PASSWORD, NAME, NICKNAME, GENDER, PHONE, ORG_ID, USER_STATUS, SORT_CODE, DELETE_FLAG, CREATE_TIME, CREATE_USER) VALUES
('u_teacher_001',   @tid, 'teacher01',   @pwd, '王老师', '王老师', '男', '13800000011', @campus, 'ENABLE', 11, 'NOT_DELETE', NOW(), '-1'),
('u_sales_001',     @tid, 'sales01',     @pwd, '李顾问', '李顾问', '女', '13800000012', @campus, 'ENABLE', 12, 'NOT_DELETE', NOW(), '-1'),
('u_cashier_001',   @tid, 'cashier01',   @pwd, '赵前台', '赵前台', '女', '13800000013', @campus, 'ENABLE', 13, 'NOT_DELETE', NOW(), '-1'),
('u_principal_001', @tid, 'principal01', @pwd, '周校长', '周校长', '男', '13800000014', @campus, 'ENABLE', 14, 'NOT_DELETE', NOW(), '-1');

INSERT IGNORE INTO SYS_RELATION (ID, TENANT_ID, OBJECT_ID, TARGET_ID, CATEGORY, EXT_JSON)
SELECT CONCAT('rel_u_', SUBSTRING(r.ID, -16)), r.TENANT_ID, u.ID, r.ID, 'SYS_USER_HAS_ROLE', NULL
FROM SYS_ROLE r
JOIN SYS_USER u ON u.TENANT_ID = r.TENANT_ID AND u.ACCOUNT IN ('teacher01', 'sales01', 'cashier01', 'principal01')
WHERE r.CODE = CASE u.ACCOUNT
    WHEN 'teacher01' THEN 'teacher'
    WHEN 'sales01' THEN 'sales'
    WHEN 'cashier01' THEN 'cashier'
    ELSE 'principal' END;
