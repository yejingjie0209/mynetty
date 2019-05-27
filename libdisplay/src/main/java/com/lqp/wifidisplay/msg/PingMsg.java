package com.lqp.wifidisplay.msg;

import java.nio.*;

public class PingMsg extends MsgBase {
	public String desc;
	public long ts = System.currentTimeMillis();
	
	public PingMsg() {
		
	}
	
	public PingMsg(String desc) {
		this.desc = desc;
	}

	@Override
	public int getMsgType() {
		return msg_type_ping;
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
