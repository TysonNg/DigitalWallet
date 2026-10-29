# Personal receive QR

Backend creates a 360px PNG with ZXing and stores it through private MediaService. Each wallet has a stable random UUID receive token in `wallet_receive_codes` (V20). The token identifies a destination and never authorizes a transfer. Personal QR has no expiration; current wallet/account status is checked when resolving it.

## API

Both endpoints require the existing JWT authentication:
- POST `/api/v1/wallet/receive-qr`: gets or creates the caller's QR. Data: `mediaId`, `contentUrl`, `receiveUrl`. Deleted image media is regenerated while retaining the token.
- GET `/api/v1/wallet/receive-qr/{token}/recipient`: resolves to `walletId`, `fullName`, `currency`. Existing lookup rules reject self-transfer and inactive recipients.

FRONTEND_PUBLIC_URL defaults to http://localhost:3000; set it to the actual application origin before generating QR in another environment. The encoded URL opens `/vi/receive/<token>`. Only server configuration determines the origin.

## Frontend

The wallet card's Receive button opens the QR dialog. Images load through `/api/media/<id>/content`; this server-side proxy reads the authenticated session and never exposes JWT to the browser. Download uses the same private endpoint. Users can copy the receive link to share it.

The localized receive page preserves the token through login, resolves verified recipient information and offers an explicit Continue button into the existing transfer form. The sender still checks the amount and confirms PIN. Camera scanning and amount-specific receive requests are outside this iteration.

## Consistency

A short independent transaction reserves the per-wallet token before image I/O. Compare-and-set attachment chooses a concurrent image winner; definitively losing candidates are deleted. Unknown commit outcomes preserve image bytes to avoid deleting a referenced image. Generated media IDs are logged for operational recovery: a process crash between image storage and attachment can leave a private unreferenced READY file, which must not be swept without checking future business references.

Media must be enabled and configured. QR creation does not change balances or create transactions. Public receive links are shareable; image storage itself stays private.
