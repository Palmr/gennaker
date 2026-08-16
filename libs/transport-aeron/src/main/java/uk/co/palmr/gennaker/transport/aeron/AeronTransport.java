package uk.co.palmr.gennaker.transport.aeron;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.palmr.gennaker.MessageHandler;
import uk.co.palmr.gennaker.Transport;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/// [Transport] backed by an embedded Aeron media driver. Each topic is
/// mapped to its own Aeron stream id; publishers offer encoded messages onto
/// an Aeron `Publication` and subscribers consume them via a polling `Agent`
/// thread. Suitable for low-latency inter-process delivery.
public final class AeronTransport implements Transport {
    /// Aeron channel URI used for all gennaker topics (IPC, named alias).
    public static final String AERON_URI = "aeron:ipc?alias=gennaker";

    private static final System.Logger LOG = System.getLogger(AeronTransport.class.getName());

    private final IdleStrategy idle = new SleepingIdleStrategy();
    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private final Map<Class<?>, Integer> streamIdByTopic = new IdentityHashMap<>();
    private final Map<Class<?>, Publication> publishersByTopic = new IdentityHashMap<>();
    private final Map<Class<?>, TopicSubscriber> subscribersByTopic = new IdentityHashMap<>();

    /// Launches an embedded Aeron media driver and connects an Aeron client to
    /// it. The driver's working directory is deleted on start and on shutdown.
    public AeronTransport() {
        final var mediaDriverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .threadingMode(ThreadingMode.SHARED)
                .sharedIdleStrategy(idle)
                .dirDeleteOnShutdown(true);
        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final var aeronCtx = new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName());
        aeron = Aeron.connect(aeronCtx);
    }

    @Override
    public <T> boolean publish(final Class<T> topicClass, final DirectBuffer message, final int limit) {
        final var pub = publishersByTopic.computeIfAbsent(topicClass, tc -> {
            final var streamId = getStreamId(tc);
            LOG.log(System.Logger.Level.DEBUG, "opened publication for {0} on stream {1}", tc.getName(), streamId);
            return aeron.addPublication(AERON_URI, streamId);
        });
        while (pub.offer(message, 0, limit) < 0) {
            idle.idle();
        }
        return true;
    }

    @Override
    public <T> void subscribe(final Class<T> topicClass, final MessageHandler<T> handler) {
        subscribersByTopic.computeIfAbsent(topicClass, tc -> {
            final var streamId = getStreamId(tc);
            LOG.log(System.Logger.Level.DEBUG, "opened subscription for {0} on stream {1}", tc.getName(), streamId);
            final var subscriber = new TopicSubscriber(aeron.addSubscription(AERON_URI, streamId), tc.getSimpleName());
            subscriber.start(idle);
            return subscriber;
        }).add(handler);
    }

    @Override
    public void shutdown() {
        CloseHelper.quietCloseAll(publishersByTopic.values());
        CloseHelper.quietCloseAll(subscribersByTopic.values());
        CloseHelper.quietClose(aeron);
        CloseHelper.quietClose(mediaDriver);
    }

    /// All handlers for a topic share one Aeron subscription polled by one
    /// agent thread. Aeron delivers each fragment to a subscription exactly
    /// once, so a second polling thread would steal messages from the first
    /// rather than see its own copy; the fan-out to handlers happens here.
    private static final class TopicSubscriber implements Agent, AutoCloseable {
        private final List<MessageHandler<?>> handlers = new CopyOnWriteArrayList<>();
        private final FragmentAssembler assembler = new FragmentAssembler((buf, offset, len, _) -> {
            for (final var handler : handlers) {
                handler.onMessage(buf, offset, len);
            }
        });
        private final Subscription subscription;
        private final String topicName;
        private AgentRunner runner;

        private TopicSubscriber(final Subscription subscription, final String topicName) {
            this.subscription = subscription;
            this.topicName = topicName;
        }

        private void start(final IdleStrategy idle) {
            runner = new AgentRunner(idle, Throwable::printStackTrace, null, this);
            AgentRunner.startOnThread(runner);
        }

        private void add(final MessageHandler<?> handler) {
            handlers.add(handler);
        }

        @Override
        public int doWork() {
            return subscription.poll(assembler, 100);
        }

        @Override
        public String roleName() {
            return "Gennaker-subscriber: " + topicName;
        }

        @Override
        public void close() {
            CloseHelper.quietClose(runner);
            CloseHelper.quietClose(subscription);
        }
    }

    private Integer getStreamId(final Class<?> topicClass) {
        // TODO: This is a terrible idea. I apologise.
        return streamIdByTopic.computeIfAbsent(topicClass, Object::hashCode);
    }
}
