package uk.co.palmr.gennaker.annotation.processor.sbe;

import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.Map;

public class SbeTypes {
    private static final Map<String, SbeType> BASE_SBE_TYPES = Map.of(
            "boolean", new SbeType("BooleanType", false),
            "char", new SbeType("char", false),
            "byte", new SbeType("int8", false),
            "short", new SbeType("int16", false),
            "int", new SbeType("int32", false),
            "long", new SbeType("int64", false),
            "float", new SbeType("float", false),
            "double", new SbeType("double", false),
            "java.lang.String", new SbeType("varStringEncoding", true)
    );

    private final Map<String, SbeType> customSbeTypes = new HashMap<>();

    // TODO: Add support for custom SBE serialisations
    public void addType(final String type, final String sbeType, final boolean variableLength) {
        customSbeTypes.put(type, new SbeType(sbeType, variableLength));
    }

    public SbeType getSbeType(final VariableElement variableElement) {
        final var type = variableElement.asType().toString();

        final var maybeCustomType = customSbeTypes.get(type);
        if (maybeCustomType == null) {
            final var maybeBaseType = BASE_SBE_TYPES.get(type);
            if (maybeBaseType == null) {
                throw new UnsupportedOperationException("No SBE type to support: " + type);
            }
            return maybeBaseType;
        }
        return maybeCustomType;
    }

    public record SbeType(String sbeType, boolean variableLength) implements Comparable<SbeType> {
        @Override
        public int compareTo(final SbeType otherType) {
            if (variableLength && otherType.variableLength) {
                return 0;
            } else if (variableLength && !otherType.variableLength) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
