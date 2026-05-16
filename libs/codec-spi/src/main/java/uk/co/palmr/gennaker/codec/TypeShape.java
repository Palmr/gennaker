package uk.co.palmr.gennaker.codec;

import java.util.List;

/**
 * The wire-level shape of a user-defined type: an ordered list of named,
 * typed fields.
 *
 * <p>Produced by the annotation processor (from a record's components or
 * from a hand-written {@code Layout}'s AST) and consumed by codec generators
 * to emit schema entries and layout helper classes.
 *
 * @param wireName  the on-the-wire name for this type (defaults to the
 *                  simple Java class name; overridden by {@code @WireName})
 * @param javaType  the fully-qualified Java type (e.g.
 *                  {@code "com.example.Order"})
 * @param fields    ordered list of fields in this type
 */
public record TypeShape(
        String wireName,
        String javaType,
        List<FieldShape> fields
) {
    public TypeShape {
        fields = List.copyOf(fields);
    }
}
