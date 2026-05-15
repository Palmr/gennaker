package uk.co.palmr.gennaker.annotations;

/**
 * Well-known {@code messageCodec} names. Using these constants gives IDE
 * autocomplete while keeping the codec selection a plain string so adding a
 * new codec doesn't require modifying this module.
 */
public final class Codecs {
    public static final String SBE = "sbe";
    public static final String JSON = "json";

    private Codecs() {
    }
}
