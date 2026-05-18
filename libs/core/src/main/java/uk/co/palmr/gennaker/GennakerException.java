package uk.co.palmr.gennaker;

/// Unchecked exception thrown for failures originating inside Gennaker itself
/// — missing generated proxies, malformed proxy constructors, or unexpected
/// failures during proxy instantiation. Callers can catch this to distinguish
/// library failures from unrelated runtime errors.
public class GennakerException extends RuntimeException {
    /// Creates an exception with the given message and underlying cause.
    ///
    /// @param message human-readable description of the failure
    /// @param cause   the underlying throwable that triggered this failure
    public GennakerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
