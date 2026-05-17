# gennaker-message-codec-json

JSON codec implementation for Gennaker. Registers under the name `"json"` (see `Codecs.JSON`).

Wire format is a compact JSON object:

```json
{"m":"placed","a":[42, 99.5]}
```

`"m"` is always the method name (first field); `"a"` is the argument array. Generated subscriber proxies drive `JsonReader` in lock-step with the expected method signature, so no schema validation overhead at runtime.

## Components

| Class | Role |
|-------|------|
| `JsonMessageCodecGenerator` | `MessageCodecGenerator` SPI impl; registered via `META-INF/services`. |
| `JsonCodecBodyEmitter` | Emits `StringBuilder`-based encoding and `JsonReader`-based decoding into generated proxies. |
| `JsonReader` | Minimal positional JSON parser used by generated subscriber proxies. Zero external dependencies. |

**Dependencies:** `:codec-spi`, `:layout-api`.