# Smart Companion

Smart Companion is an Android (Jetpack Compose + Material 3) routine and interruption-management companion built with bilingual English and Arabic (RTL) support.

## Core Capabilities

- **Routines**: Automatic rules triggered by time range, days of the week, or entering/exiting a place, with visual editing (`When`, `Only if`, `Do this`, `Except`).
- **Emergency Contacts (Always Allowed)**: Contacts marked as Emergency always ring and message through unconditionally across every profile, routine, focus session, conflict policy, and global pause state.
- **Gemini Routine Assistant**: Natural-language routine creation in English and Arabic via Firebase AI Logic (`firebase-ai`), returning validated structured JSON proposals with Approve / Edit / Reject confirmation, conflict warnings, and automatic fallback to `LocalDemoAssistantEngine` when offline.

## Firebase AI Logic Setup (No API Key in Repo)

Smart Companion calls Gemini through **Firebase AI Logic (`Firebase.ai`)** authenticated with **Firebase App Check**. **Never** commit or embed a `GEMINI_API_KEY` in the app or repository.

1. **Firebase Project & App Registration**
   - Create or select a Firebase project in the Firebase Console.
   - Register the Android app with package name `com.aistudio.smartcompanion.qwxmzk` and add your debug/release SHA-1 and SHA-256 signing fingerprints.
   - Download `google-services.json` and place it at `app/google-services.json` (or provision automatically via AI Studio Firebase setup).

2. **Enable Firebase AI Logic & App Check**
   - In the Firebase Console, enable **Firebase AI Logic** (Gemini Developer API).
   - Enable **Firebase App Check** for the Android app:
     - **Debug builds** use `DebugAppCheckProviderFactory` (accepts `FIREBASE_APPCHECK_DEBUG_TOKEN` via launch Intent extra or logcat debug token registered in Firebase Console).
     - **Release builds** use `RecaptchaAppCheckProviderFactory` with `recaptcha_site_key` defined in `app/src/main/res/values/firebase_applet_config.xml` (or `strings.xml`).

3. **Build & Run Tests**
   - Compile debug APK: `gradle assembleDebug`
   - Run unit & Robolectric tests: `gradle :app:testDebugUnitTest`
