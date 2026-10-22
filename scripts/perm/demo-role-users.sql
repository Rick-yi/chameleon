-- ============================================================================
-- 为 demo 租户创建四个岗位账号，用于验证角色权限与数据范围
--   teacher01  王老师  授课老师   → 只看自己的课与班
--   sales01    李顾问  招生顾问   → 只看自己跟进的线索
--   cashier01  赵前台  前台/财务   → 收款、退费、财务报表
--   principal01 周校长 校长/运营   → 全部经营数据
--   密码统一：Chameleon@2026!
--   幂等：可重复执行
-- ============================================================================

SET @tid = (SELECT ID FROM T_TENANT WHERE CODE = 'demo');
SET @campus = (SELECT ID FROM SYS_ORG WHERE TENANT_ID = @tid AND CATEGORY = 'DEPT' LIMIT 1);
SET @pwd = (SELECT PASSWORD FROM SYS_USER WHERE ACCOUNT = 'superAdmin' LIMIT 1);

-- 1) 用户
INSERT IGNORE INTO SYS_USER (ID, TENANT_ID, ACCOUNT, PASSWORD, NAME, NICKNAME, GENDER, PHONE, ORG_ID, USER_STATUS, SORT_CODE, DELETE_FLAG, CREATE_TIME, CREATE_USER) VALUES
('u_teacher_001',   @tid, 'teacher01',   @pwd, '王老师', '王老师', '男', '13800000011', @campus, 'ENABLE', 11, 'NOT_DELETE', NOW(), '-1'),
('u_sales_001',     @tid, 'sales01',     @pwd, '李顾问', '李顾问', '女', '13800000012', @campus, 'ENABLE', 12, 'NOT_DELETE', NOW(), '-1'),
('u_cashier_001',   @tid, 'cashier01',   @pwd, '赵前台', '赵前台', '女', '13800000013', @campus, 'ENABLE', 13, 'NOT_DELETE', NOW(), '-1'),
('u_principal_001', @tid, 'principal01', @pwd, '周校长', '周校长', '男', '13800000014', @campus, 'ENABLE', 14, 'NOT_DELETE', NOW(), '-1');

-- 2) 用户 → 角色
INSERT IGNORE INTO SYS_RELATION (ID, TENANT_ID, OBJECT_ID, TARGET_ID, CATEGORY, EXT_JSON)
SELECT CONCAT('rel_u_', SUBSTRING(r.ID, -16)), r.TENANT_ID, u.ID, r.ID, 'SYS_USER_HAS_ROLE', NULL
FROM SYS_ROLE r
JOIN SYS_USER u ON u.TENANT_ID = r.TENANT_ID AND u.ACCOUNT IN ('teacher01', 'sales01', 'cashier01', 'principal01')
WHERE r.CODE = CASE u.ACCOUNT
    WHEN 'teacher01' THEN 'teacher'
    WHEN 'sales01' THEN 'sales'
    WHEN 'cashier01' THEN 'cashier'
    ELSE 'principal' END;

-- 3) 给老师一个班级（用于验证「只看自己班」）
UPDATE COURSE_CLASS SET TEACHER_ID = 'u_teacher_001' WHERE ID = 'cc_ux_001' AND TENANT_ID = @tid;

-- 4) 给顾问分配几条线索
UPDATE CRM_LEAD SET FOLLOW_USER_ID = 'u_sales_001' WHERE TENANT_ID = @tid AND FOLLOW_USER_ID = '1543837863788879871' LIMIT 5;
