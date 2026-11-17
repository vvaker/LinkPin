# Contract: 桌面快捷方式 → RouteActivity

这是本应用对外唯一的"外部接口"：桌面快捷方式持有的 PendingIntent 指向 RouteActivity。

## Intent 契约（固定不变）

```kotlin
Intent(context, RouteActivity::class.java).apply {
    action = Intent.ACTION_VIEW
    putExtra("shortcut_id", record.id)  // 必填，String
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

- PendingIntent flags：`FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`。
- RouteActivity 在 Manifest 中 `exported=false`、`noHistory=true`、`excludeFromRecents=true`、透明主题。

## 行为契约

1. 收到 `shortcut_id` 后读取 ShortcutRecord：
   - 记录不存在 → 提示"快捷方式已删除"并结束，不得崩溃。
2. 计算 domainKey，查询 RouteRule：
   - 命中规则 → 直接启动目标；失败（App 卸载等）→ 清除规则并进入选择流程。
   - 未命中 → 单候选（非 HTTP/HTTPS）直接打开；无 App 候选且为 HTTP/HTTPS → 直接浏览器；否则显示选择面板。
3. 选择面板：列出候选 App + 浏览器选项（HTTP/HTTPS 恒有）；选择后持久化规则并启动；关闭面板不保存规则。
4. 启动目标：显式组件 + `FLAG_ACTIVITY_NEW_TASK`；浏览器规则每次重新解析默认浏览器。
5. 全程离线、无可见中间界面。
