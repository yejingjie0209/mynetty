package com.wenba.ailearn.lib.live.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.wenba.ailearn.lib.live.nsd.NsdConstant.NSD_SERVER_TYPE
import com.wenba.ailearn.lib.live.utils.LiveLocalLog


/**
 * nsd服务端
 */
class NsdServer {
    private var mNsdManager: NsdManager? = null
    private var mRegistrationListener: NsdManager.RegistrationListener? = null//注册监听器对象
    private var mServerName: String? = null

    //NSD服务接口对象
    private var registerState: IRegisterState? = null

    /**
     *  并开始NSD服务端注册
     *  @param serviceName 服务名
     *  @param port 端口号
     *  @param registerState 状态回调
     */
    fun startNsdServer(context: Context, serviceName: String, port: Int, registerState: IRegisterState?) {
        this.registerState = registerState
        initializeRegistrationListener()
        registerService(context, serviceName, port)
    }


    /**
     * 取消注册NSD服务器端
     */
    fun stopNsdServer() {
        mNsdManager?.unregisterService(mRegistrationListener)
    }


    //实例化注册监听器
    private fun initializeRegistrationListener() {
        mRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                LiveLocalLog.e("NsdServiceInfo onRegistrationFailed")
                registerState?.onRegistrationFailed(serviceInfo, errorCode)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                LiveLocalLog.e("onUnregistrationFailed serviceInfo: $serviceInfo ,errorCode:$errorCode")
                registerState?.onUnregistrationFailed(serviceInfo, errorCode)
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                mServerName = serviceInfo.serviceName
                LiveLocalLog.v("onServiceRegistered: $serviceInfo")
                LiveLocalLog.v("mServerName onServiceRegistered: $mServerName")
                registerState?.onServiceRegistered(serviceInfo)
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                LiveLocalLog.e("onServiceUnregistered serviceInfo: $serviceInfo")
                registerState?.onServiceUnregistered(serviceInfo)
            }
        }
    }

    //注册NSD服务器端
    private fun registerService(context: Context, serviceName: String, port: Int) {
        mNsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = serviceName
        serviceInfo.port = port
        serviceInfo.serviceType = NSD_SERVER_TYPE
        mNsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener)
    }


    //NSD服务注册监听接口
    interface IRegisterState {
        fun onServiceRegistered(serviceInfo: NsdServiceInfo)      //注册NSD成功

        fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int)    //注册NSD失败

        fun onServiceUnregistered(serviceInfo: NsdServiceInfo)   //取消NSD注册成功

        fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int)  //取消NSD注册失败

    }

}

