## 这个 PR 做了什么

<!-- 一句话说明；关联 Issue 用 "Closes #123" -->

## 类型

- [ ] 新功能（feat）
- [ ] 缺陷修复（fix）
- [ ] 性能优化（perf）
- [ ] 重构（refactor）
- [ ] 文档（docs）
- [ ] 构建/CI（chore）

## 影响面

- [ ] 有数据库结构变更（新表/新列/新索引）
- [ ] 有接口新增或权限变更
- [ ] 有前端页面改动
- [ ] 仅内部重构，无行为变化

## 验证方式

<!-- 贴命令与结果，不要只写「已测试」 -->

```bash
mvn test                 # 单元测试
npm run lint             # 前端规范
npm run build            # 前端构建
./scripts/it.sh          # 集成测试（如涉及）
```

## 自检清单

- [ ] 新增业务表已登记 `GlobalConfigure.TENANT_TABLES`
- [ ] 新增接口已登记权限（启动迁移），非超管账号可访问
- [ ] 涉及金额用 `BigDecimal` 且用 `compareTo` 比较
- [ ] 改动余额/状态的操作使用条件更新并校验受影响行数
- [ ] `v-html` 渲染的富文本已过 `@/utils/sanitize`
- [ ] 没有提交 `.env` / `*.pem` / `*.key` / 截图等本地产物
