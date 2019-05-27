package com.lqp.wifidisplay;


public class DisplaySessionUdp extends DisplaySession{
	private int token;
	
	public DisplaySessionUdp(RemoteDisplayManager m, RemoteDisplay d, int token) {
		super(m, d);
		this.token = token;
	}

	@Override
	protected void onHandleTick() throws Exception {
		//noting to do
	}

	@Override
	protected void onHeartbeat() throws Exception {
		//nothing to do
	}

	@Override
	public int sendFrame(int flag, byte[] data, int offset, int count) throws Exception {
		if (isAlive) {
			int key = flag & FRAME_FLAG_KEY_FRAME;
			return manager.sendUdpFrame(data, offset, count, key);
		}else {
			throw new Exception("session is dead: " + this.hashCode());
		}
	}

	@Override
	public void finishSession() throws Exception {
		manager.udpChannelBye();
		isAlive = false;
	}
}
