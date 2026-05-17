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
        writer.write("package ");
        writer.write(packageName);
        writer.write(";\n\n");
        writer.write(emitter.subscriberImports());
        writer.write("public class ");
        writer.write(className);
        writer.write(" implements MessageHandler {\n");
        writer.write(emitter.subscriberFields());
        writer.write("    private final " + interfaceName + " delegate;\n\n");
        writer.write("    public ");
        writer.write(className);
        writer.write("(final " + interfaceName + " delegate) {\n        this.delegate = delegate;\n    }\n\n");

        writer.write("    public void onMessage(final DirectBuffer buffer, final int offset, final int length) {\n");
        writer.write(emitter.subscriberOnMessageBody());
        writer.write("    }\n");
        writer.write("}\n");
    }
}
