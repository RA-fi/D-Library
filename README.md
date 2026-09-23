# D Library

D Library is an Android application for managing and accessing digital library resources such as books, newspapers, theses, notices, and other academic materials.

## Features

- User authentication and registration
- Resource browsing by category and subject
- Search and resource detail views
- PDF viewing and downloads
- Resource upload and approval workflows
- Admin and librarian management screens
- Firebase-backed data and file storage

## Tech Stack

- Java (Android)
- Gradle
- Firebase Authentication
- Firebase Firestore
- Firebase Storage

## Requirements

- Android Studio (latest stable recommended)
- JDK 11
- Android SDK (minSdk 29, targetSdk 36)
- A Firebase project configuration (`google-services.json`)

## Getting Started

1. Clone this repository.
2. Open the project in Android Studio.
3. Add your Firebase `google-services.json` file to:
   - `/home/runner/work/D-Library/D-Library/app/google-services.json`
4. Sync Gradle dependencies.
5. Build and run the app on an emulator or device.

## Build from Command Line

From `/home/runner/work/D-Library/D-Library`:

```bash
./gradlew assembleDebug
```

## License

This project is licensed under the MIT License. See `LICENSE` for details.
