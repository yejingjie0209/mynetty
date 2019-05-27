package com.lqp.wifidisplay;

import android.os.*;

class VideoClient {
	static {
		System.loadLibrary("display");
	}
	
	static private final String TAG = "VideoClient";
	
	static private final int java_cb_low_quilty_chl = 1;
	static private final int java_cb_high_quilty_chl = 2;
	static private final int java_cb_rtt			  = 3;
	static private final int java_cb_service_query_ack = 4;
	static private final int java_cb_conn_udp_ack = 5;
	static private final int java_cb_sdk_log      = 6;
	static private final int java_cb_server_bye	  =  7;
	
	static interface JniVideoClientCallback {
		void onHighTransferQuality(int bw);
		void onLowTransferQuality(int bw);
		void onRttUpdate(int rtt);
		void onServiceQueryAck(String body);
		void onConnectUdpResult(int code);
		void onServerBye();
	}
	
	private volatile long handle;
	private String desc;
	private Handler uiHandler = new Handler(Looper.getMainLooper());
	private JniVideoClientCallback callback;
	private boolean connected = false;
	
	public VideoClient(String desc, JniVideoClientCallback cb){
		this.desc = desc;
		this.callback = cb;
		
		handle = video_client_create(desc);
		if (handle == 0) {
			throw new RuntimeException("create jni video client failed");
		}
	}
	
	public void destroy() {
		checkHandle();
		
		video_client_destory(handle);
		connected = false;
		handle = 0;
	}
	
	private void checkHandle() {
		if (handle == 0) {
			throw new IllegalStateException("handle == 0");
		}
	}
	
	public void bindPeer(int ip, int port) {
		checkHandle();
		video_client_bind_peer(handle, ip, port);
	}
	
	public int probeServer() {
		checkHandle();
		return video_client_proble(handle);
	}
	
	public int clientBye() {
		checkHandle();
		
		connected = false;
		return video_client_bye(handle);
	}
	
	public int connectServer() {
		checkHandle();
		
		return video_client_connect(handle);
	}
	
	public int sendVideo(byte[] data, int offset, int len, int key) {
		checkHandle();
		
		if (!connected) {
			throw new RuntimeException("not connected");
		}
		
		return video_client_send_video(handle, data, offset, len, key);
	}
	
	private void handleEvent(int event, int val, String str) {
		switch (event) {
		case java_cb_low_quilty_chl:
			callback.onLowTransferQuality(val);
			break;
			
		case java_cb_high_quilty_chl:
			callback.onHighTransferQuality(val);
			break;
			
		case java_cb_rtt:
			callback.onRttUpdate(val);
			break;
			
		case java_cb_service_query_ack:
			callback.onServiceQueryAck(str);
			break;
			
		case java_cb_conn_udp_ack:
			if (val == 0) {
				connected = true;
			}else {
				connected = false;
			}
			callback.onConnectUdpResult(val);
			break;

		case java_cb_sdk_log:
			VideoLog.d("sdk-jni", str);
			break;

		case java_cb_server_bye: {
			callback.onServerBye();
			break;
		}

		default:
			VideoLog.e(TAG, "unknown event: " + event);
			break;
		}
	}
	
	public void jni_callback(final int event, final int val, final String str) {
		uiHandler.post(new Runnable() {
			
			@Override
			public void run() {
				handleEvent(event, val, str);
			}
		});
	}

	public static int getProtocolVersion() {
		return video_client_get_proto_version();
	}
	
	native private long video_client_create(String desc);
	native private void video_client_destory(long h);
	native void video_client_bind_peer(long h, int ip, int port);
	native int video_client_proble(long h);
	native int video_client_bye(long h);
	native int video_client_connect(long h);
	native int video_client_send_video(long h, byte[] data, int offset, int len, int key);
	native static int video_client_get_proto_version();
}
