package com.datasift.client.push;

import com.datasift.client.BaseDataSiftResult;
import com.datasift.client.push.pull.LastInteraction;
import com.datasift.client.stream.Interaction;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class PulledInteractions extends BaseDataSiftResult implements Iterable<Interaction> {

    protected boolean pulling = true;
    protected LinkedBlockingQueue<Interaction> queue = new LinkedBlockingQueue<>();

    public void setPulling(boolean pulling) {
        this.pulling = pulling;
    }

    public void stopPulling() {
        setPulling(false);
    }

    public boolean isPulling() {
        return pulling;
    }

    /**
     * {@link LinkedBlockingQueue#take() }
     * @return the next Interaction
     * @throws InterruptedException 
     */
    public Interaction take() throws InterruptedException {
        return queue.take();
    }

    /**
     * {@link LinkedBlockingQueue#poll(long, java.util.concurrent.TimeUnit) }
     * @param upto
     * @param unit
     * @return
     * @throws InterruptedException 
     */
    public Interaction take(long upto, TimeUnit unit) throws InterruptedException {
        return queue.poll(upto, unit);
    }

    @Override
    public Iterator<Interaction> iterator() {
        return queue.iterator();
    }

    protected void add(Interaction interaction) {
        if (interaction != null)
            queue.add(interaction);
    }

    /**
     * has next if there is another item in the queue and it isn't the
     * {@link LastInteraction last interaction}
     * @return 
     */
    public boolean hasNext() {
        Interaction next = queue.peek();
        return next != null && next != LastInteraction.INSTANCE;
    }
}
