package uk.co.palmr.gennaker.codec.json;

/**
 * Minimal positional JSON reader used by generated subscriber proxies.
 *
 * <p>The wire format is constrained: a single object with field {@code "m"}
 * (the method name, always first) followed by {@code "a"} (an array of
 * primitive/string arguments). Generated code drives the reader in lock-step
 * with the method signature it is dispatching to, so this class deliberately
 * does no schema validation beyond what each {@code readX} call needs.
 */
public final class JsonReader {
    private final String src;
    private int pos;

    public JsonReader(final String src) {
        this.src = src;
        this.pos = 0;
    }

    public void expectObjectStart() {
        skipWs();
        require('{');
    }

    public void expectObjectEnd() {
        skipWs();
        require('}');
    }

    public void expectArrayStart() {
        skipWs();
        require('[');
    }

    public void expectArrayEnd() {
        skipWs();
        require(']');
    }

    public void expectComma() {
        skipWs();
        require(',');
    }

    public void expectKey(final String key) {
        skipWs();
        require('"');
        for (int i = 0; i < key.length(); i++) {
            require(key.charAt(i));
        }
        require('"');
        skipWs();
        require(':');
    }

    public String readString() {
        skipWs();
        require('"');
        final StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            final char c = src.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                final char e = src.charAt(pos++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        final int code = Integer.parseInt(src.substring(pos, pos + 4), 16);
                        pos += 4;
                        sb.append((char) code);
                    }
                    default -> throw new IllegalStateException("Bad escape: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalStateException("Unterminated string");
    }

    public boolean readBoolean() {
        skipWs();
        if (src.startsWith("true", pos)) {
            pos += 4;
            return true;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return false;
        }
        throw new IllegalStateException("Expected boolean at " + pos);
    }

    public long readLong() {
        skipWs();
        final int start = pos;
        if (pos < src.length() && src.charAt(pos) == '-') {
            pos++;
        }
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
            pos++;
        }
        return Long.parseLong(src.substring(start, pos));
    }

    public int readInt() {
        return Math.toIntExact(readLong());
    }

    public short readShort() {
        return (short) readLong();
    }

    public byte readByte() {
        return (byte) readLong();
    }

    public double readDouble() {
        skipWs();
        final int start = pos;
        if (pos < src.length() && src.charAt(pos) == '-') {
            pos++;
        }
        while (pos < src.length() && "0123456789.eE+-".indexOf(src.charAt(pos)) >= 0) {
            pos++;
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    public float readFloat() {
        return (float) readDouble();
    }

    public char readChar() {
        final String s = readString();
        if (s.length() != 1) {
            throw new IllegalStateException("Expected single-character string for char, got: " + s);
        }
        return s.charAt(0);
    }

    /**
     * Read a JSON object field name (the key before the colon).
     * Reads a quoted string and consumes the following colon.
     */
    public String readFieldName() {
        final String name = readString();
        skipWs();
        require(':');
        return name;
    }

    /**
     * Try to consume {@code ']'}. Returns true if found, false otherwise.
     */
    public boolean tryArrayEnd() {
        skipWs();
        if (pos < src.length() && src.charAt(pos) == ']') {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Try to consume {@code '}'}. Returns true if found, false otherwise.
     */
    public boolean tryObjectEnd() {
        skipWs();
        if (pos < src.length() && src.charAt(pos) == '}') {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Consumes a comma and returns false, or consumes {@code ']'} and returns
     * true. Used when iterating array elements.
     */
    public boolean tryCommaOrArrayEnd() {
        skipWs();
        if (pos < src.length() && src.charAt(pos) == ']') {
            pos++;
            return true;
        }
        require(',');
        return false;
    }

    /**
     * Consumes a comma and returns false, or consumes {@code '}'} and returns
     * true. Used when iterating map entries.
     */
    public boolean tryCommaOrObjectEnd() {
        skipWs();
        if (pos < src.length() && src.charAt(pos) == '}') {
            pos++;
            return true;
        }
        require(',');
        return false;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private void require(final char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new IllegalStateException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }
}
