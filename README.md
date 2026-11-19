# Lookup Bubble 🔎

A native Java Android app for a **user-controlled floating search panel** on Tinder. It generates **public Facebook People Search** and **Google site:facebook.com** searches using a name and optional city. It does **not** identify a person by their image, prove identity, scrape websites, access private profiles, send messages, or communicate with a remote server.

## Features

- Draggable floating 🔎 bubble over Tinder, adjustable panel
- Manual name and city input with two public search options
- Optional Accessibility service restricted to Tinder (`com.tinder`) that suggests a visible **Name, age** and **Lives in City** text when you explicitly tap **Suggest**; no raw text is stored
- On-device processing only. Does not request internet, contacts, storage, location, camera, microphone, or phone permissions
- Transparent active-service notification and stop button

## Build with Android Studio

1. Install the latest [Android Studio](https://developer.android.com/studio). Open this folder as a project, not the `app` folder.
2. Let Android Studio sync Gradle and install **Android SDK Platform 35** and **Build Tools 35.0.0** if prompted.
3. Select `app` and **Build > Build APK(s)**. The APK will appear in `app/build/outputs/apk/debug/app-debug.apk`.
4. Install the debug APK on Android 8.0 (API 26) or newer. USB debugging or Android's app-install permission may be needed for sideloading.

This source package doesn't bundle a binary APK or Gradle wrapper JAR. If you have Gradle 8.11.1 and Android SDK installed, build using `gradle :app:assembleDebug`. On GitHub, the included GitHub Actions workflow builds the APK on the `dating-lookup-overlay` branch.

## Configure on the phone

1. Launch **Lookup Bubble**.
2. Tap **Allow overlay** and enable **Display over other apps**.
3. Optionally tap **Accessibility settings** and enable **Optional Tinder text capture**, after reviewing its description. This permission is powerful: enable it only if you want the optional name suggestion feature. Android controls this grant; this app cannot enable it for you.
4. Tap **Start floating bubble** and allow notifications if requested.
5. Switch to Tinder, open a visible profile, then tap the floating bubble.
6. Enter name/city manually or tap **Suggest from Tinder screen** (if Accessibility capture is enabled); review any suggested details.
7. Tap **Search Facebook People** or **Search public Facebook via Google**. Your browser opens the search. Facebook may require login; search results may be inaccurate or unavailable.
8. Tap **Stop floating bubble** when finished. Disable Accessibility in settings whenever you don't need it.

## Privacy and limitations

- **Not facial recognition.** It doesn't perform photo matching or private profile lookups.
- **No automated account actions.** It doesn't log into Tinder/Facebook, swipe, send messages, or bulk search.
- **No identity verification.** Similar names and cities don't establish that accounts belong to the same person.
- **Accessibility optional.** It reads current Tinder screen text to extract a potential name/city, not other apps. It doesn't save the screen or communicate with any server. Accessibility UI labels can change across Tinder versions; suggestions may fail, so manual entry remains supported.
- **No runtime testing in this package** unless a built APK is supplied separately. The project needs Android SDK / Gradle to compile.
- **Google Play publication:** AccessibilityService and the foreground `specialUse` category may require additional policy review and disclosures for Play Store distribution. Intended as a user-controlled personal utility, not a covert monitoring tool.

## Project structure

- `app/src/main/java/dev/animedespair/lookupbubble/MainActivity.java`: permission setup screen.
- `OverlayService.java`: floating button, search panel, browser intents.
- `TinderAccessibilityService.java`: optional on-device visible Tinder profile text suggestion.
- `SearchUrls.java`: public search URL construction.
- `.github/workflows/android-build.yml`: GitHub Actions debug APK build.

## Local URL test

Run this without Android SDK:

```bash
mkdir -p /tmp/lookup-url-test
javac -d /tmp/lookup-url-test app/src/main/java/dev/animedespair/lookupbubble/SearchUrls.java tests/TestSearchUrls.java
java -cp /tmp/lookup-url-test TestSearchUrls
```