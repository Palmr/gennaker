package uk.co.palmr.gennaker.annotations;

/**
 * Well-known {@code messageCodec} names. Using these constants gives IDE
 * autocomplete while keeping the codec selection a plain string so adding a
 * new codec doesn't require modifying this module.
 */
public final class Codecs {
    /** Simple Binary Encoding: compact, schema-derived binary wire format. */
    public static final String SBE = "sbe";

    /** JSON wire format: human-readable, easier to debug, larger on the wire. */
    public static final String JSON = "json";

    private Codecs() {
    }
}
