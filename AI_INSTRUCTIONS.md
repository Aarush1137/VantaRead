# VantaRead: AI Development Guidelines

This file is intended for any AI models (Gemini, Claude, GPT, etc.) assisting with the development of the VantaRead Android App. Please strictly follow these architectural rules, tech stack guidelines, and design principles.

## 1. Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Dagger Hilt (with KSP 2.3.20)
- **Local Database**: Room
- **Web Scraping**: Jsoup + Headless Android WebView (`WebViewScraper.kt`) to bypass Cloudflare.
- **Asynchronous Programming**: Kotlin Coroutines & Flows

## 2. Architecture (MVVM & Clean Architecture)
- **UI Layer (`/ui`)**: Jetpack Compose screens and ViewModels. ViewModels inject Repositories via Hilt.
- **Data Layer (`/data/repository`)**: Orchestrates data fetching between local (Room) and remote (Sources).
- **Source Layer (`/data/source`)**: Employs a generic plugin architecture. All scrapers (e.g., `WtrLabSource`, `NovelFullSource`) implement the `NovelSource` interface. 
  - **IMPORTANT**: Sources are loaded dynamically using Hilt Map Multibinding (`@IntoMap`, `@StringKey`). To add a new source, simply implement `NovelSource` and add the Hilt bindings.

## 3. Design Aesthetics & "VantaRead" Branding
- **Pixel-Perfect UI**: Prioritize immersive, highly customizable, and fluid interfaces. 
- **Gestural Navigation**: Use invisible touch zones (like in `ReaderScreen`) for smooth pagination without obstructing the text canvas.
- **Vanta Theme**: The app utilizes deep, immersive colors (`#8A2BE2` Vanta Purple accent) and a sleek dark mode. Avoid generic Material configurations; push for premium, Tachiyomi-style visuals.
- **Icons**: Utilize `androidx.compose.material.icons.extended`.

## 4. Web Scraping & Cloudflare Bypassing
- Do not use raw HTTP clients (like OkHttp or raw Jsoup connects) for sources protected by Cloudflare. 
- Use the `WebViewScraper.getHtml(context, url)` utility located in `/data/source/util/`. It spins up an invisible WebView, waits for the JS challenge to pass, and returns the parsed Jsoup `Document`.

## 5. Current Implementation Plan
To see what tasks are currently pending or what phase the app is in, please refer to the `task.md` and `implementation_plan.md` artifacts in the AI's artifact directory (or ask the user for the latest steps).

## 6. Firebase & Cloud Sync (Upcoming)
- We will be integrating Firebase Auth and Firestore for Cloud Syncing of bookmarks and reading progress across devices in Phase 4.

---
**Note to AI Model**: When starting a new session, review this file and the `task.md` checklist to continue exactly where the previous session left off. Do not break the Hilt DI structure or introduce deprecated libraries.
