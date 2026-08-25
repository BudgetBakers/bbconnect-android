# BBConnect Link SDK (Kotlin)

Mobile Link SDK for Android (WP4.4, Maven `com.budgetbakers:bbconnect`).
Design: `DESIGN.md` §9.2.

Pure browser orchestration — **no network calls, no API key in the app**: your
backend creates the connect session with the server SDK
(`connectSessions.create`) and hands the opaque `hostedUrl` to the app.

```kotlin
BBConnect.start(activity, hostedUrl) { outcome ->
    when (outcome) {
        is BBConnectOutcome.Success -> ...   // poll the session server-side
        is BBConnectOutcome.Failure -> ...   // machine-readable reason
        is BBConnectOutcome.Cancelled -> ... // incl. closed Custom Tab
    }
}

// The return link reopens the app via your intent-filter — forward it:
override fun onNewIntent(intent: Intent) { intent.data?.let(BBConnect::handle) }
// Custom Tabs have no dismissal callback — detect it in onResume:
override fun onResume() { super.onResume(); BBConnect.handleResume() }
```

- Chrome Custom Tabs — **never an embedded WebView** (banks block them;
  RFC 8252).
- Return-link parsing (`BBConnectReturnUrl.parse`) is built on `java.net`
  (JVM-unit-testable, no Robolectric) and pinned by the shared vector set
  `contract-tests/fixtures/return-url.json` (same vectors as the Swift SDK).
- **Step 0** of the quickstart (docs/quickstart-android.md): host
  `assetlinks.json` for app links (`autoVerify`), or register a custom scheme
  as the fallback; the returnUrl must be registered for the app in the
  partner portal. Both intent-filters are shown in
  `sample/src/main/AndroidManifest.xml`.

Sample app: `sample/` (module `:sample`, `SampleActivity`).

## Building — CI handoff (no local Android toolchain)

This repo's machines may not have Gradle/Android SDK; `make -C
sdks/kotlin-link test` no-ops with a message locally (same guarded pattern as
`sdks/python`). The module is complete, reviewed source with pinned
AGP 8.5.2 / Kotlin 2.0.21; to build & test:

```sh
./gradlew :bbconnect:testReleaseUnitTest   # JVM tests (return-URL vectors)
./gradlew :bbconnect:assembleRelease
```

The Gradle 8.9 wrapper is committed (also required by the GitHub
distribution mirror, D32 — a mirror clone must build with no local Gradle).

**Pipelines-repo handoff (budgetbakers/be/pipelines):** add a
`sdk-kotlin-link` job on an Android image (e.g.
`cimg/android:2024.11` or the in-house equivalent) running the two gradlew
commands above; trigger on `sdks/kotlin-link/**` and `contract-tests/
fixtures/return-url.json` changes. Publishing to Maven Central is a separate
manual handoff (signing + Sonatype credentials).
