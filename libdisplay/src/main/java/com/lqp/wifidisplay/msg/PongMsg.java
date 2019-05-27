package com.lqp.wifidisplay.msg;

import java.nio.*;

public class PongMsg extends MsgBase {
	public long ts;
	public String desc;
	
	public PongMsg() {
		
	}
	
	public PongMsg(String desc, long ts) {
		this.desc = desc;
		this.ts = ts;
	}
	
	@Override
	public int getMsgType() {
		return msg_type_pong;
	}

	@Override
	protected void encodeBody(ByteBuffer buffer) throws Exception {
		buffer.putLong(ts);
		
		byte[] data = desc.getBytes();
		buffer.putShort((short)data.length);
		buffer.put(data);
	}

	@Override
	protected void decodeBody(ByteBuffer buffer) throws Exception {
		ts = buffer.getLong();
		
		int len = buffer.getShort() & 0xffff;
		byte[] data = new byte[len];
		buffer.get(data);
		
		desc = new String(data);
	}

}
