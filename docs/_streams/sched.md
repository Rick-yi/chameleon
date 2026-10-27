# 排课课表增强（opt/sched 独立工作流）

- 工作目录：`D:\IdeaProject\chameleon\.jdk\wt-sched`
- 分支：`opt/sched`
- 基线：`da2ffc1`

## 一、已实现

1. 周/日视图课节显示应到/已点进度：
   - 周视图卡片增加「应到 X · 已点 Y」；
   - 日视图表格增加同列；
   - 直接排课（无班级）按该课节指定学员数统计。
2. 排课头部入口收敛：
   - 顶部只保留「排课」「更多」两个入口；
   - 排课下拉包含批量生成/直接排课/手动排课，权限判断保留；
   - 更多下拉包含导出、复制上周、课表模板、调课记录、排课配置。
3. 手动排课实时冲突预检：
   - 班级/日期/时间/教师/教室变化时 250ms 防抖调用 `/course/schedule/conflictCheckBatch`；
   - 展示冲突对象；
   - 后端返回可替代时段，可点击直接回填；
   - 有冲突时阻止提交，后端仍保留最终校验。
4. 复制上周课表：
   - 「更多 → 复制上周」，选择班级，复制到当前周；
   - 后端 `POST /course/schedule/copyLastWeek` 跳过停课日、已有相同课节、教室/教师冲突，并返回跳过原因；
   - 前端展示计划/新增/跳过数量和原因。
5. 拖拽调课沿用上一轮实现：拖拽换日期/时段，停课日/冲突校验后确认。

## 二、接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/course/schedule/conflictCheck` | 参数改为 `CourseScheduleConflictCheckParam`，支持 `excludeScheduleId` |
| POST | `/course/schedule/conflictCheckBatch` | 批量冲突预检，返回冲突说明和可替代时段 |
| POST | `/course/schedule/copyLastWeek` | 复制上周课表到目标周，返回计划/新增/跳过和跳过原因 |

## 三、验证

- 后端 Maven 构建通过。
- 前端构建通过（5419 模块）。
- 未启动后端、未连接共享数据库；接口逻辑通过编译与代码审查验证，正式联调在合并后统一做。

## 四、未完成 / 缺口

- 月视图、课程/校区筛选未做。
- 拖拽调整课时长度、拖到空白格新建排课未做（当前拖拽只换日期/开始时间）。
- 直接排课的批量冲突预检后端已具备，但前端弹窗未接入实时提示；仍由提交时后端校验。
- 教师请假/真实家长微信短信通知未做，依赖外部通道和审批流。

## 五、权限

批量冲突预检和复制上周课表由 `CourseSchedulePermissionMigration` 幂等补齐：

- 新增 `btn_sched_conflict`、`btn_sched_copy_week` 资源，挂到 `menu_schedule_001`；
- 追加到已有排课菜单的 `buttonInfo`；
- 复制已有 `/course/schedule/add` 权限角色的接口授权。

主流程无需手工执行权限 SQL。

## 六、班级班型（一对多 / 一对一）

- `COURSE_CLASS` 新增 `CLASS_TYPE`（`GROUP` 一对多 / `ONE2ONE` 一对一）与 `STUDENT_ID`（一对一班绑定学员）。
- 启动迁移 `CourseClassTypeSchemaMigration` 自动补列、回填历史班级为 `GROUP`，并创建 `(TENANT_ID, STUDENT_ID)` 索引。
- 一对一班必须绑定一名在读学员，容量固定为 1，保存后自动写入班级名册；换绑时旧学员退班、新学员入班。
- 同一学员在同一课程下只能有一个启用中的一对一班；已有在读学员的班级不能直接改成一对一班。
- 课程本身为 `ONE2ONE` 时，班级强制为一对一，忽略前端的“一对多”选择。
- 班级学员接口限制：一对一班只能加入绑定的学员；报名流程只展示当前学员自己的一个一对一班。
- 排课、点名、课消继续复用班级名册，无需额外改造。

手工迁移 SQL：

```sql
ALTER TABLE `COURSE_CLASS`
  ADD COLUMN `CLASS_TYPE` varchar(20) NOT NULL DEFAULT 'GROUP' COMMENT '班型：GROUP一对多/ONE2ONE一对一' AFTER `COURSE_ID`,
  ADD COLUMN `STUDENT_ID` varchar(20) NULL COMMENT '一对一班绑定的学员id' AFTER `CLASS_TYPE`,
  ADD INDEX `IDX_COURSE_CLASS_TENANT_STUDENT` (`TENANT_ID`, `STUDENT_ID`);
UPDATE `COURSE_CLASS` SET `CLASS_TYPE` = 'GROUP' WHERE `CLASS_TYPE` IS NULL OR `CLASS_TYPE` = '';
```
