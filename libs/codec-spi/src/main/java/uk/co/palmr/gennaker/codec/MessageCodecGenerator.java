package uk.co.palmr.gennaker.codec;

/**
 * SPI implemented by message codecs (SBE, JSON, ...).
 *
 * <p>Implementations are discovered at annotation-processing time via
 * {@link java.util.ServiceLoader}. The {@link #name()} returned by each
 * implementation is matched against {@code Topic.messageCodec()} to pick which
 * codec generates the proxies for a given topic interface.
 */
public interface MessageCodecGenerator {
    /**
     * Stable identifier referenced from {@code @Topic(messageCodec = "...")}.
     *
     * @return this codec's name (e.g. {@code "sbe"}, {@code "json"})
     */
    String name();

    /**
     * Perform any codec-specific side work (e.g. generate intermediate
     * schemas/codecs) for this topic and return an emitter that knows how to
     * fill in the codec-specific parts of the publisher and subscriber proxies.
     *
     * @param context the per-topic context to read the shape from and to write
     *                side-files through
     * @return an emitter bound to this topic
     */
    CodecBodyEmitter generate(TopicContext context);
}
