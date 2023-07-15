package com.fvbox.llk

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.fvbox.data.BoxRepository

/**
 * author: llk
 * date  : 2023/7/15
 * detail:
 */
class ShortcutService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uid = intent?.getIntExtra("uid", -1)
        Log.e("llk", "ShortcutService#onStartCommand uid=$uid")
        if (uid != null && uid >= 0){
            BoxRepository.launchApp(WeChatVAMaker.WX_PKG, uid)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}