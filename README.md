# Mughal Traders — Mobile App (Android, Kotlin)

This is the phone/tablet version of the shop system. It works exactly like a
client PC: the **MAIN PC always holds the data**, and the phone follows it
live over the same LAN / WiFi router.

## How it behaves

- **First open:** the phone asks for the MAIN PC address once
  (for example `192.168.1.10:8765` — the same address shown in the green bar
  on the main PC). It checks the main PC really answers before accepting it,
  then remembers it forever.
- **Every day after:** just open the app. It connects by itself.
- **Main PC switched off:** the phone shows **MAIN PC IS OFFLINE** and waits,
  checking every few seconds. The moment the main PC comes back, the shop
  opens by itself. The phone never invents or keeps its own copy of the shop.
- **Printing:** receipts print through the MAIN PC's printer, same as a
  client PC.
- **Real time:** a sale made on the main PC, on a client PC, or on any phone
  appears on all of them at once.

The app holds **no shop data of its own** — nothing to lose, nothing to sync,
nothing to back up on the phone.

## Getting the APK (no Android Studio, no Gradle, nothing to install)

You never build this on your PC. GitHub builds it for you:

1. Put this whole folder on GitHub (see the main README for the exact commands).
2. Open the repository on GitHub → **Actions** tab.
3. The **Build mobile APK** job runs by itself on every push.
4. When it finishes (a few minutes), open that run → **Artifacts** →
   download **MughalTraders-mobile-apk**. It is a `.zip` containing the APK.
5. Copy the APK to the phone (USB, WhatsApp, SD card…) and open it to
   install. The phone may ask to allow "install from unknown sources" — that
   is normal for an app installed this way.

### Optional: a permanent download link

Tag a version and GitHub also publishes it on the **Releases** page, where
the phone can download it directly:

```
git tag v1
git push origin v1
```

## Files

| File | What it is |
|---|---|
| `app/src/main/java/.../MainActivity.kt` | The whole app: setup screen, live shop screen, offline waiting screen |
| `app/src/main/res/layout/activity_main.xml` | The three screens' layout |
| `app/src/main/AndroidManifest.xml` | App name, internet permission, LAN (http) allowed |
| `app/build.gradle.kts` | Build settings — no third-party libraries at all |
| `../.github/workflows/build-mobile-apk.yml` | The cloud build that makes the APK |

## Version numbering

`versionCode` is taken from the GitHub build number automatically, so every
push produces an installable update over the previous one.
"# mughal-app" 
"# mughal-app" 
