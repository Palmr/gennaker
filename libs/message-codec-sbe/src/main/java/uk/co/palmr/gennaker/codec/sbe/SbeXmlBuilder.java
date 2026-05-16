package uk.co.palmr.gennaker.codec.sbe;

import uk.co.palmr.gennaker.codec.FieldKind;
import uk.co.palmr.gennaker.codec.FieldShape;
import uk.co.palmr.gennaker.codec.TypeShape;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SbeXmlBuilder {
    private final String packageName;
    private final String interfaceName;
    private final SbeTypes sbeTypes;
    private final List<ExecutableElement> methods = new ArrayList<>();
    private Map<String, TypeShape> reachableTypes = Map.of();

    private SbeXmlBuilder(final String packageName, final String interfaceName, final SbeTypes sbeTypes) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.sbeTypes = sbeTypes;
    }

    public void addType(final String type, final String sbeType, final boolean variableLength) {
        sbeTypes.addType(type, sbeType, variableLength);
    }

    public void method(final ExecutableElement methodElement) {
        methods.add(methodElement);
    }

    public void setReachableTypes(final Map<String, TypeShape> types) {
        this.reachableTypes = Map.copyOf(types);
    }

    public static SbeXmlBuilder newBuilder(final String packageName, final String interfaceName) {
        return new SbeXmlBuilder(packageName, interfaceName, new SbeTypes());
    }

    private void writeMethods(final Writer writer) throws IOException {
        for (var methodIdx = 0; methodIdx < methods.size(); methodIdx++) {
            final var methodElement = methods.get(methodIdx);
            final var methodName = methodElement.getSimpleName().toString();

            writer.write("    <sbe:message name=\"");
            writer.write(methodName);
            writer.write("\" id=\"");
            writer.write(Integer.toString(methodIdx));
            writer.write("\" description=\"method call for ");
            writer.write(packageName);
            writer.write(".");
            writer.write(interfaceName);
            writer.write("::");
            writer.write(methodName);
            writer.write("\">\n");

            writeMethodFields(writer, methodElement);

            writer.write("    </sbe:message>\n");
        }
    }

    private void writeMethodFields(final Writer writer, final ExecutableElement methodElement) throws IOException {
        // Collect all fields (flattening records) and sort: fixed-length first, then groups/data
        final var fixedFields = new ArrayList<FlatField>();
        final var varFields = new ArrayList<FlatField>();
        final var groupFields = new ArrayList<FlatField>();

        for (final var param : methodElement.getParameters()) {
            flattenParam(param, "", fixedFields, varFields, groupFields);
        }

        var fieldId = 0;
        for (final var field : fixedFields) {
            writeField(writer, field, fieldId++);
        }
        for (final var field : groupFields) {
            writeGroup(writer, field, fieldId++);
        }
        for (final var field : varFields) {
            writeDataField(writer, field, fieldId++);
        }
    }

    private void flattenParam(final VariableElement param,
                              final String prefix,
                              final List<FlatField> fixedFields,
                              final List<FlatField> varFields,
                              final List<FlatField> groupFields) {
        final var paramName = param.getSimpleName().toString();
        final var paramType = param.asType().toString();
        final var sbeType = sbeTypes.getSbeTypeOrNull(param);

        if (sbeType != null) {
            final var flatName = prefix + paramName;
            if (sbeType.variableLength()) {
                varFields.add(new FlatField(flatName, sbeType.sbeType(), false));
            } else {
                fixedFields.add(new FlatField(flatName, sbeType.sbeType(), false));
            }
        } else {
            flattenUserType(paramName, paramType, prefix, fixedFields, varFields, groupFields);
        }
    }

    private void flattenUserType(final String paramName,
                                 final String paramType,
                                 final String prefix,
                                 final List<FlatField> fixedFields,
                                 final List<FlatField> varFields,
                                 final List<FlatField> groupFields) {
        final var shape = reachableTypes.get(paramType);
        if (shape == null) {
            return;
        }
        final var fieldPrefix = prefix + paramName;
        for (final var field : shape.fields()) {
            final var flatName = fieldPrefix + capitalize(field.name());
            flattenShapeField(flatName, field, fixedFields, varFields, groupFields);
        }
    }

    private void flattenShapeField(final String flatName,
                                   final FieldShape field,
                                   final List<FlatField> fixedFields,
                                   final List<FlatField> varFields,
                                   final List<FlatField> groupFields) {
        switch (field.kind()) {
            case PRIMITIVE -> {
                final var sbeTypeName = sbeTypes.getSbeTypeByName(field.javaType());
                if (sbeTypeName != null) {
                    fixedFields.add(new FlatField(flatName, sbeTypeName.sbeType(), false));
                }
            }
            case STRING -> varFields.add(new FlatField(flatName, "varStringEncoding", false));
            case OBJECT -> flattenNestedObject(flatName, field, fixedFields, varFields, groupFields);
            case LIST -> groupFields.add(new FlatField(flatName, null, true, field));
            case MAP -> groupFields.add(new FlatField(flatName, null, true, field));
        }
    }

    private void flattenNestedObject(final String prefix,
                                     final FieldShape field,
                                     final List<FlatField> fixedFields,
                                     final List<FlatField> varFields,
                                     final List<FlatField> groupFields) {
        final var shape = (field.nestedShape() != null)
                ? field.nestedShape()
                : reachableTypes.get(field.javaType());
        if (shape == null) {
            return;
        }
        for (final var nested : shape.fields()) {
            final var flatName = prefix + capitalize(nested.name());
            flattenShapeField(flatName, nested, fixedFields, varFields, groupFields);
        }
    }

    private static void writeField(final Writer writer, final FlatField field, final int id) throws IOException {
        writer.write("        <field name=\"");
        writer.write(field.name);
        writer.write("\" id=\"");
        writer.write(Integer.toString(id));
        writer.write("\" type=\"");
        writer.write(field.sbeType);
        writer.write("\"/>\n");
    }

    private static void writeDataField(final Writer writer, final FlatField field, final int id) throws IOException {
        writer.write("        <data name=\"");
        writer.write(field.name);
        writer.write("\" id=\"");
        writer.write(Integer.toString(id));
        writer.write("\" type=\"");
        writer.write(field.sbeType);
        writer.write("\"/>\n");
    }

    private void writeGroup(final Writer writer, final FlatField field, final int id) throws IOException {
        writer.write("        <group name=\"");
        writer.write(field.name);
        writer.write("\" id=\"");
        writer.write(Integer.toString(id));
        writer.write("\" dimensionType=\"groupSizeEncoding\">\n");

        if (field.shape != null && field.shape.kind() == FieldKind.LIST) {
            writeGroupBodyForList(writer, field.shape);
        } else if (field.shape != null && field.shape.kind() == FieldKind.MAP) {
            writeGroupBodyForMap(writer, field.shape);
        }

        writer.write("        </group>\n");
    }

    private void writeGroupBodyForList(final Writer writer, final FieldShape field) throws IOException {
        final var elemType = field.javaType();
        final var elemShape = (field.nestedShape() != null)
                ? field.nestedShape()
                : reachableTypes.get(elemType);

        if (elemShape != null) {
            writeGroupFieldsFromShape(writer, elemShape);
        } else {
            writeSimpleGroupField(writer, elemType, "value");
        }
    }

    private void writeGroupBodyForMap(final Writer writer, final FieldShape field) throws IOException {
        // Key field
        writeSimpleGroupField(writer, field.keyType(), "key");
        // Value field
        final var valShape = (field.nestedShape() != null)
                ? field.nestedShape()
                : reachableTypes.get(field.javaType());
        if (valShape != null) {
            writeGroupFieldsFromShape(writer, valShape);
        } else {
            writeSimpleGroupField(writer, field.javaType(), "value");
        }
    }

    private void writeGroupFieldsFromShape(final Writer writer, final TypeShape shape) throws IOException {
        var groupFieldId = 0;
        final var fixedFields = new ArrayList<FlatField>();
        final var varFields = new ArrayList<FlatField>();

        for (final var f : shape.fields()) {
            switch (f.kind()) {
                case PRIMITIVE -> {
                    final var sbeType = sbeTypes.getSbeTypeByName(f.javaType());
                    if (sbeType != null) {
                        fixedFields.add(new FlatField(f.name(), sbeType.sbeType(), false));
                    }
                }
                case STRING -> varFields.add(new FlatField(f.name(), "varStringEncoding", false));
                default -> varFields.add(new FlatField(f.name(), "varStringEncoding", false));
            }
        }

        for (final var f : fixedFields) {
            writer.write("            <field name=\"");
            writer.write(f.name);
            writer.write("\" id=\"");
            writer.write(Integer.toString(groupFieldId++));
            writer.write("\" type=\"");
            writer.write(f.sbeType);
            writer.write("\"/>\n");
        }
        for (final var f : varFields) {
            writer.write("            <data name=\"");
            writer.write(f.name);
            writer.write("\" id=\"");
            writer.write(Integer.toString(groupFieldId++));
            writer.write("\" type=\"");
            writer.write(f.sbeType);
            writer.write("\"/>\n");
        }
    }

    private void writeSimpleGroupField(final Writer writer,
                                       final String javaType,
                                       final String fieldName) throws IOException {
        final var sbeType = sbeTypes.getSbeTypeByName(javaType);
        if (sbeType != null && sbeType.variableLength()) {
            writer.write("            <data name=\"" + fieldName + "\" id=\"0\" type=\"" + sbeType.sbeType() + "\"/>\n");
        } else if (sbeType != null) {
            writer.write("            <field name=\"" + fieldName + "\" id=\"0\" type=\"" + sbeType.sbeType() + "\"/>\n");
        }
    }

    private void writeSbeHeader(final Writer writer) throws IOException {
        writer.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <sbe:messageSchema xmlns:sbe="http://fixprotocol.io/2016/sbe"
                                   xmlns:xi="http://www.w3.org/2001/XInclude"
                                   package="baseline"
                                   id="1"
                                   version="0"
                                   semanticVersion="5.2"
                                   description="SBE Schema for Gennaker topic:\s""");
        writer.write(packageName);
        writer.write(".");
        writer.write(interfaceName);
        writer.write("""
                ."
                                   byteOrder="littleEndian">
                    <types>
                        <composite name="messageHeader" description="Message identifiers and length of message root.">
                            <type name="blockLength" primitiveType="uint16"/>
                            <type name="templateId" primitiveType="uint16"/>
                            <type name="schemaId" primitiveType="uint16"/>
                            <type name="version" primitiveType="uint16"/>
                        </composite>
                        <composite name="groupSizeEncoding" description="Repeating group dimensions.">
                            <type name="blockLength" primitiveType="uint16"/>
                            <type name="numInGroup" primitiveType="uint16"/>
                        </composite>
                        <composite name="varStringEncoding" description="Variable length UTF-8 String.">
                            <type name="length" primitiveType="uint32" maxValue="1073741824"/>
                            <type name="varData" primitiveType="uint8" length="0" characterEncoding="UTF-8"/>
                        </composite>
                        <enum name="BooleanType" encodingType="uint8" semanticType="Boolean">
                            <validValue name="FALSE">0</validValue>
                            <validValue name="TRUE">1</validValue>
                        </enum>
                    </types>
                """);
    }

    public void write(final Writer writer) throws IOException {
        writeSbeHeader(writer);
        writeMethods(writer);
        writer.write("</sbe:messageSchema>");
    }

    private static String capitalize(final String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record FlatField(String name, String sbeType, boolean isGroup, FieldShape shape) {
        FlatField(final String name, final String sbeType, final boolean isGroup) {
            this(name, sbeType, isGroup, null);
        }
    }
}
