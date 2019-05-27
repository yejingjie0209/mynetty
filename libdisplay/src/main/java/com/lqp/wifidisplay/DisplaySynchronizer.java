package com.lqp.wifidisplay;

import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.MediaCodec.*;
import android.media.projection.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

//import com.lqp.wifidisplay.opengl.*;

import java.nio.*;
import java.util.concurrent.atomic.*;

/**
 * 2018-12-06 由于c5的平板兼容性问题，注释掉opengl的代码
 */
public class DisplaySynchronizer implements SurfaceTexture.OnFrameAvailableListener {
	private static final int 	LOWER_QUALITY_MS_THRESHOLD 		= 200;
	private static final int 	LOWER_QUALITY_CONTINUE_MS 		= 5000;
	private static final int 	CHANGE_QUALITY_MS_THRESHOLD 	= 15000;
	private static final String TAG = "DispSync";

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//		if (render != null && texture != null) {
//			render.frameAvailable(texture);
//		}else {
//			//texture.releaseTexImage();
//		}

		//SdkLog.d(TAG, "onFrameAvailable called");
	}

	public interface SynchronizerStateListener {
		/**
		 * rtt < 30 - good, rtt < 80 - normal, rtt > 80, bad
		 * @param rtt
		 */
		public void onRttUpdate(int rtt);
		public void onRecordStart();
		public void onReconnecting();
		public void onReconnectSuccess();
		public DisplayMetrics onConfigMetrics(DisplayMetrics metrics);
		public void onRecordStop();
		public void onServerBye();
	}

	private Handler uiHandler = new Handler(Looper.getMainLooper());
	private Context context;
	private MediaProjection mediaProjection;
	private VirtualDisplay virtualDisplay;

	// port start
	private static final String VIDEO_MIME_TYPE = "video/avc";

	private BufferInfo mVideoBufferInfo = new BufferInfo();;
	private MediaCodec mVideoEncoder;

	private Object mRecordLock = new Object();
	private byte[] videoBuffer = new byte[1024];
	private Surface mInputSurface;
	private Surface mTextureSurface;

	private DisplaySession displaySession;
	private SynchronizerStateListener listener;

	private Thread recordThread;
	private AtomicBoolean isAlive = new AtomicBoolean(true);
	private AtomicBoolean lockResolv = new AtomicBoolean(false);

	private int origReslov;
	private volatile int curResolv;
	private int fps;
	private int iFrameInterval = 3;
	private int lastConfWidth = 0;
	private int lastConfHeight = 0;
//	private SurfaceCodecRender render;
	private int 			textureId;
	private SurfaceTexture 	texture;
	private boolean 	doProjectionStop = true;
	private volatile long   goodTs = 0;

	private Runnable recordTask = new Runnable() {
		private long checkTime = 0;

		@Override
		public void run() {
			while (isAlive.get()) {
				drainEncoder();

				long ts = System.currentTimeMillis();
				if (ts - checkTime > 1000) {
					checkTime = ts;
					checkDisplayOrientation();
				}

				try {
					displaySession.handleTick();
				} catch (Exception e) {
					VideoLog.e(TAG, "video session disconnected: " + e);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							stopScreenRecording();
						}
					});
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			VideoLog.e(TAG, "Record thread finished normally");
		}
	};

	public void setDoProjectionStop(boolean needStop) {
		this.doProjectionStop = needStop;
	}

	public DisplaySynchronizer(Context context, MediaProjection projection, DisplaySession session,
							   int resolv, final SynchronizerStateListener listener) {
		this.context = context;
		this.displaySession = session;
		this.listener = listener;

		this.mediaProjection = projection;
		this.origReslov = resolv;
		this.curResolv = resolv;
		this.fps = 15;

		session.setChannelStateCb(new ChannelStateCallback() {
			
			@Override
			public void onRttUpdate(final int rtt) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						listener.onRttUpdate(rtt);
						//Log.e("lqp", "rtt update: " + rtt);
						if (rtt > LOWER_QUALITY_MS_THRESHOLD) {
							if (goodTs == 0) {
								goodTs = System.currentTimeMillis();
							}else {
								if (goodTs + LOWER_QUALITY_CONTINUE_MS < System.currentTimeMillis()) {
									lowerResolv();

									goodTs = System.currentTimeMillis() + CHANGE_QUALITY_MS_THRESHOLD;
								}
							}
						}else {
							goodTs = System.currentTimeMillis();
						}
					}
				});
			}

			@Override
			public void onReconnecting() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						listener.onReconnecting();
					}
				});
			}

			@Override
			public void onReconnectSuccess() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						listener.onReconnectSuccess();
					}
				});
			}

			@Override
			public void onServerBye() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						listener.onServerBye();
						stopScreenRecording();
					}
				});
			}

			@Override
			public void onLowTransferQuality(int bw) {
				//VideoLog.d(TAG, "onLowTransferQuality bt: " + bw);
				if (goodTs + 5000 < System.currentTimeMillis()) {
					lowerResolv();

					goodTs = System.currentTimeMillis() + CHANGE_QUALITY_MS_THRESHOLD;
				}
			}
			
			@Override
			public void onHighTransferQuality(int bw) {
				//VideoLog.d(TAG, "onHighTransferQuality bt: " + bw);
				//higherResolv();
			}
		});
	}

	private void checkDisplayOrientation() {
		DisplayMetrics m = getAdaptedMetrics();

		int width = m.widthPixels;
		int height = m.heightPixels;

		if (width == 0 || height == 0) {
			return;
		}

		if (width != lastConfWidth || height != lastConfHeight) {
			try {
				configureRecording(m, false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void startRecording() throws Exception {
		synchronized (mRecordLock) {
			if (!isAlive.get()) {
				throw new IllegalStateException("instance is dead");
			}

			if (mVideoEncoder != null) {
				throw new IllegalStateException("mVideoEncoder != null");
			}

			initVideoEncoder();

			if (mVideoEncoder != null) {
				recordThread = new Thread(recordTask);
				recordThread.start();

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						listener.onRecordStart();
					}
				});
			} else {
				isAlive.set(false);
				displaySession.closeSession();
				throw new RuntimeException("init video encoder failed");
			}
		}
	}

	public void stopScreenRecording() {
		if (Thread.currentThread() == recordThread) {
			throw new RuntimeException("call stopScreenRecording in record thread is forbidden");
		}

		try {
			synchronized (mRecordLock) {
				if (!isAlive.get()) {
					return;
				}

				isAlive.set(false);
				while (recordThread.isAlive()) {
					try {
						//give it chance to do rest work
						mRecordLock.wait(5);
						recordThread.join(1000);
						if (recordThread.isAlive()) {
							VideoLog.e(TAG, "stopScreenRecording: thread not finish in 1000 milis");
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				VideoLog.e(TAG, "display session stoped");

				releaseEncoder();
				displaySession.closeSession();
			}
		}finally{
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					listener.onRecordStop();
				}
			});
		}
	}

	private void runOnUiThread(Runnable runnable) {
		uiHandler.post(runnable);
	}

	private void showToast(String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

	private void lowerResolv() {
		if (lockResolv.get()) {
			return;
		}

		if (curResolv == RemoteDisplayManager.RESOLV_UHD) {
			curResolv = RemoteDisplayManager.RESOLV_HD;
			VideoLog.w(TAG, "lowerResolv to: high definition");
		}else if (curResolv == RemoteDisplayManager.RESOLV_HD) {
			curResolv = RemoteDisplayManager.RESOLV_SD;
			VideoLog.w(TAG, "lowerResolv to: standard definition");
		}else {
			//already lowest
		}
	}

	private void higherResolv() {
		if (curResolv == RemoteDisplayManager.RESOLV_SD) {
			curResolv = RemoteDisplayManager.RESOLV_HD;
			VideoLog.w(TAG, "higherResolv to: back to high definition");
		}
	}

	public int getCurReslovMode() {
		return curResolv;
	}

	public boolean setLockResolv(boolean lock) {
		boolean orig = lockResolv.get();

		lockResolv.set(lock);
		return orig;
	}

	private int getCurReslovPixes() {
		int res = 720;

		if (curResolv == RemoteDisplayManager.RESOLV_SD) {
			res = 480;
		}else if (curResolv == RemoteDisplayManager.RESOLV_HD) {
			res = 720;
		}else if (curResolv == RemoteDisplayManager.RESOLV_UHD) {
			res = 1080;
		}

		return res;
	}
	
	private DisplayMetrics getAdaptedMetrics() {
		DisplayMetrics m = new DisplayMetrics();
		Display display = getWindowManager().getDefaultDisplay();
		display.getMetrics(m);

		int resol = getCurReslovPixes();

		int pixel = Math.min(m.widthPixels, m.heightPixels);
		pixel = Math.min(pixel, resol);

		int w;
		int h;

		float ratio = (float)m.widthPixels / m.heightPixels;
		if (m.widthPixels > m.heightPixels) {
			h = pixel;
			w = (int)(h * ratio) / 4 * 4;
		}else {
			w = pixel;
			h = (int)(w / ratio) / 4 * 4;
		}

		m.widthPixels = w;
		m.heightPixels = h;
		return listener.onConfigMetrics(m);
	}

	private void initVideoEncoder() {
		try {
			configureRecording(getAdaptedMetrics(), true);
		} catch (Exception e) {
			e.printStackTrace();
			releaseEncoder();
		}
	}

	private WindowManager getWindowManager() {
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		return manager;
	}

	private byte[] metaBuffer = new byte[0];
	private void drainEncoder() {
		for (;;) {
			synchronized (mRecordLock) {
				if (mVideoEncoder == null || !displaySession.isAlive()) {
					break;
				}

				int bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);

				if (bufferIndex >= 0) {
					ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
					if (encodedData == null) {
						throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
					}

					if (mVideoBufferInfo.size != 0) {
						try {
							if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
								VideoLog.e(TAG, "write meta data");
								
								if (metaBuffer.length != mVideoBufferInfo.size) {
									metaBuffer = new byte[mVideoBufferInfo.size];
								}
								
								encodedData.position(mVideoBufferInfo.offset);
								encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
								encodedData.get(metaBuffer, 0, mVideoBufferInfo.size);
								continue;
							}
								
							int flag = 0;
							int len = mVideoBufferInfo.size;
							int metaLen = 0;
							if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
								flag = DisplaySessionTcp.FRAME_FLAG_KEY_FRAME;
								metaLen = metaBuffer.length;
							}

							encodedData.position(mVideoBufferInfo.offset);
							encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);

							if (len + metaLen + DisplaySessionTcp.FRAME_HEADER_LEN > videoBuffer.length) {
								videoBuffer = new byte[DisplaySessionTcp.FRAME_HEADER_LEN + metaLen + len];
							}
							
							if (metaLen != 0) {
								for (int i = 0; i < metaBuffer.length; i++) {
									videoBuffer[DisplaySessionTcp.FRAME_HEADER_LEN + i] = metaBuffer[i];
								}
							}

							encodedData.get(videoBuffer, DisplaySessionTcp.FRAME_HEADER_LEN + metaLen, len);

							//long ts = System.currentTimeMillis();
							int ret = displaySession.sendFrame(flag, videoBuffer, DisplaySessionTcp.FRAME_HEADER_LEN, metaLen + len);
							//Log.e("lqp", "send used: " + (System.currentTimeMillis() - ts));
							if (ret < 0) {
								throw new Exception("send return error: " + ret);
							}
						} catch (Exception e) {
							e.printStackTrace();

							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									stopScreenRecording();
								}
							});

							return;
						}
					}

					mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

					if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						break;
					}
				} else {
					break;
				}
			}
		}

		return;
	}

	private void releaseComponentLocked() {
//		if (render != null) {
//			render.destroy();
//			render = null;
//		}

		if (mVideoEncoder != null) {
			mVideoEncoder.stop();
			mVideoEncoder.release();
			mVideoEncoder = null;
		}

		if (virtualDisplay != null) {
			virtualDisplay.release();
			virtualDisplay = null;
		}

		if (mTextureSurface != null) {
			mTextureSurface.release();
			mTextureSurface = null;
		}

		if (mInputSurface != null) {
			mInputSurface.release();
			mInputSurface = null;
		}

		if (texture != null) {
			textureId = -1;
			texture.release();;
			texture = null;
		}
	}

	private void releaseEncoder() {
		VideoLog.d(TAG, "releaseEncoders");

		synchronized (mRecordLock) {
			if (mediaProjection != null) {
				if (doProjectionStop) {
					mediaProjection.stop();
					mediaProjection = null;
				}
			}

			releaseComponentLocked();
		}
	}

	private int getBitrate() {
		int KB;
		switch (curResolv) {
			case RemoteDisplayManager.RESOLV_SD:
				KB = 128;
				break;

			case RemoteDisplayManager.RESOLV_HD:
				KB = 256;
				break;

			case RemoteDisplayManager.RESOLV_UHD:
				KB = 512;
				break;

			default:
				KB = 256;
				break;
		}

		return KB * 1024 * 8;
	}

	private void configureRecording(DisplayMetrics metrics, boolean create) throws Exception {
		synchronized (mRecordLock) {
			if (!create && mVideoEncoder == null) {
				return;
			}

			releaseComponentLocked();

			int bitrate = getBitrate();

			mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);

			MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, metrics.widthPixels,
					metrics.heightPixels);

			format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

			format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
			format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
			format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
			format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);

			String brand = Build.BRAND;
			String model = Build.MODEL;

			if (Build.VERSION.SDK_INT >= 21) {
				MediaCodecInfo.CodecCapabilities cap = mVideoEncoder.getCodecInfo().getCapabilitiesForType(VIDEO_MIME_TYPE);
				MediaCodecInfo.EncoderCapabilities encap = cap.getEncoderCapabilities();

				format.setInteger(MediaFormat.KEY_COMPLEXITY, encap.getComplexityRange().clamp(Integer.valueOf(5)));
				if (encap.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)) {
					format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
				}
				else if (encap.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)) {
					format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
				}
				else {
					format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
				}
			}else {
				format.setInteger(MediaFormat.KEY_BITRATE_MODE, 0);
			}
			
			mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mInputSurface = mVideoEncoder.createInputSurface();
			mVideoEncoder.start();

			// Get the display size and density.
			lastConfWidth = metrics.widthPixels;
			lastConfHeight = metrics.heightPixels;

//			render = new SurfaceCodecRender();
//			render.startRender();
//
//			textureId = GlUtil.createTextureExtern();
//			texture = new SurfaceTexture(textureId);
//			texture.setOnFrameAvailableListener(this);
//			texture.setDefaultBufferSize(lastConfWidth, lastConfHeight);
//			MediaRecorder mediaRecorder = new MediaRecorder();
//			mTextureSurface = mediaRecorder.getSurface();
//			mTextureSurface = new Surface(texture);

//			SurfaceCodecRender.RenderEglParam param = new SurfaceCodecRender.RenderEglParam();
//			param.degree = 0;
//			param.height = lastConfHeight;
//			param.width = lastConfWidth;
//			param.fps = fps;
//			param.surface = mInputSurface;
//			param.textureId = textureId;
//
//			render.initEglSurface(param);

			// Start the video input.
			virtualDisplay = mediaProjection.createVirtualDisplay("Recording Display", lastConfWidth, lastConfHeight,
					metrics.densityDpi, 0 /* flags */, mInputSurface, null /* callback */, null /* handler */);

		}
	}
}
