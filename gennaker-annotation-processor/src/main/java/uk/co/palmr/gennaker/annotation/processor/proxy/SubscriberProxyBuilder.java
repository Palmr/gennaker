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
        writer.write("import io.aeron.logbuffer.FragmentHandler;\n");
        writer.write("import io.aeron.logbuffer.Header;\n");
        writer.write("import org.agrona.DirectBuffer;\n");
        writer.write("import uk.co.palmr.gennaker.Transport;\n");
        writer.write("import org.agrona.concurrent.UnsafeBuffer;\n");
        writer.write("import java.nio.ByteBuffer;\n");
        writer.write("public class ");
        writer.write(className);
        writer.write(" implements FragmentHandler {\n");
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
                    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
                        if (length < MessageHeaderDecoder.ENCODED_LENGTH) {
                            System.err.println("Message too short");
                            return;
                        }
                        messageHeaderDecoder.wrap(buffer, offset);
                        switch (messageHeaderDecoder.templateId())
                        {
                """);
        for (final var method : methods) {
            final var methodName = method.getSimpleName().toString();
            writer.write("             case ");
            final var sbeDecoderName = getSbeDecoderName(methodName);
            writer.write(sbeDecoderName);
            writer.write(".TEMPLATE_ID -> {\n");
            writer.write("                    final var msgDecoder = new " + sbeDecoderName + "();\n" +
                    "                    msgDecoder.wrap(buffer, offset + MessageHeaderDecoder.ENCODED_LENGTH, messageHeaderDecoder.blockLength(), messageHeaderDecoder.version());\n" +
                    "                    delegate." + methodName + "(");
            writer.write(method.getParameters().stream().map(param -> "msgDecoder." + param.getSimpleName() + "()").collect(Collectors.joining(", ")));
            writer.write(");\n");
            writer.write("             }\n");

        }
        writer.write("""
                             default -> System.err.println("unknown message type: " + messageHeaderDecoder.templateId());
                         }
                     }
                    \s
                     public void handleEvent(DirectBuffer buffer, int offset) {
                         System.out.println("TODO: Invoke subscriber method here");
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
