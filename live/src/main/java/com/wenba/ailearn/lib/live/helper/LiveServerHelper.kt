package com.wenba.ailearn.lib.live.helper

import android.content.Context
import com.wenba.ailearn.lib.live.msg.LiveLogCallback
import com.wenba.ailearn.lib.live.msg.LiveMessageHelper
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.server.LiveServer

/**
 * @author Jason
 * @description:服务端帮助类
 * @date :2019/4/28 3:38 PM
 */
class LiveServerHelper(context: Context) {
    private val liveServer = LiveServer(context)


    /**
     * 服务端登录
     * @param port 端口
     */
    fun login(
        port: Int
    ) {
        liveServer.start(port)
    }

    /**
     * 服务端注销
     */
    fun logout() {
        liveServer.stop()
    }


    /**
     * 关闭某个连接
     * @param user 断开指定user的连接,如果参数为null，断开所有连接
     */
    fun disconnect(user: String?) {
        liveServer.disconnect(user)
    }


    /**
     * 设置日志监听回调
     * @param liveLogCallback 日志回调
     */
    fun setLiveLogCallback(liveLogCallback: LiveLogCallback) {
        liveServer.liveLogCallback = liveLogCallback
    }

    /**
     * 消息通信帮助类
     * @return 消息通信帮助类，该类为用户客户端、服务端之前相互发送消息的重要类
     */
    fun getLiveMessageHelper(): LiveMessageHelper {
        return liveServer.liveMessageHelper
    }

    /**
     * 连接状态改变
     */
    fun setConnectStateChanged(connectStateChanged: ConnectStateChanged) {
        liveServer.connectStateChanged = connectStateChanged
    }

}