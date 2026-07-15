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
1. **Redesigned Tabbed Authentication**: Overhauled the `AuthScreen` with a modern Material 3 `PrimaryTabRow`, supporting Email/Password and Phone verification in a clean, unified UI.
2. **Firebase Configuration Diagnostic**: Implemented a "Show Diagnosis" tool in the Auth UI. It checks for generated Android resources (App ID, API Key) to proactively inform users if `google-services.json` is missing or invalid, preventing the "infinite timeout" issue.
3. **Robust Navigation Lifecycle**: Refined `MainNavigation` with `LaunchedEffect` collectors that reactively redirect users based on `currentUser` and `continuedAsGuest` state changes, ensuring seamless login, logout, and guest transitions.
4. **VantaStorageManager**: Centralized local file management for novel content and chapters, providing a cleaner API for the `NovelRepository` and `ChapterDownloadWorker`.
5. **New Source Support**: Added `BoxNovelSource` and improved resilience for existing scrapers with better timeout handling.

## 🚀 Things To Do (Next Steps)
1. **Implement Batch Downloading Logic**: The UI for selecting "Download Next 5/10/All Chapters" exists in settings, but the background logic (e.g., a WorkManager task) to systematically download and cache those chapters without blocking the UI still needs to be implemented.
2. **Reader Chapter Transition**: Implement a "long swipe up" gesture at the bottom of the `ReaderScreen` to quickly jump to the next chapter.
3. **Expand Web Scrapers**: Monitor other sources (LightNovelPub, RoyalRoad) to see if their HTML structures change or if they introduce new Cloudflare challenges that require switching to `WebViewScraper`.

## 🧠 Guidelines for the AI
- **UI Guidelines**: Prioritize modern, premium UI design (e.g., Vanta purple accents, glassmorphism if applicable, clean spacing). Do not use generic, minimal aesthetics.
- **Scraping**: If a source fails to parse, assume Cloudflare or a JS framework (like Next.js) is interfering. Rely on `WebViewScraper.getHtml()` rather than `Jsoup.connect()` for these cases.
- **Database**: Room operations should be heavily cached and respect foreign key constraints. Use `OnConflictStrategy.REPLACE` where appropriate.
