package com.wenba.ailearn.lib.live.client

import android.content.Context
import com.wenba.ailearn.lib.live.BaseLive
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import io.netty.handler.timeout.IdleStateHandler
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit


/**
 * @author Jason
 * @description:客户端
 * @date :2019/4/25 8:50 PM
 */
class LiveClient(context: Context) : BaseLive(context) {
    companion object {
        const val READER_TIME = 20L
        const val WRITER_TIME = 10L
        const val ALL_TIME = 20L
        const val RECONNECT_TIME = 5L//重新尝试连接的间隔时间（单位：秒）
    }

    private var bootstrap: Bootstrap? = null
    private var group: NioEventLoopGroup? = null//处理线程池,一个channel对应一个
    private var future: ChannelFuture? = null
    private var isShutdown = false
    private var isConnecting: Boolean = false//是否正在连接中
    private var userId : String? = null

    /**
     * 客户端启动
     */
    internal fun start(userId: String, port: Int, host: String, liveStateChanged: ConnectStateChanged) {
        this.userId = userId

        if (group != null) {
            stop()
        }

        bootstrap = Bootstrap()
        group = NioEventLoopGroup()
        bootstrap?.group(group)?.channel(NioSocketChannel::class.java)
            ?.remoteAddress(InetSocketAddress(host, port))
            ?.handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel?) {
                    if (ch?.pipeline() == null) {
                        return
                    }
                    ch.pipeline().addLast(
                        IdleStateHandler(READER_TIME, WRITER_TIME, ALL_TIME, TimeUnit.SECONDS),
                        ObjectEncoder(),
                        ObjectDecoder(
                            Integer.MAX_VALUE,
                            ClassResolvers.weakCachingConcurrentResolver(null)
                        ),
                        LiveClientHandler(userId, this@LiveClient, liveLogCallback, liveMessageHelper, liveStateChanged)
                    )
                }
            })

        isShutdown = false
        doConnect()
    }

    /**
     * 客户端关闭
     */
    internal fun stop() {
        isShutdown = true
        group?.shutdownGracefully()
    }

    internal fun doConnect() {
        if (isShutdown || group?.isShuttingDown == true || group?.isShutdown == true || isConnecting) {
            return
        }
        try {
            liveLogCallback?.v("uid:$userId,客户端连接开始")
            isConnecting = true
            future = bootstrap?.connect()
            future?.addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture?) {
                    isConnecting = false
                    if (future?.isSuccess == false) {
                        liveLogCallback?.e("uid:$userId,连接服务器失败")
                        reConnect(future)
                        future.cause()?.printStackTrace()
                    }
                }
            })
        } catch (e: Exception) {
            isConnecting = false
            e.printStackTrace()
            reConnect(future)
            liveLogCallback?.e(e.message!!)
        }
    }


    private fun reConnect(future: ChannelFuture?) {
        future?.channel()?.eventLoop()?.schedule({ doConnect() }, RECONNECT_TIME, TimeUnit.SECONDS)
    }

}