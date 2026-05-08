# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Kartoshka is a local-first Android shopping list app. Users create lists, add items as cards, and tap a card to mark it done (removing it from the list).

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Database:** Room (local only, no cloud sync)
- **Theme:** Dark by default
- **Min SDK:** 26 (Android 8.0)

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.kartoshka.ExampleTest"

# Run instrumented tests on emulator
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

Always verify changes on the emulator before marking a task done.

## Screens

1. **My Lists** (home) — lists the user has created
2. **List Detail** — items in a list; tap card to remove
3. **New List** — create-list flow
4. **List Settings** — rename, change icon/color, delete
5. **Item Detail** — edit a single item
6. **Share** — placeholder, not yet implemented

## Design Rules

- Dark theme is the primary (and only) theme
- No traditional checkboxes — interactions are card-based
- Empty states use illustrations (e.g., hammer/drill on the first-item empty state)
- UI strings exist in English and Russian; use string resources (`strings.xml`) for all user-visible text — never hardcode strings

## Working With the Human

- The user is a **product designer with no coding background**. Use plain English. Avoid jargon; if unavoidable, define it briefly.
- Make **small, reviewable changes** — one increment at a time. Do not bundle unrelated changes.
- **Do not add features that were not asked for.** If an improvement seems useful, ask first.
- **When in doubt, ask** — do not assume.
- Prefer **simple over clever**.


## Git workflow rules

We use git for version control. Follow these rules strictly:

1. NEVER run `git commit` yourself unless I explicitly say "commit it" or 
   "commit this increment". I commit only after I've tested on the emulator 
   and approved.
2. NEVER run `git push` — pushing is my decision, manually.
3. NEVER run `git reset --hard` or `git restore` without my explicit approval — 
   these can destroy work.
4. At the start of each new increment, propose a plan first. After I approve, 
   code only that increment.
5. When you finish an increment, tell me:
   - What files changed (use `git status` to confirm)
   - What I should test on the emulator
   - A SUGGESTED commit message I can use after I approve
   Then STOP and wait for me to test.
6. If I say "looks good, commit it", THEN run:
      git add .
      git commit -m "Increment N: short description"
   Show me the output.
7. If I say "broken" or paste an error, do NOT commit. Diagnose, propose a 
   fix plan, wait for approval, fix, and we re-test.
8. Main branch is called master.
9. So far there is no remote repo, still all local.