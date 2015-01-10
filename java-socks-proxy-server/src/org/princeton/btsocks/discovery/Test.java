package org.princeton.btsocks.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;


public class Test {

	public static void main(String[] args) throws IOException {
		System.out.println("Starting discovery.");
		try {
			BittorrentDiscovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
//			discovery.announceProxy(1234);
			List<RemoteProxyAddress> proxyList = discovery.getProxies();
			for (RemoteProxyAddress proxyAddress : proxyList) {
				System.out.println(proxyAddress);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}