package uk.co.palmr.gennaker.annotation.processor.proxy;

import java.io.Writer;

public sealed interface JavaProxy permits PublisherProxyBuilder, SubscriberProxyBuilder {
    String getFileName();
    void write(Writer writer) throws Exception;
}
