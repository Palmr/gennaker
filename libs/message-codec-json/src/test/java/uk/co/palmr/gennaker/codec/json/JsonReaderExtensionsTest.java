package uk.co.palmr.gennaker.codec.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the collection-iteration helpers added to {@link JsonReader}.
 */
class JsonReaderExtensionsTest {

    @Test
    void tryArrayEndReturnsTrueOnClosingBracket() {
        final var reader = new JsonReader("[]");
        reader.expectArrayStart();
        assertTrue(reader.tryArrayEnd());
    }

    @Test
    void tryArrayEndReturnsFalseWhenNotAtEnd() {
        final var reader = new JsonReader("[1]");
        reader.expectArrayStart();
        assertFalse(reader.tryArrayEnd());
        assertEquals(1, reader.readInt());
        assertTrue(reader.tryArrayEnd());
    }

    @Test
    void tryCommaOrArrayEndReturnsTrueAtEnd() {
        final var reader = new JsonReader("[1]");
        reader.expectArrayStart();
        reader.readInt();
        assertTrue(reader.tryCommaOrArrayEnd());
    }

    @Test
    void tryCommaOrArrayEndReturnsFalseAndConsumesComma() {
        final var reader = new JsonReader("[1,2]");
        reader.expectArrayStart();
        assertEquals(1, reader.readInt());
        assertFalse(reader.tryCommaOrArrayEnd());
        assertEquals(2, reader.readInt());
        assertTrue(reader.tryCommaOrArrayEnd());
    }

    @Test
    void tryObjectEndReturnsTrueOnClosingBrace() {
        final var reader = new JsonReader("{}");
        reader.expectObjectStart();
        assertTrue(reader.tryObjectEnd());
    }

    @Test
    void tryObjectEndReturnsFalseWhenFieldsRemain() {
        final var reader = new JsonReader("{\"k\":1}");
        reader.expectObjectStart();
        assertFalse(reader.tryObjectEnd());
        assertEquals("k", reader.readFieldName());
        assertEquals(1, reader.readInt());
        assertTrue(reader.tryObjectEnd());
    }

    @Test
    void tryCommaOrObjectEndReturnsTrueAtEnd() {
        final var reader = new JsonReader("{\"a\":1}");
        reader.expectObjectStart();
        reader.readFieldName();
        reader.readInt();
        assertTrue(reader.tryCommaOrObjectEnd());
    }

    @Test
    void tryCommaOrObjectEndReturnsFalseAndConsumesComma() {
        final var reader = new JsonReader("{\"a\":1,\"b\":2}");
        reader.expectObjectStart();
        assertEquals("a", reader.readFieldName());
        assertEquals(1, reader.readInt());
        assertFalse(reader.tryCommaOrObjectEnd());
        assertEquals("b", reader.readFieldName());
        assertEquals(2, reader.readInt());
        assertTrue(reader.tryCommaOrObjectEnd());
    }

    @Test
    void readFieldNameReadsKeyAndConsumesColon() {
        final var reader = new JsonReader("{\"myKey\":42}");
        reader.expectObjectStart();
        assertEquals("myKey", reader.readFieldName());
        assertEquals(42, reader.readInt());
    }

    @Test
    void nestedArrayRoundTrips() {
        final var sb = new StringBuilder();
        sb.append("[1,2,3]");
        final var reader = new JsonReader(sb.toString());
        reader.expectArrayStart();
        final var list = new java.util.ArrayList<Integer>();
        if (!reader.tryArrayEnd()) {
            list.add(reader.readInt());
            while (!reader.tryCommaOrArrayEnd()) {
                list.add(reader.readInt());
            }
        }
        assertEquals(java.util.List.of(1, 2, 3), list);
    }

    @Test
    void nestedObjectRoundTrips() {
        final var reader = new JsonReader("{\"x\":10,\"y\":20}");
        reader.expectObjectStart();
        final var map = new java.util.LinkedHashMap<String, Integer>();
        if (!reader.tryObjectEnd()) {
            final var k1 = reader.readFieldName();
            map.put(k1, reader.readInt());
            while (!reader.tryCommaOrObjectEnd()) {
                final var k = reader.readFieldName();
                map.put(k, reader.readInt());
            }
        }
        assertEquals(java.util.Map.of("x", 10, "y", 20), map);
    }
}
