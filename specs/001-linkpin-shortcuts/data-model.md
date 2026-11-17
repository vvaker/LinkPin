# Data Model: 链接直达（LinkPin）

## ShortcutRecord（持久化，DataStore 键 `shortcuts`）

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| id | String | 唯一，UUID 生成 |
| url | String | 通过 LinkValidator 校验后的原始链接 |
| label | String | 去除首尾空白后非空，≤ 40 字符 |
| iconPath | String? | `filesDir/icons/<id>.png` 绝对路径；null 表示使用默认字母图标 |
| createdAt | Long | epoch millis，创建时设置 |
| updatedAt | Long | epoch millis，每次编辑更新 |

存储形式：`List<ShortcutRecord>` 的 JSON 数组。

## RouteRule（持久化，DataStore 键 `route_rules`）

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| domainKey | String | 由 DomainKey.of(url) 计算；HTTP/HTTPS 为小写 host，自定义 scheme 为 `scheme://host`（无 host 时为 `scheme:`）；不含端口/路径/查询/fragment |
| target | RouteTarget | `App(packageName)` 或 `Browser`（多态序列化，@SerialName 标记） |

存储形式：`Map<String, RouteTarget>` 的 JSON 对象。

## LinkTarget（运行期，不持久化）

- `LinkTarget.App(packageName, label, icon: Drawable?)`：候选 App。
- `LinkTarget.Browser(label, packageName?)`：浏览器候选（每次运行重新解析默认浏览器）。

## 序列化契约

- JSON 使用 `ignoreUnknownKeys = true`、`classDiscriminator = "type"`；新增字段必须带默认值，保证旧数据可读。
- 损坏 JSON 一律按空数据降级处理，不崩溃。

## 文件契约

- 图标文件：`<filesDir>/icons/<id>.png`，512x512，PNG。
- 删除/替换图标时仅删除应用私有目录内的旧文件，不触碰外部存储。
