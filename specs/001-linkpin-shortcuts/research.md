# Research: 链接直达（LinkPin）技术决策

## D1: 原生 Kotlin + Compose vs uni-app

- Decision: 采用原生 Kotlin 2.0.21 + Jetpack Compose 单模块。
- Rationale: 核心能力（requestPinShortcut、图标 Bitmap 处理、QUERY_ALL_PACKAGES 候选解析、透明 RouteActivity）全部依赖 Android 系统 API；uni-app 需 Native.js/UTS 插件桥接，增加启动层与包体成本。
- Alternatives considered: uni-app + UTS 原生插件；被否，理由见 `DEVELOPMENT_PLAN.md` §4.1。

## D2: Gradle 版本与 wrapper 分发源

- Decision: 使用 Gradle 8.14.3 wrapper，distributionUrl 指向腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-8.14.3-bin.zip`。
- Rationale: 计划书为 Gradle 8.9，但本机 wrapper 缓存仅有 8.14.3/8.3/9.1.0；AGP 8.7.3 要求 Gradle ≥ 8.9，8.14.3 满足且与 Kotlin 2.0.21 兼容。官方 services.gradle.org 在本网络环境返回 307 导致 wrapper 校验失败，腾讯镜像 206 可达。
- Alternatives considered: Gradle 8.9（需额外下载发行版，无必要）；Gradle 9.1.0（AGP 8.7.3 不兼容）。

## D3: 首次选择面板承载方式

- Decision: 由透明主题的 RouteActivity 直接 setContent 渲染 Compose 选择面板（Card + 遮罩），选完即启动目标并 finish。
- Rationale: 避免再注册一个 Activity，减少路由链路中的跳转与闪烁；`noHistory` + `excludeFromRecents` + 透明窗口保证无中间界面。
- Alternatives considered: 独立 RouteChoiceActivity（多一层跳转，可能产生闪烁）。

## D4: 图片选择与图标处理

- Decision: 使用 `ActivityResultContracts.PickVisualMedia`（系统 Photo Picker，无存储权限；低版本自动回退到 ACTION_OPEN_DOCUMENT）；采样解码限制到 1024px 以内，居中裁剪为正方形后压缩为 512x512 PNG 存入 `filesDir/icons/<id>.png`；桌面图标用 `createWithAdaptiveBitmap` 并在 legacy 广播路径把内容缩到画布 66% 以预留遮罩安全区。
- Rationale: 满足"大图不 OOM"与"无存储权限"两条硬约束。
- Alternatives considered: 申请 READ_MEDIA_IMAGES 权限（违反无权限目标）。

## D5: 路由候选解析

- Decision: `Intent(ACTION_VIEW, uri).addCategory(CATEGORY_BROWSABLE)` + `queryIntentActivities(…, MATCH_ALL)`，按包名去重、排除自身、按 label 排序；默认浏览器通过 `resolveActivity(https 探测链接)` 获取；HTTP/HTTPS 始终追加"浏览器"选项。
- Rationale: `QUERY_ALL_PACKAGES` 已声明，MATCH_ALL 可拿到全部可处理组件；不写死组件名，目标 App 升级后每次重新解析。
- Alternatives considered: 写死哔哩哔哩/微信读书组件名（脆弱，计划书明确否决）。

## D6: 数据存储

- Decision: 单文件 DataStore Preferences；`shortcuts` 键存 `List<ShortcutRecord>` JSON，`route_rules` 键存 `Map<domainKey, RouteTarget>` JSON；图标文件存应用私有目录。
- Rationale: 数据量小、无查询需求，DataStore 比 Room 轻；完全离线且卸载即删。
- Alternatives considered: Room（对几十条记录过重）；SharedPreferences（计划书指定 DataStore）。

## D7: 桌面固定降级

- Decision: `ShortcutManagerCompat.requestPinShortcut`（自动使用 ShortcutManager），返回 false 或不支持时发 `com.android.launcher.action.INSTALL_SHORTCUT` 广播（带权限声明）。
- Rationale: 国产桌面/旧系统对 API 支持不一致，双路径覆盖计划书 §12 风险。
- Alternatives considered: 仅用新版 API（OPPO 桌面可能不支持）。
