package com.jason.nettydemo.display.sevice;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import com.jason.nettydemo.display.screenrecord.ScanCodeActivity;

import java.lang.reflect.Method;

public class KeepAliveService extends Service {
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock = null;
    WifiManager.WifiLock wifiLock = null;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        PowerManager _powerManagement = (PowerManager) getSystemService(Context.POWER_SERVICE);

        wakeLock = _powerManagement.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "WakeLockForWifiDisplay");

        wakeLock.acquire();

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiLock = wifiManager.createWifiLock("WiFiLockForWifiDisplay");
            wifiLock.acquire();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (wakeLock != null) {
            wakeLock.release();
        }

        if (wifiLock != null) {
            wifiLock.release();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String CHANNEL_ONE_ID = "com.primedu.cn";
        String CHANNEL_ONE_NAME = "Channel One";
        NotificationChannel notificationChannel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }


        Intent invokeIntent = new Intent(this, ScanCodeActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,
                invokeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this).setChannelId(CHANNEL_ONE_ID)
                    .setTicker("Nature")
                    .setSmallIcon(android.R.drawable.arrow_up_float)
                    .setContentTitle("xxxx")
                    .setContentText("go go go, WenbaRtc~")
                    .setContentIntent(pendingIntent)
                    .getNotification();
            notification.flags |= Notification.FLAG_NO_CLEAR;
        } else {
            notification = new Notification.Builder(this)
                    .setTicker("Nature")
                    .setSmallIcon(android.R.drawable.arrow_up_float)
                    .setContentTitle("xxxx")
                    .setContentText("go go go, WenbaRtc~")
                    .setContentIntent(pendingIntent)
                    .getNotification();
            notification.flags |= Notification.FLAG_NO_CLEAR;
//            notification = new Notification(android.R.drawable.arrow_up_float,
//                    "go go go, WenbaRtc~",
//                    System.currentTimeMillis());
//            		notification.setLatestEventInfo(this,
//									    "WifiDisplay",
//									    "WifiDisplay in progress",
//									    pendingIntent);
//
//
//            Class clazz = mNotification.getClass();
//            try {
//                Method m2 = clazz.getDeclaredMethod("setLatestEventInfo", Context.class,CharSequence.class,CharSequence.class,PendingIntent.class);
//                m2.invoke(mNotification, mContext, mContentTitle,
//                        contentText, mContentIntent);
//            } catch (Exception e) {
//                // TODO: handle exception
//                e.printStackTrace();
//            }


        }


//
//		String msg = null;
//		if (intent != null) {
//			msg = intent.getStringExtra("strData");
//		}
//
//		notification.setLatestEventInfo(this,
//									    "WifiDisplay",
//									    "WifiDisplay in progress",
//									    pendingIntent);


        startForeground(1337, notification);

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

}
