package uk.co.palmr.gennaker.annotation.processor;

import com.google.auto.service.AutoService;
import uk.co.palmr.gennaker.annotation.processor.proxy.JavaProxy;
import uk.co.palmr.gennaker.annotation.processor.proxy.PublisherProxyBuilder;
import uk.co.palmr.gennaker.annotation.processor.proxy.SubscriberProxyBuilder;
import uk.co.palmr.gennaker.annotations.Topic;
import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.MessageCodecGenerator;
import uk.co.palmr.gennaker.codec.TopicContext;

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

@SupportedAnnotationTypes({"uk.co.palmr.gennaker.annotations.Topic"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class TopicAnnotationProcessor extends AbstractProcessor {

    private Map<String, MessageCodecGenerator> codecs;

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
            for (final var methodElement : methods) {
                if (methodElement.getReturnType().getKind() != TypeKind.VOID) {
                    throw new UnsupportedOperationException("Only void methods supported for now");
                }
            }

            final var context = new TopicContextImpl(processingEnv, interfaceElement, packageName, interfaceName,
                    topicAnnotation.maxMessageSize(), methods);
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
                                    List<ExecutableElement> methods) implements TopicContext {
        private TopicContextImpl {
            methods = List.copyOf(methods);
        }
    }
}
