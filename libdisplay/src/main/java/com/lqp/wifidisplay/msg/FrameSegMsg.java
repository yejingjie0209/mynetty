package com.lqp.wifidisplay.msg;

import java.nio.*;

public class FrameSegMsg extends MsgBase {
	private final static int FLAG_KEY_FRAME = 1;
	private final static int FLAG_IS_GET = 2;
	private int frameSeq;
	private int segOffset;
	private int totalSeg;
	private byte[] data;
	private int dataOffset;
	private int len;
	private boolean keyFrame;
	private boolean isGet;
	
	public void setKeyFrame(boolean keyFrame) {
		this.keyFrame = keyFrame;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public void setLen(int len) {
		this.len = len;
	}
	
	public void setDataOffset(int offset) {
		this.dataOffset = offset;
	}
	
	public void setFrameSeq(int frameSeq) {
		this.frameSeq = frameSeq;
	}
	
	public void setSegOffset(int segOffset) {
		this.segOffset = segOffset;
	}
	
	public void setTotalSeg(int totalSeg) {
		this.totalSeg = totalSeg;
	}
	
	public void setGet(boolean isGet) {
		this.isGet = isGet;
	}
	
	public FrameSegMsg() {
	}
	
	public FrameSegMsg(boolean keyFrame, int frame, int segOffset, int totalSeg, byte[] data, int offset, int len) {
		this.frameSeq = frame;
		this.segOffset = segOffset;
		this.totalSeg = totalSeg;
		this.data = data;
		this.dataOffset = offset;
		this.len = len;
		this.keyFrame = keyFrame;
	}
	
	public int getFrame() {
		return frameSeq;
	}
	
	public int getSegOffset() {
		return segOffset;
	}
	
	@Override
	public int getMsgType() {
		return msg_type_frame_data;
	}

	@Override
	protected void encodeBody(ByteBuffer buffer) throws Exception {
		int flag = 0;
		if (keyFrame) {
			flag |= FLAG_KEY_FRAME;
		}
		
		if (isGet) {
			flag |= FLAG_IS_GET;
		}
		
		buffer.putShort((short)flag);
		buffer.putInt(frameSeq);
		buffer.putShort((short)totalSeg);
		buffer.putShort((short)segOffset);
		
		buffer.putShort((short)len);
		buffer.put(data, dataOffset, len);
	}

	@Override
	protected void decodeBody(ByteBuffer buffer) throws Exception {
		notImplementedMethod();
	}

}
