package uk.co.palmr.gennaker.annotation.processor.sbe;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class SbeXmlBuilder {
    private final String packageName;
    private final String interfaceName;
    private final SbeTypes sbeTypes;
    private final List<ExecutableElement> methods = new ArrayList<>();

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

            final List<? extends VariableElement> parameters = new ArrayList<>(methodElement.getParameters());
            parameters.sort((p1, p2) -> sbeTypes.getSbeType(p1).compareTo(sbeTypes.getSbeType(p2)));

            for (var paramIdx = 0; paramIdx < parameters.size(); paramIdx++) {
                final var param = parameters.get(paramIdx);
                final var sbeType = sbeTypes.getSbeType(param);
                if (sbeType.variableLength()) {
                    writer.write("        <data name=\"");
                }
                else {
                    writer.write("        <field name=\"");
                }
                writer.write(param.getSimpleName().toString());
                writer.write("\" id=\"");
                writer.write(Integer.toString(paramIdx));
                writer.write("\" type=\"");
                writer.write(sbeType.sbeType());
                writer.write("\"/>\n");
            }

            writer.write("    </sbe:message>\n");
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
}
