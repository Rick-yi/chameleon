# CRM 跟进闭环（opt/crm 独立工作流）

- 工作目录：`D:\IdeaProject\chameleon\.jdk\wt-crm`
- 分支：`opt/crm`
- 基线：`da2ffc1 feat: 周课表支持拖拽调课`
- 约束：只改本 worktree；不改主工作区、不合并/rebase/push、不启动后端或 Vite、不改 `README.md` / `docs/26` / `docs/43+` / `P0SchemaMigration.java` / `GlobalConfigure.java`。

## 一、已实现范围

### 1. 线索手机号查重（不阻断）

- 线索新增/编辑弹窗中输入家长手机后失焦，调用轻量接口查询同手机号线索。
- 弹窗内提示：`已存在：姓名 / 状态 / 跟进人`。
- 新增时提供「继续新建」与「查看已有线索」；查看后关闭弹窗并把线索列表筛选到该手机号。
- 手机号改为选填，无手机号不触发查重、不阻断保存（前后端校验同步放宽）。
- 查重接口只返回姓名、状态、跟进人等展示信息，最多 10 条。

### 2. 跟进历史分页与方式筛选

- 跟进弹窗不再只取最近 50 条，改为服务端分页（默认 10 条/页，可切 10/20/50）。
- 支持按「全部 / 电话 / 微信 / 到店」筛选。
- 保存跟进后弹窗保持打开，自动回到第 1 页并刷新跟进列表与线索列表。

### 3. 试听独立视图与结果

- CRM 页面内增加「线索 / 试听」视图切换，不新增路由。
- 试听列表展示：试听时间、学员、家长手机、试听课程、顾问、状态、结果、备注。
- 顾问取线索跟进人（`CRM_LEAD.FOLLOW_USER_ID`），课程名称由前端课程字典映射；未新增数据库字段。
- 支持记录/修改试听状态与结果（待试听/已完成/已取消；待定/有意向/无意向）。
- 支持从试听记录「转学员」：按 `leadId` 拉取线索详情后复用现有转学员流程。
- 未做微信/短信等外部集成。

### 4. 工作台线索跟进内联

- 工作台「待跟进线索」点「跟进」不再跳转 CRM，直接在工作台弹窗填写跟进。
- 保存后刷新工作台待办（`loadAll()`），收款/退费逻辑未改动。
- 原「全部 ›」仍跳转 `/crm?todo=1`；CRM 页保留对旧 `workbench:action` 跳转参数的兼容。

### 5. 转化漏斗 / 来源转化展示

- CRM 线索视图顶部增加「线索转化漏斗」与「来源转化」概览（按权限显示）。
- 复用现有 `/crm/report/funnel`、`/crm/report/source` 接口，仅做展示，未新增报表后端。
- 无渠道成本字段，因此**不展示 ROI**，页面标注「暂无渠道成本，不含 ROI」。

## 二、接口变更

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/crm/lead/page?searchKey=` | 复用现有线索分页做手机号查重，前端再按 `parentPhone` 精确过滤并排除当前编辑线索；没有新增接口。 |
| GET | `/crm/follow/page` | 新增可选参数 `followType`（PHONE/WECHAT/VISIT）。分页参数沿用 `current/size`。 |
| POST | `/crm/trial/update` | 新增。更新试听状态/结果/备注，参数：`id`（必填）、`status`、`result`、`remark`。 |
| GET | `/crm/trial/page` | 返回体新增非数据库字段：`leadName`、`parentPhone`、`followUserName`、`leadStatus`（由线索批量回填）。 |

### 权限

- 未修改 `P0SchemaMigration`。
- 查重复用已有 `/crm/lead/page` 权限，不新增接口。
- `/crm/trial/update` 由 `CrmPermissionMigration` 幂等补齐：新增 `btn_crm_trial_update` 资源，并把接口权限/按钮授权复制给已有 `/crm/trial/add` 权限的角色和所有租户超管。
- 漏斗/来源概览沿用已有 `/crm/report/funnel`、`/crm/report/source` 权限点，无权限时前端不展示、后端不请求。

## 三、改动文件

后端：

- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/lead/controller/CrmLeadController.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/lead/service/CrmLeadService.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/lead/service/impl/CrmLeadServiceImpl.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/lead/param/CrmLeadAddParam.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/lead/param/CrmLeadEditParam.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/follow/param/CrmLeadFollowPageParam.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/follow/service/impl/CrmLeadFollowServiceImpl.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/trial/controller/CrmTrialAppointmentController.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/trial/entity/CrmTrialAppointment.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/trial/param/CrmTrialAppointmentUpdateParam.java`（新增）
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/trial/service/CrmTrialAppointmentService.java`
- `snowy-plugin/snowy-plugin-crm/src/main/java/vip/xiaonuo/crm/modular/trial/service/impl/CrmTrialAppointmentServiceImpl.java`

前端：

- `snowy-admin-web/src/api/crm/crmApi.js`
- `snowy-admin-web/src/views/crm/index.vue`
- `snowy-admin-web/src/views/workbench/index.vue`（仅线索跟进部分）

## 四、验证

- 前端：`cd snowy-admin-web && npm run build` 通过（5419 模块）。
- 后端：`JAVA_HOME=D:\IdeaProject\chameleon\.jdk\jdk-17 mvn -s D:\IdeaProject\chameleon\.jdk\maven-settings-local.xml clean package -DskipTests` 通过。
- 查重改为复用 `/crm/lead/page`，不依赖新增权限；试听结果接口由 `CrmPermissionMigration` 授权。
- 未启动后端/Vite、未连接共享数据库，因此接口未做真实联调；逻辑通过代码审查与编译验证。
- 无自动化单测新增（该模块当前无对应测试基座，避免为测试改动共享数据库）。

## 五、未完成 / 已知缺口

1. 来源 ROI：`CRM_LEAD` 无渠道成本字段，无法计算 ROI；当前只展示线索数/报名数/转化率。需要新增来源成本配置或投放费用表后再补。
2. 试听顾问：试听表无独立顾问字段，当前取线索跟进人；若业务需要「试听授课顾问」与「销售顾问」分离，需新增字段/表。
3. 试听列表暂不支持按学员姓名/手机搜索；现有接口只有 `leadId/status` 过滤。数据量大时可加联表查询。
4. 手机号查重为精确匹配，未做去空格/去分隔符以外的归一化；如存在 `138-xxxx` 等格式需统一清洗后再比较。
5. 查重不按销售数据范围过滤（为保证跨顾问重复也能提示），仅返回姓名/状态/跟进人，存在有限信息可见性权衡。
6. 跟进记录无删除/编辑，本次未涉及。
