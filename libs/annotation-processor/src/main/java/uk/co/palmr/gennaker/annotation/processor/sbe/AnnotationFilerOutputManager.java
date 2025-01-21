package uk.co.palmr.gennaker.annotation.processor.sbe;

import uk.co.real_logic.sbe.generation.java.JavaOutputManager;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Hack to get SBE to generate its files using the annotation processor's Filer.
 * This isn't perfect as it doesn't support everything (changing packages) that the original OutputManager does.
 */
public class AnnotationFilerOutputManager extends JavaOutputManager {
    private static final Set<String> ALREADY_GENERATED = new HashSet<>();
    private final String packageName;
    private final ProcessingEnvironment processingEnv;
    private final TypeElement interfaceElement;

    public AnnotationFilerOutputManager(final String baseDirName,
                                        final String packageName,
                                        final ProcessingEnvironment processingEnv,
                                        final TypeElement interfaceElement) {
        super(baseDirName, packageName);
        this.packageName = packageName;
        this.processingEnv = processingEnv;
        this.interfaceElement = interfaceElement;
    }

    @Override
    public Writer createOutput(final String name) throws IOException {
        final var sourceName = packageName + "." + name;
        if (!ALREADY_GENERATED.contains(sourceName)) {
            var f = processingEnv.getFiler().createSourceFile(sourceName, interfaceElement);
            ALREADY_GENERATED.add(sourceName);
            return f.openWriter();
        } else {
            return super.createOutput(name);
        }
    }
}
