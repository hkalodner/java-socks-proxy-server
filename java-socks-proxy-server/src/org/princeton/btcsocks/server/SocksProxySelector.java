package org.princeton.btcsocks.server;
//package org.bitcoinj.examples;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.protocols.channels.PaymentChannelClientConnection;
import org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates;
import org.bitcoinj.protocols.channels.ValueOutOfRangeException;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.bitcoinj.core.Coin.CENT;
import static org.bitcoinj.core.Coin.COIN;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.princeton.btsocks.discovery.BittorrentDiscovery;
import org.princeton.btsocks.discovery.RemoteProxyAddress;

// Taken from https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

public class SocksProxySelector extends ProxySelector {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExamplePaymentChannelClient.class);
	private WalletAppKit appKit;
	private final Coin channelSize;
	private final ECKey myKey;
	private final NetworkParameters params;


	// Keep a reference on the previous default
	ProxySelector defsel = null;
	private int requestNum = 0;

	/*
	 * A list of proxies, indexed by their address.
	 */
	Map<SocketAddress, RemoteProxyAddress> proxies = new HashMap<SocketAddress, RemoteProxyAddress>();

	public SocksProxySelector(ProxySelector def) {
		System.out.println("Running constructor");
		// Save the previous default
		defsel = def;

		// Populate the HashMap (List of proxies)
		List<RemoteProxyAddress> proxyList = getActiveProxies();
		System.out.println("Active Proxy List:\n" + proxyList);
		for (RemoteProxyAddress proxyAddress : proxyList) {
			proxies.put(proxyAddress.address(), proxyAddress);
			System.out.println(proxyAddress);
		}


		BriefLogFormatter.init();
		channelSize = COIN;
		myKey = new ECKey();
		params = RegTestParams.get();

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
		// We now have active network connections and a fully synced wallet.
		// Add a new key which will be used for the multisig contract.
		appKit.wallet().importKey(myKey); //TODO fix: i think this makes it generate a new key every time the program is run
		appKit.wallet().allowSpendingUnconfirmedTransactions();

		System.out.println(appKit.wallet());

		waitForSufficientBalance(channelSize);

	}
	
	public List<RemoteProxyAddress> getActiveProxies() {
		try {
			BittorrentDiscovery discovery = new BittorrentDiscovery(InetAddress.getLocalHost(), 6969);
			List<RemoteProxyAddress> proxyList = discovery.getProxies();
			System.out.println(proxyList);
			return proxyList;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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

		System.out.println("Trying to select");
		System.out.println("scheme = " + uri.getScheme());
		System.out.println("host = " + uri.getHost());
		System.out.println("port = " + uri.getPort());

		/*
		 * If it's a http (or https) URL, then we use our own list.
		 */
		String protocol = uri.getScheme();
		if ("socket".equalsIgnoreCase(protocol)) {
			List<RemoteProxyAddress> test = getActiveProxies();
			System.out.println(test);
			ArrayList<Proxy> l = new ArrayList<Proxy>();
			assert proxies.size() > 0;
			int proxNum = requestNum % proxies.size();
			int i = 0;
			for (RemoteProxyAddress p : proxies.values()) {
				if (i == proxNum) {
					l.add(p.toProxy());
				}
				i++;
			}

			final String host = "127.0.0.1";//TODO Fix this-- should be defined based on the address of the proxy
			final int timeoutSecs = 15;
			final InetSocketAddress server = new InetSocketAddress(host, 4242);

			log.info("Round one ...");
			System.out.println("Before Payment:");
			System.out.println(appKit.wallet());
			try {
				openAndSend(timeoutSecs, server, host, 9);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ValueOutOfRangeException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log.info(appKit.wallet().toString());
			log.info("Stopping ...");
			System.out.println("After Payment:");
			System.out.println(appKit.wallet());
			appKit.stopAsync();
			appKit.awaitTerminated();
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
	private void openAndSend(int timeoutSecs, InetSocketAddress server, String channelID, final int times) throws IOException, ValueOutOfRangeException, InterruptedException {
		PaymentChannelClientConnection client = new PaymentChannelClientConnection(
				server, timeoutSecs, appKit.wallet(), myKey, channelSize.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB), channelID);
		System.out.println("Putting " + channelSize.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB) + " in channel");
		// Opening the channel requires talking to the server, so it's asynchronous.
		final CountDownLatch latch = new CountDownLatch(1);
		Futures.addCallback(client.getChannelOpenFuture(), new FutureCallback<PaymentChannelClientConnection>() {
			@Override
			public void onSuccess(PaymentChannelClientConnection client) {
				// By the time we get here, if the channel is new then we already made a micropayment! The reason is,
				// we are not allowed to have payment channels that pay nothing at all.
				log.info("Success! Trying to make {} micropayments. Already paid {} satoshis on this channel",
						times, client.state().getValueSpent());
				final Coin MICROPAYMENT_SIZE = CENT.divide(10);
				for (int i = 0; i < times; i++) {
					System.out.println("round:"+ i+ " of transactions");
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
					log.info("Successfully sent payment of one CENT, total remaining on channel is now {}", client.state().getValueRefunded());
				}
				// if (client.state().getValueRefunded().compareTo(MICROPAYMENT_SIZE) < 0) {
				if (true) {
					// Now tell the server we're done so they should broadcast the final transaction and refund us what's
					// left. If we never do this then eventually the server will time out and do it anyway and if the
					// server goes away for longer, then eventually WE will time out and the refund tx will get broadcast
					// by ourselves.
					System.out.println("settling channel and closing");
					log.info("Settling channel for good");
					client.settle();
				} else {
					// Just unplug from the server but leave the channel open so it can resume later.
					System.out.println("close w/o settling. Not clean");
					client.disconnectWithoutSettlement();
				}
				latch.countDown();
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.error("Failed to open connection", throwable);
				latch.countDown();
			}
		}, Threading.USER_THREAD);
		latch.await();
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