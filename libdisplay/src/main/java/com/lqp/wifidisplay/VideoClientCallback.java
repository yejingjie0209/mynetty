package com.lqp.wifidisplay;

/**
 * Created by lqp on 17-7-17.
 */

public interface VideoClientCallback {
    void onHighTransferQuality(int bw);
    void onLowTransferQuality(int bw);
    void onRttUpdate(int rtt);
    void onServerBye();
}
