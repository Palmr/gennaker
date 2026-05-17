# gennaker-layout-api

API for describing how a user-defined type maps to wire fields.

Java records are handled automatically — no `Layout` needed. For non-record types, implement `Layout<T>`:

```java
@LayoutSpec
public final class OrderLayout implements Layout<Order> {

    @Override
    public void encode(Order value, EncodeSink sink) {
        sink.writeLong("orderId", value.orderId());
        sink.writeDouble("price", value.price());
    }

    @Override
    public Order decode(DecodeSource source) {
        return new Order(source.readLong("orderId"), source.readDouble("price"));
    }
}
```

The annotation processor reads the `encode`/`decode` bodies at compile time to derive the field list (names, types, order). **The bodies must be linear** — no conditionals, loops, or helper calls.

## Key types

| Type | Role |
|------|------|
| `Layout<T>` | Contract for encoding a type to an `EncodeSink` and decoding it from a `DecodeSource`. |
| `@LayoutSpec` | Marks a `Layout` implementation for discovery by the annotation processor. |
| `EncodeSink` | Field-by-field write API (`writeInt`, `writeString`, `writeObject`, `writeList`, `writeMap`, …). |
| `DecodeSource` | Field-by-field read API (symmetric with `EncodeSink`). |
| `@WireName` | Overrides the on-wire name for a type, record component, or parameter without changing the Java identifier. |