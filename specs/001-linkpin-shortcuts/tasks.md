# Tasks: 链接直达（LinkPin）Android 自用 MVP

**Input**: Design documents from `/specs/001-linkpin-shortcuts/`

**Prerequisites**: plan.md、spec.md、research.md、data-model.md、contracts/

**Tests**: 计划书 §10.1 明确要求单元测试（HTTP/HTTPS 校验、scheme 校验、域名键、序列化、去重、名称长度），因此包含测试任务。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无依赖）
- **[Story]**: 所属用户故事（US1/US2/US3）

## Path Conventions

单模块 Android 应用：`app/src/main/java/com/local/linkpin/`，测试在 `app/src/test/java/com/local/linkpin/`。

## Phase 1: Setup（工程骨架）

**Purpose**: 建立可构建的 Android 单模块工程。

- [x] T001 Create Gradle bootstrap files: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`, `.gitignore` at repo root
- [x] T002 Generate Gradle wrapper (8.14.3, Tencent mirror) producing `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- [x] T003 Create Gradle Version Catalog `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10.01, Activity Compose 1.9.3, Lifecycle 2.8.7, DataStore 1.1.1, kotlinx-serialization-json 1.7.3, JUnit 4.13.2)
- [x] T004 Create `app/build.gradle.kts` (namespace com.local.linkpin, compileSdk 35, minSdk 26, targetSdk 35, Compose + serialization plugins, Java 17)
- [x] T005 Create `app/src/main/AndroidManifest.xml` with QUERY_ALL_PACKAGES + INSTALL_SHORTCUT permissions, MainActivity (exported, launcher), RouteActivity placeholder (exported=false, noHistory, excludeFromRecents, transparent theme)
- [x] T006 Create resources: `app/src/main/res/values/strings.xml`, `values/colors.xml`, `values/themes.xml`, adaptive launcher icon `res/mipmap-anydpi-v26/ic_launcher.xml` + `res/drawable/ic_launcher_foreground.xml`
- [x] T007 Create app entry: `app/src/main/java/com/local/linkpin/LinkPinApp.kt` (Application + AppContainer) and `MainActivity.kt` with Compose placeholder list
- [x] T008 Baseline build: `gradlew.bat assembleDebug` succeeds

**Checkpoint**: 可安装、可启动的空白应用。

## Phase 2: Foundational（数据、图标、路由核心）

**Purpose**: 所有用户故事依赖的纯逻辑与基础能力。

- [x] T009 [P] Create data models + JSON codec in `app/src/main/java/com/local/linkpin/data/Models.kt` (ShortcutRecord, RouteRule, sealed RouteTarget, LinkTarget, JsonCodec)
- [x] T010 [P] Create `app/src/main/java/com/local/linkpin/routing/DomainKey.kt` (小写 host；自定义 scheme `scheme://host`/`scheme:`；忽略端口/路径/查询/fragment)
- [x] T011 [P] Create `app/src/main/java/com/local/linkpin/routing/LinkValidator.kt` (名称 1-40、拒绝 file/content/javascript/intent、短链识别 b23.tv/v.douyin.com)
- [x] T012 [P] Create `app/src/main/java/com/local/linkpin/shortcut/DefaultIcon.kt` (首字 + 哈希颜色，512x512，安全区)
- [x] T013 Create `app/src/main/java/com/local/linkpin/data/ShortcutRepository.kt` (DataStore, observeAll/upsert/delete/get, 损坏 JSON 降级为空)
- [x] T014 Create `app/src/main/java/com/local/linkpin/data/RouteRuleRepository.kt` (DataStore, get/put/clear)
- [x] T015 Create `app/src/main/java/com/local/linkpin/shortcut/IconProcessor.kt` (采样解码 ≤1024、居中裁剪、512 PNG 落盘、删除私有文件)
- [x] T016 Create `app/src/main/java/com/local/linkpin/shortcut/ShortcutPublisher.kt` (requestPinShortcut → INSTALL_SHORTCUT 降级、updateShortcuts、repin、deleteFiles、FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE)
- [x] T017 Create `app/src/main/java/com/local/linkpin/routing/LinkResolver.kt` (ACTION_VIEW+CATEGORY_BROWSABLE 候选、按包名去重排除自身、默认浏览器解析、显式启动 + NEW_TASK、失败返回 false)
- [x] T018 [P] Create unit test `app/src/test/java/com/local/linkpin/routing/LinkValidatorTest.kt`
- [x] T019 [P] Create unit test `app/src/test/java/com/local/linkpin/routing/DomainKeyTest.kt`
- [x] T020 [P] Create unit test `app/src/test/java/com/local/linkpin/data/ModelsSerializationTest.kt`
- [x] T021 Run `gradlew.bat testDebugUnitTest` — all tests pass

**Checkpoint**: 数据层/图标层/路由层逻辑完备且有测试覆盖。

## Phase 3: User Story 1 - 新建并固定快捷方式 (Priority: P1) 🎯 MVP

**Goal**: 用户在应用内创建带自定义名称/图标的快捷方式并固定到桌面。

**Independent Test**: 真机创建哔哩哔哩快捷方式（自定义名称 + 相册大图），桌面出现图标，重启后记录仍在；无真机时以编译 + 单元测试 + 代码走查验证。

### Implementation for User Story 1

- [x] T022 [US1] Create `app/src/main/java/com/local/linkpin/ui/EditShortcutViewModel.kt` (表单 StateFlow、校验、图标 Uri 管理、save 创建流程)
- [x] T023 [US1] Create `app/src/main/java/com/local/linkpin/ui/EditShortcutScreen.kt` (链接/名称输入、短链提示、Photo Picker、默认图标预览、保存按钮、错误提示)
- [x] T024 [US1] Wire create flow in `app/src/main/java/com/local/linkpin/MainActivity.kt` (屏幕状态切换：列表 ↔ 新建；保存后 publisher.create 并返回列表)
- [x] T025 [US1] Verify `gradlew.bat assembleDebug` + code review of create→pin→persist path

**Checkpoint**: US1 可独立演示。

## Phase 4: User Story 2 - 快捷方式管理 (Priority: P2)

**Goal**: 列表查看、完整编辑、删除、重新固定。

**Independent Test**: 创建后执行编辑（名称/链接/图标）、删除、重新固定，本地记录与桌面图标同步。

### Implementation for User Story 2

- [x] T026 [US2] Create `app/src/main/java/com/local/linkpin/ui/ShortcutListViewModel.kt` (observeAll StateFlow、delete、repin)
- [x] T027 [US2] Create `app/src/main/java/com/local/linkpin/ui/ShortcutListScreen.kt` (LazyColumn 列表、图标展示、编辑/重新固定/删除操作、删除确认对话框、空态、FAB)
- [x] T028 [US2] Implement edit flow in `app/src/main/java/com/local/linkpin/ui/EditShortcutViewModel.kt` (预填、publisher.update、图标替换/移除并清理旧文件、repin 提示)
- [x] T029 [US2] Wire edit/delete/repin in `app/src/main/java/com/local/linkpin/MainActivity.kt`
- [x] T030 [US2] Verify `gradlew.bat assembleDebug` + code review of edit→updateShortcuts→repin path

**Checkpoint**: 管理闭环可用。

## Phase 5: User Story 3 - 点击直达与域名记忆 (Priority: P3)

**Goal**: 桌面图标点击后按域名路由直达目标 App 或浏览器，首次选择一次、之后不再询问。

**Independent Test**: 首次点击出现选择面板，选择后直达；同域名再次点击不再询问；目标失效重新询问；无真机时以单元测试 + 代码走查验证。

### Implementation for User Story 3

- [x] T031 [US3] Implement `app/src/main/java/com/local/linkpin/routing/RouteActivity.kt` (shortcut_id 契约、记录缺失提示、规则命中/失效处理、单候选直达、浏览器回落、选择面板挂载)
- [x] T032 [US3] Create `app/src/main/java/com/local/linkpin/ui/RouteChoiceScreen.kt` (候选 App 列表 + 浏览器选项、应用图标渲染、取消不保存)
- [x] T033 [US3] Wire choice selection → `RouteRuleRepository.put` + `LinkResolver.launch` in `RouteActivity.kt`
- [x] T034 [US3] Finalize RouteActivity manifest entry + transparent theme in `app/src/main/res/values/themes.xml`
- [x] T035 [US3] Verify `gradlew.bat assembleDebug` + code review of routing decision tree

**Checkpoint**: 路由直达闭环可用。

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 文档、测试补全、最终产物。

- [x] T036 [P] Create `README.md` (安装、权限说明、已知限制：桌面图标删除/短链/QUERY_ALL_PACKAGES)
- [x] T037 [P] Review unit test coverage against plan §10.1 checklist and add missing cases in `app/src/test/java/com/local/linkpin/`
- [x] T038 Run `gradlew.bat testDebugUnitTest assembleDebug` clean — all green
- [x] T039 Verify APK artifact `app/build/outputs/apk/debug/app-debug.apk` exists; `adb devices` 状态记录（无设备则转人工真机验收）
- [x] T040 Mark quickstart.md validation scenarios status; commit all work to git

**Checkpoint**: debug APK 可交付，真机验收清单交给用户。

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖，立即开始。
- **Foundational (Phase 2)**: 依赖 Phase 1，阻断所有用户故事。
- **User Stories (Phase 3-5)**: 依赖 Phase 2；按 P1 → P2 → P3 顺序执行（单开发者）。
- **Polish (Phase 6)**: 依赖全部故事完成。

### User Story Dependencies

- **US1 (P1)**: Phase 2 完成后即可开始。
- **US2 (P2)**: 复用 US1 的编辑界面（T022/T023）。
- **US3 (P3)**: 复用 Phase 2 的 resolver/repository。

### Parallel Opportunities

- Phase 1: T001-T007 中 T003/T005/T006 可并行（不同文件）。
- Phase 2: T009-T012 可并行；T018-T020 可并行。
- Phase 6: T036/T037 可并行。

## Implementation Strategy

### MVP First

1. Phase 1 → Phase 2 完成基础能力。
2. Phase 3 完成后即为可安装 MVP（能创建并固定快捷方式）。
3. Phase 4/5 依次补齐管理与路由，Phase 6 收尾交付。

### Notes

- [P] 任务操作不同文件，可并行。
- 每完成一个阶段在 tasks.md 勾选对应任务。
- 提交以逻辑组为单位（阶段一/阶段二/…）。
- 真机验收任务（T025/T030/T035 的实机部分、T039 adb 部分）在无设备时记录状态并由用户人工执行。

## 验证记录（2026-11-16）

构建与自动化验证：

- `gradlew.bat testDebugUnitTest assembleDebug` 全绿：24 个单元测试通过，产出 `app/build/outputs/apk/debug/app-debug.apk`（约 9.3 MB）。
- 单元测试覆盖：HTTP/HTTPS 校验、非法 scheme 拒绝（file/content/javascript/intent）、短链识别、名称 1-40 规则、路由键（大小写/端口/路径/查询/fragment）、多态序列化往返与未知字段兼容。

模拟器（API 35 AOSP + AEHD）冒烟验收，全程 logcat 无 FATAL/ANR：

1. 安装与冷启动成功；空态列表与新建按钮渲染正确。
2. 创建（默认字母图标）→ requestPinShortcut → Launcher3 添加对话框 → 桌面出现图标。
3. 强杀进程重启后记录仍在（DataStore 持久化）。
4. 首次点击桌面图标 → 选择面板（候选 App + 浏览器）；选择浏览器 → 规则保存并直达，URL 正确传递。
5. 同域名再次点击 → 直接打开，不再询问。
6. 取消面板 → 再次点击仍询问（规则未保存）。
7. 相册选图（3000x2000 PNG）→ 居中裁剪压缩为 512x512 PNG 落盘，无 OOM。
8. 编辑名称 → updateShortcuts → 桌面图标名称即时刷新。
9. 删除 → 本地记录与图标文件移除 → 点击桌面残留旧图标仅提示、无崩溃。
10. RouteActivity `exported=false`：外部进程无法直接拉起（权限拒绝）。

遗留（需目标 OPPO 真机 + 真实 App 人工验收）：

- 哔哩哔哩/微信读书真实 App 直达、目标 App 卸载后规则失效。
- 自定义 scheme 直达、OPPO 桌面对固定/降级（INSTALL_SHORTCUT）的支持。
- `重新固定` 按钮在国产桌面的实际效果。
- 超大相册原图的实机内存表现与图标视觉效果确认。
