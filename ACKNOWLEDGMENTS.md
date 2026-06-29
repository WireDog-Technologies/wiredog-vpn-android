# Acknowledgments

WireDog VPN Android is built on the shoulders of excellent open-source projects. We are grateful for:

## AmneziaWG Android Tunnel Library
- **License:** Apache License 2.0
- **Copyright:** © 2023-2026 Amnezia VPN. All Rights Reserved.
- **Source:** https://github.com/amnezia-vpn/amneziawg-android
- **Description:** A WireGuard fork with handshake obfuscation (junk packets, randomized magic bytes). The `tunnel` module (`org.amnezia.awg`) provides the low-level AWG tunnel interface used by this app to establish and manage VPN connections over UDP port 443.

---

## Jetpack Compose
- **License:** Apache License 2.0
- **Copyright:** © The Android Open Source Project
- **Source:** https://developer.android.com/jetpack/compose
- **Description:** Android's modern declarative UI toolkit used to build the entire WireDog VPN interface.

---

## Hilt (Dagger)
- **License:** Apache License 2.0
- **Copyright:** © Google LLC
- **Source:** https://dagger.dev/hilt/
- **Description:** Dependency injection library for Android used throughout the app for service and repository injection.

---

## Retrofit & OkHttp
- **License:** Apache License 2.0
- **Copyright:** © Square, Inc.
- **Source:** https://square.github.io/retrofit/ | https://square.github.io/okhttp/
- **Description:** HTTP client and type-safe REST adapter used for all WireDog API communication.

---

## Moshi
- **License:** Apache License 2.0
- **Copyright:** © Square, Inc.
- **Source:** https://github.com/square/moshi
- **Description:** JSON serialization library used for API request and response parsing.

---

## Coil
- **License:** Apache License 2.0
- **Copyright:** © Coil Contributors
- **Source:** https://coil-kt.github.io/coil/
- **Description:** Image loading library for Compose used to render the U.S. server map SVG.

---

## Kotlin Coroutines
- **License:** Apache License 2.0
- **Copyright:** © JetBrains s.r.o.
- **Source:** https://github.com/Kotlin/kotlinx.coroutines
- **Description:** Asynchronous programming library used throughout the app for non-blocking network and VPN operations.

---

## AndroidX Security Crypto (EncryptedSharedPreferences)
- **License:** Apache License 2.0
- **Copyright:** © The Android Open Source Project
- **Source:** https://developer.android.com/jetpack/androidx/releases/security
- **Description:** AES256-GCM encrypted SharedPreferences backed by Android Keystore, used to store all auth tokens and credentials.

---

## License Compatibility

This project is licensed under the GNU General Public License v3 (GPLv3), which is compatible with the MIT and Apache 2.0 licenses of all dependencies listed above. All contributions and derived works must comply with the terms of the GPLv3.

For more information about these licenses:
- GPL v3: https://www.gnu.org/licenses/gpl-3.0.html
- MIT: https://opensource.org/licenses/MIT
- Apache 2.0: https://www.apache.org/licenses/LICENSE-2.0

---
