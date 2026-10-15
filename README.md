# ALETHEIA Native — *Being and Time* laboratory

An offline-first, **native Android Java** prototype for close study of Martin Heidegger's *Being and Time*.

## Install the debug APK

1. Open [GitHub Actions](https://github.com/istudyphilosophy24-glitch/ALETHEIA-Android./actions).
2. Choose **Build ALETHEIA Native APK**.
3. Open the latest **successful** run.
4. Under Artifacts, download **ALETHEIA-debug-APK** (ZIP) and extract `app-debug.apk`.
5. Install the APK using Android's standard sideload prompt. Debug builds are for testing.

## Included at this development stage

- Great Reader: two separately labeled Macquarrie–Robinson and Stambaugh translation slots; on-device TXT/DRM-free EPUB import; paged reading, local paragraph annotations and approximate same-index comparison.
- Scholarly architecture: 83 § navigation anchors; authored commentary and objections for selected sections; German-English glossary; research notes.
- Visual ontology atlas; three interactive philosophical illustrations; three argument-reconstruction challenges; offline prompts.
- Android TTS output selection, speed controls, speaking passages, system speech settings and Android share chooser.

## Boundaries and transparency

**No complete copyrighted translation is distributed with this source or APK.** Users must import lawfully obtained, DRM-free editions. Do not bypass ebook DRM. Imported EPUB chapters currently use filename order and may not follow EPUB spine order. Same-number paragraph comparison is provisional, not verified equivalence. Paragraph-by-paragraph commentary for every paragraph is not yet included. The Hermeneutic Tutor is a scripted offline study companion, not an integrated conversational AI.

The previous `AletheiaNative-source.zip` was a separate prototype. This GitHub repository now contains newly authored Android sources directly; builds do not require the earlier archive.

## Developers

JDK 17, Android SDK 35, Gradle 8.9, Android Gradle Plugin 8.7.3.

Run `gradle :app:assembleDebug` at repository root. APK: `app/build/outputs/apk/debug/app-debug.apk`.

The app is an early prototype and its TTS compatibility, especially with ElevenReader, must be tested on your device.
