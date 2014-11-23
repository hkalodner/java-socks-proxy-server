package org.princeton.btcsocks.server;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;

// Taken from https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

public class SocksProxySelector extends ProxySelector {
	// Keep a reference on the previous default
	ProxySelector defsel = null;
	private int requestNum = 0;

	/*
	 * Inner class representing a Proxy and a few extra data
	 */
	class InnerProxy {
		Proxy proxy;
		SocketAddress addr;
		// How many times did we fail to reach this proxy?
		int failedCount = 0;

		InnerProxy(InetSocketAddress a) {
			addr = a;
			proxy = new Proxy(Proxy.Type.SOCKS, a);
		}

		SocketAddress address() {
			return addr;
		}

		Proxy toProxy() {
			return proxy;
		}

		int failed() {
			return ++failedCount;
		}
	}

	/*
	 * A list of proxies, indexed by their address.
	 */
	HashMap<SocketAddress, InnerProxy> proxies = new HashMap<SocketAddress, InnerProxy>();

	public SocksProxySelector(ProxySelector def) {
		
		System.out.println("Running constructor");
		// Save the previous default
		defsel = def;

		// Populate the HashMap (List of proxies)
		InnerProxy i = new InnerProxy(new InetSocketAddress("10.9.138.0", 8888));
		proxies.put(i.address(), i);
		
		i = new InnerProxy(new InetSocketAddress("10.9.141.123", 8888));
		proxies.put(i.address(), i);
	}

	/*
	 * This is the method that the handlers will call.
	 * Returns a List of proxy.
	 */
	@Override
	public java.util.List<Proxy> select(URI uri) {
		// Let's stick to the specs. 
		if (uri == null) {
			throw new IllegalArgumentException("URI can't be null.");
		}
		
		requestNum++;
		
		System.out.println("Trying to select");
		System.out.println("scheme = " + uri.getScheme());
		System.out.println("host = " + uri.getHost());
		System.out.println("port = " + uri.getPort());

		/*
		 * If it's a http (or https) URL, then we use our own
		 * list.
		 */
		String protocol = uri.getScheme();
		if("socket".equalsIgnoreCase(protocol)) {
			ArrayList<Proxy> l = new ArrayList<Proxy>();
			
			int proxNum = requestNum % proxies.size();
			int i = 0;
			for (InnerProxy p : proxies.values()) {
				if (i == proxNum) {
					l.add(p.toProxy());
				}
				i++;
			}
			return l;
		}
		
		
		System.out.println("Going with default");
		/*
		 * Not HTTP or HTTPS (could be SOCKS or FTP)
		 * defer to the default selector.
		 */
		if (defsel != null) {
			return defsel.select(uri);
		} else {
			ArrayList<Proxy> l = new ArrayList<Proxy>();
			l.add(Proxy.NO_PROXY);
			return l;
		}
	}

	/*
	 * Method called by the handlers when it failed to connect
	 * to one of the proxies returned by select().
	 */
	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		// Let's stick to the specs again.
		if (uri == null || sa == null || ioe == null) {
			throw new IllegalArgumentException("Arguments can't be null.");
		}

		/*
		 * Let's lookup for the proxy 
		 */
		InnerProxy p = proxies.get(sa); 
		if (p != null) {
			/*
			 * It's one of ours, if it failed more than 3 times
			 * let's remove it from the list.
			 */
			if (p.failed() >= 3)
				proxies.remove(sa);
		} else {
			/*
			 * Not one of ours, let's delegate to the default.
			 */
			if (defsel != null)
				defsel.connectFailed(uri, sa, ioe);
		}
	}
}