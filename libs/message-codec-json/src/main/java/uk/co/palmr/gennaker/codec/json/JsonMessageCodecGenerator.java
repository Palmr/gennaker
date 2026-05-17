package uk.co.palmr.gennaker.codec.json;

import com.google.auto.service.AutoService;
import uk.co.palmr.gennaker.annotations.Codecs;
import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.FieldKind;
import uk.co.palmr.gennaker.codec.FieldShape;
import uk.co.palmr.gennaker.codec.MessageCodecGenerator;
import uk.co.palmr.gennaker.codec.TopicContext;
import uk.co.palmr.gennaker.codec.TypeShape;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * JSON {@link MessageCodecGenerator}. Generates a {@code __layout} helper
 * class per reachable record type and returns a {@link JsonCodecBodyEmitter}
 * that wires those helpers into the publisher and subscriber proxies.
 *
 * <p>Selected per topic with {@code @Topic(messageCodec = Codecs.JSON)}.
 *
 * <p>Registered as a service via {@link AutoService} — adding this module to
 * the {@code annotationProcessor} configuration is all that's required.
 */
@AutoService(MessageCodecGenerator.class)
public final class JsonMessageCodecGenerator implements MessageCodecGenerator {

    /** Public no-arg constructor required by {@link java.util.ServiceLoader}. */
    public JsonMessageCodecGenerator() {
    }

    @Override
    public String name() {
        return Codecs.JSON;
    }

    @Override
    public CodecBodyEmitter generate(final TopicContext context) {
        final var reachableTypes = context.reachableTypes();
        for (final var entry : reachableTypes.entrySet()) {
            generateLayoutFile(context, entry.getKey(), entry.getValue(), reachableTypes);
        }
        return new JsonCodecBodyEmitter(
                context.interfaceName(), context.maxMessageSize(), context.methods(), reachableTypes);
    }

    private void generateLayoutFile(final TopicContext context,
                                    final String javaType,
                                    final TypeShape shape,
                                    final Map<String, TypeShape> allShapes) {
        final var lastDot = javaType.lastIndexOf('.');
        final var packageName = (lastDot >= 0) ? javaType.substring(0, lastDot) : "";
        final var simpleName = (lastDot >= 0) ? javaType.substring(lastDot + 1) : javaType;
        final var layoutClassName = simpleName + "__layout";
        final var fqn = packageName.isEmpty() ? layoutClassName : packageName + "." + layoutClassName;

        try {
            final var file = context.processingEnv().getFiler()
                    .createSourceFile(fqn, context.topicInterface());
            try (var w = file.openWriter()) {
                writeLayoutClass(w, packageName, simpleName, layoutClassName, shape, allShapes);
            }
        } catch (final IOException ex) {
            context.processingEnv().getMessager().printError("Failed to write " + fqn + ": " + ex);
        }
    }

    private void writeLayoutClass(final Writer w,
                                  final String packageName,
                                  final String simpleName,
                                  final String layoutClassName,
                                  final TypeShape shape,
                                  final Map<String, TypeShape> allShapes) throws IOException {
        if (!packageName.isEmpty()) {
            w.write("package ");
            w.write(packageName);
            w.write(";\n\n");
        }
        w.write("import uk.co.palmr.gennaker.codec.json.JsonReader;\n");
        w.write("import uk.co.palmr.gennaker.codec.json.JsonWriter;\n\n");
        w.write("/**\n * Generated layout helper for {@link ");
        w.write(simpleName);
        w.write("}.\n */\n");
        w.write("final class ");
        w.write(layoutClassName);
        w.write(" {\n");
        w.write("    private ");
        w.write(layoutClassName);
        w.write("() {}\n\n");

        writeEncodeMethod(w, simpleName, shape, allShapes);
        w.write("\n");
        writeDecodeMethod(w, simpleName, shape, allShapes);

        w.write("}\n");
    }

    private void writeEncodeMethod(final Writer w,
                                   final String simpleName,
                                   final TypeShape shape,
                                   final Map<String, TypeShape> allShapes) throws IOException {
        w.write("    static void encode(final ");
        w.write(simpleName);
        w.write(" value, final StringBuilder json) {\n");
        w.write("        json.append('{');\n");

        final var fields = shape.fields();
        for (int i = 0; i < fields.size(); i++) {
            final var field = fields.get(i);
            if (i > 0) {
                w.write("        json.append(',');\n");
            }
            w.write("        json.append(\"\\\"");
            w.write(field.name());
            w.write("\\\":\");\n");
            writeFieldEncode(w, "value." + field.javaName() + "()", field, allShapes);
        }

        w.write("        json.append('}');\n");
        w.write("    }\n");
    }

    private void writeFieldEncode(final Writer w,
                                  final String accessor,
                                  final FieldShape field,
                                  final Map<String, TypeShape> allShapes) throws IOException {
        switch (field.kind()) {
            case PRIMITIVE -> w.write("        json.append(" + accessor + ");\n");
            case STRING -> w.write("        JsonWriter.writeString(" + accessor + ", json);\n");
            case OBJECT -> {
                final var nestedSimple = simpleNameOf(field.javaType());
                w.write("        " + nestedSimple + "__layout.encode(" + accessor + ", json);\n");
            }
            case LIST -> writeListEncode(w, accessor, field, allShapes);
            case MAP -> writeMapEncode(w, accessor, field, allShapes);
        }
    }

    private void writeListEncode(final Writer w,
                                 final String accessor,
                                 final FieldShape field,
                                 final Map<String, TypeShape> allShapes) throws IOException {
        w.write("        json.append('[');\n");
        w.write("        for (int i__ = 0; i__ < " + accessor + ".size(); i__++) {\n");
        w.write("            if (i__ > 0) json.append(',');\n");
        final var elemField = new FieldShape("_elem", elementFieldKind(field.javaType()), field.javaType(), field.nestedShape());
        writeFieldEncodeIndented(w, accessor + ".get(i__)", elemField, allShapes, "            ");
        w.write("        }\n");
        w.write("        json.append(']');\n");
    }

    private void writeMapEncode(final Writer w,
                                final String accessor,
                                final FieldShape field,
                                final Map<String, TypeShape> allShapes) throws IOException {
        w.write("        json.append('{');\n");
        w.write("        boolean first__ = true;\n");
        w.write("        for (var entry__ : " + accessor + ".entrySet()) {\n");
        w.write("            if (!first__) json.append(',');\n");
        w.write("            first__ = false;\n");
        // Key is always String-coerced for JSON objects
        w.write("            JsonWriter.writeString(String.valueOf(entry__.getKey()), json);\n");
        w.write("            json.append(':');\n");
        final var valField = new FieldShape("_val", elementFieldKind(field.javaType()), field.javaType(), field.nestedShape());
        writeFieldEncodeIndented(w, "entry__.getValue()", valField, allShapes, "            ");
        w.write("        }\n");
        w.write("        json.append('}');\n");
    }

    private void writeFieldEncodeIndented(final Writer w,
                                          final String accessor,
                                          final FieldShape field,
                                          final Map<String, TypeShape> allShapes,
                                          final String indent) throws IOException {
        switch (field.kind()) {
            case PRIMITIVE -> w.write(indent + "json.append(" + accessor + ");\n");
            case STRING -> w.write(indent + "JsonWriter.writeString(" + accessor + ", json);\n");
            case OBJECT -> {
                final var nestedSimple = simpleNameOf(field.javaType());
                w.write(indent + nestedSimple + "__layout.encode(" + accessor + ", json);\n");
            }
            default -> w.write(indent + "json.append(" + accessor + ");\n");
        }
    }

    private void writeDecodeMethod(final Writer w,
                                   final String simpleName,
                                   final TypeShape shape,
                                   final Map<String, TypeShape> allShapes) throws IOException {
        w.write("    static ");
        w.write(simpleName);
        w.write(" decode(final JsonReader reader) {\n");
        w.write("        reader.expectObjectStart();\n");

        final var fields = shape.fields();
        for (int i = 0; i < fields.size(); i++) {
            final var field = fields.get(i);
            if (i > 0) {
                w.write("        reader.expectComma();\n");
            }
            w.write("        reader.expectKey(\"");
            w.write(field.name());
            w.write("\");\n");
            writeFieldDecode(w, field, allShapes);
        }

        w.write("        reader.expectObjectEnd();\n");
        w.write("        return new ");
        w.write(simpleName);
        w.write("(");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                w.write(", ");
            }
            w.write(fields.get(i).name());
        }
        w.write(");\n");
        w.write("    }\n");
    }

    private void writeFieldDecode(final Writer w,
                                  final FieldShape field,
                                  final Map<String, TypeShape> allShapes) throws IOException {
        final var varName = field.name();
        switch (field.kind()) {
            case PRIMITIVE -> {
                w.write("        final ");
                w.write(field.javaType());
                w.write(" ");
                w.write(varName);
                w.write(" = reader.");
                w.write(readerCall(field.javaType()));
                w.write(";\n");
            }
            case STRING -> {
                w.write("        final String ");
                w.write(varName);
                w.write(" = reader.readString();\n");
            }
            case OBJECT -> {
                final var nestedSimple = simpleNameOf(field.javaType());
                w.write("        final ");
                w.write(nestedSimple);
                w.write(" ");
                w.write(varName);
                w.write(" = ");
                w.write(nestedSimple);
                w.write("__layout.decode(reader);\n");
            }
            case LIST -> writeListDecode(w, field, allShapes);
            case MAP -> writeMapDecode(w, field, allShapes);
        }
    }

    private void writeListDecode(final Writer w,
                                 final FieldShape field,
                                 final Map<String, TypeShape> allShapes) throws IOException {
        final var varName = field.name();
        final var elemType = field.javaType();
        w.write("        final java.util.List<" + boxedType(elemType) + "> " + varName +
                " = new java.util.ArrayList<>();\n");
        w.write("        reader.expectArrayStart();\n");
        w.write("        if (!reader.tryArrayEnd()) {\n");
        w.write("            " + varName + ".add(" + decodeExpression(elemType) + ");\n");
        w.write("            while (!reader.tryCommaOrArrayEnd()) {\n");
        w.write("                " + varName + ".add(" + decodeExpression(elemType) + ");\n");
        w.write("            }\n");
        w.write("        }\n");
    }

    private void writeMapDecode(final Writer w,
                                final FieldShape field,
                                final Map<String, TypeShape> allShapes) throws IOException {
        final var varName = field.name();
        final var keyType = field.keyType();
        final var valType = field.javaType();
        w.write("        final java.util.Map<" + boxedType(keyType) + ", " + boxedType(valType) + "> " +
                varName + " = new java.util.LinkedHashMap<>();\n");
        w.write("        reader.expectObjectStart();\n");
        w.write("        if (!reader.tryObjectEnd()) {\n");
        w.write("            String key__ = reader.readFieldName();\n");
        w.write("            " + varName + ".put(" + parseKey(keyType, "key__") + ", " + decodeExpression(valType) + ");\n");
        w.write("            while (!reader.tryCommaOrObjectEnd()) {\n");
        w.write("                key__ = reader.readFieldName();\n");
        w.write("                " + varName + ".put(" + parseKey(keyType, "key__") + ", " + decodeExpression(valType) + ");\n");
        w.write("            }\n");
        w.write("        }\n");
    }

    private static String decodeExpression(final String javaType) {
        if ("java.lang.String".equals(javaType)) {
            return "reader.readString()";
        }
        if (isJsonPrimitive(javaType) || isBoxedPrimitive(javaType)) {
            return "reader." + readerCall(javaType);
        }
        return simpleNameOf(javaType) + "__layout.decode(reader)";
    }

    private static String parseKey(final String keyType, final String varName) {
        if ("java.lang.String".equals(keyType)) {
            return varName;
        }
        return switch (keyType) {
            case "int", "java.lang.Integer" -> "Integer.parseInt(" + varName + ")";
            case "long", "java.lang.Long" -> "Long.parseLong(" + varName + ")";
            default -> varName;
        };
    }

    private static String readerCall(final String type) {
        return switch (type) {
            case "boolean", "java.lang.Boolean" -> "readBoolean()";
            case "byte", "java.lang.Byte" -> "readByte()";
            case "short", "java.lang.Short" -> "readShort()";
            case "int", "java.lang.Integer" -> "readInt()";
            case "long", "java.lang.Long" -> "readLong()";
            case "float", "java.lang.Float" -> "readFloat()";
            case "double", "java.lang.Double" -> "readDouble()";
            case "char", "java.lang.Character" -> "readChar()";
            default -> "readString()";
        };
    }

    private static boolean isJsonPrimitive(final String type) {
        return switch (type) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
            default -> false;
        };
    }

    private static boolean isBoxedPrimitive(final String type) {
        return switch (type) {
            case "java.lang.Boolean", "java.lang.Byte", "java.lang.Short",
                    "java.lang.Integer", "java.lang.Long", "java.lang.Float",
                    "java.lang.Double", "java.lang.Character" -> true;
            default -> false;
        };
    }

    private static FieldKind elementFieldKind(final String javaType) {
        if ("java.lang.String".equals(javaType)) {
            return FieldKind.STRING;
        } else if (isJsonPrimitive(javaType) || isBoxedPrimitive(javaType)) {
            return FieldKind.PRIMITIVE;
        }
        return FieldKind.OBJECT;
    }

    private static String simpleNameOf(final String fqn) {
        final var dot = fqn.lastIndexOf('.');
        return (dot >= 0) ? fqn.substring(dot + 1) : fqn;
    }

    private static String boxedType(final String type) {
        return switch (type) {
            case "boolean" -> "Boolean";
            case "byte" -> "Byte";
            case "short" -> "Short";
            case "int" -> "Integer";
            case "long" -> "Long";
            case "float" -> "Float";
            case "double" -> "Double";
            case "char" -> "Character";
            default -> simpleNameOf(type);
        };
    }
}
