package com.wenba.ailearn.lib.live.i

import com.wenba.ailearn.lib.live.constant.ConnectState

/**
 * @author Jason
 * @description:连接状态改变
 * @date :2019/4/28 12:06 PM
 */
interface ConnectStateChanged {
    /**
     * 连接状态改变
     */
    fun connectChanged(userId: String, state: ConnectState)
}