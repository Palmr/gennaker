package uk.co.palmr.gennaker;

import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static uk.co.palmr.gennaker.Gennaker.SUB_PROXY_CLASS_SUFFIX;

public class DirectTransport implements Transport {
    private final Map<Class, List<Object>> subscribersByTopic = new IdentityHashMap<>();

    @Override
    public <T> boolean publish(final Class<T> topicClass, final DirectBuffer message, final int limit) {
        subscribersByTopic.get(topicClass).forEach(subscriber -> {
            final FragmentHandler subProxy;
            final String className = topicClass.getCanonicalName() + SUB_PROXY_CLASS_SUFFIX;
            try {
                @SuppressWarnings("unchecked") final Class<? extends FragmentHandler> proxyClass = (Class<? extends FragmentHandler>) Class.forName(className);
                subProxy = proxyClass.getConstructor(topicClass).newInstance(subscriber);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to locate class " + className, e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                throw new RuntimeException("Failed to instantiate class " + className, e);
            }
            subProxy.onFragment(message, 0, limit, null);
        });
        return true;
    }

    @Override
    public <T> void subscribe(final Class<T> topicClass, final Object impl) {
        subscribersByTopic.computeIfAbsent(topicClass, c -> new ArrayList<>()).add(impl);
    }
}
