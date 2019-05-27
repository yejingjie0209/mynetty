package com.wenba.ailearn.lib.live

import android.content.Context
import com.wenba.ailearn.lib.live.msg.LiveLogCallback
import com.wenba.ailearn.lib.live.msg.LiveMessageHelper

/**
 * @author Jason
 * @description:服务端与客户端基类
 * @date :2019/4/26 7:57 PM
 */
abstract class BaseLive(var context: Context) {
    var liveLogCallback: LiveLogCallback? = null
    var liveMessageHelper = LiveMessageHelper()

}