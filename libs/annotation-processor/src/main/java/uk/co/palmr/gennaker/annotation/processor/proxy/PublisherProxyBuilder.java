package uk.co.palmr.gennaker.annotation.processor.proxy;

import javax.lang.model.element.ExecutableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PublisherProxyBuilder implements JavaProxy {
    public static final String PROXY_SUFFIX = "__pub_proxy";
    private final String packageName;
    private final String interfaceName;
    private final String className;
    private final int maxMessageSize;
    private final List<ExecutableElement> methods = new ArrayList<>();

    public PublisherProxyBuilder(final String packageName, final String interfaceName, final int maxMessageSize) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.className = interfaceName + PROXY_SUFFIX;
        this.maxMessageSize = maxMessageSize;
    }

    private void writeHeader(final Writer writer) throws IOException {
        writer.write("package ");
        writer.write(packageName);
        writer.write(";\n\n");
        writer.write("import org.agrona.concurrent.UnsafeBuffer;\n");
        writer.write("import uk.co.palmr.gennaker.Transport;\n\n");
        writer.write("import java.nio.ByteBuffer;\n\n");
        writer.write("public class ");
        writer.write(className);
        writer.write(" implements ");
        writer.write(interfaceName);
        writer.write(" {\n");
        writer.write("    private final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(" + maxMessageSize + "));\n");
        writer.write("    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();\n");
        writer.write(methods.stream()
                .map(m -> {
                    final var methodName = m.getSimpleName().toString();
                    final var sbeEncoderClassName = getSbeEncoderClassName(methodName);
                    final var sbeEncoderName = getSbeEncoderName(methodName);
                    return "    private final " + sbeEncoderClassName + " " + sbeEncoderName + " = new " + sbeEncoderClassName + "();\n";
                })
                .distinct()
                .collect(Collectors.joining()));
        writer.write("    private final Transport transport;\n\n");
        writer.write("    public ");
        writer.write(className);
        writer.write("(final Transport transport) {\n        this.transport = transport;\n    }\n\n");

    }

    @Override
    public String getFileName() {
        return packageName + "." + className;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writeHeader(writer);
        writeMethods(writer);
        writer.write("}\n");
    }

    private void writeMethods(final Writer writer) throws IOException {
        for (final ExecutableElement methodElement : methods) {
            final var methodName = methodElement.getSimpleName().toString();
            writer.write("    @Override\n");
            writer.write("    public ");
            writer.write(methodElement.getReturnType().toString());
            writer.write(" ");
            writer.write(methodName);
            writer.write("(");
            writer.write(methodElement.getParameters().stream().map(p -> p.asType() + " " + p.getSimpleName()).collect(Collectors.joining(", ")));
            writer.write(") {\n");
            writer.write("        var methodEncoder = " + getSbeEncoderName(methodName) + ".wrapAndApplyHeader(buffer, 0, headerEncoder);\n");
            writer.write(
                    methodElement.getParameters().stream()
                            .map(p -> "        methodEncoder." + p.getSimpleName() + "(" + p.getSimpleName() + ");\n").collect(Collectors.joining())
            );

            writer.write("        if (!transport.publish(");
            writer.write(interfaceName);
            writer.write(".class, buffer, headerEncoder.ENCODED_LENGTH + methodEncoder.encodedLength())) {\n");
            writer.write("            throw new UnsupportedOperationException(\"Failed to publish, this case yet to be implemented by Gennaker\"); // TODO: implement this case\n");
            writer.write("        }\n");
            writer.write("        return;\n");
            writer.write("    }\n\n");
        }
    }

    private static String getSbeEncoderName(final String methodName) {
        return methodName + "Encoder";
    }

    private static String getSbeEncoderClassName(final String methodName) {
        final var sbeEncoderName = getSbeEncoderName(methodName);
        return sbeEncoderName.substring(0, 1).toUpperCase() + sbeEncoderName.substring(1);
    }

    public void method(final ExecutableElement methodElement) {
        methods.add(methodElement);
    }
}
