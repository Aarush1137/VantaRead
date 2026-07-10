# VantaRead AI Context & Handoff Document

This document is designed to provide immediate context for any AI assistant (Cursor, Copilot, etc.) that is resuming work on the VantaRead project.

## 📱 Project Overview
VantaRead is an Android application for discovering and reading Light Novels from various web sources. 

### 🛠 Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Dependency Injection**: Dagger Hilt
- **Local Database**: Room SQLite
- **Asynchronous Programming**: Coroutines & StateFlow
- **Web Scraping**: Jsoup & a custom `WebViewScraper` (for bypassing Cloudflare)

## 🏗 Architecture & Key Components
- **Data Layer (`com.example.vantaread.data`)**:
  - `NovelRepository`: The single source of truth that coordinates between local DB caching (`NovelDao`, `ReadingHistoryDao`) and remote network scraping.
  - `NovelSource` interface: Implemented by `WtrLabSource`, `NovelFullSource`, `LightNovelPubSource`, etc. 
  - `WebViewScraper`: Crucial utility that uses an invisible Android `WebView` to execute JavaScript and bypass Cloudflare challenges (e.g., "Just a moment...") before extracting the HTML DOM.

- **UI Layer (`com.example.vantaread.ui`)**:
  - Contains screens for `Library`, `Discover` (Search), `History`, `Settings`, `NovelDetail`, and `Reader`.
  - The `ReaderScreen` has advanced features like auto-scroll, customizable fonts, themes (Vanta Black), text alignment, and margin controls.

## ✅ What Has Been Done Recently
1. **Wtr-Lab Cloudflare Bypass**: Overhauled `WtrLabSource` to use `WebViewScraper` instead of raw Jsoup. This solved an issue where WTR-Lab searches and details were returning empty because they were being intercepted by Cloudflare.
2. **Database Constraint Fix**: Fixed a fatal `FOREIGN KEY constraint failed` error that occurred when adding a novel via URL. The fix ensures that the `novelUrl` string used as the foreign key in `ChapterEntity` matches the parent `NovelEntity` exactly.
3. **Settings & Batch Download UI**: Built the `SettingsScreen` and `SettingsViewModel`, which save preferences (Theme, Default Source, Batch Download Amount) using `SharedPreferences` via `ReaderPreferencesManager`.
4. **GitHub Actions CI Fix**: Removed hardcoded absolute Windows paths from `gradle.properties` that were causing the Linux-based CI environment to fail.

## 🚀 Things To Do (Next Steps)
1. **Implement Batch Downloading Logic**: The UI for selecting "Download Next 5/10/All Chapters" exists in settings, but the background logic (e.g., a WorkManager task) to systematically download and cache those chapters without blocking the UI still needs to be implemented.
2. **Reader Chapter Transition**: Implement a "long swipe up" gesture at the bottom of the `ReaderScreen` to quickly jump to the next chapter.
3. **Expand Web Scrapers**: Monitor other sources (LightNovelPub, RoyalRoad) to see if their HTML structures change or if they introduce new Cloudflare challenges that require switching to `WebViewScraper`.

## 🧠 Guidelines for the AI
- **UI Guidelines**: Prioritize modern, premium UI design (e.g., Vanta purple accents, glassmorphism if applicable, clean spacing). Do not use generic, minimal aesthetics.
- **Scraping**: If a source fails to parse, assume Cloudflare or a JS framework (like Next.js) is interfering. Rely on `WebViewScraper.getHtml()` rather than `Jsoup.connect()` for these cases.
- **Database**: Room operations should be heavily cached and respect foreign key constraints. Use `OnConflictStrategy.REPLACE` where appropriate.
