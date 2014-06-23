package com.datasift.client.stream;

import com.datasift.client.DataSiftClient;
import com.datasift.client.DataSiftConfig;
import com.datasift.client.core.Stream;
import io.higgs.ws.client.WebSocketClient;
import io.higgs.ws.client.WebSocketEventListener;
import io.higgs.ws.client.WebSocketMessage;
import io.higgs.ws.client.WebSocketStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class StreamingData implements WebSocketEventListener {
    protected URI endpoint;
    protected WebSocketStream liveStream;
    protected DataSiftConfig config;
    protected Map<Stream, StreamSubscription> subscriptions = new NonBlockingHashMap<>();
    protected ErrorListener errorListener;
    protected StreamEventListener streamEventListener;
    protected boolean connected;
    /** Added to in {@link StreamingData#subscribe(com.datasift.client.stream.StreamSubscription)}. */
    protected Set<StreamSubscription> unsentSubscriptions = new NonBlockingHashSet<>();
    protected short MAX_TIMEOUT = 320, currentTimeout = 1;
    protected DateTime lastSeen;
    protected static Set<StreamingData> streams = new NonBlockingHashSet<>();
    public static boolean detectDeadConnection = true;
    public static int CONNECTION_TIMEOUT_LIMIT = 65;
    public static int CONNECTION_TIMEOUT = 65;

    static {
        //DS produces some fairly big websocket frames. usually 1 or 2 MB max but set to 20 to be sure
        WebSocketClient.maxFramePayloadLength = 20971520; //20 MB
        new Thread(new Runnable() {
            public void run() {
                while (detectDeadConnection) {
                    long now = DateTime.now().getMillis();
                    for (StreamingData data : streams) {
                        if (data.lastSeen != null) {
                            if (now - data.lastSeen.getMillis() >=
                                    TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT_LIMIT)) {
                                data.closeAndReconnect();
                            }
                        }
                    }
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));
                    } catch (InterruptedException e) {
                        LoggerFactory.getLogger(getClass()).info("Interrupted while waiting to check conn");
                    }
                }
            }
        }).start();
    }

    protected Logger log = LoggerFactory.getLogger(getClass());

    public StreamingData(DataSiftConfig config) {
        try {
            endpoint = new URI(String.format(
                    (config.isSslEnabled() ? "wss" : "ws") +
                            "://websocket.datasift.com:" + config.port() + "/multi?username=%s&api_key=%s",
                    config.getUsername(), config.getApiKey()));
        } catch (URISyntaxException e) {
            log.error("Unable to create endpoint URL", e);
        }
        this.config = config;
        streams.add(this);
    }

    @Override
    public synchronized void onConnect(ChannelHandlerContext ctx) {
        streamEventListener.streamOpened();
        connected = true;
        pushUnsentSubscriptions();
    }

    @Override
    public synchronized void onClose(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
        closeAndReconnect();
    }

    private void closeAndReconnect() {
        streamEventListener.streamClosed();
        synchronized (StreamingData.this) {
            connected = false;
        }
        if (config.isAutoReconnect() &&
                TimeUnit.SECONDS.toMillis(currentTimeout) <= TimeUnit.SECONDS.toMillis(MAX_TIMEOUT)) {
            currentTimeout *= 2;
        }
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(currentTimeout));
        } catch (InterruptedException ignored) {
            log.info("Sleep interrupted, reconnecting");
        }
        liveStream = null;
        //re-subscribe
        unsentSubscriptions.addAll(subscriptions.values());
        connect();
    }

    @Override
    public void onPing(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        //only on ping or message should we reset the timeout
        currentTimeout = 1;
        lastSeen = DateTime.now();
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketMessage msg) {
        currentTimeout = 1;
        lastSeen = DateTime.now();
        try {
            MultiStreamInteraction mi =
                    DataSiftClient.MAPPER.readValue(msg.data(), MultiStreamInteraction.class);
            if (mi.isDataSiftMessage()) {
                fireMessage(new DataSiftMessage(mi));
            } else {
                if (mi.getData().get("deleted") != null) {
                    streamEventListener.onDelete(new DeletedInteraction(mi));
                } else {
                    fireInteraction(mi.getHash(), new Interaction(mi.getData()));
                }
            }
        } catch (IOException e) {
            fireError(e);  //unlikely but possible
        }
    }

    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause, FullHttpResponse response) {
        if (cause != null) {
            fireError(cause);
        }
    }

    protected void connect() {
        // test before locking to avoid blocking
        if (liveStream != null)
            return;
        synchronized (this) {
            // test again with a lock
            if (liveStream != null)
                return;
            liveStream = WebSocketClient.connect(endpoint, false, config.sslProtocols());
            liveStream.subscribe(this);
        }
    }

    protected void fireInteraction(String hash, Interaction interaction) {
        for (Map.Entry<Stream, StreamSubscription> e : subscriptions.entrySet()) {
            if (e.getKey().isSameAs(hash)) {
                e.getValue().onMessage(interaction);
            }
        }
    }

    protected void fireMessage(DataSiftMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message can't be null!");
        }
        for (Map.Entry<Stream, StreamSubscription> e : subscriptions.entrySet()) {
            if (message.hashHashes()) {
                //if we have hashes then only subscriptions for each hash should be notified
                for (Stream hash : message.hashes()) {
                    if (e.getKey().isSameAs(hash)) {
                        e.getValue().onDataSiftLogMessage(message);
                    }
                }
            } else {
                //otherwise let everyone know
                e.getValue().onDataSiftLogMessage(message);
            }
        }
    }

    protected void fireError(Throwable e) {
        if (e == null) {
            throw new IllegalArgumentException("Error can't be null!");
        }
        errorListener.exceptionCaught(e);
    }

    /**
     * Subscribes a callback to listen for exceptions that may occur during streaming.
     * When exceptions occur it is unlikely we'll know which stream/subscription caused the exception
     * so instead of notifying all stream subscribers of the same exception this provides a way to list
     * the error  just once
     *
     * @param listener an error callback
     */
    public StreamingData onError(ErrorListener listener) {
        this.errorListener = listener;
        return this;
    }

    public void onStreamEvent(StreamEventListener streamEventListener) {
        this.streamEventListener = streamEventListener;
    }

    /**
     * like {@link StreamingData#subscribe(com.datasift.client.stream.StreamSubscription, io.netty.util.concurrent.GenericFutureListener) }
     * if {@code null} was passed in for the netty listener.
     * @param subscription
     * @return this
     */
    public StreamingData subscribe(final StreamSubscription subscription) {
        this.subscribe(subscription, null);
        return this;
    }

    /**
     * Checks that {@link StreamingData#onError(com.datasift.client.stream.ErrorListener)}
     * and {@link StreamingData#onStreamEvent(com.datasift.client.stream.StreamEventListener)}
     * have both been called, then connects ({@link StreamingData#connect()}) and
     * pushes unsent subscriptions ({@link StreamingData#unsentSubscriptions}).
     * @param subscription
     * @param nettyListener handle the completion of the subscription event (may post multiple times)
     * @return this
     */
    public StreamingData subscribe(final StreamSubscription subscription, GenericFutureListener<Future<Void>> nettyListener) {
        if (errorListener == null) {
            throw new IllegalStateException("You must call listen before subscribing to streams otherwise you'll miss" +
                    " any exceptions that may occur");
        }
        if (streamEventListener == null) {
            throw new IllegalStateException("You must call onStreamEvent before subscribing to streams otherwise " +
                    "you'll miss delete messages, which you are required to handle");
        }
        connect();
        unsentSubscriptions.add(subscription);
        if (connected) {
            pushUnsentSubscriptions(nettyListener);
        }
        return this;
    }

    /**
     * like {@link #pushUnsentSubscriptions(io.netty.util.concurrent.GenericFutureListener) }
     * if null is passed in for the netty listener.
     */
    protected void pushUnsentSubscriptions() {
        pushUnsentSubscriptions(null);
    }
    protected void pushUnsentSubscriptions(GenericFutureListener<Future<Void>> nettyListener) {
        for (final StreamSubscription subscription : unsentSubscriptions) {
            if (!connected || liveStream.channel() == null || !liveStream.channel().isActive()) {
                synchronized (this) {
                    connected = false;
                }
                break;
            }
            subscriptions.put(subscription.getStream(), subscription);
            ChannelFuture cfut = liveStream.send("{\"action\":\"subscribe\",\"hash\":\"" + subscription.getStream().hash() + "\"}");
            cfut.addListener(new GenericFutureListener<Future<Void>>() {
                public void operationComplete(Future<Void> future) throws Exception {
                    if (future.isSuccess()) {
                        unsentSubscriptions.remove(subscription);
                    } else {
                        fireError(future.cause());
                        subscribe(subscription);
                    }
                }
            });
            if (nettyListener != null)
                cfut.addListener(nettyListener);
        }
        if (!connected) {
            connect();
        }
    }

    public ChannelFuture unsubscribe(Stream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream can't be null");
        }
        if (stream.hash() == null || stream.hash().length() != Stream.HASH_LENGTH) {
            throw new IllegalArgumentException("Invalid stream subscription request, no hash available");
        }
        connect();
        ChannelFuture cf = liveStream.send(" { \"action\" : \"unsubscribe\" , \"hash\": \"" + stream.hash() + "\"}");
        subscriptions.remove(stream);
        return cf;
    }

    /**
     * Look through all active subscriptions for a hash that matches (linear scan).
     * Because {@link Stream#equals(java.lang.Object) Stream's equals method} considers a timestamp,
     * it's possible for there to be more than one subscription to the same stream. If for some reason
     * you have more than one subscription to the same stream hash, this will only return one of them.
     * @param hash the stream hash or null if not found
     * @return the first StreamSubscription found that matches
     */
    public StreamSubscription getSubscriptionByHash(String hash) {
        if (hash == null) return null;
        for (Map.Entry<Stream, StreamSubscription> i : subscriptions.entrySet())
            if (hash.equals(i.getKey()))
                return i.getValue();
        return null;
    }

    /**
     * @return a set of {@link Stream streams} that are subscribed to
     */
    public Set<Stream> getStreams() {
        return subscriptions.keySet();
    }

    public int getNumberOfSubscriptions() {
        return subscriptions.size();
    }
}
