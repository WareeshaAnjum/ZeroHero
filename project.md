# ⚡ ZERO HOUR
### High-yield intelligence generated for critical deadlines.

> No study plans. No fluff. Just the material you need, in the time you have left.

Zero Hour is a deadline-driven Android app that skips the planning phase entirely and instantly generates high-signal preparation material using Gemini 3.5 Flash. Built for the last hour before an exam, interview, or presentation — when time is the only resource that matters.

---

## Screenshots

<!-- Add your screenshots here --

## Features

- **Three preparation modes** — Exam, Interview, and Presentation, each with a tailored Gemini prompt and output structure
- **Deadline Matrix** — Set your remaining time (via a rotating dial) and depth level; the AI calibrates content density accordingly
- **Kinetic loading states** — Staged, context-aware progress copy instead of generic spinners
- **Dossier output** — Structured, scannable markdown output with quick-copy and navigation
- **Terminal-dark UI** — High-contrast obsidian theme with CyberMint accents, monospaced clock, and zero visual noise

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM — ViewModel + StateFlow |
| AI | Gemini 3.5 Flash via REST (OkHttp) |
| Async | Kotlin Coroutines |
| Image loading | Coil |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 16 (API 36) |

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- A [Gemini API key](https://aistudio.google.com/app/apikey)

### Setup

1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/zero-hour.git
```

2. Copy the environment example file
```bash
cp .env.example local.properties
```

3. Add your Gemini API key to `local.properties`
```
GEMINI_API_KEY=your_key_here
```

4. Open in Android Studio and run on a device or emulator (API 24+)

---

## How It Works

1. Select your mode — Exam, Interview, or Presentation
2. Set your deadline window and depth using the Deadline Matrix
3. Enter your subject and optional target company/role
4. Hit Generate — Gemini 3.5 Flash structures a full dossier in seconds
5. Study, copy, or iterate directly from the output screen

---

## Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt
└── ui/
    ├── CramTimeApp.kt       # All composables and screen logic
    ├── CramTimeViewModel.kt # State management and Gemini API calls
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## Roadmap

- [ ] Voice Mock Interview — speak your answer, get real-time Gemini feedback
- [ ] Iterative chat on dossier screen
- [ ] Error state UI with retry
- [ ] Export dossier as PDF
- [ ] Light mode (Studio theme)

---

## Built With

- [Gemini API](https://ai.google.dev/) — Google's Gemini 3.5 Flash model
- [Jetpack Compose](https://developer.android.com/compose)
- [OkHttp](https://square.github.io/okhttp/)

---

## License

MIT License — see [LICENSE](LICENSE) for details.
