package uk.co.palmr.gennaker;

import java.util.ArrayList;
import java.util.List;

public class TestTopicImpl implements TestTopic {
    private long nopCallCount = 0;
    private final List<Integer> consumedItems = new ArrayList<>();
    private String lastRepeat;

    @Override
    public void nop() {
        nopCallCount++;
    }

    @Override
    public void consume(final int x) {
        consumedItems.add(x);
    }

    @Override
    public void repeat(final String str, final int n) {
        lastRepeat = str.repeat(n);
    }

    public long getNopCallCount() {
        return nopCallCount;
    }

    public List<Integer> getConsumedItems() {
        return consumedItems;
    }

    public String getLastRepeat() {
        return lastRepeat;
    }
}
