package org.princeton.btsocks.discovery;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;

public class BitcoinDiscovery {
    
    public final byte[] MAGIC_NUMBER = {23, -112, 88, -55};
    public final int ADVERTISMENT_BUFFER_LENGTH = 40;
    
    private final Wallet wallet;
    private final NetworkParameters networkParams;
    private final BlockChain blockChain;
    private final PeerGroup peerGroup;
    
    /**
     * Construct a new BitcoinDiscovery object that will use the given wallet.
     * @param wallet wallet to burn bitcoins from to advertise address on the block chain.
     */
    public BitcoinDiscovery(Wallet wallet) {
        this.wallet = wallet;
        
        networkParams = RegTestParams.get();
        BlockStore store = new MemoryBlockStore(networkParams);

        try {
            blockChain = new BlockChain(networkParams, wallet, store);
        } catch (BlockStoreException e) {
            throw new RuntimeException(e.getMessage());
        }
        peerGroup = new PeerGroup(networkParams, blockChain);
        peerGroup.addWallet(wallet);
        peerGroup.startAndWait();
    }
    
    private byte[] announceBuffer(InetAddress address, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(ADVERTISMENT_BUFFER_LENGTH);
        buffer.put(MAGIC_NUMBER);
        buffer.put(address.getAddress());
        buffer.putInt(port);
        return buffer.array();
    }
    
    public void announceProxy(InetAddress address, int port) throws InsufficientMoneyException, InterruptedException, ExecutionException {
    	byte[] advertismentBuffer = announceBuffer(address, port);
        Transaction transaction = new Transaction(networkParams);
        transaction.addOutput(Transaction.MIN_NONDUST_OUTPUT, new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(advertismentBuffer).build());
        Wallet.SendRequest sendRequest = Wallet.SendRequest.forTx(transaction);
        wallet.completeTx(sendRequest);
        wallet.commitTx(sendRequest.tx);
        peerGroup.broadcastTransaction(sendRequest.tx).get();
    }
    
    
    public List<RemoteProxyAddress> getProxies() throws BlockStoreException {
    	List<RemoteProxyAddress> proxies = new ArrayList<RemoteProxyAddress>();
    	StoredBlock storedBlock = blockChain.getBlockStore().getChainHead();
    	Block block = storedBlock.getHeader();
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
    					System.out.println("Found announcement.");
    				}
    			}
    		}
    	}
        throw new RuntimeException("Feature not yet implemented.");
    }
    
}
