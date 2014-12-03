package org.princeton.btsocks.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Random;

public class BittorrentDiscovery {

    private InetAddress trackerAddress;
    private final int trackerPort;
    private final Random random;
    
    // Randomly generated info hash to announce to trackers.
    public static final byte[] INFO_HASH = new byte[]{103, -40, -42, -30, -1, -49, 122, -4, -67, 61, -36, 111, 66, -31, -82, -82, -91, 109, -32, -19};
    public static final byte[] PEER_ID = new byte[] {42, 50 , -42, -30, -1, -49, 122, -4, -67, 61, -36, 111, 66, -31, -82, -82, -91, 109, -32, -19};

    public BittorrentDiscovery(InetAddress trackerAddress, int trackerPort) {
        this.trackerAddress = trackerAddress;
        this.trackerPort = trackerPort;
        random = new Random();
    }

    public List<InetAddress> getProxies() throws IOException {
        ConnectResponseValues connectValues = connectToTracker();
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
        announceRequest.putShort((short) 5000); // port. Not necessary on clients.
        
        DatagramPacket announceResponsePacket = new DatagramPacket(new byte[1400], 1400);
        connectValues.socket.receive(announceResponsePacket);
        
        ByteBuffer announceResponse = ByteBuffer.wrap(announceResponsePacket.getData());
        announceResponse.order(ByteOrder.BIG_ENDIAN);
        
        TrackerAction action = TrackerAction.valueOf(announceResponse.getInt());
        if (action != TrackerAction.ANNOUNCE) {
            connectValues.socket.close();
            return null;
        }
        int responseTransactionId = announceResponse.getInt();
        if (responseTransactionId != connectValues.transactionId) {
            connectValues.socket.close();
            return null;
        }
        // TODO: actually wait the interval amount of time;
        int interval = announceResponse.getInt();
        int leechers = announceResponse.getInt();
        int seeders = announceResponse.getInt();
        
        
        connectValues.socket.close();
        return null;
    }
    
    public void announceProxy() {

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
