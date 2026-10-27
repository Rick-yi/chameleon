# 财务与报表体验（opt/finance）

> 工作流：`opt/finance`（基于 `da2ffc1`）。仅改动财务插件、财务/报表前端、报表 API 与本文档。
> 不启动后端、不连接共享数据库、不修改 P0SchemaMigration / GlobalConfigure / student / course / crm / platform。

## 1. 本次完成项

| 优先级 | 内容 | 状态 |
| --- | --- | --- |
| P1 | 指标口径 tooltip：收款金额、退费金额、净营收、课消金额、已支付订单数、客单价、生均收入、退费率、实收率；教务报表补到课率/出勤率/未点名人次口径 | 已完成 |
| P1 | 同比/环比与自定义对比期：财务报表新增对比方式选择、自定义对比期、KPI 同时展示环比与同比；导出沿用当前筛选条件 | 已完成 |
| P1 | 危险操作收口：重新对账/重算当日汇总/清理报表缓存仅 `superAdmin`、`platformAdmin` 可见；后端对重算/清缓存追加角色校验；业务端只读“对账正常/存在差异（原因）” | 已完成 |
| P2 | 数据更新时间与导出记录：页面展示服务端数据生成时间、最近日汇总日期；导出任务展示进度，可查看历史导出记录并下载 | 已完成 |
| P2 | 移动端报表分享/导出图片、教师绩效报表 | 未做（见第 6 节） |
| - | 外部支付/电子收据/扫码枪/小票打印 | 未接入，也未伪造接入 |

## 2. 新增/变更接口

### 2.1 新增接口

| 方法 | 路径 | 说明 | 权限资源 CODE |
| --- | --- | --- | --- |
| GET | `/finance/report/compare` | 返回当前区间、环比、同比、自定义对比期汇总 | 复用 `/finance/report/summary` |
| GET | `/finance/report/meta` | 数据生成时间、最近日汇总日期/更新时间、缓存 TTL | 复用 `/finance/report/summary` |
| GET | `/finance/report/export/async/page` | 我的导出记录（租户隔离，可按类型过滤） | 复用 `/finance/report/export/async/status` |

说明：本次不新增 `SYS_RESOURCE` 权限资源，不改 `P0SchemaMigration`。新接口复用已存在且已授权的权限 CODE，避免现有环境升级后 403。

### 2.2 角色校验变更

| 接口 | 原校验 | 新增校验 | 说明 |
| --- | --- | --- | --- |
| `POST /finance/report/dailyStat/rebuild` | `@SaCheckPermission("/finance/report/dailyStat/rebuild")` | `@SaCheckRole({"superAdmin","platformAdmin"}, OR)` | 非运维/平台角色即使被授予接口权限也无法重算 |
| `POST /finance/report/cache/clear` | `@SaCheckPermission("/finance/report/cache/clear")` | `@SaCheckRole({"superAdmin","platformAdmin"}, OR)` | 同上 |
| `GET /finance/report/reconcile` | `@SaCheckPermission("/finance/report/reconcile")` | 无变更 | 只读对账结果，业务角色仍自动加载；前端隐藏“重新对账”按钮 |

## 3. 资源 CODE 与授权角色

### 3.1 复用的权限资源

| 资源 CODE | 覆盖接口 | 授权角色 |
| --- | --- | --- |
| `/finance/report/summary` | `GET /finance/report/summary`、`GET /finance/report/compare`、`GET /finance/report/meta` | `superAdmin`（报表权限 SQL 显式授权）；`principal`、`cashier`（角色模板按 `/finance/report` 前缀授权）；`platformAdmin` 如需查看租户财务需由平台侧单独授权 |
| `/finance/report/export/async/status` | `GET /finance/report/export/async/status`、`GET /finance/report/export/async/page` | 同上 |

### 3.2 危险操作资源 CODE

| 资源 CODE | 接口 | 授权角色 | 额外限制 |
| --- | --- | --- | --- |
| `/finance/report/dailyStat/rebuild` | `POST /finance/report/dailyStat/rebuild` | `superAdmin` | 运行时角色必须为 `superAdmin` 或 `platformAdmin` |
| `/finance/report/cache/clear` | `POST /finance/report/cache/clear` | `superAdmin` | 运行时角色必须为 `superAdmin` 或 `platformAdmin` |
| `/finance/report/reconcile` | `GET /finance/report/reconcile` | `superAdmin`、`principal`、`cashier` | 只读；业务端仅展示结论 |

> 角色定义来源：`PlatformRoleTemplates`（`principal` 校长/运营负责人、`cashier` 前台/财务），以及平台内置 `superAdmin`、`platformAdmin`。
> 前端按 `USER_INFO.roleCodeList` 判断 `isOpsRole = superAdmin || platformAdmin`，仅该角色显示运维按钮。

## 4. 指标口径

| 指标 | 公式 | 统计范围 | 数据更新 |
| --- | --- | --- | --- |
| 收款金额 | 已支付订单实收金额合计 | 支付时间（缺失用创建时间）落在所选区间，受校区/课程/渠道筛选影响 | 实时聚合即时生效；完整覆盖的区间每日 00:30 走日汇总 |
| 退费金额 | 已退款退费单金额合计 | 退款时间（缺失依次取审批/更新/创建时间）落在所选区间 | 同上 |
| 净营收 | 收款金额 − 退费金额 | 同收款/退费区间与筛选 | 同上 |
| 已支付订单数 | 状态为已支付/已退费的订单笔数 | 支付时间（缺失用创建时间）落在所选区间 | 同上 |
| 客单价 | 收款金额 ÷ 已支付订单数 | 同收款区间；订单数为 0 时显示 0 | 同上 |
| 生均收入 | 收款金额 ÷ 去重付费学员数 | 分母为区间内实际有支付订单的学员，不是全部在读学员 | 同上 |
| 退费率 | 退费金额 ÷ 收款金额 × 100% | 同收款/退费区间；收款为 0 时显示 0 | 同上 |
| 实收率 | 收款金额 ÷（收款金额 + 待收款金额）× 100% | 待收款取区间内创建的未支付订单，受校区/课程筛选影响 | 同上 |
| 课消金额 | 课消课时 × 订单购买课时单价（权责口径），回补冲减 | 课时流水时间落在所选区间 | 点名提交即时生效；日汇总每日 00:30 回补 |
| 到课率 | 到课人次 ÷（到课 + 请假 + 缺勤）× 100% | 区间内已提交点名的记录 | 点名提交即时生效 |
| 出勤率 | 到课人次 ÷ 应到人次 × 100% | 应到 = 排课次数 × 班级在学人数，含未点名课节 | 点名提交即时生效 |
| 未点名人次 | 应到人次 − 已点名人次 | 已点名 = 到课 + 请假 + 缺勤；大于 0 表示仍有未点名 | 点名提交即时生效 |

## 5. 对比期规则

- 环比（MOM）：与当前区间天数相同、紧邻其前的上一周期。
- 同比（YOY）：当前区间起止日期各减一年；2 月 29 日自动落到 2 月 28 日。
- 自定义（CUSTOM）：用户选择任意起止日期，按整天 00:00:00 ~ 23:59:59 统计。
- 现有固定周期接口（本月、近 30 天、近 6 个月等）行为不变；对比计算只发生在新增 `/finance/report/compare` 中。
- 导出始终使用当前主筛选条件（时间/校区/课程/渠道），不会把对比期混入导出明细。

## 6. 未完成 / 阻塞项

| 项 | 原因 | 后续建议 |
| --- | --- | --- |
| 移动端报表分享/导出图片 | 允许改动域只包含 `views/report/**` 与财务插件，移动端页面不在域内 | 由移动端工作流实现，复用本次 `/finance/report/meta` 与导出接口 |
| 教师绩效报表 | 需要课程域教师课时/到课/续费数据，`course/**` 禁止修改 | 在课程域新增聚合接口后，报表页直接消费 |
| 外部支付 / 电子收据 / 扫码枪 / 小票打印 | 无支付通道与硬件对接，按约定不伪造接通 | 接入真实通道后单独设计；内部收据样式也需明确“未接支付通道”标识 |
| 联调验证 | 按要求不启动后端、不连接共享数据库 | 已通过前端构建与后端 `clean package -DskipTests`；接口行为待部署环境回归 |

## 7. 验证记录

- 前端：`cd snowy-admin-web && npm run build`（见提交说明/最终报告）。
- 后端：`JAVA_HOME=D:\IdeaProject\chameleon\.jdk\jdk-17 mvn -s D:\IdeaProject\chameleon\.jdk\maven-settings-local.xml clean package -DskipTests`。
- 未执行：数据库联调、接口回归、定时任务实跑。

## 8. 权限迁移（2026-10-27 补充）

新增接口同样受全局 URL 权限拦截，单靠方法上的 `@SaCheckPermission` 不够。已新增 `FinancePermissionMigration`：

- `/finance/report/compare`、`/finance/report/meta` 新增资源并复制 `/finance/report/summary` 的角色授权；
- `/finance/report/export/async/page` 新增资源并复制 `/finance/report/export/async/status` 的角色授权；
- 所有租户超管额外授权。

主流程无需手工执行权限 SQL。
