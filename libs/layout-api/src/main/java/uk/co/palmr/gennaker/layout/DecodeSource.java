package uk.co.palmr.gennaker.layout;

import java.util.List;
import java.util.Map;

/**
 * Provides named, typed values during decoding. Each {@code readX} call
 * corresponds to one field in the wire layout.
 *
 * <p>Implementations are provided by the message codecs (SBE, JSON, ...).
 * Users call these methods inside their {@link Layout#decode} body; the
 * annotation processor reads the call sequence at compile time to derive
 * the type's shape.
 */
public interface DecodeSource {

    /**
     * Reads a {@code boolean} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    boolean readBoolean(String name);

    /**
     * Reads a {@code byte} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    byte readByte(String name);

    /**
     * Reads a {@code short} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    short readShort(String name);

    /**
     * Reads an {@code int} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    int readInt(String name);

    /**
     * Reads a {@code long} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    long readLong(String name);

    /**
     * Reads a {@code float} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    float readFloat(String name);

    /**
     * Reads a {@code double} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    double readDouble(String name);

    /**
     * Reads a {@code char} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    char readChar(String name);

    /**
     * Reads a {@link String} field.
     *
     * @param name the wire name of the field
     * @return the decoded value
     */
    String readString(String name);

    /**
     * Reads a nested record or Layout-described value.
     *
     * @param name the wire name of the field
     * @param type the expected Java type of the nested value
     * @param <U>  the nested value type
     * @return the decoded value
     */
    <U> U readObject(String name, Class<U> type);

    /**
     * Reads a homogeneous list.
     *
     * @param name        the wire name of the field
     * @param elementType the element type
     * @param <U>         the element type
     * @return the decoded list
     */
    <U> List<U> readList(String name, Class<U> elementType);

    /**
     * Reads a homogeneous map.
     *
     * @param name      the wire name of the field
     * @param keyType   the key type
     * @param valueType the value type
     * @param <K>       the key type
     * @param <V>       the value type
     * @return the decoded map
     */
    <K, V> Map<K, V> readMap(String name, Class<K> keyType, Class<V> valueType);
}
