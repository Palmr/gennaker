package uk.co.palmr.gennaker.annotation.processor.proxy;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;

import javax.lang.model.element.ExecutableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class PublisherProxyBuilder implements JavaProxy {
    public static final String PROXY_SUFFIX = "__pub_proxy";
    private final String packageName;
    private final String interfaceName;
    private final String className;
    private final List<ExecutableElement> methods;
    private final CodecBodyEmitter emitter;

    public PublisherProxyBuilder(final String packageName,
                                 final String interfaceName,
                                 final List<ExecutableElement> methods,
                                 final CodecBodyEmitter emitter) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.className = interfaceName + PROXY_SUFFIX;
        this.methods = List.copyOf(methods);
        this.emitter = emitter;
    }

    @Override
    public String getFileName() {
        return packageName + "." + className;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write("package ");
        writer.write(packageName);
        writer.write(";\n\n");
        writer.write(emitter.publisherImports());
        writer.write("public class ");
        writer.write(className);
        writer.write(" implements ");
        writer.write(interfaceName);
        writer.write(" {\n");
        writer.write(emitter.publisherFields());
        writer.write("    private final Transport transport;\n\n");
        writer.write("    public ");
        writer.write(className);
        writer.write("(final Transport transport) {\n        this.transport = transport;\n    }\n\n");

        for (final ExecutableElement methodElement : methods) {
            writer.write("    @Override\n");
            writer.write("    public ");
            writer.write(methodElement.getReturnType().toString());
            writer.write(" ");
            writer.write(methodElement.getSimpleName().toString());
            writer.write("(");
            writer.write(methodElement.getParameters().stream()
                    .map(p -> p.asType() + " " + p.getSimpleName())
                    .collect(Collectors.joining(", ")));
            writer.write(") {\n");
            writer.write(emitter.publisherMethodBody(methodElement));
            writer.write("    }\n\n");
        }

        writer.write("}\n");
    }
}
