package org.princeton.btsocks.discovery;

import java.net.InetAddress;
import java.util.List;

public interface Discovery {
	public List<RemoteProxyAddress> getProxies();
	public void announceProxy(InetAddress address, int port) throws Exception;
}
