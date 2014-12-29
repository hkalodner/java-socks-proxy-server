package org.princeton.btsocks.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BittorrentDiscovery {

    private InetAddress trackerAddress;
    private final int trackerPort;
    private final Random random;
    
    private static final byte[] ZEROS = new byte[4];
    
    // Randomly generated info hash to announce to trackers.
    public static final byte[] INFO_HASH = new byte[]{103, -40, -42, -30, -1, -49, 122, -4, -67, 61, -36, 111, 66, -31, -82, -82, -91, 109, -32, -19};
    public static final byte[] PEER_ID = new byte[] {42, 50 , -42, -30, -1, -49, 122, -4, -67, 61, -36, 111, 66, -31, -82, -82, -91, 109, -32, -19};

    public BittorrentDiscovery(InetAddress trackerAddress, int trackerPort) {
        this.trackerAddress = trackerAddress;
        this.trackerPort = trackerPort;
        random = new Random();
    }

    public List<RemoteProxyAddress> getProxies() throws IOException {
        ConnectResponseValues connectValues = announceRequest(2);
        
        DatagramPacket announceResponsePacket = new DatagramPacket(new byte[1400], 1400);
        connectValues.socket.receive(announceResponsePacket);
        connectValues.socket.close();
        
        ByteBuffer announceResponse = ByteBuffer.wrap(announceResponsePacket.getData());
        announceResponse.order(ByteOrder.BIG_ENDIAN);
        TrackerAction action = TrackerAction.valueOf(announceResponse.getInt());
        if (action != TrackerAction.ANNOUNCE) {
            System.out.println("action != TrackerAction.ANNOUNCE");
            return null;
        }
        int responseTransactionId = announceResponse.getInt();
        if (responseTransactionId != connectValues.transactionId) {
            System.out.println("responseTransactionId != connectValues.transactionId");
            return null;
        }
        // TODO: actually wait the interval amount of time;
        int interval = announceResponse.getInt();
        int leechers = announceResponse.getInt();
        int seeders = announceResponse.getInt();
        List<RemoteProxyAddress> peers = new ArrayList<RemoteProxyAddress>();
        
        System.out.println("Enumerating peers.");
        while (announceResponse.hasRemaining()) {
            byte[] addressBuffer = new byte[4];
            announceResponse.get(addressBuffer);
            if (Arrays.equals(addressBuffer, ZEROS)) {
                // If the address is all zeros, then we know we have read beyond the list of peers.
                break;
            }
            InetAddress address = InetAddress.getByAddress(addressBuffer);
            
            byte[] portBuffer = new byte[2];
            announceResponse.get(portBuffer);
            int port = ((portBuffer[0] & 0xFF) << 8) | (portBuffer[1] & 0xFF);
            peers.add(new RemoteProxyAddress(address, port));
        }
        
        return peers;
    }
    
    public void announceProxy(int port) throws IOException {
        ConnectResponseValues connectValues = announceRequest(port);
        connectValues.socket.close();
    }
    
    private ConnectResponseValues announceRequest(int port) throws IOException {
        System.out.println("connecting to tracker");
        ConnectResponseValues connectValues = connectToTracker();
        System.out.println("connected to tracker");
        
        byte[] portBuffer = new byte[2];
        portBuffer[0] = (byte)((port >> 8) & 0xFF);
        portBuffer[1] = (byte)(port & 0xFF);
        
        ByteBuffer announceRequest = ByteBuffer.allocate(98);
        
        // TODO: Factor out the announce just like "connect" is factored out.
        announceRequest.putLong(connectValues.connectionId);
        announceRequest.putInt(TrackerAction.ANNOUNCE.getNumericValue());
        announceRequest.putInt(connectValues.transactionId);
        announceRequest.put(INFO_HASH);
        announceRequest.put(PEER_ID);
        announceRequest.putLong(0); // downloaded
        announceRequest.putLong(Long.MAX_VALUE); // left
        announceRequest.putLong(0); // uploaded
        announceRequest.putInt(2); //started
        announceRequest.putInt(0); // IP addr, 0 to have tracker use the IP it sees
        announceRequest.putInt(0); // key. Not needed.
        announceRequest.putInt(-1); // Number of peers we want returned. -1 is default, which usually results in getting 50.
        announceRequest.put(portBuffer); // port. Not necessary on clients.
        
        DatagramPacket announceRequestPacket = new DatagramPacket(announceRequest.array(), 0, 98, trackerAddress, trackerPort);
        connectValues.socket.send(announceRequestPacket);
        return connectValues;
    }

    private ConnectResponseValues connectToTracker() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        int transactionId = random.nextInt();

        ByteBuffer connectRequestBuffer = ByteBuffer.allocate(16);
        connectRequestBuffer.order(ByteOrder.BIG_ENDIAN);
        connectRequestBuffer.putLong(0x41727101980L);
        connectRequestBuffer.putInt(TrackerAction.CONNECT.getNumericValue());
        connectRequestBuffer.putInt(transactionId);

        DatagramPacket connectRequest = new DatagramPacket(connectRequestBuffer.array(), 0, 16, trackerAddress,
                trackerPort);
        socket.send(connectRequest);

        byte[] connectResponseBuffer = new byte[16];
        DatagramPacket connectResponsePacket = new DatagramPacket(connectResponseBuffer, connectResponseBuffer.length);
        socket.receive(connectResponsePacket);
        ByteBuffer connectResponse = ByteBuffer.wrap(connectResponsePacket.getData());
        TrackerAction responseAction = TrackerAction.valueOf(connectResponse.getInt());
        if (responseAction != TrackerAction.CONNECT) {
            socket.close();
            throw new RuntimeException("Unexpected response for connect request: " + responseAction);
        }
        int responseTransactionId = connectResponse.getInt();
        if (responseTransactionId != transactionId) {
            // TODO: We should probably retry the connect request, or wait for
            // another packet.
            socket.close();
            return null;
        }
        long connectionId = connectResponse.getLong();

        return new ConnectResponseValues(connectionId, transactionId, socket);
    }

    private class ConnectResponseValues {
        public final long connectionId;
        public final int transactionId;
        public final DatagramSocket socket;

        public ConnectResponseValues(long connectionId, int transactionId, DatagramSocket socket) {
            this.connectionId = connectionId;
            this.transactionId = transactionId;
            this.socket = socket;
        }
    }

}
