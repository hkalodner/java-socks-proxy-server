package org.princeton.btsocks.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

public class RemoteProxyAddress {
	
	private final SocketAddress address;
	private final Proxy proxy;
	// How many times did we fail to reach this proxy?
	private int failedCount = 0;
	
	public RemoteProxyAddress(InetAddress address, int port) {
	    this.address = new InetSocketAddress(address, port);
	    this.proxy = new Proxy(Proxy.Type.SOCKS, this.address);
	}

	public SocketAddress address() {
		return address;
	}

	public Proxy toProxy() {
		return proxy;
	}

	public int failed() {
		return ++failedCount;
	}

	public String toString() {
		return address.toString();
	}
}
