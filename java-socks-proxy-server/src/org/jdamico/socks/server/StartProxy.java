package org.jdamico.socks.server;

import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.UnknownHostException;

import org.jdamico.socks.server.impl.ProxyServerInitiator;
import org.princeton.btsocks.discovery.BittorrentDiscovery;
import org.princeton.btsocks.discovery.Discovery;
import org.princeton.btsocks.server.SocksProxySelector;

public class StartProxy {
	public static void main(String[] args) throws UnknownHostException {
		
		String noParamsMsg = "Unable to parse command-line parameters, using default configuration.";
		
		int port = 8888;
		boolean enableDebugLog = true;
		boolean isServer = true;
		
		if(args.length == 3){
			try {
				isServer = Boolean.parseBoolean(args[0].trim());
				port = Integer.parseInt(args[1].trim());
				enableDebugLog = Boolean.parseBoolean(args[2].trim());
			} catch (Exception e) {
				System.out.println(noParamsMsg);
			}
		} else {
			System.out.println(noParamsMsg);
		}
		
		ProxyServerInitiator proxyServerInitiator = new ProxyServerInitiator(port, enableDebugLog, isServer);
		proxyServerInitiator.start();
		System.out.println("Starting proxy on port " + port);
		
		if (!isServer) { 
			Discovery discovery = new BittorrentDiscovery(InetAddress.getByName("10.9.138.0"), 6969);
			SocksProxySelector ps = new SocksProxySelector(ProxySelector.getDefault(), discovery);
	        ProxySelector.setDefault(ps);
		}
	}
}
