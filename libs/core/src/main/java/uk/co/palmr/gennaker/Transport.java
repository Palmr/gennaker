package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

/// Pluggable delivery mechanism that carries encoded topic messages between
/// publishers and subscribers. Implementations decide how messages are routed
/// (in-process, Aeron IPC, etc.) but are agnostic to encoding — bytes are
/// already encoded by the time [#publish] is called.
public interface Transport {
    /// Publishes an already-encoded message on the given topic.
    ///
    /// @param topicClazz the topic interface; transports may key routing on this class identity
    /// @param message    buffer holding the encoded message starting at offset 0
    /// @param limit      number of bytes in `message` that constitute the message
    /// @param <T>        the topic interface type
    /// @param <I>        unused on this call; present for symmetry with [#subscribe]
    /// @return `true` if the message was accepted by the transport
    <T, I extends T> boolean publish(Class<T> topicClazz, DirectBuffer message, int limit);

    /// Registers a message handler to receive raw inbound messages on a topic.
    /// The handler is typically the generated subscriber proxy, which decodes
    /// the bytes and dispatches to user code. Proxy resolution is done by
    /// [Gennaker#subscribe] before this method is called — transports never
    /// need to know about codec proxies themselves.
    ///
    /// @param topicClazz the topic interface (used as a routing key)
    /// @param handler    receives encoded messages as they arrive
    void subscribe(Class<?> topicClazz, MessageHandler handler);

    /// Releases any resources held by this transport.
    void shutdown();
}
