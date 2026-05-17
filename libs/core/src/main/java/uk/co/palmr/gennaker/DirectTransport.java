package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Transport} that delivers messages in-process by invoking each
 * subscriber's generated proxy directly on the publishing thread. Useful for
 * tests and single-JVM topologies where no inter-process delivery is needed.
 */
public final class DirectTransport implements Transport {
    private final Map<Class<?>, List<Object>> subscribersByTopic = new IdentityHashMap<>();

    /** Creates a new in-process transport with no subscribers registered. */
    public DirectTransport() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, I extends T> boolean publish(final Class<T> topicClass, final DirectBuffer message, final int limit) {
        subscribersByTopic.get(topicClass).forEach(subscriber -> {
            ClassHunter.getSubscriberProxy(topicClass, (I) subscriber).onMessage(message, 0, limit);
        });
        return true;
    }

    @Override
    public <T, I extends T> void subscribe(final Class<T> topicClass, final I impl) {
        subscribersByTopic.computeIfAbsent(topicClass, c -> new ArrayList<>()).add(impl);
    }

    @Override
    public void shutdown() {
        subscribersByTopic.clear();
    }
}
