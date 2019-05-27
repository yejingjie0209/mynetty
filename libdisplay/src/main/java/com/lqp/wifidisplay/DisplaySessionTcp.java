package com.lqp.wifidisplay;

import android.os.*;

import com.lqp.wifidisplay.msg.*;

import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public final class DisplaySessionTcp extends DisplaySession{
	public final static int FRAME_HEADER_LEN = 7;
	
	private SocketChannel socket;
	private Handler uiHandler = new Handler(Looper.getMainLooper());
	ByteBuffer protBuffer = ByteBuffer.wrap(new byte[6]);
	
	private int frameSeq = 1;
	private boolean partialWrite = false;
	private boolean pendingFinish = false;
	private volatile boolean forceClose = false;
	private int keyFrameCount = 0;
	
	private LinkedList<FrameDesc> sendCache = new LinkedList<FrameDesc>();
	private LinkedList<FrameDesc> sendBuffer = new LinkedList<FrameDesc>();
	private ByteBuffer writeBuffer = ByteBuffer.allocate(MsgBase.MAX_PAYLOAD_LEN);
	
	
	static class FrameDesc {
		boolean keyFrame;
		boolean metaFrame;
		int frameSeq;
		int totalLen;
		int restLen;
		
		byte[] data;
		
		void clear() {
			keyFrame = false;
			metaFrame = false;
			frameSeq = 0;
			restLen = 0;
			totalLen = 0;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("key: " + keyFrame
							+ ", meta: " + metaFrame
							+ ", frameSeq: " + frameSeq 
							+ ", totalLen = " + totalLen 
							+ ", restLen = " + restLen);
			return builder.toString();
		}
	}
	
	public DisplaySessionTcp(RemoteDisplayManager manager, RemoteDisplay display, SocketChannel socket) {
		super(manager, display);
		
		try {
			socket.socket().setTcpNoDelay(true);
			socket.socket().setKeepAlive(false);
			socket.configureBlocking(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.socket = socket;
	}
	
	public RemoteDisplay getDisplay() {
		return display;
	}
	
	public void pushTcpFrame(FrameDesc desc) {
		if (desc.keyFrame) {
			keyFrameCount++;
		}
		
		sendBuffer.offer(desc);
	}
	
	public void cacheFrameDesc(FrameDesc desc) {
		if (sendCache.size() < 240) {
			desc.clear();
			sendCache.offer(desc);
		}
	}
	
	
	@Override
	protected void onHeartbeat() throws Exception {
		if (!partialWrite) {
			writeBuffer.clear();
			writeBuffer.putShort((short)MsgBase.DISPLAY_HEARTBEAT);
			writeBuffer.flip();
			
			while (writeBuffer.hasRemaining()) {
				socket.write(writeBuffer);
			}
		}
	}
	
	private FrameDesc popTcpFrame(boolean allowCache) {
		FrameDesc desc = sendBuffer.removeFirst();
		if (desc.keyFrame) {
			keyFrameCount--;
		}
		
		if (allowCache) {
			cacheFrameDesc(desc);
			return null;
		}else {
			return desc;
		}
	}
	
	private void trySendTcpFrame() throws Exception{
		if (sendBuffer.size() == 0) {
			partialWrite = false;
			return;
		}
		
		//try send
		long ts = System.nanoTime();
		while(sendBuffer.size() > 0) {
			if (pendingFinish && !partialWrite) {
				sendBuffer.clear();
				sendCache.clear();
				return;
			}
			
			if (System.nanoTime() - ts > 10 * 1000 * 1000) {
				VideoLog.e("lqp", "outter time slice used out: " + (System.nanoTime() - ts) / 1000000);
				break;
			}
			
			FrameDesc desc = sendBuffer.getFirst();
			ByteBuffer buffer = ByteBuffer.wrap(desc.data);
			
			buffer.position(desc.totalLen - desc.restLen);
			buffer.limit(desc.totalLen);
			while(buffer.hasRemaining()) {
				if (System.nanoTime() - ts > 10 * 1000 * 1000) {
					VideoLog.e("lqp", "inner time slice used out: " + (System.nanoTime() - ts) / 1000000);
					break;
				}
				
				//long wTs = System.nanoTime();
				int writeN = socket.write(buffer);
				
				desc.restLen -= writeN;
				
				if (writeN == 0) {
					//Log.e("lqp", "write zero byte: seq = " + zero + ", buffer = " + desc);
					break;
				}else {
					//when has sent data, update heartbeat ts to prevent send heartbeat
					heartbeatTs = System.currentTimeMillis();

					//clear time out
					onPeerResponse();
					//Log.e("lqp", "write N = " + writeN + ", time = " + (System.nanoTime() - wTs) / 1000 + ", buffer = " + desc);
				}
			}
			
			if (buffer.hasRemaining()) {
				if (desc.restLen == desc.totalLen) {
					partialWrite = false;
				}else {
					partialWrite = true;
				}
				
				break;
			}else {
				partialWrite = false;
				popTcpFrame(true);
			}
		}
		
		if (keyFrameCount > 1) {
			//save first frame
			int prevN = sendBuffer.size();
			FrameDesc partialFrame = null;
			if (partialWrite) {
				partialFrame = popTcpFrame(false);
			}
			
			while(sendBuffer.size() > 0 && keyFrameCount > 1) {
				popTcpFrame(true);
			}
			
			while (sendBuffer.size() > 0) {
				FrameDesc desc = sendBuffer.getFirst();
				if (desc.keyFrame) {
					break;
				}else {
					popTcpFrame(true);
				}
			}

			int popedN = prevN - sendBuffer.size();
			VideoLog.e("lqp", "stucked trigger skip frames: skiped = " + popedN
					     + ", prevN = " + prevN + ", curN = " + sendBuffer.size());

			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					stateCb.onLowTransferQuality(-1);
				}
			});

			if (partialFrame != null) {
				sendBuffer.addFirst(partialFrame);
			}
		}
	}
	
	@Override
	protected void onHandleTick() throws Exception {
		try {
			trySendTcpFrame();
		} catch (Exception e) {
			forceClose = true;
			throw e;
		}
	}
	
	@Override
	public int sendFrame(int flag, byte[] data, int offset, int count) throws Exception {
		if (isAlive == false) {
			throw new Exception("session closed");
		}
		
		try {

			//write header
			byte key = (flag == FRAME_FLAG_KEY_FRAME) ? (byte)1: (byte)0;
			ByteBuffer buffer = ByteBuffer.wrap(data, 0, count + offset);
			buffer.putShort((short)MsgBase.DISPLAY_MSG_FRAME);
			buffer.putInt(count + 1);
			buffer.put(key);
			buffer.flip();
			
			buffer.limit(count + offset);
			
			FrameDesc desc = null;
			if (sendCache.size() > 0) {
				desc = sendCache.poll();
			}else {
				desc = new FrameDesc();
			}
			
			if (desc.data == null || desc.data.length < buffer.limit()) {
				desc.data = new byte[buffer.limit()];
			}
			
			System.arraycopy(data, 0, desc.data, 0, buffer.limit());
			
			desc.keyFrame = (flag == FRAME_FLAG_KEY_FRAME);
			desc.metaFrame = false;
			desc.totalLen = buffer.limit();
			desc.restLen = desc.totalLen;
			desc.frameSeq = frameSeq++;
			
			pushTcpFrame(desc);
			return 0;
		} catch (Exception e) {
			forceClose = true;
			finishSession();
			throw e;
		}
	}
	
	public boolean isAlive() {
		return isAlive;
	}

	@Override
	public void finishSession() throws Exception{
		if (isAlive == false) {
			return;
		}

		try {
			if (!forceClose) {
				VideoLog.e("lqp", "not force close, partialWrite: " + partialWrite);
				if (partialWrite) {
					pendingFinish = true;

					int count = 0;
					while (partialWrite) {
						trySendTcpFrame();
						count++;

						if (count > 50) {
							VideoLog.e("lqp", "maybe error, trySendTcpFrame not make partialWrite false");
							break;
						}
					}
				}

				//write header
				protBuffer.clear();
				protBuffer.putShort((short)MsgBase.DISPLAY_MSG_FIN);
				protBuffer.flip();

				while(protBuffer.hasRemaining()) {
					socket.write(protBuffer);
				}

				VideoLog.e("lqp", "fin packet writed");
				Thread.sleep(80);
			}

		} catch (Exception e) {
			VideoLog.e("lqp", "finishSession exception: " + e);
		}finally {
			isAlive = false;
			socket.close();
			socket = null;
		}
	}
}
