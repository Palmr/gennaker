# gennaker-annotation-processor

`javac` annotation processor that generates publisher and subscriber proxy classes for every `@Topic`-annotated interface.

## What it generates

For a topic interface `com.example.OrderEvents`:

| Class | Role |
|-------|------|
| `com.example.OrderEvents__pub_proxy` | Implements `OrderEvents`; each method serialises its arguments and hands the buffer to the `Transport`. |
| `com.example.OrderEvents__sub_proxy` | Implements `MessageHandler`; `onMessage` deserialises the buffer and dispatches to the user's `OrderEvents` implementation. |

## How it works

1. `TopicAnnotationProcessor` is triggered by `@Topic` via `javax.annotation.processing`.
2. It resolves the codec named in `@Topic(messageCodec = "...")` by loading `MessageCodecGenerator` implementations through `ServiceLoader`.
3. The chosen codec's `CodecBodyEmitter` fills in the serialisation logic; the processor writes the proxy shell around it.
4. Registered via `@AutoService(Processor.class)` — no manual `META-INF/services` entry needed.

**Dependencies:** `:annotations`, `:codec-spi`, `:layout-api`, and any codec module on the annotation-processor classpath.