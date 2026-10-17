# Full Charge Alert

[简体中文](README.md) | [English](README_EN.md)

A lightweight native Android app that wakes the screen and displays a high-priority notification when a charging phone first reaches 100% battery.

## Features

- Continuously monitors the system battery state with a foreground service
- Wakes the screen, vibrates, and displays a lock-screen notification when charging is complete
- Alerts only once per charging session and resets after the charger is disconnected
- Automatically resumes monitoring after the device starts
- Optional Root enhancement: adds the app to the Doze allowlist and uses a Root command as an additional way to wake the screen
- Includes a built-in test alert button

## Build

Open the project in Android Studio, wait for Gradle synchronization to finish, and then run the app. The project requires JDK 17 and Android SDK 36 or later.

Allow notification access when opening the app for the first time. Some Android variants may also require you to enable auto-start and permit notifications on the lock screen in system settings.
