package uk.co.palmr.gennaker;

import java.lang.reflect.InvocationTargetException;

public class ClassHunter {
    static final String PUB_PROXY_CLASS_SUFFIX = "__pub_proxy";
    static final String SUB_PROXY_CLASS_SUFFIX = "__sub_proxy";

    static <T> T getPublisherProxy(final Class<T> topicClass, final Transport transport) {
        final String className = topicClass.getCanonicalName() + PUB_PROXY_CLASS_SUFFIX;
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

    static <T, I extends T> MessageHandler getSubscriberProxy(final Class<T> topicClass, final I topicImplementation) {
        final String className = topicClass.getCanonicalName() + SUB_PROXY_CLASS_SUFFIX;
        try {
            @SuppressWarnings("unchecked") final Class<? extends MessageHandler> proxyClass = (Class<? extends MessageHandler>) Class.forName(className);
            return proxyClass.getConstructor(topicClass).newInstance(topicImplementation);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to locate class " + className, e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate class " + className, e);
        }
    }
}
