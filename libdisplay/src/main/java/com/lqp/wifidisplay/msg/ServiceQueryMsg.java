package com.lqp.wifidisplay.msg;

import java.nio.*;

public final class ServiceQueryMsg extends MsgBase {
	private String desc;
	
	public ServiceQueryMsg(String desc) {
		this.desc = desc;
	}

	@Override
	public int getMsgType() {
		return msg_type_service_query;
	}

	@Override
	public void encodeBody(ByteBuffer buffer) {
		byte[] data = desc.getBytes();
		
		buffer.putShort((short)data.length);
		buffer.put(data);
	}

	@Override
	public void decodeBody(ByteBuffer buffer) throws Exception {
		//not server
		notImplementedMethod();
	}

}
