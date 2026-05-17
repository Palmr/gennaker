package uk.co.palmr.gennaker.transport.aeron;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import uk.co.palmr.gennaker.Gennaker;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class AeronTransportTest {
    private static final Duration ARRIVAL_TIMEOUT = Duration.ofSeconds(10);

    private final AeronTestTopicImpl topicImplementation = new AeronTestTopicImpl();
    private Gennaker gennaker;

    @BeforeEach
    void setUp() {
        gennaker = new Gennaker(new AeronTransport());
    }

    @AfterEach
    void tearDown() {
        gennaker.shutdown();
    }

    @Test
    void shouldDeliverPublishedCallsToSubscriber() {
        gennaker.subscribe(AeronTestTopic.class, topicImplementation);

        final AeronTestTopic publisher = gennaker.publisher(AeronTestTopic.class);

        publisher.nop();
        publisher.nop();
        publisher.nop();
        awaitUntil(() -> topicImplementation.getNopCallCount() == 3);

        publisher.consume(123);
        publisher.consume(456);
        awaitUntil(() -> topicImplementation.getConsumedItems().equals(List.of(123, 456)));

        publisher.repeat("Hi", 5);
        awaitUntil(() -> "HiHiHiHiHi".equals(topicImplementation.getLastRepeat()));
    }

    @Test
    void shouldBroadcastToAllSubscribers() {
        gennaker.subscribe(AeronTestTopic.class, topicImplementation);
        final AeronTestTopicImpl otherImpl = new AeronTestTopicImpl();
        gennaker.subscribe(AeronTestTopic.class, otherImpl);

        final AeronTestTopic publisher = gennaker.publisher(AeronTestTopic.class);
        publisher.nop();

        awaitUntil(() -> topicImplementation.getNopCallCount() == 1
                && otherImpl.getNopCallCount() == 1);
    }

    private static void awaitUntil(final BooleanSupplier condition) {
        final long deadlineNanos = System.nanoTime() + ARRIVAL_TIMEOUT.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (condition.getAsBoolean()) {
                return;
            }
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        }
        assertTrue(condition.getAsBoolean(), "condition not met within " + ARRIVAL_TIMEOUT);
    }
}
