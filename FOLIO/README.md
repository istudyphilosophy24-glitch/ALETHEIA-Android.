# FOLIO 0.8 — Five-Second Quotation Hold Fix

This release fixes accidental text-selection mode when turning pages on the Crema Grande (Android 4.4).

- Short tap in right third → next page.
- Short tap in left third → previous page.
- Short tap center → menu.
- Hold a finger still on page for 5 seconds → quotation selection dialog.
- In the dialog select words with Android text selection, then tap Underline selection, or tap Nearest sentence for simple e-ink use.
- Finger movement cancels quotation hold; lifting early causes no underline.
- Quotation underlines and reading preferences are stored locally.

All previous bookshelf, fonts, typography settings, bookmarks and EPUB/TXT features are retained. This build uses a separate Android package to avoid update signature mismatch with GitHub's temporary debug signing keys. It does not delete or overwrite v0.7; books are reimported in the new app.
