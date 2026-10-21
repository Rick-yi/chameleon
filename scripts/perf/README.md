# 性能与容量压测脚本

模拟「10 个机构 × 每个机构 800~1000 名学员」的真实形态数据，验证系统在多租户大数据量下的表现。

## 脚本说明

| 脚本 | 作用 |
|---|---|
| `scale-tenant-create.mjs` | 通过平台运营接口开通 10 个压测租户（走真实开通流程，含权限授权） |
| `scale-data-gen.py` | 批量生成业务数据：课程/课包/班级/学员/订单/课时账户/流水/排课/点名/线索/埋点 |
| `scale-clean.py` | 清理压测租户的数据（重复压测前先跑） |
| `scale-bench.mjs` | 全量接口压测：48 个接口 × N 次，输出 p50/p95 |
| `scale-bench-hot.mjs` | 重点慢接口的稳态压测（去掉首次 JIT 预热干扰） |

## 使用步骤

```powershell
# 1) 开通 10 个压测租户（需先启动后端）
node scripts/perf/scale-tenant-create.mjs

# 2) 造数（约 40 秒，单租户 3 万行左右）
python scripts/perf/scale-data-gen.py

# 3) 压测
node scripts/perf/scale-bench.mjs scale01 3      # 指定租户、每接口 3 次
node scripts/perf/scale-bench-hot.mjs scale01 8  # 重点接口稳态压测

# 4) 清理（可选）
python scripts/perf/scale-clean.py
```

## 单租户数据规模

| 表 | 行数/租户 | 10 租户合计 |
|---|---|---|
| 学员 STUDENT | 800 | 8,000 |
| 订单 STUDENT_ORDER | 1,000 | 10,000 |
| 课时账户 STUDENT_LESSON_ACCOUNT | 800 | 8,000 |
| 课时流水 STUDENT_LESSON_FLOW | 4,000 | 40,000 |
| 排课 COURSE_SCHEDULE | 240 | 2,400 |
| 点名记录 COURSE_CLASS_ATTEND | 3,200 | 32,000 |
| 线索 CRM_LEAD | 200 | 2,000 |
| 埋点 BIZ_EVENT | 20,000 | 200,000 |

## 说明

- 压测租户的账号：`admin` / `Chameleon@2026!`，机构编码 `scale01` ~ `scale10`
- 脚本只操作 `CODE like 'scale%'` 的租户数据，不会影响正式数据
- 结论与优化记录见 `docs/12-性能与容量.md`
