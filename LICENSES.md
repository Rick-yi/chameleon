# 许可证范围说明

本仓库采用**双许可**：

| 范围 | 许可证 | 文件 |
| --- | --- | --- |
| 本项目原创部分（教培业务） | **MIT**（见 [LICENSE-MIT](LICENSE-MIT)） | 459 个 |
| 上游 Snowy 及衍生部分 | **Apache License 2.0**（见 [LICENSE](LICENSE)） | 其余全部 |
| 上游附加条款 | 见 [NOTICE](NOTICE)：不可二次分发参与同类竞品 | — |

未在下方 MIT 清单内的文件，一律按 Apache License 2.0 授权。

## MIT 范围（原创业务代码）

划分依据是可机械验证的事实：Snowy v3.6.5 的导入提交（`269e94b`，1748 个文件）中**完全不存在**以下目录，因此这些目录下的代码全部为本项目原创。

### 后端

| 路径 | 说明 |
| --- | --- |
| `snowy-plugin/snowy-plugin-student/**` | 学员、订单、课时账户、退费、学杂费、续费预警 |
| `snowy-plugin/snowy-plugin-course/**` | 课程、课包、班级、排课、点名 |
| `snowy-plugin/snowy-plugin-crm/**` | 线索、跟进、试听 |
| `snowy-plugin/snowy-plugin-finance/**` | 财务汇总与导出 |
| `snowy-plugin/snowy-plugin-platform/**` | 多租户运营、套餐、平台账单 |
| `snowy-plugin-api/snowy-plugin-student-api/**` | 学员域对外接口与 DTO |
| `snowy-plugin-api/snowy-plugin-course-api/**` | 课务域对外接口与 DTO |
| `snowy-plugin-api/snowy-plugin-crm-api/**` | CRM 域对外接口与 DTO |
| `snowy-plugin-api/snowy-plugin-platform-api/**` | 平台域对外接口与 DTO |

### 前端

| 路径 | 说明 |
| --- | --- |
| `snowy-admin-web/src/views/{student,course,crm,report,app,workbench}/**` | 业务页面（含移动端 `/app`） |
| `snowy-admin-web/src/api/{student,course,crm,report}/**` | 业务接口封装 |

## 不在 MIT 范围

以下部分来自上游或属于其衍生，保持 Apache License 2.0：

- `snowy-common/**`、`snowy-web-app/**`
- `snowy-plugin/snowy-plugin-{sys,auth,dev,client,mobile,gen}/**`
- `snowy-plugin/snowy-plugin-biz/**`（上游已有 notice/org/user 等，新增的 automation/export 与之混在同一模块，整体按 Apache 2.0）
- `snowy-plugin-api/snowy-plugin-{sys,auth,dev,client,mobile,gen,biz}-api/**`
- `snowy-admin-web/src` 中除上述业务目录外的框架代码（布局、组件库、系统管理页面等）
- `docs/**`（文档，随仓库整体按 Apache 2.0）
- `app/**`（Android 壳）

## 关于源码头注释

上游要求保留源文件头部的版权声明，因此部分原创文件沿用了 Snowy 的头注释模板。**这表示来源，不代表该文件整体按 Apache 2.0 授权**：

- 位于上方 MIT 目录、且内容为本项目原创的文件，MIT 授权适用于其原创部分；
- 若某个文件确实包含上游代码，则该文件中的上游部分仍适用 Apache License 2.0。

## 如何扩展 MIT 范围

新增的原创文件默认纳入 MIT；请同时：

1. 把文件放在上表的目录内（或在此文件新增路径）；
2. 若文件是从上游拷贝/改写而来，保留其 Apache 头注释，并**不要**列入 MIT；
3. 更新本文件的文件计数与路径表。
