# 学员档案增强（opt/student 独立工作流）

- 工作目录：`D:\IdeaProject\chameleon\.jdk\wt-student`
- 分支：`opt/student`（基于 `da2ffc1`）
- 未改主工作区、未合并/rebase/push、未启动后端/Vite、未改共享数据库
- 未改 `README.md`、`docs/26`、`docs/43+`、`P0SchemaMigration.java`、`GlobalConfigure.java`

## 一、已实现

### 1. 学员批量导入/导出

- Excel 导入模板下载；
- 批量导入学员，错误行回显，返回成功/失败数量；
- 按当前筛选条件导出学员；
- 前端学员管理页提供「导入 / 导出」入口，导入成功刷新列表。

### 2. 学员标签与备注

- 标签字典：新增/编辑/删除，支持颜色、排序；
- 学员绑定/解绑标签，列表按标签筛选；
- 详情展示已选标签；
- 备注沿用学员备注字段。

### 3. 学员状态机

- 支持在读、停课、休学、退学、结课、已退费；
- 状态变更必须填写原因，写 `STUDENT_STATUS_LOG` 留痕；
- 学员表新增 `STATUS_REASON`、`STATUS_CHANGE_TIME`；
- 状态变更后列表/详情展示原因与时间。

### 4. 学员动态时间线

- 报名、收款、退费申请/审批/退款、消课/恢复/调整、状态变更、线索跟进按时间倒序合并；
- 详情页新增「全部动态」Tab；
- 复用现有订单、退费、课时流水、状态日志、CRM 跟进数据，不重复存一份。

### 5. 点名页课时账户提示

- PC 点名页显示「课程 + 剩余课时」，余额不足/未购课保留出勤但提示跳过原因；
- APP 点名页同步展示课程剩余课时；
- 多班共用同一课程账户时，明确显示扣的是课程维度余额。

## 二、数据库迁移

独立幂等迁移类：`student/core/init/StudentArchiveSchemaMigration.java`

- `STUDENT.STATUS_REASON` varchar(500)
- `STUDENT.STATUS_CHANGE_TIME` datetime
- 索引 `IDX_STUDENT_TENANT_STATUS (TENANT_ID, STATUS)`
- 新表 `STUDENT_TAG`、`STUDENT_TAG_REL`、`STUDENT_STATUS_LOG`
- 历史非在读学员回填 `STATUS_CHANGE_TIME = COALESCE(UPDATE_TIME, CREATE_TIME)`

新表未登记到 `GlobalConfigure.TENANT_TABLES`（工作流禁止修改该文件），服务层所有读写显式带 `TENANT_ID`。后续主流程可补充登记。

## 三、接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/student/status/logs?studentId=` | 状态变更记录 |
| POST | `/student/status/change` | 变更状态，必须带原因 |
| GET | `/student/importTemplate` | 下载导入模板 |
| POST | `/student/import` | 批量导入（multipart file） |
| GET | `/student/export` | 按筛选条件导出 |
| GET | `/student/timeline?studentId=` | 学员动态时间线 |
| GET | `/student/tag/list` | 标签字典 |
| GET | `/student/tag/studentTags?studentId=` | 学员已选标签 |
| POST | `/student/tag/add` `/edit` `/delete` `/bind` | 标签维护与绑定 |

## 四、权限

新增 `StudentArchivePermissionMigration`：

- 注册 12 个按钮/接口资源，挂到 `menu_student_001`；
- 读取类接口复用 `/student/page` 权限角色；
- 写入类接口复用 `/student/edit` 权限角色；
- 所有租户 `superAdmin` 补充授权。

主流程无需手工执行权限 SQL。

## 五、验证

- 后端 Maven 构建通过。
- 前端构建通过（5419 模块）。
- 未启动后端、未执行迁移、未联调接口；合并后由主流程统一启动和回归。

## 六、未完成 / 限制

1. 标签/状态新表未登记 `TENANT_TABLES`，当前靠服务层显式租户过滤。
2. 导入目前是同步处理，大文件（数千行）会占用请求；后续可改异步任务。
3. 状态变更只记录日志，不自动联动退费/停课/课时冻结；这些仍由原业务流程处理。
4. 时间线最多返回固定条数（`MAX_ITEMS`），未做服务端分页。
5. 点名页只展示余额和跳过原因，未新增“补费/报名”一键入口。
