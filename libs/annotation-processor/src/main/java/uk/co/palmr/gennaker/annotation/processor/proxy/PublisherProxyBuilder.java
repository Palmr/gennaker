package uk.co.palmr.gennaker.annotation.processor.proxy;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
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
        writer.write("package %s;%n%n".formatted(packageName));
        writer.write(emitter.publisherImports());
        writer.write("""
                public class %s implements %s {
                    private static final java.lang.System.Logger LOG = java.lang.System.getLogger("%s.%s");

                """.formatted(className, interfaceName, packageName, interfaceName));
        writer.write(emitter.publisherFields());
        writer.write("""
                    private final Transport transport;

                    public %s(final Transport transport) {
                        this.transport = transport;
                    }

                """.formatted(className));

        for (final ExecutableElement methodElement : methods) {
            writer.write(methodHeader(methodElement));
            writer.write(traceLog(methodElement));
            writer.write(emitter.publisherMethodBody(methodElement));
            writer.write("    }%n%n".formatted());
        }

        writer.write("}\n");
    }

    private static String methodHeader(final ExecutableElement method) {
        final var params = method.getParameters().stream()
                .map(p -> p.asType() + " " + p.getSimpleName())
                .collect(Collectors.joining(", "));
        return """
                    @Override
                    public %s %s(%s) {
                """.formatted(method.getReturnType(), method.getSimpleName(), params);
    }

    private static String traceLog(final ExecutableElement method) {
        final var args = method.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .map(name -> "java.lang.String.valueOf(" + name + ")")
                .collect(Collectors.joining(" + \", \" + "));
        final var argsExpr = args.isEmpty() ? "\"\"" : args;
        return """
                        if (LOG.isLoggable(java.lang.System.Logger.Level.TRACE)) {
                            LOG.log(java.lang.System.Logger.Level.TRACE, "pub %s(" + %s + ")");
                        }
                """.formatted(method.getSimpleName(), argsExpr);
    }
}
