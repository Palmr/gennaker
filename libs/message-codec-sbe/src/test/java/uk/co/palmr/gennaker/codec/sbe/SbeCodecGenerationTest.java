package uk.co.palmr.gennaker.codec.sbe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.co.palmr.gennaker.annotation.processor.TopicAnnotationProcessor;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test that the SBE codec, discovered via ServiceLoader, produces
 * publisher and subscriber proxies wired to SBE's encoder/decoder API.
 *
 * <p>Uses the JDK's javac directly so the SBE codec can write its intermediate
 * XML schema to a real filesystem (compile-testing's in-memory filer breaks
 * SBE's parseSchema round-trip).
 */
class SbeCodecGenerationTest {

    @Test
    void generatesSbeFlavouredProxiesForDefaultTopic(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        final var sourceFile = sourceDir.resolve("demo/Echo.java");
        Files.writeString(sourceFile, String.join("\n",
                "package demo;",
                "",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "",
                "@Topic",
                "public interface Echo {",
                "    void say(String message);",
                "    void repeat(int count, String message);",
                "}",
                ""));

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(outputDir));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classOutputDir));

            final Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromPaths(List.of(sourceFile));
            final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, sources);
            task.setProcessors(List.of(new TopicAnnotationProcessor()));

            final boolean success = task.call();
            assertTrue(success,
                    () -> "Compilation failed: " + diagnostics.getDiagnostics().stream()
                            .map(d -> d.getMessage(null))
                            .collect(Collectors.joining("\n")));
        }

        final var pub = Files.readString(outputDir.resolve("demo/Echo__pub_proxy.java"));
        assertTrue(pub.contains("implements Echo"), pub);
        assertTrue(pub.contains("MessageHeaderEncoder headerEncoder"), pub);
        assertTrue(pub.contains("SayEncoder sayEncoder"), pub);
        assertTrue(pub.contains("RepeatEncoder repeatEncoder"), pub);
        assertTrue(pub.contains("sayEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)"), pub);
        assertTrue(pub.contains("transport.publish(Echo.class, buffer,"), pub);

        final var sub = Files.readString(outputDir.resolve("demo/Echo__sub_proxy.java"));
        assertTrue(sub.contains("implements MessageHandler"), sub);
        assertTrue(sub.contains("MessageHeaderDecoder messageHeaderDecoder"), sub);
        assertTrue(sub.contains("case SayDecoder.TEMPLATE_ID"), sub);
        assertTrue(sub.contains("case RepeatDecoder.TEMPLATE_ID"), sub);
        assertTrue(sub.contains("delegate.say(msgDecoder.message())"), sub);
        assertTrue(sub.contains("delegate.repeat(msgDecoder.count(), msgDecoder.message())"), sub);

        assertTrue(Files.exists(outputDir.resolve("demo/SayEncoder.java")), "expected SBE-generated SayEncoder");
        assertTrue(Files.exists(outputDir.resolve("demo/RepeatDecoder.java")), "expected SBE-generated RepeatDecoder");
        assertTrue(Files.exists(outputDir.resolve("demo/MessageHeaderEncoder.java")), "expected SBE-generated MessageHeaderEncoder");
    }
}
