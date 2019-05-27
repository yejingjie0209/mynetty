package com.jason.nettydemo.display.screenrecord;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import com.google.zxing.Result;
import com.jason.nettydemo.R;
import com.jason.nettydemo.display.qrcode.CameraPreviewDecoder;
import com.jason.nettydemo.display.qrcode.YuvUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanCodeActivity extends Activity {
    public final static String SCAN_RESULT = "SCAN_RESULT";

    private Camera camera;
    private CameraPreviewDecoder decoder = new CameraPreviewDecoder();

    private Thread workThread;
    private AtomicBoolean isAlive = new AtomicBoolean(true);
    private BlockingQueue<byte[]> workQueue = new ArrayBlockingQueue<byte[]>(10);

    private int dataWidth;
    private int dataHeight;
    private Runnable workTask = new Runnable() {

        @Override
        public void run() {
            Rect rect = new Rect();
            rect.left = 0;
            rect.top = 0;

            boolean saved = false;
            while (isAlive.get()) {
                byte[] image = null;
                try {
                    image = workQueue.take();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (image == null) {
                    continue;
                }

                int rectLen = Math.min(dataWidth, dataHeight);
                rectLen = (int) (rectLen * 0.6f);

                int marginH = (dataHeight - rectLen) / 2;
                int marginW = (dataWidth - rectLen) / 2;

                rect.left = marginH;
                rect.top = marginW;

                rect.right = rect.left + rectLen;
                rect.bottom = rect.top + rectLen;

                //这是旋转成垂直方向后的, 宽高需要对换
                final Result result = decoder.decode(image, dataHeight, dataWidth, rect);
                if (result != null) {
                    Log.e("lqp", "find code: " + result.getText());

                    //save image
                    if (!saved) {
                        saved = true;

                        OutputStream stream = null;
                        try {
                            stream = new FileOutputStream("/sdcard/scan_code.jpg");
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        YuvImage yuvImage = new YuvImage(image, ImageFormat.NV21, dataHeight, dataWidth, null);
                        yuvImage.compressToJpeg(rect, 100, stream);
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.putExtra(SCAN_RESULT, result.getText());
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });

                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scan_code);

        final SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
        surfaceView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (camera != null) {
                    camera.autoFocus(new AutoFocusCallback() {

                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            // TODO Auto-generated method stub

                        }
                    });
                }
            }
        });
        surfaceView.getHolder().addCallback(new Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // TODO Auto-generated method stub

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                initCamera(surfaceView, surfaceView.getHeight(), surfaceView.getWidth());
            }
        });

        workThread = new Thread(workTask);
        workThread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    AskForPermission();
                }else{
                    setCamera();
                }
            }
        }
    }

    private void AskForPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Permission!");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
        builder.create().show();
    }

    private SurfaceView surfaceView;
    private int width;
    private int height;

    private void initCamera(SurfaceView surfaceView, int width, int height) {
        this.surfaceView = surfaceView;
        this.width = width;
        this.height = height;

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }else{
                setCamera();
            }
        } else {
            setCamera();
        }

    }

    private void setCamera() {
        camera = Camera.open(CameraInfo.CAMERA_FACING_BACK);

        Camera.Parameters parameters = camera.getParameters();
        List<Size> prevList = parameters.getSupportedPreviewSizes();

        float bestRatio = (float) width / height;

        List<Size> candidates = new ArrayList<Size>();

        for (Size size : prevList) {
            float ratio = (float) size.width / size.height;
            if (Math.abs(bestRatio - ratio) < 0.8) {
                candidates.add(size);
            }

            Log.e("lqp", String.format("prevList: w = %d, h = %d", size.width, size.height));
        }

        Size prevSize = null;

        int wDiff = 100000;
        for (Size s : candidates) {
            if (Math.abs(s.width - width) < wDiff) {
                prevSize = s;
                wDiff = Math.abs(s.width - width);
            }
        }

        if (prevSize == null) {
            Log.e("lqp", "prevSize == null");
            return;
        }

        this.dataWidth = prevSize.width;
        this.dataHeight = prevSize.height;

        byte[] prevBuffer = new byte[prevSize.width * prevSize.height * 3 / 2];

        parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
        parameters.setPreviewSize(prevSize.width, prevSize.height);
        parameters.setPreviewFrameRate(12);
        parameters.setPreviewFormat(ImageFormat.NV21);

        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        camera.addCallbackBuffer(prevBuffer);
        camera.setPreviewCallbackWithBuffer(new PreviewCallback() {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //decode

                //Log.e("lqp", "onPreviewFrame: size: " + data.length);
                camera.addCallbackBuffer(data);

                byte[] copy = Arrays.copyOf(data, data.length);

                while (workQueue.size() > 12) {
                    workQueue.poll();
                }

                YuvUtil.rotateNV21_90(data, copy, ScanCodeActivity.this.dataWidth, ScanCodeActivity.this.dataHeight);

                workQueue.offer(copy);
            }
        });

        camera.startPreview();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        isAlive.set(false);

        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }

//        while (workThread.isAlive()) {
//            try {
//                workThread.join(1000);
//            } catch (InterruptedException e) {
//                Log.e("lqp", "decode worker thread not exit in 1000 ms");
//            }
//        }
    }
}
