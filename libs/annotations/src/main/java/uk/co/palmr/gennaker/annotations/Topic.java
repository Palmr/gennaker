package uk.co.palmr.gennaker.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a Gennaker topic. The annotation processor generates
 * a publisher proxy implementing the interface and a subscriber proxy that
 * decodes inbound messages and dispatches to a user-supplied implementation.
 *
 * <p>Each topic chooses its wire format independently via {@link #messageCodec()}.
 *
 * <pre>
 * &#64;Topic(messageCodec = Codecs.JSON)
 * public interface Ping {
 *     void doPing(String message);
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
@Inherited
public @interface Topic {
    /**
     * Maximum encoded size, in bytes, of a single message on this topic. Used
     * to size send/receive buffers in the generated proxies.
     * If a message ends up encoded larger than this, the proxy will throw a
     * runtime exception.
     *
     * @return the buffer size budget for one message
     */
    int maxMessageSize() default 1024;

    /**
     * Name of the message codec to use for this topic. Use a constant from
     * {@link Codecs} (e.g. {@link Codecs#SBE}, {@link Codecs#JSON}); the value
     * is resolved against codecs discovered on the annotation processor path
     * via {@link java.util.ServiceLoader}.
     *
     * @return the codec name, defaulting to {@link Codecs#SBE}
     */
    String messageCodec() default Codecs.SBE;
}
