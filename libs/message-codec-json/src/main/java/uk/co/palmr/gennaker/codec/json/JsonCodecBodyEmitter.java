package uk.co.palmr.gennaker.codec.json;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.TypeShape;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;

/**
 * Emits JSON-flavoured publisher and subscriber proxy bodies.
 *
 * <p>Wire format: a single JSON object {@code {"m":"<method>","a":[<args>]}}.
 * The {@code "m"} field is always emitted first so the subscriber can pick
 * which method to dispatch to before parsing the argument list.
 */
public final class JsonCodecBodyEmitter implements CodecBodyEmitter {
    private final String interfaceName;
    private final int maxMessageSize;
    private final List<ExecutableElement> methods;
    private final Map<String, TypeShape> reachableTypes;

    public JsonCodecBodyEmitter(final String interfaceName,
                                final int maxMessageSize,
                                final List<ExecutableElement> methods,
                                final Map<String, TypeShape> reachableTypes) {
        this.interfaceName = interfaceName;
        this.maxMessageSize = maxMessageSize;
        this.methods = List.copyOf(methods);
        this.reachableTypes = Map.copyOf(reachableTypes);
    }

    @Override
    public String publisherImports() {
        return """
                import org.agrona.concurrent.UnsafeBuffer;
                import uk.co.palmr.gennaker.Transport;
                import uk.co.palmr.gennaker.codec.json.JsonWriter;

                import java.nio.ByteBuffer;
                import java.nio.charset.StandardCharsets;

                """;
    }

    @Override
    public String publisherFields() {
        return "    private final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(" + maxMessageSize + "));\n" +
                "    private final StringBuilder json = new StringBuilder();\n";
    }

    @Override
    public String publisherMethodBody(final ExecutableElement method) {
        final var methodName = method.getSimpleName().toString();
        final var sb = new StringBuilder();
        sb.append("        json.setLength(0);\n");
        sb.append("        json.append(\"{\\\"m\\\":\\\"").append(methodName).append("\\\",\\\"a\\\":[\");\n");
        final var params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append("        json.append(',');\n");
            }
            sb.append(appendParam(params.get(i)));
        }
        sb.append("        json.append(\"]}\");\n");
        sb.append("        final byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);\n");
        sb.append("        if (bytes.length > buffer.capacity()) {\n");
        sb.append("            throw new IllegalStateException(\"JSON message size \" + bytes.length + \" exceeds buffer capacity \" + buffer.capacity());\n");
        sb.append("        }\n");
        sb.append("        buffer.putBytes(0, bytes);\n");
        sb.append("        if (!transport.publish(").append(interfaceName).append(".class, buffer, bytes.length)) {\n");
        sb.append("            throw new UnsupportedOperationException(\"Failed to publish, this case yet to be implemented by Gennaker\"); // TODO: implement this case\n");
        sb.append("        }\n");
        return sb.toString();
    }

    private String appendParam(final VariableElement param) {
        final var name = param.getSimpleName().toString();
        final var type = param.asType().toString();
        return switch (type) {
            case "java.lang.String" -> "        JsonWriter.writeString(" + name + ", json);\n";
            case "char" -> "        JsonWriter.writeChar(" + name + ", json);\n";
            case "byte", "short" -> "        json.append((int) " + name + ");\n";
            default -> {
                if (reachableTypes.containsKey(type)) {
                    final var simple = simpleNameOf(type);
                    yield "        " + simple + "__layout.encode(" + name + ", json);\n";
                }
                yield "        json.append(" + name + ");\n";
            }
        };
    }

    @Override
    public String subscriberImports() {
        return """
                import org.agrona.DirectBuffer;
                import uk.co.palmr.gennaker.MessageHandler;
                import uk.co.palmr.gennaker.codec.json.JsonReader;

                import java.nio.charset.StandardCharsets;

                """;
    }

    @Override
    public String subscriberFields() {
        return "";
    }

    @Override
    public String subscriberOnMessageBody() {
        final var sb = new StringBuilder();
        sb.append("        final byte[] bytes = new byte[length];\n");
        sb.append("        buffer.getBytes(offset, bytes);\n");
        sb.append("        final JsonReader reader = new JsonReader(new String(bytes, StandardCharsets.UTF_8));\n");
        sb.append("        reader.expectObjectStart();\n");
        sb.append("        reader.expectKey(\"m\");\n");
        sb.append("        final String method = reader.readString();\n");
        sb.append("        reader.expectComma();\n");
        sb.append("        reader.expectKey(\"a\");\n");
        sb.append("        reader.expectArrayStart();\n");
        sb.append("        switch (method) {\n");
        for (final var m : methods) {
            final var methodName = m.getSimpleName().toString();
            sb.append("            case \"").append(methodName).append("\" -> {\n");
            final var params = m.getParameters();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append("                reader.expectComma();\n");
                }
                final var p = params.get(i);
                final var paramType = p.asType().toString();
                sb.append("                final ").append(paramType).append(' ').append(p.getSimpleName());
                sb.append(" = ").append(decodeExpression(paramType)).append(";\n");
            }
            sb.append("                reader.expectArrayEnd();\n");
            sb.append("                reader.expectObjectEnd();\n");
            sb.append("                delegate.").append(methodName).append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(params.get(i).getSimpleName());
            }
            sb.append(");\n");
            sb.append("            }\n");
        }
        sb.append("            default -> System.err.println(\"unknown method: \" + method);\n");
        sb.append("        }\n");
        return sb.toString();
    }

    private String decodeExpression(final String type) {
        if (reachableTypes.containsKey(type)) {
            return simpleNameOf(type) + "__layout.decode(reader)";
        }
        return "reader." + readerCall(type);
    }

    private static String readerCall(final String type) {
        return switch (type) {
            case "java.lang.String" -> "readString()";
            case "char" -> "readChar()";
            case "boolean" -> "readBoolean()";
            case "byte" -> "readByte()";
            case "short" -> "readShort()";
            case "int" -> "readInt()";
            case "long" -> "readLong()";
            case "float" -> "readFloat()";
            case "double" -> "readDouble()";
            default -> throw new IllegalArgumentException("Unsupported JSON parameter type: " + type);
        };
    }

    private static String simpleNameOf(final String fqn) {
        final var dot = fqn.lastIndexOf('.');
        return (dot >= 0) ? fqn.substring(dot + 1) : fqn;
    }
}
