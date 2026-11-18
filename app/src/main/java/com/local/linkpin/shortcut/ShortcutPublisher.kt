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
    const val EXTRA_SHORTCUT_LABEL = "shortcut_label"

    /**
     * 直达内容的意图：桌面快捷方式与应用内列表点击共用同一条路由。
     * 调用方从非 Activity Context 启动时，自行补 FLAG_ACTIVITY_NEW_TASK。
     */
    fun routeIntent(context: Context, shortcutId: String, label: String? = null): Intent =
        Intent(context, RouteActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_ID, shortcutId)
            if (!label.isNullOrBlank()) putExtra(EXTRA_SHORTCUT_LABEL, label)
        }

    /**
     * 尽力请求桌面移除旧图标：仅对旧版 INSTALL_SHORTCUT 广播安装的图标有效，
     * requestPinShortcut 固定的图标系统不允许应用移除。
     */
    fun requestLegacyUninstall(context: Context, label: String?, intent: Intent) {
        runCatching {
            val uninstall = Intent("com.android.launcher.action.UNINSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
                if (!label.isNullOrBlank()) putExtra(Intent.EXTRA_SHORTCUT_NAME, label)
                putExtra("duplicate", false)
            }
            context.sendBroadcast(uninstall)
        }
    }
}

/**
 * 桌面快捷方式发布：
 * - 桌面支持时 requestPinShortcut；
 * - 不支持时降级为旧版 INSTALL_SHORTCUT 广播；
 * - 编辑走 updateShortcuts；桌面入口由列表按需调用 create。
 */
class ShortcutPublisher(
    private val context: Context,
    private val iconProcessor: IconProcessor,
) {

    private fun routeIntent(record: ShortcutRecord): Intent =
        ShortcutContract.routeIntent(context, record.id, record.label).apply {
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

    /**
     * 删除记录时清理应用私有目录内的图标文件，并尽力请求桌面移除旧图标。
     *
     * 说明：通过 requestPinShortcut 固定的桌面快捷方式，系统不允许应用自行移除
     * （只能由用户长按图标删除），因此这里只做两类尽力而为的清理：
     * 1. 旧版 INSTALL_SHORTCUT 广播安装的图标，用 UNINSTALL_SHORTCUT 广播移除；
     * 2. removeLongLivedShortcuts 清理可能存在的动态长驻快捷方式。
     */
    fun deleteRecord(record: ShortcutRecord) {
        record.iconPath?.let { path ->
            runCatching { java.io.File(path).delete() }
        }
        ShortcutContract.requestLegacyUninstall(context, record.label, routeIntent(record))
        runCatching {
            ShortcutManagerCompat.removeLongLivedShortcuts(context, listOf(record.id))
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
