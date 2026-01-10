# HBooks - Audiobook Streaming App

## Overview

HBooks is a modern Android audiobook streaming application that lets users discover, organize, and listen to audiobooks. Built with Kotlin and Jetpack Compose, it delivers a smooth, intuitive experience for audiobook enthusiasts.

## Features

### 🎧 Audio Playback
- Stream audiobooks directly from the cloud
- Background playback with media notification controls
- Playback speed control (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x)
- Sleep timer with customizable duration
- Skip forward/backward controls
- Shuffle and repeat modes

### 📚 Library Management
- Create and manage custom playlists
- Track recently played audiobooks
- Resume playback from where you left off (synced across sessions)
- Add books to favorites

### 🔍 Discovery
- Browse curated audiobook collections
- Search by title, author, or category
- View detailed book information including cover art, author, and description

### 👤 User Experience
- Secure authentication (sign up, sign in, password reset)
- Personalized user profiles
- Dark mode support
- Mini player for quick access during navigation
- Clean, Material 3 design

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with StateFlow
- **Dependency Injection**: Hilt
- **Backend**: Firebase (Authentication, Firestore, Cloud Storage)
- **Media Playback**: Media3 / ExoPlayer with MediaSessionService
- **Image Loading**: Coil

## Requirements

- Android 7.0 (API 24) or higher
- Internet connection for streaming
- Firebase account for authentication

## Getting Started

1. Clone the repository
2. Add your `google-services.json` file to the `app/` directory
3. Build and run with Android Studio

```bash
./gradlew assembleDebug
./gradlew :app:installDebug
```

## Screenshots

*Coming soon*

## License

This project is for educational purposes.
