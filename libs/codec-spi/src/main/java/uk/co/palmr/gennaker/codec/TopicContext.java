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
    /**
     * Returns the active javac processing environment.
     *
     * @return the processing environment (for filer, messager, and type-utility access)
     */
    ProcessingEnvironment processingEnv();

    /**
     * Returns the topic interface being processed.
     *
     * @return the {@code @Topic}-annotated interface
     */
    TypeElement topicInterface();

    /**
     * Returns the package the proxies will be emitted into.
     *
     * @return the topic interface's package name
     */
    String packageName();

    /**
     * Returns the simple name of the topic interface.
     *
     * @return the topic interface's simple name
     */
    String interfaceName();

    /**
     * Returns the per-topic encoded-message size budget.
     *
     * @return the {@code maxMessageSize} declared on {@code @Topic}
     */
    int maxMessageSize();

    /**
     * Returns the methods declared on the topic interface.
     *
     * @return the methods in declaration order
     */
    List<ExecutableElement> methods();

    /**
     * All Layout-described types reachable (transitively) from this topic's
     * method parameters. Keyed by fully-qualified Java type name.
     *
     * <p>Empty when the topic uses only primitives and {@link String}.
     *
     * @return reachable type shapes keyed by fully-qualified Java type name
     */
    Map<String, TypeShape> reachableTypes();
}
