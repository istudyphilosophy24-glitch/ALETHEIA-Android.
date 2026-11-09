# Verification log

- `EpubBook.java` is pure Java; it can be compiled against a plain JDK for parser smoke tests.
- Android UI, manifest, signing and installation must be validated using GitHub Actions and a real Android 4.4 device.
- Battery improvements are hypotheses, not measured outcomes.
- EPUB test set should include Greek polytonic, German words, footnotes, image-rich EPUB, huge chapters and malformed ZIP packages.
- If GitHub Actions build fails, preserve logs, fix and rebuild rather than presenting uncompiled source as an installable APK.
