package com.lqp.wifidisplay;

import android.util.*;

public class VideoLog {
	public static interface LogCallback {
		public void e(String tag, String msg);
		public void d(String tag, String msg);
	}
	
	private static LogCallback mCallback;
	public static boolean PRINT_LOG = false;
	
	public static void setCallback(LogCallback mCallback) {
		VideoLog.mCallback = mCallback;
	}
	
	public static void e(String tag, String msg) {
		if (mCallback != null) {
			mCallback.e(tag, msg);
		}
		
		Log.e(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		if (mCallback != null) {
			mCallback.e(tag, msg);
		}
		
		Log.w(tag, msg);
	}
	
	public static void d(String tag, String msg) {
		if (mCallback != null) {
			mCallback.d(tag, msg);
		}
		
		if (PRINT_LOG) {
			Log.d(tag, msg);
		}
	}
}
