package com.lqp.wifidisplay;

public interface ChannelStateCallback {
	void onHighTransferQuality(int bw);
	void onLowTransferQuality(int bw);
	void onRttUpdate(int rtt);
	void onReconnecting();
	void onReconnectSuccess();
	void onServerBye();
}
