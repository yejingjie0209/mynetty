package com.lqp.wifidisplay.msg;

import java.net.*;
import java.nio.*;

public class GetFrameSegMsg extends MsgBase {
	public int head;
	public int frame;
	public int seg;
	
	public SocketAddress address;

	@Override
	public int getMsgType() {
		return msg_type_get_frame;
	}

	@Override
	protected void encodeBody(ByteBuffer buffer) throws Exception {
		notImplementedMethod();
	}

	@Override
	protected void decodeBody(ByteBuffer buffer) throws Exception {
		head = buffer.getInt();
		frame = buffer.getInt();
		seg = buffer.getShort() & 0xffff;
	}
}
