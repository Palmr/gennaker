package uk.co.palmr.gennaker.codec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

/**
 * Per-topic context handed to a {@link MessageCodecGenerator} so it can read
 * the topic shape and write side-files via the annotation processor's
 * {@link javax.annotation.processing.Filer Filer}.
 */
public interface TopicContext {
    ProcessingEnvironment processingEnv();

    TypeElement topicInterface();

    String packageName();

    String interfaceName();

    int maxMessageSize();

    List<ExecutableElement> methods();

    /**
     * All Layout-described types reachable (transitively) from this topic's
     * method parameters. Keyed by fully-qualified Java type name.
     *
     * <p>Empty when the topic uses only primitives and {@link String}.
     */
    Map<String, TypeShape> reachableTypes();
}
