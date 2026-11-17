# 链接直达（LinkPin）

Android 本地小工具：把视频、书籍或网页链接固定为桌面快捷方式。点击快捷方式时按域名直达目标 App 或默认浏览器，同一域名只问一次"用哪个打开"。

- 应用 ID：`com.local.linkpin`
- 平台：Android 8.0（API 26）及以上
- 特性：完全离线、自定义名称与相册图标（选图后可拖动/缩放裁剪、点击预览）、默认字母图标、首次路由选择（可勾选“记住”）与域名记忆、完整编辑/删除/重新固定

## 构建

前置条件：JDK 17、Android SDK 35（`local.properties` 中配置 `sdk.dir`）。首次构建需要联网下载依赖，Gradle wrapper 使用腾讯镜像。

```cmd
gradlew.bat testDebugUnitTest assembleDebug
```

产物：`app\build\outputs\apk\debug\app-debug.apk`（debug 签名，可直接侧载）

## 安装

```cmd
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

安装后权限说明：

- `QUERY_ALL_PACKAGES`：查询能处理目标链接的已安装应用（自用 MVP 采用；上商店前需改为定向 `<queries>`）。
- `com.android.launcher.permission.INSTALL_SHORTCUT`：旧版桌面快捷方式降级广播。
- 不申请网络、存储、后台、账号等任何权限。

## 使用

1. 打开应用，点右下角 +，粘贴链接（如 `https://www.bilibili.com/video/BV1xx`）、填写名称，可选相册图片作为图标（选图后可拖动/缩放裁剪）。
2. 保存后系统会请求把快捷方式固定到桌面。
3. 首次点击桌面图标时，同一域名会出现一次"选择打开方式"面板；勾选"记住我的选择"后同域名其他链接直接打开、不再询问，不勾选则下次继续询问。
4. 列表页支持编辑、删除与重新固定。

## 已知限制

- 桌面已固定的旧图标无法由应用自动删除；删除记录后需长按桌面图标手动移除。
- 短链（`b23.tv`、`v.douyin.com` 等）可能先经浏览器跳转一次，建议输入最终内容页地址。
- 部分国产桌面对快捷方式固定支持不一致：不支持时会尝试旧版 INSTALL_SHORTCUT 广播，仍失败请在列表页"重新固定"。
- 数据仅存本机；卸载应用会清除所有记录、规则与图标文件。

## 验收

- 单元测试：`gradlew.bat testDebugUnitTest`（链接校验、路由键、序列化）。
- 真机验收清单见 `specs/001-linkpin-shortcuts/quickstart.md` 与 `DEVELOPMENT_PLAN.md` §10.2（需目标 OPPO 手机）。
