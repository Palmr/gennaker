package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// [Transport] that delivers messages in-process by invoking each subscribed
/// handler directly on the publishing thread. Useful for tests and single-JVM
/// topologies where no inter-process delivery is needed.
public final class DirectTransport implements Transport {
    private final Map<Class<?>, List<MessageHandler<?>>> handlersByTopic = new IdentityHashMap<>();

    /// Creates a new in-process transport with no subscribers registered.
    public DirectTransport() {
    }

    @Override
    public <T> boolean publish(final Class<T> topicClass, final DirectBuffer message, final int limit) {
        handlersByTopic.get(topicClass).forEach(handler -> handler.onMessage(message, 0, limit));
        return true;
    }

    @Override
    public <T> void subscribe(final Class<T> topicClass, final MessageHandler<T> handler) {
        handlersByTopic.computeIfAbsent(topicClass, _ -> new ArrayList<>()).add(handler);
    }

    @Override
    public void shutdown() {
        handlersByTopic.clear();
    }
}
