package org.princeton.btsocks.discovery;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.protocols.channels.StoredPaymentChannelServerStates;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class TestBitcoinDiscovery {

	public static void main(String[] args) throws BlockStoreException, UnknownHostException, InsufficientMoneyException, InterruptedException, ExecutionException {
//		NetworkParameters networkParams = RegTestParams.get();
//        BlockStore store = new MemoryBlockStore(networkParams);
//		Wallet wallet = new Wallet(networkParams);
//		BlockChain chain = new BlockChain(networkParams, wallet, store);
//		PeerGroup peerGroup = new PeerGroup(networkParams, chain);
//		peerGroup.addWallet(wallet);
//		peerGroup.startAndWait();
	    
	    NetworkParameters params = RegTestParams.get();

	    WalletAppKit appKit = new WalletAppKit(params, new File("."), "discovery_wallet");
	    appKit.connectToLocalHost();
	    appKit.startAsync();
	    appKit.awaitRunning();
	        
		ECKey myKey = new ECKey();
		appKit.wallet().importKey(myKey);
//		waitForSufficientBalance(appKit.wallet(), myKey, appKit.params(), Coin.valueOf(10, 0));
//		System.out.println("Money received.");
		
		BitcoinDiscovery discovery = new BitcoinDiscovery(appKit);
//		discovery.announceProxy(InetAddress.getLocalHost(), 8080);
//		Thread.sleep(20000);
		
		List<RemoteProxyAddress> addresses = discovery.getProxies(); 
		System.out.println("getProxies returned " + addresses.size() + " proxies.");

		
		appKit.stop();
		appKit.awaitTerminated();
	}
	
	private static void waitForSufficientBalance(Wallet wallet, ECKey myKey, NetworkParameters params, Coin amount) {
	    // Not enough money in the wallet.
	    Coin amountPlusFee = amount.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB);
	    ListenableFuture<Coin> balanceFuture = wallet.getBalanceFuture(amountPlusFee, Wallet.BalanceType.AVAILABLE);
	    if (!balanceFuture.isDone()) {
	        System.out.println("Please send " + amountPlusFee.toFriendlyString() +
	                " BTC to " + myKey.toAddress(params));
	        Futures.getUnchecked(balanceFuture);  // Wait.
	    }
	}
}