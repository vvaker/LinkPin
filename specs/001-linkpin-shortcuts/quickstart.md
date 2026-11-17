# Quickstart: 链接直达（LinkPin）验证指南

## 前置条件

- JDK 17（`JAVA_HOME=D:\Android\Java\jdk-17`）
- Android SDK 35（`sdk.dir` 见 `local.properties`）
- 构建机可联网（首次构建下载依赖；Gradle wrapper 走腾讯镜像）

## 构建与测试命令（仓库根目录）

```cmd
gradlew.bat testDebugUnitTest assembleDebug
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

## 安装

```cmd
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 核心验证场景（有真机时）

1. 新建快捷方式：输入 `https://www.bilibili.com/video/BV1xx` 与自定义名称，选相册大图 → 桌面出现图标，不崩溃。
2. 不选图保存 → 默认字母图标可辨识。
3. 重启应用 → 记录仍在。
4. 编辑名称/图标 → 桌面刷新或可"重新固定"。
5. 删除 → 提示长按桌面图标移除。
6. 首次点击桌面图标 → 出现"哔哩哔哩 / 浏览器"选择；选择后直达视频。
7. 同域名其他链接再次点击 → 不再询问。
8. 卸载目标 App 后点击 → 重新询问，不崩溃。
9. 点击已删除记录的旧图标 → 仅提示"已删除"。

## 无真机的替代验证

- `gradlew.bat testDebugUnitTest` 覆盖链接校验、路由键、序列化等纯逻辑。
- `adb devices` 无设备时，真机清单（`DEVELOPMENT_PLAN.md` §10.2）由用户在目标 OPPO 手机上人工执行。
