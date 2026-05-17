package uk.co.palmr.gennaker.layout;

import java.util.List;
import java.util.Map;

/**
 * Receives named, typed values during encoding. Each {@code writeX} call
 * corresponds to one field in the wire layout.
 *
 * <p>Implementations are provided by the message codecs (SBE, JSON, ...).
 * Users call these methods inside their {@link Layout#encode} body; the
 * annotation processor reads the call sequence at compile time to derive
 * the type's shape.
 */
public interface EncodeSink {

    /**
     * Writes a {@code boolean} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeBoolean(String name, boolean value);

    /**
     * Writes a {@code byte} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeByte(String name, byte value);

    /**
     * Writes a {@code short} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeShort(String name, short value);

    /**
     * Writes an {@code int} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeInt(String name, int value);

    /**
     * Writes a {@code long} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeLong(String name, long value);

    /**
     * Writes a {@code float} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeFloat(String name, float value);

    /**
     * Writes a {@code double} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeDouble(String name, double value);

    /**
     * Writes a {@code char} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeChar(String name, char value);

    /**
     * Writes a {@link String} field.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     */
    void writeString(String name, String value);

    /**
     * Writes a nested record or Layout-described value.
     *
     * @param name  the wire name of the field
     * @param value the value to encode
     * @param <U>   the nested value type
     */
    <U> void writeObject(String name, U value);

    /**
     * Writes a homogeneous list. The element type is inferred from the static
     * type at the call site.
     *
     * @param name   the wire name of the field
     * @param values the list to encode
     * @param <U>    the element type
     */
    <U> void writeList(String name, List<U> values);

    /**
     * Writes a homogeneous map.
     *
     * <p>Key type must be a primitive wrapper or {@link String}: SBE groups
     * need fixed-position fields for keys, and JSON object keys are strings.
     * Composite keys ({@code Map<MyRecord, V>}) are rejected at compile time.
     *
     * <p>SBE lowers this to a repeating group of (key, value) entries;
     * JSON emits a JSON object.
     *
     * @param name   the wire name of the field
     * @param values the map to encode
     * @param <K>    the key type
     * @param <V>    the value type
     */
    <K, V> void writeMap(String name, Map<K, V> values);
}
