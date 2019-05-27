package com.wenba.ailearn.lib.live.utils

import android.util.Log

/**
 * @author Jason
 * @description:sdk内部日志打印
 * @date :2019/4/26 10:03 AM
 */
object LiveLocalLog {
    const val TAG = "LiveLocalLog"

    fun v(msg: String) {
        Log.v(TAG, msg)
    }

    fun i(msg: String) {
        Log.i(TAG, msg)
    }

    fun w(msg: String) {
        Log.w(TAG, msg)
    }

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
    }

}