

package	org.jdamico.socks.server.commons;



import java.net.DatagramPacket;
import java.net.Socket;

import org.jdamico.socks.server.StartClientProxy;



public class DebugLog
{
	
	private static DebugLog INSTANCE = null;
	private DebugLog() {}
	
	public static DebugLog getInstance(){
		if(INSTANCE == null) INSTANCE = new DebugLog();
		return INSTANCE;
	}

	
	public	void	println( String txt )	{
		if( StartClientProxy.enableDebugLog )	print( txt + Constants.EOL );
	}

	
	public	void	print( String txt )	{
		if( !StartClientProxy.enableDebugLog )	return;
		if( txt == null )	return;
		System.out.print( txt );	
	}
	
	/////////////////////////////////////////////////
	
	public	void	error( String txt )	{
		if( StartClientProxy.enableDebugLog )	println( "Error : " + txt );
	}
	
	/////////////////////////////////////////////////
	
	public	void	error( Exception e )	{
		if( !StartClientProxy.enableDebugLog )	return;
		println( "ERROR : " + e.toString() );
		e.printStackTrace();
	}
	

	
	public	String	getSocketInfo( Socket sock )	{
	
		if( sock == null )	return "<NA/NA:0>";
		
		return	"<"+Utils.getInstance().iP2Str( sock.getInetAddress() )+":"+
				sock.getPort() + ">";
	}
	
	
	
	public	String	getSocketInfo( DatagramPacket DGP )	{
	
		if( DGP == null )	return "<NA/NA:0>";
		
		return	"<"+Utils.getInstance().iP2Str( DGP.getAddress() )+":"+
				DGP.getPort() + ">";
	}
	
	
}
