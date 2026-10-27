# 续费与流失闭环（工作流 renew）

- 分支：`opt/renew`（基于 `da2ffc1`）
- 工作目录：`.jdk/wt-renew`
- 改动域：`studentrenewarn/**`、`views/student/renewWarn.vue`、`views/app/student/renew.vue`、新增 `api/student/renewApi.js`、本文件
- 未改动：`README.md`、`docs/26`、`docs/43+`、`P0SchemaMigration.java`、`GlobalConfigure.java`、`workbench/index.vue`

## 一、实现内容

### 1. 续费预警阈值可配置

支持三级配置，生效顺序：**班级 > 课程 > 机构默认 > 系统旧阈值**。

| 配置项 | 说明 | 默认 |
| --- | --- | --- |
| 剩余课时阈值 | 剩余课时 ≤ 该值触发“课时不足” | 3 |
| 到期前 N 天 | 课包在 N 天内到期触发“即将到期” | 7 |
| 重复提醒间隔（天） | 同一学员+课程在间隔内不重复生成预警 | 7 |

- 机构默认未配置时，继续读取旧配置键 `SNOWY_RENEW_WARN_LESSON_THRESHOLD` / `SNOWY_RENEW_WARN_EXPIRE_DAYS`，行为与上线前一致。
- 扫描时按「班级配置 → 课程配置 → 机构默认配置」解析生效值；班级配置通过 `CourseApi.getCourseClassInfo`（取课程）与 `CourseApi.getStudyingStudentIdList`（取在班学员）匹配。
- 前端入口：续费预警页右上角「预警配置」。
- 相关接口：
  - `GET /student/renewWarn/config/list`
  - `POST /student/renewWarn/config/save`
  - `POST /student/renewWarn/config/delete`

### 2. 预警行内动作

预警列表可直接操作，不再只跳页面：

| 动作 | 接口 | 行为 |
| --- | --- | --- |
| 发续费提醒 | `POST /student/renewWarn/notify` | 写入 `NOTIFY_LOG`（家长+教职工），更新预警 `LAST_NOTIFY_TIME`；不调用真实短信/微信 |
| 转顾问 | `POST /student/renewWarn/assign` | 校验顾问用户，写入 `FOLLOW_USER_ID/FOLLOW_TIME`，给顾问发站内通知 |
| 标记已处理 | `POST /student/renewWarn/process` | `STATUS=PROCESSED`，记录处理时间/说明 |
| 忽略 | `POST /student/renewWarn/ignore` | `STATUS=IGNORED` |
| 记录跟进 | `POST /student/renewWarn/follow/add` | 写入回访记录，可约定下次跟进 |
| 登记流失 | `POST /student/renewWarn/follow/add`（`bizType=LOSS`） | 记录流失类型、原因、回访内容 |
| 续费 | 复用现有 `POST /student/order/renew` | 收款入账后现有逻辑自动置为已续费 |

- 可指派顾问接口：`GET /student/renewWarn/assigneeOptions`，优先返回 `sales` 角色用户；没有招生顾问时退回全部启用用户。

### 3. 风险学员可处理

续费预警页新增「风险学员」Tab，复用 `StudentReportApi.riskStudents` 的数据（不重复造表），支持：

- 联系家长：`follow/add`，`bizType=RISK`，记录跟进方式/内容/下次跟进。
- 发提醒：`notify`，按学员+课程落库家长与教职工通知。
- 生成待办：`POST /student/renewWarn/risk/todo`，复用 `STUDENT_RENEW_WARN` 生成待处理预警；已有待处理时不重复生成。
- 转顾问：`assign`，风险学员没有预警时自动生成待办再指派。
- 登记流失：`follow/add`，`bizType=LOSS`。

风险分页接口：`GET /student/renewWarn/risk/page`（支持 `type=LOW_LESSON/NO_ATTEND/STOPPED`、`campusId`、`courseId`、`classId`）。

### 4. 预警快照与当前值

- 预警表保留 `REMAIN_LESSONS` 作为“预警时剩余课时”快照，新增 `SNAPSHOT_TIME` 记录快照时刻。
- 列表实时回填“当前剩余课时 / 当前到期时间 / 当前账户状态”（`CURRENT_REMAIN_LESSONS` 等为非数据库字段），并给出：
  - `currentRenewed=true`：预警后课时已补充（当前剩余 > 快照），前端展示「课时已补充」，避免把已续费学员误当流失。
  - 已续费状态仍以 `STATUS=RENEWED` 为准（支付入账联动）。
- 退学/结课：新增回访记录表 `STUDENT_FOLLOW_RECORD`，通过 `bizType=LOSS` + `LOSS_TYPE`（退学/结课/转校/其他）+ `LOSS_REASON` + 回访内容记录。
  - **不自动修改 `STUDENT.STATUS`**，避免与退费/停课流程冲突；状态仍由原流程维护。
- 回访查询：`GET /student/renewWarn/follow/page`（按学员/预警/业务类型）。

### 5. 续费率/流失率

- 接口：`GET /student/renewWarn/stats`，默认最近 90 天，可传 `startTime/endTime`。
- 口径：
  - 续费率 = `STATUS=RENEWED` 预警数 / 预警总数。
  - 流失率 = 统计期内登记流失且出现在预警学员集合中的学员数 / 预警涉及学员数。
  - 另外返回预警总数、待处理、已处理、已忽略、流失登记数等。
- 这是轻量经营指标，不是同期群（cohort）分析，详见“未完成/限制”。

## 二、数据库迁移（幂等，由主流程统一执行）

> 已新增独立迁移类 `StudentRenewWarnSchemaMigration`（`studentrenewarn/init`），启动时自动补表补列；本次工作流**未启动后端、未执行迁移**。主流程亦可手工执行以下 SQL。

### 2.1 续费预警表补列

```sql
ALTER TABLE `STUDENT_RENEW_WARN`
  ADD COLUMN `SNAPSHOT_TIME` datetime NULL DEFAULT NULL COMMENT '预警快照时间' AFTER `LAST_NOTIFY_TIME`,
  ADD COLUMN `FOLLOW_USER_ID` varchar(20) NULL DEFAULT NULL COMMENT '跟进人id' AFTER `SNAPSHOT_TIME`,
  ADD COLUMN `FOLLOW_TIME` datetime NULL DEFAULT NULL COMMENT '转顾问时间' AFTER `FOLLOW_USER_ID`,
  ADD COLUMN `PROCESS_REMARK` varchar(500) NULL DEFAULT NULL COMMENT '处理说明' AFTER `FOLLOW_TIME`,
  ADD COLUMN `PROCESS_TIME` datetime NULL DEFAULT NULL COMMENT '处理时间' AFTER `PROCESS_REMARK`,
  ADD INDEX `IDX_WARN_TENANT_STATUS_CREATE` (`TENANT_ID`, `STATUS`, `CREATE_TIME`);
```

### 2.2 新建阈值配置表

```sql
CREATE TABLE IF NOT EXISTS `STUDENT_RENEW_WARN_CONFIG` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
  `SCOPE_TYPE` varchar(20) NOT NULL COMMENT '配置范围(DEFAULT/COURSE/CLASS)',
  `SCOPE_ID` varchar(20) NOT NULL DEFAULT '0' COMMENT '范围id(默认0)',
  `LESSON_THRESHOLD` decimal(10,2) NOT NULL DEFAULT 3.00 COMMENT '剩余课时阈值',
  `EXPIRE_DAYS` int NOT NULL DEFAULT 7 COMMENT '到期提前天数',
  `REPEAT_DAYS` int NOT NULL DEFAULT 7 COMMENT '重复提醒间隔(天)',
  `ENABLED` varchar(10) NOT NULL DEFAULT 'YES' COMMENT '是否启用',
  `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
  PRIMARY KEY (`ID`),
  INDEX `IDX_WARN_CONFIG_SCOPE` (`TENANT_ID`, `SCOPE_TYPE`, `SCOPE_ID`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '续费预警阈值配置' ROW_FORMAT = Dynamic;
```

### 2.3 新建回访/流失记录表

```sql
CREATE TABLE IF NOT EXISTS `STUDENT_FOLLOW_RECORD` (
  `ID` varchar(20) NOT NULL COMMENT '主键',
  `TENANT_ID` varchar(20) NULL DEFAULT NULL COMMENT '租户id',
  `STUDENT_ID` varchar(20) NOT NULL COMMENT '学员id',
  `WARN_ID` varchar(20) NULL DEFAULT NULL COMMENT '续费预警id',
  `BIZ_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '业务类型(RENEW_WARN/RISK/LOSS)',
  `FOLLOW_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '跟进方式',
  `CONTENT` varchar(1000) NULL DEFAULT NULL COMMENT '跟进内容',
  `NEXT_FOLLOW_TIME` datetime NULL DEFAULT NULL COMMENT '下次跟进时间',
  `FOLLOW_USER_ID` varchar(20) NULL DEFAULT NULL COMMENT '跟进人id',
  `LOSS_TYPE` varchar(20) NULL DEFAULT NULL COMMENT '流失类型',
  `LOSS_REASON` varchar(500) NULL DEFAULT NULL COMMENT '流失原因',
  `DELETE_FLAG` varchar(255) NULL DEFAULT 'NOT_DELETE' COMMENT '删除标志',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建用户',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '修改用户',
  PRIMARY KEY (`ID`),
  INDEX `IDX_FOLLOW_TENANT_STUDENT` (`TENANT_ID`, `STUDENT_ID`, `CREATE_TIME`),
  INDEX `IDX_FOLLOW_TENANT_WARN` (`TENANT_ID`, `WARN_ID`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学员回访跟进记录' ROW_FORMAT = Dynamic;
```

### 2.4 租户隔离说明

本次不允许修改 `GlobalConfigure.java`，因此两张新表**未登记到 `GlobalConfigure.TENANT_TABLES`**。代码已在所有读写中显式带上 `TENANT_ID = 当前租户` 条件（`RenewWarnTenantUtil`），不会串租户。

后续如需统一的 MyBatis 租户拦截器自动处理，请由主流程在 `GlobalConfigure.TENANT_TABLES` 中补充：

```java
"STUDENT_RENEW_WARN_CONFIG", "STUDENT_FOLLOW_RECORD"
```

## 三、权限

`/student/**` 受 `BIZ_PERMISSION_PATH_ARR` 接口权限校验，新增接口必须授权。已新增 `StudentRenewWarnPermissionMigration`，启动时幂等执行：

- 注册 12 个按钮/接口资源（`btn_rn_*`），挂到 `menu_renew_001`；
- 追加到已有续费预警菜单的 `buttonInfo`；
- 复制已有 `/student/renewWarn/page` 权限角色的接口授权；
- 补充所有租户 `superAdmin` 的接口授权。

主流程无需手工执行权限 SQL。若迁移失败，日志会提示按本文件历史 SQL 兜底。

## 四、改动文件

后端（均在 `snowy-plugin/snowy-plugin-student/src/main/java/vip/xiaonuo/student/modular/studentrenewarn/`）：

- `controller/StudentRenewWarnController.java`
- `service/StudentRenewWarnService.java`
- `service/StudentRenewWarnConfigService.java`
- `service/impl/StudentRenewWarnServiceImpl.java`
- `service/impl/StudentRenewWarnConfigServiceImpl.java`
- `entity/StudentRenewWarn.java`
- `entity/StudentRenewWarnConfig.java`（新）
- `entity/StudentFollowRecord.java`（新）
- `enums/StudentRenewWarnStatusEnum.java`
- `mapper/StudentRenewWarnConfigMapper.java`（新）
- `mapper/StudentFollowRecordMapper.java`（新）
- `init/StudentRenewWarnSchemaMigration.java`（新）
- `util/RenewWarnTenantUtil.java`（新）
- `dto/ResolvedWarnConfig.java`、`dto/StudentRenewWarnRiskVO.java`、`dto/StudentRenewWarnStatsVO.java`（新）
- `param/*`：配置、提醒、转顾问、已处理、风险分页、风险待办、跟进、统计参数（新）

前端：

- `snowy-admin-web/src/api/student/renewApi.js`（新，未改 `studentApi.js`）
- `snowy-admin-web/src/views/student/renewWarn.vue`
- `snowy-admin-web/src/views/app/student/renew.vue`

## 五、验证

- 后端：`JAVA_HOME=D:\IdeaProject\chameleon\.jdk\jdk-17 mvn -s D:\IdeaProject\chameleon\.jdk\maven-settings-local.xml clean package -DskipTests` 通过。
- 前端：`cd snowy-admin-web && npm run build` 通过。
- 未启动后端、未执行迁移、未改共享数据库、未合并/rebase/push。

## 六、未完成 / 限制

1. **未接真实短信/微信**：`notify` 只写 `NOTIFY_LOG` 并更新 `LAST_NOTIFY_TIME`，与本次需求一致。
2. **流失登记不改学员状态**：只记录流失类型/原因/回访，退学/停课/退费状态仍由现有流程维护，避免双写冲突。
3. **重复提醒间隔与自动化规则的关系**：配置里的 `REPEAT_DAYS` 用于扫描去重；`AutoNotifyRenewWarnHandler` 的 `repeatHours` 是自动化规则自己的参数（该文件不在本工作流改动域），两者独立。如需统一，由后续工作流合并。
4. **续费率/流失率为轻量口径**：按统计期预警与流失登记计算，未做同期群、按课程/校区下钻、续费金额归因。
5. **风险学员数据来自现有报表接口**：一次最多返回 300 条（可传 `limit`，上限 1000），未做数据库级分页；大数据量下建议后续改为 SQL 分页。
6. **新表未登记 `TENANT_TABLES`**：因禁止修改 `GlobalConfigure.java`，当前由代码显式租户过滤；主流程可后续补充登记。
7. 权限迁移已自动执行（`StudentRenewWarnPermissionMigration`）；未给 `sales` 等业务角色自动授权，按需在角色管理里补充。
