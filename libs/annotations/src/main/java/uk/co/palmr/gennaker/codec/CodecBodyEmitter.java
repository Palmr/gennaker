package uk.co.palmr.gennaker.codec;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

/**
 * Fills in the codec-specific portions of the generated publisher and
 * subscriber proxy sources. The proxy "shell" (package declaration, class
 * header, method signatures, transport plumbing) is owned by the annotation
 * processor; everything that differs between codecs lives here.
 */
public interface CodecBodyEmitter {

    /** Fully-qualified imports the publisher proxy needs in addition to its baseline. */
    List<String> publisherImports();

    /** Field declarations injected into the publisher proxy class body. */
    String publisherFields(List<ExecutableElement> methods);

    /** Body of a single publisher proxy method (between the curly braces). */
    String publisherMethodBody(ExecutableElement method, String interfaceSimpleName);

    /** Fully-qualified imports the subscriber proxy needs in addition to its baseline. */
    List<String> subscriberImports();

    /** Field declarations injected into the subscriber proxy class body. */
    String subscriberFields(List<ExecutableElement> methods);

    /** Body of {@code onMessage(DirectBuffer buffer, int offset, int length)}. */
    String subscriberOnMessageBody(List<ExecutableElement> methods);
}
