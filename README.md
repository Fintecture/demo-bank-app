# Fintecture Demo Bank App

An installable simulated bank for testing the app-to-app handoff and the return to your own app,
against Fintecture sandbox and test.

Requires Android 8.0 or later. Supports PIS. It reaches only the sandbox and test environments; the
route it uses does not exist in production.

## Install

Download the APK from [Releases](https://github.com/Fintecture/demo-bank-app/releases), check its
SHA-256 against the value published with the release, and install it. Android asks you to allow
installing from this source.

## How it works

Select **Demo Bank** in the Connect bank list. This app handles that bank only; any other bank
behaves exactly as it does today.

Connect then sends the payer to `/demo-bank/auth`. With this app installed, Android opens it instead
of a browser. Without it, the web consent screen is served as before.

The app shows the payment outcome pickers. On confirm it continues to `/oauth/callback`, so the rest
of the journey runs unchanged and your `redirect_uri` is called as a real bank would call it.

## Controls

Released builds show **Confirm**, which continues to the callback with the selected outcome.

Builds from source additionally show:

| | |
|---|---|
| **Confirm but do not return me** | completes the payment without returning the payer, reproducing a bank that does not come back |
| **Cancel** | leaves without completing |

## Hosts

The app claims `/demo-bank/auth` on:

```
api.test.fintecture.com
api.sandbox.fintecture.com
api-sandbox.fintecture.com   (legacy name for the same sandbox host)
```

Production is not claimed.

## App link verification

Android fetches `assetlinks.json` from each claimed host **at install time** and does not retry on
its own. The host must already publish this app's signing fingerprint, otherwise the link opens in
the browser. If the association file is published after you install, reinstall the app.

To inspect or force verification on a test device:

```sh
adb shell pm get-app-links com.fintecture.demobank
adb shell pm verify-app-links --re-verify com.fintecture.demobank
```

`verified` against each host means the handoff will work. A numeric state such as `1024` means
verification failed.

## Custom scheme

`ftedemobank://auth` opens the app without any domain association, which is useful when an https
link is swallowed by a WebView. It carries no host, so such a link must pass `origin` explicitly.

## Building from source

```sh
cd android
./gradlew :app:assembleDebug
```

Needs `local.properties` with `sdk.dir` pointing at the Android SDK, and JDK 17 or 21. The APK is
written to `android/app/build/outputs/apk/debug/`.

For a signed release build, supply the signing material through the environment:

```sh
export FTE_DEMOBANK_KEYSTORE=/path/to/keystore.jks
export FTE_DEMOBANK_KEYSTORE_PASSWORD=...
export FTE_DEMOBANK_KEY_ALIAS=demobank
./gradlew :app:assembleRelease
```

Without those variables the release build succeeds unsigned.

## Documentation

https://doc.fintecture.com/docs/connect-mobile-app-integration
