package com.wenba.ailearn.lib.live.nsd


import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.wenba.ailearn.lib.live.nsd.NsdConstant.NSD_SERVER_TYPE
import com.wenba.ailearn.lib.live.utils.LiveLocalLog
import java.net.InetAddress

/**
 * nsd客户端
 */
class NsdClient {
    //NSD_SERVICE_NAME和NSD_SERVER_TYPE需要与服务器端完全一致
    private var mDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var mNsdManager: NsdManager? = null

    private var mContext: Context? = null
    private var mServiceName: String? = null
    private var mIDiscoverState: IDiscoverState? = null

    /**
     * 客户端启动nsd探测
     * @param mServiceName 服务名与服务端保持一致
     * @param mIDiscoverState 探索状态回调
     */
    fun startNsdClient(
        mContext: Context,
        mServiceName: String,
        mIDiscoverState: IDiscoverState?
    ) {
        this.mContext = mContext
        this.mServiceName = mServiceName
        this.mIDiscoverState = mIDiscoverState
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        initializeDiscoveryListener()
        mNsdManager?.discoverServices(NSD_SERVER_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)

    }


    /**
     * 客户端停止服务探测
     */
    fun stopNsdServer() {
        mNsdManager?.stopServiceDiscovery(mDiscoveryListener)
    }

    private fun initializeDiscoveryListener() {
        mDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                mIDiscoverState?.onDiscoverFail(errorCode, "onStartDiscoveryFailed")
                mNsdManager?.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                mIDiscoverState?.onDiscoverFail(errorCode, "onStopDiscoveryFailed")
                mNsdManager?.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != NSD_SERVER_TYPE) {
                } else if (serviceInfo.serviceName == mServiceName) {
                    mNsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            mIDiscoverState?.onDiscoverFail(errorCode, "onResolveFailed")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val port = serviceInfo.port
                            val host = serviceInfo.host
                            LiveLocalLog.e("onServiceResolved: host:$host, port:$port")
                            mIDiscoverState?.onDiscoverSuccess(host, port)
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                LiveLocalLog.e("onServiceLost: serviceInfo=$serviceInfo")
                mIDiscoverState?.onDiscoverFail(100, "onServiceLost")
            }
        }
    }


    interface IDiscoverState {
        fun onDiscoverFail(errorCode: Int, msg: String)
        fun onDiscoverSuccess(host: InetAddress, port: Int)
    }

}
