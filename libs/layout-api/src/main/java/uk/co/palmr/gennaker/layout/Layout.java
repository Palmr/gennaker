package uk.co.palmr.gennaker.layout;

/**
 * Describes how a user-defined type {@code T} is encoded to and decoded from
 * a message on the wire.
 *
 * <p>A Layout is a <em>compile-time specification</em>, not a runtime
 * serialiser. The annotation processor reads the AST of the {@code encode}
 * and {@code decode} methods and derives the field list (names, types, order)
 * from the sequence of {@code sink.writeX(name, ...)} /
 * {@code source.readX(name)} calls. The bodies must be <em>linear</em>: a
 * flat sequence of write/read calls with no conditionals, loops, or helper
 * method calls.
 *
 * <p>For Java records, the processor auto-derives the Layout from the record
 * components — no implementation of this interface is needed.
 *
 * @param <T> the type this layout describes
 */
public interface Layout<T> {

    /**
     * Encode {@code value} into the given sink.
     */
    void encode(T value, EncodeSink sink);

    /**
     * Decode a value of type {@code T} from the given source.
     */
    T decode(DecodeSource source);
}
