# 架构总览

本文说明系统的模块划分、多租户隔离、账务模型与启动流程。更细的业务规则见 [docs/05-核心业务逻辑细化.md](docs/05-核心业务逻辑细化.md)、[docs/14-权限体系.md](docs/14-权限体系.md)。

## 一、工程结构

```
chameleon/
├── snowy-common                     通用能力：上下文、租户、异常、分页、Excel、事件
├── snowy-plugin-api/                各插件的对外接口（API + DTO），供跨插件调用
│   ├── snowy-plugin-sys-api         用户/角色/机构
│   ├── snowy-plugin-student-api     学员/课时/订单 DTO
│   ├── snowy-plugin-course-api      课程/班级/排课 DTO
│   └── ...
├── snowy-plugin/                    业务插件（按域拆分，互不直接依赖实现）
│   ├── snowy-plugin-sys             用户、机构、职位、角色、资源、关系
│   ├── snowy-plugin-auth            登录、鉴权、SSO、第三方登录
│   ├── snowy-plugin-student         学员、订单、课时账户、退费、学杂费、续费预警
│   ├── snowy-plugin-course          课程、课包、班级、排课、点名
│   ├── snowy-plugin-crm             线索、跟进、试听
│   ├── snowy-plugin-finance         财务汇总、导出
│   ├── snowy-plugin-platform        多租户运营、套餐、平台账单
│   ├── snowy-plugin-biz             公告、自动化规则、导出任务
│   └── snowy-plugin-dev             字典、配置、文件、定时任务
├── snowy-web-app                    启动模块：配置、过滤器、启动迁移、打包
├── snowy-admin-web                  前端（Vue3 + Antdv4 + Vite），含 PC 与 `/app` 移动端
└── app                              Android WebView 壳（不含业务逻辑）
```

插件之间**只通过 `*-api` 调用**，不直接依赖对方的 Service，避免域与域缠死。

## 二、请求链路

```
HTTP → TenantContextFilter（从登录会话取 tenantId 写入 ThreadLocal）
     → Sa-Token 鉴权链（登录校验 → 超管/平台角色 → 业务域接口权限兜底）
     → Controller → Service → Mapper
                          └→ MyBatis-Plus 插件链：
                             TenantLineInnerInterceptor（自动拼 TENANT_ID）
                             PaginationInnerInterceptor
```

关键点：

- **租户只认登录会话**，不认请求头，防止伪造 `X-Tenant-*` 越权（登录前的机构识别除外）。
- 请求结束在 `finally` 中 `TenantContext.clear()`；异步/定时任务必须自己设置和清理。

## 三、多租户模型

采用**共享库 + `TENANT_ID` 行级隔离**：

| 机制 | 位置 |
| --- | --- |
| 白名单（哪些表参与自动隔离） | `GlobalConfigure.TENANT_TABLES` |
| 条件注入 | MyBatis-Plus `TenantLineInnerInterceptor` |
| 上下文 | `TenantContext`（ThreadLocal，兜底值 `"0"`） |
| 启动自检 | `TenantIsolationAudit` 比对 `INFORMATION_SCHEMA`，未登记表告警 |

**新增业务表必须登记白名单**，否则该表不参与自动隔离，需要在服务层显式 `.eq(tenantId)`。未登记时的兜底是「依赖人工」，这是已知取舍，所以加了启动自检。

平台侧表（`T_TENANT*`、`T_PLATFORM_*`、`DEV_JOB`）不参与隔离：`DEV_JOB` 的 `TENANT_ID` 只标记任务归属，调度器需要在无租户上下文时扫描全部租户任务。

## 四、账务模型

四条主线，全部以「一手证据（订单/流水）+ 不变量」为准则。

```
报名/续费                   点名                         退费
  │                          │                            │
  ▼                          ▼                            ▼
STUDENT_ORDER  ──实收入账──► STUDENT_LESSON_ACCOUNT ◄──扣减──  STUDENT_REFUND
（课包快照）                 TOTAL / USED / REMAIN / GIFT        （申请→审批→退款）
  │                          │                            │
  └──────────► STUDENT_LESSON_FLOW（消课/回充/退费流水，按订单分摊）
```

- **订单即快照**：下单时把课包课时、赠课、有效期、金额写入订单，课包后续改价不影响已购；收款后 `recharge(order)` 读的也是订单快照。
- **课时账户不变量**：`REMAIN = TOTAL − USED`。由点名扣课、回充、报名、退费四条 SQL 分别维护，`LessonAccountInvariant` 在退费路径实时校验，漂移只告警不阻断。
- **金额口径单一实现**：`RefundAmountCalculator` 计算「折算单价（4 位）→ 退费金额（2 位）」，申请/试算/审批共用，配 10 个不变量用例。
- **并发保护**：所有改动余额的更新都是条件更新（`eq(状态).ge(余额, 扣减量)`）并校验受影响行数，不做「先查再改」。
- **学杂费分账**：`STUDENT_SUNDRY_ORDER` 独立成表，不产生课时、不参与课消，财务汇总单独成栏，避免污染客单价与续费率。

## 五、排课与点名

- 班级（`COURSE_CLASS`）挂课程、教师、教室、容量；班型支持一对多/一对一。
- 课节（`COURSE_SCHEDULE`）由「班级 + 日期 + 时段」构成，靠唯一约束防教师/教室/班级并发撞车。
- 名单 = 班级在读学员 ∪ 该课节直接指定学员（`COURSE_SCHEDULE_STUDENT`）。
- 点名（`COURSE_CLASS_ATTEND`）按课包规则决定是否扣课（`AttendDeductRule`），同课节同学员唯一，重复提交不重复扣课。
- 余额不足/账户冻结时**保留出勤记录但不消课**，并写明原因，不影响同批其他学员。

## 六、权限模型

三层叠加：

1. **菜单可见性**：`SYS_RESOURCE`（目录/菜单/按钮）+ `SYS_RELATION`（角色-资源）。
2. **接口权限**：业务域路径（`/student/**`、`/course/**` 等）必须在当前用户的 `permissionCodeList` 内，**精确匹配 apiUrl**。这是兜底——即使某个接口漏标 `@SaCheckPermission` 也拦得住。
3. **数据范围**：`SCOPE_ALL / SCOPE_ORG / SCOPE_ORG_CHILD / SCOPE_SELF`，落到 `StudentDataScopeService` 等工具。

新增接口必须登记权限（参考 `StudentSundryPermissionMigration`），否则非超管账号直接 403。权限由启动迁移自动写入 `SYS_RESOURCE` + `SYS_RELATION`，并按「已有同类权限的角色」派生授权。

## 七、启动流程

用 `StartupPhase` 显式阶段链替代原先的魔数排序：

```
BASE_SCHEMA        底座补表补列（P0SchemaMigration）
PLATFORM_SCHEMA    平台运营表
DOMAIN_SCHEMA      各业务域建表/补列
DOMAIN_PERMISSION  接口权限与菜单按钮
PLATFORM_BOOTSTRAP 平台账号初始化
AUDIT              租户隔离自检 + 迁移失败汇总
TIMER_START        定时任务
```

- 迁移**幂等**：缺表建表、缺列补列，可重复执行；因此没有引入 Flyway。
- 失败**不静默**：迁移监听器失败时登记到 `StartupMigrationRegistry`，启动末尾统一输出「哪几项没成功、要手工跑什么 SQL」，应用照常启动。

## 八、前端

- 技术栈：Vue 3 + Ant Design Vue 4 + Vite 6 + Pinia，动态路由由后端菜单生成。
- PC 与移动端共用一套工程：`/app/*` 是移动端页面，Android 壳只加载 WebView。
- 权限：按钮级用 `hasPerm('/接口路径')` 控制显隐。
- 安全：`v-html` 渲染的富文本（公告等）必须过 `@/utils/sanitize`（dompurify）。
- 交互约定：页签分段 + 无边框筛选即时生效 + 重置；课表支持分屏对比；报表支持日/月/年切换。

## 九、关键设计取舍

| 取舍 | 原因 |
| --- | --- |
| 共享库行级隔离，而非分库 | 中小机构数量多、单租户数据量小，分库运维成本高；用白名单 + 自检控制风险 |
| 启动期幂等迁移，而非 Flyway | 早期多环境并存、手工补 SQL 频繁；代价是迁移顺序要靠阶段链约束 |
| 学杂费独立成表，而非加订单类型 | 混表要求每处查询都加类型过滤，漏一处就静默污染报表 |
| 订单快照，而非实时读课包 | 改价/删课包不能影响已购记录与退费核算 |
| 条件更新 + 不变量，而非事务锁 | 高并发下避免锁等待，同时用返回行数与不变量兜住正确性 |
