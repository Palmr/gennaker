package uk.co.palmr.gennaker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class ClassHunter {
    static final String PUB_PROXY_CLASS_SUFFIX = "__pub_proxy";
    static final String SUB_PROXY_CLASS_SUFFIX = "__sub_proxy";

    private static final ClassValue<MethodHandle> PUBLISHER_CONSTRUCTORS = new ClassValue<>() {
        @Override
        protected MethodHandle computeValue(final Class<?> topicClass) {
            return findConstructor(topicClass, PUB_PROXY_CLASS_SUFFIX, Transport.class);
        }
    };

    private static final ClassValue<MethodHandle> SUBSCRIBER_CONSTUCTORS = new ClassValue<>() {
        @Override
        protected MethodHandle computeValue(final Class<?> topicClass) {
            return findConstructor(topicClass, SUB_PROXY_CLASS_SUFFIX, topicClass);
        }
    };

    private ClassHunter() {
    }

    static <T> T getPublisherProxy(final Class<T> topicClass, final Transport transport) {
        final MethodHandle constructor = PUBLISHER_CONSTRUCTORS.get(topicClass);
        try {
            @SuppressWarnings("unchecked") final T proxy = (T) constructor.invoke(transport);
            return proxy;
        }
        catch (Throwable t) {
            throw new GennakerException(
                    "Publisher proxy constructor for " + topicClass.getName() + " threw — see cause", t);
        }
    }

    static <T, I extends T> MessageHandler getSubscriberProxy(final Class<T> topicClass, final I topicImplementation) {
        final MethodHandle constructor = SUBSCRIBER_CONSTUCTORS.get(topicClass);
        try {
            return (MessageHandler) constructor.invoke(topicImplementation);
        }
        catch (Throwable t) {
            throw new GennakerException(
                    "Subscriber proxy constructor for " + topicClass.getName() + " threw — see cause", t);
        }
    }

    private static MethodHandle findConstructor(final Class<?> topicClass, final String suffix, final Class<?> argType) {
        final String className = topicClass.getCanonicalName() + suffix;
        final Class<?> proxyClass;

        try {
            proxyClass = Class.forName(className, true, topicClass.getClassLoader());
        }
        catch (ClassNotFoundException e) {
            throw new GennakerException(
                    "No generated proxy " + className + " found for topic " + topicClass.getName() +
                            ". Is the interface annotated with @Topic, and has the annotation processor run on its module?",
                    e);
        }

        try {
            return MethodHandles.publicLookup().findConstructor(proxyClass, MethodType.methodType(void.class, argType))
                    .asType(MethodType.methodType(Object.class, Object.class));
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new GennakerException(
                    "Generated proxy " + className + " does not expose the expected public constructor (" +
                            argType.getName() + "). This usually means the generated source is stale; rebuild the module.",
                    e);
        }
    }
}
