**[မြန်မာ](README.my.md)** | English

# Koe-Na-Win

ကိုးနဝင်း 81-day prayer beads counting daily tracker — log your daily beads count and track your progress toward completing 81 days.

## Features

- **Daily log** — record the beads count you completed each day
- **81-day tracking** — see at a glance how many days are done and how many remain
- **On-device storage** — records stay on your phone, works without internet
- **Burmese-first UI**

## Project structure

```
Koe-Na-Win/
├── android/        # Native Android project
├── app/            # App source
├── assets/         # Images, fonts
├── ios/            # iOS export scaffold
├── web/            # Web export scaffold
├── gradle/         # Gradle wrapper config
├── .env.example    # Required environment variables (copy to .env)
└── metadata.json   # Project metadata
```

## Getting started

1. Copy `.env.example` to `.env` and fill in the required keys.
2. Open the `android/` project in **Android Studio**.
3. Sync Gradle and run on a device or emulator.

## Tech stack

- Kotlin, Android
- On-device local storage
- Google Gemini API (server-side) for smart features

## License

All rights reserved.
