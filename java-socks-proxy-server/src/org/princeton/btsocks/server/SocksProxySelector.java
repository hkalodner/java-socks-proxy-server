package org.princeton.btsocks.server;
//package org.bitcoinj.examples;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.protocols.channels.PaymentChannelClientConnection;
import org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates;
import org.bitcoinj.protocols.channels.ValueOutOfRangeException;
import org.bitcoinj.utils.BriefLogFormatter;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import org.princeton.btsocks.discovery.Discovery;
import org.princeton.btsocks.discovery.RemoteProxyAddress;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.bitcoinj.core.Coin.CENT;
import static org.bitcoinj.core.Coin.COIN;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Taken from https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

public class SocksProxySelector extends ProxySelector {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(SocksProxySelector.class);
	private WalletAppKit appKit;
	private final Coin channelSize;
	private final ECKey myKey;
	private final NetworkParameters params;
	private final Discovery discovery;


	// Keep a reference on the previous default
	ProxySelector defsel = null;
	private int requestNum = 0;

	/*
	 * A list of proxies, indexed by their address.
	 */
	Map<SocketAddress, RemoteProxyAddress> proxies = new HashMap<SocketAddress, RemoteProxyAddress>();

	public SocksProxySelector(ProxySelector def, Discovery discovery) {
		// Save the previous default
		defsel = def;

		BriefLogFormatter.init();
		channelSize = COIN;
		myKey = new ECKey();
		params = RegTestParams.get();
		this.discovery = discovery;

		appKit = new WalletAppKit(params, new File("."), "payment_channel_example_client") {
			@Override
			protected List<WalletExtension> provideWalletExtensions() {
				// The StoredPaymentChannelClientStates object is responsible for, amongst other things, broadcasting
				// the refund transaction if its lock time has expired. It also persists channels so we can resume them
				// after a restart.
				// We should not send a PeerGroup in the StoredPaymentChannelClientStates constructor
				// since WalletAppKit will find it for us.
				return ImmutableList.<WalletExtension>of(new StoredPaymentChannelClientStates(null));
			}
		};

		appKit.connectToLocalHost();
		appKit.startAsync();
		appKit.awaitRunning();
//		assert appKit.state() == AbstractIdleService.Service.State;
		// We now have active network connections and a fully synced wallet.
		// Add a new key which will be used for the multisig contract.
		appKit.wallet().importKey(myKey); //TODO fix: i think this makes it generate a new key every time the program is run
		appKit.wallet().allowSpendingUnconfirmedTransactions();

		//System.out.println(appKit.wallet());

		waitForSufficientBalance(channelSize);
		
		// Populate the HashMap (List of proxies)
		List<RemoteProxyAddress> proxyList = getActiveProxies();

		
		
		for (RemoteProxyAddress proxyAddress : proxyList) {
			Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress.address());
			Socket socket = new Socket(proxy);
			try {
				System.out.println("Testing out proxy " + proxyAddress.address());
				InetSocketAddress final_addr = new InetSocketAddress("74.125.236.195", 80);
				socket.connect(final_addr);
			} catch (IOException e1) {
				System.out.println("Proxy failed");
				continue;
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (proxyAddress.paymentChannel() == null) {
				try {
					final int timeoutSecs = 15;
					proxyAddress.setPaymentChannel(openPaymentChannel(timeoutSecs, proxyAddress));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ValueOutOfRangeException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			proxies.put(proxyAddress.address(), proxyAddress);
			System.out.println("Set up proxy: " + proxyAddress);
		}
	}
	
	public List<RemoteProxyAddress> getActiveProxies() {
		List<RemoteProxyAddress> proxyList = discovery.getProxies();
		return proxyList;
	}

	/*
	 * This is the method that the handlers will call. Returns a List of proxy.
	 */
	@Override
	public java.util.List<Proxy> select(URI uri) {
		// Let's stick to the specs.
		if (uri == null) {
			throw new IllegalArgumentException("URI can't be null.");
		}

		requestNum++;
		
		/*
		 * If it's a http (or https) URL, then we use our own list.
		 */
		String protocol = uri.getScheme();
		if ("socket".equalsIgnoreCase(protocol)) {
			ArrayList<Proxy> l = new ArrayList<Proxy>();
			assert proxies.size() > 0;
			int proxNum = requestNum % proxies.size();
			int i = 0;
			for (RemoteProxyAddress p : proxies.values()) {
				if (i == proxNum) {
					l.add(p.toProxy());
					sendPayment(p.paymentChannel());
				}
				i++;
			}

			return l;
		}

		System.out.println("Going with default");
		/*
		 * Not HTTP or HTTPS (could be SOCKS or FTP) defer to the default
		 * selector.
		 */
		if (defsel != null) {
			return defsel.select(uri);
		} else {
			ArrayList<Proxy> l = new ArrayList<Proxy>();
			l.add(Proxy.NO_PROXY);
			return l;
		}
	}

	/*
	 * Method called by the handlers when it failed to connect to one of the
	 * proxies returned by select().
	 */
	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		// Let's stick to the specs again.
		if (uri == null || sa == null || ioe == null) {
			throw new IllegalArgumentException("Arguments can't be null.");
		}

		/*
		 * Let's lookup for the proxy
		 */
		RemoteProxyAddress p = proxies.get(sa);
		if (p != null) {
			/*
			 * It's one of ours, if it failed more than 3 times let's remove it
			 * from the list.
			 */
			if (p.failed() >= 3)
				proxies.remove(sa);
		} else {
			/*
			 * Not one of ours, let's delegate to the default.
			 */
			if (defsel != null)
				defsel.connectFailed(uri, sa, ioe);
		}
	}
	
	private PaymentChannelClientConnection openPaymentChannel(int timeoutSecs, RemoteProxyAddress proxyServer) throws InterruptedException, IOException, ValueOutOfRangeException, ExecutionException {
		InetSocketAddress server = new InetSocketAddress(proxyServer.address().getAddress(), 4242);
		
		PaymentChannelClientConnection client = new PaymentChannelClientConnection(
				server, timeoutSecs, appKit.wallet(), myKey, channelSize.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB), proxyServer.address().getHostName());
		// Opening the channel requires talking to the server, so it's asynchronous.

		return client.getChannelOpenFuture().get();
	}
	
	private void sendPayment(PaymentChannelClientConnection client) {
		System.out.println("Putting " + channelSize.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB) + " in channel");
		System.out.println("Channel now has " + client.state().getValueSpent() + " sent and " + client.state().getValueRefunded() + " refunded");
		
		final Coin MICROPAYMENT_SIZE = CENT.divide(10);
		try {
			// Wait because the act of making a micropayment is async, and we're not allowed to overlap.
			// This callback is running on the user thread (see the last lines in openAndSend) so it's safe
			// for us to block here: if we didn't select the right thread, we'd end up blocking the payment
			// channels thread and would deadlock.
			Uninterruptibles.getUninterruptibly(client.incrementPayment(MICROPAYMENT_SIZE));
		} catch (ValueOutOfRangeException e) {
			log.error("Failed to increment payment by a CENT, remaining value is {}", client.state().getValueRefunded());
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			log.error("Failed to increment payment", e);
			throw new RuntimeException(e);
		}
	}
	
	private void waitForSufficientBalance(Coin amount) {
		// Not enough money in the wallet.
		Coin amountPlusFee = amount.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB);
		// ESTIMATED because we don't really need to wait for confirmation.
		ListenableFuture<Coin> balanceFuture = appKit.wallet().getBalanceFuture(amountPlusFee, Wallet.BalanceType.ESTIMATED);
		if (!balanceFuture.isDone()) {
			System.out.println("Please send " + amountPlusFee.toFriendlyString() +
					" to " + myKey.toAddress(params));
			Futures.getUnchecked(balanceFuture);
		}
	}
}