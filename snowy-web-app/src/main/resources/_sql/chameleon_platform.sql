-- ============================================================
-- chameleon SaaS 平台域表（平台级，不参与租户隔离）
-- 执行方式：mysql -uroot -p chameleon < chameleon_platform.sql
-- 依赖：先导入 snowy_mysql.sql（底座 33 张表）
-- ============================================================

-- 租户套餐
CREATE TABLE `T_TENANT_PACKAGE`  (
  `ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '套餐名称',
  `PRICE` decimal(12,2) NULL DEFAULT NULL COMMENT '价格(元)',
  `VALIDITY_DAYS` int(11) NULL DEFAULT NULL COMMENT '有效期(天)',
  `FUNC_CONFIG` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '功能开关配置(JSON)',
  `STUDENT_LIMIT` int(11) NULL DEFAULT NULL COMMENT '学员数上限',
  `CAMPUS_LIMIT` int(11) NULL DEFAULT NULL COMMENT '校区数上限',
  `REMARK` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `SORT_CODE` int(11) NULL DEFAULT NULL COMMENT '排序码',
  `EXT_JSON` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '扩展信息',
  `DELETE_FLAG` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除标志',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建用户',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `UPDATE_USER` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改用户',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐' ROW_FORMAT = Dynamic;

-- 租户（SaaS 机构）
CREATE TABLE `T_TENANT`  (
  `ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键(租户id)',
  `CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户编码(登录识别机构)',
  `NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '机构名称',
  `CONTACT_NAME` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系人',
  `CONTACT_PHONE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `PACKAGE_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '套餐id',
  `EXPIRE_TIME` datetime NULL DEFAULT NULL COMMENT '到期时间',
  `STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态(试用/正常/停用)',
  `REMARK` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `EXT_JSON` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '扩展信息',
  `DELETE_FLAG` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除标志',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建用户',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `UPDATE_USER` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改用户',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE INDEX `UK_TENANT_CODE` (`CODE`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户' ROW_FORMAT = Dynamic;

-- 种子数据：默认套餐 + 演示租户
INSERT INTO `T_TENANT_PACKAGE` (`ID`, `NAME`, `PRICE`, `VALIDITY_DAYS`, `FUNC_CONFIG`, `STUDENT_LIMIT`, `CAMPUS_LIMIT`, `REMARK`, `SORT_CODE`, `EXT_JSON`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`, `UPDATE_TIME`, `UPDATE_USER`) VALUES ('pkg_demo', '专业版', 1999.00, 365, '{}', 500, 3, '默认套餐', 1, '{}', 'NOT_DELETE', NOW(), '1', NOW(), '1');

INSERT INTO `T_TENANT` (`ID`, `CODE`, `NAME`, `CONTACT_NAME`, `CONTACT_PHONE`, `PACKAGE_ID`, `EXPIRE_TIME`, `STATUS`, `REMARK`, `EXT_JSON`, `DELETE_FLAG`, `CREATE_TIME`, `CREATE_USER`, `UPDATE_TIME`, `UPDATE_USER`) VALUES ('1554740179362967450', 'demo', '示例机构', '张三', '13800000000', 'pkg_demo', DATE_ADD(NOW(), INTERVAL 365 DAY), 'TRIAL', '演示租户', '{}', 'NOT_DELETE', NOW(), '1', NOW(), '1');
