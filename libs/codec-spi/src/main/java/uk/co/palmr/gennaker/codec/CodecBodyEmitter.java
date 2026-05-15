package uk.co.palmr.gennaker.codec;

import javax.lang.model.element.ExecutableElement;

/**
 * Fills in the codec-specific portions of the generated publisher and
 * subscriber proxy sources. The proxy "shell" (package declaration, class
 * header, transport/delegate field, constructor, method signatures) is owned
 * by the annotation processor; everything that differs between codecs lives
 * here.
 *
 * <p>An emitter is produced by
 * {@link MessageCodecGenerator#generate(TopicContext)} and is already bound to
 * the topic it serves; it has no per-topic state on its API.
 */
public interface CodecBodyEmitter {

    /**
     * Complete import block for the publisher proxy, placed immediately after
     * the {@code package} statement. Must include the import for
     * {@code uk.co.palmr.gennaker.Transport} and end with a trailing blank
     * line.
     */
    String publisherImports();

    /**
     * Field declarations injected into the publisher proxy class body
     * immediately before the (shell-owned) {@code transport} field. Each
     * declaration should end with a newline; the block as a whole should end
     * with a newline.
     */
    String publisherFields();

    /**
     * Body of a single publisher proxy method (the lines between the curly
     * braces of the method declaration). The shell is responsible for emitting
     * the {@code @Override}, the signature, and the closing brace. Lines
     * should be indented eight spaces. The block should end with a newline.
     */
    String publisherMethodBody(ExecutableElement method);

    /**
     * Complete import block for the subscriber proxy. Must include imports
     * for {@code org.agrona.DirectBuffer} and
     * {@code uk.co.palmr.gennaker.MessageHandler} and end with a trailing
     * blank line.
     */
    String subscriberImports();

    /**
     * Field declarations injected into the subscriber proxy class body
     * immediately before the (shell-owned) {@code delegate} field.
     */
    String subscriberFields();

    /**
     * Body of {@code onMessage(DirectBuffer buffer, int offset, int length)}.
     * The shell emits the method signature and the closing brace.
     */
    String subscriberOnMessageBody();
}
