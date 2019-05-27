package com.wenba.ailearn.lib.live.helper

import android.content.Context
import com.wenba.ailearn.lib.live.client.LiveClient
import com.wenba.ailearn.lib.live.constant.ConnectState
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.model.UserModel
import com.wenba.ailearn.lib.live.msg.LiveLogCallback
import com.wenba.ailearn.lib.live.msg.LiveMessageHelper

/**
 * @author Jason
 * @description: 客户端帮助类
 * @date :2019/4/28 3:38 PM
 */
class LiveClientHelper(context: Context) {
    var userId: String? = null
    private val liveClient = LiveClient(context)

    /**
     * 客户端登录
     * @param port 端口号
     * @param host ip地址
     * @param userId 用户id，唯一
     * @param password 密码（非必填）
     * @param userType 用户类型(非必填)
     * @param liveStateChanged 连接状态改变回调
     */
    fun login(
        port: Int,
        host: String,
        userId: String,
        password: String,
        userType: Int,
        liveStateChanged: ConnectStateChanged
    ) {
        this.userId = userId
        liveClient.start(userId, port, host, object : ConnectStateChanged {
            override fun connectChanged(userId: String, state: ConnectState) {
                liveStateChanged.connectChanged(userId, state)
                when (state) {
                    ConnectState.CONNECTED -> {
                        liveClient.liveMessageHelper.sendMessageToServer(UserModel(userId, password, userType))
                    }
                    else -> Unit
                }
            }
        })
    }

    /**
     * 客户端注销
     */
    fun logout() {
        liveClient.stop()
    }


    /**
     * 设置日志监听回调
     * @param liveLogCallback 日志回调
     */
    fun setLiveLogCallback(liveLogCallback: LiveLogCallback) {
        liveClient.liveLogCallback = liveLogCallback
    }

    /**
     * 消息通信帮助类
     * @return 消息通信帮助类，该类为用户客户端、服务端之前相互发送消息的重要类
     */
    fun getLiveMessageHelper(): LiveMessageHelper {
        return liveClient.liveMessageHelper
    }
}