package uk.co.palmr.gennaker.annotation.processor.sbe;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SBE-specific {@link CodecBodyEmitter} that produces the body fragments
 * referencing the SBE-generated encoder/decoder classes.
 */
public final class SbeCodecBodyEmitter implements CodecBodyEmitter {
    private final String interfaceName;
    private final int maxMessageSize;
    private final List<ExecutableElement> methods;

    public SbeCodecBodyEmitter(final String interfaceName,
                               final int maxMessageSize,
                               final List<ExecutableElement> methods) {
        this.interfaceName = interfaceName;
        this.maxMessageSize = maxMessageSize;
        this.methods = List.copyOf(methods);
    }

    @Override
    public String publisherImports() {
        return """
                import org.agrona.concurrent.UnsafeBuffer;
                import uk.co.palmr.gennaker.Transport;

                import java.nio.ByteBuffer;

                """;
    }

    @Override
    public String publisherFields() {
        final var sb = new StringBuilder();
        sb.append("    private final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(").append(maxMessageSize).append("));\n");
        sb.append("    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();\n");
        sb.append(methods.stream()
                .map(m -> {
                    final var methodName = m.getSimpleName().toString();
                    final var encoderClass = encoderClassName(methodName);
                    final var encoderField = encoderFieldName(methodName);
                    return "    private final " + encoderClass + " " + encoderField + " = new " + encoderClass + "();\n";
                })
                .distinct()
                .collect(Collectors.joining()));
        return sb.toString();
    }

    @Override
    public String publisherMethodBody(final ExecutableElement method) {
        final var methodName = method.getSimpleName().toString();
        final var sb = new StringBuilder();
        sb.append("        var methodEncoder = ").append(encoderFieldName(methodName)).append(".wrapAndApplyHeader(buffer, 0, headerEncoder);\n");
        sb.append(method.getParameters().stream()
                .map(p -> "        methodEncoder." + p.getSimpleName() + "(" + p.getSimpleName() + ");\n")
                .collect(Collectors.joining()));
        sb.append("        if (!transport.publish(").append(interfaceName).append(".class, buffer, headerEncoder.ENCODED_LENGTH + methodEncoder.encodedLength())) {\n");
        sb.append("            throw new UnsupportedOperationException(\"Failed to publish, this case yet to be implemented by Gennaker\"); // TODO: implement this case\n");
        sb.append("        }\n");
        sb.append("        return;\n");
        return sb.toString();
    }

    @Override
    public String subscriberImports() {
        return """
                import org.agrona.DirectBuffer;
                import uk.co.palmr.gennaker.MessageHandler;

                """;
    }

    @Override
    public String subscriberFields() {
        return "    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();\n";
    }

    @Override
    public String subscriberOnMessageBody() {
        final var sb = new StringBuilder();
        sb.append("""
                        if (length < MessageHeaderDecoder.ENCODED_LENGTH) {
                            throw new UnsupportedOperationException("Message too short, this case yet to be implemented by Gennaker"); // TODO: implement this case
                        }
                        messageHeaderDecoder.wrap(buffer, offset);
                        switch (messageHeaderDecoder.templateId()) {
                """);
        for (final var method : methods) {
            final var methodName = method.getSimpleName().toString();
            final var decoderName = decoderClassName(methodName);
            sb.append("            case ").append(decoderName).append(".TEMPLATE_ID -> {\n");
            sb.append("                final var msgDecoder = new ").append(decoderName).append("();\n");
            sb.append("                msgDecoder.wrap(buffer, offset + MessageHeaderDecoder.ENCODED_LENGTH, messageHeaderDecoder.blockLength(), messageHeaderDecoder.version());\n");
            sb.append("                delegate.").append(methodName).append("(");
            sb.append(method.getParameters().stream()
                    .map(p -> "msgDecoder." + p.getSimpleName() + "()")
                    .collect(Collectors.joining(", ")));
            sb.append(");\n");
            sb.append("            }\n");
        }
        sb.append("            default -> System.err.println(\"unknown message type: \" + messageHeaderDecoder.templateId());\n");
        sb.append("        }\n");
        return sb.toString();
    }

    private static String encoderFieldName(final String methodName) {
        return methodName + "Encoder";
    }

    private static String encoderClassName(final String methodName) {
        final var fieldName = encoderFieldName(methodName);
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private static String decoderClassName(final String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1) + "Decoder";
    }
}
