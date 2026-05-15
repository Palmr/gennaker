# Gennaker
## Ergonomic messaging for Java

Annotate an interface, get a typed publisher and subscriber wired to a 
configurable transport. No hand-rolled encoders, no manual dispatch, no glue.

## At a glance

```java
@Topic
public interface Ping {
    void doPing(String message);
    void doRepeat(int count, String message);
}
```

```java
var gennaker = new Gennaker(new AeronTransport());

Ping pingPublisher = gennaker.publisher(Ping.class);
gennaker.subscribe(Ping.class, new MyPingHandler());

pingPublisher.doPing("Hello");
```

The annotation processor generates `Ping__pub_proxy` (implements `Ping`,
encodes each call and pushes it onto the transport) and `Ping__sub_proxy`
(decodes inbound messages and dispatches to the registered handler).

## How it works

For each `@Topic`-annotated interface, the annotation processor:

1. Picks a **message codec** by name (`@Topic(messageCodec = Codecs.JSON)`,
   defaults to SBE).
2. Asks the codec to do any codec-specific work (e.g. SBE generates encoder/
   decoder classes from a derived schema; JSON has no side-files).
3. Generates a publisher proxy that implements the interface and a
   subscriber proxy that implements `MessageHandler`, both wired to the
   chosen `Transport`.

Codecs are discovered via `java.util.ServiceLoader`, so adding a new wire
format is a new module. The annotation processor never has to know about
it.

## Modules

```
:libs:core                 # runtime: Gennaker entry point, DirectTransport, AeronTransport
:libs:annotations          # @Topic and Codecs constants, the only module users see
:libs:annotation-processor # discovers codecs, generates proxy shells
:libs:codec-spi            # MessageCodecGenerator / CodecBodyEmitter / TopicContext
:libs:message-codec-sbe    # SBE codec (binary, schema-derived, fast)
:libs:message-codec-json   # JSON codec (human-readable, easier debugging)
:example                   # demo: heterogeneous SBE + JSON topics over Aeron
```

A typical user depends on `:libs:annotations` and `:libs:core` plus whichever
codec module(s) they want as `annotationProcessor`.

## Codecs

| Codec | Module                     | Wire format            |
| ----- | -------------------------- | ---------------------- |
| SBE   | `:libs:message-codec-sbe`  | Binary (SBE schema)    |
| JSON  | `:libs:message-codec-json` | `{"m":..., "a":[...]}` |

Selected per-topic:

```java
@Topic(messageCodec = Codecs.JSON)
public interface Pong { void doPong(String message); }
```

Both supported parameter sets are currently primitives + `String`.

## Transports

| Transport         | Use case                          |
| ----------------- | --------------------------------- |
| `DirectTransport` | In-process, synchronous dispatch  |
| `AeronTransport`  | IPC / multi-process via Aeron     |

Choice is currently constructor-driven; pushing the choice into the
annotation is on the roadmap.

## Status

A working proof-of-concept, used end-to-end in the `example/` module.

Generating publishers and subscribers from a Java interface works. The actual 
API isn't very fleshed out yet.

- Only `void` methods (no return values / RPC yet).
- Parameters limited to primitives + `String`.
- Transport choice is hard-coded by the application, not derived from the
  topic.
- Backpressure handling is a `TODO` in the generated publisher.

## Roadmap

**API surface**

- [ ] RPC: interface methods that return values
- [ ] Richer parameter types: arbitrary records, collections, nullables
- [ ] User-supplied custom serialisers for specific types

**Transports**

- [ ] Annotation-driven transport selection (the `Transport` enum exists but
      isn't wired)
- [ ] Batching / transactional sends
- [ ] Backpressure: configurable block-vs-drop when the queue is full
- [ ] Reliability levels (at-most-once / at-least-once)

**Codecs**

- [x] Pluggable codec SPI with `ServiceLoader` discovery
- [x] SBE codec
- [x] JSON codec
- [ ] Schema evolution story for binary codecs

**Project**

- [ ] A richer example showing multi-process operation over Aeron
- [ ] Publish to Maven Central
- [ ] Document the SPI for third-party codec authors

## Building

```
./gradlew build
```

Requires JDK 21. The example uses Aeron, which needs
`--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED` (already wired into
the build).
