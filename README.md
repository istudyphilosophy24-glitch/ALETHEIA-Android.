# PHILOSOPHIA GRAND — the one-APK Crema Grande dictionary

An Android **4.4 KitKat (API 19)** native e-ink dictionary, designed for the
Crema Grande. **No account, network, database file, download inside the app,
file manager, SD-card setup, or dictionary picker.** One APK contains all the
Wiktionary data and the curated philosophical glossary.

## Download a ready-to-install APK

After the **PHILOSOPHIA GRAND — Bundled Million-Entry APK** build completes,
open [Releases](https://github.com/istudyphilosophy24-glitch/ALETHEIA-Android./releases)
and select **PHILOSOPHIA GRAND 2.0 — Android KitKat**.
Download `PHILOSOPHIA-Grand-Android-4.4-Million-Entries.apk` directly.

If GitHub Releases publishing is blocked by repository settings, open **Actions**,
select **PHILOSOPHIA GRAND — Bundled Million-Entry APK**, and download
`PHILOSOPHIA-GRAND-APK-Android-4.4` from the newest *successful* run.
You must unzip the Actions artifact before transferring the APK.

**This APK is a debug-signed sideload build.** You may need to allow
'Unknown sources' in Android settings. Since it uses a new Android package ID
(`org.philosophia.grande`), it does not conflict with the earlier
`org.philosophia.ereader` development build. For a future update, keep the
same signing key; this GitHub debug key is not guaranteed stable across runs.

## One-time first launch

The verified archive contains **1,793,921 distinct sense records and
1,356,788 distinct language+headword combinations**. The uncompressed SQLite
database is **393,191,424 bytes**, built by the previous successful GitHub
workflow run 42422307149 using Wiktionary/Wiktextract/Kaikki extraction data.
The database contains **69 curated editorial philosophical terms** as well
as the dictionary's other technical and general words. Definitions are
prioritized by lexical clues but are not all manually curated.

The APK carries the complete SQLite file compressed as
`assets/philosophia.lexpack`. At first launch, a simple visible status page
automatically unpacks it to the app's private storage and validates the data.
This uses a **64 KiB streaming buffer** and therefore does not load 393 MB
into RAM. Around **425 MB of additional internal storage** must be available
*after installing the APK*. The bundled asset is already in the APK; no
second download is required. Future launches open the private SQLite file
directly. When the database cannot be installed, the app shows an actionable
error and retry control; it does not silently fall back to a tiny glossary.

### E-ink / KitKat choices
- No Internet permission, no analytics, no AndroidX or Google services.
- minSdk 19, targetSdk 19, native Java and Android's SQLite engine.
- No file-picker or legacy external-storage permission needed.
- Grayscale, high contrast, still screens and infrequent setup updates.
- Index-based search with LIMIT 60 instead of loading all results.
- Definitions, philosophical filter, bookmarks, adjustable reading type.
- Compatible SQLite schema uses B-tree indexing and Android 4.4-era SQL.
- All on-device data operations are offline.

## Rebuild with GitHub Actions

The workflow file is
`.github/workflows/build-bundled-apk.yml`. It downloads the **verified**
large SQLite artifact produced in GitHub Actions run 42422307149, validates
the count, compresses the database into the APK assets, compiles with Gradle
8.0.2 and Android Gradle Plugin 8.1.4, checks APK contents and minSdkVersion,
then uploads a downloadable APK artifact and attempts to publish a stable
GitHub release.

This workflow currently depends on the earlier GitHub Actions database
artifact remaining available. Once that artifact expires, re-run the
`Build million-sense offline database` workflow on the
`philosophia-kitkat` branch and change the `run-id` in the bundled
workflow to the fresh successful run. Never silently substitute a dummy DB.

## Licensing and limitations

App source: MIT. The included Wiktionary-origin dictionary must retain
appropriate Wiktionary/Wiktextract and contributor attribution; refer to
`DATA_LICENSE.md` and the About screen.

**GitHub Actions build success is not a physical Crema Grande installation
test.** The APK is intended for Android 4.4, but actual sideload installation
and first-launch unpacking on the specific reader should be verified before
relying on it. The large compressed APK plus extracted DB require free
internal storage, especially if other books are present.
