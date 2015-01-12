package org.princeton.btsocks.discovery;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FullPrunedBlockChain;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.StoredUndoableBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.FullPrunedBlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.store.MemoryFullPrunedBlockStore;

public class BitcoinDiscovery {
    
    public static final byte[] MAGIC_NUMBER = {23, -112, 88, -55};
    public static final int ADVERTISMENT_BUFFER_LENGTH = 40;
    
//    private final Wallet wallet;
//    private final AbstractBlockChain blockChain;
//    private final BlockStore store;
    private final WalletAppKit appKit;
//    private final NetworkParameters networkParams;
//    private final PeerGroup peerGroup;
    
    /**
     * Construct a new BitcoinDiscovery object that will use the given wallet.
     * @param wallet wallet to burn bitcoins from to advertise address on the block chain.
     */
    public BitcoinDiscovery(WalletAppKit appKit) {
        this.appKit = appKit;
        
//        store = new MemoryBlockStore(wallet.getNetworkParameters());
//        
//        try {
//            blockChain = new BlockChain(wallet.getNetworkParameters(), wallet, store);
//        } catch (BlockStoreException e) {
//            throw new RuntimeException(e.getMessage());
//        }
    }
    
    private byte[] announceBuffer(InetAddress address, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(ADVERTISMENT_BUFFER_LENGTH);
        buffer.put(MAGIC_NUMBER);
        buffer.put(address.getAddress());
        
        byte[] portBuffer = new byte[2];
        portBuffer[0] = (byte)((port >> 8) & 0xFF);
        portBuffer[1] = (byte)(port & 0xFF);
        buffer.put(portBuffer);
        
        return buffer.array();
    }
    
    public void announceProxy(InetAddress address, int port) throws InsufficientMoneyException, InterruptedException, ExecutionException {
        Wallet wallet = appKit.wallet();
    	byte[] advertismentBuffer = announceBuffer(address, port);
         Transaction transaction = new Transaction(wallet.getNetworkParameters());
        // TODO: For some reason bitcoinj doesn't want to complete this transaction with the min nondust amount
        // transaction.addOutput(Transaction.MIN_NONDUST_OUTPUT, new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(advertismentBuffer).build());
        transaction.addOutput(Coin.COIN, new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(advertismentBuffer).build());
        Wallet.SendRequest sendRequest = Wallet.SendRequest.forTx(transaction);
//        wallet.completeTx(sendRequest);
//        wallet.commitTx(sendRequest.tx);
        wallet.sendCoins(sendRequest);
        // peerGroup.broadcastTransaction(sendRequest.tx).get();
    }
    
    public List<RemoteProxyAddress> getProxies() throws InterruptedException, ExecutionException, BlockStoreException {
        BlockChain blockChain = appKit.chain();
        List<RemoteProxyAddress> proxies = new ArrayList<RemoteProxyAddress>();
        System.out.println("Block chain height: " + blockChain.getBestChainHeight());
        Peer peer = appKit.peerGroup().getDownloadPeer();
        StoredBlock storedBlock = blockChain.getChainHead();
        Block block = peer.getBlock(storedBlock.getHeader().getHash()).get(); 
        
        int i = 0;
        do {
            List<RemoteProxyAddress> addressesInBlock = findProxiesInBlock(block);
            proxies.addAll(addressesInBlock);
            
            i += 1;
            Sha256Hash prevBlockHash = block.getPrevBlockHash();
            block = peer.getBlock(prevBlockHash).get();
        } while (block != null && i < 20);
        
        return proxies;
    }
    
    private static List<RemoteProxyAddress> findProxiesInBlock(Block block) {
        List<RemoteProxyAddress> proxies = new ArrayList<RemoteProxyAddress>();
        List<Transaction> transactions = block.getTransactions();
        
        for (Transaction transaction : transactions) {
            for(TransactionOutput output : transaction.getOutputs()) {
                Script script = output.getScriptPubKey();
                List<ScriptChunk> chunks = script.getChunks();
                ScriptChunk chunk1 = chunks.get(0);
                ScriptChunk chunk2 = chunks.get(1);
                if (chunk1.isOpCode() && chunk1.opcode == ScriptOpCodes.OP_RETURN) {
                    byte[] prefix = Arrays.copyOf(chunk2.data, MAGIC_NUMBER.length);
                    if (Arrays.equals(prefix, MAGIC_NUMBER)) {
                        ByteBuffer announceBuffer = ByteBuffer.wrap(chunk2.data, MAGIC_NUMBER.length,
                                                                    chunk2.data.length - MAGIC_NUMBER.length);
                        byte[] addressBuffer = new byte[4];
                        announceBuffer.get(addressBuffer);
                        InetAddress address;
                        try {
                            address = InetAddress.getByAddress(addressBuffer);
                        } catch (UnknownHostException e) {
                            continue;
                        }
                        byte[] portBuffer = new byte[2];
                        announceBuffer.get(portBuffer);
                        int port = ((portBuffer[0] & 0xFF) << 8) | (portBuffer[1] & 0xFF);
                        proxies.add(new RemoteProxyAddress(address, port));
                    }
                }
            }
        }
        return proxies;
    }
}
