# Nuskha Android preview

Private development build. Preview release 0.2 on the Releases page.

Implemented:
- Android InputMethodService with app-wide text insertion, delete, shift and number keys.
- Optional Android on-device speech recognition. Refuses cloud fallback.
- Local dictionary, snippets, rule-based cleanup, text actions and bounded history.
- Optional AI cleanup via a self-hosted Cloudflare Worker (opt-in, per-device pairing code; device key generated on-device and stored wrapped in Android Keystore; worker enforces bearer auth, atomic single-use pairing (Durable Object) and a soft per-day rate limit).
- Standalone setup/settings app.

Not yet implemented or proven:
- Real-device runtime verification (IME lifecycle, microphone results, Keystore behavior). Emulator CI covers build, install and launch only.
- SpeechRecognizer availability varies by device.
- Signed production release, privacy review for public distribution.

The app declares RECORD_AUDIO and INTERNET (used only for the opt-in Cloudflare cleanup). No secrets in this repo: device keys are generated per device at pairing time.
