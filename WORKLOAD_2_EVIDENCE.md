# Workload 2 — Custom booking form for Kelsey Renee Beauty

**Date:** 2026-07-18 / updated 2026-07-19
**Linear ticket:** AIF-48 (https://linear.app/aifrienddan-or-hcihy-tech/issue/AIF-48/build-custom-booking-form-for-kelsey-renee-beauty-skip-vagaro)
**⚠️ Linear connector was unauthenticated in this session — could not post this as a comment. Paste this file's content into AIF-48 manually, or ask me to retry once Linear is authorized.**

## Notification plan — corrected 2026-07-19

The first build (2026-07-18) wired `/api/notify` to Resend, emailing Dan on every booking request. **Dan corrected this:** the real requirement is an **SMS sent directly to Kelsey** notifying her of the new appointment — not an email to Dan. That version has been fully replaced; no mailto/email fallback ships in this build.

`/api/notify` now sends an SMS via Twilio directly to Kelsey using the **HCiHY Twilio subaccount** (created 2026-07-05) — the same subaccount already wired (but not yet live) for Leilani's Classy Cleaning. Mirrored the exact pattern from `leilanis-classy-cleaning/api/notify-sms.js`: raw `fetch` to the Twilio REST API (no `twilio` npm package needed, so `package.json`/`package-lock.json` were removed — nothing to install), same env var names (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`).

### Kelsey's phone number

Not found anywhere in the workspace (checked `hcihy-clients-vercelapps/`, `studiolash/`, Notion fallback trackers, onboarding form templates — all empty of a stored value). **Dan provided it directly: 661-436-6728** — the same number already public on the site's `tel:`/`sms:` links. Hardcoded in `api/notify.js` as `KELSEY_PHONE = '+16614366728'`.

## What was built

- **`booking.html`** — page at `/booking`. Fields: name, phone, email (optional), service (dropdown), preferred day, preferred time, notes. No calendar sync, no reminders, no payment.
- **`api/notify.js`** — Vercel serverless function. Validates required fields, sends one SMS to Kelsey (`+16614366728`) via Twilio with the booking details. No email, no mailto, no other recipient.
- **`vercel.json`** — builds `booking.html` and `api/notify.js` alongside the static site, with explicit routes for `/`, `/booking`, and `/api/notify`.
- **`index.html`** — Vagaro booking CTA replaced with a link to `/booking`; call/text fallback buttons kept.

## Deployed

- **Live URL:** https://studio-lash-sigma.vercel.app/booking (production, project `prj_P5Wif0I8zN6RryU4ZsztNJ1WNf1y`, team `hchy`)
- Pushed to `origin/main` on `github.com/AIFriendDan/studiolash`.

## Verification performed

1. **Local (`vercel dev`):**
   - `GET /` → 200, `GET /booking` → 200.
   - `POST /api/notify` missing fields → `400 {"ok":false,"error":"Missing required fields"}`.
   - `POST /api/notify` valid payload, no Twilio env vars set → `500 {"ok":false,"error":"SMS notification is not configured yet"}` — fails safely.
2. **Production:** same checks re-run against the live URL, same results.
3. Filled out the actual booking form in-browser (first build, Resend version) and confirmed the client correctly surfaces a fallback error with the call/text number when the send fails — same client-side error-handling code carries over unchanged for the SMS version.

## ⚠️ NOT YET fully verified — blocked on Dan (same blocker as Leilani's)

**`TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` have not been added to the `studio-lash` Vercel project yet.** Per `leilanis-classy-cleaning/README.md`, this is the same open blocker there too — the HCiHY Twilio subaccount's toll-free number is **still pending Twilio Trust Hub verification**. Even once the env vars are set here, actual SMS sends may fail (or need to wait) until that verification clears — that's a Twilio-side blocker, not something either codebase can route around.

Until then, the booking form accepts submissions but the SMS won't send — visitors get told to call/text instead, so nothing is silently lost.

## To finish this off, Dan needs to:

1. Add `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` to the `studio-lash` Vercel project (same values already meant for `leilanis-classy-cleaning` — same subaccount, so this is one set of credentials covering both projects). `vercel env add <NAME> production` from `studiolash/`, or via the Vercel dashboard.
2. Confirm the Twilio Trust Hub toll-free verification has cleared (check the Twilio console) — SMS sends will fail until it has.
3. Redeploy (`vercel deploy --prod` from `studiolash/`) after adding env vars — they don't apply retroactively.
4. Do one real test submission and confirm Kelsey actually receives the text.

## Definition of done — status

- [x] Booking form live at /booking
- [x] Wired to Twilio SMS → Kelsey directly (code complete, deployed, no email/mailto fallback shipped)
- [ ] **SMS pipeline confirmed working end-to-end** — blocked on Dan adding Twilio env vars + Trust Hub verification clearing
- [ ] Test submission verified — pending the above
