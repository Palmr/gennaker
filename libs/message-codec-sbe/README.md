# gennaker-message-codec-sbe

SBE (Simple Binary Encoding) codec implementation for Gennaker. Registers under the name `"sbe"` (see `Codecs.SBE`).

At compile time the codec generates an SBE XML schema from the topic's `TypeShape` graph, invokes the SBE tool to produce encoder/decoder classes, then emits proxy bodies that call those classes. The result is allocation-free, fixed-layout binary messages at runtime.

## Components

| Class | Role |
|-------|------|
| `SbeMessageCodecGenerator` | `MessageCodecGenerator` SPI impl; entry point for the codec. |
| `SbeXmlBuilder` | Builds the SBE XML schema (`<sbe:messageSchema>`) from the topic's `TypeShape`s. |
| `SbeCodecBodyEmitter` | Emits SBE encoder/decoder calls into the generated publisher and subscriber proxy bodies. |
| `SbeTypes` | Maps Java types to SBE primitive types. |
| `AnnotationFilerOutputManager` | Bridges the SBE tool's `OutputManager` to the annotation processor's `Filer`. |

**Dependencies:** `:codec-spi`, `:layout-api`, `uk.co.real-logic:sbe-tool`.