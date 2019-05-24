# live

live为局域网长连接nio通信库，底层基于netty4库

## 简单集成
目前为测试阶段只通过module方式引入，后续传入maven库


## 包含的功能
* 服务端登录注销
* 客户端登录注销
* 服务端nsd注册
* 客户端nsd探测

## 使用
* 服务端登录注销使用：LiveServerHelper类
* 客户端登录注销使用：LiveClientHelper类
* 消息通信帮助类使用: LiveMessageHelper类
* 服务端nsd注册使用：NsdServer类
* 客户端nsd探测使用：NsdClient类

### 服务端登录注销使用：LiveServerHelper类

```kotlin
    /**
     * 服务端登录
     * @param port 端口
     */
    fun login(port: Int) 

    /**
     * 服务端注销
     */
    fun logout()


    /**
     * 设置日志监听回调
     * @param liveLogCallback 日志回调
     */
    fun setLiveLogCallback(liveLogCallback: LiveLogCallback)
    /**
     * 消息通信帮助类
     * @return 消息通信帮助类，该类为用户客户端、服务端之前相互发送消息的重要类
     */
    fun getLiveMessageHelper(): LiveMessageHelper 

    /**
     * 连接状态改变
     */
    fun setConnectStateChanged(connectStateChanged: ConnectStateChanged) 

```

### 客户端登录注销使用：LiveClientHelper类

```kotlin
class LiveClientHelper(context: Context) {
 /**
     * 客户端登录
     * @param port 端口号
     * @param host ip地址
     * @param userId 用户id，唯一
     * @param password 密码（非必填）
     * @param userType 用户类型(非必填)
     * @param liveStateChanged 连接状态改变回调
     */
    fun login(
        port: Int,
        host: String,
        userId: String,
        password: String,
        userType: Int,
        liveStateChanged: ConnectStateChanged
    ) 

    /**
     * 客户端注销
     */
    fun logout()

    /**
     * 设置日志监听回调
     * @param liveLogCallback 日志回调
     */
    fun setLiveLogCallback(liveLogCallback: LiveLogCallback) 

    /**
     * 消息通信帮助类
     * @return 消息通信帮助类，该类为用户客户端、服务端之前相互发送消息的重要类
     */
    fun getLiveMessageHelper(): LiveMessageHelper
}
```

### 消息通信帮助类使用: LiveMessageHelper类
```kotlin
/**
 * @author Jason
 * @description: 消息通信帮助类，该类为用户客户端、服务端之前相互发送消息的重要类
 * @date :2019/4/28 10:17 AM
 */
class LiveMessageHelper {
    /**
     * 接收客户端消息回调
     * @param userId 用户Id，唯一
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    var receiveClientMessage: ((userId: String, msg: Any) -> Unit)? = null

    /**
     * 接收服务端消息回调
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    var receiveServerMessage: ((msg: Any) -> Unit)? = null

    /**
     * 发送消息给服务端
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    fun sendMessageToServer(msg: Any)
    /**
     * 发送消息给客户端
     * @param userId 客户端用户id，唯一
     * @param msg 消息，可为任意类型，但对象需实现序列化
     */
    fun sendMessageToClient(userId: String, msg: Any)
}
```


### 服务端nsd注册使用：NsdServer类
```kotlin
class NsdServer {
    /**
     *  并开始NSD服务端注册
     *  @param serviceName 服务名
     *  @param port 端口号
     *  @param registerState 状态回调
     */
    fun startNsdServer(context: Context, serviceName: String, port: Int, registerState: IRegisterState?)

    /**
     * 取消注册NSD服务器端
     */
    fun stopNsdServer()
}
```
### 客户端nsd探测使用：NsdClient类

```kotlin
class NsdClient {
    /**
     * 客户端启动nsd探测
     * @param mServiceName 服务名与服务端保持一致
     * @param mIDiscoverState 探索状态回调
     */
    fun startNsdClient(
        mContext: Context,
        mServiceName: String,
        mIDiscoverState: IDiscoverState?
    )


    /**
     * 客户端停止服务探测
     */
    fun stopNsdServer() 
}
```# mynetty
