# Pixel Shrink

Pixel Shrink is a native Android image utility built with Kotlin and Jetpack Compose. Image operations are intended to live in the platform-independent C++ core, with thin platform adapters for Android and a future iOS app.

## Project structure

- `app/src/main/java` — Android UI and platform integration
- `app/src/main/cpp` — portable C++ image-processing core and Android JNI adapter
- `app/src/main/cpp/CMakeLists.txt` — native build configuration

## Prerequisites

- Android Studio with Android SDK Platform 37 and Build Tools installed
- Android NDK (side by side)
- CMake 3.22.1
- JDK 17

Open the project in Android Studio and allow Gradle sync to download the Gradle wrapper and dependencies. Select an emulator or connected Android device, then run the `app` configuration.

The `com.pixelshrink.app` application ID is a working project identifier. Confirm the permanent store ID before release setup.
