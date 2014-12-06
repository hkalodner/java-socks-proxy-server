

package	org.jdamico.socks.server.commons;



import java.net.DatagramPacket;
import java.net.Socket;

public class DebugLog
{
	
	public boolean enabled;
	
	public DebugLog(boolean enabled) {
		this.enabled = enabled;
	}
	
	public	void	println( String txt )	{
		if(enabled)	print( txt + Constants.EOL );
	}

	
	public	void	print( String txt )	{
		if( !enabled)	return;
		if( txt == null )	return;
		System.out.print( txt );	
	}
	
	/////////////////////////////////////////////////
	
	public	void	error( String txt )	{
		if(enabled)	println( "Error : " + txt );
	}
	
	/////////////////////////////////////////////////
	
	public	void	error( Exception e )	{
		if( !enabled)	return;
		println( "ERROR : " + e.toString() );
		e.printStackTrace();
	}
	

	
	public	String	getSocketInfo( Socket sock )	{
	
		if( sock == null )	return "<NA/NA:0>";
		
		return	"<"+Utils.iP2Str( sock.getInetAddress() )+":"+
				sock.getPort() + ">";
	}
	
	
	
	public	String	getSocketInfo( DatagramPacket DGP )	{
	
		if( DGP == null )	return "<NA/NA:0>";
		
		return	"<"+Utils.iP2Str( DGP.getAddress() )+":"+
				DGP.getPort() + ">";
	}
	
	
}
