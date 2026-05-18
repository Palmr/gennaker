package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// [Transport] decorator that records every [#publish] call before forwarding
/// it to a delegate. Intended for integration tests: wrap a [DirectTransport]
/// (or any other transport), wire the system under test through the recorder,
/// and assert on the captured traffic afterwards.
///
/// Recordings are kept as per-topic lists of encoded byte arrays, so tests in
/// a large topology can query just the topics they care about without
/// scanning unrelated traffic. Subscriptions and shutdown are forwarded
/// unchanged.
///
/// ```java
/// var recorder = new RecordingTransport(new DirectTransport());
/// var gennaker = new Gennaker(recorder);
/// // ... exercise the system ...
/// assertEquals(2, recorder.bytesFor(Ping.class).size());
/// recorder.decodeAs(Ping.class, assertingPingImpl);
/// ```
public final class RecordingTransport implements Transport {
    private final Transport delegate;
    private final Map<Class<?>, List<byte[]>> bytesByTopic = new IdentityHashMap<>();

    /// Wraps the given transport. Publishes are recorded and then forwarded;
    /// subscribes and shutdown pass through directly.
    ///
    /// @param delegate the transport to delegate to
    public RecordingTransport(final Transport delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> boolean publish(final Class<T> topicClass, final DirectBuffer message, final int limit) {
        final byte[] copy = new byte[limit];
        message.getBytes(0, copy, 0, limit);
        bytesByTopic.computeIfAbsent(topicClass, _ -> new ArrayList<>()).add(copy);
        return delegate.publish(topicClass, message, limit);
    }

    @Override
    public <T> void subscribe(final Class<T> topicClass, final MessageHandler<T> handler) {
        delegate.subscribe(topicClass, handler);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    /// Returns the raw encoded byte arrays recorded for the given topic, in
    /// publish order. Useful for regression tests that snapshot wire-format
    /// output.
    ///
    /// @param topicClass the topic to query
    /// @return an unmodifiable list of encoded message bodies in publish
    ///         order, or an empty list if nothing was published on this topic
    public List<byte[]> bytesFor(final Class<?> topicClass) {
        final List<byte[]> list = bytesByTopic.get(topicClass);
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }

    /// Returns the number of messages recorded for the given topic.
    ///
    /// @param topicClass the topic to query
    /// @return the count of recorded messages, or zero if none
    public int countFor(final Class<?> topicClass) {
        final List<byte[]> list = bytesByTopic.get(topicClass);
        return list == null ? 0 : list.size();
    }

    /// Discards recordings for every topic.
    public void clear() {
        bytesByTopic.clear();
    }

    /// Discards recordings for the given topic only.
    ///
    /// @param topicClass the topic whose recordings should be discarded
    public void clear(final Class<?> topicClass) {
        bytesByTopic.remove(topicClass);
    }

    /// Replays every recorded message for `topicClass` through the generated
    /// subscriber proxy onto `topicImplementation`. The implementation
    /// receives decoded method calls exactly as a live subscriber would,
    /// allowing assertions to be written in terms of topic methods rather
    /// than raw bytes.
    ///
    /// @param topicClass          the `@Topic`-annotated interface
    /// @param topicImplementation receives decoded calls for each recorded message
    /// @param <T>                 the topic interface type
    /// @param <I>                 a subtype of `T` implementing the topic
    public <T, I extends T> void decodeAs(final Class<T> topicClass, final I topicImplementation) {
        final List<byte[]> list = bytesByTopic.get(topicClass);
        if (list == null) {
            return;
        }
        final MessageHandler<T> handler = ClassHunter.getSubscriberProxy(topicClass, topicImplementation);
        for (final byte[] bytes : list) {
            handler.onMessage(new UnsafeBuffer(bytes), 0, bytes.length);
        }
    }
}
