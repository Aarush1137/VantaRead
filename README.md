# 📖 VantaRead

VantaRead is a modern Android light novel and web novel reader built with **Kotlin**, **Jetpack Compose**, and **Material 3**. It is designed with a plugin-based architecture, allowing support for multiple novel sources while providing a clean, customizable reading experience.

> **Project Status:** 🚀 Under Active Development (Phase 1 Completed)

---

## ✨ Features

### 📚 Personal Library

* Save novels to your local library
* Bookmark favorite novels for easy access
* Offline metadata storage (via Room DB)

### 🔍 Discover & Search

* Search novels from supported sources seamlessly
* Real-time search grid with high-quality cover art
* Primary Source: **Royal Road** (Fast, reliable, and no Cloudflare CAPTCHAs)

### 📖 Novel Details

* Cover artwork and genre tags
* Author information and publication status
* Synopsis description
* Scrollable chapter list with instant reading access

### 👓 Reader

* Clean distraction-free reading interface
* Edge-to-edge Material 3 design
* Light and Dark themes (including pure AMOLED Vanta Black)
* Adjustable font size and margins
* Custom accent colors

---

## 🏗️ Architecture

The project follows a modern Android architecture with clear separation of concerns using Jetpack Navigation 3.0.

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

The repository acts as the single source of truth, combining local storage with online content providers.

---

## 🔌 Source Plugin System

VantaRead uses a modular plugin architecture so that multiple novel sources can be added without changing the core application.

Each source implements a common interface:

```kotlin
interface NovelSource {
    suspend fun searchNovels(query: String): List<Novel>
    suspend fun getPopularNovels(): List<Novel>
    suspend fun getNovelDetails(novelUrl: String): NovelDetails
    suspend fun getChapterList(novelUrl: String): List<Chapter>
    suspend fun getChapterContent(chapterUrl: String): String
}
```

Current Implemented Sources:
1. **Royal Road (Default):** Highly reliable for searching and reading without bot-protection blocks.
2. **NovelFull / WTR-Lab:** Included as experimental sources (Note: Subject to Cloudflare challenges).

---

## 🛠️ Tech Stack

* **Kotlin**
* **Jetpack Compose & Navigation 3**
* **Material 3**
* **Room Database**
* **Hilt (Dependency Injection)**
* **Coil (Image Loading)**
* **Jsoup (Web Scraping)**
* **Coroutines & StateFlow**
* **MVVM Architecture**

---

## 🎯 Roadmap

### Phase 1: Foundation (Completed)
* [x] Android project setup & Compose architecture
* [x] Jetpack Navigation 3 routing setup
* [x] Room database local storage
* [x] Source plugin framework (Hilt Multi-binding)
* [x] Royal Road source integration
* [x] Discover / Search screen
* [x] Novel detail page
* [x] Reader screen with Font/Theme customization

### Phase 2: Progress & History (Up Next)
* [ ] Reading history and "Continue Reading" tracking
* [ ] Save last read page/scroll position
* [ ] Custom tap zones for page turning (left/right/center)
* [ ] Auto-scroll feature

### Phase 3: Offline Support
* [ ] Chapter downloading via WorkManager
* [ ] Offline reading mode support
* [ ] Background notifications for new chapter releases

### Phase 4: Cloud & Social
* [ ] Firebase Authentication (Email/Google)
* [ ] Profile screen with reading statistics
* [ ] Cloud Sync for bookmarks and history

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
