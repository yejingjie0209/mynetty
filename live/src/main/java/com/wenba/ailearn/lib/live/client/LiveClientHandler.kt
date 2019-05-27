package com.wenba.ailearn.lib.live.client

import com.wenba.ailearn.lib.live.constant.ConnectState
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.model.LiveCode
import com.wenba.ailearn.lib.live.msg.LiveLogCallback
import com.wenba.ailearn.lib.live.msg.LiveMessageHelper
import com.wenba.ailearn.lib.live.utils.LiveLocalLog
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import java.io.Serializable


/**
 * @author Jason
 * @description:客户端handler
 * @date :2019/4/25 9:24 PM
 */
internal class LiveClientHandler(
    private val userId: String,
    private val client: LiveClient,
    private val liveLogCallback: LiveLogCallback?
    , private val liveMessageHelper: LiveMessageHelper?
    , private val liveStateChanged: ConnectStateChanged?
) : SimpleChannelInboundHandler<Any?>() {

    /**
     * 此方法会在连接到服务器后被调用
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        liveMessageHelper?.ctx = ctx
        liveLogCallback?.v("uid:$userId,连接服务器成功")
        liveStateChanged?.connectChanged(userId, ConnectState.CONNECTED)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        liveLogCallback?.v("uid:$userId,与服务器连接断开")
        liveStateChanged?.connectChanged(userId, ConnectState.DISCONNECT)
        //重连
        client.doConnect()

    }

    /**
     * 接收到服务器数据时调用
     */
    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any?) {
        when (msg) {
            is String -> liveMessageHelper?.receiveServerMessage?.invoke(msg)
            is Serializable -> {
                when (msg) {
                    LiveCode.HEART -> {
//                        liveLogCallback?.v("收到来自服务器心跳")
                        LiveLocalLog.v("uid:$userId,收到来自服务器心跳")
                    }
                    LiveCode.DISCONNECT -> {
                        client.stop()
                    }
                    else -> {
                        liveMessageHelper?.receiveServerMessage?.invoke(msg)
                    }
                }
            }
        }
    }

    /**
     * 捕捉到异常
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        liveLogCallback?.e(cause.message ?: "")
        cause.printStackTrace()
        ctx.close()
    }


    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        super.userEventTriggered(ctx, evt)
        if (evt is IdleStateEvent) {
            when {
                evt.state() == IdleState.READER_IDLE -> {
                    liveLogCallback?.e("uid:$userId,长期未收到服务器数据")
                    ctx.channel().close()
                }
                //可以选择重新连接
                evt.state() == IdleState.WRITER_IDLE -> {
//                    liveLogCallback?.v("发送心跳包")
                    LiveLocalLog.v("uid:$userId,发送心跳包")
                    ctx.writeAndFlush(LiveCode.HEART)
                }
                evt.state() == IdleState.ALL_IDLE -> liveLogCallback?.v("ALL")
            }
        }
    }
}