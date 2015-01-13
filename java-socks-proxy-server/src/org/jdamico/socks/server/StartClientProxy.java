package org.jdamico.socks.server;


import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.UnknownHostException;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.impl.ProxyServerInitiator;
import org.princeton.btcsocks.server.SocksProxySelector;
import org.princeton.btsocks.discovery.BittorrentDiscovery;
import org.princeton.btsocks.discovery.Discovery;


public class StartClientProxy {
	
	public static void main(String[] args) throws UnknownHostException {
		
		/*
		 * enableDebugLog
		 * listenPort
		 */
		System.out.println("StartClientProxy");
		
		int port = Constants.LISTEN_PORT;
		boolean enableDebugLog = true;
		
		String noParamsMsg = "Unable to parse command-line parameters, using default configuration.";
		System.out.println("Checking args");
		if(args.length == 2){
			try {
				String prePort = args[0].trim();
				port = Integer.parseInt(prePort);
				String preDebug = args[1].trim();
				enableDebugLog = Boolean.parseBoolean(preDebug);
			} catch (Exception e) {
				System.out.println(noParamsMsg);
			}
		}	else System.out.println(noParamsMsg);
		System.out.println("Starting proxy client on port " + port);
		new ProxyServerInitiator(port, enableDebugLog, false).start();
		
		Discovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
		SocksProxySelector ps = new SocksProxySelector(ProxySelector.getDefault(), discovery);
        ProxySelector.setDefault(ps);
	}
	
}

