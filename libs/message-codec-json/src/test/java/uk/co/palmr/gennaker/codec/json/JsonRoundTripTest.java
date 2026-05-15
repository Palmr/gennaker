package uk.co.palmr.gennaker.codec.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Round-trip {@link JsonWriter} → {@link JsonReader} for every supported
 * primitive plus {@link String}.
 */
class JsonRoundTripTest {

    @Test
    void plainStringRoundTrips() {
        final var sb = new StringBuilder();
        JsonWriter.writeString("hello world", sb);
        assertEquals("hello world", new JsonReader(sb.toString()).readString());
    }

    @Test
    void stringWithQuotesAndBackslashesRoundTrips() {
        final var input = "she said \"hi\" \\ then left";
        final var sb = new StringBuilder();
        JsonWriter.writeString(input, sb);
        assertEquals(input, new JsonReader(sb.toString()).readString());
    }

    @Test
    void controlCharactersRoundTripViaEscapeSequences() {
        final var input = "line1\nline2\tcol\rfin";
        final var sb = new StringBuilder();
        JsonWriter.writeString(input, sb);
        final var encoded = sb.toString();
        assertEquals(-1, encoded.indexOf('\n'));
        assertEquals(-1, encoded.indexOf('\t'));
        assertEquals(input, new JsonReader(encoded).readString());
    }

    @Test
    void nullStringSerialisesAsJsonNullLiteral() {
        final var sb = new StringBuilder();
        JsonWriter.writeString(null, sb);
        assertEquals("null", sb.toString());
    }

    @Test
    void charRoundTripsAsSingleCharJsonString() {
        final var sb = new StringBuilder();
        JsonWriter.writeChar('Z', sb);
        assertEquals('Z', new JsonReader(sb.toString()).readChar());
    }

    @Test
    void integerPrimitivesRoundTrip() {
        assertEquals((byte) -7, new JsonReader("-7").readByte());
        assertEquals((short) 1234, new JsonReader("1234").readShort());
        assertEquals(42, new JsonReader("42").readInt());
        assertEquals(Long.MIN_VALUE, new JsonReader(Long.toString(Long.MIN_VALUE)).readLong());
    }

    @Test
    void floatingPointPrimitivesRoundTrip() {
        assertEquals(3.14f, new JsonReader("3.14").readFloat(), 0.0001f);
        assertEquals(-2.5e10, new JsonReader("-2.5e10").readDouble(), 0.0);
    }

    @Test
    void booleanRoundTrips() {
        assertEquals(true, new JsonReader("true").readBoolean());
        assertEquals(false, new JsonReader("false").readBoolean());
    }

    @Test
    void envelopeDrivesMethodDispatch() {
        final var sb = new StringBuilder();
        sb.append("{\"m\":\"doRepeat\",\"a\":[");
        sb.append(3);
        sb.append(',');
        JsonWriter.writeString("hi", sb);
        sb.append("]}");

        final var reader = new JsonReader(sb.toString());
        reader.expectObjectStart();
        reader.expectKey("m");
        assertEquals("doRepeat", reader.readString());
        reader.expectComma();
        reader.expectKey("a");
        reader.expectArrayStart();
        assertEquals(3, reader.readInt());
        reader.expectComma();
        assertEquals("hi", reader.readString());
        reader.expectArrayEnd();
        reader.expectObjectEnd();
    }

    @Test
    void malformedInputRaisesClearError() {
        final var reader = new JsonReader("{\"m\":\"x\"");
        reader.expectObjectStart();
        reader.expectKey("m");
        assertEquals("x", reader.readString());
        assertThrows(IllegalStateException.class, reader::expectComma);
    }
}
