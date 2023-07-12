package com.fvbox.llk.utils

import android.app.Application
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.fvbox.R
import com.fvbox.app.ui.main.MainActivity

object ShortcutHelper {
    private val tag = "ShortcutHelper"

    /**
     * 创建快捷方式
     */
    fun createShortSimple(ctx: Context) {
        createShortcut(ctx, MainActivity::class.java.name, ctx.getString(R.string.app_name))
    }

    /**
     * 检查桌面上是否存在快捷方式
     */
    fun checkHasShortcut(ctx: Context): Boolean {
        val shortId = ctx.packageName + APP_SHORTCUT_INFO_ID
        val shortcuts =
            ShortcutManagerCompat.getShortcuts(ctx, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        if (shortcuts.isNotEmpty()) {
            for (shortcut in shortcuts) {
                if (TextUtils.equals(shortId, shortcut.id)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 创建快捷方式
     * label必须是唯一的，如果桌面存在相同label的icon则无法创建快捷方式
     *
     * @param context context
     * @param label   快捷方式名称，可以为空
     */
    private fun createShortcut(context: Context, mainClassName: String, label: String) {
        if (TextUtils.isEmpty(mainClassName)) {
            Log.w(tag, "createShortcut error: empty mainClassName")
            return
        }
        val shortcutSupported: Boolean =
            ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        if (!shortcutSupported) {
            Log.w(tag, "createShortcut error: no shortcutSupported")
            return
        }

        val broadcastIntent = Intent("${context.packageName}.SHORTCUT_ADDED")
        broadcastIntent.setPackage(context.packageName)

        val shortcutInfo: ShortcutInfoCompat = newShortcutInfo(context, mainClassName, label)
        val flag = PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, broadcastIntent, flag)
        // 返回值仅仅是调用成功，并不是快捷方式创建成功
        try {
            val result: Boolean = ShortcutManagerCompat.requestPinShortcut(
                context,
                shortcutInfo,
                pendingIntent.intentSender
            )
            Log.d(tag, "requestPinShortcut result = $result")
        } catch (ex: IllegalStateException) {
            Log.e(tag, "requestPinShortcut error: " + ex.message)
        }
    }

    fun createShortcutMethod2(context: Context) {
        // 创建快捷方式的 Intent
        // 创建快捷方式的 Intent
        val shortcutIntent = Intent(Intent.ACTION_MAIN)
        shortcutIntent.setClassName(context, MainActivity::class.java.name)
        shortcutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        // 创建快捷方式 Intent
        val addIntent = Intent()
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "MyApp")
        addIntent.putExtra(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher)
        )
        addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"

        // 发送广播，创建快捷方式
        context.sendBroadcast(addIntent)
    }

    /**
     * 生成快捷方式信息
     */
    private fun newShortcutInfo(
        ctx: Context,
        mainClassName: String,
        label: String
    ): ShortcutInfoCompat {
        var labelStr = label
        val application: Application = ctx.applicationContext as Application
        val shortcutId = ctx.packageName + APP_SHORTCUT_INFO_ID
        val builder: ShortcutInfoCompat.Builder =
            ShortcutInfoCompat.Builder(application, shortcutId)
        if (TextUtils.isEmpty(labelStr)) {
            labelStr = application.getString(R.string.app_name)
        }
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setPackage(ctx.packageName)
        intent.setClassName(application.packageName, mainClassName)
        return builder
            .setActivity(ComponentName(application, mainClassName)) // 这个必须加防止有些机型快捷方式不显示
            .setIcon(IconCompat.createWithResource(application, R.mipmap.ic_launcher))
            .setLongLabel(labelStr)
            .setShortLabel(labelStr)
            .setIntent(intent)
            .build()
    }

    private const val APP_SHORTCUT_INFO_ID: String = ".shortcutId"
}