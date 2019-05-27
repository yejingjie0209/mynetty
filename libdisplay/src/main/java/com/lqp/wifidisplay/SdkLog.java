package com.lqp.wifidisplay;

/**
 * Created by lqp on 17-12-1.
 */

public class SdkLog {
    public static void e(String tag, String msg) {
        VideoLog.e(tag, msg);
    }

    public static void w(String tag, String msg) {
        VideoLog.d(tag, msg);
    }

    public static void d(String tag, String msg) {
        VideoLog.d(tag, msg);
    }
}
