package org.jdamico.socks.server;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.impl.ProxyServerInitiator;
import org.princeton.btsocks.discovery.BittorrentDiscovery;
import org.princeton.btsocks.discovery.RemoteProxyAddress;

public class StartServerProxy {
	
	public	boolean	enableDebugLog = true;
	public static void main(String[] args) {
		
		/*
		 * enableDebugLog
		 * listenPort
		 */
		
		int port = 0;
		
		String noParamsMsg = "Unable to parse command-line parameters, using default configuration.";
		
		boolean enableDebugLog = true;
		
		if(args.length == 2){
			try {
				String prePort = args[0].trim();
				port = Integer.parseInt(prePort);
				String preDebug = args[1].trim();
				enableDebugLog = Boolean.parseBoolean(preDebug);
			} catch (Exception e) {
				System.out.println(noParamsMsg);
			}
		}else System.out.println(noParamsMsg);
		
		try {
			ProxyServerInitiator proxyServerInitiator = new ProxyServerInitiator(port, enableDebugLog);
			proxyServerInitiator.start();
			System.out.println("Starting proxy server on port " + port);
			BittorrentDiscovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
			discovery.announceProxy(proxyServerInitiator.getPort());
			System.out.println("Advertising server on bittorrent");
		} catch (IOException e) {
			// TODO Auto-generated catch =block
			e.printStackTrace();
		}
		
	}
	
}

