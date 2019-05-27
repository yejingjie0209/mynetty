package com.jason.nettydemo.display.screenrecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.jason.nettydemo.R;
import com.lqp.wifidisplay.*;
import com.lqp.wifidisplay.DisplaySynchronizer.*;
import com.lqp.wifidisplay.RemoteDisplayManager.*;
import com.jason.nettydemo.display.sevice.KeepAliveService;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ScreenActivity extends Activity {
    private static final String TAG = "ScreenActivity";

    private static final int PERMISSION_CODE = 1;
    private static final int SCAN_CODE = 2;

    private static final int MSG_TIME_OUT = 1;
    private MediaProjectionManager mProjectionManager;

    private List<RemoteDisplay> deviceList = new ArrayList<RemoteDisplay>();
    private BaseAdapter listAdapter;
    private RemoteDisplayManager displayManager;
    private DisplaySession displaySession;
    private DisplaySynchronizer displaySynchronizer;

    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_screen);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Intent intent = new Intent(this, KeepAliveService.class);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //android8.0以上通过startForegroundService启动service
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        HandlerThread thread = new HandlerThread("wifi-display");
        thread.start();

        handler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_TIME_OUT:
                        if (deviceList.size() == 0) {
/*
						final RemoteDisplay d = (RemoteDisplay) msg.obj;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								confirmConnection(d, RemoteDisplayManager.CONN_MODE_TCP, true);
							}
						});

						break;
*/
                            showToast("find device failed, check network");
                            break;
                        }
                }
            }
        };

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        {
            displayManager = RemoteDisplayManager.getInstance(getApplicationContext());

            displayManager.setDiviceListener(new DisplayCallback() {

                @Override
                public void onFindRemoteDisplay(final RemoteDisplay display) {
                    Log.d("lqp", "find display: " + display.desc + ", from: " + display.tcpAddress);
                    onFindDevice(display);
                }

                @Override
                public void onFindError(final int code) {
                    showErrorCode(code);
                }

                @Override
                public void onConnectSuccess(DisplaySession session) {
                    displaySession = session;
                    prepareRecording();
                }

                @Override
                public void onConnectError(final int code) {
                    setStatusText("connect error");
                    showErrorCode(code);
                }

                void showErrorCode(int code) {
                    if (code == ERR_SOCKET_ERROR) {
                        showToast("socket error");
                    } else if (code == ERR_WIFI_NOT_AVALIABLE) {
                        showToast("wifi not avaliable");
                    } else if (code == ERR_ALREADY_IN_CONNECT) {
                        showToast("already in connection");
                        stopScreenRecording();
                    } else if (code == ERR_IP_ADDRESS_ERROR) {
                        showToast("ERR_IP_ADDRESS_ERROR");
                    }

                }
            });
        }

        Button mToggleButton = (Button) findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScreenRecording();
            }
        });

        Button button = (Button) findViewById(R.id.search_device);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                deviceList.clear();
                listAdapter.notifyDataSetChanged();
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        String desc = ((TextView) findViewById(R.id.ip_address)).getText().toString();
                        probeDevice(desc);
                    }
                });
            }
        });

        listAdapter = new BaseAdapter() {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder = null;
                if (convertView == null) {
                    holder = new ViewHolder();

                    convertView = View.inflate(getApplicationContext(), R.layout.list_item, null);

                    holder.descView = (TextView) convertView.findViewById(R.id.device_desc);

                    convertView.setTag(holder);

                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                RemoteDisplay display = deviceList.get(position);

                holder.descView.setText(display.toString());

                return convertView;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public int getCount() {
                return deviceList.size();
            }

            class ViewHolder {
                TextView descView;
            }
        };

        ListView listView = (ListView) findViewById(R.id.device_list);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                RadioGroup group = (RadioGroup) findViewById(R.id.mode);
                int selectId = group.getCheckedRadioButtonId();
                int mode = RemoteDisplayManager.CONN_MODE_TCP;
                if (selectId == R.id.udp_mode) {
                    mode = RemoteDisplayManager.CONN_MODE_UDP;
                }

                RemoteDisplay display = deviceList.get(position);
                confirmConnection(display, mode, false);
            }
        });

        findViewById(R.id.scan).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScreenActivity.this, ScanCodeActivity.class);
                startActivityForResult(intent, SCAN_CODE);
            }
        });
    }


    private void confirmConnection(final RemoteDisplay display, final int mode, boolean force) {
        Builder builder = new AlertDialog.Builder(this);

        String head = "";
        if (force) {
            head = "devide not responed, still ";
        }

        builder.setMessage(String.format(head + "connect to : [%s], mode:%s?",
                display.desc, mode == RemoteDisplayManager.CONN_MODE_TCP ? "TCP" : "UDP"));
        builder.setPositiveButton("Y", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setStatusText("connecting");

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        stopScreenRecording();
                        displayManager.connectDisplay(display, mode);
                    }
                });

                dialog.dismiss();
            }
        });

        builder.setNegativeButton("N", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void onFindDevice(RemoteDisplay display) {
        deviceList.add(display);
        handler.removeMessages(MSG_TIME_OUT);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        stopScreenRecording();
//        handler.getLooper().quit();
    }

    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void probeDevice(String connDesc) {
        String[] item = connDesc.split(":");
        final String ipAddr = item[0];
        int port = RemoteDisplayManager.DEFAULT_PORT;

        try {
            if (item.length > 1) {
                port = Integer.valueOf(item[1]);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }


        final int probePort = port;
        handler.post(new Runnable() {

            @Override
            public void run() {
                displayManager.probeRemoteDisplay(ipAddr, probePort);
            }
        });

        handler.removeMessages(MSG_TIME_OUT);

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(ipAddr), port);

            RemoteDisplay d = new RemoteDisplay(address, port, "unknow", true);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_TIME_OUT, d), 3000);
        } catch (Exception e) {

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this,
                        "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }

            MediaProjection projection = mProjectionManager.getMediaProjection(resultCode, data);

            startRecording(projection);
        } else if (requestCode == SCAN_CODE) {
            if (resultCode != RESULT_OK) {
                //some warning?
            } else {
                deviceList.clear();
                listAdapter.notifyDataSetChanged();
                String desc = data.getStringExtra(ScanCodeActivity.SCAN_RESULT);
                String[] descs = desc.split(";");

                int ver = -1;
                try {
                    ver = Integer.valueOf(descs[descs.length - 1]);
                } catch (Exception e) {

                }

                if (ver == -1 || ver != RemoteDisplayManager.getProtocolVersion()) {
                    showToast("版本不匹配, 请升级客户端和PC端到最新版本");
                    return;
                }

                for (int i = 0; i < descs.length - 1; i++) {
                    probeDevice(descs[i]);
                }
            }
        } else {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }

    }

    public void prepareRecording() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
    }

    private void setStatusText(String string) {
        TextView textView = (TextView) findViewById(R.id.status);
        textView.setText(string);
    }

    private void setRttText(String rtt) {
        TextView textView = (TextView) findViewById(R.id.rtt);
        textView.setText(rtt);
    }

    private void stopScreenRecording() {
        if (displaySynchronizer != null) {
            displaySynchronizer.stopScreenRecording();
            displaySynchronizer = null;
        }
    }

    private void startRecording(MediaProjection projection) {
        int resolv = RemoteDisplayManager.RESOLV_HD;

        RadioGroup group = (RadioGroup) findViewById(R.id.resolution);
        int id = group.getCheckedRadioButtonId();
        if (id == R.id.utral_hd) {
            resolv = RemoteDisplayManager.RESOLV_UHD;
        } else if (id == R.id.hd) {
            resolv = RemoteDisplayManager.RESOLV_HD;
        } else if (id == R.id.sd) {
            resolv = RemoteDisplayManager.RESOLV_SD;
        }

        displaySynchronizer = new DisplaySynchronizer(this.getApplicationContext(), projection, displaySession, resolv, new SynchronizerStateListener() {
            int rttCount = 0;

            @Override
            public void onRecordStop() {
                setStatusText("record stoped");
                setRttText("--");

                displaySynchronizer = null;
            }

            @Override
            public void onServerBye() {
                showToast("server say goodbye");
            }

            @Override
            public void onRttUpdate(int rtt) {
                if (++rttCount > 2) {
                    setRttText(rtt + "ms");
                    rttCount = 0;

                    //showToast("rtt update: " + rtt);
                }
            }

            @Override
            public void onRecordStart() {
                setStatusText("recording");
            }

            @Override
            public void onReconnecting() {
                //display reconnecting
                setStatusText("reconnecting");
                showToast("reconnecting...");
            }

            @Override
            public void onReconnectSuccess() {
                setStatusText("recon-recording");
                showToast("reconnecting success!");
            }

            @Override
            public DisplayMetrics onConfigMetrics(DisplayMetrics metrics) {
                //可以在这里进行适配metrics分辨率崩溃的场景, 注意不能任意放大分辨率,
                // 要选取接近的, 绝大部分情况直接返回即可
                return metrics;
            }
        });

        try {
            displaySynchronizer.setLockResolv(true);
            displaySynchronizer.startRecording();
        } catch (Exception e) {
            showToast("channel start recording failed: " + e);
        }
    }
}
