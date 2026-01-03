# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Andromitron is an Android application for implementing a local HTTPS proxy with domain-based filtering capabilities. The project aims to create a VPN-based proxy service that can filter network traffic at the system level.

## Development Commands

### Build and Development
```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK on connected device
./gradlew installDebug
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run unit tests for debug build
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "net.yalab.andromitron.ExampleUnitTest"
```

### Code Quality
```bash
# Run lint checks
./gradlew lint

# Generate lint report
./gradlew lintDebug
```

## Project Architecture

### Core Components (Planned)
Based on the specification in `android_proxy_spec.md`:

- **VPN Service**: System-level traffic capture using Android's VPNService
- **Filter Engine**: Domain-based filtering logic (blocklist/whitelist)
- **TUN Interface**: Network packet processing
- **Database Layer**: Room database for storing blocklists and logs
- **UI Layer**: Jetpack Compose interface (planned)
- **Network Layer**: OkHttp for actual network communication

### Current State
- Basic Android project structure with standard gradle configuration
- Package: `net.yalab.andromitron`
- Target SDK: 36, Min SDK: 35
- Kotlin-based development
- Standard test structure in place

### Key Technical Approaches
1. **VPNService + TUN Interface**: Primary method for system-wide traffic interception
2. **DNS-level blocking**: For HTTPS traffic filtering
3. **SNI parsing**: Extract domain names from TLS handshakes
4. **Hybrid filtering**: Combination of DNS monitoring and SNI analysis

### Required Permissions (from spec)
- `BIND_VPN_SERVICE`
- `INTERNET` 
- `FOREGROUND_SERVICE`

## File Structure

- `app/`: Main application module
- `app/src/main/`: Application source code
- `app/src/test/`: Unit tests
- `app/src/androidTest/`: Instrumented tests
- `android_proxy_spec.md`: Detailed technical specification and architecture documentation
- `gradle/libs.versions.toml`: Dependency version management

## Development Notes

- The project is in initial setup phase - only basic Android project structure exists
- Core VPN proxy functionality needs to be implemented based on the specification
- UI design will use Jetpack Compose for modern Android development
- Security considerations include VPN permission management and local data encryption