# gennaker-core

Runtime entry point for publishing and subscribing to topics.

`Gennaker` wraps a `Transport` and exposes two operations:

```java
Gennaker gennaker = new Gennaker(transport);

// Get a publisher — returns the generated __pub_proxy class
OrderEvents pub = gennaker.publisher(OrderEvents.class);
pub.placed(42L, 99.5);

// Register a subscriber — wraps your impl in a generated __sub_proxy
gennaker.subscribe(OrderEvents.class, myOrderEventsImpl);

gennaker.shutdown();
```

`ClassHunter` resolves the generated proxy classes by convention: `<CanonicalName>__pub_proxy` and `<CanonicalName>__sub_proxy`. These classes are produced at compile time by `:annotation-processor`.

**Dependencies:** `:annotations`, transport runtime (not bundled here).