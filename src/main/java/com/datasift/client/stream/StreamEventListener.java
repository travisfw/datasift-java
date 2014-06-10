package com.datasift.client.stream;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public interface StreamEventListener {

    default void streamClosed() {
    }

    default void streamOpened() {
    }

    void onDelete(DeletedInteraction di);

}
