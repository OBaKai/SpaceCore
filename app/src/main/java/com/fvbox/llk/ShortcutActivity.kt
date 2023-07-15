package com.fvbox.llk

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.fvbox.data.BoxRepository

class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        finish()

        val uid = intent?.getIntExtra("uid", -1)
        Log.e("llk", "ShortcutActivity#onStartCommand uid=$uid")
        if (uid != null && uid >= 0){
            BoxRepository.launchApp(WeChatVAMaker.WX_PKG, uid)
        }
    }
}