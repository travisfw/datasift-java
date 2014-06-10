package com.datasift.client.stream;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public interface ErrorListener {
    void exceptionCaught(Throwable t);
}
