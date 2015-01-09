package org.princeton.btcsocks.server;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.protocols.channels.PaymentChannelCloseException;
import org.bitcoinj.protocols.channels.PaymentChannelServerListener;
import org.bitcoinj.protocols.channels.PaymentChannelServerState;
import org.bitcoinj.protocols.channels.ServerConnectionEventHandler;
import org.bitcoinj.protocols.channels.StoredPaymentChannelServerStates;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

// Taken from https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

public class SocksProxySelectorServer extends ProxySelector implements PaymentChannelServerListener.HandlerFactory {
	
	// Keep a reference on the previous default
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExamplePaymentChannelServer.class);
	ProxySelector defsel = null;
	
	private WalletAppKit appKit;

	public SocksProxySelectorServer(ProxySelector def) {
		System.out.println("Running constructor");
		// Save the previous default
		defsel = def;
		
		NetworkParameters params = RegTestParams.get();

        // Bring up all the objects we need, create/load a wallet, sync the chain, etc. We override WalletAppKit so we
        // can customize it by adding the extension objects - we have to do this before the wallet file is loaded so
        // the plugin that knows how to parse all the additional data is present during the load.
		appKit = new WalletAppKit(params, new File("."), "payment_channel_example_server") {
            @Override
            protected List<WalletExtension> provideWalletExtensions() {
                // The StoredPaymentChannelClientStates object is responsible for, amongst other things, broadcasting
                // the refund transaction if its lock time has expired. It also persists channels so we can resume them
                // after a restart.
                return ImmutableList.<WalletExtension>of(new StoredPaymentChannelServerStates(null));
            }
        };
        appKit.connectToLocalHost();
        appKit.startAsync();
        appKit.awaitRunning();
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

		System.out.println("Trying to select");
		System.out.println("scheme = " + uri.getScheme());
		System.out.println("host = " + uri.getHost());
		System.out.println("port = " + uri.getPort());

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
		if (defsel != null)
			defsel.connectFailed(uri, sa, ioe);
	}

	@Override
	public ServerConnectionEventHandler onNewConnection(final SocketAddress clientAddress) {
        // Each connection needs a handler which is informed when that payment channel gets adjusted. Here we just log
        // things. In a real app this object would be connected to some business logic.
        System.out.println("connection established");
        return new ServerConnectionEventHandler() {
            @Override
            public void channelOpen(Sha256Hash channelId) {
                System.out.println("channel opened!");
                log.info("Channel open for {}: {}.", clientAddress, channelId);

                // Try to get the state object from the stored state set in our wallet
                System.out.println("here1");
                StoredPaymentChannelServerStates serverState = (StoredPaymentChannelServerStates) appKit.wallet().getExtensions().get(StoredPaymentChannelServerStates.class.getName());
                
                PaymentChannelServerState state = null;
                try {
                    System.out.println("here2");
                    state = serverState.getChannel(channelId).getOrCreateState(appKit.wallet(), appKit.peerGroup());
                } catch (VerificationException e) {
                    // This indicates corrupted data, and since the channel was just opened, cannot happen
                    System.out.println("was an error opening channel");
                    throw new RuntimeException(e);
                }
                System.out.println("here3");
                log.info("   with a maximum value of {}, expiring at UNIX timestamp {}.",
                        // The channel's maximum value is the value of the multisig contract which locks in some
                        // amount of money to the channel
                        state.getMultisigContract().getOutput(0).getValue(),
                        // The channel expires at some offset from when the client's refund transaction becomes
                        // spendable.
                        state.getRefundTransactionUnlockTime() + StoredPaymentChannelServerStates.CHANNEL_EXPIRE_OFFSET);

                System.out.println("here4");
            }

            @Override
            public ListenableFuture<ByteString> paymentIncrease(Coin by, Coin to, ByteString info) {
                log.info("Client {} paid increased payment by {} for a total of " + to.toString(), clientAddress, by);
                return null;
            }

            @Override
            public void channelClosed(PaymentChannelCloseException.CloseReason reason) {
                log.info("Client {} closed channel for reason {}", clientAddress, reason);
            }
        };
    }
}