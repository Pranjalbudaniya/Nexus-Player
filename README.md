<div align="center">

<img src="app/src/main/res/drawable/ic_nexus_logo.png" width="128" height="128" alt="Nexus Player Logo" style="border-radius: 28px;" />

# Nexus Player

**A modern, privacy-first, offline Android video player crafted with Jetpack Compose & AndroidX Media3.**

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg?style=for-the-badge)](https://github.com/Pranjalbudaniya/Nexus-Player/releases)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028%2B)-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3 ExoPlayer](https://img.shields.io/badge/AndroidX-Media3%201.5.1-FF6F00.svg?style=for-the-badge)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)

[**Features**](#-features) • [**Architecture**](#-architecture--tech-stack) • [**Getting Started**](#-getting-started) • [**Modular Structure**](#-modular-structure) • [**Testing**](#-testing) • [**License**](#-license)

</div>

---

## 📖 Overview

**Nexus Player** is an uncompromising, feature-rich offline media player engineered for Android. Built from the ground up with modern Android architecture guidelines, it pairs the raw power of **AndroidX Media3 (ExoPlayer)** with an elegant, responsive UI designed entirely in **Jetpack Compose** and **Material 3**.

Nexus Player operates **100% offline**: no user accounts, no tracking telemetry, no cloud dependencies, and zero advertisements. Your media library and viewing habits stay completely private on your device.

---

## ✨ Features

### 🎬 High-Performance Playback Engine
* **Hardware-Accelerated Codecs**: Ultra-low latency playback powered by AndroidX Media3 ExoPlayer.
* **Broad Container & Codec Support**: Seamlessly plays `MP4`, `MKV`, `WebM`, `AVI`, `MOV`, `TS`, `FLV`, and more.
* **Dynamic Aspect Ratios**: Instant switching between *Fit to Screen*, *Fill / Crop*, *Stretch*, *16:9*, and *4:3*.
* **Precision Seek & Speed**: Interactive scrubber with buffered progress indication, step intervals (±5s, ±10s, ±30s), and adjustable playback rates (0.25x – 2.0x).
* **Network Stream Playback**: Play video feeds directly from raw HTTP and HTTPS streams.

### 👆 Intuitive Player Gestures
* **Left-Edge Swipe**: Smooth vertical gesture for display brightness.
* **Right-Edge Swipe**: Smooth vertical gesture for media audio volume.
* **Double-Tap Seeking**: Rapidly jump forward or backward with visual ripple animations.
* **Hold to Boost**: Long-press anywhere on the screen for temporary 2.0x speed boost.
* **System Lock Mode**: Lock controls to prevent accidental touches during media sessions.

### 🔊 Advanced Audio & DSP Processing
* **5-Band Hardware Equalizer**: Hardware DSP audio shaping with built-in presets (*Flat*, *Bass Boost*, *Vocal*, *Treble*, *Rock*, *Pop*, *Custom*).
* **Audio Boost up to 200%**: Amplify quiet dialogue and low-gain video tracks cleanly.
* **Audio Sync Offset**: Real-time delay adjustment (±500ms with ±50ms fine-tuning) to eliminate Bluetooth latency issues.
* **Multi-Track Switching**: Seamlessly toggle between multiple audio tracks and languages.
* **Per-Video Memory**: Automatically remembers custom audio delay settings per video.

### 💬 Comprehensive Subtitle Engine
* **Format Flexibility**: Comprehensive support for embedded subtitle tracks and external subtitle files (`.srt`, `.vtt`, `.ass`, `.ssa`).
* **Real-Time Subtitle Synchronization**: Live sync delay offset adjustments on the fly.
* **Visual Customization**: Tailor subtitle text size, font color, outline stroke, and background container opacity to suit any lighting condition.

### 📁 Smart Media Library & File Management
* **Scoped Storage Access Framework (SAF)**: Modern, permission-safe storage directory management with automatic background scanning.
* **Folder Browsing**: Traverse device directory structures with media counts and thumbnail previews.
* **File Operations**: In-app video file renaming, folder moves, and deletion with MediaStore synchronization.
* **Technical Codec Inspector**: Deep metadata dialog displaying resolution, container, bitrate, frame rate, audio channels, and sampling frequency.

### 📊 Local Analytics & Playback History
* **100% Offline Analytics**: Local watch metrics including total watch time, completed video counts, and format distributions.
* **7-Day Activity Chart**: Visual bar graph summarizing daily viewing activity over the past week.
* **Watch History**: Resume playback right where you left off with playback count tracking and continue-watching cards.

### 🎨 Material 3 Design System
* **4dp Spatial Grid**: Pixel-perfect spacing, compact typography, and minimum 48dp touch targets.
* **Adaptive Themes**: Full support for Light, Dark, System, and **AMOLED True Black** modes.
* **Dynamic Material You**: Harmonious system accent extraction with custom fallback palettes.

---

## 🏗️ Architecture & Tech Stack

Nexus Player adheres to **Clean Architecture** principles across a decoupled, multi-module Gradle layout:

```
┌────────────────────────────────────────────────────────┐
│                        :app                            │
└────────────────────────────────────────────────────────┘
                           │
      ┌────────────────────┼────────────────────┐
      ▼                    ▼                    ▼
:feature:player     :feature:home        :feature:library
:feature:search     :feature:playlists   :feature:more
:feature:onboarding        │
      │                    │                    │
      └────────────────────┼────────────────────┘
                           ▼
                    :core:navigation
                           │
      ┌────────────────────┼────────────────────┐
      ▼                    ▼                    ▼
  :core:media         :core:player        :core:scanner
  :core:database      :core:designsystem  :core:ui
                           │
                           ▼
                     :core:common
```

### Core Technologies
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
* **Media Engine**: [AndroidX Media3 / ExoPlayer 1.5.1](https://developer.android.com/media/media3)
* **Database**: [AndroidX Room 2.6.1](https://developer.android.com/training/data-storage/room) with automated SQLite schema migrations
* **Dependency Injection**: [Google Dagger-Hilt 2.51.1](https://dagger.dev/hilt/)
* **Preferences**: [AndroidX DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
* **Asynchronous Primitives**: Kotlin Coroutines & Reactive `StateFlow` / `SharedFlow`
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/)

---

## 📁 Modular Structure

| Module | Responsibility |
|---|---|
| `:app` | Application entry point, Hilt dependency assembly, and manifest declaration |
| `:core:common` | Base models, dispatcher providers, settings models, and utility extensions |
| `:core:database` | Room database definition, DAOs, entities, and migration specifications |
| `:core:designsystem`| Material 3 design tokens, color schemes, typography, spacing, and shapes |
| `:core:media` | MediaStore access layer, SAF document tree operations, and file management |
| `:core:navigation` | Type-safe Compose navigation routes and destination arguments |
| `:core:player` | ExoPlayer lifecycle wrapper, equalizer controller, and audio effects DSP |
| `:core:scanner` | High-efficiency recursive background media scanner and indexing service |
| `:core:ui` | Reusable Compose UI components, top app bars, video cards, dialogs, and sheets |
| `:feature:home` | Home dashboard (Continue Watching, Recently Added, Favorites, Folders) |
| `:feature:library` | Video grid and folder navigation screens with sort and filter options |
| `:feature:player` | Fullscreen landscape/portrait video playback screen and gesture overlay |
| `:feature:playlists`| Custom playlist creation, queue management, and playlist detail screens |
| `:feature:search` | Real-time title and metadata search interface |
| `:feature:more` | Settings accordion, watch history, offline analytics, and about dialogs |
| `:feature:onboarding`| First-launch storage permission guidance and folder selection flow |

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1+) or Meerkat
* **JDK**: OpenJDK 17 or higher
* **Android SDK**: API 36 (Minimum Supported: API 28 / Android 9.0)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/Pranjalbudaniya/Nexus-Player.git
cd Nexus-Player

# Build the Debug APK
./gradlew assembleDebug

# Build the Release APK
./gradlew assembleRelease
```

The generated APK will be available at:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

### Install to Connected Device / Emulator
```bash
./gradlew installDebug
```

---

## 🧪 Testing

Nexus Player includes a comprehensive unit test suite covering ViewModels, UseCases, Repositories, Database Migrations, and Compose UI screens:

```bash
# Run all unit and Robolectric tests across all modules
./gradlew testDebugUnitTest
```

All 531+ tests run hermetically without requiring external network connectivity or cloud services.

---

## 🔒 Privacy & Permissions

Nexus Player adheres strictly to Android privacy standards:
* **Storage Access**: Uses `READ_MEDIA_VIDEO` on Android 13+ (API 33+) and `READ_EXTERNAL_STORAGE` on Android 9–12. Supports Storage Access Framework (SAF) scoped folder trees for fine-grained user control.
* **Internet**: `INTERNET` and `ACCESS_NETWORK_STATE` permissions are utilized strictly for user-initiated network stream URLs (HTTP/HTTPS video playback).
* **Zero Telemetry**: No analytical trackers, third-party advertising SDKs, crash beacons, or remote logging libraries are bundled in the application.

---

## 📄 License

```
Copyright 2026 Pranjal Budaniya

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<div align="center">
Made with ❤️ by <a href="https://github.com/Pranjalbudaniya">Pranjal Budaniya</a>
</div>
