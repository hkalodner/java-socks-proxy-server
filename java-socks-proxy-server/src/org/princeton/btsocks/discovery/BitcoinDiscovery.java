package org.princeton.btsocks.discovery;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;

public class BitcoinDiscovery {
    
    public final byte[] MAGIC_NUMBER = {23, -112, 88, -55};
    public final int ADVERTISMENT_BUFFER_LENGTH = 40;
    
    private final Wallet wallet;
    private final InetAddress address;
    private final int port;
    private final byte[] advertismentBuffer;
    private final NetworkParameters networkParams;
    private final BlockChain blockChain;
    private final PeerGroup peerGroup;
    
    /**
     * Construct a new BitcoinDiscovery object that will use the given wallet.
     * @param wallet wallet to burn bitcoins from to advertise address on the block chain.
     */
    public BitcoinDiscovery(Wallet wallet, InetAddress address, int port) {
        this.wallet = wallet;
        this.address = address;
        this.port = port;
        
        ByteBuffer buffer = ByteBuffer.allocate(ADVERTISMENT_BUFFER_LENGTH);
        buffer.put(MAGIC_NUMBER);
        buffer.put(address.getAddress());
        buffer.putInt(port);
        
        networkParams = UnitTestParams.get();
        BlockStore store = new MemoryBlockStore(networkParams);
                
        advertismentBuffer = buffer.array();
        try {
            blockChain = new BlockChain(networkParams, wallet, store);
        } catch (BlockStoreException e) {
            throw new RuntimeException(e.getMessage());
        }
        peerGroup = new PeerGroup(networkParams, blockChain);
        peerGroup.addWallet(wallet);
        peerGroup.startAndWait();
    }
    
    public void announceProxy(int port) {
        Transaction transaction = new Transaction(networkParams);
        transaction.addOutput(Transaction.MIN_NONDUST_OUTPUT, new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(advertismentBuffer).build());
    }
    
    
    public List<RemoteProxyAddress> getProxies() {
        throw new RuntimeException("Feature not yet implemented.");
    }
    
}
