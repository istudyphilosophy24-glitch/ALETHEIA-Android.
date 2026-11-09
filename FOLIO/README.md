# FOLIO 0.7 — Quotation & Reading Styles Edition

Native offline KitKat (API 19) reader. Installs **alongside** Shelf v0.6 under the package name `org.folio.crema.annotation`, because GitHub-generated debug signing keys are not stable across runs. Your previous app and books are not overwritten. Import books again into the new edition.

## New
- Long-press text to select a quotation, choose **Underline** in the selection toolbar.
- Select **Remove underline** for selected text or delete from the quotation library.
- Quotation list with jump-to-passage, clipboard copy, and text export.
- Touch-friendly fallback: **Underline a sentence** from reading menu.
- 3 FOLIO-designed reading **styles**, composed from device system typefaces: **Clarity** (larger accessible sans), **Logos** (scholarly serif), **Novel** (comfortable compact serif). These are typographic profiles, not original font files or original drawn glyphs.
- Battery & focus guidance; safe shortcut to manage apps manually. Android cannot responsibly guarantee killing all other apps.

# FOLIO — Offline Crema Grande Reader

**Status:** Android 4.4 Shelf Edition / 0.6 source release. **Do not assume untested features are complete.**

FOLIO is a Java-only, offline, monochrome EPUB/TXT reader for older E Ink devices including the Crema Grande (Android 4.4, API 19). No account, bookstore, networking permission, analytics, subscriptions, or root is required.

## Implemented

- Single Android APK; no native code, no runtime dependencies, no network permission.
- Two-column monochrome cover bookshelf (EPUB 2/3 embedded covers) with list toggle, title/author metadata, search, last-read sorting, and delete imported copy.
- Local file picker for `.epub` / `.txt`; auto-copies selected files into app-private storage.
- EPUB2 and common reflowable EPUB3 spine parsing; basic navigation document / NCX labels.
- HTML-based chapter text display through Android's built-in, script-free `Html.fromHtml`, including common inline formatting and limited inline images.
- Page-by-page navigation, tappable zones, physical volume/DPAD keys, basic internal-reference navigation.
- Reading position saved into SQLite; approximate passage restoration when font size changes.
- Chapter chooser, bookmarks, selectable system typefaces and imported TTF/OTF fonts, font size, optional bold, first-line indentation, paragraph spacing, line spacing, margins, optional footer.
- Bounded per-page text layout; no animated transitions, services, wake locks or network calls.

## Known limitations (important)

- **PDF is not implemented**: Android 4.4 predates `PdfRenderer`. A separately licensed native PDF engine needs to be integrated and benchmarked on hardware.
- EPUB3 fixed-layout, scripting, interactive EPUB, synchronized media, some CSS, tables and vertical text are not supported.
- Inline images are downscaled and limited; heavy image books may be slow.
- The legacy Android `Html.fromHtml` parser cannot exactly match all publisher CSS; complex layout can differ.
- EPUB HTML note anchors jump to the chapter, not yet to the exact footnote ID.
- `Spanned` fragmentation at page boundaries can change paragraph formatting in unusual cases.
- Full-text search, exportable annotations, reading highlights, exact CFI, and vendor-specific E Ink refresh commands are **not in 0.6**.
- Real-device performance and battery improvement have **not been measured**.
- Support for DRM-protected EPUBs is not provided.

## Build on GitHub Actions

The companion `.github/workflows/folio-crema-build.yml` builds this project from `FOLIO/` using Gradle 7.6, Android Gradle Plugin 7.4.2, JDK 17, compile SDK 33, min SDK 19. The output `app-debug.apk` is signed with a debug key and enables v1 (JAR) signatures for Android 4.4. GitHub Actions artifacts contain a ZIP including the APK. Do not rename the ZIP to `.apk`; extract it first.

**Important:** Shelf Edition uses a different Android application ID (`org.folio.crema.shelf`) from FOLIO 0.5, and installs alongside it. Old books/bookmarks stay in the original app. Books must be imported into Shelf Edition separately. A debug key might differ between CI runs; for reliable future in-place updates, persistent private release signing is necessary. Never upload private keystores to public GitHub repositories.

### Manual build

Install JDK 17, Gradle 7.6, Android SDK 33, Build Tools 33.0.2, then from `FOLIO/`:

```sh
gradle --no-daemon assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

### Install on Crema Grande

1. Back up books and settings before changing reader apps. This app does **not** replace firmware or the Korean launcher.
2. Download the GitHub Actions artifact ZIP, unzip, and copy `app-debug.apk` to the reader with a USB cable.
3. Enable `Settings → Security → Unknown sources` as appropriate for the device.
4. Open APK with an installer/file manager; launch **FOLIO Crema Reader**.
5. Tap **+ Import** and navigate to your existing EPUB/TXT location. Files are copied into FOLIO private storage to ensure reliable access.

### Updating

Do not uninstall without backing up your reading data: uninstalling will delete FOLIO's private imported books and SQLite bookmarks. If an update fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the new debug APK uses a different signing key. Do **not** uninstall merely to force an update without preserving data.

## Reader controls

- Left third of text: previous page; right third: next page; middle: menu.
- Volume/DPAD right: next; Volume/DPAD left: previous.
- Reader menu: table of contents, bookmarks, typography, and return to library.
- Long-press book in library to remove the *imported copy*; source files are not modified.

## Security/privacy

No `INTERNET` permission; no remote resources or scripts. EPUB files are treated as untrusted, bounded by per-entry reading limits, entry-count and import size caps, and XML parser restrictions. The implementation is experimental and should be audited before use with adversarial EPUBs.

## Implementation roadmap

- **0.1** Validate installation and page rendering on real Crema Grande.
- **0.5** EPUB/TXT/library/bookmarks/typography (this source milestone).
- **0.6** Shelf Edition: actual EPUB covers, monochrome grid, font import, first-line indents and paragraph spacing.
- **1.0** Hardware tests, renderer fixes, true EPUB3/footnote handling, repeatable private signing.
- **2.0** PDF via vetted API19-compatible backend; text search, highlighting and exportable annotations.
- **3.0** Optional philosophy reading collections and paragraph-level cross-references.

## Copyright

Original FOLIO application source in this repository is supplied under the MIT license in `LICENSE`. Android tooling and Gradle plugins retain their respective licenses.

## Shelf Edition (0.6) usage

- Opens as a two-column bookshelf by default. Tap **List** for compact list mode, or **Shelf** to return to covers.
- When an EPUB includes a cover image in its manifest, FOLIO saves a tiny local thumbnail during import. Without an embedded cover it generates a monochrome text cover (no network fetch).
- Open a book and tap the middle third of the page. Select **Typography & layout** for font selection, font size, first-line indent, paragraph spacing, line spacing, margins, heavier text, footer. Apply saves settings.
- To add a custom font, tap **Import TTF / OTF** from the typography dialog, select a font file in the local file picker; this copies it into FOLIO Shelf private storage. FOLIO doesn't bundle or redistribute fonts.
- Body justification is unavailable through native Android 4.4 TextView. Complex publisher-defined paragraphs may still differ from the original.
- Do not uninstall the original 0.5 app to try this version; they are side-by-side intentionally to protect previous imported books and bookmarks.
