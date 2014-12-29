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
		
		int port = Constants.LISTEN_PORT;
		
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
		
		new ProxyServerInitiator(port, enableDebugLog).start();
		
		try {
			BittorrentDiscovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
			discovery.announceProxy(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}

