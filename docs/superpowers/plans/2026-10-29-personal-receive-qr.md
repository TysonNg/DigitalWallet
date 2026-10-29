# Personal receive QR implementation

Goal: after real media verification succeeds, create backend-generated personal receive QR images stored through MediaService and display them in the dashboard. No browser QR generation, camera scanner or amount-specific requests in this iteration.

- Stable public random canonical UUID token per wallet (UUID.randomUUID().toString(), strict 36-character validation shared with the frontend), persisted in V20 wallet_receive_codes; token conveys destination only, never authorization.
- POST /wallet/receive-qr returns the owner's current PNG metadata and share URL; regenerates when its media was deleted. Wallet must be ACTIVE. Storage keeps private permissions.
- GET /wallet/receive-qr/{token}/recipient resolves the current recipient through the existing authenticated lookup use case (including inactive/self checks); response exposes walletId/fullName/currency only.
- QR data is an application URL using FRONTEND_PUBLIC_URL (default http://localhost:3000) and /vi/receive/<token>. Backend ZXing renderer produces a 360px PNG with quiet zone. Do not trust client-supplied origins or recipients.
- Per-wallet token reservation is committed before image I/O. Atomic image attachment picks a concurrent winner and deletes losing generated media. If database outcome is unknown, preserve bytes rather than delete a possibly referenced image.
- Next.js server action creates/gets QR. Authenticated binary proxy uses getFreshAccessToken and a fixed backend media path; never exposes JWT or accepts upstream URLs.
- Dashboard has a Receive button and accessible dialog with image, retry, download and copy-link controls. A localized public receive page preserves the token through login, resolves recipient after authentication, and links to the existing transfer form; no auto-submit or PIN changes.
- Tests: stable token/image reuse, deleted-image regeneration, concurrent attachment cleanup, inactive/self/unknown rejection, image decode matching exact URL, private PNG proxy auth/path/error behavior and locale messages.
- QR files without an attached code after an abrupt process crash may remain as private READY media; log generated IDs for operational recovery. No destructive reference-based sweep is introduced without avatar/attachment ownership semantics.