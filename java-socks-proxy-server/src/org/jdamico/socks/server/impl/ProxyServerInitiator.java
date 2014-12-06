

package	org.jdamico.socks.server.impl;



import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.DebugLog;



public class ProxyServerInitiator	implements	Runnable
{
	
	
	protected	Object	m_lock;
	
	protected	Thread			m_TheThread		= null;

	protected	ServerSocket	m_ListenSocket	= null;
	
	protected	int				m_nPort			= 0;
	
	public	int		getPort()		{	return	m_nPort;		}
	
	private DebugLog debugLog;

	public	ProxyServerInitiator(int listenPort, boolean enableDebugLog) {
		
		m_lock = this;	
		m_nPort			= listenPort;
		debugLog = new DebugLog(enableDebugLog);
		debugLog.println( "SOCKS Server Created." );
	}
	
	public	void setLock( Object lock ) {
		this.m_lock = lock;
	}
	

	public	void start() {
		m_TheThread = new Thread( this );
		m_TheThread.start();
		debugLog.println( "SOCKS Server Started." );
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
				debugLog.println( "Connection from : " + debugLog.getSocketInfo( clientSocket ) );
				ProxyHandler proxy = new ProxyHandler(clientSocket, debugLog);
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
}

