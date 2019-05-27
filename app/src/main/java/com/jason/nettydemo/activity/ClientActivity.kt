package com.jason.nettydemo.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ScrollView
import com.jason.nettydemo.R
import com.jason.nettydemo.base.BaseActivity
import com.jason.nettydemo.constants.Constant.SERVER_NAME
import com.jason.nettydemo.model.DataModel
import com.jason.nettydemo.model.TestModel
import com.jason.nettydemo.utils.CoroutinesUtils
import com.jason.nettydemo.utils.FileUtil
import com.jason.nettydemo.utils.toast
import com.wenba.ailearn.lib.live.helper.LiveClientHelper
import com.wenba.ailearn.lib.live.constant.ConnectState
import com.wenba.ailearn.lib.live.i.ConnectStateChanged
import com.wenba.ailearn.lib.live.model.UserModel
import com.wenba.ailearn.lib.live.nsd.NsdClient
import com.wenba.ailearn.lib.live.utils.LiveLocalLog
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/**
 * @author Jason
 * @description:
 * @date :2019/4/28 2:54 PM
 */
class ClientActivity : BaseActivity(), View.OnClickListener {
    companion object {
        const val SEND_DELAY_MILL = 20 * 1000L
    }

    private val clientList = CopyOnWriteArrayList<LiveClientHelper>()
    private var nsdStartTime = 0L
    private var port = 0
    private var host: String? = null
    private var nsdClient: NsdClient? = NsdClient()
    private var job: Job? = null
    private var startRequestTime = 0L
    private var startSendTime = 0L
    private var isSending = false
    private var autoSendCount = 0
    private val dataFormat = SimpleDateFormat("HH:mm:ss SSS", Locale.getDefault())
    private var costList = Collections.synchronizedList(mutableListOf<Int>())

    private var loginedList = CopyOnWriteArrayList<String>()//已登录客户端账号

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        btn_clear.setOnClickListener(this)
        ll_client.visibility = View.VISIBLE
        startNsd()
        switch_send.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                sendMsg()
            }else{
                autoSendCount = 0
                cancelSend()
            }
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            btn_login_random -> {
                randomLogin()
                hideLogin()
            }
            btn_logout -> {
                logout()
            }
            btn_send -> {
                sendMsgByUser()
            }
            btn_nsd -> {
                startNsd()
            }
        }
    }

    private fun hideLogin() {
        if (host == null || port == 0) {
            toast("nsd未探测到服务端地址，无法登录，请确保服务端已开启nsd注册!")
            return
        }
    }

    private fun randomLogin() {
        val id = (0..999999999).random().toString()
        login(id)
    }

    private fun sendMsg() {
        isSending = true
        job = CoroutinesUtils.launch {
            while (isSending){
                val rand = Random.Default.nextInt(1, 10) * 1000
                delay(SEND_DELAY_MILL + rand)
                clientList.forEach {
                    val max = if(autoSendCount % 3 == 0){
                        150
                    }else{
                        5
                    }
                   val msg = String(ByteArray(max * 1024))
                   sendTextMsg(it,msg)
                    autoSendCount ++
                }
           }
       }
    }

    private fun sendMsgByUser() {
        sendDataMsg(ByteArray(1024 * 1024))
    }

    private fun cancelSend(){
        isSending = false
        runBlocking {
            job?.cancel()
            job?.join()
        }
    }
    private fun sendTextMsg(it: LiveClientHelper,string: String) {
        val time = dataFormat.format(Date())
        logs.append("$time send ${string.length/1024}K data to server\n")
        startRequestTime = System.currentTimeMillis()
        runBlocking {
            it.getLiveMessageHelper().sendMessageToServer(string)
        }
        CoroutinesUtils.launchInUI {
            tv_msg.text = logs.toString()
        }
    }

    private fun sendDataModel(dataModel: DataModel) {
        val time = dataFormat.format(Date())
        logs.append("$time send ${dataModel.data.length/1024}K to server \n")
        CoroutinesUtils.launchInUI {
            tv_msg.text = logs.toString()
        }
        runBlocking {
            startSendTime = System.currentTimeMillis()
            clientList.forEach {
                it.getLiveMessageHelper().sendMessageToServer(dataModel)
            }
        }
    }

    private fun sendDataMsg(dataByte : ByteArray) {
        val time = dataFormat.format(Date())
        logs.append("$time send ${dataByte.size/1024}K to server \n")
        CoroutinesUtils.launchInUI {
            tv_msg.text = logs.toString()
        }
        runBlocking {
            startSendTime = System.currentTimeMillis()
            clientList.forEach {
                it.getLiveMessageHelper().sendMessageToServer(dataByte)
            }
        }
    }

    private var lastTime = 0L

    @SuppressLint("SetTextI18n")
    private fun login(userId: String): Boolean {
        if (host == null || port == 0) {
            return false
        }

        if (clientList.size >= 27) {
            toast("客户端不能超过线程最大数27")
            return false
        }
        val client = LiveClientHelper(this)
        clientList.add(client)

        client.setLiveLogCallback(mLiveLogCallback)
        client.getLiveMessageHelper().receiveServerMessage = { msg ->
            CoroutinesUtils.launchInUI {
                when (msg) {
                    is DataModel -> {
                        val currentTime = System.currentTimeMillis()
                        val diff = currentTime - msg.sendTime
                        logs.append("收到响应${msg.data.length/1024}k，时长:$diff 时差:${currentTime - lastTime}\n")
                        lastTime = currentTime
                        receiveAmount++
                        tv_data_amount.text = "接收到数据包个数：$receiveAmount"
                        //收到后回复一个给服务端
                        if(switch_test.isChecked){
                            sendDataModel(msg)
                        }
                    }
                    is String -> {
                        val diff = System.currentTimeMillis() - startRequestTime
                        costList.add(diff.toInt())
                        logs.append("收到${msg.length/1024}k，时长:$diff, ${getCostDataString()}\n")
                    }
                    is ByteArray -> {
                        if(msg.size == 333){
                            val diff = System.currentTimeMillis() - startSendTime
                            logs.append("手动发送数据到服务端成功，耗时：$diff\n")
                        }
                    }
                }
                tv_msg.text = logs.toString()
                clearLogs()
                sv.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
        client.login(port, host!!, userId, "123", 1
            , object : ConnectStateChanged {
                override fun connectChanged(userId: String, state: ConnectState) {
                    when (state) {
                        ConnectState.CONNECTED -> {
                            loginedList.add(userId)
                            updateLoginedUser()
                        }
                        ConnectState.DISCONNECT -> {
                            loginedList.remove(userId)
                            updateLoginedUser()
                            CoroutinesUtils.launchInUI {
                                tv_state.text = "断开次数：${++stateDisconnectAmount}"
                            }
                        }
                        else -> Unit
                    }
                }
            })
        return true
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


    private fun updateLoginedUser() {
        val str = StringBuffer()
        str.append("连接数：${loginedList.size}, id:")
        loginedList.forEach {
            str.append("$it,")
        }
        CoroutinesUtils.launchInUI {
            tv_logined.text = str.toString()
        }

    }

    private fun logout() {
        Thread {
            clientList.forEach {
                it.logout()
            }
            clientList.clear()
        }.start()
    }


    private fun startNsd() {
        tv_nas_msg.text = "NSD探索中..."
        nsdStartTime = System.currentTimeMillis()
        nsdClient?.startNsdClient(this, SERVER_NAME, object : NsdClient.IDiscoverState {
            @SuppressLint("SetTextI18n")
            override fun onDiscoverFail(errorCode: Int, msg: String) {
                CoroutinesUtils.launchInUI {
                    tv_nas_msg.text = "nsd探索失败,errorCode = $errorCode,msg=$msg"
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onDiscoverSuccess(host: InetAddress, port: Int) {
                val endTime = System.currentTimeMillis() - nsdStartTime

                LiveLocalLog.v("host=$host,port=$port")
                this@ClientActivity.host = host.hostAddress
                this@ClientActivity.port = port

                CoroutinesUtils.launchInUI {
                    tv_ip.text = "host=$host,port=$port"
                    tv_nas_msg.text = "nsd探索成功 耗时$endTime,"
                }
                if (clientList.size == 0) {
                    randomLogin()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        logout()
        nsdClient?.stopNsdServer()
    }


}