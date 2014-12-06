package org.jdamico.socks.server.commons;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {
	
	private Utils() {}

	
	public static InetAddress calcInetAddress( byte[] addr, DebugLog debugLog)	{
		InetAddress	IA  = null;
		String		sIA = "";		
		
		if( addr.length < 4 )	{
			debugLog.error( "calcInetAddress() - Invalid length of IP v4 - "+addr.length+" bytes" );	
			return null;
		}
		
		// IP v4 Address Type
		for( int i=0; i<4; i++ )	{
			sIA += byte2int( addr[i] );
			if( i<3 )	sIA += ".";
		}
		
		try	{
			IA = InetAddress.getByName( sIA );
		}
		catch( UnknownHostException e )	{
			return null;
		}
		
		return	IA; // IP Address
	}
	
	public static int byte2int( byte b )	{
		int	res = b;
		if( res < 0 ) res = (int)( 0x100 + res );
		return	res;
	}

	
	public static int calcPort( byte Hi, byte Lo )	{
		
		return ( (byte2int( Hi ) << 8) | byte2int( Lo ) );	
	}
	
	
	public static String	iP2Str( InetAddress IP )	{
		if( IP == null )	return "NA/NA";
		
		return	IP.getHostName()+"/"+IP.getHostAddress();
	}

}
