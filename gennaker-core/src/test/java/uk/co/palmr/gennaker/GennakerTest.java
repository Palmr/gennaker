package uk.co.palmr.gennaker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GennakerTest {
    private final TestTopicImpl topicImplementation = new TestTopicImpl();

    @Test
    void shouldDoTheBasics() {
        final Gennaker gennaker = new Gennaker();
        gennaker.subscribe(TestTopic.class, topicImplementation);


        TestTopic publisher = gennaker.publisher(TestTopic.class);


        publisher.nop();
        publisher.nop();
        publisher.nop();
        assertEquals(3, topicImplementation.getNopCallCount());

        publisher.consume(123);
        publisher.consume(456);
        assertEquals(List.of(123, 456), topicImplementation.getConsumedItems());

        publisher.repeat("Hi", 5);
        assertEquals("HiHiHiHiHi", topicImplementation.getLastRepeat());
    }

    @Test
    void shouldHandleLateJoin() {
        final Gennaker gennaker = new Gennaker();
        TestTopic publisher = gennaker.publisher(TestTopic.class);


        gennaker.subscribe(TestTopic.class, topicImplementation);


        publisher.nop();
        assertEquals(1, topicImplementation.getNopCallCount());
    }

    @Test
    void shouldBroadcastToAllSubscribers() {
        final Gennaker gennaker = new Gennaker();
        gennaker.subscribe(TestTopic.class, topicImplementation);

        final TestTopicImpl otherImpl = new TestTopicImpl();
        gennaker.subscribe(TestTopic.class, otherImpl);


        TestTopic publisher = gennaker.publisher(TestTopic.class);


        publisher.nop();
        assertEquals(1, topicImplementation.getNopCallCount());
        assertEquals(1, otherImpl.getNopCallCount());
    }
}
