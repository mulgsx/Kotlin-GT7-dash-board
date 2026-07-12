# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run lint
./gradlew lint

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.gt7dashjp.ExampleUnitTest"

# Install on connected device
./gradlew installDebug
```

## Architecture

The app receives Salsa20-encrypted UDP telemetry from Gran Turismo 7 (PS5, port 33740) and displays it as a real-time dashboard on Android.

### Data flow

```
GT7 (PlayStation) ──UDP:33740──► GT7Communication
                                      │
                    Salsa20 decrypt   │
                    + parse fields    │
                                      ▼
                              onPacketReceived(count, rpm)
                                      │
                         withContext(Dispatchers.Main)
                                      │
                                      ▼
                              MainActivity (mutableStateOf)
                                      │
                                      ▼
                              GT7PacketCounterUI (Compose)
```

### Key files

| File | Role |
|------|------|
| `GT7Communication.kt` | UDP receive loop (Dispatchers.IO coroutine), Salsa20 decryption, field extraction |
| `MainActivity.kt` | Holds GT7Communication instance, owns Compose state (packetCount, rpm) |
| `ui/theme/GT7PacketCounterUI.kt` | All UI — landscape dashboard + settings drawer |

### GT7 Protocol

- **Receive port**: 33740
- **Send port (heartbeat)**: 33739 — send `"A"` to keep GT7 streaming; repeated every 100 packets or on 5-second timeout
- **Encryption**: Salsa20, key = `"Simulator Interface Packet GT7 ver 0.0"` (padded to 32 bytes)
- **IV construction**: read 4 bytes at offset `0x40` as little-endian int (`iv1`), then `iv2 = iv1 XOR 0xDEADBEAF`; IV = `[iv2, iv1]` in little-endian
- **Magic number**: first 4 bytes of decrypted packet must equal `0x47375330`; discard packet otherwise

### Packet field reference

See `packet_structure.md` for the full offset table. Key fields used in code:

| Offset | Field | Type | Notes |
|--------|-------|------|-------|
| 0x3C | `rpm` | Float | Engine RPM |

### Dependencies

- `org.bouncycastle:bcprov-jdk15to18` — Salsa20Engine for packet decryption
- `androidx.lifecycle:lifecycle-runtime-ktx` — provides Kotlin Coroutines (no separate coroutines dep needed)
- Jetpack Compose BOM 2024.09.00 + Material3

### Orientation

The activity is locked to **landscape** (`screenOrientation="landscape"` in AndroidManifest.xml).
