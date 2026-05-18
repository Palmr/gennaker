package uk.co.palmr.gennaker;


import org.agrona.DirectBuffer;

/// Receives raw encoded messages from a transport. Generated subscriber
/// proxies implement this interface to decode the bytes and dispatch to user
/// code; transports invoke [#onMessage] once per inbound message. The type
/// parameter marks the topic interface this handler is bound to, letting the
/// transport SPI keep handler/topic pairings type-safe at compile time.
///
/// @param <T> the topic interface this handler is bound to
@FunctionalInterface
public interface MessageHandler<T> {
    /// Called by the transport when an encoded message arrives.
    ///
    /// @param buffer the buffer containing the encoded message
    /// @param offset start offset of the message within `buffer`
    /// @param length number of bytes that make up the message
    void onMessage(DirectBuffer buffer, int offset, int length);
}
