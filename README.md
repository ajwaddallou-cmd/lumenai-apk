# Nuskha Android preview 0.1
Private development build. No release approval yet.

Implemented:
- Android InputMethodService with app-wide text insertion, delete, shift and number keys.
- Optional Android on-device speech recognition. Refuses cloud fallback.
- Local dictionary, snippets, rule-based cleanup, text actions and bounded history.
- Standalone setup/settings app.

Not yet implemented or proven:
- LLM cleanup/style generation and agentic tools.
- Pixel 8 runtime, input-method lifecycle, microphone results or accessibility checks.
- Whisper model download or guaranteed offline-language availability.
- Signed production release, privacy review for public distribution.

Browser harness verifies UI logic, not native Android execution. No network permission is declared. Only RECORD_AUDIO is requested, in the setup app by a user action. No user data has been sent to a model.
