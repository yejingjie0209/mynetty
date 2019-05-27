package com.wenba.ailearn.lib.live.server

import android.content.Context
import com.wenba.ailearn.lib.live.BaseLive
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.model.LiveCode
import com.wenba.ailearn.lib.live.utils.isNetworkConnected
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit


/**
 * @author Jason
 * @description:服务端
 * @date :2019/4/25 6:39 PM
 */
class LiveServer(context: Context) : BaseLive(context) {
    var connectStateChanged: ConnectStateChanged? = null

    private var bootstrap: ServerBootstrap? = null
    private var bossGroup: NioEventLoopGroup? = null//专门处理端口的 Accept 事件
    private var workerGroup: NioEventLoopGroup? = null//处理io
    private var future: ChannelFuture? = null
    private var isShutdown = false

    /**
     * 服务启动
     */
    internal fun start(port: Int) {
        if (bossGroup != null || workerGroup != null) {
            stop()
        }

        bootstrap = ServerBootstrap()
        bossGroup = NioEventLoopGroup()
        workerGroup = NioEventLoopGroup()

        bootstrap?.group(bossGroup, workerGroup)
            // 指定所使用的nio传输channel
            ?.channel(NioServerSocketChannel::class.java)
//            ?.handler(LoggingHandler(LogLevel.INFO))
            // 指定本地监听的地址
            ?.localAddress(InetSocketAddress(port))
            // 添加一个handler
            ?.childHandler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(socketChannel: SocketChannel?) {
                    socketChannel?.pipeline()?.addLast(
                        ObjectEncoder(),
                        ObjectDecoder(
                            Integer.MAX_VALUE,
                            ClassResolvers.weakCachingConcurrentResolver(null)
                        ),
                        LiveServerHandler(liveLogCallback, liveMessageHelper, connectStateChanged)
                    )
                }
            })

        isShutdown = false
        doConnect()
    }

    internal fun stop() {
        isShutdown = true
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
    }

    /**
     * 断开某个连接
     */
    internal fun disconnect(userId: String? = null) {
        if (userId != null) {
            liveMessageHelper.sendMessageToClient(userId, LiveCode.DISCONNECT)
        } else {
            liveMessageHelper.userMap.forEach {
                liveMessageHelper.sendMessageToClient(it.key, LiveCode.DISCONNECT)
            }
        }
    }


    private fun doConnect() {
        try {
            liveLogCallback?.v("服务准备启动")
            future = bootstrap?.bind()

            future?.addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture?) {
                    if (future?.isSuccess == true) {
                        liveLogCallback?.v("服务启动成功")
                        if (!context.isNetworkConnected()) {
                            liveLogCallback?.e("网络未连接,请检查网络状况")
                        }
                    } else {
                        liveLogCallback?.e("服务启动失败")
                        reConnect(future)
                        future?.cause()?.printStackTrace()
                    }
                }
            })
            future?.channel()?.closeFuture()
        } catch (e: Exception) {
            e.printStackTrace()
            reConnect(future)
            liveLogCallback?.e("服务异常${e.message!!}")
        }
    }


    private fun reConnect(future: ChannelFuture?) {
        future?.channel()?.eventLoop()?.schedule({ doConnect() }, 10, TimeUnit.SECONDS)
    }
}