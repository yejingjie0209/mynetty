package com.lqp.wifidisplay;

import android.os.*;
import android.widget.*;

import java.util.concurrent.atomic.*;

public abstract class DisplaySession {
	private final static String TAG = "DisplaySession";

	public final static int FRAME_FLAG_KEY_FRAME = 1;
	public final static int TIME_OUT_SEC = 20;
	
	protected RemoteDisplayManager manager;
	protected RemoteDisplay display;
	protected long heartbeatTs;
	private AtomicInteger timeout = new AtomicInteger(0);
	protected ChannelStateCallback stateCb;
	protected volatile boolean isAlive = true;
	private boolean isReconnecting = false;
	protected Handler uiHandler = new Handler(Looper.getMainLooper());
	
	public DisplaySession(RemoteDisplayManager m, RemoteDisplay d) {
		manager = m;
		display = d;

		m.setVideoClientCallback(new VideoClientCallback() {
			@Override
			public void onRttUpdate(int rtt) {
				onPeerResponse();

				ChannelStateCallback cb = stateCb;
				if (cb != null) {
					cb.onRttUpdate(rtt);
				}
			}

			@Override
			public void onServerBye() {
				ChannelStateCallback cb = stateCb;
				if (cb != null) {
					cb.onServerBye();
				}
			}

			@Override
			public void onLowTransferQuality(int bw) {
				//VideoLog.d(TAG, "bad quality: " + bw / 1024);

				ChannelStateCallback cb = stateCb;
				if (cb != null) {
					cb.onLowTransferQuality(bw);
				}
			}

			@Override
			public void onHighTransferQuality(int bw) {
				//VideoLog.d(TAG, "good quality: " + bw / 1024);

				ChannelStateCallback cb = stateCb;
				if (cb != null) {
					cb.onHighTransferQuality(bw);
				}
			}
		});
	}

	private void showToast(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(manager.getContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public boolean isAlive() {
		return isAlive;
	}
	
	public void setChannelStateCb(ChannelStateCallback cb) {
		this.stateCb = cb;
	}
	
	public void closeSession() {
		manager.closeDisplaySession();
	}

    public void onPeerResponse() {
		timeout.set(0);

		if (stateCb != null) {
			if (isReconnecting) {
				stateCb.onReconnectSuccess();
				isReconnecting = false;
			}
		}
		//VideoLog.e("lqp", "peer responsed");
	}
	
	public void handleTick() throws Exception {

		try {
			if (System.currentTimeMillis() - heartbeatTs > 1000) {
				heartbeatTs = System.currentTimeMillis();
				
				if (timeout.get() >= TIME_OUT_SEC) {
					throw new Exception("heartbeat timeout");
				}
				
				if (timeout.incrementAndGet() > 3) {
					if (stateCb != null) {
						if (isReconnecting == false) {
							VideoLog.d(TAG, "onReconnecting: timeout: " + timeout);
							stateCb.onReconnecting();
							isReconnecting = true;
						}
					}
				}

				onHeartbeat();
			}
			
			onHandleTick();
		} catch (Exception e) {
			throw e;
		}
	}
	
	abstract public void finishSession() throws Exception;
	abstract protected void onHandleTick() throws Exception;
	abstract protected void onHeartbeat() throws Exception;
	abstract public int sendFrame(int flag, byte[] data, int offset, int count) throws Exception;
}
