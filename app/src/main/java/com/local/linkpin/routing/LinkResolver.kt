package com.local.linkpin.routing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.local.linkpin.data.LinkTarget
import com.local.linkpin.data.RouteTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 候选解析与启动：
 * - ACTION_VIEW + CATEGORY_BROWSABLE 查询，按包名去重、排除自身；
 * - 默认浏览器用 https 探测链接解析（每次运行重新解析）；
 * - 显式组件 + FLAG_ACTIVITY_NEW_TASK 启动，失败返回 false。
 */
class LinkResolver(private val context: Context) {

    data class Resolution(
        val apps: List<LinkTarget.App>,
        val browser: LinkTarget.Browser?,
    )

    suspend fun resolve(url: String): Resolution = withContext(Dispatchers.IO) {
        val uri = Uri.parse(url)
        val queryIntent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        val infos = runCatching {
            context.packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        val apps = infos
            .mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val label = runCatching { info.loadLabel(context.packageManager).toString() }
                    .getOrDefault(packageName)
                LinkTarget.App(packageName = packageName, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label }

        Resolution(apps = apps, browser = resolveDefaultBrowser())
    }

    suspend fun launch(target: RouteTarget, url: String): Boolean = withContext(Dispatchers.IO) {
        when (target) {
            is RouteTarget.App -> launchApp(target.packageName, url)
            RouteTarget.Browser -> launchBrowser(url)
        }
    }

    private fun resolveDefaultBrowser(): LinkTarget.Browser? {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkpin.invalid/"))
        return runCatching {
            val info = context.packageManager.resolveActivity(probe, PackageManager.MATCH_ALL)
                ?: return@runCatching null
            val packageName = info.activityInfo.packageName
            if (packageName == context.packageName) {
                null
            } else {
                LinkTarget.Browser(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = packageName,
                )
            }
        }.getOrNull()
    }

    private fun launchApp(packageName: String, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) == null) return false
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun launchBrowser(url: String): Boolean {
        return try {
            val browser = resolveDefaultBrowser()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                browser?.packageName?.let { setPackage(it) }
            }
            if (browser != null && intent.resolveActivity(context.packageManager) == null) {
                return false
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
