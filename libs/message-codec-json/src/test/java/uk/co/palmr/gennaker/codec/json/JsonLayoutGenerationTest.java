package uk.co.palmr.gennaker.codec.json;

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
 * End-to-end tests that the JSON codec generates {@code __layout} helper classes
 * for record-typed topic method parameters.
 */
class JsonLayoutGenerationTest {

    @Test
    void generatesLayoutForRecordParameter(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Point.java"), String.join("\n",
                "package demo;",
                "",
                "public record Point(int x, int y) {}",
                ""));

        Files.writeString(sourceDir.resolve("demo/Canvas.java"), String.join("\n",
                "package demo;",
                "",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "import uk.co.palmr.gennaker.annotations.Codecs;",
                "",
                "@Topic(messageCodec = Codecs.JSON)",
                "public interface Canvas {",
                "    void drawAt(Point position);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Point.java"), sourceDir.resolve("demo/Canvas.java")));
        assertTrue(success, "Compilation should succeed");

        final var pub = Files.readString(outputDir.resolve("demo/Canvas__pub_proxy.java"));
        assertTrue(pub.contains("Point__layout.encode(position, json)"), pub);

        final var sub = Files.readString(outputDir.resolve("demo/Canvas__sub_proxy.java"));
        assertTrue(sub.contains("Point__layout.decode(reader)"), sub);

        final var layout = Files.readString(outputDir.resolve("demo/Point__layout.java"));
        assertTrue(layout.contains("static void encode(final Point value"), layout);
        assertTrue(layout.contains("static Point decode(final JsonReader reader"), layout);
        assertTrue(layout.contains("\"x\""), layout);
        assertTrue(layout.contains("\"y\""), layout);
        assertTrue(layout.contains("value.x()"), layout);
        assertTrue(layout.contains("value.y()"), layout);
        assertTrue(layout.contains("new Point("), layout);
    }

    @Test
    void generatesLayoutForNestedRecord(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Inner.java"),
                "package demo;\npublic record Inner(long id, String label) {}\n");

        Files.writeString(sourceDir.resolve("demo/Outer.java"),
                "package demo;\npublic record Outer(int seq, Inner inner) {}\n");

        Files.writeString(sourceDir.resolve("demo/Pipe.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "import uk.co.palmr.gennaker.annotations.Codecs;",
                "@Topic(messageCodec = Codecs.JSON)",
                "public interface Pipe {",
                "    void send(Outer msg);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Inner.java"),
                        sourceDir.resolve("demo/Outer.java"),
                        sourceDir.resolve("demo/Pipe.java")));
        assertTrue(success, "Compilation should succeed for nested records");

        assertTrue(Files.exists(outputDir.resolve("demo/Outer__layout.java")), "Outer__layout should be generated");
        assertTrue(Files.exists(outputDir.resolve("demo/Inner__layout.java")), "Inner__layout should be generated");

        final var outerLayout = Files.readString(outputDir.resolve("demo/Outer__layout.java"));
        assertTrue(outerLayout.contains("Inner__layout.encode"), outerLayout);
        assertTrue(outerLayout.contains("Inner__layout.decode"), outerLayout);
    }

    @Test
    void generatesLayoutForRecordWithListField(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Basket.java"),
                "package demo;\nimport java.util.List;\npublic record Basket(String name, List<Integer> items) {}\n");

        Files.writeString(sourceDir.resolve("demo/Store.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "import uk.co.palmr.gennaker.annotations.Codecs;",
                "@Topic(messageCodec = Codecs.JSON)",
                "public interface Store {",
                "    void checkout(Basket basket);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Basket.java"), sourceDir.resolve("demo/Store.java")));
        assertTrue(success, "Compilation should succeed for record with List field");

        final var layout = Files.readString(outputDir.resolve("demo/Basket__layout.java"));
        assertTrue(layout.contains("json.append('[')"), layout);
        assertTrue(layout.contains("json.append(']')"), layout);
        assertTrue(layout.contains("reader.expectArrayStart()"), layout);
    }

    @Test
    void nonRecordClassWithoutLayoutSpecProducesError(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/BadType.java"),
                "package demo;\npublic class BadType { public int x; }\n");

        Files.writeString(sourceDir.resolve("demo/BadTopic.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "import uk.co.palmr.gennaker.annotations.Codecs;",
                "@Topic(messageCodec = Codecs.JSON)",
                "public interface BadTopic {",
                "    void send(BadType payload);",
                "}",
                ""));

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(outputDir));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classOutputDir));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, currentClasspath());
            final Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromPaths(
                    List.of(sourceDir.resolve("demo/BadType.java"), sourceDir.resolve("demo/BadTopic.java")));
            final var task = compiler.getTask(null, fileManager, diagnostics, null, null, sources);
            task.setProcessors(List.of(new TopicAnnotationProcessor()));
            final boolean success = task.call();
            assertTrue(!success, "Compilation should fail for unsupported class type");
        }

        final var msgs = diagnostics.getDiagnostics().stream()
                .map(d -> d.getMessage(null)).collect(Collectors.joining("\n"));
        assertTrue(msgs.contains("@LayoutSpec") || msgs.contains("is not a record"), msgs);
    }

    @Test
    void wireNameAnnotationOverridesFieldName(@TempDir final Path workDir) throws IOException {
        final var sourceDir = workDir.resolve("src");
        final var outputDir = workDir.resolve("gen");
        final var classOutputDir = workDir.resolve("classes");
        Files.createDirectories(sourceDir.resolve("demo"));
        Files.createDirectories(outputDir);
        Files.createDirectories(classOutputDir);

        Files.writeString(sourceDir.resolve("demo/Vec.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.layout.WireName;",
                "public record Vec(",
                "    @WireName(\"px\") int x,",
                "    @WireName(\"py\") int y",
                ") {}",
                ""));

        Files.writeString(sourceDir.resolve("demo/Drawing.java"), String.join("\n",
                "package demo;",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "import uk.co.palmr.gennaker.annotations.Codecs;",
                "@Topic(messageCodec = Codecs.JSON)",
                "public interface Drawing {",
                "    void move(Vec vector);",
                "}",
                ""));

        final boolean success = compile(sourceDir, outputDir, classOutputDir,
                List.of(sourceDir.resolve("demo/Vec.java"), sourceDir.resolve("demo/Drawing.java")));
        assertTrue(success, "Compilation should succeed");

        final var layout = Files.readString(outputDir.resolve("demo/Vec__layout.java"));
        assertTrue(layout.contains("\"px\""), layout);
        assertTrue(layout.contains("\"py\""), layout);
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
                diagnostics.getDiagnostics().forEach(d -> System.err.println(d.getMessage(null)));
            }
            return ok;
        }
    }

    private static List<Path> currentClasspath() {
        return java.util.Arrays.stream(
                System.getProperty("java.class.path", "").split(java.io.File.pathSeparator))
                .filter(s -> !s.isBlank())
                .map(Path::of)
                .filter(java.nio.file.Files::exists)
                .toList();
    }
}
