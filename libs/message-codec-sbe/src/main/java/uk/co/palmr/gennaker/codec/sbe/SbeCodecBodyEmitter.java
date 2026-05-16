package uk.co.palmr.gennaker.codec.sbe;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.FieldKind;
import uk.co.palmr.gennaker.codec.FieldShape;
import uk.co.palmr.gennaker.codec.TypeShape;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SBE-specific {@link CodecBodyEmitter} that produces the body fragments
 * referencing the SBE-generated encoder/decoder classes.
 */
public final class SbeCodecBodyEmitter implements CodecBodyEmitter {
    private final String interfaceName;
    private final int maxMessageSize;
    private final List<ExecutableElement> methods;
    private final Map<String, TypeShape> reachableTypes;

    public SbeCodecBodyEmitter(final String interfaceName,
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

                import java.nio.ByteBuffer;

                """;
    }

    @Override
    public String publisherFields() {
        final var sb = new StringBuilder();
        sb.append("    private final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(")
                .append(maxMessageSize).append("));\n");
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
        sb.append("        var methodEncoder = ").append(encoderFieldName(methodName))
                .append(".wrapAndApplyHeader(buffer, 0, headerEncoder);\n");

        for (final var param : method.getParameters()) {
            emitPublisherParam(sb, param, "methodEncoder");
        }

        sb.append("        if (!transport.publish(").append(interfaceName)
                .append(".class, buffer, headerEncoder.ENCODED_LENGTH + methodEncoder.encodedLength())) {\n");
        sb.append("            throw new UnsupportedOperationException(\"Failed to publish, this case yet to be implemented by Gennaker\"); // TODO: implement this case\n");
        sb.append("        }\n");
        sb.append("        return;\n");
        return sb.toString();
    }

    private void emitPublisherParam(final StringBuilder sb,
                                    final VariableElement param,
                                    final String encoderVar) {
        final var paramName = param.getSimpleName().toString();
        final var paramType = param.asType().toString();

        if (!reachableTypes.containsKey(paramType)) {
            // Primitive or String — direct setter
            sb.append("        ").append(encoderVar).append('.').append(paramName)
                    .append('(').append(paramName).append(");\n");
        } else {
            // Record/Layout type — flatten fields
            final var shape = reachableTypes.get(paramType);
            emitPublisherShape(sb, paramName, paramName, shape, encoderVar);
        }
    }

    private void emitPublisherShape(final StringBuilder sb,
                                    final String accessor,
                                    final String prefix,
                                    final TypeShape shape,
                                    final String encoderVar) {
        // First pass: fixed-length fields
        for (final var field : shape.fields()) {
            switch (field.kind()) {
                case PRIMITIVE -> {
                    final var flatName = prefix + capitalize(field.name());
                    sb.append("        ").append(encoderVar).append('.').append(flatName)
                            .append('(').append(accessor).append('.').append(field.javaName()).append("());\n");
                }
                default -> { /* handled in later passes */ }
            }
        }
        // Second pass: groups (lists/maps)
        for (final var field : shape.fields()) {
            switch (field.kind()) {
                case LIST -> emitPublisherList(sb, accessor, prefix, field, encoderVar);
                case MAP -> emitPublisherMap(sb, accessor, prefix, field, encoderVar);
                default -> { /* already handled or handled next */ }
            }
        }
        // Third pass: variable-length (String, nested objects with strings)
        for (final var field : shape.fields()) {
            switch (field.kind()) {
                case STRING -> {
                    final var flatName = prefix + capitalize(field.name());
                    sb.append("        ").append(encoderVar).append('.').append(flatName)
                            .append('(').append(accessor).append('.').append(field.javaName()).append("());\n");
                }
                case OBJECT -> emitPublisherNestedObject(sb, accessor, prefix, field, encoderVar);
                default -> { /* already handled */ }
            }
        }
    }

    private void emitPublisherList(final StringBuilder sb,
                                   final String accessor,
                                   final String prefix,
                                   final FieldShape field,
                                   final String encoderVar) {
        final var flatName = prefix + capitalize(field.name());
        final var groupVar = flatName + "Group";
        sb.append("        var ").append(groupVar).append(" = ").append(encoderVar)
                .append('.').append(flatName).append("Count(")
                .append(accessor).append('.').append(field.javaName()).append("().size());\n");
        sb.append("        for (var elem__ : ").append(accessor).append('.').append(field.javaName()).append("()) {\n");
        sb.append("            ").append(groupVar).append(".next();\n");
        emitGroupElementEncode(sb, "elem__", field, groupVar);
        sb.append("        }\n");
    }

    private void emitPublisherMap(final StringBuilder sb,
                                  final String accessor,
                                  final String prefix,
                                  final FieldShape field,
                                  final String encoderVar) {
        final var flatName = prefix + capitalize(field.name());
        final var groupVar = flatName + "Group";
        sb.append("        var ").append(groupVar).append(" = ").append(encoderVar)
                .append('.').append(flatName).append("Count(")
                .append(accessor).append('.').append(field.javaName()).append("().size());\n");
        sb.append("        for (var entry__ : ").append(accessor).append('.').append(field.javaName())
                .append("().entrySet()) {\n");
        sb.append("            ").append(groupVar).append(".next();\n");
        sb.append("            ").append(groupVar).append(".key(entry__.getKey());\n");
        sb.append("            ").append(groupVar).append(".value(entry__.getValue());\n");
        sb.append("        }\n");
    }

    private void emitGroupElementEncode(final StringBuilder sb,
                                        final String accessor,
                                        final FieldShape listField,
                                        final String groupVar) {
        final var elemShape = (listField.nestedShape() != null)
                ? listField.nestedShape()
                : reachableTypes.get(listField.javaType());

        if (elemShape != null) {
            // Record element — set each field
            for (final var f : elemShape.fields()) {
                if (f.kind() == FieldKind.PRIMITIVE) {
                    sb.append("            ").append(groupVar).append('.').append(f.name())
                            .append('(').append(accessor).append('.').append(f.javaName()).append("());\n");
                }
            }
            for (final var f : elemShape.fields()) {
                if (f.kind() == FieldKind.STRING) {
                    sb.append("            ").append(groupVar).append('.').append(f.name())
                            .append('(').append(accessor).append('.').append(f.javaName()).append("());\n");
                }
            }
        } else {
            // Primitive/String element
            sb.append("            ").append(groupVar).append(".value(").append(accessor).append(");\n");
        }
    }

    private void emitPublisherNestedObject(final StringBuilder sb,
                                           final String accessor,
                                           final String prefix,
                                           final FieldShape field,
                                           final String encoderVar) {
        final var nestedShape = (field.nestedShape() != null)
                ? field.nestedShape()
                : reachableTypes.get(field.javaType());
        if (nestedShape != null) {
            final var nestedAccessor = accessor + "." + field.javaName() + "()";
            final var nestedPrefix = prefix + capitalize(field.name());
            emitPublisherShape(sb, nestedAccessor, nestedPrefix, nestedShape, encoderVar);
        }
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

            emitSubscriberParamDecoding(sb, method);

            sb.append("                delegate.").append(methodName).append("(");
            sb.append(method.getParameters().stream()
                    .map(p -> {
                        if (reachableTypes.containsKey(p.asType().toString())) {
                            return p.getSimpleName().toString();
                        }
                        return "msgDecoder." + p.getSimpleName() + "()";
                    })
                    .collect(Collectors.joining(", ")));
            sb.append(");\n");
            sb.append("            }\n");
        }
        sb.append("            default -> System.err.println(\"unknown message type: \" + messageHeaderDecoder.templateId());\n");
        sb.append("        }\n");
        return sb.toString();
    }

    private void emitSubscriberParamDecoding(final StringBuilder sb, final ExecutableElement method) {
        for (final var param : method.getParameters()) {
            final var paramType = param.asType().toString();
            if (reachableTypes.containsKey(paramType)) {
                emitSubscriberRecordDecoding(sb, param.getSimpleName().toString(), paramType,
                        param.getSimpleName().toString(), "msgDecoder");
            }
        }
    }

    private void emitSubscriberRecordDecoding(final StringBuilder sb,
                                              final String varName,
                                              final String javaType,
                                              final String prefix,
                                              final String decoderVar) {
        final var shape = reachableTypes.get(javaType);
        if (shape == null) {
            return;
        }
        final var simpleName = simpleNameOf(javaType);

        // Read each field in record declaration order
        for (final var field : shape.fields()) {
            final var flatName = prefix + capitalize(field.name());
            switch (field.kind()) {
                case PRIMITIVE, STRING ->
                    sb.append("                final var ").append(flatName)
                            .append("__ = ").append(decoderVar).append('.').append(flatName).append("();\n");
                case OBJECT -> emitSubscriberRecordDecoding(sb, flatName, field.javaType(), flatName, decoderVar);
                case LIST -> emitSubscriberListDecoding(sb, flatName, field, decoderVar);
                default -> { /* maps handled similarly to lists */ }
            }
        }

        // Construct the record
        sb.append("                final var ").append(varName).append(" = new ").append(simpleName).append("(");
        for (int i = 0; i < shape.fields().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final var field = shape.fields().get(i);
            final var flatName = prefix + capitalize(field.name());
            if (field.kind() == FieldKind.PRIMITIVE || field.kind() == FieldKind.STRING) {
                sb.append(flatName).append("__");
            } else {
                sb.append(flatName);
            }
        }
        sb.append(");\n");
    }

    private void emitSubscriberListDecoding(final StringBuilder sb,
                                            final String varName,
                                            final FieldShape field,
                                            final String decoderVar) {
        final var elemShape = (field.nestedShape() != null)
                ? field.nestedShape()
                : reachableTypes.get(field.javaType());
        final var elemSimple = simpleNameOf(field.javaType());

        sb.append("                final var ").append(varName).append("Group = ")
                .append(decoderVar).append('.').append(varName).append("();\n");
        sb.append("                final var ").append(varName)
                .append(" = new java.util.ArrayList<").append(elemSimple).append(">();\n");
        sb.append("                for (var g__ : ").append(varName).append("Group) {\n");

        if (elemShape != null) {
            // Reconstruct record from group fields
            for (final var f : elemShape.fields()) {
                sb.append("                    final var ").append(f.name())
                        .append("__ = g__.").append(f.name()).append("();\n");
            }
            sb.append("                    ").append(varName).append(".add(new ").append(elemSimple).append("(");
            for (int i = 0; i < elemShape.fields().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(elemShape.fields().get(i).name()).append("__");
            }
            sb.append("));\n");
        } else {
            sb.append("                    ").append(varName).append(".add(g__.value());\n");
        }
        sb.append("                }\n");
    }

    private static String capitalize(final String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String simpleNameOf(final String fqn) {
        final var dot = fqn.lastIndexOf('.');
        return (dot >= 0) ? fqn.substring(dot + 1) : fqn;
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
