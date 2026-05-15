package uk.co.palmr.gennaker.annotation.processor;

import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.MessageCodecGenerator;
import uk.co.palmr.gennaker.codec.TopicContext;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Codec used only by the processor's tests to exercise the SPI without
 * depending on SBE or JSON. Records every {@link TopicContext} it is asked to
 * generate for, and emits the minimal proxy bodies needed to satisfy the
 * generated shells.
 */
public final class FakeMessageCodecGenerator implements MessageCodecGenerator {

    public static final String NAME = "fake";

    /** Cleared on each {@link #generate(TopicContext)} so tests can inspect the latest run. */
    public static final List<String> INVOCATIONS = new ArrayList<>();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CodecBodyEmitter generate(final TopicContext context) {
        INVOCATIONS.add(context.packageName() + "." + context.interfaceName());
        return new FakeEmitter(context.methods());
    }

    private static final class FakeEmitter implements CodecBodyEmitter {
        private final List<ExecutableElement> methods;

        FakeEmitter(final List<ExecutableElement> methods) {
            this.methods = List.copyOf(methods);
        }

        @Override
        public String publisherImports() {
            return "import uk.co.palmr.gennaker.Transport;\n\n";
        }

        @Override
        public String publisherFields() {
            return "";
        }

        @Override
        public String publisherMethodBody(final ExecutableElement method) {
            return "        // fake codec: " + method.getSimpleName() + "\n";
        }

        @Override
        public String subscriberImports() {
            return "import org.agrona.DirectBuffer;\nimport uk.co.palmr.gennaker.MessageHandler;\n\n";
        }

        @Override
        public String subscriberFields() {
            return "";
        }

        @Override
        public String subscriberOnMessageBody() {
            final var sb = new StringBuilder();
            sb.append("        // fake codec dispatch over ").append(methods.size()).append(" method(s)\n");
            return sb.toString();
        }
    }
}
