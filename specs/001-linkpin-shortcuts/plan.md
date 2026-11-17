# Implementation Plan: 链接直达（LinkPin）Android 自用 MVP

**Branch**: `001-linkpin-shortcuts` | **Date**: 2026-11-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-linkpin-shortcuts/spec.md`（源文档：工作区根目录 `DEVELOPMENT_PLAN.md` v1.0）

## Summary

构建一个完全离线的 Android 本地小工具：把视频/书籍/网页链接固定为桌面快捷方式，支持自定义名称与图标；点击快捷方式按域名路由直达目标 App（或默认浏览器），同一域名首次选择后不再询问。技术方案为原生 Kotlin 单模块应用 + Jetpack Compose，替代 uni-app 方案（见 [research.md](./research.md) 决策 D1）。

## Technical Context

**Language/Version**: Kotlin 2.0.21

**Primary Dependencies**: Jetpack Compose BOM 2024.10.01 + Material 3、Activity Compose 1.9.3、Lifecycle 2.8.7、DataStore Preferences 1.1.1、kotlinx.serialization JSON 1.7.3

**Storage**: DataStore Preferences（JSON 序列化的记录与路由规则）+ 应用私有目录 `filesDir/icons/`（512x512 PNG 图标）

**Testing**: JUnit 4 单元测试（纯逻辑：链接校验、路由键、序列化）+ OPPO 真机验收（无真机时人工执行）

**Target Platform**: Android 8.0+（minSdk 26；compileSdk/targetSdk 35）

**Project Type**: mobile-app（单模块 Android 应用）

**Performance Goals**: 大图图标解码不 OOM（采样 + 512 上限）；点击快捷方式到目标 App 无可见中间界面闪烁

**Constraints**: 全程离线；不申请 INTERNET/存储/后台权限；Gradle Wrapper 8.14.3（腾讯镜像源）+ AGP 8.7.3 + JDK 17

**Scale/Scope**: 单人自用 MVP，记录规模为几十条；单模块、无 Hilt/Navigation/Room

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` 目前是未填充的模板（所有原则均为占位符）。按 speckit 惯例跳过宪法原则检查，视为无门禁违规；后续若填充宪法需重新核验。

## Project Structure

### Documentation (this feature)

```text
specs/001-linkpin-shortcuts/
├── plan.md              # 本文件
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（桌面快捷方式 → RouteActivity 意图契约）
└── tasks.md             # Phase 2 输出（$speckit-tasks）
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/local/linkpin/
    │   │   ├── data/        # Models.kt、ShortcutRepository.kt、RouteRuleRepository.kt
    │   │   ├── shortcut/    # IconProcessor.kt、DefaultIcon.kt、ShortcutPublisher.kt
    │   │   ├── routing/     # LinkValidator.kt、DomainKey.kt、LinkResolver.kt、RouteActivity.kt
    │   │   ├── ui/          # ShortcutListScreen.kt、EditShortcutScreen.kt、RouteChoiceScreen.kt、theme/
    │   │   ├── LinkPinApp.kt
    │   │   └── MainActivity.kt
    │   └── res/             # 主题、字符串、自适应启动图标
    └── test/java/com/local/linkpin/  # 单元测试
gradle/libs.versions.toml
settings.gradle.kts
build.gradle.kts
gradle.properties
```

**Structure Decision**: 单模块 Android 应用，代码按 data / shortcut / routing / ui 四层分包；`LinkPinApp`（Application）内建手动依赖容器；列表页与编辑页用简单屏幕状态切换，不引入导航库。

## Complexity Tracking

> 无宪法违规需要记录。
