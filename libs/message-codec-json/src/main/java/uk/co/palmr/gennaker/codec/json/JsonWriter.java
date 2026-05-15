package uk.co.palmr.gennaker.codec.json;

/**
 * Minimal JSON writing helpers used by generated publisher proxies.
 *
 * <p>The generated code appends most primitives directly into a
 * {@link StringBuilder}; this class is only needed where character escaping
 * matters ({@link String} and {@code char}).
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    public static void writeString(final String s, final StringBuilder out) {
        if (s == null) {
            out.append("null");
            return;
        }
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    public static void writeChar(final char c, final StringBuilder out) {
        writeString(String.valueOf(c), out);
    }
}
