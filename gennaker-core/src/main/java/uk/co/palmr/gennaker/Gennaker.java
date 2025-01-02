package uk.co.palmr.gennaker;

import java.lang.reflect.InvocationTargetException;

public class Gennaker {
    static final String PUB_PROXY_CLASS_SUFFIX = "__pub_proxy";
    static final String SUB_PROXY_CLASS_SUFFIX = "__sub_proxy";

    // TODO: this should be dynamically loaded based on the Transport enum in the annotations package
    private final Transport transport = new DirectTransport();

    public <T, I extends T> void subscribe(final Class<T> topicClazz, final I topicImplementation) {
        transport.subscribe(topicClazz, topicImplementation);
    }

    public <T> T publisher(final Class<T> topicClazz) {
        final String className = topicClazz.getCanonicalName() + PUB_PROXY_CLASS_SUFFIX;
        try {
            @SuppressWarnings("unchecked") final Class<? extends T> proxyClass = (Class<? extends T>) Class.forName(className);
            return proxyClass.getConstructor(Transport.class).newInstance(transport);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to locate class " + className, e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate class " + className, e);
        }
    }
}
