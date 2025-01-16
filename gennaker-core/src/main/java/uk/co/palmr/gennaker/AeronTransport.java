package uk.co.palmr.gennaker;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;

import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

import static uk.co.palmr.gennaker.Gennaker.SUB_PROXY_CLASS_SUFFIX;

public class AeronTransport implements Transport {
    private final IdleStrategy idle = new SleepingIdleStrategy();
    private final MediaDriver mediaDriver;
    private final Aeron aeron;
    private final Map<Class, Integer> streamIdByTopic = new IdentityHashMap<>();
    private final Map<Class, Publication> publishersByTopic = new IdentityHashMap<>();
    private final Map<Class, Subscription> subscribersByTopic = new IdentityHashMap<>();

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
    public boolean publish(final Class topicClass, final DirectBuffer message, final int limit) {
        final var pub = publishersByTopic.computeIfAbsent(topicClass, tc -> aeron.addPublication("aeron:ipc?alias=gennaker", streamIdByTopic.computeIfAbsent(tc, x -> streamIdByTopic.size())));
        while (pub.offer(message, 0, limit) < 0) {
            idle.idle();
        }
        return true;
    }

    @Override
    public void subscribe(final Class topicClass, final Object impl) {
        final AgentRunner subscriberRunner;
        var sub = subscribersByTopic.computeIfAbsent(topicClass, tc -> aeron.addSubscription("aeron:ipc?alias=gennaker", streamIdByTopic.computeIfAbsent(tc, x -> streamIdByTopic.size())));
        final String className = topicClass.getCanonicalName() + SUB_PROXY_CLASS_SUFFIX;
        final FragmentHandler subProxy;
        try {
            @SuppressWarnings("unchecked") final Class<? extends FragmentHandler> proxyClass = (Class<? extends FragmentHandler>) Class.forName(className);
            subProxy = proxyClass.getConstructor(topicClass).newInstance(impl);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to locate class " + className, e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate class " + className, e);
        }

        final var fragmentAssembler = new FragmentAssembler(subProxy);

        subscriberRunner = new AgentRunner(idle,
                Throwable::printStackTrace, null, new Agent() {
            @Override
            public int doWork() {
                sub.poll(fragmentAssembler, 100);
                return 0;
            }

            @Override
            public String roleName() {
                return "Aeron-Subscription: " + topicClass.getSimpleName();
            }
        });

        AgentRunner.startOnThread(subscriberRunner);
    }
}
