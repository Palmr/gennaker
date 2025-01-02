package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

public interface Transport {
    <T> boolean publish(Class<T> topicClass, DirectBuffer message, int limit);
    <T> void subscribe(Class<T> topicClass, Object impl);
}
