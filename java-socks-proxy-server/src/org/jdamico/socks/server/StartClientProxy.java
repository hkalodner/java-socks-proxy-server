package org.jdamico.socks.server;


import java.net.ProxySelector;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.impl.ProxyServerInitiator;
import org.princeton.btcsocks.server.SocksProxySelector;


public class StartClientProxy {
	
	public static void main(String[] args) {
		
		/*
		 * enableDebugLog
		 * listenPort
		 */
		System.out.println("StartClientProxy");
		SocksProxySelector ps = new SocksProxySelector(ProxySelector.getDefault());
        ProxySelector.setDefault(ps);
		
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
		new ProxyServerInitiator(port, enableDebugLog).start();
	}
	
}

