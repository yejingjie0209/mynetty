package com.lqp.wifidisplay.opengl;

import android.annotation.*;
import android.graphics.*;
import android.opengl.*;
import android.opengl.Matrix;
import android.os.*;
import android.view.*;

import com.lqp.wifidisplay.*;
import com.lqp.wifidisplay.opengl.gles.*;

import java.util.concurrent.atomic.*;

import static android.opengl.GLES20.*;

/**
 * Created by lqp on 17-11-29.
 */

@SuppressLint("NewApi")
public class SurfaceCodecRender implements Runnable {
    public static class RenderEglParam {
        public EGLContext sharedContext;
        public Surface surface;
        public int width;
        public int height;
        public int degree;
        public int fps;
        public int textureId;
    }

    private static final String LOG_TAG = "husky-render";

    public static final int CMD_INIT_EGL_CONTEXT    = 1;
    public static final int CMD_UPDATE_EGL_CONTEXT  = 2;
    public static final int CMD_FRAME_AVAILABLE     = 3;
    public static final int CMD_STOP_RENDER         = 4;
    public static final int CMD_SET_TEXTURE         = 5;
    public static final int CMD_CHECK_INSERT_FRAME  = 6;
    public static final int CMD_DESTORY_RENDER      = 7;

    private TextureShow textureShow;

    private volatile Handler workHandler;
    private AtomicBoolean isAlive = new AtomicBoolean(false);
    private Surface surface;
    private int     textureId;
    private EglCore mEglCore;
    private WindowSurface mInputWindowSurface;
    private Object opFence = new Object();
    private float[]  renderMatrix = new float[16];
    private volatile EGLContext sharedContext;
    private int         viewWidth;
    private int         viewHeight;
    private int         fps;
    private Thread thread;

    private void initEglContext(RenderEglParam param) {
        this.surface = param.surface;
        this.sharedContext = param.sharedContext;

        mEglCore = new EglCore(param.sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, surface, true);
        mInputWindowSurface.makeCurrent();
        textureId = param.textureId;

        int w;
        int h;
        if ((param.degree / 90) % 2 != 0) {
            w = param.height;
            h = param.width;
        }else {
            w = param.width;
            h = param.height;
        }

        this.fps = param.fps;
        viewWidth = w;
        viewHeight = h;
        GLES20.glClearColor(0, 0, 0, 1);
        glViewport(0, 0, viewWidth, viewHeight);

        final float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(renderMatrix, 0);

        Matrix.orthoM(renderMatrix, 0, -1, 1, -1, 1, 3f, 7f);
        //Matrix.perspectiveM(projectionMatrix, 0, 90, 1, 3f, 7f);
        //Matrix.frustumM(projectionMatrix, 0, -1, 1, -1, 1, 3f, 7f);

        {
            final float[] trans = new float[16];
            final float[] temp = new float[16];
            final float[] m1 = new float[16];
            final float[] m2 = new float[16];

            Matrix.setIdentityM(m1, 0);
            Matrix.rotateM(m1, 0, 180, 0, 0f, 1f);

            Matrix.setIdentityM(m2, 0);
            Matrix.rotateM(m2, 0, -180f, 0f, 1f, 0f);

            Matrix.multiplyMM(temp, 0, m2, 0, m1, 0);
            System.arraycopy(temp, 0, m1, 0, temp.length);

            Matrix.setIdentityM(trans, 0);
            Matrix.translateM(trans, 0, 0f, 0f, -3f);

            Matrix.multiplyMM(modelMatrix, 0, trans, 0, m1, 0);
        }

        final float[] temp = new float[16];
        Matrix.setIdentityM(temp, 0);
        Matrix.multiplyMM(temp, 0, renderMatrix, 0, modelMatrix, 0);
        System.arraycopy(temp, 0, renderMatrix, 0, temp.length);

        textureShow = new TextureShow();
    }

    public EGLContext getEglContext() {
        return sharedContext;
    }

    public void initEglSurface(RenderEglParam param) {
        synchronized (opFence) {
            if (isAlive.get()) {
                workHandler.sendMessage(workHandler.obtainMessage(CMD_INIT_EGL_CONTEXT, param));
            }
        }
    }

    public void updateEglContext(EGLContext sharedContext) {
        synchronized (opFence) {
            if (thread != null) {
                workHandler.sendMessage(workHandler.obtainMessage(CMD_UPDATE_EGL_CONTEXT, sharedContext));
            }
        }
    }

    long	lastFpsCalcTs = 0;
    long    acceptTs = 0;
    int 	accepted = 0;
    private int frames;

    public boolean acceptFrame(long ts) {
        frames++;

        long current = ts;
        int progress = (int)(current - lastFpsCalcTs) * 100 / 1000;
        int needNow = (progress * this.fps / 100) + 1;

        boolean accept = false;
        if (accepted < needNow) {
            accepted++;
            acceptTs = current;
            accept = true;
        }else {
            //ZoneLog.e("lqp", "drop frame: " + frames);
        }

        if (lastFpsCalcTs == 0) {
            lastFpsCalcTs = ts;
        }else {
            if (lastFpsCalcTs + 1000 < current) {
                long diff = current - lastFpsCalcTs;

                long fps = frames * 1000 / diff;
                //SdkLog.d(LOG_TAG, "opengl prview fps: " + fps + ", accept fps: " + (accepted * 1000 / diff));

                lastFpsCalcTs = current;
                frames = 0;
                accepted = 0;
            }
        }

        return accept;
    }

    private void initHandler() {
        workHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CMD_INIT_EGL_CONTEXT: {
                        RenderEglParam param = (RenderEglParam) msg.obj;
                        initEglContext(param);
                        break;
                    }

                    case CMD_UPDATE_EGL_CONTEXT:
                        updateSharedContext((EGLContext) msg.obj);
                        break;

                    case CMD_CHECK_INSERT_FRAME: {
                        long now = System.currentTimeMillis();
                        if (acceptTs + 100 < now) {
                            drawFrame(now);
                            //SdkLog.d(LOG_TAG, "insert frame!!");
                            workHandler.sendEmptyMessageDelayed(CMD_CHECK_INSERT_FRAME, 100);
                        }
                        break;
                    }

                    case CMD_FRAME_AVAILABLE: {
                        SurfaceTexture texture = (SurfaceTexture) msg.obj;
                        texture.updateTexImage();
                        long timestamp = System.currentTimeMillis();

                        if (mInputWindowSurface != null && acceptFrame(timestamp)) {
                            drawFrame(timestamp);
                        }

                        workHandler.removeMessages(CMD_CHECK_INSERT_FRAME);
                        workHandler.sendEmptyMessageDelayed(CMD_CHECK_INSERT_FRAME, 100);
                        break;
                    }

                    case CMD_STOP_RENDER: {
                        releaseEncoder();
                        break;
                    }

                    case CMD_DESTORY_RENDER: {
                        Looper.myLooper().quit();
                        break;
                    }

                    case CMD_SET_TEXTURE: {
                        textureId = msg.arg1;
                        break;
                    }
                }
            }
        };
    }

    public void frameAvailable(SurfaceTexture st) {
        synchronized (opFence) {
            if (!isAlive.get() || surface == null) {
                return;
            }
        }

        float[] transform = new float[16];      // TODO - avoid alloc every frame
        st.getTransformMatrix(transform);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            SdkLog.e(LOG_TAG, "HEY: got SurfaceTexture with timestamp of zero");
        }

        Handler h = workHandler;
        if (h == null) {
            return;
        }

        h.sendMessage(h.obtainMessage(CMD_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, st));
    }

    private int drawCount;
    private void drawFrame(long ts) {
        drawCount++;
        //glClearColor(1.0f, ((counter++) % 255) / 255f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //Matrix.setIdentityM(renderMatrix, 0);
        textureShow.onDrawFrame(renderMatrix, textureId);

        int xpos = (drawCount * 2) % (viewWidth - 50);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, 5, 1);
        GLES20.glClearColor(0f, 0.5f, 0.5f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        mInputWindowSurface.setPresentationTime(ts * 1000 * 1000);
        mInputWindowSurface.swapBuffers();
    }

    public void startRender() {
        SdkLog.d(LOG_TAG, "startRender start");

        synchronized (opFence) {
            if (isAlive.get()) {
                return;
            }

            thread = new Thread(this);
            thread.start();

            while(isAlive.get() == false) {
                try {
                    opFence.wait(1000);
                    if (isAlive.get() == false) {
                        SdkLog.d(LOG_TAG, "render not start in 1000 milis");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateSharedContext(EGLContext newSharedContext) {
        SdkLog.d(LOG_TAG, "handleUpdatedSharedContext " + newSharedContext);

        this.sharedContext = newSharedContext;

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        textureShow = new TextureShow();

        GLES20.glClearColor(0, 0, 0, 1);
        glViewport(0, 0, viewWidth, viewHeight);
    }

    private void releaseEncoder() {
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    public boolean isRunning() {
        return isAlive.get();
    }

    public void setTextureId(int tex) {
        synchronized (opFence) {
            if (isAlive.get()) {
                workHandler.sendMessage(workHandler.obtainMessage(CMD_SET_TEXTURE, tex, 0));
            }

        }
    }

    public void stopRender() {
        SdkLog.d(LOG_TAG, "stopRender called");
        synchronized (opFence) {
            if (isAlive.get()) {
                workHandler.sendEmptyMessage(CMD_STOP_RENDER);
            }
        }
    }

    public void destroy() {
        synchronized (opFence) {
            if (isAlive.get()) {
                workHandler.removeCallbacksAndMessages(null);
                workHandler.sendEmptyMessage(CMD_DESTORY_RENDER);

                while(thread.isAlive()) {
                    try {
                        opFence.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();

        initHandler();

        isAlive.set(true);

        synchronized (opFence) {
            opFence.notifyAll();
        }

        Looper.loop();

        workHandler = null;
        isAlive.set(false);
        synchronized (opFence) {
            opFence.notifyAll();
        }
    }
}