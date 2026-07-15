# 📖 VantaRead

VantaRead is a modern Android light novel and web novel reader built with **Kotlin**, **Jetpack Compose**, and **Material 3**. It is designed with a plugin-based architecture, allowing support for multiple novel sources while providing a clean, customizable reading experience. 

It features an intelligent **headless WebView scraper** designed to bypass Cloudflare Bot Protection seamlessly, allowing you to read from top sites without missing a beat!

> **Project Status:** 🚀 Actively Maintained & Fully Functional!

---

## ✨ Features

### 📚 Personal Library & Offline Downloads
* **Local Library:** Bookmark favorite novels for easy access and tracking.
* **Offline Reading:** Download individual chapters or entire novels for offline reading using a robust WorkManager implementation.
* **Grouped Downloads Screen:** Beautifully organized accordion UI grouping all your downloaded chapters by novel.

### 👓 The Reader
* **Distraction-Free Interface:** Edge-to-edge Material 3 reading screen with full customization (adjustable font sizes, margins, fonts, and themes including pure AMOLED Vanta Black).
* **Reading Progress:** Automatically saves your scroll position.
* **Auto-Scroll:** Kick back and relax! Toggle auto-scrolling with an adjustable speed slider.
* **Smart Navigation:** Tap zones (left/right) to navigate chapters, overlay HUDs, and seamless prev/next chapter transitions.

### 🔍 Discovery & Sources
* **Multi-Source Support:** Read from NovelFull, LightNovelPub, WTR-Lab, and Royal Road.
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
3. **WTR-Lab:** Direct HTML scraping.
4. **Royal Road:** Pure Jsoup fallback.

---

## 🛠️ Tech Stack

* **Kotlin**
* **Jetpack Compose & Navigation 3**
* **Material 3 (Dynamic Color Support)**
* **Room Database (SQLite)**
* **Hilt (Dependency Injection)**
* **Coil (Image Loading)**
* **WorkManager (Background Downloads)**
* **Jsoup & Android WebView (Web Scraping)**
* **Coroutines & StateFlow**

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
