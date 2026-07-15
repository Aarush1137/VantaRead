# 📖 VantaRead

VantaRead is a modern Android light novel and web novel reader built with **Kotlin**, **Jetpack Compose**, and **Material 3**. It is designed with a plugin-based architecture, allowing support for multiple novel sources while providing a clean, customizable reading experience. 

It features an intelligent **headless WebView scraper** designed to bypass Cloudflare Bot Protection seamlessly, allowing you to read from top sites without missing a beat!

> **Project Status:** 🚀 Actively Maintained & Fully Functional!

---

## ✨ Features

### 📚 Personal Library, History & Stats
* **Local Library:** Bookmark favorite novels for easy access and tracking.
* **Continue Reading:** Seamlessly resume your recent novels right from the Library or History tabs.
* **Reading Statistics:** Track your total chapters read, novels started, and your most read novel on the new Stats screen.
* **Offline Reading:** Download individual chapters or entire novels for offline reading using a robust WorkManager implementation.
* **Grouped Downloads Screen:** Beautifully organized accordion UI grouping all your downloaded chapters by novel.
* **Resilient Downloads:** Downloads require connectivity, avoid duplicate WorkManager jobs, retry failed fetches, and surface per-novel progress through notifications.

### 👓 The Reader
* **Distraction-Free Interface:** Edge-to-edge Material 3 reading screen with full customization (adjustable font sizes, margins, fonts, and themes including pure AMOLED Vanta Black).
* **Reading Progress:** Automatically saves your scroll position.
* **Auto-Scroll:** Kick back and relax! Toggle auto-scrolling with an adjustable speed slider.
* **Smart Navigation:** Tap zones (left/right) to navigate chapters, overlay HUDs, and seamless prev/next chapter transitions.
* **Read Aloud:** Built-in text-to-speech controls highlight and follow the current paragraph.
* **Quick Chapter Advance:** Pull upward at the end of a chapter to open the next one.

### Account & Cloud Sync
* **Firebase Authentication:** Sign in with email/password or phone verification.
* **Private Bookmark Sync:** Back up and restore bookmarked novels across devices. Sync uses stable IDs, removes stale cloud bookmarks, and safely handles libraries larger than Firestore's 500-write batch limit.
* **Secure Rules:** `firestore.rules` scopes bookmark access strictly to the authenticated user.

### 🔍 Discovery & Sources
* **Multi-Source Support:** Read from NovelFull, LightNovelPub, FreeWebNovel, ScribbleHub, and Royal Road.
* **Advanced Web Scraper:** Utilizes native Jsoup for speed, falling back to a headless Android `WebView` that evaluates JS challenges to seamlessly bypass strict Cloudflare Bot Protection (WAF). 
* **Chapter Synthesis:** Instantly calculates chapters for paginated novels (500+ chapters load instantly without UI freezing).
* **Add Via URL:** Instantly import any supported novel directly from its URL.

---

## 🏗️ Architecture

The project follows a modern Android architecture with clear separation of concerns using **Jetpack Navigation 3.0**.

```
UI (Jetpack Compose / NavDisplay)
        ↓
   ViewModels (Hilt)
        ↓
 Repository (Data Layer)
        ↓
 ┌─────────────────────────┐
 │ Room Database (Local)   │
 │ Source Plugin (Remote)  │
 └─────────────────────────┘
```

The repository acts as the single source of truth, combining local SQLite storage (Room) with online content providers, tracking your reading history, and managing active offline downloads.

---

## 🔌 Source Plugin System

VantaRead uses a modular plugin architecture so that multiple novel sources can be added without changing the core application. Each source implements a common `NovelSource` interface.

Current Implemented Sources:
1. **LightNovelPub:** Fast, optimized with Chapter Synthesis.
2. **NovelFull:** Optimized AJAX routing to bypass pagination.
3. **FreeWebNovel:** Bypasses Cloudflare using the headless WebView scraper.
4. **ScribbleHub:** Advanced scraper for Cloudflare and AJAX chapters.
5. **Royal Road:** Pure Jsoup fallback.

---

## 🛠️ Tech Stack

* **Kotlin**
* **Jetpack Compose & Navigation 3**
* **Material 3 (Dynamic Color Support)**
* **Room Database (SQLite)**
* **Hilt (Dependency Injection)**
* **Coil (Image Loading)**
* **WorkManager (Background Downloads)**
* **Firebase Authentication & Cloud Firestore**
* **Jsoup & Android WebView (Web Scraping)**
* **Coroutines & StateFlow**

---

## Firebase setup

The Android app expects a local Firebase config at `app/google-services.json`. This file is intentionally ignored because it contains a Firebase/Google API key. Use `app/google-services.example.json` as the shape reference, then download the real Android config from Firebase Console for package `com.example.vantaread`.

Do not commit `app/google-services.json`. If a key is accidentally pushed, rotate or revoke it in Google Cloud/Firebase before closing the GitHub secret alert.

Before releasing a build, enable the required sign-in providers in the Firebase console:

1. Enable **Email/Password** under Authentication > Sign-in method.
2. Enable and configure **Phone** authentication, including SHA-1/SHA-256 fingerprints for the release signing key.
3. Create a Cloud Firestore database.
4. Deploy the included `firestore.rules` using the Firebase CLI or paste them into Firestore Rules in the console.

The included rules permit an authenticated user to read and write only `users/{uid}/bookmarks/*`.

---

## 🤝 Contributing

Contributions, bug reports, feature requests, and improvements are welcome.

If you'd like to contribute:
1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a Pull Request.

---

## 📄 License

This project is licensed under the MIT License.
