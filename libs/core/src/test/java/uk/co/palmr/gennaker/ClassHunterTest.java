package uk.co.palmr.gennaker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassHunterTest {

    /** Plain interface, NOT annotated with @Topic — no proxy will be generated. */
    interface UngeneratedTopic {
        void doSomething();
    }

    @Test
    void publisherProxyShouldFailWithGuidanceWhenNoProxyClassExists() {
        final GennakerException ex = assertThrows(GennakerException.class,
                () -> ClassHunter.getPublisherProxy(UngeneratedTopic.class, new DirectTransport()));

        final String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("__pub_proxy"), "should name the missing class: " + msg);
        assertTrue(msg.contains("@Topic"), "should mention @Topic: " + msg);
        assertInstanceOf(ClassNotFoundException.class, ex.getCause());
    }

    @Test
    void subscriberProxyShouldFailWithGuidanceWhenNoProxyClassExists() {
        final UngeneratedTopic impl = () -> { };
        final GennakerException ex = assertThrows(GennakerException.class,
                () -> ClassHunter.getSubscriberProxy(UngeneratedTopic.class, impl));

        final String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("__sub_proxy"), "should name the missing class: " + msg);
        assertTrue(msg.contains("@Topic"), "should mention @Topic: " + msg);
        assertInstanceOf(ClassNotFoundException.class, ex.getCause());
    }

    @Test
    void resolvedConstructorShouldBeCachedPerTopic() {
        final DirectTransport transport = new DirectTransport();
        final TestTopic first = ClassHunter.getPublisherProxy(TestTopic.class, transport);
        final TestTopic second = ClassHunter.getPublisherProxy(TestTopic.class, transport);
        assertEquals(first.getClass(), second.getClass());
    }
}
