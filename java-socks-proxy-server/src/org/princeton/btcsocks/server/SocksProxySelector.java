package org.princeton.btcsocks.server;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import org.princeton.btsocks.discovery.RemoteProxyAddress;

import com.google.common.net.InetAddresses;

// Taken from https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

public class SocksProxySelector extends ProxySelector {
	// Keep a reference on the previous default
	ProxySelector defsel = null;
	private int requestNum = 0;

	/*
	 * A list of proxies, indexed by their address.
	 */
	Map<SocketAddress, RemoteProxyAddress> proxies = new HashMap<SocketAddress, RemoteProxyAddress>();

	public SocksProxySelector(ProxySelector def) {
		
		System.out.println("Running constructor");
		// Save the previous default
		defsel = def;

		// Populate the HashMap (List of proxies)
		
		try {
			RemoteProxyAddress i = new RemoteProxyAddress(InetAddress.getByName("10.9.138.0"), 8888);
			proxies.put(i.address(), i);
			i = new RemoteProxyAddress(InetAddress.getByName("10.9.141.123"), 8888);
			proxies.put(i.address(), i);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			for (RemoteProxyAddress p : proxies.values()) {
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
		RemoteProxyAddress p = proxies.get(sa); 
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