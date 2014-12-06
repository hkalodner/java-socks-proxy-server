package org.princeton.btsocks.discovery;

import java.net.InetAddress;

public class RemoteProxyAddress {
	
	public final InetAddress address;
	public final int port;
	
	public RemoteProxyAddress(InetAddress address, int port) {
	    this.address = address;
	    this.port = port;
	}

}
