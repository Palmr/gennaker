package uk.co.palmr.gennaker.codec.sbe;

import uk.co.palmr.gennaker.codec.FieldKind;

import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.Map;

class SbeTypes {
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

    public void addType(final String type, final String sbeType, final boolean variableLength) {
        customSbeTypes.put(type, new SbeType(sbeType, variableLength));
    }

    /**
     * Get the SBE type for a parameter element. Returns null if the type is a
     * user-defined type that needs flattening (record/Layout-described).
     */
    public SbeType getSbeTypeOrNull(final VariableElement variableElement) {
        final var type = variableElement.asType().toString();
        return getSbeTypeByName(type);
    }

    /**
     * Get the SBE type for a parameter element.
     * Throws if not a known primitive/String type.
     */
    public SbeType getSbeType(final VariableElement variableElement) {
        final var type = variableElement.asType().toString();
        final var result = getSbeTypeByName(type);
        if (result == null) {
            throw new UnsupportedOperationException("No SBE type to support: " + type);
        }
        return result;
    }

    /**
     * Look up an SBE type by Java type name. Returns null if not a known type.
     */
    public SbeType getSbeTypeByName(final String type) {
        final var maybeCustomType = customSbeTypes.get(type);
        if (maybeCustomType != null) {
            return maybeCustomType;
        }
        return BASE_SBE_TYPES.get(type);
    }

    /**
     * Check if the given field kind from a TypeShape represents a variable-length
     * SBE field.
     */
    public static boolean isVariableLength(final FieldKind kind, final String javaType) {
        if (kind == FieldKind.STRING) {
            return true;
        }
        if (kind == FieldKind.LIST || kind == FieldKind.MAP || kind == FieldKind.OBJECT) {
            return true;
        }
        return false;
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
