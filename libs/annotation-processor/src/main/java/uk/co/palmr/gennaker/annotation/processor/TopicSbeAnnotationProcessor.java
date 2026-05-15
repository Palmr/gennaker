package uk.co.palmr.gennaker.annotation.processor;

import com.google.auto.service.AutoService;
import uk.co.palmr.gennaker.annotation.processor.proxy.JavaProxy;
import uk.co.palmr.gennaker.annotation.processor.proxy.PublisherProxyBuilder;
import uk.co.palmr.gennaker.annotation.processor.proxy.SubscriberProxyBuilder;
import uk.co.palmr.gennaker.annotation.processor.sbe.AnnotationFilerOutputManager;
import uk.co.palmr.gennaker.annotation.processor.sbe.SbeXmlBuilder;
import uk.co.palmr.gennaker.annotations.Codecs;
import uk.co.palmr.gennaker.annotations.Topic;
import uk.co.real_logic.sbe.SbeTool;
import uk.co.real_logic.sbe.generation.java.JavaGenerator;
import uk.co.real_logic.sbe.xml.IrGenerator;

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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.nio.file.Path;
import java.util.Set;

import static uk.co.real_logic.sbe.SbeTool.parseSchema;
import static uk.co.real_logic.sbe.generation.TargetCodeGeneratorLoader.precedenceChecks;

@SupportedAnnotationTypes({"uk.co.palmr.gennaker.annotations.Topic"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class TopicSbeAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        final var messager = processingEnv.getMessager();

        for (final Element topicElement : roundEnvironment.getElementsAnnotatedWith(Topic.class)) {
            if (topicElement.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Gennaker Topic annotation can only be applied to interfaces");
                return true;
            }

            final var topicAnnotation = topicElement.getAnnotation(Topic.class);
            if (Codecs.SBE.equals(topicAnnotation.messageCodec())) {
                final var interfaceElement = (TypeElement) topicElement;
                final var packageElement = processingEnv.getElementUtils().getPackageOf(interfaceElement);
                final var interfaceName = interfaceElement.getSimpleName().toString();
                final var packageName = packageElement.getQualifiedName().toString();

                final var sbeXmlBuilder = SbeXmlBuilder.newBuilder(packageName, interfaceName);

                final var publisherProxyBuilder = new PublisherProxyBuilder(packageName, interfaceName, topicAnnotation.maxMessageSize());
                final var subscriberProxyBuilder = new SubscriberProxyBuilder(packageName, interfaceName);

                for (final var methodElement : ElementFilter.methodsIn(topicElement.getEnclosedElements())) {
                    if (methodElement.getReturnType().getKind() != TypeKind.VOID) {
                        throw new UnsupportedOperationException("Only void methods supported for now");
                    }

                    sbeXmlBuilder.method(methodElement);
                    publisherProxyBuilder.method(methodElement);
                    subscriberProxyBuilder.method(methodElement);
                }

                writeAndGenerateSbe(packageName, interfaceName, sbeXmlBuilder, interfaceElement, messager);

                writeJava(publisherProxyBuilder, interfaceElement, messager);
                writeJava(subscriberProxyBuilder, interfaceElement, messager);
            }
        }

        return false;
    }

    private void writeAndGenerateSbe(final String packageName, final String interfaceName, final SbeXmlBuilder sbeXmlBuilder, final TypeElement interfaceElement, final Messager messager) {
        try {
            final var sbeXmlFile = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "resources", packageName + "." + interfaceName + ".xml");
            final var sbeFilePath = sbeXmlFile.toUri().getPath();
            final var writer = sbeXmlFile.openWriter();
            sbeXmlBuilder.write(writer);
            writer.close();
            sbeStubGen(packageName, processingEnv, interfaceElement, sbeFilePath);
        } catch (Exception ex) {
            messager.printError("Failed while generating SBE XML:" + ex);
            throw new RuntimeException(ex);
        }
    }

    private void writeJava(final JavaProxy javaProxyBuilder, final TypeElement interfaceElement, final Messager messager) {
        try {
            final var file = processingEnv.getFiler().createSourceFile(javaProxyBuilder.getFileName(), interfaceElement);
            final var writer = file.openWriter();
            javaProxyBuilder.write(writer);
            writer.close();
        } catch (Exception ex) {
            messager.printError(ex.toString());
        }
    }

    private static void sbeStubGen(final String packageName, final ProcessingEnvironment processingEnv, final TypeElement interfaceElement, final String fileName) throws Exception {
        final Path path = Path.of(fileName);
        final var outputDirName = resolveOutputDirName(path);
        final var schema = parseSchema(fileName);
        final var ir = new IrGenerator().generate(schema, packageName);

        System.setProperty("sbe.java.generate.interfaces", "true");
        System.setProperty("sbe.decode.unknown.enum.values", "true");

        final var outputManager = new AnnotationFilerOutputManager(outputDirName, ir.applicableNamespace(), processingEnv, interfaceElement);
        final boolean shouldSupportTypesPackageNames = Boolean.getBoolean("sbe.types.package.override");
        JavaGenerator codecGenerator = new JavaGenerator(ir,
                System.getProperty("sbe.java.encoding.buffer.type", SbeTool.JAVA_DEFAULT_ENCODING_BUFFER_TYPE),
                System.getProperty("sbe.java.decoding.buffer.type", SbeTool.JAVA_DEFAULT_DECODING_BUFFER_TYPE),
                Boolean.getBoolean("sbe.java.generate.group-order.annotation"),
                Boolean.getBoolean("sbe.java.generate.interfaces"),
                Boolean.getBoolean("sbe.decode.unknown.enum.values"),
                shouldSupportTypesPackageNames,
                precedenceChecks(),
                outputManager);
        codecGenerator.generate();
    }

    private static String resolveOutputDirName(final Path path) {
        if (path == null) {
            return ".";
        }

        final Path parentDir = path.getParent();
        if (parentDir == null) {
            return ".";
        }

        final Path theBetterParentDir = parentDir.getParent();
        return theBetterParentDir == null ? parentDir.toString() : theBetterParentDir.toString();
    }
}
