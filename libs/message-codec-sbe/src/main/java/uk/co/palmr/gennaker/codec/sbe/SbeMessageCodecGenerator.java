package uk.co.palmr.gennaker.codec.sbe;

import com.google.auto.service.AutoService;
import uk.co.palmr.gennaker.annotations.Codecs;
import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.MessageCodecGenerator;
import uk.co.palmr.gennaker.codec.TopicContext;
import uk.co.real_logic.sbe.SbeTool;
import uk.co.real_logic.sbe.generation.java.JavaGenerator;
import uk.co.real_logic.sbe.xml.IrGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.nio.file.Path;

import static uk.co.real_logic.sbe.SbeTool.parseSchema;
import static uk.co.real_logic.sbe.generation.TargetCodeGeneratorLoader.precedenceChecks;

@AutoService(MessageCodecGenerator.class)
public class SbeMessageCodecGenerator implements MessageCodecGenerator {

    @Override
    public String name() {
        return Codecs.SBE;
    }

    @Override
    public CodecBodyEmitter generate(final TopicContext context) {
        final var sbeXmlBuilder = SbeXmlBuilder.newBuilder(context.packageName(), context.interfaceName());
        for (final var method : context.methods()) {
            sbeXmlBuilder.method(method);
        }

        writeAndGenerateSbe(context, sbeXmlBuilder);

        return new SbeCodecBodyEmitter(context.interfaceName(), context.maxMessageSize(), context.methods());
    }

    private void writeAndGenerateSbe(final TopicContext context, final SbeXmlBuilder sbeXmlBuilder) {
        try {
            final var processingEnv = context.processingEnv();
            final var packageName = context.packageName();
            final var interfaceName = context.interfaceName();
            final var interfaceElement = context.topicInterface();

            final var sbeXmlFile = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "resources", packageName + "." + interfaceName + ".xml");
            final var sbeFilePath = sbeXmlFile.toUri().getPath();
            try (var writer = sbeXmlFile.openWriter()) {
                sbeXmlBuilder.write(writer);
            }
            sbeStubGen(packageName, processingEnv, interfaceElement, sbeFilePath);
        } catch (Exception ex) {
            context.processingEnv().getMessager().printError("Failed while generating SBE XML: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private static void sbeStubGen(final String packageName,
                                   final ProcessingEnvironment processingEnv,
                                   final TypeElement interfaceElement,
                                   final String fileName) throws Exception {
        final Path path = Path.of(fileName);
        final var outputDirName = resolveOutputDirName(path);
        final var schema = parseSchema(fileName);
        final var ir = new IrGenerator().generate(schema, packageName);

        System.setProperty("sbe.java.generate.interfaces", "true");
        System.setProperty("sbe.decode.unknown.enum.values", "true");

        final var outputManager = new AnnotationFilerOutputManager(outputDirName, ir.applicableNamespace(), processingEnv, interfaceElement);
        final boolean shouldSupportTypesPackageNames = Boolean.getBoolean("sbe.types.package.override");
        final JavaGenerator codecGenerator = new JavaGenerator(ir,
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
