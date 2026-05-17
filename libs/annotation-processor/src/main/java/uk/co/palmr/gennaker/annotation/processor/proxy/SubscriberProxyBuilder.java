package uk.co.palmr.gennaker.annotation.processor.proxy;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;

import java.io.IOException;
import java.io.Writer;

public final class SubscriberProxyBuilder implements JavaProxy {
    public static final String PROXY_SUFFIX = "__sub_proxy";
    private final String packageName;
    private final String interfaceName;
    private final String className;
    private final CodecBodyEmitter emitter;

    public SubscriberProxyBuilder(final String packageName,
                                  final String interfaceName,
                                  final CodecBodyEmitter emitter) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.className = interfaceName + PROXY_SUFFIX;
        this.emitter = emitter;
    }

    @Override
    public String getFileName() {
        return packageName + "." + className;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write("package %s;%n%n".formatted(packageName));
        writer.write(emitter.subscriberImports());
        writer.write("""
                public class %s implements MessageHandler {
                    private static final java.lang.System.Logger LOG = java.lang.System.getLogger("%s.%s");

                """.formatted(className, packageName, interfaceName));
        writer.write(emitter.subscriberFields());
        writer.write("""
                    private final %s delegate;

                    public %s(final %s delegate) {
                        this.delegate = delegate;
                    }

                    public void onMessage(final DirectBuffer buffer, final int offset, final int length) {
                        if (LOG.isLoggable(java.lang.System.Logger.Level.TRACE)) {
                            LOG.log(java.lang.System.Logger.Level.TRACE, "sub received " + length + " bytes");
                        }
                """.formatted(interfaceName, className, interfaceName));
        writer.write(emitter.subscriberOnMessageBody());
        writer.write("    }\n}\n");
    }
}
