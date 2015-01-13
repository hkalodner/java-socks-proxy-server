package org.princeton.btsocks.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.bitcoinj.protocols.channels.PaymentChannelClientConnection;

public class RemoteProxyAddress {
	
	private final InetSocketAddress address;
	private final Proxy proxy;
	private PaymentChannelClientConnection paymentChannel = null;
	// How many times did we fail to reach this proxy?
	private int failedCount = 0;
	
	public RemoteProxyAddress(InetAddress address, int port) {
	    this.address = new InetSocketAddress(address, port);
	    this.proxy = new Proxy(Proxy.Type.SOCKS, this.address);
	}

	public InetSocketAddress address() {
		return address;
	}

	public Proxy toProxy() {
		return proxy;
	}
	
	public PaymentChannelClientConnection paymentChannel() {
		return paymentChannel;
	}
	
	public void setPaymentChannel(PaymentChannelClientConnection paymentChannel) {
		this.paymentChannel = paymentChannel;
	}

	public int failed() {
		return ++failedCount;
	}

	public String toString() {
		return address.toString();
	}
}
