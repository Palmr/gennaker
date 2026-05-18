package uk.co.palmr.gennaker.annotation.processor;

import com.google.auto.service.AutoService;
import uk.co.palmr.gennaker.annotation.processor.proxy.JavaProxy;
import uk.co.palmr.gennaker.annotation.processor.proxy.PublisherProxyBuilder;
import uk.co.palmr.gennaker.annotation.processor.proxy.SubscriberProxyBuilder;
import uk.co.palmr.gennaker.annotations.Topic;
import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.MessageCodecGenerator;
import uk.co.palmr.gennaker.codec.TopicContext;
import uk.co.palmr.gennaker.codec.TypeShape;
import uk.co.palmr.gennaker.annotation.processor.shape.ShapeExtractor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Annotation processor that emits a publisher and a subscriber proxy for
 * every {@code @Topic}-annotated interface on the compilation unit.
 *
 * <p>Discovers {@link MessageCodecGenerator} implementations on its classpath
 * via {@link ServiceLoader} and dispatches each topic to the codec named in
 * {@code @Topic(messageCodec = ...)}.
 *
 * <p>Registered as a service via {@link AutoService} — users just put the
 * module on their {@code annotationProcessor} configuration; no manual
 * wiring needed.
 */
@SupportedAnnotationTypes({"uk.co.palmr.gennaker.annotations.Topic"})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@AutoService(Processor.class)
public final class TopicAnnotationProcessor extends AbstractProcessor {

    private Map<String, MessageCodecGenerator> codecs;

    /** Public no-arg constructor required by the {@link Processor} SPI. */
    public TopicAnnotationProcessor() {
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        codecs = new HashMap<>();
        for (final var codec : ServiceLoader.load(MessageCodecGenerator.class, getClass().getClassLoader())) {
            codecs.put(codec.name(), codec);
        }
    }

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        final var messager = processingEnv.getMessager();
        final var shapeExtractor = new ShapeExtractor(processingEnv);
        shapeExtractor.discoverLayouts(roundEnvironment);

        for (final Element topicElement : roundEnvironment.getElementsAnnotatedWith(Topic.class)) {
            if (topicElement.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Gennaker Topic annotation can only be applied to interfaces", topicElement);
                return true;
            }

            final var topicAnnotation = topicElement.getAnnotation(Topic.class);
            final var codecName = topicAnnotation.messageCodec();
            final var codec = codecs.get(codecName);
            if (codec == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "No MessageCodecGenerator registered for messageCodec=\"" + codecName + "\"." +
                                " Discovered codecs: " + codecs.keySet() +
                                ". Ensure the codec module is on the annotation processor classpath.",
                        topicElement);
                return true;
            }

            final var interfaceElement = (TypeElement) topicElement;
            final var packageElement = processingEnv.getElementUtils().getPackageOf(interfaceElement);
            final var interfaceName = interfaceElement.getSimpleName().toString();
            final var packageName = packageElement.getQualifiedName().toString();

            final var methods = ElementFilter.methodsIn(topicElement.getEnclosedElements());
            var hasInvalidMethod = false;
            for (final var methodElement : methods) {
                if (methodElement.getReturnType().getKind() != TypeKind.VOID) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Only void methods supported for now", methodElement);
                    hasInvalidMethod = true;
                }
            }
            if (hasInvalidMethod) {
                continue;
            }

            final var reachableTypes = shapeExtractor.collectReachableTypes(methods);
            final var context = new TopicContextImpl(processingEnv, interfaceElement, packageName, interfaceName,
                    topicAnnotation.maxMessageSize(), methods, reachableTypes);
            final CodecBodyEmitter emitter = codec.generate(context);

            final var publisherProxyBuilder = new PublisherProxyBuilder(packageName, interfaceName, methods, emitter);
            final var subscriberProxyBuilder = new SubscriberProxyBuilder(packageName, interfaceName, emitter);

            writeJava(publisherProxyBuilder, interfaceElement, messager);
            writeJava(subscriberProxyBuilder, interfaceElement, messager);
        }

        return false;
    }

    private void writeJava(final JavaProxy javaProxyBuilder, final TypeElement interfaceElement, final Messager messager) {
        try {
            final var file = processingEnv.getFiler().createSourceFile(javaProxyBuilder.getFileName(), interfaceElement);
            try (var writer = file.openWriter()) {
                javaProxyBuilder.write(writer);
            }
        } catch (Exception ex) {
            messager.printError(ex.toString());
        }
    }

    private record TopicContextImpl(ProcessingEnvironment processingEnv,
                                    TypeElement topicInterface,
                                    String packageName,
                                    String interfaceName,
                                    int maxMessageSize,
                                    List<ExecutableElement> methods,
                                    Map<String, TypeShape> reachableTypes) implements TopicContext {
        private TopicContextImpl {
            methods = List.copyOf(methods);
            reachableTypes = Map.copyOf(reachableTypes);
        }
    }
}
