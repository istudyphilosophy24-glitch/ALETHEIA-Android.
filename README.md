# ALETHEIA Native v0.5 — *Being and Time* philosophical laboratory

**Native Android Java app** built with Gradle and GitHub Actions. Offline-first, with no server-side collection of imported primary texts.

## Installation
1. Open [GitHub Actions](https://github.com/istudyphilosophy24-glitch/ALETHEIA-Android./actions).
2. Select the latest successful **Build ALETHEIA Native APK** run.
3. Download the `ALETHEIA-debug-APK` artifact; extract `app-debug.apk` from the ZIP.
4. Sideload the APK on Android 8 or later. This is a *debug* test build, not a production Play Store package.

**Debug signing warning:** GitHub Actions builds may have different transient debug signatures. If Android refuses to update an earlier ALETHEIA APK, it may be necessary to uninstall the previous test build, **which erases that build's private local data**. Use the Scholar's Workspace > **Export my notebook** first if available. A production release needs persistent private signing credentials.

## Great Reader
- Opens immediately to an offline original study guide for all 83 published sections, with distinct orientations and questions.
- Imports legally acquired **searchable PDF**, **DRM-free EPUB**, or **UTF-8 TXT** from device storage or Google Drive's Android document provider.
- Separate **Macquarrie–Robinson** and **Stambaugh** translation slots; independent paragraph paging, adjustable type, text selection/copy, bookmarking, offline notes and on-device search.
- Comparative Translation Laboratory shows the two imported texts and permits independent paragraph-index adjustment; the text alignment is *not* verified.
- Selected close-reading analyses and objections plus section-level study orientation for every section. Not every paragraph of the full copyrighted texts has been independently annotated.
- Scanned PDF without searchable text will require separate text recognition; DRM-locked ebooks are not imported.

**The complete English translations are not bundled.** Your personal lawful possession of a licensed edition does not by itself provide permission to redistribute the text through this public repository. Import your own copy through the Reader and it will be stored locally in the app.

## Audio
- Speech through Android's **TextToSpeech** engine, user-selectable installed engine, speed control and audio test, with a queued play request during engine initialization.
- Separate ElevenReader **share/handoff** workflow and **Copy for ElevenReader > Write text** fallback.
- A system settings selection does not guarantee ElevenReader can service Android TTS API calls. Its own import and narration capabilities are distinct from Android's TTS service. The app does not bypass subscriptions or usage limits.

## Learning environments
- 83 original thematic section navigation entries and unique section orientations.
- 46 concept glossary entries.
- Native Canvas ontological atlas, graphical equipment, anxiety and temporality demonstrations.
- 12 varied argument reconstruction encounters, randomized display positions, native vector graphics and saved encounter progress.
- Hermeneutic Tutor with offline structured questions, not a generative live AI.
- Scholar's Workspace with local notes, section completion, annotation and **JSON export/restore** for notebook and progress.

## Building
Requires Java 17, Gradle 8.9 and Android SDK 35. The source is compiled by `.github/workflows/build-apk.yml` on every main-branch push. Build locally:

```bash
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Remaining limitations
This is a substantial native beta, not a fully peer-reviewed edition. Full philological H-page alignment, paragraph-specific research commentary for every paragraph, conversational AI, more labs, comprehensive Android device testing, and durable release signing still need work. Imported text never leaves the application automatically; Android TTS engines and external ElevenReader sharing may transmit selected passages under those providers' terms.
