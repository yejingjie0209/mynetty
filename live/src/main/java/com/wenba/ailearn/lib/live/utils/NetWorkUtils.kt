@file:Suppress("DEPRECATION")

package com.wenba.ailearn.lib.live.utils

import android.content.Context
import android.net.ConnectivityManager


/**
 * @author Jason
 * @description:网络是否可用
 * @date :2019/5/5 11:32 AM
 */
fun Context.isNetworkConnected(): Boolean {
    val mConnectivityManager = this
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val mNetworkInfo = mConnectivityManager.activeNetworkInfo
    if (mNetworkInfo != null) {
        return mNetworkInfo.isAvailable
    }
    return false
}