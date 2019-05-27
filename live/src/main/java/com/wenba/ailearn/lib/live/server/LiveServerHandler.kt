package com.wenba.ailearn.lib.live.server

import com.wenba.ailearn.lib.live.constant.ConnectState
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.model.ChannelState
import com.wenba.ailearn.lib.live.model.LiveCode
import com.wenba.ailearn.lib.live.model.UserModel
import com.wenba.ailearn.lib.live.msg.LiveLogCallback
import com.wenba.ailearn.lib.live.msg.LiveMessageHelper
import com.wenba.ailearn.lib.live.utils.LiveLocalLog
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.io.Serializable


/**
 * @author Jason
 * @description:服务端handler
 * @date :2019/4/25 7:38 PM
 */
//@ChannelHandler.Sharable//注解@Sharable可以让它在channels间共享
class LiveServerHandler(
    private val liveLogCallback: LiveLogCallback?
    , private val liveMessageHelper: LiveMessageHelper?
    , private val connectStateChanged: ConnectStateChanged?
) : SimpleChannelInboundHandler<Any?>() {
    // 存储连接和用户信息的map，键为连接编号，值为用户名

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (ctx == null) {
            liveLogCallback?.e("server ctx=null")
            return
        }
        val channelId = getChannelId(ctx)
//        liveLogCallback?.v("connect id = $channelId")
        LiveLocalLog.v("connect id = $channelId")

        when (msg) {
            is String -> {
                val uid = getUserId(channelId)
                if (uid != null) {
                    liveMessageHelper?.receiveClientMessage?.invoke(uid, msg)
                } else {
                    liveLogCallback?.e("uid为空")
                    liveMessageHelper?.receiveClientMessage?.invoke("", msg)
                }
            }

            is Serializable -> {
                when (msg) {
                    is UserModel -> {
                        //用户信息会在连接的第一时刻收到
                        liveMessageHelper?.userMap?.put(
                            msg.userId,
                            ChannelState(channelId, ConnectState.CONNECTED, ctx)
                        )
                        connectStateChanged?.connectChanged(msg.userId, ConnectState.CONNECTED)
                        liveLogCallback?.v("连入用户：$msg")
                    }
                    LiveCode.HEART -> {
                        ctx.writeAndFlush(LiveCode.HEART)
                        //测试完放开
//                        liveLogCallback?.v(msg.toString())
                    }
                    else -> {
                        val uid = getUserId(channelId)
                        if (uid != null) {
                            liveMessageHelper?.receiveClientMessage?.invoke(uid, msg)
                        } else {
                            liveLogCallback?.e("uid为空")
                            liveMessageHelper?.receiveClientMessage?.invoke("", msg)
                        }
                    }
                }
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        super.channelReadComplete(ctx)
        ctx?.flush()
//        liveLogCallback?.v("LiveServer channelReadComplete: ")
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)//channel断开连接1
        val channelId = getChannelId(ctx)
        val userId = getUserId(channelId)
        if (userId != null) {
            connectStateChanged?.connectChanged(userId, ConnectState.DISCONNECT)
        }
        disconnectUser(channelId)
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        cause?.printStackTrace()
        liveLogCallback?.e(cause?.message ?: "")
        ctx?.close()
    }

    private fun disconnectUser(channelId: String) {
        val user = getUserId(channelId)
        if (user != null) {
            liveMessageHelper?.userMap?.forEach {
                it.value.connectState = ConnectState.DISCONNECT
            }
        }

    }

    private fun getUserId(channelId: String): String? {
        liveMessageHelper?.userMap?.forEach {
            if (it.value.channelId == channelId) {
                return it.key
            }
        }
        return null
    }

    private fun getChannelId(ctx: ChannelHandlerContext?): String {
        return ctx?.channel()?.id()?.asLongText() ?: ""
    }

//    override fun channelActive(ctx: ChannelHandlerContext?) {
//        super.channelActive(ctx)//channel连接2
//
//    }
//
//    override fun channelRegistered(ctx: ChannelHandlerContext?) {
//        super.channelRegistered(ctx)//channel注册1
//    }
//
//    override fun channelUnregistered(ctx: ChannelHandlerContext?) {
//        super.channelUnregistered(ctx)//某channel注销2
//    }
//
//    override fun channelWritabilityChanged(ctx: ChannelHandlerContext?) {
//        super.channelWritabilityChanged(ctx)
//    }


}