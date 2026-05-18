package uk.co.palmr.gennaker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingTransportTest {

    @Test
    void shouldRecordPublishesAndForwardToDelegate() {
        final RecordingTransport recorder = new RecordingTransport(new DirectTransport());
        final Gennaker gennaker = new Gennaker(recorder);
        final TestTopicImpl impl = new TestTopicImpl();
        gennaker.subscribe(TestTopic.class, impl);

        final TestTopic publisher = gennaker.publisher(TestTopic.class);
        publisher.nop();
        publisher.consume(7);
        publisher.consume(9);

        assertEquals(1, impl.getNopCallCount());
        assertEquals(List.of(7, 9), impl.getConsumedItems());

        assertEquals(3, recorder.countFor(TestTopic.class));
        assertEquals(3, recorder.bytesFor(TestTopic.class).size());

        gennaker.shutdown();
    }

    @Test
    void shouldReturnEmptyListForUnrecordedTopic() {
        final RecordingTransport recorder = new RecordingTransport(new DirectTransport());
        assertEquals(0, recorder.countFor(TestTopic.class));
        assertTrue(recorder.bytesFor(TestTopic.class).isEmpty());
    }

    @Test
    void shouldDecodeRecordedMessagesIntoProvidedImpl() {
        final RecordingTransport recorder = new RecordingTransport(new DirectTransport());
        final Gennaker gennaker = new Gennaker(recorder);
        gennaker.subscribe(TestTopic.class, new TestTopicImpl());

        final TestTopic publisher = gennaker.publisher(TestTopic.class);
        publisher.consume(11);
        publisher.consume(22);
        publisher.repeat("ab", 3);

        final TestTopicImpl replayed = new TestTopicImpl();
        recorder.decodeAs(TestTopic.class, replayed);

        assertEquals(List.of(11, 22), replayed.getConsumedItems());
        assertEquals("ababab", replayed.getLastRepeat());

        gennaker.shutdown();
    }

    @Test
    void shouldExposeRawBytesForRegressionAssertions() {
        final RecordingTransport recorder = new RecordingTransport(new DirectTransport());
        final Gennaker gennaker = new Gennaker(recorder);
        gennaker.subscribe(TestTopic.class, new TestTopicImpl());

        final TestTopic publisher = gennaker.publisher(TestTopic.class);
        publisher.consume(42);
        publisher.consume(42);

        final List<byte[]> bytes = recorder.bytesFor(TestTopic.class);
        assertEquals(2, bytes.size());
        assertTrue(bytes.get(0).length > 0);
        assertEquals(bytes.get(0).length, bytes.get(1).length);
        assertNotSame(bytes.get(0), bytes.get(1));
        for (int i = 0; i < bytes.get(0).length; i++) {
            assertEquals(bytes.get(0)[i], bytes.get(1)[i], "byte " + i);
        }
    }

    @Test
    void clearShouldDropAllRecordings() {
        final RecordingTransport recorder = new RecordingTransport(new DirectTransport());
        final Gennaker gennaker = new Gennaker(recorder);
        gennaker.subscribe(TestTopic.class, new TestTopicImpl());

        gennaker.publisher(TestTopic.class).nop();
        assertEquals(1, recorder.countFor(TestTopic.class));

        recorder.clear();
        assertEquals(0, recorder.countFor(TestTopic.class));

        gennaker.shutdown();
    }

    @Test
    void clearForTopicShouldDropOnlyThatTopic() {
        final RecordingTransport recorder = new RecordingTransport(new DirectTransport());
        final Gennaker gennaker = new Gennaker(recorder);
        gennaker.subscribe(TestTopic.class, new TestTopicImpl());

        gennaker.publisher(TestTopic.class).nop();
        recorder.clear(TestTopic.class);
        assertEquals(0, recorder.countFor(TestTopic.class));

        gennaker.shutdown();
    }
}
