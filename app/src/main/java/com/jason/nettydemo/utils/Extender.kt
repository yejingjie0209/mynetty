package com.jason.nettydemo.utils

import android.content.Context
import android.widget.Toast

/**
 * @author Jason
 * @description:
 * @date :2019/5/6 6:41 PM
 */
fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}