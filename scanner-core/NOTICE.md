# OSS Document Scanner attribution

- Upstream: https://github.com/ossappscollective/OSS-DocumentScanner
- Pinned snapshot: `05f9611a35e7e2ff35de4f54e845c724d9168bc7`
- License: MIT (see `LICENSE`)

Local changes: extracted the Android/OpenCV contour detection, quadrilateral ordering,
perspective transform, white-paper enhancement and color simplification concepts into
a standalone Kotlin Android library. NativeScript UI, Tesseract OCR and QR support are
not included. The Compose application owns capture, crop handles, page ordering and PDF export.
