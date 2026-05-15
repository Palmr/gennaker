package uk.co.palmr.gennaker.annotation.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicProxyAnnotationProcessorTest {

    @BeforeEach
    void resetFakeCodecState() {
        FakeMessageCodecGenerator.INVOCATIONS.clear();
    }

    @Test
    void invokesCodecAndGeneratesProxiesForTopicInterface() {
        final var source = JavaFileObjects.forSourceLines("demo.Greeter",
                "package demo;",
                "",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "",
                "@Topic(messageCodec = \"fake\")",
                "public interface Greeter {",
                "    void hello(String name);",
                "}");

        final Compilation compilation = javac()
                .withProcessors(new TopicAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("demo.Greeter__pub_proxy");
        assertThat(compilation).generatedSourceFile("demo.Greeter__sub_proxy");
        assertEquals(1, FakeMessageCodecGenerator.INVOCATIONS.size());
        assertEquals("demo.Greeter", FakeMessageCodecGenerator.INVOCATIONS.get(0));
    }

    @Test
    void unknownCodecProducesCompilationErrorListingDiscoveredCodecs() {
        final var source = JavaFileObjects.forSourceLines("demo.Bad",
                "package demo;",
                "",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "",
                "@Topic(messageCodec = \"nope\")",
                "public interface Bad {",
                "    void noop();",
                "}");

        final Compilation compilation = javac()
                .withProcessors(new TopicAnnotationProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("No MessageCodecGenerator registered for messageCodec=\"nope\"");
        assertThat(compilation).hadErrorContaining("Discovered codecs: [fake]");
    }

    @Test
    void topicOnNonInterfaceIsRejected() {
        final var source = JavaFileObjects.forSourceLines("demo.NotAnInterface",
                "package demo;",
                "",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "",
                "@Topic(messageCodec = \"fake\")",
                "public class NotAnInterface {",
                "}");

        final Compilation compilation = javac()
                .withProcessors(new TopicAnnotationProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Topic annotation can only be applied to interfaces");
    }

    @Test
    void nonVoidMethodsCauseProcessorFailure() {
        final var source = JavaFileObjects.forSourceLines("demo.HasReturn",
                "package demo;",
                "",
                "import uk.co.palmr.gennaker.annotations.Topic;",
                "",
                "@Topic(messageCodec = \"fake\")",
                "public interface HasReturn {",
                "    int answer();",
                "}");

        final Compilation compilation = javac()
                .withProcessors(new TopicAnnotationProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertTrue(compilation.diagnostics().stream()
                        .anyMatch(d -> d.getMessage(null).contains("Only void methods supported for now")),
                () -> "Expected diagnostic about void-only methods, got: " + compilation.diagnostics());
    }
}
