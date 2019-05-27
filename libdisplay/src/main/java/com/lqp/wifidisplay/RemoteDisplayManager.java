package com.lqp.wifidisplay;

import android.content.*;
import android.net.*;
import android.net.NetworkInfo.*;
import android.os.*;

import com.lqp.wifidisplay.msg.*;

import org.json.*;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class RemoteDisplayManager {
	public static final String TAG = "RemoteDisplayManager";

	public static final int CONN_MODE_TCP = 1;
	public static final int CONN_MODE_UDP = 2;
	public static final int DEFAULT_PORT = 12167;
	public static final int RESOLV_SD = 1;
	public static final int RESOLV_HD = 2;
	public static final int RESOLV_UHD = 3;

	private static RemoteDisplayManager sInstance = null;
	public static synchronized RemoteDisplayManager getInstance(Context context) {
		if (sInstance != null) {
			return sInstance;
		}
		
		sInstance = new RemoteDisplayManager(context);
		return sInstance;
	}
	
	//instance fields
	private String myDesc = "android-client";
	private DisplayCallback deviceListener;
	private Context mContext;
	private VideoClient videoClient;
	private volatile VideoClientCallback clientCb;
	private volatile DisplaySession mCurrentSession;
	private int connMode;
	private Handler uiHandler = new Handler(Looper.getMainLooper());
	private RemoteDisplay connDisplay;
	private volatile boolean isUdpConnecting;

	public boolean isWifiAvaliable() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo ninfo = cm.getActiveNetworkInfo();
		return ninfo != null && ninfo.isAvailable() && ninfo.getType() == ConnectivityManager.TYPE_WIFI
				&& ninfo.getDetailedState() == DetailedState.CONNECTED;
	}
	
	public void setVideoClientCallback(VideoClientCallback cb) {
		this.clientCb = cb;
	}
	
	public int sendUdpFrame(byte[] data, int offset, int count, int key) {
		checkVideoClient();
		return videoClient.sendVideo(data, offset, count, key);
	}

	public Context getContext() {
		return mContext;
	}
	
	public void udpChannelBye() {
		checkVideoClient();
		videoClient.clientBye();
	}
	
	private class VideoCallback implements VideoClient.JniVideoClientCallback {
		private long rttCounter = 0;

		@Override
		public void onHighTransferQuality(int bw) {
			VideoLog.d(TAG, "onHighTransferQuality: " + bw);
			VideoClientCallback cb = clientCb;
			if (cb != null) {
				cb.onHighTransferQuality(bw);
			}
		}

		@Override
		public void onLowTransferQuality(int bw) {
			VideoLog.d(TAG, "onLowTransferQuality: " + bw);
			VideoClientCallback cb = clientCb;
			if (cb != null) {
				cb.onLowTransferQuality(bw);
			}
		}

		@Override
		public void onRttUpdate(int rtt) {
			if ((rttCounter++ % 6) == 0) {
				VideoLog.d(TAG, "onRttUpdate: " + rtt);
			}

			VideoClientCallback cb = clientCb;
			if (cb != null) {
				cb.onRttUpdate(rtt);
			}
		}

		@Override
		public void onServiceQueryAck(String body) {
			VideoLog.d(TAG, "onServiceQueryAck: " + body);

			try {
				JSONObject obj = new JSONObject(body);
				int status = obj.getInt("status");
				int tcpPort = obj.getInt("tcp_port");
				int udp_port = obj.getInt("udp_port");
				String ip = obj.getString("ip");
				String desc = obj.getString("desc");
				
				InetSocketAddress addr = new InetSocketAddress(ip, tcpPort);
				RemoteDisplay display = new RemoteDisplay(addr, udp_port, desc, status == MsgBase.DISPLAY_IDLE);
				
				DisplayCallback cb = deviceListener;
				if (cb != null) {
					cb.onFindRemoteDisplay(display);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onConnectUdpResult(int code) {
			VideoLog.d(TAG, "onConnectUdpResult: code: " + code);
			if (code == 0) {
				if (mCurrentSession == null) {
					if (isUdpConnecting) {
						isUdpConnecting = false;
						DisplaySession session = new DisplaySessionUdp(RemoteDisplayManager.this, connDisplay, 0);
						deviceListener.onConnectSuccess(session);
						mCurrentSession = session;
					}else {
						VideoLog.d(TAG, "udp is not connecting, user my close session, so ignore it");
					}
				}else {
					VideoLog.d(TAG, "seems a repeat ack, ignore it");
				}
			}else {
				isUdpConnecting = false;
				connDisplay = null;
			}
		}

		@Override
		public void onServerBye() {
			VideoLog.d(TAG, "onServerBye");
			VideoClientCallback cb = clientCb;
			if (cb != null) {
				cb.onServerBye();
			}
		}
	}
	
	private RemoteDisplayManager(Context context) {
		VideoLog.PRINT_LOG = true;

		mContext = context;
		videoClient = new VideoClient(myDesc, new VideoCallback());
	}
	
	public void setDiviceListener(DisplayCallback l) {
		deviceListener = l;
	}
	
	private void connectTcp(RemoteDisplay display, int mode) {
		try {
			SocketChannel socketChannel = SocketChannel.open();
			socketChannel.socket().connect(display.tcpAddress, 3000);

			VideoLog.d("lqp", "connect success");
			
			ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[512]);
			byteBuffer.putShort((short)MsgBase.DISPLAY_MSG_PACKET0);
			byte[] desc = myDesc.getBytes();
			
			byteBuffer.putInt(desc.length);
			byteBuffer.put(desc);
			byteBuffer.flip();
			
			while(byteBuffer.hasRemaining()) {
				socketChannel.write(byteBuffer);
			}
			
			final DisplaySessionTcp session = new DisplaySessionTcp(this, display, socketChannel);
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onConnectSuccess(session);
				}
			});
			
			mCurrentSession = session;
			
			connMode = mode;
		} catch (Exception e) {
			e.printStackTrace();
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onConnectError(DisplayCallback.ERR_SOCKET_ERROR);
				}
			});
		}
	}

	private void connectUdp(RemoteDisplay display, int mode) {
		connDisplay = display;
		connMode = mode;
		isUdpConnecting = true;

		InetSocketAddress address = (InetSocketAddress) display.tcpAddress;
		int ip = inetAddressToInt(address.getAddress());
		int port = display.udp_port;

		videoClient.bindPeer(ip, port);
		videoClient.connectServer();
	}
	
	public synchronized void connectDisplay(RemoteDisplay display, int mode) {
		if (mCurrentSession != null) {
			closeDisplaySession();
		}
		
		if (isWifiAvaliable() == false) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onConnectError(DisplayCallback.ERR_WIFI_NOT_AVALIABLE);
				}
			});

			return;
		}
		
		if (mode == CONN_MODE_TCP) {
			connectTcp(display, mode);
		}else if (mode == CONN_MODE_UDP) {
			connectUdp(display, mode);
		}else {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onConnectError(DisplayCallback.ERR_CONN_MODE_ERROR);
				}
			});
		}
	}
	
	synchronized void closeDisplaySession() {
		isUdpConnecting = false;
		if (mCurrentSession != null) {
			try {
				mCurrentSession.finishSession();
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				mCurrentSession = null;
				setVideoClientCallback(null);
			}
		}
	}
	
	public static boolean validateIp(String ip) {
	    String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	    return ip.matches(PATTERN);
	}
	
	private static int inetAddressToInt(InetAddress addr) {
		byte[] val = addr.getAddress();
		
		int ip = 0;
		ip |= (val[0] & 0xff) << 24;
		ip |= (val[1] & 0xff) << 16;
		ip |= (val[2] & 0xff) << 8;
		ip |= (val[3] & 0xff);
		return ip;
	}

	public void probeRemoteDisplay(String ip, int port) {
		checkVideoClient();
		if (isWifiAvaliable() == false) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onFindError(DisplayCallback.ERR_WIFI_NOT_AVALIABLE);
				}
			});
			return;
		}
		
		if (!validateIp(ip)) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onFindError(DisplayCallback.ERR_IP_ADDRESS_ERROR);
				}
			});
			return;
		}
		
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					deviceListener.onFindError(DisplayCallback.ERR_IP_ADDRESS_ERROR);
				}
			});
			return;
		}

		int intIp = inetAddressToInt(addr);
		videoClient.bindPeer(intIp, port);
		videoClient.probeServer();
	}
	
	private void checkVideoClient() {
		if (videoClient == null) {
			throw new RuntimeException("videoClient == null");
		}
	}
	
	public void setMyDescription(String desc) {
		if (desc.getBytes().length > MsgBase.MAX_DESC_LEN) {
			throw new IllegalArgumentException("desc is too long, max 255 bytes");
		}
		
		if (!desc.equals(myDesc)) {
			videoClient.destroy();
			videoClient = new VideoClient("android-dev-lqp", new VideoCallback());
		}
		
		this.myDesc = desc;
	}

	public static int getProtocolVersion() {
		return VideoClient.getProtocolVersion();
	}
	
	public static interface DisplayCallback {
		public static final int ERR_WIFI_NOT_AVALIABLE = 1;
		public static final int ERR_SOCKET_ERROR = 2;
		
		public static final int ERR_ALREADY_IN_CONNECT = 3;
		public static final int ERR_IP_ADDRESS_ERROR = 4;
		public static final int ERR_CONN_MODE_ERROR = 5;
		
		public void onFindRemoteDisplay(RemoteDisplay display);
		public void onFindError(int code);
		
		public void onConnectSuccess(DisplaySession session);
		public void onConnectError(int code);
	}
}
