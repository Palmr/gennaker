package uk.co.palmr.gennaker.annotation.processor.proxy;

import javax.lang.model.element.ExecutableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SubscriberProxyBuilder implements JavaProxy {

    public static final String PROXY_SUFFIX = "__sub_proxy";
    private final String packageName;
    private final String interfaceName;
    private final String className;
    private final List<ExecutableElement> methods = new ArrayList<>();

    public SubscriberProxyBuilder(final String packageName, final String interfaceName) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.className = interfaceName + PROXY_SUFFIX;
    }

    private void writeHeader(final Writer writer) throws IOException {
        writer.write("package ");
        writer.write(packageName);
        writer.write(";\n\n");
        writer.write("""
                import org.agrona.DirectBuffer;
                import uk.co.palmr.gennaker.MessageHandler;

                """);
        writer.write("public class ");
        writer.write(className);
        writer.write(" implements MessageHandler {\n");
        writer.write("    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();\n");
        writer.write("    private final " + interfaceName + " delegate;\n\n");
        writer.write("    public ");
        writer.write(className);
        writer.write("(final " + interfaceName + " delegate) {\n        this.delegate = delegate;\n    }\n\n");
    }

    @Override
    public String getFileName() {
        return packageName + "." + className;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writeHeader(writer);

        writer.write("""
                    public void onMessage(final DirectBuffer buffer, final int offset, final int length) {
                        if (length < MessageHeaderDecoder.ENCODED_LENGTH) {
                            throw new UnsupportedOperationException("Message too short, this case yet to be implemented by Gennaker"); // TODO: implement this case
                        }
                        messageHeaderDecoder.wrap(buffer, offset);
                        switch (messageHeaderDecoder.templateId()) {
                """);
        for (final var method : methods) {
            final var methodName = method.getSimpleName().toString();
            writer.write("            case ");
            final var sbeDecoderName = getSbeDecoderName(methodName);
            writer.write(sbeDecoderName);
            writer.write(".TEMPLATE_ID -> {\n");
            writer.write("                final var msgDecoder = new " + sbeDecoderName + "();\n" +
                    "                msgDecoder.wrap(buffer, offset + MessageHeaderDecoder.ENCODED_LENGTH, messageHeaderDecoder.blockLength(), messageHeaderDecoder.version());\n" +
                    "                delegate." + methodName + "(");
            writer.write(method.getParameters().stream().map(param -> "msgDecoder." + param.getSimpleName() + "()").collect(Collectors.joining(", ")));
            writer.write(");\n");
            writer.write("            }\n");

        }
        writer.write("""
                            default -> System.err.println("unknown message type: " + messageHeaderDecoder.templateId());
                        }
                    }
                """);
        writer.write("}\n");
    }

    public void method(final ExecutableElement methodElement) {
        methods.add(methodElement);
    }

    private static String getSbeDecoderName(final String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1) + "Decoder";
    }
}
