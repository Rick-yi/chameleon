# Android 壳工程（教培助手 App）

> 定位：**只做 WebView 容器**。业务逻辑全部在 H5（`snowy-admin-web` 的 `/app/*`），
> 壳不感知任何业务，方便随时替换 H5、复用同一套后端接口。

---

## 一、壳只做 5 件事

| # | 能力 | 实现 |
|---|---|---|
| 1 | 加载 H5 | `BuildConfig.H5_BASE_URL` + `BuildConfig.H5_ENTRY_PATH`（默认 `/app/`） |
| 2 | 免登录 | 启动 Intent 带 `token` extra → 拼成 `?token=xxx` 传给 H5，前端落地为登录态并从地址栏移除 |
| 3 | 返回键 | 能后退就后退；到首页后再按两次退出 |
| 4 | 文件选择/上传 | `WebChromeClient.onShowFileChooser` |
| 5 | 加载进度 | 顶部 3dp 进度条 |

其它（推送、离线缓存、扫码）属于 P1，见 `docs/13-移动端原型.md`。

---

## 二、目录

```
app/
├── settings.gradle.kts        # 仓库与插件
├── build.gradle.kts           # 插件版本
├── gradle.properties
└── app/
    ├── build.gradle.kts       # H5 地址、SDK 版本、依赖
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/chameleon/app/MainActivity.kt
        └── res/
            ├── layout/activity_main.xml
            ├── values/strings.xml, themes.xml
            └── xml/network_security_config.xml
```

---

## 三、构建

```bash
cd app
# 用 Android Studio 打开（推荐，会自动拉取 Gradle 与 SDK）
# 或命令行：
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

需要：JDK 17、Android SDK 34、Gradle 8.7+（Android Studio 自带）。

> 本仓库不包含 Gradle wrapper 与签名配置，构建前请自行补 `gradle/wrapper` 与 CI 签名。

---

## 四、配置 H5 地址

`app/app/build.gradle.kts`：

```kotlin
defaultConfig {
    buildConfigField("String", "H5_BASE_URL", "\"https://demo.chameleon.cc\"")
    buildConfigField("String", "H5_ENTRY_PATH", "\"/app/\"")
}
buildTypes {
    debug {
        // 模拟器访问宿主机：10.0.2.2；真机请用局域网 IP
        buildConfigField("String", "H5_BASE_URL", "\"http://10.0.2.2:81\"")
    }
}
```

真机调试：`adb reverse tcp:81 tcp:81` 后用 `http://127.0.0.1:81`。

---

## 五、与 H5 的约定

1. **入口路径**：壳打开 `{base}/app/`，未登录时前端跳 `/app/login`。
2. **免登录传参**：`{base}/app/login?token={saToken}`，前端会把 token 落地为登录态并从地址栏移除（避免 token 泄露到历史记录/截图）。
3. **登录态存储**：前端 `localStorage`（`tool.data`），壳不做持久化。
4. **原生能力**：当前只用到文件选择；需要相机/推送时，在 `MainActivity` 里加 `JavascriptInterface` 并在 H5 侧通过 `window.chameleon.xxx()` 调用。

---

## 六、与 PC 的差异

App 端只保留高频主流程（课表、点名、招生跟进、收款、分视角报表），
配置类功能（课包/课程/排课模板、权限、导出）仍以 PC 为准。
详见 `docs/13-移动端原型.md`。
