# gennaker-transport-spi

Minimal SPI shared between the Gennaker runtime, generated proxies, and pluggable transport implementations. Two interfaces, no behaviour.

| Type | Role |
|------|------|
| `Transport` | Delivery mechanism contract. Implementations route encoded message bytes for a topic to subscribed handlers. Implemented by `DirectTransport` (`:core`), `AeronTransport` (`:transport-aeron`), and any custom transport. |
| `MessageHandler<T>` | Receives raw encoded messages for topic `T`. Implemented by the generated `__sub_proxy` classes; the type parameter keeps handler/topic pairings type-safe at the `Transport.subscribe` boundary. |

Lives in its own module because the annotation processor and codec emitters generate source that references these types, but must not depend on `:core` (which would create a cycle through `annotationProcessor`).

**Dependencies:** `org.agrona:agrona` (for `DirectBuffer`).
