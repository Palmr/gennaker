package uk.co.palmr.gennaker.codec;

/**
 * One field in a {@link TypeShape}.
 *
 * @param name        wire name of the field (may differ from {@code javaName}
 *                    when {@code @WireName} is present on the record component)
 * @param javaName    Java field/accessor name on the source record; equals
 *                    {@code name} when no {@code @WireName} is applied
 * @param kind        classification (primitive, string, object, list, map)
 * @param javaType    fully-qualified Java type string (e.g. {@code "int"},
 *                    {@code "java.lang.String"}, {@code "com.example.Leg"})
 * @param nestedShape for {@link FieldKind#OBJECT} and list/map element types:
 *                    the shape of the nested type; {@code null} for primitives
 *                    and strings
 * @param keyType     for {@link FieldKind#MAP}: the fully-qualified key type;
 *                    {@code null} otherwise
 * @param keyShape    for {@link FieldKind#MAP} with a non-primitive key:
 *                    the shape of the key type; {@code null} otherwise
 */
public record FieldShape(
        String name,
        String javaName,
        FieldKind kind,
        String javaType,
        TypeShape nestedShape,
        String keyType,
        TypeShape keyShape
) {
    /**
     * Convenience constructor for primitive, string, and object fields (no
     * key type). Sets {@code javaName} equal to {@code name}.
     */
    public FieldShape(final String name, final FieldKind kind, final String javaType, final TypeShape nestedShape) {
        this(name, name, kind, javaType, nestedShape, null, null);
    }
}
