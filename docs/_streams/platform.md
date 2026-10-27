# 平台运营能力（opt/platform）

- 分支：`opt/platform`（基于 `da2ffc1`）
- 工作目录：`.jdk/wt-platform`
- 未改主工作区、未合并/rebase/push、未启动后端/Vite（工作流阶段）

## 一、已实现

### 1. 套餐功能开关与状态

- 套餐新增账号数上限 `ACCOUNT_LIMIT`、上下架状态 `STATUS`；
- 套餐列表展示：状态、学员/校区/账号限额、功能开关；
- 功能开关扩展为：报表、自动化、家长端、支付、招生、学员、课务、财务；
- 支持上架/下架、复制套餐（副本默认下架）；
- 已被租户使用的套餐删除时有保护。

### 2. 平台经营指标

`GET /platform/report/metrics`，基于现有租户/套餐/续费/登录数据实时计算：

- 活跃租户、付费租户、MRR、ARR；
- 当月续费金额/租户数、续费率、到期未续、流失率；
- 当月新增、试用租户、试用转化率；
- 30 天内到期、已过期；
- 近 12 个月趋势、到期分布。

平台看板新增 MRR/ARR/续费率/流失率/试用转化率/30 天内到期卡片。

### 3. 租户健康度

`GET /platform/report/tenantHealth`，规则评分 0-100：

- 登录得分、课消得分、收款得分、到期得分；
- HEALTHY/WARNING/RISK 等级；
- 流失预警标记；
- 可解释扣分原因。

平台看板新增「租户健康度」表格。

### 4. 内部账单/收款记录

新增表 `T_PLATFORM_BILL` 与账单菜单 `/platform/bill`：

- 账单分页/汇总/新增/编辑/删除；
- 登记收款、作废账单；
- 导出对账 Excel；
- 续费时自动生成已收款账单（`recordRenewBill`）；
- 不接真实支付/发票平台，发票状态仅人工登记。

## 二、数据库迁移

独立幂等迁移：`platform/core/init/PlatformOpsSchemaMigration.java`

- `T_TENANT_PACKAGE.ACCOUNT_LIMIT`
- `T_TENANT_PACKAGE.STATUS`（默认 ON）
- 新表 `T_PLATFORM_BILL`
- 菜单资源 `menu_plat_bill_001`，由 `PlatformBootstrap` 授权给 platformAdmin

## 三、接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/platform/report/metrics` | MRR/ARR/续费/流失/试用转化 |
| GET | `/platform/report/tenantHealth` | 租户健康度 |
| POST | `/platform/package/status` | 套餐上下架 |
| POST | `/platform/package/copy` | 复制套餐 |
| GET | `/platform/bill/page` | 账单分页 |
| GET | `/platform/bill/summary` | 账单汇总 |
| POST | `/platform/bill/add/edit/delete` | 账单维护 |
| POST | `/platform/bill/pay` | 登记收款 |
| POST | `/platform/bill/void` | 作废 |
| GET | `/platform/bill/export` | 导出对账 |

`/platform/**` 由平台运营角色校验保护，不额外增加按钮级权限。

## 四、前端

- `views/platform/package.vue`：状态/限额/功能开关、上下架、复制。
- `views/platform/bill.vue`：账单列表、汇总卡、维护、收款、作废、导出。
- `views/platform/dashboard.vue`：MRR/ARR/续费率/流失率/试用转化率卡片 + 租户健康度表。

## 五、验证

- 后端构建通过。
- 前端构建通过（5420 模块）。
- 平台运营接口/页面联调由主流程用 `platformAdmin` 统一执行。

## 六、未完成 / 限制

1. 真实支付、电子发票、微信/短信通知未接，账单仅内部对账。
2. 账单导出上限 5000 条。
3. 租户健康度是规则评分，不是机器学习预测。
4. 平台指标实时计算，未落汇总表；租户量大时需加缓存/日汇总。
