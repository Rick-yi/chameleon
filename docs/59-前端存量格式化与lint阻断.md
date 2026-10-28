# 前端存量格式化与 lint 阻断

本轮只做代码格式统一，不含功能改动，与 `docs/58` 的功能修复分开提交，便于单独 review。

## 一、基线

`npm run lint` 长期不可用：663 个前端文件中 105 个存在格式问题，共 1557 条（1536 error、21 warning）。其中 1523 条可由 `--fix` 自动修复，绝大多数是历史 CRLF 与 Prettier 排版差异。

## 二、自动化处理

`eslint --fix` 作用于 `src`，重排 **96 个 Vue + 2 个 JS 文件**，问题从 1557 降到 29（13 error、16 warning）。

## 三、剩余 13 个真实缺陷（非格式问题）

`--fix` 无法处理，逐个手工修复：

| 文件 | 问题 |
| --- | --- |
| `TreeSelect/menuTreeSelect.vue` | 本地 `ref` 与 `prop` 同名，本地声明遮蔽 prop |
| `XnFilePreview/index.vue` | 同上，`fileType` 被遮蔽 |
| `app/home/index.vue` | `v-if` 与 `v-for` 同元素，骨架屏逻辑不清晰，改为 `<template>` 包裹 |
| `XnDataList/index.vue` | 3 处 `v-for` 缺 `key` |
| `exm/iconSelect/index.vue` | 2 处 `v-for` 缺 `key` |
| `dev/slideshow/subForm.vue` | `Object` 类型的 default 写成 `[]`；`text` 槽位参数未使用 |
| `mobile/resource/menu/form.vue` | `&nbsp` 缺分号导致模板解析错误 |
| `sys/resource/menu/form.vue` | 同上 |

两个重名变量的修复不只是过 lint：本地 ref 遮蔽 prop 属于隐蔽缺陷，平时不报错，只在特定交互下表现异常。

## 四、一个 Prettier 与 Vue 模板的冲突

`student/index.vue` 原先有内联多语句事件：

```html
@click="record.editing = true; record.editName = record.name"
```

Prettier 把分号替换为换行后，在 Vue 模板的内联表达式里换行不构成语句分隔，**构建直接失败**（`Error parsing JavaScript expression: Unexpected token, expected ","`）。

把分号手工加回去可以恢复构建，但 `npm run lint` 又报 `Delete ';'`——形成死锁。最终解法不是压制规则，而是把逻辑提取成方法：

```js
const onStartEditTag = (record) => {
	record.editing = true
	record.editName = record.name
}
```

模板改为 `@click="onStartEditTag(record)"`，冲突消失且代码更清晰。

## 五、如何确认没有改坏

98 个文件、3400 余行 diff，逐行 review 不现实，采用三层验证：

**第一层：语义等价性脚本**

- 模板层：用正则提取每个 `.vue` 文件中 `v-if` / `v-for` / `@click` / `:xxx` / `#slot` 等指令名，对比格式化前后的多重集合。**96 个文件全部一致**，说明没有增删任何指令。
- 脚本层：提取 `<script>` 段内所有标识符，对比多重集合。**98 个文件中仅 2 个有差异**，且正是刻意重命名的 `menuTreeSelect`（`defaultSelectKeys` → `selectedKeys`）和 `XnFilePreview`（`fileType` → `currentFileType`）。

这一步排除了「格式化顺手改了语义」的可能。

**第二层：生产构建**

`npm run build` 通过。此前的构建失败也由这一层发现，说明构建不是形式主义。

**第三层：真实浏览器回归**

用 CDP 脚本跑 16 项页面检查：PC 9 个页面、APP 6 个页面，外加「无前端异常」与「无 5xx 接口」。全部通过。

期间发现「PC 班级管理」报 FAIL（找不到「班型」文案）。把格式化改动 stash 回退后重跑原始代码，**同样失败**——确认是回归脚本的断言过期（页签文案早已改为「一对多 / 一对一」），与本轮无关。已修正断言，现在这 16 项是真正有效的检查。

## 六、CI 收紧

`.github/workflows/ci.yml` 的代码规范检查去掉 `continue-on-error: true`，lint 成为阻断关卡。

`npm run lint` 退出码 0，剩余 16 条 warning 不阻断：

- 4 处 `v-html`（`BizNoticeCard`、`topMenu`、`biz/notice/detail`、`emailTemplateConfig/preview`）——若通知内容允许富文本输入，这是 XSS 面，但需引入净化库，不宜在本轮顺手改；
- 其余为模板变量遮蔽（`day`、`record`、`module`），不影响运行。

## 七、验证汇总

| 项目 | 结果 |
| --- | --- |
| `npm run lint` | 退出码 0（0 error，16 warning） |
| 前端生产构建 | 通过 |
| 模板指令语义比对 | 96 个文件一致 |
| 脚本标识符比对 | 仅 2 个刻意重命名文件有差异 |
| UI 回归 | 16 项全通过，无前端异常、无 5xx |

## 八、说明

- 本轮未发布生产。
- 后端代码零改动。
