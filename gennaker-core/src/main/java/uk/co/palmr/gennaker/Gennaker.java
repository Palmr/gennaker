package uk.co.palmr.gennaker;

public class Gennaker {
    // TODO: this should be dynamically loaded based on the Transport enum in the annotations package
    private final Transport transport;

    public Gennaker(final Transport transport) {
        this.transport = transport;
    }

    public <T, I extends T> void subscribe(final Class<T> topicClass, final I topicImplementation) {
        transport.subscribe(topicClass, topicImplementation);
    }

    public <T> T publisher(final Class<T> topicClass) {
        return ClassHunter.getPublisherProxy(topicClass, transport);
    }

    public void shutdown() {
        transport.shutdown();
    }
}
