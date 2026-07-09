# 📚 VantaRead

VantaRead is a modern Android light novel and web novel reader built with **Kotlin**, **Jetpack Compose**, and **Material 3**. It is designed with a plugin-based architecture, allowing support for multiple novel sources while providing a clean, customizable reading experience.

> **Project Status:** 🚧 Under Active Development

---

## ✨ Features

### 📖 Personal Library

* Save novels to your local library
* Continue reading from where you left off
* Reading progress tracking
* Offline metadata storage

### 🔍 Discover & Search

* Search novels from supported sources
* Dynamic source selection
* Fast and responsive search interface

### 📚 Novel Details

* Cover artwork
* Author information
* Novel description
* Scrollable chapter list

### 📖 Reader

* Clean distraction-free reading interface
* Edge-to-edge Material 3 design
* Light and Dark themes
* Adjustable font size
* Custom accent colors
* Previous/Next chapter navigation

### ⚙ Reader Customization

* Theme selection
* Font size controls
* Persistent settings using DataStore

---

# 🏗 Architecture

The project follows a modern Android architecture with clear separation of concerns.

```
UI (Jetpack Compose)
        │
   ViewModels
        │
 Repository
        │
 ┌───────────────┐
 │ Room Database │
 │ Source Plugin │
 └───────────────┘
```

The repository acts as the single source of truth, combining local storage with online content providers.

---

# 📂 Project Structure

```
com.example.vantaread
│
├── data
│   ├── database
│   ├── repository
│   ├── model
│   └── source
│
├── di
│
├── ui
│   ├── library
│   ├── discover
│   ├── details
│   ├── reader
│   ├── components
│   └── theme
│
├── datastore
│
└── util
```

---

# 🔌 Source Plugin System

VantaRead uses a modular plugin architecture so that multiple novel sources can be added without changing the core application.

Each source implements a common interface:

```kotlin
interface NovelSource {
    suspend fun searchNovel(query: String): List<Novel>
    suspend fun getNovelDetails(url: String): NovelDetails
    suspend fun getChapterText(url: String): String
}
```

The first implementation will support **wtr-lab.com**, with additional sources planned for future releases.

---

# 🗄 Data Layer

## Room Database

### Novel

Stores:

* Title
* Cover URL
* Author
* Summary
* Source ID

### Chapter

Stores:

* Chapter title
* Chapter URL
* Novel ID
* Read status

---

# 🛠 Tech Stack

* **Kotlin**
* **Jetpack Compose**
* **Material 3**
* **Room Database**
* **Hilt (Dependency Injection)**
* **DataStore Preferences**
* **Jsoup**
* **Coroutines**
* **Flow**
* **MVVM Architecture**

---

# 🚀 Roadmap

## Version 1.0

* [x] Android project setup
* [x] Modern Compose architecture
* [ ] Room database
* [ ] Source plugin framework
* [ ] WTR-Lab source integration
* [ ] Library screen
* [ ] Search screen
* [ ] Novel detail page
* [ ] Reader screen
* [ ] Reader customization
* [ ] Reading progress
* [ ] Persistent settings

---

## Future Plans

* Multiple source plugins
* Offline chapter downloads
* Favorites and bookmarks
* Reading history
* Search filters
* Category browsing
* Chapter caching
* Auto-update library
* Backup & restore
* OPDS support
* Tablet optimizations
* Material You dynamic color
* Sync across devices
* Text-to-Speech (TTS)

---

# ✅ Verification

The application will be verified using:

* Scraping tests for supported sources
* Room persistence testing
* UI testing for Compose screens
* Reader performance testing
* Edge-to-edge compatibility testing

---

# 🤝 Contributing

Contributions, bug reports, feature requests, and improvements are welcome.

If you'd like to contribute:

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a Pull Request.

---

# 📄 License

This project is licensed under the MIT License.

---

## ⭐ Vision

VantaRead aims to become a fast, elegant, and extensible Android novel reader that combines a premium reading experience with a flexible plugin architecture. The goal is to keep the core application lightweight while making it easy to support additional content sources in the future.
