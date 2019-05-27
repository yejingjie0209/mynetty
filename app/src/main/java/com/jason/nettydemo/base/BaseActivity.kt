package com.jason.nettydemo.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.ScrollView
import com.jason.nettydemo.R
import com.jason.nettydemo.utils.CoroutinesUtils
import com.wenba.ailearn.lib.live.msg.LiveLogCallback
import com.wenba.ailearn.lib.live.utils.LiveLocalLog
import kotlinx.android.synthetic.main.activity_live.*

/**
 * @author Jason
 * @description:
 * @date :2019/4/28 2:55 PM
 */
abstract class BaseActivity : AppCompatActivity(), View.OnClickListener {
    var logs = StringBuffer()
    var receiveAmount = 0//接收数
    var sendAmount = 0//发送数
    var stateDisconnectAmount = 0//断开数

    @SuppressLint("SetTextI18n")
    val mLiveLogCallback = object : LiveLogCallback {
        override fun v(msg: String, code: Int?) {
            CoroutinesUtils.launchInUI {
                //logs.append("logv-------------->:$msg\n")
                //tv_msg.text = logs.toString()
                //sv.fullScroll(ScrollView.FOCUS_DOWN)
            }
            clearLogs()
        }

        override fun e(msg: String, code: Int?) {
            CoroutinesUtils.launchInUI {
                //logs.append("loge--------------->:$msg\n")
                //tv_msg.text = logs.toString()
                //sv.fullScroll(ScrollView.FOCUS_DOWN)
            }
            clearLogs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        when (v) {
            btn_clear -> {
                logs.setLength(0)
                tv_msg.text = logs.toString()
                receiveAmount = 0
                sendAmount = 0
                stateDisconnectAmount = 0
                tv_state?.text = "断开次数：$stateDisconnectAmount"
                tv_data_amount?.text = "接收到数据包个数：$receiveAmount"
            }
        }
    }

    open fun clearLogs() {
        if (logs.length > 2000) {
            logs.delete(0, logs.length / 2)
        }
        LiveLocalLog.d("logsLength = ${logs.length}")
    }
}