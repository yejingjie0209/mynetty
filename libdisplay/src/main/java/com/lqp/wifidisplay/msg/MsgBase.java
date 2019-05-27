package com.lqp.wifidisplay.msg;

import java.nio.*;
import java.util.*;

public abstract class MsgBase {
	private final static int  HEADER_MAGIC  = 0x544D; //MT
	private final static int  HEADER_SEED = 0x5555;
	private final static int  PROTOCOL_VERSION = 0x8002;
	
	public final static int MAX_PAYLOAD_LEN = 1024;
	public final static int MAX_DESC_LEN = 255;
	
	public final static int MAX_SEGMENT_LEN = 900;
	
	public static final int  msg_type_service_query 	= 100;
	public static final int  msg_type_service_query_ack = 101;
	public static final int  msg_type_ping				= 102;
	public static final int  msg_type_pong 				= 103;
	public static final int msg_type_frame_data			= 104;
	public static final int msg_type_get_frame			= 105;
	
	public static final int  DISPLAY_MSG_PACKET0 = 1;
	public static final int  DISPLAY_MSG_FRAME   = 2;
	public static final int  DISPLAY_MSG_FIN     = 3;
	public static final int  DISPLAY_HEARTBEAT   = 4;
	public static final int  DISPLAY_MSG_RESET_DECODER = 5;
	
	public static final int  DISPLAY_IDLE = 1;
	public static final int  DISPLAY_IN_USE = 2;
	
	private static Map<Integer, Class<? extends MsgBase>> decodeTable = new HashMap<Integer, Class<? extends MsgBase>>();
	
	public static void registerDecoderType(int type, Class<? extends MsgBase> msg) {
		decodeTable.put(type, msg);
	}
	
	static {
		registerDecoderType(msg_type_service_query_ack, ServiceQueryAckMsg.class);
		registerDecoderType(msg_type_ping, PingMsg.class);
		registerDecoderType(msg_type_pong, PongMsg.class);
		registerDecoderType(msg_type_get_frame, GetFrameSegMsg.class);
	}
	
	public void encodeTo(ByteBuffer buffer) throws Exception {
		buffer.order(ByteOrder.BIG_ENDIAN);
		
		buffer.putShort((short)(HEADER_MAGIC ^ HEADER_SEED));
		buffer.putShort((short)(PROTOCOL_VERSION ^ HEADER_SEED));
		buffer.putShort((short)(getMsgType() ^ HEADER_SEED));
		
		encodeBody(buffer);
	}
	
	public static MsgBase decodeFrom(ByteBuffer buffer) throws Exception {
		buffer.order(ByteOrder.BIG_ENDIAN);
		
		int magic = (buffer.getShort() ^ HEADER_SEED) & 0xffff;
		int version = (buffer.getShort() ^ HEADER_SEED) & 0xffff;
		
		if (magic != HEADER_MAGIC || version != PROTOCOL_VERSION) {
			throw new Exception("malfromed message!!!");
		}
		
		int type = (buffer.getShort() ^ HEADER_SEED) & 0xffff;
		
		Class<? extends MsgBase> msgCls = decodeTable.get(type);
		if (msgCls == null) {
			throw new Exception("unknow decoder msg type: " + type);
		}
		
		MsgBase msg = msgCls.newInstance();
		
		msg.decodeBody(buffer);
		
		return msg;
	}
	
	protected void notImplementedMethod() throws Exception{
		throw new Exception("not implemented method");
	}
	
	abstract public int getMsgType();

	abstract protected void encodeBody(ByteBuffer buffer) throws Exception;
	abstract protected void decodeBody(ByteBuffer buffer) throws Exception;
}
