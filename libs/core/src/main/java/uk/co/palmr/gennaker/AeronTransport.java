package uk.co.palmr.gennaker;

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

import java.util.IdentityHashMap;
import java.util.Map;

public class AeronTransport implements Transport {
    public static final String AERON_URI = "aeron:ipc?alias=gennaker";
    private final IdleStrategy idle = new SleepingIdleStrategy();
    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private final Map<Class<?>, Integer> streamIdByTopic = new IdentityHashMap<>();
    private final Map<Class<?>, Publication> publishersByTopic = new IdentityHashMap<>();
    private final Map<Class<?>, Subscription> subscribersByTopic = new IdentityHashMap<>();

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
    public <T, I extends T> boolean publish(final Class<T> topicClass, final DirectBuffer message, final int limit) {
        final var pub = publishersByTopic.computeIfAbsent(topicClass, tc -> aeron.addPublication(AERON_URI, getStreamId(tc)));
        while (pub.offer(message, 0, limit) < 0) {
            idle.idle();
        }
        return true;
    }

    @Override
    public <T, I extends T> void subscribe(final Class<T> topicClass, final I impl) {
        final var sub = subscribersByTopic.computeIfAbsent(topicClass, tc -> aeron.addSubscription(AERON_URI, getStreamId(tc)));
        final var subscriberProxy = ClassHunter.getSubscriberProxy(topicClass, impl);
        final var fragmentAssembler = new FragmentAssembler((buf, offset, len, header) -> subscriberProxy.onMessage(buf, offset, len));

        record SubscriberAgent(Subscription sub, FragmentAssembler handler, String topicName) implements Agent {
            @Override
            public int doWork() {
                sub.poll(handler, 100);
                return 0;
            }

            @Override
            public String roleName() {
                return "Gennaker-subscriber: " + topicName;
            }
        }

        AgentRunner.startOnThread(new AgentRunner(idle, Throwable::printStackTrace, null,
                new SubscriberAgent(sub, fragmentAssembler, topicClass.getSimpleName())));
    }

    @Override
    public void shutdown() {
        CloseHelper.quietCloseAll(publishersByTopic.values());
        CloseHelper.quietCloseAll(subscribersByTopic.values());
        CloseHelper.quietClose(aeron);
        CloseHelper.quietClose(mediaDriver);
    }

    private Integer getStreamId(final Class<?> topicClass) {
        // TODO: This is a terrible idea. I apologise.
        return streamIdByTopic.computeIfAbsent(topicClass, Object::hashCode);
    }
}
