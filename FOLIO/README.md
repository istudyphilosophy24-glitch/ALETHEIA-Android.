# FOLIO v1.0 — Philosophia Edition

Native Android 4.4 (API 19) Crema Grande reader with shelf, EPUB/TXT, full-screen covers, permanent underlines, 5-second quotation selection, integrated **verified** PHILOSOPHIA GRAND offline dictionary, and 55+ distinct open-license typeface assets retrieved and packaged at build time. No network permission on the device.

## Full dictionary facts

1,793,921 **definition/sense rows**, and 1,356,788 distinct **language+headword** combinations; do not mistake these for 1.5 million unique English words. The single APK embeds a compressed SQLite pack; the user needs approximately 425 MB of free storage **after installation** for first-use expansion. Dictionary init starts when opening Dictionary or selecting Look up from a reading quotation dialog, not on every launch.

## Build

Build on the folio-philosophia-v1 branch via `.github/workflows/folio-philosophia-v1.yml`. It downloads the verified SQLite artifact from earlier PHILOSOPHIA data workflow run 42422307149, verifies size/count/integrity, downloads and licenses at least 55 genuine fonts from Google Fonts, and creates a legacy-signed KitKat APK. If any minimum count or validation fails, the build fails; there is no fake fallback.

All Google Fonts assets are downloaded at build time; font family licenses are embedded. Current FOLIO v0.9 installation is retained under a distinct Android application ID. PDF rendering is still NOT included.
