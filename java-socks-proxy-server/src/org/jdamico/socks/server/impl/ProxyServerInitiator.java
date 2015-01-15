

package	org.jdamico.socks.server.impl;



import static org.bitcoinj.core.Coin.COIN;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.protocols.channels.PaymentChannelServerListener;
import org.bitcoinj.protocols.channels.PaymentChannelCloseException.CloseReason;
import org.bitcoinj.protocols.channels.PaymentChannelServerListener.HandlerFactory;
import org.bitcoinj.protocols.channels.ServerConnectionEventHandler;
import org.bitcoinj.protocols.channels.StoredPaymentChannelServerStates;
import org.princeton.btsocks.discovery.Discovery;
import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.DebugLog;
import org.princeton.btsocks.discovery.BitcoinDiscovery;
import org.princeton.btsocks.discovery.BittorrentDiscovery;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

public class ProxyServerInitiator	implements	Runnable, HandlerFactory
{
	
	
	protected	Object	m_lock;
	
	protected	Thread			m_TheThread		= null;

	protected	ServerSocket	m_ListenSocket	= null;
	
	protected	int				m_nPort			= 0;
	
	public	int		getPort()		{	return	m_nPort;		}
	
	private DebugLog debugLog;
	
	private WalletAppKit appKit;
	
	Map<InetAddress, Semaphore> clientPaymentMap = new HashMap<InetAddress, Semaphore>();
	
	boolean isServer;

	public	ProxyServerInitiator(int listenPort, boolean enableDebugLog, boolean isServer) {
		
		m_lock = this;	
		m_nPort			= listenPort;
		debugLog = new DebugLog(enableDebugLog);
		debugLog.println( "SOCKS Server Created." );
		this.isServer = isServer;
	}
	
	public	void setLock( Object lock ) {
		this.m_lock = lock;
	}
	

	public	void start() {
		
		m_TheThread = new Thread( this );
		m_TheThread.start();
		debugLog.println( "SOCKS Server Started." );
		
		if (isServer) {
			NetworkParameters params = RegTestParams.get();
			appKit = new WalletAppKit(params, new File("."), "payment_channel_example_server") {
	            @Override
	            protected List<WalletExtension> provideWalletExtensions() {
	                // The StoredPaymentChannelClientStates object is responsible for, amongst other things, broadcasting
	                // the refund transaction if its lock time has expired. It also persists channels so we can resume them
	                // after a restart.
	                return ImmutableList.<WalletExtension>of(new StoredPaymentChannelServerStates(null));
	            }
	        };
	        appKit.connectToLocalHost();
	        appKit.startAsync();
	        appKit.awaitRunning();        
	        
	        try {
	        	System.out.println("Announcing on port " + getPort());
//	        	Discovery discovery = new BitcoinDiscovery(appKit);
	        	Discovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
		        discovery.announceProxy(InetAddress.getLocalHost(), getPort());
				System.out.println("Advertising server on bittorrent");
	        	
	        	System.out.println("Starting payment server");
	        	final PaymentChannelServerListener server = new PaymentChannelServerListener(appKit.peerGroup(), appKit.wallet(), 15, COIN, this);
				server.bindAndStart(4242);
				System.out.println("Finished starting payment server");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public	void	stop()	{
		
		debugLog.println( "SOCKS Server Stopped." );
		m_TheThread.interrupt();
	}
	
	public	void	run()
	{
		setLock( this );
		listen();
		close();
	}

	public	void close() {
		
		if( m_ListenSocket != null )	{
			try	{
				m_ListenSocket.close();
			}
			catch( IOException e )	{
			}
		}
		m_ListenSocket = null;
		
		debugLog.println( "SOCKS Server Closed." );
	}
	
	public	boolean	isActive()	{
		return	(m_ListenSocket != null);	
	}
	
	
	private	void prepareToListen()	throws java.net.BindException, IOException {
		synchronized( m_lock )
		{
			m_ListenSocket = new ServerSocket( m_nPort );
			m_ListenSocket.setSoTimeout( Constants.LISTEN_TIMEOUT );
	
			if( m_nPort == 0 )	{
				m_nPort = m_ListenSocket.getLocalPort();
			}
			debugLog.println( "SOCKS Server Listen at Port : " + m_nPort );
		}
	}
	
	protected	void listen() {
	
		try
		{
			prepareToListen();
		}
		catch( java.net.BindException e )	{
			debugLog.error( "The Port "+m_nPort+" is in use !" );
			debugLog.error( e );
			return;
		}
		catch( IOException e )	{
			debugLog.error( "IO Error Binding at port : "+m_nPort );
			return;
		}

		while( isActive() )	{
			checkClientConnection();
			Thread.yield();
		}
	}
	
	public	void checkClientConnection()	{
		synchronized( m_lock )
		{
		//	Close() method was probably called.
			if( m_ListenSocket == null )	return;
	
			try
			{
				Socket clientSocket = m_ListenSocket.accept();
				clientSocket.setSoTimeout( Constants.DEFAULT_SERVER_TIMEOUT );
				debugLog.println( "SOCKS Connection from : " + debugLog.getSocketInfo( clientSocket ) );
				
				Semaphore payment = null;
				if (isServer) {
					InetAddress address = clientSocket.getLocalAddress();
					if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
			            address = InetAddress.getLocalHost();
					}
					
					payment = clientPaymentMap.get(address);
					if (payment == null) {
						payment = new Semaphore(4, true);
						clientPaymentMap.put(address, payment);
						System.out.println("Creating entry in checkClientConnection");
					}
				}
				ProxyHandler proxy = new ProxyHandler(clientSocket, debugLog, payment);
				proxy.start();
			}
			catch( InterruptedIOException e )		{
			//	This exception is thrown when accept timeout is expired
			}
			catch( Exception e )	{
				debugLog.error( e );
			}
		}	// synchronized
	}
	
	public ServerConnectionEventHandler onNewConnection(final SocketAddress clientAddress) {
    	System.out.println("Proxy server initiator recieving new payment connection from " + clientAddress);
    	InetSocketAddress socketAddress = (InetSocketAddress) clientAddress;
    	InetAddress address = socketAddress.getAddress();
    	if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
			try {
				address = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	final InetAddress realAddress = address;

    	return new ServerConnectionEventHandler() {
        	
        	// Important!
        	// This assumes that the client has connected to the server via SOCKS before opening a payment channel
        	
            @Override
            public void channelOpen(Sha256Hash channelId) {
            	System.out.println("ProxyHandler: Server opened channel");
//                channelOpenFuture.set(channelId);
            }

            @Override
            public void channelClosed(CloseReason reason) {
            	System.out.println("ProxyHandler: Server closed channel");
//                serverCloseFuture.set(null);
            }

			@Override
			public ListenableFuture<ByteString> paymentIncrease(Coin by, Coin to, ByteString info) {
				System.out.println("Server recieved payment!!!");
				Semaphore payment = clientPaymentMap.get(realAddress);
				if (payment == null) {
					System.out.println("Creating entry in paymentIncrease");
					payment = new Semaphore(4, true);
					clientPaymentMap.put(realAddress, payment);
				}
				payment.release();
				return null;
			}
        };
    }
	
}

