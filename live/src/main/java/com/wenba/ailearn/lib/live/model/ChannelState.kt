package com.wenba.ailearn.lib.live.model

import com.wenba.ailearn.lib.live.constant.ConnectState
import io.netty.channel.ChannelHandlerContext

/**
 * @author Jason
 * @description: 频道连接状态
 * @date :2019/5/5 6:38 PM
 */
class ChannelState(var channelId: String, var connectState: ConnectState,var ctx: ChannelHandlerContext)