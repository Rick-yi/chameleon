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
| `snowy-web-app/.../docker-compose.yml` | 服务/容器/网络名、库名 → `chameleon-*`（**注意：上游未提供根目录 Dockerfile，该文件的 build 段当前不可用**） |
| `snowy-web-app/.../_sql/snowy_mysql.sql` | `DEV_CONFIG` 中系统名称/版权/描述/默认密码的种子值；`SYS_USER` 中两个种子账号的密码哈希（SM3） |
| `docs/*` | 新增环境准备、启动验证、改造说明三份文档 |

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
| 前端开发服务器 `npm run dev` | ⚠️ 未验证（接口层已验证，浏览器端视觉验收待做） |
| `mvn test` | ⚠️ 未验证。`snowy-web-app/src/test` 为 JUnit 4 空测试，依赖已具备 |

> 本仓库内置了一份便携 JDK 17（`.jdk/jdk-17`，已被 `.gitignore` 忽略），
> 用法见 [docs/01-环境准备.md](docs/01-环境准备.md) 第 1.1 节。

> 上表的数据库与登录验证使用**外部 MySQL / Redis 实例**，连接信息通过环境变量
> （`SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*`）注入，未写入任何受版本控制的文件。
> 复现步骤见 [docs/02-启动与验证.md](docs/02-启动与验证.md)。

## 九、文档索引

- [docs/01-环境准备.md](docs/01-环境准备.md) —— JDK 17 / Maven / Node / MySQL / Redis 安装与自检
- [docs/02-启动与验证.md](docs/02-启动与验证.md) —— 建库导入、启动前后端、登录验收清单、常见问题
- [docs/03-改名范围与上游同步.md](docs/03-改名范围与上游同步.md) —— 改名边界、为什么不改包名、如何同步上游、如何做深度改名
- [docs/04-教培SaaS产品方案.md](docs/04-教培SaaS产品方案.md) —— 产品定位、模块划分、数据模型与里程碑
- [docs/05-核心业务逻辑细化.md](docs/05-核心业务逻辑细化.md) —— 课时账务、排课、退费、续费预警的业务规则
- [docs/06-P0完成说明.md](docs/06-P0完成说明.md) —— 多租户开通闭环、平台鉴权、退费修正、定时任务与 v1 支付范围

## 十、版权与出处

本项目为 Snowy 的衍生工程，遵循 Apache License 2.0。

- Snowy 版权归属：小诺开源技术（<https://www.xiaonuo.vip>）
- 请勿删除或修改源码头部的版权与作者声明及出处；
- 二次开源不得参与同类竞争（上游条款）。

Copyright [2022] [https://www.xiaonuo.vip]
