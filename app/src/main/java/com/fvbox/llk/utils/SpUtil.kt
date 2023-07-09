package com.fvbox.llk.utils

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV
import java.util.*

object SpUtil {

    private lateinit var mmkv: MMKV

    fun init(ctx: Context){
        val dir = MMKV.initialize(ctx)
        mmkv = MMKV.defaultMMKV()
    }

    fun put(key: String, value: Any?) {
        when (value) {
            is String -> mmkv.encode(key, value)
            is Float -> mmkv.encode(key, value)
            is Boolean -> mmkv.encode(key, value)
            is Int -> mmkv.encode(key, value)
            is Long -> mmkv.encode(key, value)
            is Double -> mmkv.encode(key, value)
            is ByteArray -> mmkv.encode(key, value)
            else -> return
        }
    }

    fun <T : Parcelable> put(key: String, t: T?) {
        if (t == null) {
            return
        }
        mmkv.encode(key, t)
    }

    fun put(key: String, sets: Set<String>?) {
        if (sets == null) {
            return
        }
        mmkv.encode(key, sets)
    }

    fun getInt(key: String): Int {
        return mmkv.decodeInt(key, 0)
    }

    fun getDouble(key: String): Double {
        return mmkv.decodeDouble(key, 0.00)
    }

    fun getLong(key: String): Long {
        return mmkv.decodeLong(key, 0L)
    }

    fun getBoolean(key: String): Boolean {
        return mmkv.decodeBool(key, false)
    }

    fun getFloat(key: String): Float {
        return mmkv.decodeFloat(key, 0F)
    }

    fun getByteArray(key: String): ByteArray? {
        return mmkv.decodeBytes(key)
    }

    fun getString(key: String): String? {
        return mmkv.decodeString(key, null)
    }

    fun <T : Parcelable> getParcelable(key: String, tClass: Class<T>): T? {
        return mmkv.decodeParcelable(key, tClass)
    }

    fun getStringSet(key: String): Set<String>? {
        return mmkv.decodeStringSet(key, Collections.emptySet())
    }

    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    fun clearAll() {
        mmkv.clearAll()
    }
}