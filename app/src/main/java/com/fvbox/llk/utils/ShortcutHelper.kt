package com.fvbox.llk.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat


object ShortcutHelper {

    fun addDeskTopShortCutCompat(
        ctx: Context,
        uid: Int,
        name: String,
        icon: Drawable,
    ) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) {
            val intent = Intent()
            intent.`package` = ctx.packageName
            intent.action = "${ctx.packageName}.va.action.shortcut"
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.putExtra("uid", uid)

            val pinShortcutInfo = ShortcutInfoCompat.Builder(ctx, uid.toString())
                .setShortLabel(name)
                .setLongLabel(name)
                .setIcon(
                    IconCompat.createWithBitmap(getBitmapFromDrawable(icon))
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


    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bmp
    }
}