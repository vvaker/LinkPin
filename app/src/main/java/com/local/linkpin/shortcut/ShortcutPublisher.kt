package com.local.linkpin.shortcut

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.routing.RouteActivity

/** 桌面快捷方式 → RouteActivity 的意图契约（见 contracts/route-contract.md）。 */
object ShortcutContract {
    const val EXTRA_SHORTCUT_ID = "shortcut_id"
}

/**
 * 桌面快捷方式发布：
 * - 桌面支持时 requestPinShortcut；
 * - 不支持时降级为旧版 INSTALL_SHORTCUT 广播；
 * - 编辑走 updateShortcuts；重新固定重新走 create。
 */
class ShortcutPublisher(
    private val context: Context,
    private val iconProcessor: IconProcessor,
) {

    private fun routeIntent(record: ShortcutRecord): Intent =
        Intent(context, RouteActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(ShortcutContract.EXTRA_SHORTCUT_ID, record.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun buildInfo(record: ShortcutRecord): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, record.id)
            .setShortLabel(record.label)
            .setIcon(IconCompat.createWithAdaptiveBitmap(iconProcessor.loadIcon(record)))
            .setIntent(routeIntent(record))
            .build()

    /** 创建并请求固定；桌面不支持固定请求时降级旧版广播。 */
    fun create(record: ShortcutRecord): Boolean {
        return if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, buildInfo(record), null)
        } else {
            legacyInstall(record)
        }
    }

    /** 编辑后刷新桌面快捷方式。 */
    fun update(record: ShortcutRecord): Boolean =
        ShortcutManagerCompat.updateShortcuts(context, listOf(buildInfo(record)))

    /** 重新固定：等价于再次 create。 */
    fun repin(record: ShortcutRecord): Boolean = create(record)

    /** 删除记录时清理应用私有目录内的图标文件。 */
    fun deleteRecord(record: ShortcutRecord) {
        record.iconPath?.let { path ->
            runCatching { java.io.File(path).delete() }
        }
    }

    private fun legacyInstall(record: ShortcutRecord): Boolean {
        return try {
            val pending = PendingIntent.getActivity(
                context,
                record.id.hashCode(),
                routeIntent(record),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val install = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, pending)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, record.label)
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON,
                    iconProcessor.legacySafeIcon(iconProcessor.loadIcon(record)),
                )
                putExtra("duplicate", false)
            }
            context.sendBroadcast(install)
            true
        } catch (e: Exception) {
            false
        }
    }
}
