#   WireDog VPN - Android Application

Copyright (c) 2026 WireDog Technologies

[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Android 9+](https://img.shields.io/badge/Android-9%2B-green.svg)](https://developer.android.com)

## Features

- **AmneziaWG Protocol** - WireGuard fork with handshake obfuscation — traffic appears as noise, bypassing deep packet inspection
- **Secure Authentication** - Email/password and anonymous account options
- **Kill Switch** - Prevents data leaks if VPN connection drops
- **Split Tunneling** - Choose which apps and IP ranges route through the VPN
- **Auto-Connect** - Automatically reconnect to VPN on boot and network changes
- **Server Selection** - Choose from multiple U.S. VPN servers with favorites
- **Subscription Management** - Built-in subscription validation and management
- **On-Device Logging** - Privacy-respecting debug logs with PII redaction

## Requirements

- **Android Studio**: Ladybug (2024.2) or later
- **JDK**: 17 or later
- **Android SDK**: API 28 (Android 9.0) minimum, API 36 target
- **Kotlin**: 2.0 or later

### VPN Permission

Building and running this project requires the `BIND_VPN_SERVICE` permission, which Android grants automatically when the user accepts the VPN connection prompt. No special developer account is required to develop locally.

## Setup

1. **Clone the repository**

2. **Open the project in Android Studio**
   - Let Gradle sync complete
   - Select the `debug` build variant for local development
   - Run on a device or emulator (API 28+)

3. **Connect a device or start an emulator**
   - VPN functionality requires a real device for full testing; emulators have limited VPN support

## Project Structure

```
wiredog-vpn-android/
└── app/
    └── src/main/java/com/wiredog/vpn/
        ├── data/                    # Repositories, API DTOs, remote/local data sources
        ├── domain/                  # Data models
        ├── ui/                      # Jetpack Compose screens, ViewModels, components
        ├── di/                      # Hilt dependency injection modules
        ├── service/                 # VPN foreground service and AmneziaWG tunnel manager
        └── receiver/                # Boot receiver for auto-connect
├── gradle/
│   └── libs.versions.toml           # Centralized dependency versions
└── app/
    └── build.gradle.kts             # App module build configuration
```

## Security Issues

**Do not open public GitHub issues for security vulnerabilities.**

If you believe you have found a security vulnerability, please email support@wiredogvpn.com with a description of the vulnerability, steps to reproduce, potential impact, and suggested fix if available.

## License

Licensed under the **GNU General Public License v3 (GPLv3)**. See [`LICENSE`](LICENSE) for details.

This project includes the AmneziaWG Android tunnel library (Apache 2.0 License). See [`ACKNOWLEDGMENTS.md`](ACKNOWLEDGMENTS.md) for full attribution.

## Questions?

- Open a [GitHub Issue](https://github.com/[fill]/wiredog-vpn-android/issues)
- Read [`CONTRIBUTING.md`](CONTRIBUTING.md) for contribution guidelines
