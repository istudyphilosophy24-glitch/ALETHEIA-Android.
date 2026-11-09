# FOLIO v0.9 — Reader-Fit Edition

Android 4.4 compatible, offline E Ink EPUB/TXT reading application.

### Changes from v0.8
- EPUB **full-page embedded covers** (including SVG-wrapped `<image>`) now display in a dedicated white image pane instead of a small inline text line.
- Cover display defaults to **Fit entire image** with no clipping, and optional **Fill / crop** in the reading menu.
- Inline EPUB illustrations scale to the available viewport rather than a fixed 450dp height.
- Paragraphs and image layout reflow after typography or viewport changes; reading position is preserved by text anchor.
- HTML5 semantic block tags are converted to Android 4.4-friendly tags.
- Five-second quotation hold and normal-tap navigation remain intact.

### Important limitations
- PDF is **not** currently supported by this APK. PDFs are fixed-page documents; fit-to-page requires a separate native PDF engine, and true text reflow requires reliable source text/tags or conversion. This version does not pretend to support PDFs.
- Some EPUB CSS, tables, SVG vector art and fixed-layout books exceed Android 4.4's native text renderer capabilities. We prioritize readable reflowable EPUB 2 and ordinary EPUB 3.
- v0.9 is a separate package because GitHub's debug signing identity is not stable across runs. Your older app and data remain untouched. Reimport books into v0.9.
- On-device visual validation and battery measurements are still required.
