# PHILOSOPHIA · KitKat offline dictionary

**Crema Grande / Android 4.4 (API 19)** native dictionary, e-ink-first, no online runtime permission.

This is a **separate GitHub branch** in the ALETHEIA-Android. repository; the main branch is untouched.

## Download the APK
Open this repository's **Actions** tab. Select **PHILOSOPHIA KitKat APK** and open its newest successful run for the \`philosophia-kitkat\` branch. Download **PHILOSOPHIA-Android-4.4-APK**; extract the APK from the downloaded artifact ZIP, copy the APK to your Crema Grande, and install it. Device must permit installation from unknown sources. This is a debug-signed sideload APK, not a Play Store package.

## Download the dictionary pack
The **Build million-sense offline database** workflow downloads open Wiktionary extraction data and generates **PHILOSOPHIA-Million-Sense-SQLite** as a downloadable GitHub Actions artifact. **Check the workflow result first**: only a successful run actually produces the million-sense database. An artifact is available only for its retention period. The generated \`philosophia.sqlite\` is an offline SQLite 3 file, not an APK.

The app ships with a small original technical-philosophy starter pack; it does **not** ship with a million entries. Counts are computed from actual data rows, and one row means one *sense*, not necessarily one unique headword. The full build enforces ≥1,000,000 real sense records. Kaikki's upstream postprocessed data link is deprecated and may eventually disappear; the pipeline will fail instead of inventing entries if that happens.

## Install on Crema Grande (Android 4.4)
1. Install the APK once.
2. Download the dictionary artifact on a PC or phone, then transfer it by USB or SD card.
3. Inside PHILOSOPHIA tap **INSTALL DICTIONARY** and choose a raw \`.sqlite\` file OR its ZIP archive. The installer verifies its schema and integrity before replacing the old pack. It opens KitKat's \`ACTION_GET_CONTENT\` picker: a file manager with picker support must be present.
4. **Space-saving alternative for huge packs:** extract the Actions artifact on a PC and copy just \`philosophia.sqlite\` to \`/sdcard/PHILOSOPHIA/philosophia.sqlite\`. Launch PHILOSOPHIA and select **RELOAD**. A known external-card path is also checked. This method avoids doubling storage during an import.

The database is **not** something to install by tapping it as an APK. The PHILOSOPHIA app imports or opens it.

## Compatibility and honesty
- Android app uses native Java, minSdkVersion 19, SQLite B-tree indexes, and no network, FTS5, or AndroidX.
- Pack uses only SQLite 3.7-era features supported by Android KitKat. Encoding UTF-8, journal DELETE, 4096 byte pages.
- Importer accepts \`.sqlite\` or ZIP (including zipped GitHub Actions artifacts).
- Device RAM is conserved by limiting SQLite results to 60 per query.
- Verified: database generator on sample input, parser/SQLite schema. **A physical Crema Grande install remains unverified until tested on-device.** GitHub Actions must succeed for APK validity to be established.
- This is not affiliated with Crema's manufacturer.

## Building
The Actions APK workflow runs Python 3 to create a curated demo asset and builds Gradle \`:app:assembleDebug\` using JDK 17 and Android SDK 34. To build locally: generate assets with \`python3 tools/build_pack.py --demo --output app/src/main/assets/demo.sqlite\`, then build with Gradle 8.0.2 + Android Gradle Plugin 8.1.4.

## Licenses
App source: MIT. Wiktionary-origin database material: CC BY-SA and/or GFDL according to source licenses. Preserve attribution in redistribution; see \`DATA_LICENSE.md\`.
