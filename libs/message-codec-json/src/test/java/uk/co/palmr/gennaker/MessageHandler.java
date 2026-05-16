package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

/**
 * Test-only stand-in matching the {@code :libs:core} MessageHandler interface.
 */
@FunctionalInterface
public interface MessageHandler {
    void onMessage(DirectBuffer buffer, int offset, int length);
}
