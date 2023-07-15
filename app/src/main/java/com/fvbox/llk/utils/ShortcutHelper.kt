package com.fvbox.llk.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.fvbox.llk.ShortcutService

object ShortcutHelper {

    fun addDeskTopShortCutCompat(ctx: Context,
                                 uid: Int,
                                 name: String,
                                 icon: Drawable) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) {
            val intent = Intent(ctx, ShortcutService::class.java)
            intent.action = Intent.ACTION_VIEW  //必须设置，否则报错
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) //设不设置都行
            intent.putExtra("uid", uid)

            val pinShortcutInfo = ShortcutInfoCompat.Builder(ctx, uid.toString())
                .setShortLabel(name)
                .setLongLabel(name)
                .setIcon(
                    IconCompat.createWithBitmap((icon as BitmapDrawable).bitmap)
                )
                .setIntent(intent)
                .build()
            val pinnedShortcutCallbackIntent =
                ShortcutManagerCompat.createShortcutResultIntent(ctx, pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                ctx,
                /* request code */ 0,
                pinnedShortcutCallbackIntent, /* flags */
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            ShortcutManagerCompat.requestPinShortcut(
                ctx,
                pinShortcutInfo,
                successCallback.intentSender
            )
        }
    }
}