package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

public interface Transport {
    <T, I extends T> boolean publish(Class<T> topicClazz, DirectBuffer message, int limit);
    <T, I extends T> void subscribe(Class<T> topicClazz, I impl);

    void shutdown();
}
