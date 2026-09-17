# Fintecture Demo Bank App

A simulated bank that is a real installed app, so an integrator can test the app to app handoff and
the return to their own app in sandbox rather than discovering in production that neither works.

While the Demo Bank is only a web page, the operating system is never asked to open anything, so the
two things a mobile integrator most needs to test are the two things sandbox cannot reproduce.

## What it does

Connect hands the payer to `/demo-bank/auth`. If this app is installed, the OS opens it instead of a
browser; if not, the existing web consent screen is served and nothing changes. The app shows the
outcome pickers, and on confirm continues to `/oauth/callback` so the rest of the flow runs exactly
as it does today.

**It deliberately shows no payment details.** The point under test is whether the OS hands off and
whether the payer comes back. An amount on screen would prove nothing and would have delayed the
first device evidence, so the payment data stays server side where it already lives.

## Install

Download the APK from [Releases](https://github.com/Fintecture/demo-bank-app/releases), check its
SHA-256 against the one published with the release, and install it on an Android device. Android
will ask you to allow installing from this source.

It only ever talks to Fintecture's test and sandbox environments. The production route does not
exist, so the app cannot move money and cannot be pointed at a real payment.

## Controls

Released builds show **Confirm** only: it continues to the callback with the chosen outcome, as a
real bank app would.

Builds from source also expose two diagnostics we use internally, and which are deliberately absent
from the released app:

| | |
|---|---|
| **Confirm but do not return me** | completes the payment and strands the payer, reproducing a bank that takes the money and never comes back |
| **Cancel** | leaves without completing |

## Building

```sh
cd android
./gradlew :app:assembleDebug
```

Needs `local.properties` with `sdk.dir` pointing at the Android SDK, and a JDK (Android Studio's
bundled `jbr` works). The APK lands in `app/build/outputs/apk/debug/`.

## Before the verified link works

`autoVerify` makes Android fetch `assetlinks.json` from each declared host at install time, so those
hosts must serve it with **this app's signing fingerprint** before the handoff resolves. Until then
the link opens in the browser and the app looks broken for a reason that has nothing to do with the
integration.

Force the association while testing:

```sh
adb shell pm set-app-links --package com.fintecture.demobank 0 all
adb shell pm verify-app-links --re-verify com.fintecture.demobank
adb shell pm get-app-links com.fintecture.demobank
```

The custom scheme (`ftedemobank://auth`) needs no association and is the fallback diagnostic: it
survives a raw WebView that would swallow the https link. It carries no host, so a scheme link must
pass `origin` explicitly or the app has nowhere to send the payer back to.

## Hosts

Declared for `api.test.fintecture.com`, `api-sandbox.fintecture.com` and
`api-sandbox-test.fintecture.com`. Production is deliberately absent: the route is not mounted there.

## Branding

Fintecture design system tokens, from `.claude/fintecture-branding.skill`: navy `#0B1643` as the
structural surface, mint `#1DDBA9` as the single accent, white canvas, hairline borders and 12–16dp
radii.
