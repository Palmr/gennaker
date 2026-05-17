package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

/**
 * Pluggable delivery mechanism that carries encoded topic messages between
 * publishers and subscribers. Implementations decide how messages are routed
 * (in-process, Aeron IPC, etc.) but are agnostic to encoding — bytes are
 * already encoded by the time {@link #publish} is called.
 */
public interface Transport {
    /**
     * Publishes an already-encoded message on the given topic.
     *
     * @param topicClazz the topic interface; transports may key routing on this class identity
     * @param message    buffer holding the encoded message starting at offset 0
     * @param limit      number of bytes in {@code message} that constitute the message
     * @param <T>        the topic interface type
     * @param <I>        unused on this call; present for symmetry with {@link #subscribe}
     * @return {@code true} if the message was accepted by the transport
     */
    <T, I extends T> boolean publish(Class<T> topicClazz, DirectBuffer message, int limit);

    /**
     * Registers a topic implementation to receive decoded inbound messages.
     *
     * @param topicClazz the topic interface
     * @param impl       user implementation invoked by the generated subscriber proxy
     * @param <T>        the topic interface type
     * @param <I>        the concrete implementation type
     */
    <T, I extends T> void subscribe(Class<T> topicClazz, I impl);

    /** Releases any resources held by this transport. */
    void shutdown();
}
