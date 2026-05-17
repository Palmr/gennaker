# gennaker-transport-aeron

Aeron-backed `Transport` implementation for Gennaker. IPC and multi-process delivery via an embedded Aeron media driver.

Each `@Topic` interface is mapped to its own Aeron stream id; publishers offer encoded messages onto an Aeron `Publication`, subscribers consume them via a polling `AgentRunner` thread. Suitable for low-latency in-host and cross-host messaging.

```java
Gennaker gennaker = new Gennaker(new AeronTransport());

OrderEvents pub = gennaker.publisher(OrderEvents.class);
gennaker.subscribe(OrderEvents.class, myOrderEventsImpl);

pub.placed(42L, 99.5);

gennaker.shutdown();   // closes Aeron client and the embedded media driver
```

All topics share the channel URI `aeron:ipc?alias=gennaker`; stream id is derived from the topic class. Construction launches an embedded `MediaDriver`; `shutdown()` tears it down.

## Components

| Class | Role |
|-------|------|
| `AeronTransport` | `Transport` SPI impl backed by an embedded Aeron media driver. |

**Dependencies:** `:core`, `io.aeron:aeron-all`.
