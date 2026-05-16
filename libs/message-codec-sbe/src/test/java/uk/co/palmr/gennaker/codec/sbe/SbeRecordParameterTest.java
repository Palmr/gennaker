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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the SBE codec generates correct schemas and proxies when a topic
 * method parameter is a Java record (auto-derived layout).
 */
class SbeRecordParameterTest {

    @Test
    void generatesFlattenedFieldsForRecordParameter(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Point.java"),
                "package demo;\npublic record Point(int x, int y) {}\n");

        Files.writeString(sourceDir.resolve("demo/Grid.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "@Topic",
                "public interface Grid {",
                "    void plot(Point position);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Point.java"), sourceDir.resolve("demo/Grid.java")));
        assertTrue(success, () -> "Compilation failed");

        // Publisher should reference flattened encoder fields
        final var pub = Files.readString(outputDir.resolve("demo/Grid__pub_proxy.java"));
        assertTrue(pub.contains("plotEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)"), pub);
        // Flattened field setters: positionX and positionY
        assertTrue(pub.contains("positionX"), pub);
        assertTrue(pub.contains("positionY"), pub);
        assertTrue(pub.contains("position.x()"), pub);
        assertTrue(pub.contains("position.y()"), pub);

        // Subscriber should reconstruct the record
        final var sub = Files.readString(outputDir.resolve("demo/Grid__sub_proxy.java"));
        assertTrue(sub.contains("case PlotDecoder.TEMPLATE_ID"), sub);
        assertTrue(sub.contains("new Point("), sub);
    }

    @Test
    void generatesSchemaWithFlattenedFieldNames(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Range.java"),
                "package demo;\npublic record Range(long from, long to) {}\n");

        Files.writeString(sourceDir.resolve("demo/Window.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "@Topic",
                "public interface Window {",
                "    void scroll(Range viewport);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Range.java"), sourceDir.resolve("demo/Window.java")));
        assertTrue(success, () -> "Compilation failed");

        // SBE schema XML should have flattened field names
        final var xmlFiles = Files.walk(outputDir)
                .filter(p -> p.toString().endsWith(".xml"))
                .toList();
        assertTrue(!xmlFiles.isEmpty(), "Expected SBE schema XML to be generated");
        final var xml = Files.readString(xmlFiles.get(0));
        assertTrue(xml.contains("viewportFrom") || xml.contains("viewport"), xml);
    }

    @Test
    void mixedPrimitiveAndRecordParameters(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Payload.java"),
                "package demo;\npublic record Payload(String data) {}\n");

        Files.writeString(sourceDir.resolve("demo/Bus.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "@Topic",
                "public interface Bus {",
                "    void publish(int priority, Payload body);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Payload.java"), sourceDir.resolve("demo/Bus.java")));
        assertTrue(success, () -> "Compilation failed");

        final var pub = Files.readString(outputDir.resolve("demo/Bus__pub_proxy.java"));
        // Direct primitive setter
        assertTrue(pub.contains("methodEncoder.priority(priority)"), pub);
        // Flattened record field
        assertTrue(pub.contains("bodyData"), pub);
    }

    private static boolean compile(final Path sourceDir,
                                   final Path outputDir,
                                   final Path classOutputDir,
                                   final List<Path> sourceFiles) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(outputDir));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classOutputDir));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, currentClasspath());
            final Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
            final var task = compiler.getTask(null, fileManager, diagnostics, null, null, sources);
            task.setProcessors(List.of(new TopicAnnotationProcessor()));
            final boolean ok = task.call();
            if (!ok) {
                System.err.println("Compilation diagnostics:");
                diagnostics.getDiagnostics().forEach(d -> System.err.println(d.getMessage(null)));
            }
            return ok;
        }
    }

    private static java.util.List<java.nio.file.Path> currentClasspath() {
        return java.util.Arrays.stream(
                System.getProperty("java.class.path", "").split(java.io.File.pathSeparator))
                .filter(s -> !s.isBlank())
                .map(java.nio.file.Path::of)
                .filter(java.nio.file.Files::exists)
                .toList();
    }
}
