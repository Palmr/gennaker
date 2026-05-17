# gennaker-codec-spi

Service Provider Interface (SPI) that codec modules implement to plug into the annotation processor.

## Key types

| Type | Role |
|------|------|
| `MessageCodecGenerator` | SPI entry point. Declares a stable `name()` (e.g. `"sbe"`) matched against `@Topic(messageCodec = "...")`, and a `generate(TopicContext)` method that returns a `CodecBodyEmitter`. Discovered via `ServiceLoader`. |
| `CodecBodyEmitter` | Fills in the codec-specific portions of the generated publisher and subscriber proxies: imports, field declarations, method bodies. |
| `TopicContext` | Per-topic view handed to a `MessageCodecGenerator`: the interface element, package, methods, max message size, and the map of reachable `TypeShape`s. |
| `TypeShape` / `FieldShape` / `FieldKind` | Wire-level type descriptors built from record components or `Layout` ASTs. Consumed by codec generators to emit schema entries and helper classes. |

## Implementing a codec

1. Implement `MessageCodecGenerator` and `CodecBodyEmitter`.
2. Register the generator in `META-INF/services/uk.co.palmr.gennaker.codec.MessageCodecGenerator`.
3. Put the module on the annotation-processor classpath.