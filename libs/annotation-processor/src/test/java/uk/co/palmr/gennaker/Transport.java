package uk.co.palmr.gennaker;

import org.agrona.DirectBuffer;

/**
 * Test-only stand-in matching the {@code :libs:core} Transport interface so
 * generated proxies compile in this module's tests without pulling in core.
 */
public interface Transport {
    <T, I extends T> boolean publish(Class<T> topicClazz, DirectBuffer message, int limit);
    <T, I extends T> void subscribe(Class<T> topicClazz, I impl);
    void shutdown();
}
