package uk.co.palmr.gennaker;


import org.agrona.DirectBuffer;

@FunctionalInterface
public interface MessageHandler {
    void onMessage(DirectBuffer buffer, int offset, int length);
}
