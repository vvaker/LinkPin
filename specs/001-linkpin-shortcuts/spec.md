# Feature Specification: 链接直达（LinkPin）Android 自用 MVP

**Feature Branch**: `001-linkpin-shortcuts`

**Created**: 2026-11-16

**Status**: Draft

**Input**: 用户描述："执行该工作区下的开发计划" —— 即工作区根目录 `DEVELOPMENT_PLAN.md` v1.0（状态：已确认，待执行）。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 新建并固定桌面快捷方式 (Priority: P1)

用户把视频、书籍或网页链接粘贴进应用，填写 1-40 个字符的自定义名称，可选相册图片作为图标（不选则使用按名称首字和哈希颜色自动生成的默认图标）。保存后应用请求系统把快捷方式固定到桌面。

**Why this priority**: 创建与固定是整个产品唯一的入口能力，没有它后续管理、路由全部无法触达。

**Independent Test**: 在 Android 8.0 及以上设备安装 APK 后，创建一条哔哩哔哩链接快捷方式（自定义名称 + 相册图标），桌面出现对应图标；重启应用后记录仍然存在。

**Acceptance Scenarios**:

1. **Given** 用户在新建页，**When** 填写合法链接与 1-40 字符名称并保存，**Then** 系统发起桌面固定请求并生成快捷方式。
2. **Given** 用户未选择图片，**When** 保存，**Then** 图标为按名称首字和哈希颜色生成的默认字母图标。
3. **Given** 用户选择相册中的超大图片，**When** 保存，**Then** 图片被采样解码、居中裁剪为正方形并压缩保存，不出现内存溢出。
4. **Given** 当前桌面不支持固定请求，**When** 保存，**Then** 降级为旧版 INSTALL_SHORTCUT 广播。
5. **Given** 应用重启，**When** 打开列表页，**Then** 之前创建的快捷方式记录仍然存在。

---

### User Story 2 - 快捷方式管理（查看/编辑/删除/重新固定） (Priority: P2)

用户在应用内查看已创建的快捷方式，支持完整编辑（链接、名称、图标）、删除，以及桌面未刷新时重新固定。

**Why this priority**: 没有管理能力，用户无法纠正错误输入或更换图标，产品不可长期使用。

**Independent Test**: 创建一条记录后，在列表页执行编辑（改名称和图标）、删除、重新固定，桌面图标与本地记录同步更新。

**Acceptance Scenarios**:

1. **Given** 已有快捷方式记录，**When** 打开应用，**Then** 列表展示每条记录的名称、图标和链接。
2. **Given** 用户在编辑页修改名称/链接/图标，**When** 保存，**Then** 本地记录更新，并通过 updateShortcuts 刷新桌面快捷方式。
3. **Given** 桌面未刷新编辑结果，**When** 用户点击"重新固定"，**Then** 重新发起固定请求。
4. **Given** 用户删除记录，**When** 确认删除，**Then** 本地记录与图标文件被移除，界面提示用户长按桌面图标手动删除。
5. **Given** 记录已删除，**When** 用户点击桌面残留旧图标，**Then** 仅提示"快捷方式已删除"，不崩溃。

---

### User Story 3 - 点击直达与域名记忆 (Priority: P3)

用户点击桌面快捷方式后，应用按链接类型解析候选目标（已安装 App 与浏览器）。同一域名首次出现多个候选时展示一次选择面板，用户选择后记住结果，后续点击直达；无对应 App 时回落浏览器；目标失效时清除规则并重新询问。

**Why this priority**: 这是产品的核心价值——消除重复的"是否用 App 打开"询问，但它依赖 P1/P2 提供数据基础。

**Independent Test**: 创建哔哩哔哩视频快捷方式，首次点击出现"哔哩哔哩 / 浏览器"选择，选择后直达视频；再次点击同一域名其他链接不再出现选择。

**Acceptance Scenarios**:

1. **Given** 同域名首次点击且存在多个候选，**When** 点击桌面图标，**Then** 显示一次选择面板（目标 App 与浏览器选项）。
2. **Given** 用户已做过选择，**When** 再次点击同域名任意链接，**Then** 直接打开所选目标，不再询问。
3. **Given** 该链接只有一个可处理 App 且非 HTTP/HTTPS，**When** 点击，**Then** 直接打开，不显示面板。
4. **Given** 目标 App 被卸载或无法处理链接，**When** 点击，**Then** 清除规则并重新进入选择流程，不崩溃。
5. **Given** 规则为浏览器，**When** 点击，**Then** 使用当前默认浏览器打开。
6. **Given** HTTP/HTTPS 链接没有任何 App 候选，**When** 点击，**Then** 直接由浏览器打开。
7. **Given** 用户关闭选择面板而未选择，**When** 下次点击，**Then** 再次询问（不保存规则）。
8. **Given** 点击后启动目标，**When** 观察屏幕，**Then** 不出现可见的中间界面或启动页闪烁。

### Edge Cases

- 短链（`b23.tv`、`v.douyin.com` 等）：输入页显示提示"链接可能先经过浏览器跳转，建议使用最终内容页地址"；不联网展开。
- 非法链接：拒绝 `file://`、`content://`、`javascript:`、`intent:`、空白 scheme、无 host 的 HTTP/HTTPS、含空白字符的链接。
- 名称为空白或超过 40 个字符时不允许保存。
- 路由键提取：HTTP/HTTPS 用小写 host；自定义 scheme 有 host 用 `scheme://host`、无 host 用 `scheme:`；端口、路径、查询参数和 fragment 不参与。
- 桌面已固定的旧图标无法由应用自动移除：删除时提示用户长按图标手动删除。
- 数据仅存本机，卸载应用会清除全部记录与图标文件。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 支持创建快捷方式记录，字段包含唯一 id、链接、名称、图标路径、创建/更新时间。
- **FR-002**: 系统 MUST 校验名称（去除首尾空白后非空、长度 ≤ 40 字符）与链接（HTTP/HTTPS 必须有合法 host；自定义 scheme 非空且无空白；拒绝 file/content/javascript/intent）。
- **FR-003**: 系统 MUST 对 `b23.tv`、`v.douyin.com` 等短链显示跳转提示，且不联网展开。
- **FR-004**: 系统 MUST 在保存后请求桌面固定（requestPinShortcut），桌面不支持时降级为 INSTALL_SHORTCUT 广播。
- **FR-005**: 系统 MUST 使用系统 Photo Picker 选择图标，不申请存储权限；采样解码大图；居中裁剪为正方形并保存为 512x512 PNG；为桌面图标预留遮罩安全区。
- **FR-006**: 系统 MUST 在未选图时按名称首字和哈希颜色生成默认图标。
- **FR-007**: 系统 MUST 支持列表查看、完整编辑、删除与重新固定；编辑后调用 updateShortcuts 刷新桌面。
- **FR-008**: 系统 MUST 在删除时移除记录与对应图标文件，并提示用户手动删除桌面残留图标。
- **FR-009**: 系统 MUST 使点击已删除记录对应的旧桌面图标时只提示"快捷方式已删除"，不得崩溃。
- **FR-010**: 系统 MUST 按域名记忆首次选择：多候选时展示一次选择面板；单候选直接打开；选择结果按路由键持久化。
- **FR-011**: 系统 MUST 在规则目标失效（App 卸载或无法处理）时清除规则并重新进入选择流程。
- **FR-012**: 系统 MUST 使浏览器规则在每次运行时重新解析当前默认浏览器。
- **FR-013**: 系统 MUST 使用 `ACTION_VIEW` + `CATEGORY_BROWSABLE` 解析候选，按包名去重并排除自身；HTTP/HTTPS 始终允许选择浏览器。
- **FR-014**: 系统 MUST 使用显式组件并以 `FLAG_ACTIVITY_NEW_TASK` 启动目标 App；快捷方式 PendingIntent 使用 `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`。
- **FR-015**: 系统 MUST 声明 `QUERY_ALL_PACKAGES` 与 `com.android.launcher.permission.INSTALL_SHORTCUT` 权限，不声明 INTERNET、存储、前台服务权限。
- **FR-016**: 系统 MUST 全程离线运行，数据仅存本机（DataStore + 应用私有目录）。
- **FR-017**: 系统 MUST 支持 Android 8.0（API 26）及以上，中文界面。

### Key Entities *(include if feature involves data)*

- **ShortcutRecord**：一条桌面快捷方式的本地记录；属性为唯一 id、链接 url、名称 label、图标文件路径 iconPath（可空）、createdAt、updatedAt。
- **RouteRule**：一条域名路由规则；属性为 domainKey（路由键）与 target（RouteTarget：App 包名或 Browser）。
- **LinkTarget**：运行期候选目标（App 含包名/标签/图标，Browser 含标签/包名），不持久化。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 用户能在 1 分钟内完成一条快捷方式的创建并固定到桌面。
- **SC-002**: 同域名完成首次选择后，后续点击 100% 不再出现选择面板。
- **SC-003**: 桌面图标点击到目标 App 内容页之间没有任何中间确认步骤或可见闪烁。
- **SC-004**: 开发计划 §10.2 的 13 项 OPPO 真机验收场景全部通过。
- **SC-005**: 应用不申请网络与存储权限（安装后权限列表可见验证）。
- **SC-006**: 创建、编辑、删除、重新固定形成完整闭环，重启后数据不丢失。

## Assumptions

- 目标手机为 OPPO 设备，Android 8.0 及以上。
- 仅个人侧载使用，不涉及应用商店审核，`QUERY_ALL_PACKAGES` 可接受。
- 首次选择按域名生效，同一域名共享一个打开方式。
- 用户优先粘贴最终内容页链接，工具不联网展开短链。
- 构建机已具备 JDK 17、Android SDK 35 与 adb；首次构建需要联网下载依赖。
- 真机验收依赖目标 OPPO 手机连接；无真机时该项延期并由人工执行。
