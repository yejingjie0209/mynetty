package com.lqp.wifidisplay.msg;

import java.nio.*;

public final class ServiceQueryAckMsg extends MsgBase {
	public int status;
	public int port;
	public String desc;
	
	@Override
	public int getMsgType() {
		return msg_type_service_query_ack;
	}
	
	public boolean isIdle() {
		if (status == DISPLAY_IDLE) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void encodeBody(ByteBuffer buffer) throws Exception{
		notImplementedMethod();
	}

	@Override
	public void decodeBody(ByteBuffer buffer) throws Exception {
		int status = buffer.getShort() & 0xffff;
		int port = buffer.getShort() & 0xffff;
		int len = buffer.getShort() & 0xffff;
		
		this.status = status;
		this.port = port;
		
		byte[] data = new byte[len];
		buffer.get(data);
		
		desc = new String(data);
	}

}
