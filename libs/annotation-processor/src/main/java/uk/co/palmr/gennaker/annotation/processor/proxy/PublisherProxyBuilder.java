package uk.co.palmr.gennaker.annotation.processor.proxy;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;

import javax.lang.model.element.ExecutableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public final class PublisherProxyBuilder implements JavaProxy {
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
        writer.write("    private static final java.lang.System.Logger LOG = java.lang.System.getLogger(\"");
        writer.write(packageName);
        writer.write(".");
        writer.write(interfaceName);
        writer.write("\");\n\n");
        writer.write(emitter.publisherFields());
        writer.write("    private final Transport transport;\n\n");
        writer.write("    public ");
        writer.write(className);
        writer.write("(final Transport transport) {\n        this.transport = transport;\n    }\n\n");

        for (final ExecutableElement methodElement : methods) {
            final var methodName = methodElement.getSimpleName().toString();
            writer.write("    @Override\n");
            writer.write("    public ");
            writer.write(methodElement.getReturnType().toString());
            writer.write(" ");
            writer.write(methodName);
            writer.write("(");
            writer.write(methodElement.getParameters().stream()
                    .map(p -> p.asType() + " " + p.getSimpleName())
                    .collect(Collectors.joining(", ")));
            writer.write(") {\n");
            writer.write("        if (LOG.isLoggable(java.lang.System.Logger.Level.TRACE)) {\n");
            writer.write("            LOG.log(java.lang.System.Logger.Level.TRACE, \"pub ");
            writer.write(methodName);
            writer.write("(\"");
            final var params = methodElement.getParameters();
            for (int i = 0; i < params.size(); i++) {
                writer.write(" + ");
                if (i > 0) {
                    writer.write("\", \" + ");
                }
                writer.write("java.lang.String.valueOf(");
                writer.write(params.get(i).getSimpleName().toString());
                writer.write(")");
            }
            writer.write(" + \")\");\n");
            writer.write("        }\n");
            writer.write(emitter.publisherMethodBody(methodElement));
            writer.write("    }\n\n");
        }

        writer.write("}\n");
    }
}
