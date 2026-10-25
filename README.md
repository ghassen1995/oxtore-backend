# Oxtore backend (monolith)

Spring Boot 4 / Java 21 / PostgreSQL. REST API on port 8081, Socket.IO notifications on 8082.

## Running locally

```bash
cp .env.example .env        # then fill in at least JWT_SECRET and OTP_HMAC_SECRET
docker compose up --build   # the Dockerfile builds the jar itself
```

Production deployment (VPS, Nginx, HTTPS): see [DEPLOY.md](DEPLOY.md).

`docker compose` reads `.env` automatically. `.env` is git-ignored; never commit secrets.

## Auth & OTP configuration

Sign-up creates an **unverified** account and returns tokens. The app then calls
`POST /api/auth/otp/send {email, method: "email"|"whatsapp"}` and
`POST /api/auth/verify-otp {email, code}`. Unverified users can still log in; the response has `isVerified: false`.

### Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | yes | JWT signing key (HS512, ≥ 64 bytes). `openssl rand -hex 64` |
| `JWT_REFRESH_SECRET` | yes | Refresh-token signing key (≥ 64 bytes, different from `JWT_SECRET`). The app refuses to start without it |
| `JWT_EXPIRATION_MS` (86400000), `JWT_REFRESH_EXPIRATION_MS` (604800000) | no | Access token 24 h, refresh token 7 days |
| `GOOGLE_CLIENT_ID` | for Google sign-in | OAuth 2.0 *Web application* client ID. Empty = `POST /api/auth/google` answers `503 GOOGLE_UNAVAILABLE` |
| `OTP_HMAC_SECRET` | yes | Key used to hash OTP codes (≥ 32 chars). The app refuses to start without it. `openssl rand -hex 32` |
| `MAIL_HOST`, `MAIL_PORT` (587), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_STARTTLS` (true), `MAIL_SMTP_AUTH` (true) | for email | SMTP server. Empty `MAIL_HOST` = email channel disabled. `MAIL_SMTP_AUTH=false` only for a local test server such as Mailpit |
| `MAIL_FROM_EMAIL`, `MAIL_FROM_NAME` (Oxtore) | for email | Sender address shown to users |
| `WHATSAPP_PROVIDER` | no | `none` (default) or `twilio` |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE` | for WhatsApp | Twilio credentials and WhatsApp sender (E.164) |
| `TWILIO_WHATSAPP_CONTENT_SID` | for WhatsApp in production | Approved authentication template, code as `{{1}}`. Without it a free-form message is sent, which Twilio only delivers in the sandbox or within a 24 h session |
| `SPRING_PROFILES_ACTIVE=dev` + `OTP_DEV_LOG_CODES=true` | local only | Writes codes to the log for channels without a provider. Never enable in production |
| `APP_UPLOAD_DIR`, `APP_PUBLIC_BASE_URL` | existing | Image uploads |

A channel without a configured provider answers `503 OTP_CHANNEL_UNAVAILABLE`. A provider error answers
`503 OTP_DELIVERY_FAILED`; in both cases nothing is recorded as sent. The startup log lists which channels are
configured (`OTP delivery EMAIL: configured`).

> Hosting note: some hosts (Railway on some plans) block outbound SMTP. If email delivery fails there, add an
> HTTPS-based sender (e.g. Resend) as another `OtpSender` implementation.

### Sessions, password reset, Google

| Endpoint | Auth | Behavior |
|---|---|---|
| `POST /api/auth/refresh {refreshToken}` | public | New `{accessToken, refreshToken}`. Refresh tokens are JWTs signed with `JWT_REFRESH_SECRET` (never accepted as access tokens). Rejected (`401 INVALID_REFRESH_TOKEN`) when expired, malformed, or issued before the user's last password reset |
| `POST /api/auth/logout` | token | `{message}`. Tokens are stateless: the app discards them, nothing is revoked server-side |
| `POST /api/auth/forgot-password {email}` | public | Emails a 6-digit reset code (same OTP rules and limits as sign-up). Same `200 {message, expiresIn, resendIn}` for unknown emails |
| `POST /api/auth/reset-password {email, code, newPassword}` | public | Sets the password (8–72 chars), marks the email verified, invalidates older refresh tokens |
| `POST /api/auth/google {idToken}` | public | Verifies the Google ID token (signature, audience = `GOOGLE_CLIENT_ID`, issuer, expiry) and requires a Google-verified email. Signs in the linked account, else links the account with that email (an *unverified* account loses its password when linked, against pre-registration hijacking), else creates a verified account without phone or usable password |

Requests without a valid access token get `401 {"code":"UNAUTHORIZED"}` (not 403), which triggers the app's refresh.

**Google setup:** Google Cloud Console → APIs & Services → Credentials → OAuth client ID → *Web application*. Add the
app's origins (e.g. `http://localhost:4200`, `http://localhost:8100`, the production web origin) as *Authorized JavaScript
origins*. Put the client ID in `GOOGLE_CLIENT_ID` here and in `googleClientId` in the app's `environment*.ts`.
Native Capacitor builds can't use this web flow (Google blocks embedded webviews) and would need a native plugin.

### OTP rules (`otp.*` in `application.properties`)

- 6 digits from `SecureRandom`; stored only as HMAC-SHA256 (`otp_codes.code_hash`), never in plain text or in logs.
- Valid 10 minutes, single use; a new send invalidates the previous code.
- 5 wrong attempts invalidate the code.
- 60 s between sends; at most 5 sends per email and 20 send requests per IP per hour (`429` with `Retry-After`).
- `otp/send` answers the same for unknown or already-verified emails, and `verify-otp` answers `OTP_INVALID` for them.

Error responses use `{code, message, status}`. Codes: `VALIDATION_ERROR`, `INVALID_INPUT`, `USER_ALREADY_EXIST`,
`OTP_INVALID`, `OTP_EXPIRED`, `OTP_TOO_MANY_ATTEMPTS`, `OTP_COOLDOWN`, `OTP_RATE_LIMITED`, `OTP_DELIVERY_FAILED`,
`OTP_CHANNEL_UNAVAILABLE`, `OTP_INVALID_PHONE`.

### Schema

`ddl-auto=update` adds `users.email_verified` (existing rows backfilled to `true`, so existing accounts stay
usable), `users.avatar` and the `otp_codes` table. The equivalent SQL is in `db/manual/2026-10-09_auth_otp.sql`
for environments that run without `ddl-auto`.
