# 链接直达（LinkPin）Android 自用 MVP 开发计划

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 文档状态 | 已确认，待执行 |
| 更新日期 | 2026-11-16 |
| 目标平台 | Android |
| 交付方式 | 个人侧载 APK |
| 应用名称 | 链接直达 |
| Application ID | `com.local.linkpin` |

## 1. 项目概述

“链接直达”是一个 Android 本地小工具，用于把视频、书籍或普通网页链接固定为桌面快捷方式。用户点击快捷方式后，工具根据链接类型和已保存的域名规则，直接打开目标 App 的具体内容；没有对应 App 时回落到浏览器。

核心目标是消除重复的“是否用 App 打开”询问，并提供完全自定义的快捷方式名称和图标。

典型场景：

- 把哔哩哔哩视频链接固定到桌面，点击后直接进入该视频。
- 把微信读书书籍页固定到桌面，点击后直接进入书籍内容。
- 把普通网页固定到桌面，点击后由用户选择的浏览器打开。

## 2. 成功标准

- 同一域名首次点击时允许选择目标 App 或浏览器，之后不再询问。
- 已安装目标 App 时，可以直接进入对应视频、书籍或内容页。
- 快捷方式支持自定义名称和相册图片图标。
- 支持创建、查看、编辑、删除和重新固定快捷方式。
- 全程离线，不申请网络、存储、后台或账号权限。
- 在目标 OPPO 手机上成功安装、固定快捷方式并完成真机直达验证。

## 3. 范围

### 3.1 MVP 包含

- 新建快捷方式：链接、名称、图标。
- 首次路由选择：同一域名仅选择一次，之后记住结果。
- 路由目标：已安装 App 或默认浏览器。
- 自定义图标：相册选图、裁剪缩放、默认字母图标。
- 快捷方式管理：列表、完整编辑、删除、重新固定。
- Android 8.0 及以上版本。
- 中文界面和本地数据存储。

### 3.2 MVP 不包含

- iOS、H5、小程序或其他跨端版本。
- 账号、云同步、备份恢复和多设备同步。
- 联网解析短链接、自动抓取网页图标或内容预览。
- 广告、统计、崩溃上报或其他遥测。
- 应用商店上架、商用合规、备案和隐私政策。
- 批量导入、分组、排序和复杂规则引擎。

## 4. 技术栈

| 层级 | 选择 |
| --- | --- |
| 开发语言 | Kotlin 2.0.21 |
| 界面 | Jetpack Compose + Material 3 |
| 架构 | 单模块、MVVM、StateFlow |
| 存储 | DataStore Preferences + kotlinx.serialization |
| 构建 | Gradle 8.9 + AGP 8.7.3 + JDK 17 |
| SDK | compileSdk 35、targetSdk 35、minSdk 26 |
| 快捷方式 | ShortcutManager + 旧版 INSTALL_SHORTCUT 降级 |
| 依赖管理 | Gradle Version Catalog |
| 测试 | JUnit 单元测试 + OPPO 真机验收 |

主要依赖：

- Compose BOM `2024.10.01`
- Activity Compose `1.9.3`
- Lifecycle `2.8.7`
- DataStore Preferences `1.1.1`
- kotlinx.serialization JSON `1.7.3`

### 4.1 为什么选择原生 Kotlin

本工具的核心能力几乎全部依赖 Android 系统 API：

- 创建并固定桌面快捷方式。
- 把用户选择的图片转换为快捷方式 Bitmap 图标。
- 查询已安装应用能够处理哪些链接。
- 通过透明 Activity 启动目标 App，避免展示中间界面。

uni-app 的标准 API 无法覆盖这些能力。若使用 uni-app，仍需通过 Native.js、UTS 原生插件或离线打包编写 Android 原生代码，并额外承担 JS 桥接、启动层、包体增大和调试复杂度。因此 MVP 直接使用 Kotlin + Compose。

未来如果产品扩展为跨端内容管理器，可以保留 Android 原生快捷方式模块，再用 uni-app 承载其他端的 UI。

## 5. 产品行为

### 5.1 新建快捷方式

用户填写：

- 链接：HTTP/HTTPS URL 或合法的自定义 scheme。
- 名称：1 至 40 个字符。
- 图标：可从相册选择，也可使用自动生成的默认图标。

保存流程：

1. 校验输入。
2. 生成唯一的 `shortcut_id`。
3. 保存快捷方式记录和图标文件。
4. 构建 ShortcutInfo 和不可变 PendingIntent。
5. 调用 `requestPinShortcut` 请求桌面固定。
6. 若当前桌面不支持，降级为旧版 `INSTALL_SHORTCUT` 广播。

### 5.2 首次路由与域名记忆

`RouteActivity` 通过 Intent extra `shortcut_id` 接收快捷方式标识，读取记录后执行路由。

候选查询：

- 使用 `ACTION_VIEW`、`CATEGORY_BROWSABLE` 和链接数据查询 Activity。
- 按包名去重，排除本应用自身。
- 通过通用 HTTPS 链接识别浏览器类应用。
- HTTP/HTTPS 链接始终允许选择“浏览器”。

首次访问某个域名：

- 若存在多个可选目标，展示一次选择面板，列出目标 App 和浏览器。
- 若只有一个目标，直接打开，不展示选择面板。
- 用户选择后，将结果保存到域名规则。
- 用户关闭选择面板时不保存规则，下次点击会再次询问。

后续访问同一域名：

- 直接读取已保存规则并启动目标。
- 不再展示应用选择器、浏览器提示条或中间 UI。
- 目标 App 被卸载或无法处理链接时，清除规则并重新进入一次选择流程。
- 浏览器规则每次运行时重新解析当前默认浏览器。

路由键规则：

- HTTP/HTTPS：使用小写 host，例如 `www.bilibili.com`。
- 自定义 scheme：存在 host 时使用 `scheme://host`，否则使用 `scheme:`。
- 路径和查询参数不参与路由键，保证同一站点共享选择结果。

### 5.3 编辑、删除与重新固定

- 编辑支持修改链接、名称和图标。
- 编辑后更新本地记录，并调用 `ShortcutManager.updateShortcuts` 刷新桌面图标和名称。
- 如果桌面未刷新，提供“重新固定”操作。
- 删除会移除应用内记录和对应图标文件。
- 桌面已经固定的旧图标无法保证由应用自动移除，界面提示用户长按桌面图标手动删除。
- 删除记录后再次点击旧图标，只提示“快捷方式已删除”，不得崩溃。

### 5.4 图标处理

- 使用系统 Photo Picker 选择图片，不申请存储权限。
- 解码时限制目标尺寸，避免大图导致内存溢出。
- 图片居中裁剪为正方形，保存为 512x512 PNG。
- 生成适配 ShortcutManager 的 Bitmap 图标，并预留启动器遮罩安全区。
- 未选择图片时，根据名称首字和哈希颜色生成默认图标。
- 删除或替换图标时，只清理应用私有目录内的旧文件。

### 5.5 输入校验与安全

名称：

- 去除首尾空白后不能为空。
- 长度不超过 40 个字符。

链接：

- HTTP/HTTPS 必须存在合法 host。
- 自定义 scheme 必须具有非空 scheme，且不能包含空白字符。
- 拒绝 `file://`、`content://`、`javascript:` 和 `intent:`。
- 对 `b23.tv`、`v.douyin.com` 等短链显示提示：链接可能先经过浏览器跳转，建议使用最终内容页地址。
- 不自动联网展开短链。

## 6. 架构与数据流

工程保持单模块，建议目录如下：

```text
app/src/main/java/com/local/linkpin/
├── data/        # 数据模型、DataStore 仓库、JSON 序列化
├── shortcut/    # 图标处理、ShortcutManager、旧版快捷方式降级
├── routing/     # 候选解析、路由规则、RouteActivity
├── ui/          # 列表页、创建/编辑页、首次选择面板
├── LinkPinApp.kt
└── MainActivity.kt
```

状态与依赖：

- 使用 `LinkPinApp` 创建简单的手动依赖容器。
- 使用 ViewModel 和 StateFlow 暴露界面状态。
- 不引入 Hilt、Navigation Compose 或 Room。
- 列表页和编辑页通过简单的屏幕状态切换。

主要数据流：

```text
创建/编辑页
  -> ShortcutRepository
  -> ShortcutPublisher
  -> 系统桌面固定快捷方式

桌面快捷方式
  -> RouteActivity
  -> RouteRuleRepository
  -> LinkResolver
  -> 目标 App 或浏览器
```

## 7. 关键接口与数据类型

```kotlin
@Serializable
data class ShortcutRecord(
    val id: String,
    val url: String,
    val label: String,
    val iconPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class RouteRule(
    val domainKey: String,
    val target: RouteTarget,
)

@Serializable
sealed interface RouteTarget {
    @Serializable
    data class App(val packageName: String) : RouteTarget

    @Serializable
    data object Browser : RouteTarget
}

sealed interface LinkTarget {
    data class App(
        val packageName: String,
        val label: String,
        val icon: Any?,
    ) : LinkTarget

    data class Browser(
        val label: String,
        val packageName: String?,
    ) : LinkTarget
}
```

核心接口：

```kotlin
interface ShortcutRepository {
    fun observeAll(): Flow<List<ShortcutRecord>>
    suspend fun upsert(record: ShortcutRecord)
    suspend fun delete(id: String)
}

interface RouteRuleRepository {
    suspend fun get(domainKey: String): RouteTarget?
    suspend fun put(rule: RouteRule)
    suspend fun clear(domainKey: String)
}

interface ShortcutPublisher {
    suspend fun create(record: ShortcutRecord): Boolean
    suspend fun update(record: ShortcutRecord): Boolean
    suspend fun repin(record: ShortcutRecord): Boolean
    suspend fun deleteRecord(record: ShortcutRecord)
}

interface LinkResolver {
    suspend fun resolve(url: String): List<LinkTarget>
    suspend fun launch(target: RouteTarget, url: String): Boolean
}
```

`RouteActivity` 的输入契约固定为：

```kotlin
Intent(context, RouteActivity::class.java).apply {
    putExtra("shortcut_id", record.id)
}
```

## 8. Android 清单与权限

需要声明：

- `android.permission.QUERY_ALL_PACKAGES`
- `com.android.launcher.permission.INSTALL_SHORTCUT`

不声明：

- `android.permission.INTERNET`
- 存储权限
- 前台服务权限
- 后台任务或自启动权限

`RouteActivity` 配置：

- `android:exported="false"`
- 透明窗口主题
- `android:noHistory="true"`
- `android:excludeFromRecents="true"`

快捷方式 PendingIntent 必须使用：

- `FLAG_UPDATE_CURRENT`
- `FLAG_IMMUTABLE`

实际启动目标 App 时使用显式组件，并添加 `FLAG_ACTIVITY_NEW_TASK`。

## 9. 实施阶段

预计总投入为 3 至 5 个开发日。

### 阶段一：工程骨架

任务：

- 创建 Gradle Wrapper、单模块 Android 工程和 Version Catalog。
- 配置 Kotlin、Compose、Material 3、DataStore 和序列化依赖。
- 建立基础主题、应用入口和空白列表页。
- 配置 Manifest 权限与 RouteActivity 占位。

完成标准：

- `gradlew.bat assembleDebug` 成功。
- debug APK 可安装并启动。

### 阶段二：数据与快捷方式

任务：

- 实现快捷方式、路由规则数据模型和 DataStore 仓库。
- 实现 Photo Picker、图标裁剪压缩和默认字母图标。
- 实现 ShortcutManager 创建、更新和重新固定。
- 实现旧版 INSTALL_SHORTCUT 降级路径。

完成标准：

- 能从应用内创建带自定义图标和名称的桌面快捷方式。
- 重启应用后记录仍然存在。

### 阶段三：管理界面

任务：

- 实现快捷方式列表。
- 实现新建和完整编辑表单。
- 实现删除、图标替换和重新固定。
- 增加输入校验和短链提示。

完成标准：

- 创建、编辑、删除闭环可用。
- 大图选择不会崩溃。

### 阶段四：路由与首次选择

任务：

- 实现候选 App 和浏览器识别。
- 实现域名规则存储与失效处理。
- 实现透明 RouteActivity。
- 实现首次选择 Compose 面板。
- 实现显式启动、浏览器回落和错误提示。

完成标准：

- 同一域名首次选择后，后续点击不再询问。
- 哔哩哔哩和微信读书链接可直达内容。
- 目标 App 不存在时浏览器回落正常。

### 阶段五：验证与交付

任务：

- 完成单元测试。
- 在 OPPO 真机执行验收场景。
- 构建 debug APK。
- 编写安装、权限和已知限制说明。

完成标准：

- 所有验收项通过。
- APK 可在目标手机安装并完整使用。

## 10. 测试计划

### 10.1 单元测试

- HTTP/HTTPS URL 校验。
- 自定义 scheme 校验和非法 scheme 拒绝。
- 域名键提取：大小写、端口、查询参数和 fragment。
- 路由规则序列化、读取和清除。
- 候选目标按包名去重和浏览器分类。
- 名称长度和空白处理。

### 10.2 OPPO 真机测试

1. 创建哔哩哔哩视频快捷方式，自定义名称和图标。
2. 首次点击出现“哔哩哔哩 / 浏览器”选择。
3. 选择哔哩哔哩后直达具体视频。
4. 再次点击同一域名的其他视频，不再出现选择。
5. 选择浏览器后，同域名后续直接进入浏览器。
6. 创建微信读书书籍链接并直达对应内容。
7. 创建自定义 scheme 快捷方式并直达目标 App。
8. 卸载目标 App 后点击快捷方式，规则失效且无崩溃。
9. 编辑名称、链接和图标后，桌面快捷方式更新或可重新固定。
10. 删除记录后点击旧桌面图标，只提示已删除。
11. 选择超大图片作为图标，不出现内存溢出。
12. 重启手机、应用被杀死后，域名记忆仍然生效。
13. 连续点击后续快捷方式时，不出现可见的中间界面或启动页闪烁。

### 10.3 构建与安装

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 11. 验收清单

- [ ] 快捷方式支持自定义名称。
- [ ] 快捷方式支持相册图片图标。
- [ ] 未选图时提供可辨识的默认图标。
- [ ] 首次点击按域名选择 App 或浏览器。
- [ ] 后续点击不再询问。
- [ ] 哔哩哔哩链接直达具体视频。
- [ ] 微信读书链接直达具体书籍。
- [ ] 浏览器规则使用当前默认浏览器。
- [ ] 目标 App 不存在时正常回落。
- [ ] 支持完整编辑、删除和重新固定。
- [ ] 离线运行，无网络和存储权限。
- [ ] debug APK 在目标 OPPO 手机安装成功。

## 12. 风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 首次构建需要联网下载 Gradle/AGP/Compose | 无缓存时构建失败 | 提前确认网络；构建阶段按需申请网络权限 |
| 国产桌面对固定快捷方式支持不一致 | 图标无法固定 | requestPinShortcut 失败时降级到 INSTALL_SHORTCUT |
| 桌面不会立即刷新编辑后的图标 | 显示旧名称或图标 | 提供重新固定操作，并提示手动删除旧图标 |
| 已固定图标无法由应用直接删除 | 桌面残留入口 | 删除时提示用户长按图标移除 |
| 短链依赖浏览器重定向 | 可能再次出现 App 跳转提示 | 输入页提示优先使用最终内容页链接 |
| QUERY_ALL_PACKAGES 不适合商店分发 | 后续商用需要重构 | 自用 MVP 使用该权限；商用版改为定向 queries |
| 目标 App 更新导致 Activity 变化 | 直达失效 | 不写死组件名，每次根据链接重新解析 |
| 相册大图导致内存压力 | 应用崩溃 | 采样解码、限制尺寸并压缩保存 |

## 13. 假设与默认值

- 目标手机运行 Android 8.0 及以上版本。
- MVP 只服务个人侧载，不考虑应用商店审核。
- 使用 `QUERY_ALL_PACKAGES` 简化已安装应用查询。
- 首次选择按域名生效，同一域名共享一个打开方式。
- HTTP/HTTPS 链接存在 App 和浏览器候选时，首次展示选择面板。
- 自定义 scheme 只有一个处理者时直接打开。
- 用户优先粘贴最终内容页链接，工具不联网展开短链。
- 数据只保存在本机，卸载应用会删除快捷方式记录和图标。
- 本机当前没有 Android Studio 和 Gradle 缓存；实现阶段使用 Gradle Wrapper 命令行构建。
- 当前安装有 JDK 17、Android SDK 35 和 adb，但首次构建需要联网获取依赖。

## 14. 后续商用方向

本节不属于 MVP。

若未来考虑商业化，需要补充：

- 将 `QUERY_ALL_PACKAGES` 改为定向 `<queries>` 声明。
- 国内应用商店所需的 APP 备案、隐私政策和资质审核。
- 域名校验和风险提示，防止伪造图标被用于钓鱼。
- 第三方 Logo 和名称的商标风险处理。
- 明确且可持续的商业模式。

独立付费工具的市场空间有限，更适合作为以下产品的组件：

- 链接收藏或稍后读产品。
- 第三方桌面或内容聚合产品。
- 企业内部入口门户和 MDM 快捷入口。
- 营销短链的 App/浏览器直达路由能力。
