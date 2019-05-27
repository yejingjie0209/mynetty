package com.wenba.ailearn.lib.live.msg

/**
 * @author Jason
 * @description:日志回调
 * @date :2019/4/26 8:45 PM
 */
interface LiveLogCallback {
    fun v(msg: String, code: Int? = 0)
    fun e(msg: String, code: Int? = 0)
}