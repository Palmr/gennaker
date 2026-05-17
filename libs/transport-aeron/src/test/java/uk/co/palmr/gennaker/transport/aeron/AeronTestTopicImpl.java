package uk.co.palmr.gennaker.transport.aeron;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

final class AeronTestTopicImpl implements AeronTestTopic {
    private final AtomicLong nopCallCount = new AtomicLong();
    private final List<Integer> consumedItems = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> lastRepeat = new AtomicReference<>();

    @Override
    public void nop() {
        nopCallCount.incrementAndGet();
    }

    @Override
    public void consume(final int x) {
        consumedItems.add(x);
    }

    @Override
    public void repeat(final String str, final int n) {
        lastRepeat.set(str.repeat(n));
    }

    long getNopCallCount() {
        return nopCallCount.get();
    }

    List<Integer> getConsumedItems() {
        return consumedItems;
    }

    String getLastRepeat() {
        return lastRepeat.get();
    }
}
