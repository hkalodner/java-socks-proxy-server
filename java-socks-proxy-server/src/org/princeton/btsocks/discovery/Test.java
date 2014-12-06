package org.princeton.btsocks.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

public class Test {

	public static void main(String[] args) throws IOException {
		System.out.println("Starting discovery.");
		try {
			BittorrentDiscovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
			discovery.getProxies();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}