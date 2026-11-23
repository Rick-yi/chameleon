# Chameleon 教育系统

基于 [Snowy](https://gitee.com/xiaonuobase/snowy) 构建的**教培机构教育系统**（多租户 SaaS）。

> 上游基线：**Snowy v3.6.5**（tag `v3.6.5`，commit `2c9da36e218901c20cd3b261f33e75b89aeec984`）
> 上游仓库：<https://gitee.com/xiaonuobase/snowy.git> ｜ 上游文档：<https://xiaonuo.vip/doc>
> 许可证：Apache License 2.0（见 [LICENSE](LICENSE)）
> 架构与设计取舍见 [ARCHITECTURE.md](ARCHITECTURE.md)

---

## 一、这是什么

Chameleon 教育系统是把 Snowy v3.6.5 全量引入本仓库作为"底座"后的工程：

- 保留了 Snowy 的全部能力：RBAC 权限、机构/职位/用户/角色/资源、字典、配置、代码生成、文件、短信/邮件/推送、三方登录（OAuth/OIDC/SAML/CAS/JWT/钉钉/企微/飞书/微信…）、国密 SM2/SM3/SM4 前后端加解密、SSO、监控等；
- 把**面向用户的产品标识**改成了 Chameleon 教育系统（浏览器标题、系统名称、版权文案、默认密码、数据库名等）；
- 把**开发环境配置**落到本机可用的默认值（MySQL / Redis / 端口）；
- **不引入上游 git 历史**（本仓库为一个全新的初始提交），后续如需同步上游请按 [docs/03](docs/03-改名范围与上游同步.md) 手动比对。

## 二、功能特性

面向教培机构日常经营，覆盖「招生 → 报名收款 → 排课点名 → 课时账务 → 经营分析」全链路。

| 模块 | 能力 |
| --- | --- |
| 多租户 | 平台运营端管理租户开通/续费/停用、套餐限额与功能开关、平台账单、租户健康度 |
| 学员档案 | 标签、状态机（在读/停课/休学/退学/结课）、状态变更流水、动态时间线、导入导出 |
| 招生 CRM | 线索、跟进记录、试听预约、线索转学员 |
| 课务 | 课程、课包、班级（一对多/一对一班型）、排课（冲突检测/批量/复制上周/停课日/教师不可用）、点名消课 |
| 财务 | 收款台、订单与续费、退费（申请→审批→退款）、课时账户与流水、报表与对账 |
| 学杂费 | 校服/教材/活动费等一次性收费，独立成表与课包分账，不污染客单价与续费率 |
| 报表 | 经营看板、财务、教务、招生，支持日/月/年切换与下钻 |
| 课表 | 多条件筛选、分屏查看（最多 4 窗口） |
| 移动端 | `/app` 工作台、学员名册与详情、点名、课表、收款台、续费预警；详情内可调课包（预设或自定义） |

技术要点：

- **租户行级隔离**：白名单表由拦截器自动拼租户条件，启动时自检未登记表并告警；
- **启动迁移阶段化**：结构 → 权限 → 初始化 → 自检 → 定时任务，失败集中汇总而非静默；
- **账务不变量**：课时账户「剩余 = 总课时 − 已用」在退费路径实时校验；
- **金额口径收敛**：退费算式单一实现，申请/试算/审批共用，配有 10 个不变量用例。

## 三、技术栈

| 层 | 组件 | 版本 |
|---|---|---|
| 后端 | Spring Boot | 3.5.9 |
| 后端 | Java | **17** |
| 后端 | MyBatis-Plus | 3.5.5 |
| 后端 | Sa-Token（鉴权 / SSO / OAuth2） | 1.44.0 |
| 后端 | Redisson、Druid、dynamic-datasource | 3.45.0 / 1.2.21 / 4.3.1 |
| 后端 | Knife4j（OpenAPI 文档） | 4.5.0 |
| 后端 | MySQL 驱动 | 8.3.0（服务端 8.0 / 5.7） |
| 前端 | Vue | 3.5.13 |
| 前端 | Ant Design Vue | 4.2.6 |
| 前端 | Vite / Node.js | 6.0.7 / **≥ 18** |
| 前端 | Pinia / Vue Router | 2.2.2 / 4.4.5 |

## 四、目录结构

```
chameleon
├── snowy-admin-web      前端工程（Vue3 + Antdv4 + Vite）
├── snowy-common         基础通用模块（工具、加解密、异常、基类）
├── snowy-plugin         插件实现（auth/biz/client/dev/gen/mobile/sys）
├── snowy-plugin-api     插件对外 API 包（跨插件调用的接口契约）
├── snowy-web-app        主启动模块（Spring Boot 入口 + 配置文件 + SQL）
├── docs                 本项目的环境准备 / 启动 / 改造说明
└── pom.xml              Maven 聚合 POM
```

> 说明：Maven 模块目录名与 Java 包名 `vip.xiaonuo.*` 保持上游原样（原因见 [docs/03](docs/03-改名范围与上游同步.md)）。

## 五、环境要求

| 组件 | 要求 |
|---|---|
| JDK | **17**（必须） |
| Maven | 3.8+ |
| Node.js | ≥ 18 |
| MySQL | 8.0 或 5.7 |
| Redis | 6/7 均可 |

详细安装步骤见 [docs/01-环境准备.md](docs/01-环境准备.md)。

## 六、快速开始

```bash
# 1. 初始化数据库：建库 + 按依赖顺序导入全部 SQL（含业务表、菜单、权限与演示数据）
#    需要 mysql 客户端在 PATH；Windows 可用 MYSQL_BIN 指定绝对路径
node scripts/db-init.mjs --user root --password 你的密码 --database chameleon
#    生产环境不要演示数据：追加 --skip-demo

#    注意：只导入 _sql/snowy_mysql.sql 是不够的——它只建底座表，
#    业务表（COURSE / STUDENT / COURSE_CLASS …）在 chameleon_*.sql 里。

# 2. 按需修改数据库/Redis 连接
#    snowy-web-app/src/main/resources/application.properties

# 3. 启动后端（默认 82 端口）
mvn clean package -DskipTests
java -jar snowy-web-app/target/snowy-web-app-3.0.0.jar

# 4. 启动前端（默认 81 端口，开发代理到 82）
cd snowy-admin-web
npm install
npm run dev
```

启动后访问：

| 用途 | 地址 |
|---|---|
| 管理后台 | <http://localhost:81> |
| 后端接口 | <http://localhost:82> |
| Knife4j 接口文档 | <http://localhost:82/doc.html>（账号 `admin` / `123456`） |

### 默认账号

登录页需要填写**机构编码**（演示机构为 `demo`）。

| 机构编码 | 账号 | 初始密码 | 说明 |
|---|---|---|---|
| `demo` | `superAdmin` | `Chameleon@2026!` | 演示机构超级管理员 |
| `demo` | `bizAdmin` | `Chameleon@2026!` | 演示机构业务管理员 |
| `platform` | `platformAdmin` | `Chameleon@2026!` | 平台运营（租户/套餐管理） |

> 初始密码等于系统"默认密码"配置，因此**首次登录会被判定为密码过期并要求修改**（`SysPasswordUtil.isUserPasswordExpired`），这是上游设计而非故障。修改后前端会写入新密码。

## 七、默认端口与连接配置

| 配置项 | 默认值 | 位置 |
|---|---|---|
| 后端端口 | `82` | `snowy-web-app/src/main/resources/application.properties` |
| 前端端口 | `81` | `snowy-admin-web/.env.development`（`VITE_PORT`） |
| 前端 API 基址 | `http://127.0.0.1:82` | 同上（`VITE_API_BASEURL`），Vite 将 `/api` 代理并去掉前缀 |
| MySQL | `localhost:3306/chameleon`，`root` / `12345678` |
| Redis | `127.0.0.1:6379`，`database=1`，无密码 |
| 激活 profile | `local`（无对应 `application-local.*` 文件时即使用 `application.properties`） | `application.properties` |

> 建议：如需保存个人连接信息而不污染仓库，可在 `snowy-web-app/src/main/resources/` 下新建 `application-local.yml`（Spring Boot 会自动按 profile 覆盖），并把它加入 `.gitignore`。

## 八、本次改造记录（相对上游 v3.6.5）

**改为 Chameleon 教育系统的内容**

| 文件 | 改动 |
|---|---|
| `README.md` | 全量重写为本项目说明 |
| `snowy-admin-web/index.html` | 浏览器标题、`noscript` 文案 → Chameleon 教育系统 |
| `snowy-admin-web/src/config/index.js` | 系统名称 / 版权文案 → Chameleon 教育系统（默认口令 `Chameleon@2026!` 保持不变） |
| `snowy-admin-web/package.json` | 包名 `chameleon-admin-web`、描述 |
| `snowy-web-app/.../application.properties` | `spring.application.name=chameleon`；JDBC 库名 `snowy`→`chameleon` |
| `snowy-web-app/.../application-docker.yml` | `MYSQL_DB` 默认值 → `chameleon` |
| `snowy-web-app/.../docker-compose.yml` | 服务/容器/网络名、库名 → `chameleon-*`（根目录已提供多阶段 `Dockerfile`，build 段可用） |
| `snowy-web-app/.../_sql/snowy_mysql.sql` | `DEV_CONFIG` 中系统名称/版权/描述/默认密码的种子值；`SYS_USER` 中两个种子账号的密码哈希（SM3） |
| `docs/*` | 新增环境准备、启动验证、改造说明、方案与部署文档 |
| `Dockerfile` | 新增多阶段构建（maven 编译 + JRE 运行），供 compose 部署 |

**刻意保留、未改名的内容**

- 所有源文件头部的 Snowy 版权与作者声明（Apache-2.0 与上游条款要求保留）；
- `LICENSE`、Knife4j 页脚署名 `Copyright 2020-2024[SNOWY]`；
- Java 包名 `vip.xiaonuo.*`、Maven `groupId`/`artifactId`、模块目录名 `snowy-*`；
- 配置键命名空间 `SNOWY_*`、`snowy.config.*`、CSS 变量 `--snowy-*`、`snowy-admin-web/src/snowy.js`、代码生成器默认插件名等；
- 前端 logo 图片资源（`public/img/logo.png` 与 `DEV_CONFIG.SNOWY_SYS_LOGO` 的 base64）仍为上游素材，**请替换为你自己的商标**。

## 九、测试与验证

| 项目 | 命令 | 说明 |
| --- | --- | --- |
| 后端单元测试 | `mvn test` | **119** 个用例，纯逻辑不连库，CI 默认执行 |
| 集成测试 | `./scripts/it.sh` | **9** 个用例（并发扣课不超扣、退费扣减一致性），需本机 MySQL 与独立库 `chameleon_it` |
| 前端代码规范 | `cd snowy-admin-web && npm run lint` | 0 error；CI 为阻断项 |
| 前端生产构建 | `cd snowy-admin-web && npm run build` | 通过 |

集成测试会真实写库，因此使用独立的 `chameleon_it` 库，初始化与运行方式见 [CONTRIBUTING.md](CONTRIBUTING.md)。

> 仓库不包含便携 JDK、本地截图、`.env` 等本机产物，这些已在 `.gitignore` 中排除。

## 十、文档索引

### 快速上手

- [01-环境准备](docs/01-环境准备.md) —— JDK 17 / Maven / Node / MySQL / Redis 安装与自检
- [02-启动与验证](docs/02-启动与验证.md) —— 建库导入、启动前后端、登录验收清单、常见问题
### 核心设计

- [ARCHITECTURE.md](ARCHITECTURE.md) —— 模块划分、多租户隔离、账务模型、启动流程与关键取舍
- [04-教培SaaS产品方案](docs/04-教培SaaS产品方案.md) —— 产品定位、模块划分、数据模型与里程碑
- [05-核心业务逻辑细化](docs/05-核心业务逻辑细化.md) —— 课时账务、排课、退费、续费预警的业务规则
- [09-主体流程](docs/09-主体流程.md) —— 十条主干业务流程（工作台/租户运营/招生/报名收款/排课/点名消课/退费续费/报表/权限/通知）
### 专题说明

- [06-P0完成说明](docs/06-P0完成说明.md) —— 多租户开通闭环、平台鉴权、退费修正、定时任务与 v1 支付范围
- [07-报表与看板规划](docs/07-报表与看板规划.md) —— 财务/教务/招生/平台看板的指标口径、页面、接口与里程碑
- [11-自动化规则](docs/11-自动化规则.md) —— 自动化中心：规则清单、SPI 扩展方式、定时执行与执行记录
- [12-性能与容量](docs/12-性能与容量.md) —— 10 机构 × 800 学员压测：问题清单、优化前后对比与容量基线
- [13-移动端原型](docs/13-移动端原型.md) —— 移动端 App 原型：角色视角、信息架构、主流程与 Android 壳方案
- [14-权限体系](docs/14-权限体系.md) —— 教培角色体系：岗位角色、菜单/接口/数据范围与越权兜底
- [15-数据治理与审计](docs/15-数据治理与审计.md) —— 埋点归档、权限变更审计与按钮码修正
- [16-排课助手与转化自动化](docs/16-排课助手与转化自动化.md) —— 推荐可排课时段、试听结束自动推进、数据库一键初始化与 CI
- [17-并发约束与异步导出](docs/17-并发约束与异步导出.md) —— 排课唯一约束与冲突明细、移动端消息中心、通用异步导出
- [26-生产试用体验问题与需求清单](docs/26-生产试用体验问题与需求清单.md) —— 按角色梳理的试用问题、优先级与需求
### 运维与部署

- [08-部署与运维](docs/08-部署与运维.md) —— 部署方式、数据库初始化顺序、环境变量、备份恢复与巡检
- [03-改名范围与上游同步](docs/03-改名范围与上游同步.md) —— 改名边界、为什么不改包名、如何同步上游、如何做深度改名
- [SECURITY.md](SECURITY.md)
- [CHANGELOG.md](CHANGELOG.md)
- [.env.example](.env.example)
### 开发规范

- [10-界面设计规范](docs/10-界面设计规范.md) —— 企业级 UI 规范：设计令牌、页面三段式结构、组件规整与图表配色
- [CONTRIBUTING.md](CONTRIBUTING.md)
### 模块专题

- [crm](docs/_streams/crm.md) —— CRM 查重、跟进分页、试听视图与工作台内联跟进
- [sched](docs/_streams/sched.md) —— 课表到课进度、按钮收敛、冲突预检与复制上周
- [renew](docs/_streams/renew.md) —— 续费阈值、提醒/转顾问、风险学员、快照与流失记录
- [student](docs/_streams/student.md) —— 学员标签、状态机、导入导出与动态时间线
- [finance](docs/_streams/finance.md) —— 财务指标口径、同比环比、导出记录与危险操作收口
- [platform](docs/_streams/platform.md) —— 平台套餐开关、经营指标、租户健康度与内部账单
### 迭代记录

按开发顺序记录每轮改动的背景与验证，属于历史细节，按需查阅。

<details>
<summary>展开 49 篇迭代记录</summary>

- [18-报表优化](docs/18-报表优化.md) —— 缓存自动失效、漏斗口径单调、KPI 环比与查询瘦身
- [19-课表操作优化](docs/19-课表操作优化.md) —— 取消排课入口、批量取消与生成冲突明细
- [20-停课日与教师不可用](docs/20-停课日与教师不可用.md) —— 节假日停课、教师不可用时段与排课推荐联动
- [21-排课并发与课表导出](docs/21-排课并发与课表导出.md) —— 教师/教室并发唯一约束、教室大小写归一与课表异步导出
- [22-点名与学员详情体验](docs/22-点名与学员详情体验.md) —— 点名方块交互、状态循环与学员详情加载修复
- [23-移动端适配](docs/23-移动端适配.md) —— 手机浏览器下的页头、筛选、表格、弹窗与悬浮菜单适配
- [24-儿童风主题](docs/24-儿童风主题.md) —— 果子绿 + 爱马仕橙配色、大圆角与主题配置位置
- [25-工作台快速录入学员](docs/25-工作台快速录入学员.md) —— 建档 + 选课包 + 收款一次完成，含渠道与回滚说明
- [27-报名稳定性加固](docs/27-报名稳定性加固.md) —— 提交幂等、重复手机号提醒、草稿恢复与登录回跳
- [28-平台租户运营闭环](docs/28-平台租户运营闭环.md) —— 租户续费、停用/启用、重置管理员密码与登录/续费记录
- [29-学员关键数据可见](docs/29-学员关键数据可见.md) —— 列表与详情展示剩余课时、课包到期、班级、待收款与风险标签
- [30-工作台待办闭环](docs/30-工作台待办闭环.md) —— 今日教学指标、逾期提示、收款/审批/跟进直达与今日收款口径修正
- [31-点名汇总与请求解包修复](docs/31-点名汇总与请求解包修复.md) —— 已点名/待提交分组、跳过原因常驻与前端 res.data 解包修复
- [32-手机点名体验改造](docs/32-手机点名体验改造.md) —— 390px 确认按钮全宽、44px 触控、16px 输入、对比度与移动端外壳修复
- [33-学员列表移动卡片](docs/33-学员列表移动卡片.md) —— 手机端学员卡片，首屏可见剩余课时/到期/班级/待收款与拨号
- [34-主按钮可读性统一](docs/34-主按钮可读性统一.md) —— 果子绿底深绿字，主按钮对比度从 2.49:1 提升到 5.0:1
- [35-报名流程统一](docs/35-报名流程统一.md) —— 工作台、学员列表、学员详情共用三步报名流程，成功后展示订单详情
- [36-语义文字色治理](docs/36-语义文字色治理.md) —— 次要/警告/危险/成功文字统一 token，白底对比度提升到 4.8:1 以上
- [37-租户边界与敏感日志加固](docs/37-租户边界与敏感日志加固.md) —— 权限审计/导出任务租户隔离、历史日志密码脱敏与签名重算
- [38-校区必填与订单字段补齐](docs/38-校区必填与订单字段补齐.md) —— 校区必填与历史回填，订单号/收款时间/优惠/经办人/备注补齐
- [39-自动化可靠性升级](docs/39-自动化可靠性升级.md) —— 预览/参数护栏、租约锁、目标级幂等、触达回执与 holdout 归因
- [40-APP工作台对齐](docs/40-APP工作台对齐.md) —— APP 保留自有页面结构，功能对齐 PC 工作台，补收款/退费/跟进/报名/续费直达
- [41-课包赠送时长与课时校验](docs/41-课包赠送时长与课时校验.md) —— 课包赠送时长、总有效期叠加，正课/赠课不设上限
- [42-老师管理](docs/42-老师管理.md) —— 老师账号、校区、带班统计、停用启用与重置密码，PC 表格与 APP/手机卡片适配
- [43-全链路回归与极简操作](docs/43-全链路回归与极简操作.md) —— APP 学员新增/订单详情补齐，校区过滤班级、唯一选项自动带出、草稿与优惠显示修正
- [44-APP收款台搜索](docs/44-APP收款台搜索.md) —— APP 收款台按姓名/手机/订单号搜索，订单与退费单共用关键词
- [45-APP收款台已收款卡片](docs/45-APP收款台已收款卡片.md) —— 已收款卡片首屏显示实收金额与收款渠道，未收款显示应收
- [46-APP学员卡片关键数据](docs/46-APP学员卡片关键数据.md) —— 学员名册卡片首屏显示剩余课时、所在班级、课包到期与风险着色
- [47-班级容量展示](docs/47-班级容量展示.md) —— 班级列表显示在学/容量、剩余名额与满班预警色
- [48-APP首页收款口径](docs/48-APP首页收款口径.md) —— APP 今日收款改用财务日报汇总，避免只统计最近 20 条订单
- [49-APP退费卡申请信息](docs/49-APP退费卡申请信息.md) —— 退费单卡片显示申请时间与已有审批意见
- [50-退费原因与试算](docs/50-退费原因与试算.md) —— 退费原因独立落库，申请前展示可退课时/赠课/预计退款
- [51-线索跟进快捷视图](docs/51-线索跟进快捷视图.md) —— 线索列表新增全部/待跟进/今日/已逾期快捷筛选
- [52-课表拖拽调课](docs/52-课表拖拽调课.md) —— 周视图课节可拖拽换日期/时段，带停课日、冲突与时间校验
- [53-报表日月年维度](docs/53-报表日月年维度.md) —— 报表与工作台支持日/月/年周期切换
- [54-全局复测与问题修复](docs/54-全局复测与问题修复.md) —— 全链路复测与问题修复
- [55-班级管理页签切换](docs/55-班级管理页签切换.md) —— 班级管理一对多/一对一页签
- [56-班级管理筛选栏](docs/56-班级管理筛选栏.md) —— 班级筛选：老师/课程/教室/满班/已排课
- [57-课表多条件筛选与分屏](docs/57-课表多条件筛选与分屏.md) —— 课表六项筛选与分屏查看
- [58-安全性能与口径加固](docs/58-安全性能与口径加固.md) —— 关闭 Druid 暴露、消除 N+1、退费算式收敛
- [59-前端存量格式化与lint阻断](docs/59-前端存量格式化与lint阻断.md) —— 前端存量格式化，lint 进 CI
- [60-启动可靠性与前端安全加固](docs/60-启动可靠性与前端安全加固.md) —— 迁移失败可见、公告 XSS 净化、续费配置测试
- [61-启动迁移阶段化](docs/61-启动迁移阶段化.md) —— StartupPhase 显式阶段链
- [62-核心业务流程补测](docs/62-核心业务流程补测.md) —— 续费预警与退费的判定规则测试
- [63-账务不变量校验](docs/63-账务不变量校验.md) —— 课时账户余额自洽校验
- [64-集成测试基座](docs/64-集成测试基座.md) —— 并发扣课与退费一致性（独立测试库）
- [65-学杂费与学员详情重点信息](docs/65-学杂费与学员详情重点信息.md) —— 学杂费分账与学员详情 KPI
- [66-学员详情页性能优化](docs/66-学员详情页性能优化.md) —— 页签懒加载与服务端分页
- [67-移动端调课包与自定义课包](docs/67-移动端调课包与自定义课包.md) —— 移动端调课包与自定义课包

</details>

## 十一、版权与出处

本项目为 Snowy 的衍生工程，遵循 Apache License 2.0。

- Snowy 版权归属：小诺开源技术（<https://www.xiaonuo.vip>）
- 请勿删除或修改源码头部的版权与作者声明及出处；
- 二次开源不得参与同类竞争（上游条款）。
- 分发时须同时保留 `LICENSE` 与 `NOTICE`。

Copyright [2022] [https://www.xiaonuo.vip]

## 十二、安全须知（部署前必读）

**仓库内的账号与口令全部是「本地初始化用的默认值」，不具备任何安全性。**

### 会被公开的默认值

| 位置 | 内容 | 说明 |
| --- | --- | --- |
| `_sql/snowy_mysql.sql` | `SNOWY_SYS_DEFAULT_PASSWORD_FOR_B/C` | 新建用户的默认口令 |
| `_sql/snowy_mysql.sql` | 内置 `superAdmin` 的口令哈希 | 无盐 SM3，明文可见于文档 |
| `application.properties` | 本地 MySQL `root` 口令 | 仅 localhost 开发用 |
| `docker-compose.yml` | compose 默认数据库口令 | 仅演示用 |

### 上公网前必须完成

1. **修改所有内置账号口令**：`superAdmin`、`platformAdmin` 及各机构管理员；
2. **修改默认口令配置**：`DEV_CONFIG` 中 `SNOWY_SYS_DEFAULT_PASSWORD_FOR_B/C`，否则新建用户永远是公开口令；
3. **改数据库口令**：通过环境变量注入，不要使用 compose 默认值；
4. **禁用演示租户与演示账号**；
5. **关闭 Druid 监控台**：`application-prod.yml` 已默认关闭，并确认反代层拒绝 `/druid/**`；
6. 关闭接口文档：生产已设 `knife4j.production=true`（`application-prod.yml`），确认 `/doc.html` 不可访问；
   本地调试用的 `knife4j.basic.username/password`（admin/123456）绝不能带到公网。
7. **启用 HTTPS**：反向代理只暴露前端静态资源与 `/api`。

### 不要提交的内容

`.env`、`*.pem`、`*.key`、`*.p12`、`application-local.properties` 已在 `.gitignore` 中排除。若曾误提交，仅删除文件不够——历史提交仍可检出，需要重写历史或轮换凭据。

