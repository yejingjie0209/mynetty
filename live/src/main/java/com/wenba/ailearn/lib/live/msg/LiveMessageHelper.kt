package com.wenba.ailearn.lib.live.msg

import com.wenba.ailearn.lib.live.model.ChannelState
import io.netty.channel.ChannelHandlerContext
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Jason
 * @description: 消息通信帮助类，该类为用户客户端、服务端之前相互发送消息的重要类
 * @date :2019/4/28 10:17 AM
 */
class LiveMessageHelper {
    /**
     * 接收客户端消息回调
     * @param userId 用户Id，唯一
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    var receiveClientMessage: ((userId: String, msg: Any) -> Unit)? = null

    /**
     * 接收服务端消息回调
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    var receiveServerMessage: ((msg: Any) -> Unit)? = null//接收服务端消息

    /**
     * 发送消息给服务端
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    fun sendMessageToServer(msg: Any) {
        ctx?.writeAndFlush(msg)
    }

    /**
     * 发送消息给客户端
     * @param userId 客户端用户id，唯一
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    fun sendMessageToClient(userId: String, msg: Any) {
        userMap.forEach {
            if (it.key == userId) {
                it.value.ctx.writeAndFlush(msg)
                return
            }
        }
    }

    internal var ctx: ChannelHandlerContext? = null
    internal var userMap = ConcurrentHashMap<String, ChannelState>()
}