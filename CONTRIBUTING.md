# 贡献指南

感谢你愿意参与。提交前请先读一遍本文件，能省掉大部分来回沟通。

## 开发环境

| 组件 | 版本要求 |
| --- | --- |
| JDK | **17**（必须，低版本会在编译期报 `--release` 无效） |
| Maven | 3.8+ |
| Node.js | ≥ 18 |
| MySQL | 8.0 / 5.7 |
| Redis | 6 / 7 |

本地启动步骤见 [README 快速开始](README.md#五快速开始)。默认连接（`localhost:3306` / `localhost:6379`）可直接跑，无需环境变量。

## 分支与提交

- 从 `main` 切功能分支：`feat/<主题>`、`fix/<主题>`、`docs/<主题>`、`perf/<主题>`、`refactor/<主题>`。
- 一个提交只做一件事，不要把格式化与功能混在一起。
- 提交信息用中文或英文都可以，但格式统一为 `type: 简述`，正文说明**为什么**改、影响面是什么。

```
feat: 学杂费独立分账与学员详情重点信息
fix: 关闭生产Druid监控台并加固性能与账务口径
```

## 测试

```bash
# 单元测试（纯逻辑，不连库，CI 默认执行）
mvn test

# 前端构建与规范
cd snowy-admin-web && npm run lint && npm run build
```

### 集成测试（需要本机 MySQL）

集成测试会写数据，因此使用**独立的 `chameleon_it` 库**，不要连演示库：

```bash
mysql -uroot -p -e "CREATE DATABASE chameleon_it DEFAULT CHARACTER SET utf8mb4"
export PATH="/path/to/mysql/bin:$PATH"
MYSQL_DB=chameleon_it SKIP_DEMO=1 ./scripts/db-init.sh
./scripts/it.sh
```

`*IT` 不在 surefire 默认范围内，不会在 CI 里跑（CI 无数据库）。

## 代码约定

- 遵循仓库现有风格：服务层用 `ServiceImpl<Mapper, Entity>`，参数对象放 `param/`，返回值用 `CommonResult`。
- 涉及金额一律 `BigDecimal`，比较用 `compareTo`（`equals` 会因精度误判）。
- 更新库存量字段时用**条件更新**（`eq(状态).ge(余额, 扣减量)`）并校验受影响行数，不要「先查再改」。
- 新增业务表必须登记到 `GlobalConfigure.TENANT_TABLES`，否则不参与自动租户隔离。
- 新增接口要在启动迁移里登记权限（参考 `StudentSundryPermissionMigration`），否则非超管账号会 403。
- 富文本用 `v-html` 渲染前必须过 `@/utils/sanitize`。

## 提交 PR

1. 确认 `mvn test`、`npm run lint`、`npm run build` 全部通过；
2. 在 PR 描述里写清：解决什么问题、怎么验证的、有没有数据库结构变更；
3. 有结构变更的，说明升级方式（本项目用启动期幂等迁移，不是 Flyway）。

## 不要提交的内容

`.env`、`*.pem`、`*.key`、`*.p12`、`application-local.properties`、`gui-test-screenshots/` 已在 `.gitignore` 中排除。如果曾经误提交，删除文件不够——历史仍可检出，需要重写历史或轮换凭据。
