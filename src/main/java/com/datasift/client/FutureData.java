package com.datasift.client;

import org.cliffc.high_scale_lib.NonBlockingHashSet;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class FutureData<T> {
    protected NonBlockingHashSet<FutureResponse<T>> listeners = new NonBlockingHashSet<FutureResponse<T>>();
    private T data;

    /**
     * Wraps any object in a {@link FutureData} instance
     * <p/>
     * Intended use is to enable any object obtained without a future to be passed to API methods.
     * This allows API methods to accept FutureData objects and alleviates the need for a user to add
     * many callbacks and instead just pass futures around as if they were already obtained values
     *
     * @param obj the object to wrap
     * @param <A> the type of the object
     * @return a future that will fire onData events for the given object
     */
    public static <A> FutureData<A> wrap(A obj) {
        FutureData<A> future = new FutureData<A>();
        future.data = obj;
        return future;
    }

    /**
     * Invoked when a response is received and this future data is now available for use/processing
     *
     * @param data the data received or an object wrapping said data
     */
    public void received(T data) {
        this.data = data;
        notifyListeners();
        doNotify();
    }

    /**
     * Adds an event listener that is notified when data is received
     *
     * @param response the future which should listen for a response
     * @return this to enable chaining
     */
    public FutureData<T> onData(FutureResponse<T> response) {
        this.listeners.add(response);
        //if we already received a response then notify straight away
        if (this.data != null) {
            response.apply(this.data);
            doNotify();
        }
        return this;
    }

    protected void notifyListeners() {
        for (FutureResponse<T> res : listeners) {
            res.apply(data);
        }
    }

    protected void doNotify() {
        synchronized (this) {
            notify();
        }
    }

    /**
     * Forces the client to wait until a response is received before returning
     *
     * @return a result instance
     * @throws InterruptedException if an interrupt is thrown it is possible that a response isn't available yet
     *                              the user must check to ensure null isn't returned
     */
    public T sync() throws InterruptedException {
        synchronized (this) {
            wait();
        }
        return data;
    }
}
