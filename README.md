# Chameleon

基于 [Snowy](https://gitee.com/xiaonuobase/snowy) 开源快速开发平台构建的业务中台基础工程。

> 上游基线：**Snowy v3.6.5**（tag `v3.6.5`，commit `2c9da36e218901c20cd3b261f33e75b89aeec984`）
> 上游仓库：<https://gitee.com/xiaonuobase/snowy.git> ｜ 上游文档：<https://xiaonuo.vip/doc>
> 许可证：Apache License 2.0（见 [LICENSE](LICENSE)）

---

## 一、这是什么

Chameleon 是把 Snowy v3.6.5 全量引入本仓库作为"底座"后的工程：

- 保留了 Snowy 的全部能力：RBAC 权限、机构/职位/用户/角色/资源、字典、配置、代码生成、文件、短信/邮件/推送、三方登录（OAuth/OIDC/SAML/CAS/JWT/钉钉/企微/飞书/微信…）、国密 SM2/SM3/SM4 前后端加解密、SSO、监控等；
- 把**面向用户的产品标识**改成了 Chameleon（浏览器标题、系统名称、版权文案、默认密码、数据库名等）；
- 把**开发环境配置**落到本机可用的默认值（MySQL / Redis / 端口）；
- **不引入上游 git 历史**（本仓库为一个全新的初始提交），后续如需同步上游请按 [docs/03](docs/03-改名范围与上游同步.md) 手动比对。

## 二、技术栈

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

## 三、目录结构

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

## 四、环境要求

| 组件 | 要求 | 本机现状（截至交付时） |
|---|---|---|
| JDK | **17**（必须） | 仅 1.8 ❌ 需自行安装 |
| Maven | 3.8+ | 3.9.7 ✅ |
| Node.js | ≥ 18 | v22.14.0 ✅ |
| MySQL | 8.0 或 5.7 | 未安装 ❌ |
| Redis | 6/7 均可 | 未安装 ❌ |

详细安装步骤见 [docs/01-环境准备.md](docs/01-环境准备.md)。

## 五、快速开始

```bash
# 1. 建库并导入初始数据（脚本内不含 CREATE DATABASE / USE 语句）
mysql -uroot -p -e "CREATE DATABASE chameleon DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -uroot -p chameleon < snowy-web-app/src/main/resources/_sql/snowy_mysql.sql

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

## 六、默认端口与连接配置

| 配置项 | 默认值 | 位置 |
|---|---|---|
| 后端端口 | `82` | `snowy-web-app/src/main/resources/application.properties` |
| 前端端口 | `81` | `snowy-admin-web/.env.development`（`VITE_PORT`） |
| 前端 API 基址 | `http://127.0.0.1:82` | 同上（`VITE_API_BASEURL`），Vite 将 `/api` 代理并去掉前缀 |
| MySQL | `localhost:3306/chameleon`，`root` / `12345678` | `application.properties` |
| Redis | `127.0.0.1:6379`，`database=1`，无密码 | `application.properties` |
| 激活 profile | `local`（无对应 `application-local.*` 文件时即使用 `application.properties`） | `application.properties` |

> 建议：如需保存个人连接信息而不污染仓库，可在 `snowy-web-app/src/main/resources/` 下新建 `application-local.yml`（Spring Boot 会自动按 profile 覆盖），并把它加入 `.gitignore`。

## 七、本次改造记录（相对上游 v3.6.5）

**改为 Chameleon 的内容**

| 文件 | 改动 |
|---|---|
| `README.md` | 全量重写为本项目说明 |
| `snowy-admin-web/index.html` | 浏览器标题、`noscript` 文案 → Chameleon |
| `snowy-admin-web/src/config/index.js` | 系统名称 / 版权文案 / 默认重置密码 → Chameleon |
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

## 八、验证状态

| 项目 | 状态 |
|---|---|
| 文件树与上游 v3.6.5 一致性（1745 个文件全量落地） | ✅ 已核对（逐文件 SHA256，0 缺失） |
| 前端依赖安装与生产构建 `npm run build` | ✅ 已实测（1134 包，5320 模块，产物 870 文件） |
| 后端 Maven 编译 `mvn compile` | ✅ 已实测（JDK 17.0.20.1，18 个模块全部 SUCCESS） |
| 后端打包 `mvn package`（可执行 jar） | ✅ 已实测（`snowy-web-app-3.0.0.jar`，194.8 MiB，含 `BOOT-INF/classes/vip/xiaonuo/Application.class`） |
| 数据库脚本导入 | ✅ 已实测（760 条语句 0 失败，33 张表，`SYS_USER` 2 行，`SNOWY_SYS_NAME=Chameleon`） |
| 后端启动 | ✅ 已实测（`Started Application in 47.987 seconds`，`http://localhost:82`，Knife4j `/doc.html` 200） |
| **登录进系统管理** | ✅ 已实测（`superAdmin` / `Chameleon@2026!` 取得 token；`getLoginUser`、`loginMenu`、`sys/user/page` 均返回真实数据） |
| 前端开发服务器 `npm run dev` | ✅ 已实测（生产构建 + 静态服务 + Chrome CDP 浏览器验收） |
| `mvn test` | ✅ 已实测（24 个用例全绿：扣课规则、收入确认、课时分摊、租户能力、资源可见性、点名状态、Excel 导出） |
| 业务闭环 GUI 验收 | ✅ 已实测（招生/学员/课程/课表/点名/收款/报表/租户等页面，截图见 `gui-test-screenshots/`） |
| 易用性改造（工作台 / 收款台 / 一键点名 / 结构化课表） | ✅ 已实测（后端冒烟 17/17 通过，页面无控制台错误） |

> 本仓库内置了一份便携 JDK 17（`.jdk/jdk-17`，已被 `.gitignore` 忽略），
> 用法见 [docs/01-环境准备.md](docs/01-环境准备.md) 第 1.1 节。

> 上表的数据库与登录验证使用**外部 MySQL / Redis 实例**，连接信息通过环境变量
> （`SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*`）注入，未写入任何受版本控制的文件。
> 复现步骤见 [docs/02-启动与验证.md](docs/02-启动与验证.md)。
>
> 也可在**本机自建** MySQL 8 + Redis 5（无需外部实例）：按 `docs/08-部署与运维.md`
> 「本地自建数据库」一节初始化后，直接用仓库默认配置（`localhost:3306` / `localhost:6379`）启动即可。

## 九、文档索引

- [docs/01-环境准备.md](docs/01-环境准备.md) —— JDK 17 / Maven / Node / MySQL / Redis 安装与自检
- [docs/02-启动与验证.md](docs/02-启动与验证.md) —— 建库导入、启动前后端、登录验收清单、常见问题
- [docs/03-改名范围与上游同步.md](docs/03-改名范围与上游同步.md) —— 改名边界、为什么不改包名、如何同步上游、如何做深度改名
- [docs/04-教培SaaS产品方案.md](docs/04-教培SaaS产品方案.md) —— 产品定位、模块划分、数据模型与里程碑
- [docs/05-核心业务逻辑细化.md](docs/05-核心业务逻辑细化.md) —— 课时账务、排课、退费、续费预警的业务规则
- [docs/06-P0完成说明.md](docs/06-P0完成说明.md) —— 多租户开通闭环、平台鉴权、退费修正、定时任务与 v1 支付范围
- [docs/07-报表与看板规划.md](docs/07-报表与看板规划.md) —— 财务/教务/招生/平台看板的指标口径、页面、接口与里程碑
- [docs/08-部署与运维.md](docs/08-部署与运维.md) —— 部署方式、数据库初始化顺序、环境变量、备份恢复与巡检
- [docs/09-主体流程.md](docs/09-主体流程.md) —— 十条主干业务流程（工作台/租户运营/招生/报名收款/排课/点名消课/退费续费/报表/权限/通知）
- [docs/10-界面设计规范.md](docs/10-界面设计规范.md) —— 企业级 UI 规范：设计令牌、页面三段式结构、组件规整与图表配色
- [docs/11-自动化规则.md](docs/11-自动化规则.md) —— 自动化中心：规则清单、SPI 扩展方式、定时执行与执行记录
- [docs/12-性能与容量.md](docs/12-性能与容量.md) —— 10 机构 × 800 学员压测：问题清单、优化前后对比与容量基线
- [docs/13-移动端原型.md](docs/13-移动端原型.md) —— 移动端 App 原型：角色视角、信息架构、主流程与 Android 壳方案
- [docs/14-权限体系.md](docs/14-权限体系.md) —— 教培角色体系：岗位角色、菜单/接口/数据范围与越权兜底
- [docs/15-数据治理与审计.md](docs/15-数据治理与审计.md) —— 埋点归档、权限变更审计与按钮码修正
- [docs/16-排课助手与转化自动化.md](docs/16-排课助手与转化自动化.md) —— 推荐可排课时段、试听结束自动推进、数据库一键初始化与 CI
- [docs/17-并发约束与异步导出.md](docs/17-并发约束与异步导出.md) —— 排课唯一约束与冲突明细、移动端消息中心、通用异步导出
- [docs/18-报表优化.md](docs/18-报表优化.md) —— 缓存自动失效、漏斗口径单调、KPI 环比与查询瘦身
- [docs/19-课表操作优化.md](docs/19-课表操作优化.md) —— 取消排课入口、批量取消与生成冲突明细
- [docs/20-停课日与教师不可用.md](docs/20-停课日与教师不可用.md) —— 节假日停课、教师不可用时段与排课推荐联动
- [docs/21-排课并发与课表导出.md](docs/21-排课并发与课表导出.md) —— 教师/教室并发唯一约束、教室大小写归一与课表异步导出
- [docs/22-点名与学员详情体验.md](docs/22-点名与学员详情体验.md) —— 点名方块交互、状态循环与学员详情加载修复
- [docs/23-移动端适配.md](docs/23-移动端适配.md) —— 手机浏览器下的页头、筛选、表格、弹窗与悬浮菜单适配
- [docs/24-儿童风主题.md](docs/24-儿童风主题.md) —— 果子绿 + 爱马仕橙配色、大圆角与主题配置位置
- [docs/25-工作台快速录入学员.md](docs/25-工作台快速录入学员.md) —— 建档 + 选课包 + 收款一次完成，含渠道与回滚说明
- [docs/26-生产试用体验问题与需求清单.md](docs/26-生产试用体验问题与需求清单.md) —— 按角色梳理的试用问题、优先级与需求
- [docs/27-报名稳定性加固.md](docs/27-报名稳定性加固.md) —— 提交幂等、重复手机号提醒、草稿恢复与登录回跳
- [docs/28-平台租户运营闭环.md](docs/28-平台租户运营闭环.md) —— 租户续费、停用/启用、重置管理员密码与登录/续费记录
- [docs/29-学员关键数据可见.md](docs/29-学员关键数据可见.md) —— 列表与详情展示剩余课时、课包到期、班级、待收款与风险标签
- [docs/30-工作台待办闭环.md](docs/30-工作台待办闭环.md) —— 今日教学指标、逾期提示、收款/审批/跟进直达与今日收款口径修正
- [docs/31-点名汇总与请求解包修复.md](docs/31-点名汇总与请求解包修复.md) —— 已点名/待提交分组、跳过原因常驻与前端 res.data 解包修复
- [docs/32-手机点名体验改造.md](docs/32-手机点名体验改造.md) —— 390px 确认按钮全宽、44px 触控、16px 输入、对比度与移动端外壳修复
- [docs/33-学员列表移动卡片.md](docs/33-学员列表移动卡片.md) —— 手机端学员卡片，首屏可见剩余课时/到期/班级/待收款与拨号
- [docs/34-主按钮可读性统一.md](docs/34-主按钮可读性统一.md) —— 果子绿底深绿字，主按钮对比度从 2.49:1 提升到 5.0:1
- [docs/35-报名流程统一.md](docs/35-报名流程统一.md) —— 工作台、学员列表、学员详情共用三步报名流程，成功后展示订单详情
- [docs/36-语义文字色治理.md](docs/36-语义文字色治理.md) —— 次要/警告/危险/成功文字统一 token，白底对比度提升到 4.8:1 以上
- [docs/37-租户边界与敏感日志加固.md](docs/37-租户边界与敏感日志加固.md) —— 权限审计/导出任务租户隔离、历史日志密码脱敏与签名重算
- [docs/38-校区必填与订单字段补齐.md](docs/38-校区必填与订单字段补齐.md) —— 校区必填与历史回填，订单号/收款时间/优惠/经办人/备注补齐
- [docs/39-自动化可靠性升级.md](docs/39-自动化可靠性升级.md) —— 预览/参数护栏、租约锁、目标级幂等、触达回执与 holdout 归因
- [docs/40-APP工作台对齐.md](docs/40-APP工作台对齐.md) —— APP 保留自有页面结构，功能对齐 PC 工作台，补收款/退费/跟进/报名/续费直达
- [docs/41-课包赠送时长与课时校验.md](docs/41-课包赠送时长与课时校验.md) —— 课包赠送时长、总有效期叠加，正课/赠课不设上限

## 十、版权与出处

本项目为 Snowy 的衍生工程，遵循 Apache License 2.0。

- Snowy 版权归属：小诺开源技术（<https://www.xiaonuo.vip>）
- 请勿删除或修改源码头部的版权与作者声明及出处；
- 二次开源不得参与同类竞争（上游条款）。

Copyright [2022] [https://www.xiaonuo.vip]
