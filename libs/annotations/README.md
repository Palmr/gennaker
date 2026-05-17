# gennaker-annotations

User-facing annotations applied to topic interfaces.

| Type | Purpose |
|------|---------|
| `@Topic` | Marks an interface as a Gennaker message topic. Sets `maxMessageSize` and selects a `messageCodec` by name. |
| `Codecs` | Constants for the built-in codec names (`"sbe"`, `"json"`). |
| `Transport` | Enum of supported transports (`DIRECT`, `AERON`). |

## Usage

```java
@Topic(messageCodec = Codecs.SBE, maxMessageSize = 512)
public interface OrderEvents {
    void placed(long orderId, double price);
}
```

The annotation processor in `:annotation-processor` picks these up at compile time and generates publisher/subscriber proxy classes.