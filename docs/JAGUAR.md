# Jaguar TV

This is Jaguar Fitness Club's TV app: a fork of [Screenlite Web Kiosk](https://github.com/screenlite/android-web-kiosk) (MIT) that opens the gym CRM's TV page, `https://desk.jaguarfitnessclub.in/tv`, full screen on an Android TV and keeps it there.

The app is only a frame. Pairing, the ads loop and the birthday, anniversary and welcome cards all live in the CRM's player page, so changing them needs a CRM deploy, not a new app. The CRM side is documented in the `gym-crm` repository: `docs/TV-PLAN.md` (the design), `docs/TV-HANDOFF.md` (status and next steps) and `docs/runbooks/tv.md` (running, testing and installing on the gym's TCL Google TV).

## Branches

| Branch | On top of | What it adds |
|---|---|---|
| `main` | upstream v0.0.11 (`5e48558`) | Untouched upstream |
| `security-deps-update` | `main` | AGP 9.4.1, JDK 17, minSdk 23 (Android 6.0), targetSdk 36, updated libraries, hardened WebView and release build |
| `crm-ui` | `security-deps-update` | The CRM's look: colours, DM Sans, Jaguar badge screens, TV settings screen, launcher icon and TV banner |
| `jaguar-server` | `crm-ui` | App id `in.jaguarfitnessclub.tv`, the live URL built in, debug server choice, navigation locked to our page. **Build from this branch** |

Merge them into `main` in that order once the app has been checked on the gym's TV. The `upstream` remote stays so upstream fixes can still be merged; keep our changes small for that reason. The Kotlin package stays `org.screenlite.webkiosk` for the same reason.

## Which server it opens

| Build | Server |
|---|---|
| Release | Always `https://desk.jaguarfitnessclub.in/tv` (`BuildConfig.LIVE_URL`). Settings shows "Server: Live", read-only |
| Debug | Chosen in settings: **Live**, **This Mac** (`http://localhost:5180/tv`) or **Custom** (any http/https address) |

Either way, the WebView refuses to navigate away from the chosen origin. The rules are in `data/ServerConfig.kt` and `app/WebViewManager.kt`.

**This Mac** needs `adb reverse tcp:5180 tcp:5180` first. The CRM's dev API accepts browser requests only from the origin `http://localhost:5180`, and the reverse gives the emulator's or TV's page exactly that origin. `10.0.2.2` would not work.

Settings open with **five quick presses of OK** on the remote (`adb shell input keyevent 23`), over anything on screen.

## Take a break

Settings › **Take a break** (bottom left) lets the gym use the TV for something else: 30 minutes, 1 hour, 2 hours or Until opened. The TV goes to its home screen and the stay-on-top check leaves it alone until the time is up, then brings Jaguar TV back within one check. Opening Jaguar TV ends a break early, and a restart ends "Until opened". The break is kept in `data/TvBreak.kt`; `StayOnTopService` skips while it is on.

## Build

Needs JDK 17 (`brew install openjdk@17`) and the Android SDK (Android Studio puts it at `~/Library/Android/sdk`; `local.properties` points to it and is git-ignored).

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # onto the running emulator or a connected TV
adb shell am start -n in.jaguarfitnessclub.tv/org.screenlite.webkiosk.MainActivity
```

Emulator setup and the full test loop with the CRM running locally: `gym-crm/docs/runbooks/tv.md` §2.

## Release build (not done yet)

A release APK must be signed, and every later update must be signed with **the same key**. If the key is lost, the TV can only take an update by uninstalling the app and pairing the screen again. So:

1. Create the key once, outside any repository:

   ```sh
   mkdir -p ~/.gym-crm/tv-signing && cd ~/.gym-crm/tv-signing
   keytool -genkeypair -v -keystore jaguar-tv-release.jks -alias jaguar-tv \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

   Keep the passwords next to it in a file only you can read (`chmod 600`), and make a second copy offline. The staff app's key lives the same way in `~/.gym-crm/android-signing/`.

2. **Local signed build**: copy the key in as `app/release.keystore` (git-ignored), build, then remove it.

   ```sh
   cp ~/.gym-crm/tv-signing/jaguar-tv-release.jks app/release.keystore
   KEYSTORE_PASSWORD=... KEY_ALIAS=jaguar-tv KEY_PASSWORD=... ./gradlew assembleRelease
   rm app/release.keystore
   # app/build/outputs/apk/release/app-release.apk
   ```

   Without `app/release.keystore` the release APK is built unsigned and will not install.

3. **GitHub release build** (`.github/workflows/release-signed-apk.yml`): add the repository secrets `KEYSTORE_BASE64` (`base64 -i jaguar-tv-release.jks | pbcopy`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Then raise `versionCode`/`versionName` in `app/build.gradle.kts` and push a tag like `v1.0.0`; the job attaches `jaguar-tv-v1-0-0.apk` to the release. Every update needs a higher `versionCode`.

## Install on the TV

Full steps for the gym's TCL (Google TV): `gym-crm/docs/runbooks/tv.md` §3. In short:

1. Turn on Developer options and network debugging on the TV.
2. `adb connect <TV-IP>:5555`, then `adb install -r jaguar-tv-….apk`.
3. `adb shell appops set in.jaguarfitnessclub.tv SYSTEM_ALERT_WINDOW allow`, so it can reopen itself after boot.
4. Turn off ambient mode and the energy saver's "turn off display".
5. Open Jaguar TV and pair the code from the desk: **Manage › TV › Add screen**.

## What upstream already does (kept)

It starts on boot (`BootReceiver`), keeps the screen awake, and comes back to the front if someone presses Home (`StayOnTopService`). It runs full screen with the system bars hidden, retries with backoff when the network drops, and keeps DOM storage so the screen's token survives restarts. Muted video starts without a key press.
