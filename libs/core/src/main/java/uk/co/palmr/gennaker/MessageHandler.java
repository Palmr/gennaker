package uk.co.palmr.gennaker;


import org.agrona.DirectBuffer;

/// Receives raw encoded messages from a transport. Generated subscriber
/// proxies implement this interface to decode the bytes and dispatch to user
/// code; transports invoke [#onMessage] once per inbound message.
@FunctionalInterface
public interface MessageHandler {
    /// Called by the transport when an encoded message arrives.
    ///
    /// @param buffer the buffer containing the encoded message
    /// @param offset start offset of the message within `buffer`
    /// @param length number of bytes that make up the message
    void onMessage(DirectBuffer buffer, int offset, int length);
}
