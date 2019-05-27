package com.jason.nettydemo.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.ScrollView
import com.jason.nettydemo.R
import com.jason.nettydemo.base.BaseActivity
import com.jason.nettydemo.constants.Constant
import com.jason.nettydemo.display.screenrecord.ScreenActivity
import com.jason.nettydemo.model.DataModel
import com.jason.nettydemo.utils.CoroutinesUtils
import com.jason.nettydemo.utils.toast
import com.wenba.ailearn.lib.live.constant.ConnectState
import com.wenba.ailearn.lib.live.helper.LiveServerHelper
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.nsd.NsdServer
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.activity_live.btn_logout
import kotlinx.android.synthetic.main.activity_live.btn_send
import kotlinx.android.synthetic.main.activity_live.sv
import kotlinx.android.synthetic.main.activity_live.tv_data_amount
import kotlinx.android.synthetic.main.activity_live.tv_msg
import kotlinx.android.synthetic.main.activity_live_server.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/**
 * @author Jason
 * @description:
 * @date :2019/4/28 2:54 PM
 */
class ServerActivity : BaseActivity(), View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private val server = LiveServerHelper(this)
    private var userList = CopyOnWriteArrayList<String>()
    private val nsd = NsdServer()
    private var job: Job? = null
    private var isSending = false

    private val dataFormat = SimpleDateFormat("HH:mm:ss SSS", Locale.getDefault())

    private var sendDelayTime = 30 * 1000L

    private var dataByte: ByteArray? = null

    private var costList = Collections.synchronizedList(mutableListOf<Int>())

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_server)

        startNsd()
        rg.setOnCheckedChangeListener(this)
        rg_time.setOnCheckedChangeListener(this)
        switch_send_server.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isSending = true
                job = CoroutinesUtils.launch {
                    while (isSending) {
                        sendMsg()
                        delay(sendDelayTime)
                    }
                }
            } else {
                isSending = false
                runBlocking {
                    job?.cancel()
                    job?.join()
                }
            }
        }
        server.setLiveLogCallback(mLiveLogCallback)
        server.getLiveMessageHelper().receiveClientMessage = { id, msg ->
            CoroutinesUtils.launchInUI {
                when (msg) {
                    is DataModel -> {
                        receiveAmount++
                        val diff = System.currentTimeMillis() - msg.sendTime
                        costList.add(diff.toInt())
                        logs.append("--->$id, length:${msg.data.length / 1024}K, index:${msg.index} cost:$diff, ${getCostDataString()} \n")
          git              printPackageAmount()
                    }
                    is String -> {
                        logs.append("--->$id，length:${msg.length / 1024}K \n")
                        val string = String(ByteArray(1024 * 20))
                        server.getLiveMessageHelper().sendMessageToClient(id, string)
                    }
                    is ByteArray -> {
                        logs.append("--->$id，length:${msg.size / 1024}K \n")
                        if (msg.size == 1024 * 1024) {
                            server.getLiveMessageHelper().sendMessageToClient(id, ByteArray(333))
                        }
                    }
                }
                tv_msg.text = logs.toString()
                clearLogs()
                sv.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        server.setConnectStateChanged(object : ConnectStateChanged {
            @SuppressLint("SetTextI18n")
            override fun connectChanged(userId: String, state: ConnectState) {
                when (state) {
                    ConnectState.CONNECTED -> {
                        Log.e("连接成功", "user：$userId")
                        if (userList.find { it == userId }.isNullOrBlank()) {
                            userList.add(userId)
                        }
                    }
                    ConnectState.DISCONNECT -> {
                        userList.remove(userId)
                        runOnUiThread {
                            tv_msg.text = logs.toString()
                        }
                    }
                }
                CoroutinesUtils.launchInUI {
                    tv_user.text = "连接数:${userList.size}"
                }

            }
        })
    }

    @Synchronized
    private fun getCostDataString(): String{
        costList.sort()
        val min = costList.first()
        val max = costList.last()
        var total = 0
        costList.forEach {
            total += it
        }
        val avg = total / costList.size
        return "min:$min , max:$max, avg:$avg"
    }

    override fun clearLogs() {
        super.clearLogs()
    }

    private fun getUserListString(): String {
        val users = StringBuffer()
        userList.forEach {
            users.append("$it,")
        }
        return users.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun printPackageAmount() {
        tv_data_amount.text =
            "发包数：$sendAmount，收包数：$receiveAmount"
    }

    private fun sendMsg() {
        CoroutinesUtils.launchInIO {
            if (dataByte == null) {
                dataByte = when (true) {
                    rb_100.isChecked -> {
                        ByteArray(1024 * 20)
                    }
                    rb_150.isChecked -> {
                        ByteArray(1024 * 50)
                    }
                    rb_500.isChecked -> {
                        ByteArray(1024 * 100)
                    }
                    rb_1m.isChecked -> {
                        ByteArray(1024 * 200)
                    }
                    rb_10m.isChecked -> {
                        ByteArray(1024 * 500)
                    }
                    rb_100m.isChecked -> {
                        ByteArray(1024 * 1025)
                    }
                    else -> ByteArray(0)
                }
            }
            val startTime = System.currentTimeMillis()
            userList.forEachIndexed { index, uId ->
                sendAmount++
                val data = DataModel(String(dataByte!!), System.currentTimeMillis(), index)
                server.getLiveMessageHelper().sendMessageToClient(uId, data)
            }
            val time = dataFormat.format(Date())
            logs.append("<---$time send$sendAmount, 耗时:${System.currentTimeMillis() - startTime}\n")
            CoroutinesUtils.launchInUI {
                tv_msg.text = logs.toString()
                printPackageAmount()
            }
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            btn_login -> {
                server.login(8080)
            }
            btn_logout -> {
                server.logout()
            }
            btn_send -> {
                sendMsg()
            }
            btn_nsd -> {
                startNsd()
            }
            btn_screen -> {
                startActivity(Intent(this, ScreenActivity::class.java))
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        group ?: return
        if (group.id == R.id.rg) {
            dataByte = null
            costList.clear()
        } else {
            when (checkedId) {
                R.id.rb_10s -> sendDelayTime = 10 * 1000L
                R.id.rb_20s -> sendDelayTime = 20 * 1000L
                R.id.rb_30s -> sendDelayTime = 30 * 1000L
                R.id.rb_40s -> sendDelayTime = 40 * 1000L
                R.id.rb_60s -> sendDelayTime = 60 * 1000L
            }
        }
    }

    private fun startNsd() {
        nsd.startNsdServer(this, Constant.SERVER_NAME, Constant.PORT, object : NsdServer.IRegisterState {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                toast("nsd服务注册成功")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                toast("nsd服务注册失败")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                toast("nsd服务注销成功")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                toast("nsd服务注销失败")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        server.logout()
        nsd.stopNsdServer()
    }


}