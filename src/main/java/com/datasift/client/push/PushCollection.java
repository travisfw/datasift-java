package com.datasift.client.push;

import com.datasift.client.BaseDataSiftResult;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class PushCollection extends BaseDataSiftResult implements Iterable<PushSubscription> {
    @JsonProperty
    private int count;
    @JsonProperty
    private List<PushSubscription> subscriptions;

    /**
     * @return how many subscriptions are in this collection
     */
    public int getCount() {
        return count;
    }

    /**
     * @return A list of subscriptions in this collection, will never be null but may be empty
     */
    public List<PushSubscription> getSubscriptions() {
        return listOrEmpty();
    }

    @Override
    public Iterator<PushSubscription> iterator() {
        return listOrEmpty().iterator();
    }

    private List<PushSubscription> listOrEmpty() {
        return subscriptions == null ? Collections.EMPTY_LIST : subscriptions;
    }
}
