package uk.co.palmr.gennaker.codec;

/**
 * Classifies a field in a {@link TypeShape}.
 */
public enum FieldKind {
    /** A Java primitive ({@code int}, {@code long}, {@code double}, ...). */
    PRIMITIVE,
    /** {@link String}. */
    STRING,
    /** A nested record or Layout-described object. */
    OBJECT,
    /** A homogeneous {@link java.util.List}. */
    LIST,
    /** A homogeneous {@link java.util.Map}. */
    MAP
}
