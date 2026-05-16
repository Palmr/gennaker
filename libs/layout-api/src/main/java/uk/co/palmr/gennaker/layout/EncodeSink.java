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

    void writeBoolean(String name, boolean value);

    void writeByte(String name, byte value);

    void writeShort(String name, short value);

    void writeInt(String name, int value);

    void writeLong(String name, long value);

    void writeFloat(String name, float value);

    void writeDouble(String name, double value);

    void writeChar(String name, char value);

    void writeString(String name, String value);

    /**
     * Write a nested record or Layout-described value.
     */
    <U> void writeObject(String name, U value);

    /**
     * Write a homogeneous list. The element type is inferred from the static
     * type at the call site.
     */
    <U> void writeList(String name, List<U> values);

    /**
     * Write a homogeneous map.
     *
     * <p>Key type must be a primitive wrapper or {@link String}: SBE groups
     * need fixed-position fields for keys, and JSON object keys are strings.
     * Composite keys ({@code Map<MyRecord, V>}) are rejected at compile time.
     *
     * <p>SBE lowers this to a repeating group of (key, value) entries;
     * JSON emits a JSON object.
     */
    <K, V> void writeMap(String name, Map<K, V> values);
}
