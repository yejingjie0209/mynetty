package com.lqp.wifidisplay;

import java.net.*;

public class RemoteDisplay {
	public final SocketAddress tcpAddress;
	public final int udp_port;
	public final String desc;
	public final boolean isIdle;
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("address: " + tcpAddress);
		builder.append("\ndesc: " + desc);
		builder.append("\nstatus : " + (isIdle ? "idle" : "busy"));
		
		
		return builder.toString();
	}
	
	public RemoteDisplay(SocketAddress address, int udp_port, String desc, boolean idle) {
		this.tcpAddress = address;
		this.udp_port = udp_port;
		this.desc = desc;
		this.isIdle = idle;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tcpAddress == null) ? 0 : tcpAddress.hashCode());
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteDisplay other = (RemoteDisplay) obj;
		if (tcpAddress == null) {
			if (other.tcpAddress != null)
				return false;
		} else if (!tcpAddress.equals(other.tcpAddress))
			return false;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		return true;
	}
}
