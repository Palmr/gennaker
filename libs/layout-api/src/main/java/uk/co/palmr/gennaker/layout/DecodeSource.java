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

    boolean readBoolean(String name);

    byte readByte(String name);

    short readShort(String name);

    int readInt(String name);

    long readLong(String name);

    float readFloat(String name);

    double readDouble(String name);

    char readChar(String name);

    String readString(String name);

    /**
     * Read a nested record or Layout-described value.
     */
    <U> U readObject(String name, Class<U> type);

    /**
     * Read a homogeneous list.
     */
    <U> List<U> readList(String name, Class<U> elementType);

    /**
     * Read a homogeneous map.
     */
    <K, V> Map<K, V> readMap(String name, Class<K> keyType, Class<V> valueType);
}
