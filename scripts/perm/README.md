# 权限体系脚本

| 脚本 | 作用 |
|---|---|
| `gen-role-buttons.py` | 生成「业务接口 → 按钮资源」的 SQL（`_sql/chameleon_role_api_button.sql`） |
| `demo-role-users.sql` | 为 demo 租户创建四个岗位演示账号（teacher01 / sales01 / cashier01 / principal01） |
| `role-scope-smoke.mjs` | 权限与数据范围验证（62 项用例） |

## 使用

```powershell
# 1) 登记业务接口为按钮资源（幂等）
mysql -uroot -p chameleon < snowy-web-app/src/main/resources/_sql/chameleon_role_api_button.sql

# 2) 为租户套用角色模板（平台运营接口，幂等）
POST /platform/roleTemplate/apply {"tenantId":"xxx"}

# 3) 验证权限与数据范围
node scripts/perm/role-scope-smoke.mjs demo
```

角色模板定义在 `PlatformRoleTemplates`（后端常量），四个岗位角色与 App 端视角一一对应。
详见 `docs/14-权限体系.md`。
